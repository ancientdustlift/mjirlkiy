package com.vladik.godbox;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public final class Vassals {
   public static boolean on = true;
   static final Map<Kingdoms.K, Kingdoms.K> LORD = new EnumMap<>(Kingdoms.K.class);
   private static final Set<Kingdoms.K> EMPIRE = EnumSet.noneOf(Kingdoms.K.class);
   private static int t;

   private Vassals() {
   }

   static int pop(Kingdoms.K k) {
      int n = 0;

      for (MobEntity m : k.members) {
         if (m.isAlive()) {
            n++;
         }
      }

      return n;
   }

   static Kingdoms.K lord(Kingdoms.K k) {
      return LORD.get(k);
   }

   static int count(Kingdoms.K l) {
      int n = 0;

      for (Kingdoms.K v : LORD.values()) {
         if (v == l) {
            n++;
         }
      }

      return n;
   }

   public static void tick(MinecraftServer s) {
      if (++t % 200 == 0) {
         for (Kingdoms.K v : new ArrayList<>(LORD.keySet())) {
            Kingdoms.K l = LORD.get(v);
            if (!v.alive) {
               LORD.remove(v);
            } else if (!l.alive) {
               LORD.remove(v);
               Chronicle.say(s, v, "обретает независимость после падения сюзерена");
            }
         }

         for (Entry<Kingdoms.K, Kingdoms.K> e : LORD.entrySet()) {
            Kingdoms.K vx = e.getKey();
            Kingdoms.K l = e.getValue();
            if (Diplomacy.rel(vx, l) != 2) {
               Diplomacy.set(vx, l, 2);
            }

            for (Kingdoms.K o : Kingdoms.K.values()) {
               if (o != vx && o != l && o.alive && Diplomacy.rel(l, o) == 1 && Diplomacy.rel(vx, o) != 1) {
                  Diplomacy.set(vx, o, 1);
               }
            }
         }

         if (t % 2400 == 0) {
            tribute();
         }

         if (on) {
            for (Kingdoms.K a : Kingdoms.K.values()) {
               for (Kingdoms.K d : Kingdoms.K.values()) {
                  if (a != d
                     && a.alive
                     && d.alive
                     && a.world == d.world
                     && a.world != null
                     && Diplomacy.rel(a, d) == 1
                     && !LORD.containsKey(d)
                     && LORD.get(a) != d) {
                     int pa = pop(a);
                     int pd = pop(d);
                     if (pd != 0 && pa >= 8 && pd * 3 <= pa && a.world.getRandom().nextInt(3) == 0) {
                        submit(s, d, a);
                     }
                  }
               }
            }

            if (t % 1200 == 0) {
               for (Kingdoms.K vx : new ArrayList<>(LORD.keySet())) {
                  Kingdoms.K l = LORD.get(vx);
                  if (vx.world != null && pop(vx) > pop(l) * 6 / 5 + 3 && vx.world.getRandom().nextInt(4) == 0) {
                     LORD.remove(vx);
                     Chronicle.say(s, vx, "восстаёт против сюзерена «" + l.title + "»!");
                     Diplomacy.declare(s, vx, l);
                  }
               }
            }
         }

         for (Kingdoms.K l : Kingdoms.K.values()) {
            if (!l.alive || count(l) < 2) {
               EMPIRE.remove(l);
            } else if (EMPIRE.add(l)) {
               Diplomacy.broadcast(s, Text.literal("\ud83d\udc51 " + l.title + " провозглашает себя ИМПЕРИЕЙ!").formatted(l.color, Formatting.BOLD));
               Chronicle.log(l.title + " стало империей (вассалов: " + count(l) + ")");
            }
         }
      }
   }

   static void submit(MinecraftServer s, Kingdoms.K d, Kingdoms.K a) {
      LORD.put(d, a);

      for (Kingdoms.K v : new ArrayList<>(LORD.keySet())) {
         if (LORD.get(v) == d) {
            LORD.put(v, a);
         }
      }

      LORD.remove(a, d);
      Diplomacy.set(a, d, 2);

      for (MobEntity m : d.members) {
         m.setTarget(null);
      }

      for (MobEntity m : a.members) {
         if (m.getTarget() instanceof MobEntity tm && Kingdoms.of(tm) == d) {
            m.setTarget(null);
         }
      }

      ServerWorld var11 = d.world;
      if (var11 instanceof ServerWorld && d.capital != null) {
         Fx.sound(var11, d.capital.getX(), d.capital.getY(), d.capital.getZ(), SoundEvents.BLOCK_BELL_USE, 4.0F, 0.7F);
      }

      Diplomacy.broadcast(s, Text.literal("\ud83c\udff3 " + d.title + " склоняет колено перед «" + a.title + "» и становится вассалом").formatted(a.color));
      Chronicle.log(d.title + " стало вассалом «" + a.title + "»");
   }

   static void tribute() {
      for (Entry<Kingdoms.K, Kingdoms.K> e : LORD.entrySet()) {
         Civ.City v = Civ.city(e.getKey());
         Civ.City l = Civ.city(e.getValue());
         int w = v.wood / 5;
         int st = v.stone / 5;
         int f = v.food / 6;
         int o = v.ore / 5;
         v.wood -= w;
         l.wood += w;
         v.stone -= st;
         l.stone += st;
         v.food -= f;
         l.food += f;
         v.ore -= o;
         l.ore += o;
         int g = Economy.gold(e.getKey()) / 4;
         Economy.add(e.getKey(), -g);
         Economy.add(e.getValue(), g);
      }
   }

   public static void power(ServerPlayerEntity god, ServerWorld w, BlockPos pos) {
      Kingdoms.K d = Tech.nearest(w, pos);
      if (d == null) {
         god.sendMessage(Text.literal("Рядом нет королевства"), true);
      } else if (LORD.containsKey(d)) {
         Kingdoms.K l = LORD.remove(d);
         Chronicle.say(w.getServer(), d, "освобождено богом от власти «" + l.title + "»");
      } else {
         Kingdoms.K a = null;

         for (Kingdoms.K o : Kingdoms.K.values()) {
            if (o != d && o.alive && o.world == w && LORD.get(o) != d && (a == null || pop(o) > pop(a))) {
               a = o;
            }
         }

         if (a == null) {
            god.sendMessage(Text.literal("Нет другого королевства, которому можно присягнуть"), true);
         } else {
            submit(w.getServer(), d, a);
         }
      }
   }

   static String info(Kingdoms.K k) {
      Kingdoms.K l = LORD.get(k);
      if (l != null) {
         return "вассал «" + l.title + "»";
      } else {
         int n = count(k);
         if (n >= 2) {
            return "Империя, вассалов " + n;
         } else {
            return n == 1 ? "сюзерен, вассалов 1" : "независимо";
         }
      }
   }

   public static void clear() {
      LORD.clear();
      EMPIRE.clear();
      t = 0;
   }
}
