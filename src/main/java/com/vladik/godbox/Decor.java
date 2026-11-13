package com.vladik.godbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CarvedPumpkinBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.util.math.random.Random;

final class Decor {
   static boolean on = true;
   static int timer;
   private static final Map<Civ.Building, Integer> PAVED = new WeakHashMap<>();
   private static final Block[] FLOWERS = new Block[]{
      Blocks.POPPY,
      Blocks.DANDELION,
      Blocks.CORNFLOWER,
      Blocks.ALLIUM,
      Blocks.AZURE_BLUET,
      Blocks.OXEYE_DAISY,
      Blocks.LILY_OF_THE_VALLEY,
      Blocks.RED_TULIP,
      Blocks.ORANGE_TULIP,
      Blocks.PINK_TULIP,
      Blocks.WHITE_TULIP,
      Blocks.BLUE_ORCHID
   };

   private Decor() {
   }

   static void clear() {
      PAVED.clear();
      timer = 0;
   }

   public static void tick(MinecraftServer server) {
      if (on && ++timer % 60 == 0) {
         for (Kingdoms.K k : Kingdoms.K.values()) {
            if (k.alive && k.world != null && k.capital != null && k.world.shouldTickEntity(k.capital)) {
               Civ.City c = Civ.city(k);
               if (c.buildings.size() >= 3 && k.world.getRandom().nextInt(2) == 0) {
                  decorate(k, c, k.world, 1);
               }
            }
         }
      }
   }

   static boolean loaded(ServerWorld w, Civ.Building b) {
      return w.isChunkLoaded(b.cx() >> 4, b.cz() >> 4);
   }

   static int decorate(Kingdoms.K k, Civ.City c, ServerWorld w, int times) {
      Random r = w.getRandom();
      List<Civ.Building> bs = new ArrayList<>();

      for (Civ.Building b : c.buildings) {
         if (b.done && b.kind != Civ.Kind.QUARRY && loaded(w, b)) {
            bs.add(b);
         }
      }

      if (bs.isEmpty()) {
         return 0;
      } else {
         int n = 0;

         for (int i = 0; i < times; i++) {
            Civ.Building bx = bs.get(r.nextInt(bs.size()));
            Integer pe = PAVED.get(bx);
            if (pe != null && pe == c.era) {
               BlockPos g = spot(k, c, bx, w, r);
               if (g != null && place(k, c, bx, w, g, r)) {
                  n++;
               }
            } else {
               PAVED.put(bx, c.era);
               if (pave(k, c, bx, w)) {
                  n++;
               }
            }
         }

         return n;
      }
   }

   static BlockState pathFor(int e) {
      if (e <= 2) {
         return Blocks.DIRT_PATH.getDefaultState();
      } else if (e <= 4) {
         return Blocks.GRAVEL.getDefaultState();
      } else if (e <= 6) {
         return Blocks.COBBLESTONE.getDefaultState();
      } else if (e <= 8) {
         return Blocks.STONE_BRICKS.getDefaultState();
      } else {
         return e == 9 ? Blocks.POLISHED_ANDESITE.getDefaultState() : Blocks.POLISHED_DEEPSLATE.getDefaultState();
      }
   }

   static boolean isPathBlock(BlockState s) {
      for (int e = 0; e <= 10; e++) {
         if (s.isOf(pathFor(e).getBlock())) {
            return true;
         }
      }

      return false;
   }

   static boolean soilOrPath(BlockState s) {
      return isPathBlock(s) || s.isIn(BlockTags.DIRT) || s.isOf(Blocks.SAND) || s.isOf(Blocks.RED_SAND) || s.isOf(Blocks.GRAVEL) || s.isOf(Blocks.DIRT_PATH);
   }

