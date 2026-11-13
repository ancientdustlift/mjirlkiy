package com.vladik.godbox;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap.Type;

final class CivPlus {
   static final int NO = Integer.MIN_VALUE;

   private CivPlus() {
   }

   static int cap(Civ.City c) {
      return 6 + 3 * c.count(Civ.Kind.HOUSE) + 6 * c.count(Civ.Kind.MANOR) + 2 * c.count(Civ.Kind.TEMPLE);
   }

   static Civ.Kind choose(Civ.City c, int vill, Random r) {
      int houses = c.count(Civ.Kind.HOUSE);
      int manors = c.count(Civ.Kind.MANOR);
      int homes = houses + manors;
      int farms = c.count(Civ.Kind.FARM);
      int bar = c.count(Civ.Kind.BARRACKS);
      int tow = c.count(Civ.Kind.TOWER);
      int well = c.count(Civ.Kind.WELL);
      int mill = c.count(Civ.Kind.MILL);
      int smithy = c.count(Civ.Kind.SMITHY);
      int gran = c.count(Civ.Kind.GRANARY);
      int temple = c.count(Civ.Kind.TEMPLE);
      Civ.Kind home = c.era >= 3 && homes >= 5 && r.nextInt(3) == 0 ? Civ.Kind.MANOR : Civ.Kind.HOUSE;
      if (farms == 0) {
         return Civ.Kind.FARM;
      } else if (homes < 3) {
         return home;
      } else if (vill >= cap(c) - 1 && homes < 160) {
         return home;
      } else if (farms < 1 + homes / 3) {
         return Civ.Kind.FARM;
      } else if (well == 0 && homes >= 2) {
         return Civ.Kind.WELL;
      } else if (bar == 0 && homes >= 3) {
         return Civ.Kind.BARRACKS;
      } else if (gran == 0 && homes >= 4) {
         return Civ.Kind.GRANARY;
      } else {
         int wh25 = c.count(Civ.Kind.WAREHOUSE);
         if (wh25 == 0 && homes >= 5) {
            return Civ.Kind.WAREHOUSE;
         } else if (wh25 < 5 && c.wood + c.stone + c.food + c.ore > Storage.cap(c) * 3) {
            return Civ.Kind.WAREHOUSE;
         } else if (smithy == 0 && homes >= 4 && bar > 0 && c.era >= 3) {
            return Civ.Kind.SMITHY;
         } else if (mill == 0 && farms >= 2) {
            return Civ.Kind.MILL;
         } else if (tow < Math.min(14, homes / 3)) {
            return Civ.Kind.TOWER;
         } else if (temple == 0 && homes >= 7) {
            return Civ.Kind.TEMPLE;
         } else if (well < 1 + homes / 8) {
            return Civ.Kind.WELL;
         } else if (bar < Math.min(10, 1 + homes / 6)) {
            return Civ.Kind.BARRACKS;
         } else {
            return homes < 160 ? home : null;
         }
      }
   }

   static String extra(Civ.City c) {
      return ", больших домов "
         + c.count(Civ.Kind.MANOR)
         + ", колодцев "
         + c.count(Civ.Kind.WELL)
         + ", мельниц "
         + c.count(Civ.Kind.MILL)
         + ", кузниц "
         + c.count(Civ.Kind.SMITHY)
         + ", амбаров "
         + c.count(Civ.Kind.GRANARY)
         + ", складов "
         + c.count(Civ.Kind.WAREHOUSE)
         + ", храмов "
         + c.count(Civ.Kind.TEMPLE);
   }

   static void hop(VillagerEntity v) {
      if (v.isOnGround()) {
         v.addVelocity(0.0, 0.4, 0.0);
         v.velocityModified = true;
      }
   }

   static void bob(VillagerEntity v) {
      if (v.isOnGround()) {
         v.addVelocity(0.0, 0.2, 0.0);
         v.velocityModified = true;
      }

      v.swingHand(net.minecraft.util.Hand.MAIN_HAND);
   }

   static void workFx(VillagerEntity v, int work) {
      if (work % 4 == 0) {
         bob(v);
      }
   }

   static void onFinish(Kingdoms.K k, Civ.City c, ServerWorld w, Civ.Building b) {
      double x = b.cx() + 0.5;
      double y = b.y0 + 4;
      double z = b.cz() + 0.5;
      Fx.particles(w, ParticleTypes.FIREWORK, x, y + 2.0, z, 40, 1.5, 1.5, 1.5, 0.15);
      Fx.sound(w, x, y, z, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.2F, 1.0F);
      Fx.sound(w, k.capital.getX() + 0.5, k.capital.getY() + 1, k.capital.getZ() + 0.5, SoundEvents.BLOCK_BELL_USE, 1.5F, 1.0F);

      for (MobEntity m : k.members) {
         if (m instanceof VillagerEntity v && v.squaredDistanceTo(x, y, z) < 400.0) {
            hop(v);
            Fx.particles(w, ParticleTypes.HAPPY_VILLAGER, v.getX(), v.getY() + 1.8, v.getZ(), 3, 0.3, 0.3, 0.3, 0.02);
         }
      }

      if (b.kind == Civ.Kind.TEMPLE) {
         Fx.particles(w, ParticleTypes.END_ROD, x, b.y0 + 9, z, 60, 1.0, 3.0, 1.0, 0.08);
      }
   }

