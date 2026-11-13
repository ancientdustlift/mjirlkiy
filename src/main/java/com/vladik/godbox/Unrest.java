package com.vladik.godbox;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public final class Unrest {
   public static boolean on = true;
   static final Map<Kingdoms.K, Integer> UNREST = new EnumMap<>(Kingdoms.K.class);
   private static int t;

   private Unrest() {
   }

   static int unrest(Kingdoms.K k) {
      return UNREST.getOrDefault(k, 0);
   }

   static void add(Kingdoms.K k, int n) {
      UNREST.put(k, Math.max(0, Math.min(100, unrest(k) + n)));
   }

   public static void tick(MinecraftServer s) {
      if (++t % 1200 == 0) {
         for (Kingdoms.K k : Kingdoms.K.values()) {
            if (!k.alive) {
               UNREST.remove(k);
            } else if (k.capital != null) {
               ServerWorld c = k.world;
               if (c instanceof ServerWorld) {
                  Civ.City cx = Civ.city(k);
                  int pop = Vassals.pop(k);
                  int towns = Conquest.towns(k).size();
                  int d = -1;
                  if (pop > 35) {
                     d++;
                  }

                  if (pop > 60) {
                     d++;
                  }

                  d += Math.max(0, towns - 2);
                  if (cx.hungry) {
                     d += 2;
                  }

                  if (Diplomacy.atWar(k)) {
                     d++;
                  }

                  if (cx.count(Civ.Kind.TEMPLE) > 0) {
                     d--;
                  }

                  if (cx.religion >= 0 && cx.faith > 50) {
                     d--;
                  }

                  if (Kings.king(k) == null) {
                     d += 2;
                  }

                  if (towns == 0 && pop < 25) {
                     d -= 2;
                  }

                  add(k, d);
                  if (on && unrest(k) >= 70 && c.getRandom().nextInt(3) == 0) {
                     uprising(s, c, k);
                  }
               }
            }
         }
      }
   }

   static void uprising(MinecraftServer s, ServerWorld w, Kingdoms.K k) {
      List<BlockPos> ts = Conquest.towns(k);
      Kingdoms.K free = null;

      for (Kingdoms.K o : Kingdoms.K.values()) {
         if (!o.alive) {
            free = o;
            break;
         }
      }

      if (!ts.isEmpty() && free != null) {
         BlockPos far = null;
         double fd = -1.0;

         for (BlockPos p : ts) {
            double d = p.getSquaredDistance(k.capital);
            if (d > fd) {
               fd = d;
               far = p;
            }
         }

         split(s, w, k, free, far);
      } else {
         mutiny(s, w, k);
      }

      UNREST.put(k, 25);
   }

   static void split(MinecraftServer s, ServerWorld w, Kingdoms.K k, Kingdoms.K n, BlockPos p) {
      Conquest.towns(k).remove(p);
      n.world = w;
      n.capital = p;
      n.alive = true;
      n.grow = 0;
      n.fallTimer = 0;
      n.members.clear();
      Races.set(n, Races.of(k));
      Civ.found(n);
      Civ.City cn = Civ.city(n);
      Civ.City ck = Civ.city(k);
      cn.era = ck.era;
      cn.religion = ck.religion;
      cn.culture = ck.culture;
      cn.faith = ck.faith / 2;
      cn.wood = ck.wood / 3;
      ck.wood = ck.wood - cn.wood;
      cn.stone = ck.stone / 3;
      ck.stone = ck.stone - cn.stone;
      cn.food = ck.food / 3;
      ck.food = ck.food - cn.food;
      cn.ore = ck.ore / 3;
      ck.ore = ck.ore - cn.ore;

      for (Civ.Building b : new ArrayList<>(ck.buildings)) {
         if (b != ck.quarry && b.done) {
            double dx = (b.x0 + b.x1) / 2.0 - p.getX();
            double dz = (b.z0 + b.z1) / 2.0 - p.getZ();
            if (!(dx * dx + dz * dz > 2025.0)) {
               ck.buildings.remove(b);
               cn.buildings.add(b);
            }
         }
      }

      int moved = 0;

      for (MobEntity m : new ArrayList<>(k.members)) {
         if (m.isAlive()
            && !m.getCommandTags().contains("gbking")
            && !m.getCommandTags().contains("gb_king")
            && !(m.squaredDistanceTo(p.getX(), m.getY(), p.getZ()) > 2500.0)) {
            k.members.remove(m);
            Kingdoms.join(n, m);
            n.members.add(m);
            m.setTarget(null);
            moved++;
         }
      }

      Random r = w.getRandom();

      for (int i = moved; i < 6; i++) {
         Kingdoms.spawn(n, i % 2 == 0 ? EntityType.VILLAGER : Kingdoms.soldier(r), Fx.surface(w, p.getX() + r.nextInt(7) - 3, p.getZ() + r.nextInt(7) - 3));
      }

      Conquest.recolor(w, k.wool, n.wool, p, 45);
      Tech.MASK.put(n, Tech.MASK.getOrDefault(k, 0));
      Economy.GOLD.put(n, Economy.gold(k) / 3);
      Economy.add(k, -Economy.gold(k) / 3);
      Vassals.LORD.remove(n);
      Diplomacy.declare(s, n, k);
      Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.EVENT_RAID_HORN.value(), 6.0F, 0.8F);
      Diplomacy.broadcast(
         s,
         Text.literal("\ud83d\udd25 ГРАЖДАНСКАЯ ВОЙНА! Город «" + Colonies.name(p) + "» откололся от «" + k.title + "» и стал «" + n.title + "»")
            .formatted(n.color, Formatting.BOLD)
      );
      Chronicle.log("Гражданская война: «" + n.title + "» откололось от «" + k.title + "»");
   }

   static void mutiny(MinecraftServer s, ServerWorld w, Kingdoms.K k) {
      Random r = w.getRandom();
      int n = 3 + r.nextInt(3);

      for (int i = 0; i < n; i++) {
         double a = r.nextDouble() * Math.PI * 2.0;
         BlockPos p = Fx.surface(w, k.capital.getX() + (int)(Math.cos(a) * 14.0), k.capital.getZ() + (int)(Math.sin(a) * 14.0));
         MobEntity m = (i % 2 == 0 ? EntityType.VINDICATOR : EntityType.PILLAGER).create(w);
         if (m != null) {
            m.refreshPositionAndAngles(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, r.nextFloat() * 360.0F, 0.0F);
            m.setCustomName(Text.literal("Мятежник").formatted(Formatting.DARK_RED));
            m.setPersistent();
            w.spawnEntity(m);
         }
      }

      Civ.city(k).food = Math.max(0, Civ.city(k).food - 10);
      Chronicle.say(s, k, "— в столице вспыхнул мятеж! Вооружённые бунтовщики грабят амбары");
   }

   public static void power(ServerPlayerEntity god, ServerWorld w, BlockPos pos) {
      Kingdoms.K k = Tech.nearest(w, pos);
      if (k != null && k.capital != null) {
         uprising(w.getServer(), w, k);
      } else {
         god.sendMessage(Text.literal("Рядом нет королевства"), true);
      }
   }

   public static void clear() {
      UNREST.clear();
      t = 0;
   }
}
