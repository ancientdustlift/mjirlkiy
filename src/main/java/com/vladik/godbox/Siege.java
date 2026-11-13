package com.vladik.godbox;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World.ExplosionSourceType;

public final class Siege {
   public static boolean on = true;
   static final Map<Kingdoms.K, Integer> WALL = new EnumMap<>(Kingdoms.K.class);
   private static int t;

   private Siege() {
   }

   static boolean natural(BlockState s) {
      return s.isIn(BlockTags.DIRT)
         || s.isOf(Blocks.SAND)
         || s.isOf(Blocks.RED_SAND)
         || s.isOf(Blocks.STONE)
         || s.isOf(Blocks.SNOW_BLOCK)
         || s.isOf(Blocks.ANDESITE)
         || s.isOf(Blocks.GRANITE)
         || s.isOf(Blocks.DIORITE)
         || s.isOf(Blocks.TERRACOTTA);
   }

   static int radius(Kingdoms.K k) {
      int r = 14;

      for (Civ.Building b : Civ.city(k).buildings) {
         int d = (int)Math.ceil(Math.sqrt(Math.pow(b.cx() - k.capital.getX(), 2.0) + Math.pow(b.cz() - k.capital.getZ(), 2.0)))
            + Math.max(b.x1 - b.x0, b.z1 - b.z0) / 2;
         if (d + 6 > r) {
            r = d + 6;
         }
      }

      return Math.min(60, r);
   }

   static void walls(ServerWorld w, Kingdoms.K k, int lvl) {
      TownBuilder.Job j = new TownBuilder.Job(w);
      int R = radius(k);
      int cx = k.capital.getX();
      int cz = k.capital.getZ();
      int n = (int)Math.ceil((Math.PI * 2) * R * 1.5);
      Set<Long> seen = new HashSet<>();
      List<Civ.Building> bs = new ArrayList<>(Civ.city(k).buildings);
      BlockState body = lvl >= 2 ? Blocks.STONE_BRICKS.getDefaultState() : Blocks.STRIPPED_SPRUCE_LOG.getDefaultState();
      BlockState top = lvl >= 2 ? Blocks.STONE_BRICK_WALL.getDefaultState() : Blocks.SPRUCE_FENCE.getDefaultState();
      int h = lvl >= 2 ? 4 : 3;

      for (int i = 0; i < n; i++) {
         double a = (Math.PI * 2) * i / n;
         double deg = Math.toDegrees(a) % 90.0;
         if (!(deg < 360.0 / n * 3.0) && !(deg > 90.0 - 360.0 / n * 3.0)) {
            int x = cx + (int)Math.round(Math.cos(a) * R);
            int z = cz + (int)Math.round(Math.sin(a) * R);
            if (seen.add(BlockPos.asLong(x, 0, z)) && w.isChunkLoaded(x >> 4, z >> 4) && !Roads.inBuilding(bs, x, z)) {
               int y = TownBuilder.ground(w, x, z);
               BlockPos g = new BlockPos(x, y - 1, z);
               BlockState s = w.getBlockState(g);
               if (s.getFluidState().isEmpty() && !Roads.road(s) && (natural(s) || s.isOf(Blocks.STRIPPED_SPRUCE_LOG) || s.isOf(Blocks.STONE_BRICKS))) {
                  for (int dy = 0; dy < h; dy++) {
                     j.put(g.up(dy), body);
                  }

                  if ((x + z & 1) == 0) {
                     j.put(g.up(h), top);
                  }
               }
            }
         }
      }

      if (lvl >= 2) {
         for (int ix = 0; ix < 8; ix++) {
            double a = (Math.PI * 2) * ix / 8.0 + (Math.PI / 8);
            int x = cx + (int)Math.round(Math.cos(a) * R);
            int z = cz + (int)Math.round(Math.sin(a) * R);
            if (w.isChunkLoaded(x >> 4, z >> 4) && TownBuilder.dry(w, x, z)) {
               int y = TownBuilder.ground(w, x, z);

               for (int dx = -1; dx <= 1; dx++) {
                  for (int dz = -1; dz <= 1; dz++) {
                     for (int dy = -1; dy < 7; dy++) {
                        j.put(new BlockPos(x + dx, y + dy, z + dz), Blocks.STONE_BRICKS.getDefaultState());
                     }

                     if ((dx + dz & 1) == 0) {
                        j.put(new BlockPos(x + dx, y + 7, z + dz), Blocks.STONE_BRICK_WALL.getDefaultState());
                     }
                  }
               }

               j.put(new BlockPos(x, y + 7, z), Blocks.LANTERN.getDefaultState());
            }
         }
      }

      Tasks.add(j);
      WALL.put(k, lvl);
      Chronicle.say(w.getServer(), k, lvl >= 2 ? "возводит каменные стены с башнями" : "обносит столицу частоколом");
   }