   static void leaves(ServerWorld w, List<BlockPos> logs) {
      int n = 0;

      for (BlockPos l : logs) {
         for (int dx = -2; dx <= 2 && n < 140; dx++) {
            for (int dy = -1; dy <= 2 && n < 140; dy++) {
               for (int dz = -2; dz <= 2 && n < 140; dz++) {
                  BlockPos q = l.add(dx, dy, dz);
                  if (w.getBlockState(q).isIn(BlockTags.LEAVES)) {
                     if (n % 5 == 0) {
                        w.breakBlock(q, false);
                     } else {
                        w.setBlockState(q, Blocks.AIR.getDefaultState(), 3);
                     }

                     n++;
                  }
               }
            }
         }
      }
   }

   static void tickCity(Kingdoms.K k, Civ.City c, ServerWorld w, int timer) {
      Random r = w.getRandom();
      if (timer % 20 == 0) {
         int phase = timer / 20 % 2;

         for (Civ.Building b : c.buildings) {
            if (b.done && b.kind == Civ.Kind.MILL) {
               spin(w, b, phase);
            }
         }
      }

      if (!c.water && timer % 600 == 0) {
         c.water = true;
      }

      if (c.fireAt == null && timer % 100 == 0) {
         placeFire(k, c, w);
      }

      if (timer % 100 == 0) {
         for (Civ.Building bx : c.buildings) {
            if (bx.done) {
               double x = bx.cx() + 0.5;
               double z = bx.cz() + 0.5;
               switch (bx.kind) {
                  case TEMPLE:
                     Fx.particles(w, ParticleTypes.END_ROD, x, bx.y0 + 9.5, z, 8, 0.3, 1.5, 0.3, 0.02);

                     for (MobEntity m : k.members) {
                        if (m.isAlive() && m.getHealth() < m.getMaxHealth() && m.squaredDistanceTo(x, bx.y0, z) < 576.0) {
                           m.heal(2.0F);
                           Fx.particles(w, ParticleTypes.HEART, m.getX(), m.getY() + m.getHeight() + 0.3, m.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
                        }
                     }
                     break;
                  case SMITHY:
                     Fx.sound(w, x, bx.y0 + 1, z, SoundEvents.BLOCK_ANVIL_USE, 0.45F, 1.2F + r.nextFloat() * 0.3F);
                     Fx.particles(w, ParticleTypes.CRIT, bx.x0 + 1.5, bx.y0 + 1.1, bx.z1 - 0.5, 8, 0.2, 0.2, 0.2, 0.2);
                     break;
                  case WELL:
                     Fx.particles(w, ParticleTypes.SPLASH, x, bx.y0 + 1, z, 6, 0.2, 0.1, 0.2, 0.1);
                     break;
                  case MILL:
                     Fx.sound(w, x, bx.y0 + 6, bx.z0 - 0.5, SoundEvents.BLOCK_GRINDSTONE_USE, 0.25F, 0.6F);
               }
            }
         }
      }
   }

   static void placeFire(Kingdoms.K k, Civ.City c, ServerWorld w) {
      BlockPos g = Civ.ground(w, k.capital.getX(), k.capital.getZ() + 8);
      c.fireAt = g;
      BlockState at = w.getBlockState(g);
      if (!Civ.inCity(g.getX(), g.getZ(), 1)) {
         if (Civ.solid(w, g.down()) && at.getFluidState().isEmpty() && (at.isAir() || at.isReplaceable())) {
            w.setBlockState(g, Blocks.CAMPFIRE.getDefaultState());
            int[][] seats = new int[][]{{2, 0}, {-2, 0}, {0, 2}};

            for (int[] s : seats) {
               BlockPos q = Civ.ground(w, g.getX() + s[0], g.getZ() + s[1]);
               BlockState qs = w.getBlockState(q);
               if ((qs.isAir() || qs.isReplaceable()) && qs.getFluidState().isEmpty() && Civ.solid(w, q.down())) {
                  w.setBlockState(q, Blocks.SPRUCE_SLAB.getDefaultState());
               }
            }
         }
      }
   }

   static List<BlockPos> blades(Civ.Building b, int phase) {
      BlockPos h = new BlockPos(b.cx(), b.y0 + 6, b.z0 - 1);
      List<BlockPos> l = new ArrayList<>();
      if (phase == 0) {
         for (int i = 1; i <= 3; i++) {
            l.add(h.add(i, 0, 0));
            l.add(h.add(-i, 0, 0));
            l.add(h.up(i));
            l.add(h.down(i));
         }
      } else {
         for (int i = 1; i <= 2; i++) {
            l.add(h.add(i, i, 0));
            l.add(h.add(-i, i, 0));
            l.add(h.add(i, -i, 0));
            l.add(h.add(-i, -i, 0));
         }
      }

      return l;
   }

   static void spin(ServerWorld w, Civ.Building b, int phase) {
      BlockPos hub = new BlockPos(b.cx(), b.y0 + 6, b.z0 - 1);
      if (w.getBlockState(hub).isOf(Blocks.OAK_LOG)) {
         for (BlockPos q : blades(b, 1 - phase)) {
            if (w.getBlockState(q).isOf(Blocks.WHITE_WOOL)) {
               w.setBlockState(q, Blocks.AIR.getDefaultState(), 2);
            }
         }

         for (BlockPos qx : blades(b, phase)) {
            if (w.getBlockState(qx).isAir()) {
               w.setBlockState(qx, Blocks.WHITE_WOOL.getDefaultState(), 2);
            }
         }
      }
   }

   static boolean night(Kingdoms.K k, Civ.City c, ServerWorld w, VillagerEntity v, Civ.W s) {
      long t = w.getTimeOfDay() % 24000L;
      boolean n = t > 13500L && t < 22500L;
      if (!n) {
         Sleep.wake(v);
         if (s.rest != null) {
            s.rest = null;
            s.state = 0;
            s.best = 1.0E9;
            s.stuck = 0;
         }

         return false;
      } else {
         Random r = w.getRandom();
         if (s.rest == null) {
            if (s.target != null) {
               w.setBlockBreakingInfo(v.getId(), s.target, -1);
            }

            s.target = null;
            s.state = 0;
            List<Civ.Building> homes = new ArrayList<>();

            for (Civ.Building b : c.buildings) {
               if (b.done && (b.kind == Civ.Kind.HOUSE || b.kind == Civ.Kind.MANOR)) {
                  homes.add(b);
               }
            }

            BlockPos bedPos = Sleep.find(k, homes, w, v);
            if (bedPos != null) {
               s.rest = bedPos;
            } else if (!homes.isEmpty() && r.nextInt(3) != 0) {
               s.rest = Civ.front(k, homes.get(r.nextInt(homes.size())), w);
            } else {
               BlockPos f = c.fireAt != null ? c.fireAt : k.capital.add(0, 0, 8);
               double a = r.nextDouble() * Math.PI * 2.0;
               s.rest = Civ.ground(w, f.getX() + (int)Math.round(Math.cos(a) * 3.0), f.getZ() + (int)Math.round(Math.sin(a) * 3.0));
            }
         }

         if (Sleep.night(w, v, s.rest)) {
            return true;
         } else {
            double d = Civ.hdist(v, s.rest);
            if (d > 2.5) {
               Civ.walk(v, s.rest);
            } else {
               Civ.halt(v);
               if (r.nextInt(30) == 0) {
                  Fx.particles(w, ParticleTypes.NOTE, v.getX(), v.getY() + 2.2, v.getZ(), 1, 0.2, 0.1, 0.2, 0.5);
               }
            }

            return true;
         }
      }
   }

   static BlockPos depot(Kingdoms.K k, Civ.City c, ServerWorld w, VillagerEntity v) {
      BlockPos best = k.capital;
      double bd = Civ.hdist(v, best);

      for (Civ.Building b : c.buildings) {
         if (b.done && (b.kind == Civ.Kind.GRANARY || b.kind == Civ.Kind.WAREHOUSE)) {
            BlockPos f = Civ.front(k, b, w);
            double d = Civ.hdist(v, f);
            if (d < bd) {
               bd = d;
               best = f;
            }
         }
      }

      return best;
   }

   static void arm(Civ.City c, MobEntity m, ServerWorld w) {
      if (m != null && c.count(Civ.Kind.SMITHY) != 0 && c.ore >= 1) {
         c.ore--;
         if (m.getType() != EntityType.PILLAGER) {
            m.equipStack(EquipmentSlot.MAINHAND, new ItemStack(w.getRandom().nextBoolean() ? Items.IRON_SWORD : Items.IRON_AXE));
            m.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
         }

         m.equipStack(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
         m.setEquipmentDropChance(EquipmentSlot.FEET, 0.0F);
         Fx.sound(w, m.getX(), m.getY(), m.getZ(), SoundEvents.BLOCK_ANVIL_USE, 0.6F, 1.1F);
      }
   }

   static BlockPos findWater(Kingdoms.K k, Civ.City c, ServerWorld w, VillagerEntity v) {
      Random r = w.getRandom();
      int R = Civ.radius(c) + 10;
      BlockPos from = v != null ? v.getBlockPos() : k.capital;
      BlockPos best = null;
      double bd = Double.MAX_VALUE;

      for (int i = 0; i < 48; i++) {
         int x = Conquest.cx(k, r) + r.nextInt(R * 2 + 1) - R;
         int z = Conquest.cz(k) + r.nextInt(R * 2 + 1) - R;
         if (!Civ.inCity(x, z, 0)) {
            BlockPos top = w.getTopPosition(Type.WORLD_SURFACE, new BlockPos(x, 0, z)).down();
            if (w.getFluidState(top).isIn(FluidTags.WATER) && w.getBlockState(top.up()).isAir() && !Civ.claimed(top) && shore(w, top) != null) {
               double d = top.getSquaredDistance(from);
               if (d < bd) {
                  bd = d;
                  best = top.toImmutable();
               }
            }
         }
      }

      c.water = best != null;
      return best;
   }

   static BlockPos shore(ServerWorld w, BlockPos t) {
      for (int d = 1; d <= 3; d++) {
         for (Direction dir : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
            BlockPos o = t.offset(dir, d);
            BlockPos g = Civ.ground(w, o.getX(), o.getZ());
            BlockState below = w.getBlockState(g.down());
            if (below.getFluidState().isEmpty() && Civ.solid(w, g.down()) && Math.abs(g.getY() - t.getY() - 1) <= 2) {
               return g;
            }
         }
      }

      return null;
   }

   static void fish(Kingdoms.K k, Civ.City c, ServerWorld w, VillagerEntity v, Civ.W s) {
      BlockPos t = s.target;
      if (t != null && w.getFluidState(t).isIn(FluidTags.WATER)) {
         s.work++;
         Random r = w.getRandom();
         double x = t.getX() + 0.5;
         double y = t.getY() + 1.0;
         double z = t.getZ() + 0.5;
         if (s.work == 1) {
            Fx.sound(w, x, y, z, SoundEvents.ENTITY_FISHING_BOBBER_SPLASH, 0.4F, 1.4F);
         }

         Fx.particles(w, ParticleTypes.FISHING, x, y, z, 1, 0.05, 0.0, 0.05, 0.0);
         if (s.work >= 18) {
            s.work = 0;
            Fx.particles(w, ParticleTypes.SPLASH, x, y, z, 12, 0.3, 0.1, 0.3, 0.1);
            Fx.sound(w, x, y, z, SoundEvents.ENTITY_FISHING_BOBBER_SPLASH, 0.8F, 1.0F);
            Fx.particles(w, new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(Items.COD)), x, y + 0.3, z, 5, 0.2, 0.2, 0.2, 0.1);
            Fx.sound(w, v.getX(), v.getY(), v.getZ(), SoundEvents.ENTITY_VILLAGER_WORK_FISHERMAN, 0.7F, 1.0F);
            hop(v);
            s.bag[2] = s.bag[2] + 2 + (r.nextInt(3) == 0 ? 1 : 0);
            if (s.bag[2] >= 5) {
               s.state = 3;
               s.target = null;
               Civ.hold(v, new ItemStack(r.nextBoolean() ? Items.COD : Items.SALMON));
            }
         }
      } else {
         s.state = 0;
         s.target = null;
      }
   }

   static BlockState st(Block b) {
      return b.getDefaultState();
   }

   static BlockState[] houseStyle(Civ.Building b) {
      int i = Math.floorMod(b.x0 * 31 + b.z0 * 17, 4);

      return switch (i) {
         case 0 -> new BlockState[]{st(Blocks.OAK_LOG), st(Blocks.OAK_PLANKS), st(Blocks.DARK_OAK_PLANKS), st(Blocks.SPRUCE_SLAB)};
         case 1 -> new BlockState[]{st(Blocks.SPRUCE_LOG), st(Blocks.SPRUCE_PLANKS), st(Blocks.DARK_OAK_PLANKS), st(Blocks.DARK_OAK_SLAB)};
         case 2 -> new BlockState[]{st(Blocks.BIRCH_LOG), st(Blocks.BIRCH_PLANKS), st(Blocks.SPRUCE_PLANKS), st(Blocks.SPRUCE_SLAB)};
         default -> new BlockState[]{st(Blocks.SPRUCE_LOG), st(Blocks.COBBLESTONE), st(Blocks.BRICKS), st(Blocks.BRICK_SLAB)};
      };
   }

   static BlockState hanging() {
      return Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true);
   }