   static boolean pave(Kingdoms.K k, Civ.City c, Civ.Building b, ServerWorld w) {
      BlockPos f = Civ.front(k, b, w);
      int ddx = f.getX() - b.cx();
      int ddz = f.getZ() - b.cz();
      boolean ax = Math.abs(ddx) > Math.abs(ddz);
      int sign = ax ? Integer.signum(ddx) : Integer.signum(ddz);
      if (sign == 0) {
         return false;
      } else {
         int e = c.era;
         BlockState path = pathFor(e);
         int wide = e >= 3 ? 1 : 0;
         int placed = 0;

         for (int t = 1; t <= 3; t++) {
            for (int s = -wide; s <= wide; s++) {
               int x;
               int z;
               if (ax) {
                  x = (sign > 0 ? b.x1 : b.x0) + sign * t;
                  z = b.cz() + s;
               } else {
                  z = (sign > 0 ? b.z1 : b.z0) + sign * t;
                  x = b.cx() + s;
               }

               if (!Civ.inCity(x, z, 0)) {
                  BlockPos top = Civ.ground(w, x, z);
                  BlockPos under = top.down();
                  if (Math.abs(top.getY() - b.y0) <= 1) {
                     BlockState us = w.getBlockState(under);
                     if (soilOrPath(us)) {
                        BlockState ts = w.getBlockState(top);
                        if (!ts.isAir()) {
                           if (!ts.isReplaceable() || !ts.getFluidState().isEmpty()) {
                              continue;
                           }

                           w.setBlockState(top, Blocks.AIR.getDefaultState(), 3);
                        }

                        w.setBlockState(under, path, 3);
                        placed++;
                     }
                  }
               }
            }
         }

         return placed > 0;
      }
   }

   static boolean free(ServerWorld w, BlockPos q) {
      BlockState s = w.getBlockState(q);
      return (s.isAir() || s.isReplaceable()) && s.getFluidState().isEmpty();
   }

   static boolean isDecor(BlockState s) {
      return s.isIn(BlockTags.FENCES)
         || s.isIn(BlockTags.WALLS)
         || s.isIn(BlockTags.FLOWERS)
         || s.isIn(BlockTags.STAIRS)
         || s.isOf(Blocks.LANTERN)
         || s.isOf(Blocks.SOUL_LANTERN)
         || s.isOf(Blocks.TORCH)
         || s.isOf(Blocks.BARREL)
         || s.isOf(Blocks.HAY_BLOCK)
         || s.isOf(Blocks.COMPOSTER)
         || s.isOf(Blocks.AZALEA)
         || s.isOf(Blocks.FLOWERING_AZALEA)
         || s.isOf(Blocks.CARVED_PUMPKIN)
         || s.isOf(Blocks.OAK_LEAVES);
   }

