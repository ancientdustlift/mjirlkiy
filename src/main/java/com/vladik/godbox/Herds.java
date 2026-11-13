package com.vladik.godbox;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;

public final class Herds {
   public static boolean on = true;
   static final Map<Kingdoms.K, BlockPos> PEN = new EnumMap<>(Kingdoms.K.class);
   static final Map<Kingdoms.K, Integer> SIZE = new EnumMap<>(Kingdoms.K.class);
   static int tamed;
   private static int t;

   private Herds() {
   }

   static String tag(Kingdoms.K k) {
      return "gb_herd_" + k.id;
   }

   static int count(Kingdoms.K k) {
      return SIZE.getOrDefault(k, 0);
   }

   static boolean wild(AnimalEntity a) {
      return Ecosystem.prey(a) && !Ecosystem.herd(a) && !a.isLeashed();
   }

   static void tame(ServerWorld w, Kingdoms.K k, AnimalEntity a) {
      a.addCommandTag(tag(k));
      a.setPersistent();
      tamed++;
      Fx.particles(w, ParticleTypes.HEART, a.getX(), a.getY() + 1.0, a.getZ(), 5, 0.3, 0.3, 0.3, 0.0);
   }

   public static void tick(MinecraftServer s) {
      if (on && ++t % 200 == 0) {
         for (Kingdoms.K k : Kingdoms.K.values()) {
            if (k.alive && k.capital != null) {
               ServerWorld cap = k.world;
               if (cap instanceof ServerWorld) {
                  BlockPos var17 = k.capital;
                  if (!cap.isChunkLoaded(var17.getX() >> 4, var17.getZ() >> 4)) {
                     continue;
                  }

                  Civ.City c = Civ.city(k);
                  Random r = cap.getRandom();
                  String tg = tag(k);
                  List<AnimalEntity> herd = cap.getEntitiesByClass(
                     AnimalEntity.class, new Box(var17).expand(64.0, 32.0, 64.0), ax -> ax.isAlive() && ax.getCommandTags().contains(tg)
                  );
                  SIZE.put(k, herd.size());
                  int cap2 = 6 + c.era * 2;
                  if (herd.size() < cap2) {
                     List<AnimalEntity> wild = cap.getEntitiesByClass(
                        AnimalEntity.class, new Box(var17).expand(40.0, 20.0, 40.0), ax -> ax.isAlive() && wild(ax)
                     );
                     if (!wild.isEmpty()) {
                        AnimalEntity a = wild.get(r.nextInt(wild.size()));
                        tame(cap, k, a);
                        herd.add(a);
                        if (tamed % 10 == 1) {
                           Chronicle.say(s, k, "приручает диких животных");
                        }
                     }
                  }

                  BlockPos pen = PEN.get(k);
                  if (pen == null && herd.size() >= 3 && c.wood >= 10 && pasture(cap, k, c)) {
                     c.wood -= 10;
                     pen = PEN.get(k);
                  }

                  BlockPos home = pen != null ? pen : var17;
                  int range = pen != null ? 4 : 20;

                  for (AnimalEntity a : herd) {
                     if (a.getTarget() == null && a.getBlockPos().getSquaredDistance(home) > range * range) {
                        a.getNavigation().startMovingTo(home.getX() + 0.5 + r.nextInt(5) - 2.0, home.getY(), home.getZ() + 0.5 + r.nextInt(5) - 2.0, 1.0);
                     }
                  }

                  c.food = c.food + herd.size() / 2;
                  if (c.food < 5 && herd.size() > 2) {
                     AnimalEntity ax = herd.remove(r.nextInt(herd.size()));
                     if (!ax.isBaby()) {
                        ax.discard();
                        c.food += 15;
                        Fx.particles(cap, ParticleTypes.POOF, ax.getX(), ax.getY() + 0.5, ax.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
                     }
                  }

                  if (pen != null && herd.size() >= 2 && herd.size() < cap2 && r.nextInt(4) == 0) {
                     AnimalEntity ax = herd.get(r.nextInt(herd.size()));
                     if (!ax.isBaby() && ax.getBreedingAge() == 0) {
                        Ecosystem.baby(cap, ax);
                     }
                  }
                  continue;
               }
            }

            SIZE.remove(k);
         }
      }
   }

   static boolean pasture(ServerWorld w, Kingdoms.K k, Civ.City c) {
      Random r = w.getRandom();
      List<Civ.Building> bs = Highways.allBuildings();

      for (int tr = 0; tr < 10; tr++) {
         double a = r.nextDouble() * Math.PI * 2.0;
         int d = 18 + r.nextInt(14);
         int x = k.capital.getX() + (int)(Math.cos(a) * d);
         int z = k.capital.getZ() + (int)(Math.sin(a) * d);
         if (w.isChunkLoaded(x >> 4, z >> 4) && TownBuilder.dryArea(w, x, z, 5)) {
            boolean clash = false;

            for (int dx = -5; dx <= 5 && !clash; dx += 2) {
               for (int dz = -5; dz <= 5; dz += 2) {
                  if (Roads.inBuilding(bs, x + dx, z + dz)) {
                     clash = true;
                     break;
                  }
               }
            }

            if (!clash) {
               int y = TownBuilder.ground(w, x, z);
               Highways.Place pl = new Highways.Place(w);
               Block fence = c.era >= 10 ? Blocks.IRON_BARS : (c.era >= 4 ? Blocks.SPRUCE_FENCE : Blocks.OAK_FENCE);

               for (int dx = -4; dx <= 4; dx++) {
                  for (int dzx = -4; dzx <= 4; dzx++) {
                     boolean edge = Math.abs(dx) == 4 || Math.abs(dzx) == 4;
                     pl.put(new BlockPos(x + dx, y - 1, z + dzx), Blocks.GRASS_BLOCK);
                     pl.put(new BlockPos(x + dx, y, z + dzx), edge ? fence : Blocks.AIR);
                     pl.put(new BlockPos(x + dx, y + 1, z + dzx), Blocks.AIR);
                  }
               }

               pl.put(new BlockPos(x, y, z - 4), Blocks.OAK_FENCE_GATE);
               pl.put(new BlockPos(x + 3, y, z + 3), Blocks.HAY_BLOCK);
               pl.put(new BlockPos(x - 3, y, z + 3), Blocks.WATER_CAULDRON);
               pl.put(new BlockPos(x + 4, y + 1, z + 4), Blocks.LANTERN);
               pl.put(new BlockPos(x - 4, y + 1, z - 4), Blocks.LANTERN);
               Tasks.add(pl);
               PEN.put(k, new BlockPos(x, y, z));
               Chronicle.say(w.getServer(), k, "строит пастбище для стада");
               return true;
            }
         }
      }

      return false;
   }

   public static void power(ServerPlayerEntity god, ServerWorld w, BlockPos pos) {
      Kingdoms.K k = Tech.nearest(w, pos);
      if (k == null) {
         god.sendMessage(Text.literal("Рядом нет королевства"), true);
      } else {
         int n = 0;

         for (AnimalEntity a : w.getEntitiesByClass(AnimalEntity.class, new Box(pos).expand(10.0, 6.0, 10.0), ax -> ax.isAlive() && wild(ax))) {
            tame(w, k, a);
            if (k.capital != null) {
               a.getNavigation().startMovingTo(k.capital.getX() + 0.5, k.capital.getY(), k.capital.getZ() + 0.5, 1.1);
            }

            n++;
         }

         Fx.sound(w, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ENTITY_VILLAGER_YES, 2.0F, 1.0F);
         god.sendMessage(Text.literal("\ud83d\udc11 " + k.title + " приручило " + n + " животных").formatted(k.color), true);
      }
   }

   public static void clear() {
      PEN.clear();
      SIZE.clear();
      tamed = 0;
      t = 0;
   }
}