   static BlockState fire() {
      return Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.SIGNAL_FIRE, true);
   }

   static void walls(
      Civ.Project p,
      int x0,
      int z0,
      int x1,
      int z1,
      int ya,
      int yb,
      BlockState wall,
      BlockState corner,
      BlockState win,
      int winLo,
      int winHi,
      int doorX,
      int doorZ,
      int doorTop
   ) {
      for (int y = ya; y <= yb; y++) {
         for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
               boolean ex = x == x0 || x == x1;
               boolean ez = z == z0 || z == z1;
               if ((ex || ez) && (x != doorX || z != doorZ || y > doorTop)) {
                  BlockState s = wall;
                  if (ex && ez) {
                     s = corner;
                  } else if (win != null && y >= winLo && y <= winHi && Math.floorMod(ez ? x - x0 : z - z0, 2) == 1) {
                     s = win;
                  }

                  p.add(new BlockPos(x, y, z), s);
               }
            }
         }
      }
   }

   static void rect(Civ.Project p, int x0, int z0, int x1, int z1, int y, BlockState s) {
      for (int x = x0; x <= x1; x++) {
         for (int z = z0; z <= z1; z++) {
            p.add(new BlockPos(x, y, z), s);
         }
      }
   }

   static void put(Civ.Project p, int x, int y, int z, BlockState s) {
      p.add(new BlockPos(x, y, z), s);
   }

   static void extras(Kingdoms.K k, Civ.Building b, ServerWorld w, Civ.Project p, int doorX, int doorZ) {
      int y0 = b.y0;
      int x0 = b.x0;
      int z0 = b.z0;
      int x1 = b.x1;
      int z1 = b.z1;
      int cx = b.cx();
      int cz = b.cz();
      BlockState glass = st(Blocks.GLASS);
      switch (b.kind) {
         case TEMPLE:
            walls(p, x0, z0, x1, z1, y0, y0 + 4, st(Blocks.QUARTZ_BLOCK), st(Blocks.CHISELED_STONE_BRICKS), glass, y0 + 2, y0 + 3, doorX, doorZ, y0 + 2);
            rect(p, x0, z0, x1, z1, y0 + 5, st(Blocks.STONE_BRICKS));
            rect(p, x0 + 1, z0 + 1, x1 - 1, z1 - 1, y0 + 6, st(Blocks.QUARTZ_BLOCK));
            rect(p, x0 + 2, z0 + 2, x1 - 2, z1 - 2, y0 + 7, st(Blocks.QUARTZ_BLOCK));
            put(p, cx, y0 + 8, cz, st(Blocks.GOLD_BLOCK));
            put(p, cx, y0 + 9, cz, st(Blocks.END_ROD));
            put(p, cx, y0, cz, st(Blocks.ENCHANTING_TABLE));
            int[][] cs = new int[][]{{x0 + 1, z0 + 1}, {x1 - 1, z0 + 1}, {x0 + 1, z1 - 1}, {x1 - 1, z1 - 1}};

            for (int[] c : cs) {
               put(p, c[0], y0, c[1], st(Blocks.BOOKSHELF));
               put(p, c[0], y0 + 1, c[1], st(Blocks.BOOKSHELF));
            }

            int[][] ls = new int[][]{{x0 + 2, z0 + 2}, {x1 - 2, z0 + 2}, {x0 + 2, z1 - 2}, {x1 - 2, z1 - 2}};

            for (int[] c : ls) {
               put(p, c[0], y0 + 4, c[1], hanging());
            }
            break;
         case SMITHY:
            walls(p, x0, z0, x1, z1, y0, y0 + 2, st(Blocks.COBBLESTONE), st(Blocks.STONE_BRICKS), st(Blocks.IRON_BARS), y0 + 1, y0 + 1, doorX, doorZ, y0 + 1);
            rect(p, x0, z0, x1, z1, y0 + 3, st(Blocks.STONE_BRICK_SLAB));
            put(p, x1, y0 + 3, z1, st(Blocks.BRICKS));
            put(p, x1, y0 + 4, z1, st(Blocks.BRICKS));
            put(p, x1, y0 + 5, z1, fire());
            put(p, x0 + 1, y0, z1 - 1, st(Blocks.ANVIL));
            put(p, x1 - 1, y0, z1 - 1, st(Blocks.BLAST_FURNACE));
            put(p, x1 - 1, y0, z0 + 1, st(Blocks.LAVA_CAULDRON));
            put(p, x0 + 1, y0, z0 + 1, st(Blocks.SMITHING_TABLE));
            break;
         case WELL:
            for (int x = x0; x <= x1; x++) {
               for (int z = z0; z <= z1; z++) {
                  if (x != cx || z != cz) {
                     put(p, x, y0, z, (x + z & 1) == 0 ? st(Blocks.MOSSY_COBBLESTONE) : st(Blocks.COBBLESTONE));
                  }
               }
            }

            put(p, cx, y0 - 1, cz, st(Blocks.WATER));
            put(p, cx, y0, cz, st(Blocks.WATER));
            int[][] posts = new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}};

            for (int[] c : posts) {
               put(p, c[0], y0 + 1, c[1], st(Blocks.OAK_FENCE));
               put(p, c[0], y0 + 2, c[1], st(Blocks.OAK_FENCE));
            }

            rect(p, x0, z0, x1, z1, y0 + 3, st(Blocks.OAK_SLAB));
            put(p, cx, y0 + 2, cz, hanging());
            break;
         case MILL:
            walls(p, x0, z0, x1, z1, y0, y0 + 3, st(Blocks.COBBLESTONE), st(Blocks.STONE_BRICKS), glass, y0 + 2, y0 + 2, doorX, doorZ, y0 + 1);
            rect(p, x0, z0, x1, z1, y0 + 4, st(Blocks.SPRUCE_PLANKS));
            walls(
               p,
               x0 + 1,
               z0 + 1,
               x1 - 1,
               z1 - 1,
               y0 + 5,
               y0 + 7,
               st(Blocks.OAK_PLANKS),
               st(Blocks.OAK_LOG),
               null,
               0,
               0,
               Integer.MIN_VALUE,
               Integer.MIN_VALUE,
               0
            );
            rect(p, x0 + 1, z0 + 1, x1 - 1, z1 - 1, y0 + 8, st(Blocks.DARK_OAK_PLANKS));
            put(p, cx, y0 + 9, cz, k.wool.getDefaultState());
            put(p, cx, y0 + 6, z0, st(Blocks.OAK_LOG));
            put(p, cx, y0 + 6, z0 - 1, st(Blocks.OAK_LOG));

            for (BlockPos q : blades(b, 0)) {
               p.add(q, st(Blocks.WHITE_WOOL));
            }

            put(p, x0 + 1, y0, z1 - 1, st(Blocks.HAY_BLOCK));
            put(p, x1 - 1, y0, z1 - 1, st(Blocks.HAY_BLOCK));
            put(p, x1 - 1, y0, z0 + 1, st(Blocks.BARREL));
            put(p, cx, y0 + 3, cz, hanging());
            break;
         case HOUSE:
            put(p, cx, y0 + 2, cz, hanging());
            break;
         case GRANARY:
            walls(p, x0, z0, x1, z1, y0, y0 + 2, st(Blocks.OAK_PLANKS), st(Blocks.STRIPPED_OAK_LOG), st(Blocks.OAK_FENCE), y0 + 1, y0 + 1, doorX, doorZ, y0 + 1);
            rect(p, x0, z0, x1, z1, y0 + 3, st(Blocks.SPRUCE_PLANKS));
            rect(p, x0 + 1, z0 + 1, x1 - 1, z1 - 1, y0 + 4, st(Blocks.HAY_BLOCK));
            put(p, cx, y0 + 5, cz, st(Blocks.HAY_BLOCK));
            put(p, x0 + 1, y0, z0 + 1, st(Blocks.HAY_BLOCK));
            put(p, x0 + 1, y0 + 1, z0 + 1, st(Blocks.HAY_BLOCK));
            put(p, x1 - 1, y0, z0 + 1, st(Blocks.BARREL));
            put(p, x1 - 1, y0, z1 - 1, st(Blocks.CHEST));
            put(p, x0 + 1, y0, z1 - 1, st(Blocks.COMPOSTER));
            put(p, cx, y0 + 2, cz, hanging());
            break;
         case WAREHOUSE:
            walls(p, x0, z0, x1, z1, y0, y0, st(Blocks.STONE_BRICKS), st(Blocks.STRIPPED_SPRUCE_LOG), null, 0, 0, doorX, doorZ, y0 + 1);
            walls(
               p,
               x0,
               z0,
               x1,
               z1,
               y0 + 1,
               y0 + 3,
               st(Blocks.SPRUCE_PLANKS),
               st(Blocks.STRIPPED_SPRUCE_LOG),
               st(Blocks.SPRUCE_FENCE),
               y0 + 2,
               y0 + 2,
               doorX,
               doorZ,
               y0 + 1
            );
            rect(p, x0 - 1, z0 - 1, x1 + 1, z1 + 1, y0 + 4, st(Blocks.DARK_OAK_SLAB));
            rect(p, x0, z0, x1, z1, y0 + 4, st(Blocks.DARK_OAK_PLANKS));
            rect(p, x0 + 1, z0 + 1, x1 - 1, z1 - 1, y0 + 5, st(Blocks.DARK_OAK_PLANKS));
            rect(p, x0 + 2, cz, x1 - 2, cz, y0 + 6, st(Blocks.DARK_OAK_SLAB));

            for (int xi = x0 + 1; xi <= x1 - 1; xi++) {
               for (int zi = z0 + 1; zi <= z1 - 1; zi++) {
                  if (Math.abs(xi - doorX) + Math.abs(zi - doorZ) > 1 && (zi != cz || xi <= x0 + 1 || xi >= x1 - 1)) {
                     if ((xi + zi & 1) == 0) {
                        put(p, xi, y0, zi, st(Blocks.CHEST));
                     } else {
                        put(p, xi, y0, zi, st(Blocks.BARREL));
                        put(p, xi, y0 + 1, zi, st(Blocks.BARREL));
                     }
                  }
               }
            }

            put(p, cx, y0 + 3, cz, hanging());
            break;
         case MANOR:
            if (b.rev >= 2) {
               manor(k, b, p, doorX, doorZ);
            } else {
               BlockState[] sty = houseStyle(b);
               walls(p, x0, z0, x1, z1, y0, y0 + 2, sty[1], sty[0], glass, y0 + 1, y0 + 1, doorX, doorZ, y0 + 1);
               rect(p, x0, z0, x1, z1, y0 + 3, st(Blocks.SPRUCE_PLANKS));
               walls(p, x0, z0, x1, z1, y0 + 4, y0 + 5, sty[1], sty[0], glass, y0 + 4, y0 + 4, Integer.MIN_VALUE, Integer.MIN_VALUE, 0);

               for (int x = x0 - 1; x <= x1 + 1; x++) {
                  for (int z = z0 - 1; z <= z1 + 1; z++) {
                     boolean in = x >= x0 && x <= x1 && z >= z0 && z <= z1;
                     put(p, x, y0 + 6, z, in ? sty[2] : sty[3]);
                  }
               }

               rect(p, x0 + 1, z0 + 1, x1 - 1, z1 - 1, y0 + 7, sty[2]);
               rect(p, x0 + 2, cz, x1 - 2, cz, y0 + 8, sty[2]);
               put(p, cx, y0 + 9, cz, k.wool.getDefaultState());
               put(p, x1 - 1, y0 + 8, z1 - 1, fire());
               put(p, x0 + 1, y0, z0 + 1, st(Blocks.CRAFTING_TABLE));
               put(p, x1 - 1, y0, z0 + 1, st(Blocks.BARREL));
               put(p, x0 + 1, y0, z1 - 1, st(Blocks.BOOKSHELF));
               put(p, x1 - 1, y0, z1 - 1, st(Blocks.LOOM));
               put(p, cx, y0 + 2, cz, hanging());
               put(p, cx, y0 + 5, cz, hanging());
               put(p, x0 + 1, y0 + 4, z0 + 1, st(Blocks.BOOKSHELF));
            }
      }

      road(k, b, w, p);
   }

   private static void manor(Kingdoms.K k, Civ.Building b, Civ.Project p, int doorX, int doorZ) {
      int y0 = b.y0;
      int x0 = b.x0;
      int z0 = b.z0;
      int x1 = b.x1;
      int z1 = b.z1;
      int cx = b.cx();
      int cz = b.cz();
      BlockState[] sty = houseStyle(b);
      BlockState glass = st(Blocks.GLASS);
      BlockState roof = Eras.style(p.era, Civ.Kind.MANOR, sty[2]);
      walls(p, x0, z0, x1, z1, y0, y0 + 2, sty[1], sty[0], glass, y0 + 1, y0 + 1, doorX, doorZ, y0 + 1);
      Direction out = doorX == x1 ? Direction.EAST : (doorX == x0 ? Direction.WEST : (doorZ == z1 ? Direction.SOUTH : Direction.NORTH));
      Arch.door(p::add, new BlockPos(doorX, y0, doorZ), out, Arch.woodenDoor(sty[1]));
      boolean doorOnX = doorX == x0 || doorX == x1;
      int lx = doorOnX ? (doorX == x0 ? x1 - 1 : x0 + 1) : x0 + 2;
      int lz = doorOnX ? cz : (doorZ == z0 ? z1 - 1 : z0 + 1);
      Direction face = doorOnX ? (doorX == x0 ? Direction.WEST : Direction.EAST) : (doorZ == z0 ? Direction.NORTH : Direction.SOUTH);
      floorBand(p, x0, z0, x1, z1, y0 + 3, sty[0], sty[1], lx, lz);
      walls(p, x0, z0, x1, z1, y0 + 4, y0 + 6, sty[1], sty[0], glass, y0 + 5, y0 + 5, Integer.MIN_VALUE, Integer.MIN_VALUE, 0);

      for (int y = y0; y <= y0 + 4; y++) {
         put(p, lx, y, lz, Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, face));
      }

      floorBand(p, x0, z0, x1, z1, y0 + 7, sty[0], sty[1], Integer.MIN_VALUE, Integer.MIN_VALUE);
      Arch.gable(p::add, x0, z0, x1, z1, y0 + 7, roof, sty[1], roof);
      int top = y0 + 7 + Arch.gableHeight(x0, z0, x1, z1);
      put(p, cx, top, cz, k.wool.getDefaultState());
      Arch.chimney(p::add, x1 - 1, z1 - 1, y0 + 8, top, st(Blocks.BRICKS));
      put(p, x0 + 1, y0, z0 + 1, st(Blocks.CRAFTING_TABLE));
      put(p, x1 - 1, y0, z0 + 1, st(Blocks.BARREL));
      put(p, x0 + 1, y0, z1 - 1, st(Blocks.BOOKSHELF));
      put(p, x1 - 1, y0, z1 - 1, st(Blocks.LOOM));
      put(p, cx, y0 + 2, cz, hanging());
      put(p, cx, y0 + 6, cz, hanging());
      put(p, x0 + 1, y0 + 4, z0 + 1, st(Blocks.BOOKSHELF));
   }

   private static void floorBand(Civ.Project p, int x0, int z0, int x1, int z1, int y, BlockState log, BlockState plank, int hx, int hz) {
      for (int x = x0; x <= x1; x++) {
         for (int z = z0; z <= z1; z++) {
            if (x != hx || z != hz) {
               boolean ex = x == x0 || x == x1;
               boolean ez = z == z0 || z == z1;
               BlockState s = ex && ez ? log : (ez ? Arch.beam(log, Axis.X) : (ex ? Arch.beam(log, Axis.Z) : plank));
               p.add(new BlockPos(x, y, z), s);
            }
         }
      }
   }

   private static int street(int a, int dir) {
      int m = Math.floorMod(a - 5, 11);
      if (m == 0) {
         return a;
      } else {
         return dir > 0 ? a + (11 - m) : a - m;
      }
   }

   static void road(Kingdoms.K k, Civ.Building b, ServerWorld w, Civ.Project p) {
      if (b.kind != Civ.Kind.QUARRY) {
         BlockPos s = Civ.front(k, b, w);
         int tx = k.capital.getX();
         int tz = k.capital.getZ();
         int[] pos = new int[]{s.getX(), s.getZ()};
         int[] budget = new int[]{170};
         boolean doorX = Math.abs(tx - b.cx()) >= Math.abs(tz - b.cz());
         if (doorX) {
            leg(w, p, pos, tx + street(pos[0] - tx, tx > pos[0] ? 1 : -1), pos[1], tx, tz, budget);
            leg(w, p, pos, pos[0], pos[1] >= tz ? tz + 5 : tz - 6, tx, tz, budget);
            leg(w, p, pos, tx, pos[1], tx, tz, budget);
         } else {
            leg(w, p, pos, pos[0], tz + street(pos[1] - tz, tz > pos[1] ? 1 : -1), tx, tz, budget);
            leg(w, p, pos, pos[0] >= tx ? tx + 5 : tx - 6, pos[1], tx, tz, budget);
            leg(w, p, pos, pos[0], tz, tx, tz, budget);
         }
      }
   }

   private static void leg(ServerWorld w, Civ.Project p, int[] pos, int ex, int ez, int tx, int tz, int[] budget) {
      boolean alongX = pos[0] != ex;

      while ((pos[0] != ex || pos[1] != ez) && budget[0]-- > 0) {
         int x = pos[0];
         int z = pos[1];
         if (Math.abs(x - tx) <= 5 && Math.abs(z - tz) <= 5) {
            budget[0] = 0;
            return;
         }

         if (!Civ.inCity(x, z, 0)) {
            pave(w, p, x, z);
         }

         int ox = alongX ? x : x + 1;
         int oz = alongX ? z + 1 : z;
         if (!Civ.inCity(ox, oz, 0)) {
            pave(w, p, ox, oz);
         }

         if (budget[0] % 9 == 4) {
            lamp(w, p, alongX ? x : x - 1, alongX ? z - 1 : z);
         }

         if (alongX) {
            pos[0] += Integer.signum(ex - x);
         } else {
            pos[1] += Integer.signum(ez - z);
         }
      }
   }

   static void pave(ServerWorld w, Civ.Project p, int x, int z) {
      if (!p.b.done || w.isChunkLoaded(x >> 4, z >> 4)) {
         BlockPos g = Civ.ground(w, x, z);
         BlockState below = w.getBlockState(g.down());
         if (below.isOf(Blocks.GRASS_BLOCK)
            || below.isOf(Blocks.DIRT)
            || below.isOf(Blocks.COARSE_DIRT)
            || below.isOf(Blocks.PODZOL)
            || below.isOf(Blocks.MYCELIUM)
            || Decor.isPathBlock(below)) {
            BlockState at = w.getBlockState(g);
            if (!at.isAir() && at.isReplaceable() && at.getFluidState().isEmpty()) {
               p.add(g, Blocks.AIR.getDefaultState());
            }

            p.add(g.down(), Decor.pathFor(p.era));
            p.raw.add(g.down());
         } else if (below.isOf(Blocks.SAND) || below.isOf(Blocks.GRAVEL)) {
            p.add(g.down(), st(Blocks.SMOOTH_SANDSTONE));
         }
      }
   }

   static void lamp(ServerWorld w, Civ.Project p, int x, int z) {
      if (!p.b.done || w.isChunkLoaded(x >> 4, z >> 4)) {
         if (!Civ.inCity(x, z, 0)) {
            BlockPos g = Civ.ground(w, x, z);
            BlockState at = w.getBlockState(g);
            if ((at.isAir() || at.isReplaceable()) && at.getFluidState().isEmpty()) {
               if (Civ.solid(w, g.down())) {
                  p.add(g, st(Blocks.OAK_FENCE));
                  p.add(g.up(), st(Blocks.OAK_FENCE));
                  p.add(g.up(2), st(Blocks.LANTERN));
               }
            }
         }
      }
   }
}