   static boolean crowded(ServerWorld w, BlockPos g) {
      for (int dx = -2; dx <= 2; dx++) {
         for (int dz = -2; dz <= 2; dz++) {
            for (int dy = 0; dy <= 2; dy++) {
               if (isDecor(w.getBlockState(g.add(dx, dy, dz)))) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   static BlockPos spot(Kingdoms.K k, Civ.City c, Civ.Building b, ServerWorld w, Random r) {
      BlockPos door = Civ.front(k, b, w);
      int bw = b.x1 - b.x0 + 1;
      int bd = b.z1 - b.z0 + 1;

      for (int i = 0; i < 14; i++) {
         int d = 1 + r.nextInt(2);
         int x;
         int z;
         switch (r.nextInt(4)) {
            case 0:
               x = b.x0 + r.nextInt(bw);
               z = b.z0 - d;
               break;
            case 1:
               x = b.x0 + r.nextInt(bw);
               z = b.z1 + d;
               break;
            case 2:
               x = b.x0 - d;
               z = b.z0 + r.nextInt(bd);
               break;
            default:
               x = b.x1 + d;
               z = b.z0 + r.nextInt(bd);
         }

         if (Math.abs(door.getX() - x) + Math.abs(door.getZ() - z) > 2 && !Civ.inCity(x, z, 0)) {
            boolean near = false;

            for (Civ.Building o : c.buildings) {
               if (o != b && o.inside(x, z, 1)) {
                  near = true;
                  break;
               }
            }

            if (!near) {
               BlockPos g = Civ.ground(w, x, z);
               if (Math.abs(g.getY() - b.y0) <= 2) {
                  BlockState below = w.getBlockState(g.down());
                  if ((below.isIn(BlockTags.DIRT) || below.isOf(Blocks.SAND) || below.isOf(Blocks.RED_SAND) || below.isOf(Blocks.SNOW_BLOCK))
                     && free(w, g)
                     && w.getBlockState(g.up()).isAir()
                     && w.getBlockState(g.up(2)).isAir()
                     && !crowded(w, g)) {
                     return g;
                  }
               }
            }
         }
      }

      return null;
   }

   static Direction facing(Civ.Building b, BlockPos g) {
      if (g.getZ() < b.z0) {
         return Direction.SOUTH;
      } else if (g.getZ() > b.z1) {
         return Direction.NORTH;
      } else {
         return g.getX() < b.x0 ? Direction.EAST : Direction.WEST;
      }
   }

   static boolean place(Kingdoms.K k, Civ.City c, Civ.Building b, ServerWorld w, BlockPos g, Random r) {
      int e = c.era;
      boolean soil = w.getBlockState(g.down()).isIn(BlockTags.DIRT);
      Direction face = facing(b, g);
      int roll = r.nextInt(10);

      boolean ok = switch (b.kind) {
         case FARM -> roll < 4 ? hay(w, g, r) : (roll < 7 ? scarecrow(w, g, face) : set(w, g, Blocks.COMPOSTER.getDefaultState()));
         case BARRACKS, TOWER, CASTLE -> roll < 4 ? flag(k, w, g) : (roll < 8 ? lamp(w, g, e) : barrels(w, g, r));
         case WAREHOUSE, GRANARY, MILL, SMITHY -> roll < 5 ? barrels(w, g, r) : (roll < 7 ? hay(w, g, r) : lamp(w, g, e));
         case TEMPLE -> roll < 5 && soil ? flowers(w, g, r) : (roll < 7 && soil ? bush(w, g, face, r) : lamp(w, g, e));
         default -> roll < 4 && soil
            ? flowers(w, g, r)
            : (roll < 6 ? lamp(w, g, e) : (roll < 8 ? bench(w, g, face, e) : (roll < 9 && soil ? bush(w, g, face, r) : barrels(w, g, r))));
      };
      if (ok) {
         Fx.particles(w, ParticleTypes.HAPPY_VILLAGER, g.getX() + 0.5, g.getY() + 1, g.getZ() + 0.5, 5, 0.3, 0.3, 0.3, 0.02);
      }

      return ok;
   }

   static boolean set(ServerWorld w, BlockPos p, BlockState s) {
      w.setBlockState(p, s, 3);
      return true;
   }

   static boolean flowers(ServerWorld w, BlockPos g, Random r) {
      Block f = FLOWERS[r.nextInt(FLOWERS.length)];
      set(w, g, f.getDefaultState());

      for (Direction d : Type.HORIZONTAL) {
         if (r.nextInt(3) != 0) {
            BlockPos q = g.offset(d);
            if (!Civ.inCity(q.getX(), q.getZ(), 0) && free(w, q) && w.getBlockState(q.down()).isIn(BlockTags.DIRT)) {
               set(w, q, (r.nextInt(4) == 0 ? FLOWERS[r.nextInt(FLOWERS.length)] : f).getDefaultState());
            }
         }
      }

      return true;
   }

   static boolean lamp(ServerWorld w, BlockPos g, int e) {
      Block post = e <= 4
         ? Blocks.OAK_FENCE
         : (e <= 6 ? Blocks.COBBLESTONE_WALL : (e <= 8 ? Blocks.STONE_BRICK_WALL : (e == 9 ? Blocks.POLISHED_BLACKSTONE_WALL : Blocks.DEEPSLATE_BRICK_WALL)));
      Block light = e <= 1 ? Blocks.TORCH : (e == 9 ? Blocks.SOUL_LANTERN : Blocks.LANTERN);
      int h = e <= 1 ? 1 : 2;

      for (int i = 0; i < h; i++) {
         set(w, g.up(i), post.getDefaultState());
      }

      set(w, g.up(h), light.getDefaultState());
      return true;
   }

   static boolean bench(ServerWorld w, BlockPos g, Direction face, int e) {
      Block st = e <= 3 ? Blocks.OAK_STAIRS : (e <= 6 ? Blocks.SPRUCE_STAIRS : (e <= 8 ? Blocks.DARK_OAK_STAIRS : Blocks.STONE_BRICK_STAIRS));
      BlockState s = st.getDefaultState().with(StairsBlock.FACING, face);
      set(w, g, s);
      BlockPos q = g.offset(face.rotateYClockwise());
      if (free(w, q) && w.getBlockState(q.up()).isAir() && !free(w, q.down()) && !Civ.inCity(q.getX(), q.getZ(), 0)) {
         set(w, q, s);
      }

      return true;
   }

   static boolean bush(ServerWorld w, BlockPos g, Direction face, Random r) {
      if (r.nextBoolean()) {
         return set(w, g, (r.nextBoolean() ? Blocks.FLOWERING_AZALEA : Blocks.AZALEA).getDefaultState());
      } else {
         BlockState leaf = Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
         set(w, g, leaf);
         BlockPos q = g.offset(face.rotateYClockwise());
         if (free(w, q) && !free(w, q.down()) && !Civ.inCity(q.getX(), q.getZ(), 0)) {
            set(w, q, leaf);
         }

         return true;
      }
   }

   static boolean barrels(ServerWorld w, BlockPos g, Random r) {
      BlockState bar = Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.UP);
      set(w, g, bar);
      if (r.nextBoolean()) {
         set(w, g.up(), bar);
      }

      return true;
   }

   static boolean hay(ServerWorld w, BlockPos g, Random r) {
      set(w, g, Blocks.HAY_BLOCK.getDefaultState());
      if (r.nextInt(5) < 2) {
         set(w, g.up(), Blocks.HAY_BLOCK.getDefaultState());
      }

      return true;
   }

   static boolean scarecrow(ServerWorld w, BlockPos g, Direction face) {
      set(w, g, Blocks.OAK_FENCE.getDefaultState());
      set(w, g.up(), Blocks.HAY_BLOCK.getDefaultState());
      set(w, g.up(2), Blocks.CARVED_PUMPKIN.getDefaultState().with(CarvedPumpkinBlock.FACING, face.getOpposite()));
      return true;
   }

   static boolean flag(Kingdoms.K k, ServerWorld w, BlockPos g) {
      if (!w.getBlockState(g.up(3)).isAir()) {
         return false;
      } else {
         Direction side = free(w, g.up(3).east()) && free(w, g.up(2).east()) ? Direction.EAST : Direction.WEST;
         if (free(w, g.up(3).offset(side)) && free(w, g.up(2).offset(side))) {
            for (int i = 0; i <= 3; i++) {
               set(w, g.up(i), Blocks.OAK_FENCE.getDefaultState());
            }

            set(w, g.up(3).offset(side), k.wool.getDefaultState());
            set(w, g.up(2).offset(side), k.wool.getDefaultState());
            return true;
         } else {
            return false;
         }
      }
   }

   static void power(ServerPlayerEntity god, ServerWorld w, BlockPos pos) {
      Kingdoms.K best = null;
      double bd = 40000.0;

      for (Kingdoms.K k : Kingdoms.K.values()) {
         if (k.alive && k.world == w && k.capital != null) {
            double d = k.capital.getSquaredDistance(pos);
            if (d < bd) {
               bd = d;
               best = k;
            }
         }
      }

      if (best == null) {
         god.sendMessage(Text.literal("Рядом нет поселений").formatted(Formatting.GRAY), true);
      } else {
         Civ.City c = Civ.city(best);
         int n = 0;

         for (Civ.Building b : new ArrayList<>(c.buildings)) {
            if (b.done && b.kind != Civ.Kind.QUARRY && (PAVED.get(b) == null || PAVED.get(b) != c.era) && loaded(w, b)) {
               PAVED.put(b, c.era);
               if (pave(best, c, b, w)) {
                  n++;
               }
            }
         }

         n += decorate(best, c, w, 60);
         BlockPos cp = best.capital;
         Fx.particles(w, ParticleTypes.HAPPY_VILLAGER, cp.getX() + 0.5, cp.getY() + 3, cp.getZ() + 0.5, 80, 8.0, 2.0, 8.0, 0.05);
         Fx.sound(w, cp.getX() + 0.5, cp.getY() + 1, cp.getZ() + 0.5, SoundEvents.ENTITY_VILLAGER_CELEBRATE, 1.5F, 1.0F);
         god.sendMessage(Text.literal(best.title + ": благоустроено мест — " + n).formatted(best.color), false);
      }
   }
}
