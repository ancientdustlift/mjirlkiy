package com.vladik.godbox;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class Dynasty {
   static final String ROYAL = "gbroyal:";
   static final String REGNAL = "gbr:";
   static final String PARENT = "gbp:";
   public static final Map<Kingdoms.K, Dynasty.House> HOUSES = new EnumMap<>(Kingdoms.K.class);

   private Dynasty() {
   }

   static Dynasty.House house(Kingdoms.K k) {
      return HOUSES.computeIfAbsent(k, x -> new Dynasty.House());
   }

   public static String houseName(Kingdoms.K k) {
      Dynasty.House h = HOUSES.get(k);
      return h != null && !h.name.isEmpty() ? h.name : "—";
   }

   static String first(MobEntity m) {
      String n = Life.name(m);
      int i = n.indexOf(32);
      return i > 0 ? n.substring(0, i) : n;
   }

   static String familyName(String root) {
      if (root.isEmpty()) {
         return "Безымянные";
      } else {
         char last = root.charAt(root.length() - 1);
         return "аяоеийь".indexOf(last) >= 0 ? root.substring(0, root.length() - 1) + "ичи" : root + "овичи";
      }
   }

   static String roman(int n) {
      String[] r = new String[]{"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
      return n >= 1 && n <= 10 ? r[n] : Integer.toString(n);
   }

   static boolean royal(Kingdoms.K k, MobEntity m) {
      Dynasty.House h = HOUSES.get(k);
      String t = Life.tag(m, "gbroyal:");
      return h != null && h.id > 0 && t != null && t.equals(Integer.toString(h.id));
   }

   static List<MobEntity> blood(Kingdoms.K k) {
      List<MobEntity> out = new ArrayList<>();

      for (MobEntity m : k.members) {
         if (m.isAlive() && m instanceof VillagerEntity && royal(k, m)) {
            out.add(m);
         }
      }

      out.sort((a, b) -> Integer.compare(Life.age(b), Life.age(a)));
      return out;
   }

   public static MobEntity heir(Kingdoms.K k) {
      List<MobEntity> b = blood(k);

      for (MobEntity m : b) {
         if (!m.isBaby()) {
            return m;
         }
      }

      return b.isEmpty() ? null : b.get(0);
   }

   public static MobEntity pick(Kingdoms.K k) {
      MobEntity h = heir(k);
      if (h != null) {
         return h;
      } else {
         MobEntity best = null;
         int ba = -1;

         for (MobEntity m : k.members) {
            if (m instanceof VillagerEntity v && v.isAlive() && !v.isBaby()) {
               int a = Life.age(v);
               if (a > ba) {
                  ba = a;
                  best = v;
               }
            }
         }

         return best;
      }
   }

   public static void crowned(MinecraftServer s, Kingdoms.K k, ServerWorld w, MobEntity m) {
      Dynasty.House h = house(k);
      boolean inherit = royal(k, m);
      if (!inherit) {
         String old = h.name;
         h.id++;
         h.name = familyName(first(m));
         h.reigns = 0;
         h.regnal.clear();
         Life.setTag(m, "gbroyal:", Integer.toString(h.id));
         if (!old.isEmpty()) {
            Chronicle.say(s, k, "династия " + old + " пресеклась");
         }
      }

      h.reigns++;
      String root = first(m);
      int n = h.regnal.merge(root, 1, Integer::sum);
      Life.setTag(m, "gbr:", roman(n));
      String who = root + " " + roman(n);
      if (inherit) {
         Chronicle.say(
            s, k, "престол унаследовал" + (m.isBaby() ? " малолетний " : " ") + who + " из рода " + h.name + " (" + h.reigns + "-й правитель династии)"
         );
      } else {
         Chronicle.say(s, k, "коронован " + who + " — основана династия " + h.name);
      }
   }

   public static void ensure(MinecraftServer s, Kingdoms.K k, ServerWorld w, MobEntity king) {
      if (!royal(k, king)) {
         crowned(s, k, w, king);
      }
   }

   public static boolean dispute(MinecraftServer s, Kingdoms.K k, ServerWorld w, MobEntity heir) {
      List<MobEntity> b = blood(k);
      MobEntity rival = null;

      for (MobEntity m : b) {
         if (m != heir && !m.isBaby()) {
            rival = m;
            break;
         }
      }

      if (rival == null) {
         return false;
      } else {
         boolean weak = heir.isBaby() || Life.age(rival) >= Life.age(heir) - 3;
         if (weak && w.getRandom().nextInt(3) == 0) {
            List<BlockPos> towns = Conquest.TOWNS.get(k);
            if (towns != null && !towns.isEmpty()) {
               BlockPos p = towns.get(w.getRandom().nextInt(towns.size()));
               if (!w.isChunkLoaded(p)) {
                  return false;
               } else {
                  Chronicle.say(s, k, "спор за трон! " + first(rival) + " не признаёт " + first(heir) + " и поднимает мятеж");
                  Fx.particles(w, ParticleTypes.ANGRY_VILLAGER, rival.getX(), rival.getY() + 2.0, rival.getZ(), 12, 0.5, 0.5, 0.5, 0.05);
                  rival.removeCommandTag("gbroyal:" + Life.tag(rival, "gbroyal:"));
                  Rebels.rebel(w, k, p);
                  return true;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   public static MobEntity born(Kingdoms.K k, VillagerEntity child) {
      if (k == null) {
         return null;
      } else {
         List<MobEntity> adults = new ArrayList<>();

         for (MobEntity m : k.members) {
            if (m != child && m.isAlive() && m instanceof VillagerEntity v && !v.isBaby()) {
               adults.add(m);
            }
         }

         if (adults.isEmpty()) {
            return null;
         } else {
            MobEntity parent = null;
            MobEntity king = Kings.king(k);
            List<MobEntity> blood = blood(k);
            int roll = child.getRandom().nextInt(100);
            if (king != null && roll < 25) {
               parent = king;
            } else if (!blood.isEmpty() && roll < 40) {
               List<MobEntity> grown = new ArrayList<>();

               for (MobEntity mx : blood) {
                  if (!mx.isBaby()) {
                     grown.add(mx);
                  }
               }

               if (!grown.isEmpty()) {
                  parent = grown.get(child.getRandom().nextInt(grown.size()));
               }
            }

            if (parent == null) {
               double best = Double.MAX_VALUE;

               for (MobEntity mxx : adults) {
                  double d = mxx.squaredDistanceTo(child);
                  if (d < best) {
                     best = d;
                     parent = mxx;
                  }
               }
            }

            if (parent == null) {
               return null;
            } else {
               Life.setTag(child, "gbp:", first(parent));
               String r = Life.tag(parent, "gbroyal:");
               if (r != null && royal(k, parent)) {
                  Life.setTag(child, "gbroyal:", r);
                  Treaties.born(k, child);
                  ServerWorld w = k.world;
                  if (w != null) {
                     Civ.tell(
                        w,
                        Text.literal(
                              "\ud83d\udc76 " + k.title + ": в роду " + houseName(k) + " родился наследник — " + first(child) + ", дитя " + first(parent)
                           )
                           .formatted(k.color)
                     );
                     Fx.particles(w, ParticleTypes.HEART, child.getX(), child.getY() + 1.0, child.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
                  }
               }

               return parent;
            }
         }
      }
   }

   public static String describe(Kingdoms.K k) {
      Dynasty.House h = HOUSES.get(k);
      return h != null && !h.name.isEmpty() ? "род " + h.name + " (правителей: " + h.reigns + ", кровных родичей: " + blood(k).size() + ")" : "династии нет";
   }

   static List<String> save() {
      List<String> o = new ArrayList<>();

      for (Entry<Kingdoms.K, Dynasty.House> e : HOUSES.entrySet()) {
         Dynasty.House h = e.getValue();
         if (h.id != 0) {
            StringBuilder sb = new StringBuilder();

            for (Entry<String, Integer> r : h.regnal.entrySet()) {
               if (sb.length() > 0) {
                  sb.append(',');
               }

               sb.append(r.getKey()).append(':').append(r.getValue());
            }

            o.add("$|" + e.getKey().name() + "|" + h.id + "|" + h.name.replace("|", "") + "|" + h.reigns + "|" + (sb.length() == 0 ? "-" : sb));
         }
      }

      return o;
   }

   static void load(String[] a) {
      try {
         Dynasty.House h = house(Kingdoms.K.valueOf(a[1]));
         h.id = Integer.parseInt(a[2]);
         h.name = a[3];
         h.reigns = Integer.parseInt(a[4]);
         h.regnal.clear();
         if (a.length > 5 && !a[5].equals("-")) {
            for (String p : a[5].split(",")) {
               int i = p.lastIndexOf(58);
               if (i > 0) {
                  h.regnal.put(p.substring(0, i), Integer.parseInt(p.substring(i + 1)));
               }
            }
         }
      } catch (Exception var7) {
      }
   }

   public static void clear() {
      HOUSES.clear();
   }

   static final class House {
      int id;
      String name = "";
      int reigns;
      final Map<String, Integer> regnal = new HashMap<>();
   }
}