   static void shoot(ServerWorld w, MobEntity m, BlockPos target) {
      lob(w, m, m.getX(), m.getEyeY() + 1.0, m.getZ(), target, new ItemStack(Items.COBBLESTONE), 2.5F);
   }

   static void lob(ServerWorld w, MobEntity m, double ox, double oy, double oz, BlockPos target, ItemStack item, float power) {
      double dx = target.getX() + 0.5 - ox;
      double dz = target.getZ() + 0.5 - oz;
      double dy = target.getY() - oy;
      double d = Math.sqrt(dx * dx + dz * dz);
      double T = Math.max(20.0, d / 1.1);
      SnowballEntity sb = new SnowballEntity(w, m);
      sb.setItem(item);
      sb.setPosition(ox, oy, oz);
      sb.setVelocity(dx / T * 1.25, (dy + 0.015 * T * T) / T * 1.15, dz / T * 1.25);
      w.spawnEntity(sb);
      Fx.sound(w, ox, oy, oz, SoundEvents.BLOCK_PISTON_EXTEND, 1.5F, 0.5F);
      Vec3d[] last = new Vec3d[]{sb.getPos()};
      int[] age = new int[]{0};
      Tasks.add(() -> {
         age[0]++;
         if (sb.isAlive() && age[0] < 200) {
            last[0] = sb.getPos();
            if (age[0] % 2 == 0) {
               Fx.particles(w, ParticleTypes.SMOKE, last[0].x, last[0].y, last[0].z, 2, 0.05, 0.05, 0.05, 0.0);
            }

            return false;
         } else {
            if (sb.isAlive()) {
               sb.discard();
            }

            w.createExplosion(null, last[0].x, last[0].y, last[0].z, power, ExplosionSourceType.TNT);
            return true;
         }
      });
   }

   static void catapults(ServerWorld w, Kingdoms.K k) {
      Civ.City c = Civ.city(k);
      int shots = 0;

      for (Kingdoms.K o : Kingdoms.K.values()) {
         if (o != k && o.alive && o.world == k.world && Diplomacy.rel(k, o) == 1) {
            List<BlockPos> targets = new ArrayList<>(Conquest.towns(o));
            if (o.capital != null) {
               targets.add(o.capital);
            }

            for (MobEntity m : new ArrayList<>(k.members)) {
               if (shots >= 2 || c.stone < 1) {
                  return;
               }

               if (m.isAlive() && !(m instanceof VillagerEntity)) {
                  for (BlockPos tp : targets) {
                     double d2 = m.getBlockPos().getSquaredDistance(tp);
                     if (!(d2 > 2500.0) && !(d2 < 144.0)) {
                        BlockPos g = Fx.surface(w, tp.getX() + w.getRandom().nextInt(9) - 4, tp.getZ() + w.getRandom().nextInt(9) - 4);
                        int r = SiegeEngines.request(w, k, m, g, Tech.has(k, Tech.T.GUNPOWDER) ? SiegeEngines.Type.CANNON : SiegeEngines.Type.CATAPULT);
                        if (r != 1) {
                           if (r == 0 && !Tech.cannon(w, k, m, g)) {
                              shoot(w, m, g);
                           }

                           c.stone--;
                           shots++;
                        }
                        break;
                     }
                  }
               }
            }
         }
      }
   }

   public static void tick(MinecraftServer s) {
      t++;
      if (on) {
         if (t % 100 == 0) {
            for (Kingdoms.K k : Kingdoms.K.values()) {
               if (k.alive) {
                  ServerWorld want = k.world;
                  if (want instanceof ServerWorld && Tech.has(k, Tech.T.MECHANICS)) {
                     catapults(want, k);
                  }
               }
            }
         }

         if (t % 3000 == 0) {
            for (Kingdoms.K kx : Kingdoms.K.values()) {
               if (kx.alive && kx.capital != null) {
                  ServerWorld var12 = kx.world;
                  if (var12 instanceof ServerWorld && Tech.has(kx, Tech.T.FORT)) {
                     int want = Civ.city(kx).era >= 4 ? 2 : 1;
                     if (!Tech.bastions(var12, kx) && WALL.getOrDefault(kx, 0) < want) {
                        Civ.City c = Civ.city(kx);
                        if (c.stone >= 40 && c.wood >= 20) {
                           c.stone -= 40;
                           c.wood -= 20;
                           walls(var12, kx, want);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static void power(ServerPlayerEntity god, ServerWorld w, BlockPos pos) {
      Kingdoms.K k = Tech.nearest(w, pos);
      if (k != null && k.capital != null) {
         walls(w, k, Civ.city(k).era >= 4 ? 2 : 1);
         god.sendMessage(Text.literal("«" + k.title + "» строит стены").formatted(k.color), true);
      } else {
         god.sendMessage(Text.literal("Нет королевства рядом"), true);
      }
   }

   public static void clear() {
      WALL.clear();
      t = 0;
   }
}
