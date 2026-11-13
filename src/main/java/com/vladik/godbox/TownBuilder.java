package com.vladik.godbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;

final class TownBuilder {
   static final int COTTAGE = 0;
   static final int TALL = 1;
   static final int LONG = 2;
   private static final Civ.Kind H = Civ.Kind.HOUSE;
   private static final Block[] FLOWERS = new Block[]{
      Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.ALLIUM, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY, Blocks.LILY_OF_THE_VALLEY
   };
   private static final Map<Long, TownBuilder.Job> UNDER_BUILD = new HashMap<>();
   static final int[][] PLOTS = new int[][]{{-11, 0, 0}, {5, 0, 1}, {5, 1, 0}, {-15, 2, 1}, {-17, 1, 0}, {11, 0, 0}, {11, 2, 1}};
   static final Map<Long, Integer> TOWN_ERA = new HashMap<>();

   private TownBuilder() {
   }

   static int ground(ServerWorld w, int x, int z) {
      BlockPos p;
      for (p = Fx.surface(w, x, z); p.getY() > w.getBottomY() + 1; p = p.down()) {
         BlockState s = w.getBlockState(p.down());
         if (!s.getFluidState().isEmpty() || !s.isIn(BlockTags.LEAVES) && !s.isIn(BlockTags.LOGS) && !s.isReplaceable()) {
            break;
         }
      }

      return p.getY();
   }

   static boolean dry(ServerWorld w, int x, int z) {
      if (!w.isChunkLoaded(x >> 4, z >> 4)) {
         return false;
      } else {
         int y = ground(w, x, z);
         BlockPos g = new BlockPos(x, y - 1, z);
         BlockState s = w.getBlockState(g);
         return s.getFluidState().isEmpty() && w.getFluidState(g.up()).isEmpty() && !s.isAir();
      }
   }

   static boolean dryArea(ServerWorld w, int x, int z, int h) {
      int y = ground(w, x, z);
      int st = Math.max(2, h / 2);

      for (int dx = -h; dx <= h; dx += st) {
         for (int dz = -h; dz <= h; dz += st) {
            if (!dry(w, x + dx, z + dz)) {
               return false;
            }

            if (Math.abs(ground(w, x + dx, z + dz) - y) > 5) {
               return false;
            }
         }
      }

      return true;
   }

   static BlockPos dryNear(ServerWorld w, BlockPos c, int r) {
      for (int d = 0; d <= r; d += 2) {
         for (int dx = -d; dx <= d; dx += 2) {
            for (int dz = -d; dz <= d; dz += 2) {
               if (Math.max(Math.abs(dx), Math.abs(dz)) == d) {
                  int x = c.getX() + dx;
                  int z = c.getZ() + dz;
                  if (dryArea(w, x, z, 10)) {
                     return new BlockPos(x, ground(w, x, z), z);
                  }
               }
            }
         }
      }

      return null;
   }

   static BlockPos landOr(ServerWorld w, BlockPos p) {
      if (dry(w, p.getX(), p.getZ())) {
         return new BlockPos(p.getX(), ground(w, p.getX(), p.getZ()), p.getZ());
      } else {
         for (int d = 2; d <= 48; d += 2) {
            for (int dx = -d; dx <= d; dx += 2) {
               for (int dz = -d; dz <= d; dz += 2) {
                  if (Math.max(Math.abs(dx), Math.abs(dz)) == d) {
                     int x = p.getX() + dx;
                     int z = p.getZ() + dz;
                     if (dryArea(w, x, z, 4)) {
                        return new BlockPos(x, ground(w, x, z), z);
                     }
                  }
               }
            }
         }

         return p;
      }
   }

   private static BlockState st(int era, Civ.Kind kind, Block b) {
      return Eras.style(era, kind, b.getDefaultState());
   }

   static void flatten(TownBuilder.Job j, int x0, int z0, int x1, int z1, int y) {
      if (!j.dry) {
         for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
               int lo = y;

               for (int yy = y - 1; yy >= y - 12 && yy > j.w.getBottomY(); lo = yy--) {
                  BlockPos p = new BlockPos(x, yy, z);
                  if (j.w.getBlockState(p).isSolidBlock(j.w, p)) {
                     break;
                  }
               }

               for (int yyx = lo; yyx < y; yyx++) {
                  j.put(new BlockPos(x, yyx, z), Blocks.DIRT.getDefaultState());
               }

               for (int yyx = y; yyx <= y + 7; yyx++) {
                  BlockPos p = new BlockPos(x, yyx, z);
                  if (!j.w.getBlockState(p).isAir()) {
                     j.put(p, Blocks.AIR.getDefaultState());
                  }
               }
            }
         }
      }
   }

   static BlockState hedge() {
      return Blocks.FLOWERING_AZALEA_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
   }

   static void clear(TownBuilder.Job j, int x0, int z0, int x1, int z1, int yFrom, int yTo) {
      if (!j.dry) {
         for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
               for (int y = yFrom; y <= yTo; y++) {
                  BlockPos p = new BlockPos(x, y, z);
                  if (!j.w.getBlockState(p).isAir()) {
                     j.put(p, Blocks.AIR.getDefaultState());
                  }
               }
            }
         }
      }
   }

   static void floor(TownBuilder.Job j, int era, int rev, int x0, int z0, int x1, int z1, int y) {
      BlockState fnd = st(era, H, Blocks.STONE_BRICKS);
      BlockState fl = st(era, H, Blocks.SPRUCE_PLANKS);

      for (int x = x0; x <= x1; x++) {
         for (int z = z0; z <= z1; z++) {
            boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
            j.put(new BlockPos(x, y - 1, z), edge ? fnd : fl);
            if (edge) {
               j.put(new BlockPos(x, y - 2, z), fnd);
               if (rev >= 2) {
                  sink(j, x, z, y - 3, fnd);
               }
            }
         }
      }
   }

   private static boolean foundation(BlockState cur, BlockState s) {
      if (cur.isOf(s.getBlock())) {
         return true;
      } else {
         for (int e = 0; e <= 10; e++) {
            if (cur.isOf(st(e, H, Blocks.STONE_BRICKS).getBlock())) {
               return true;
            }
         }

         return false;
      }
   }

   static void sink(TownBuilder.Job j, int x, int z, int yFrom, BlockState s) {
      for (int yy = yFrom; yy >= yFrom - 10 && yy > j.w.getBottomY(); yy--) {
         BlockPos p = new BlockPos(x, yy, z);
         BlockState cur = j.w.getBlockState(p);
         if (cur.isSolidBlock(j.w, p) && (!j.dry || !foundation(cur, s))) {
            break;
         }

         j.put(p, s);
      }
   }

   static void walls(
      TownBuilder.Job j, int era, int x0, int z0, int x1, int z1, int y, int h, boolean door, boolean south, BlockState log, BlockState plank, BlockState low
   ) {
      BlockState glass = st(era, H, Blocks.GLASS);
      int cx = (x0 + x1) / 2;
      int fz = south ? z1 : z0;

      for (int x = x0; x <= x1; x++) {
         for (int z = z0; z <= z1; z++) {
            boolean ex = x == x0 || x == x1;
            boolean ez = z == z0 || z == z1;
            if (ex || ez) {
               int i = ez ? x - x0 : z - z0;
               int len = ez ? x1 - x0 : z1 - z0;

               for (int dy = 0; dy < h; dy++) {
                  BlockPos p = new BlockPos(x, y + dy, z);
                  if (ex && ez) {
                     j.put(p, log);
                  } else if (!door || x != cx || z != fz || dy >= 2) {
                     if (len >= 8 && i % 4 == 0) {
                        j.put(p, log);
                     } else if (dy == 1 && i % 4 == 2) {
                        j.put(p, glass);
                     } else {
                        j.put(p, dy == 0 && low != null ? low : plank);
                     }
                  }
               }
            }
         }
      }
   }

   static void ring(TownBuilder.Job j, int x0, int z0, int x1, int z1, int y, BlockState log, BlockState plank, int hx, int hz) {
      for (int x = x0; x <= x1; x++) {
         for (int z = z0; z <= z1; z++) {
            boolean ex = x == x0 || x == x1;
            boolean ez = z == z0 || z == z1;
            if (x != hx || z != hz) {
               BlockState c = ex && ez ? log : (ez ? Arch.beam(log, Axis.X) : (ex ? Arch.beam(log, Axis.Z) : plank));
               j.put(new BlockPos(x, y, z), c);
            }
         }
      }
   }

   static void front(TownBuilder.Job j, int era, int x0, int z0, int x1, int z1, int y, boolean south) {
      Direction out = south ? Direction.SOUTH : Direction.NORTH;
      int fz = south ? z1 : z0;
      int oz = south ? z1 + 1 : z0 - 1;
      int cx = (x0 + x1) / 2;
      Arch.door(j::put, new BlockPos(cx, y, fz), out, Blocks.SPRUCE_DOOR);
      Arch.wallTorch(j::put, new BlockPos(cx + 1, y + 1, fz), out);

      for (int x = x0 + 1; x < x1; x++) {
         if (x != cx && x != cx + 1 && (Math.abs(x - cx) <= 2 || (x - x0) % 4 == 2)) {
            j.put(new BlockPos(x, y, oz), hedge());
         }
      }

      if (era >= 1) {
         for (int xx = x0 + 1; xx < x1; xx++) {
            if (xx != cx && (xx - x0) % 4 == 2) {
               Arch.shutters(j::put, new BlockPos(xx, y + 1, fz), out, Blocks.SPRUCE_TRAPDOOR);
            }
         }

         Arch.shutters(j::put, new BlockPos(x0, y + 1, z0 + 2), Direction.WEST, Blocks.SPRUCE_TRAPDOOR);
         Arch.shutters(j::put, new BlockPos(x1, y + 1, z0 + 2), Direction.EAST, Blocks.SPRUCE_TRAPDOOR);
      }
   }

   static void house(TownBuilder.Job j, int era, int x0, int z0, int y) {
      cottage(j, era, 2, x0, z0, y, true);
   }

   static void cottage(TownBuilder.Job j, int era, int rev, int x0, int z0, int y, boolean south) {
      int x1 = x0 + 4;
      int z1 = z0 + 4;
      int cx = x0 + 2;
      int cz = z0 + 2;
      flatten(j, x0 - 1, z0 - 1, x1 + 1, z1 + 1, y);
      clear(j, x0 - 1, z0 - 1, x1 + 1, z1 + 1, y + 8, y + 10);
      BlockState log = st(era, H, Blocks.OAK_LOG);
      BlockState plank = st(era, H, Blocks.OAK_PLANKS);
      BlockState roof = st(era, H, Blocks.DARK_OAK_PLANKS);
      BlockState low = st(era, H, Blocks.COBBLESTONE);
      floor(j, era, rev, x0, z0, x1, z1, y);
      walls(j, era, x0, z0, x1, z1, y, 3, true, south, log, plank, low);
      ring(j, x0, z0, x1, z1, y + 3, log, plank, Integer.MIN_VALUE, 0);
      Arch.gable(j::put, x0, z0, x1, z1, y + 3, roof, plank, roof);
      int top = y + 3 + Arch.gableHeight(x0, z0, x1, z1);
      int bz = south ? z0 + 1 : z1 - 1;
      Direction out = south ? Direction.SOUTH : Direction.NORTH;
      Arch.chimney(j::put, x1 - 1, bz, y + 4, top, st(era, H, Blocks.BRICKS));
      front(j, era, x0, z0, x1, z1, y, south);
      Arch.bed(j::put, new BlockPos(x0 + 1, y, bz), out.getOpposite(), Blocks.WHITE_BED);
      j.put(new BlockPos(x1 - 1, y, bz), Blocks.CRAFTING_TABLE.getDefaultState());
      j.put(new BlockPos(x1 - 1, y + 1, bz), Arch.pot(x0 + z0));
      j.put(new BlockPos(x1 - 1, y, south ? z1 - 1 : z0 + 1), Blocks.BARREL.getDefaultState());
      j.put(new BlockPos(cx, y + 2, cz), Arch.hanging(Blocks.LANTERN));
   }

   static void tall(TownBuilder.Job j, int era, int rev, int x0, int z0, int y, boolean south) {
      int x1 = x0 + 4;
      int z1 = z0 + 5;
      int cx = x0 + 2;
      int cz = z0 + 2;
      Direction out = south ? Direction.SOUTH : Direction.NORTH;
      int fz = south ? z1 : z0;
      int oz = south ? z1 + 1 : z0 - 1;
      int oz2 = south ? z1 + 2 : z0 - 2;
      flatten(j, x0 - 1, Math.min(z0 - 1, oz2), x1 + 1, Math.max(z1 + 1, oz2), y);
      int sh = rev >= 2 ? 3 : 2;
      int yt = y + 4 + sh;
      clear(j, x0 - 1, z0 - 1, x1 + 1, z1 + 1, y + 8, y + 9 + sh);
      BlockState log = st(era, H, Blocks.SPRUCE_LOG);
      BlockState plank = st(era, H, Blocks.OAK_PLANKS);
      BlockState roof = st(era, H, Blocks.DARK_OAK_PLANKS);
      BlockState low = st(era, H, Blocks.STONE_BRICKS);
      BlockState upper = st(era, H, Blocks.BIRCH_PLANKS);
      floor(j, era, rev, x0, z0, x1, z1, y);
      walls(j, era, x0, z0, x1, z1, y, 3, true, south, log, plank, low);
      int lx = x0 + 1;
      int lz = south ? z0 + 1 : z1 - 1;
      ring(j, x0, z0, x1, z1, y + 3, log, plank, lx, lz);
      walls(j, era, x0, z0, x1, z1, y + 4, sh, true, south, log, upper, null);
      ring(j, x0, z0, x1, z1, yt, log, plank, Integer.MIN_VALUE, 0);
      Arch.gable(j::put, x0, z0, x1, z1, yt, roof, upper, roof);
      int top = yt + Arch.gableHeight(x0, z0, x1, z1);
      Arch.chimney(j::put, x1 - 1, lz, yt + 1, top, st(era, H, Blocks.BRICKS));

      for (int dy = 0; dy <= (rev >= 2 ? 4 : 3); dy++) {
         j.put(new BlockPos(lx, y + dy, lz), Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, out));
      }

      front(j, era, x0, z0, x1, z1, y, south);
      BlockState slab = Arch.slabOr(st(era, H, Blocks.SPRUCE_PLANKS));
      if (slab.contains(SlabBlock.TYPE)) {
         slab = slab.with(SlabBlock.TYPE, SlabType.TOP);
      }

      for (int x = x0; x <= x1; x++) {
         j.put(new BlockPos(x, y + 3, oz), slab);
         j.put(new BlockPos(x, y + 3, oz2), slab);
         j.put(new BlockPos(x, y + 4, oz2), Blocks.SPRUCE_FENCE.getDefaultState());
      }

      j.put(new BlockPos(x0, y + 4, oz), Blocks.SPRUCE_FENCE.getDefaultState());
      j.put(new BlockPos(x1, y + 4, oz), Blocks.SPRUCE_FENCE.getDefaultState());

      for (int dy = 0; dy < 3; dy++) {
         j.put(new BlockPos(x0, y + dy, oz2), Blocks.SPRUCE_FENCE.getDefaultState());
         j.put(new BlockPos(x1, y + dy, oz2), Blocks.SPRUCE_FENCE.getDefaultState());
      }

      j.put(new BlockPos(x0 + 1, y + 4, oz2), Blocks.SPRUCE_FENCE.getDefaultState());
      j.put(new BlockPos(x0 + 1, y + 5, oz2), Arch.pot(x0 * 7 + z0));
      Arch.door(j::put, new BlockPos(cx, y + 4, fz), out, Blocks.SPRUCE_DOOR);
      j.put(new BlockPos(x1 - 1, y, lz), Blocks.FURNACE.getDefaultState());
      j.put(new BlockPos(x1 - 1, y, cz + (south ? 1 : -1)), Blocks.CRAFTING_TABLE.getDefaultState());
      j.put(new BlockPos(x1 - 1, y, south ? z1 - 1 : z0 + 1), Blocks.BARREL.getDefaultState());
      j.put(new BlockPos(cx, y + 2, cz + 1), Arch.hanging(Blocks.LANTERN));
      Arch.bed(j::put, new BlockPos(x1 - 1, y + 4, lz), out.getOpposite(), Blocks.RED_BED);
      j.put(new BlockPos(cx, y + 4, lz), Blocks.CHEST.getDefaultState());
      j.put(new BlockPos(x0 + 1, y + 4, south ? z1 - 1 : z0 + 1), Blocks.BOOKSHELF.getDefaultState());
      j.put(new BlockPos(cx, yt - 1, cz + 1), Arch.hanging(Blocks.LANTERN));
   }

   static void longhouse(TownBuilder.Job j, int era, int rev, int x0, int z0, int y, boolean south) {
      int x1 = x0 + 8;
      int z1 = z0 + 4;
      int cz = z0 + 2;
      flatten(j, x0 - 1, z0 - 1, x1 + 1, z1 + 1, y);
      clear(j, x0 - 1, z0 - 1, x1 + 1, z1 + 1, y + 8, y + 10);
      BlockState log = st(era, H, Blocks.DARK_OAK_LOG);
      BlockState plank = st(era, H, Blocks.SPRUCE_PLANKS);
      BlockState roof = st(era, H, Blocks.SPRUCE_PLANKS);
      BlockState low = st(era, H, Blocks.MOSSY_COBBLESTONE);
      floor(j, era, rev, x0, z0, x1, z1, y);
      walls(j, era, x0, z0, x1, z1, y, 3, true, south, log, plank, low);
      ring(j, x0, z0, x1, z1, y + 3, log, plank, Integer.MIN_VALUE, 0);
      Arch.gable(j::put, x0, z0, x1, z1, y + 3, roof, plank, st(era, H, Blocks.DARK_OAK_PLANKS));
      int top = y + 3 + Arch.gableHeight(x0, z0, x1, z1);
      int bz = south ? z0 + 1 : z1 - 1;
      Direction out = south ? Direction.SOUTH : Direction.NORTH;
      Arch.chimney(j::put, x1 - 2, bz, y + 4, top, st(era, H, Blocks.BRICKS));
      front(j, era, x0, z0, x1, z1, y, south);
      Arch.bed(j::put, new BlockPos(x0 + 1, y, bz), out.getOpposite(), Blocks.WHITE_BED);
      Arch.bed(j::put, new BlockPos(x0 + 2, y, bz), out.getOpposite(), Blocks.LIGHT_BLUE_BED);
      j.put(new BlockPos(x1 - 1, y, bz), Blocks.FURNACE.getDefaultState());
      j.put(new BlockPos(x1 - 3, y, bz), Blocks.CRAFTING_TABLE.getDefaultState());
      j.put(new BlockPos(x1 - 1, y, south ? z1 - 1 : z0 + 1), Blocks.CHEST.getDefaultState());
      j.put(new BlockPos(x0 + 4, y, bz), Blocks.BARREL.getDefaultState());
      j.put(new BlockPos(x0 + 4, y + 1, bz), Arch.pot(x0 + z0 * 3));
      j.put(new BlockPos(x0 + 2, y + 2, cz), Arch.hanging(Blocks.LANTERN));
      j.put(new BlockPos(x1 - 2, y + 2, cz), Arch.hanging(Blocks.LANTERN));
   }

   static void garden(TownBuilder.Job j, int x0, int z0, int x1, int z1, int y, int seed) {
      flatten(j, x0, z0, x1, z1, y);
      int gx = (x0 + x1) / 2;

      for (int x = x0; x <= x1; x++) {
         for (int z = z0; z <= z1; z++) {
            boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
            j.put(new BlockPos(x, y - 1, z), Blocks.GRASS_BLOCK.getDefaultState());
            if (edge) {
               if (x != gx || z != z0 && z != z1 || ((seed & 1) == 0 ? z != z0 : z != z1)) {
                  j.put(new BlockPos(x, y, z), Blocks.OAK_FENCE.getDefaultState());
               }
            } else {
               j.put(new BlockPos(x, y, z), FLOWERS[Math.floorMod(seed * 3 + x * 7 + z * 5, FLOWERS.length)].getDefaultState());
            }
         }
      }
   }

   static void well(TownBuilder.Job j, int era, int cx, int cz, int y) {
      flatten(j, cx - 1, cz - 1, cx + 1, cz + 1, y);
      BlockState ring = st(era, H, Blocks.COBBLESTONE);
      BlockState wall = st(era, H, Blocks.COBBLESTONE_WALL);
      BlockState roof = Arch.slabOr(st(era, H, Blocks.SPRUCE_PLANKS));

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            int x = cx + dx;
            int z = cz + dz;
            if (dx == 0 && dz == 0) {
               j.put(new BlockPos(x, y - 3, z), ring);
               j.put(new BlockPos(x, y - 2, z), Blocks.WATER.getDefaultState());
               j.put(new BlockPos(x, y - 1, z), Blocks.WATER.getDefaultState());
            } else {
               j.put(new BlockPos(x, y - 2, z), ring);
               j.put(new BlockPos(x, y - 1, z), ring);
               j.put(new BlockPos(x, y, z), wall);
               if (dx != 0 && dz != 0) {
                  j.put(new BlockPos(x, y + 1, z), Blocks.OAK_FENCE.getDefaultState());
                  j.put(new BlockPos(x, y + 2, z), Blocks.OAK_FENCE.getDefaultState());
               }
            }

            j.put(new BlockPos(x, y + 3, z), roof);
         }
      }

      j.put(new BlockPos(cx, y + 2, cz), Arch.hanging(Blocks.LANTERN));
   }

   static void stall(TownBuilder.Job j, Kingdoms.K k, int x0, int z0, int y, int seed) {
      flatten(j, x0, z0, x0 + 2, z0 + 2, y);

      for (int x = 0; x < 3; x++) {
         for (int z = 0; z < 3; z++) {
            boolean corner = (x == 0 || x == 2) && (z == 0 || z == 2);
            if (corner) {
               j.put(new BlockPos(x0 + x, y, z0 + z), Blocks.SPRUCE_FENCE.getDefaultState());
               j.put(new BlockPos(x0 + x, y + 1, z0 + z), Blocks.SPRUCE_FENCE.getDefaultState());
            }

            j.put(new BlockPos(x0 + x, y + 2, z0 + z), (x % 2 == 0 ? k.wool : Blocks.WHITE_WOOL).getDefaultState());
         }
      }

      Block[] goods = new Block[]{Blocks.HAY_BLOCK, Blocks.PUMPKIN, Blocks.MELON, Blocks.BARREL};
      j.put(new BlockPos(x0 + 1, y, z0), goods[Math.floorMod(seed, goods.length)].getDefaultState());
      j.put(new BlockPos(x0 + 1, y, z0 + 2), Blocks.BARREL.getDefaultState());
   }

   static void lamp(TownBuilder.Job j, int era, int x, int y, int z) {
      BlockState post = era >= 5 ? st(era, H, Blocks.STONE_BRICK_WALL) : Blocks.OAK_FENCE.getDefaultState();

      for (int dy = 0; dy < 3; dy++) {
         j.put(new BlockPos(x, y + dy, z), post);
      }

      j.put(new BlockPos(x, y + 3, z), era >= 3 ? Blocks.LANTERN.getDefaultState() : Blocks.TORCH.getDefaultState());
   }

   static void street(TownBuilder.Job j, int era, BlockPos c, int x0, int z0, int x1, int z1) {
      for (int x = x0; x <= x1; x++) {
         for (int z = z0; z <= z1; z++) {
            if (Math.abs(x - c.getX()) > 3 || Math.abs(z - c.getZ()) > 3) {
               pave(j, era, c.getY(), x, z);
            }
         }
      }
   }

   static void pave(TownBuilder.Job j, int era, int cy, int x, int z) {
      if (dry(j.w, x, z)) {
         int gy = ground(j.w, x, z);
         if (Math.abs(gy - cy) <= 3) {
            BlockPos g = new BlockPos(x, gy - 1, z);
            BlockState s;
            if (era >= 5) {
               s = Math.floorMod(x * 3 + z, 5) == 0 ? st(era, H, Blocks.CRACKED_STONE_BRICKS) : st(era, H, Blocks.STONE_BRICKS);
            } else if (era >= 3) {
               s = Math.floorMod(x + z * 2, 4) == 0 ? Blocks.GRAVEL.getDefaultState() : st(era, H, Blocks.COBBLESTONE);
            } else {
               s = Math.floorMod(x * 5 + z * 3, 6) == 0 ? Blocks.COARSE_DIRT.getDefaultState() : Blocks.DIRT_PATH.getDefaultState();
            }

            j.put(g, s);
            BlockState above = j.w.getBlockState(g.up());
            if (!above.isAir() && above.isReplaceable()) {
               j.put(g.up(), Blocks.AIR.getDefaultState());
            }
         }
      }
   }

   static void farm(TownBuilder.Job j, int x0, int z0, int y) {
      flatten(j, x0, z0, x0 + 6, z0 + 6, y);

      for (int x = 0; x < 7; x++) {
         for (int z = 0; z < 7; z++) {
            BlockPos f = new BlockPos(x0 + x, y - 1, z0 + z);
            boolean edge = x == 0 || x == 6 || z == 0 || z == 6;
            if (edge) {
               j.put(f, Blocks.OAK_LOG.getDefaultState());
            } else if (x == 3 && z == 3) {
               j.put(f, Blocks.WATER.getDefaultState());
            } else {
               j.put(f, Blocks.FARMLAND.getDefaultState().with(FarmlandBlock.MOISTURE, 7));
               j.put(f.up(), Blocks.WHEAT.getDefaultState().with(CropBlock.AGE, j.w.getRandom().nextInt(8)));
            }
         }
      }
   }

   static void plaza(TownBuilder.Job j, Kingdoms.K k, int era, BlockPos c) {
      int y = c.getY();
      flatten(j, c.getX() - 3, c.getZ() - 3, c.getX() + 3, c.getZ() + 3, y);

      for (int x = -3; x <= 3; x++) {
         for (int z = -3; z <= 3; z++) {
            j.put(
               new BlockPos(c.getX() + x, y - 1, c.getZ() + z),
               (x + z) % 2 == 0 ? st(era, Civ.Kind.HOUSE, Blocks.COBBLESTONE) : Blocks.GRAVEL.getDefaultState()
            );
         }
      }

      for (int dy = 0; dy < 4; dy++) {
         j.put(c.up(dy), Blocks.OAK_FENCE.getDefaultState());
      }

      j.put(c.up(4), k.wool.getDefaultState());
      j.put(c.add(1, 4, 0), k.wool.getDefaultState());
      j.put(c.add(1, 3, 0), k.wool.getDefaultState());
      int[][] cr = new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}};

      for (int[] q : cr) {
         j.put(c.add(q[0], 0, q[1]), Blocks.OAK_FENCE.getDefaultState());
         j.put(c.add(q[0], 1, q[1]), era >= 3 ? Blocks.LANTERN.getDefaultState() : Blocks.TORCH.getDefaultState());
      }

      if (era >= 4) {
         j.put(c.add(-2, 0, 0), Blocks.BELL.getDefaultState());
      }

      Block bench = era >= 5 ? Blocks.STONE_BRICK_STAIRS : Blocks.SPRUCE_STAIRS;
      int[][] bs = new int[][]{{2, -3, 0}, {-2, -3, 0}, {2, 3, 1}, {-2, 3, 1}, {-3, 2, 2}, {-3, -2, 2}, {3, 2, 3}, {3, -2, 3}};
      Direction[] df = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

      for (int[] q : bs) {
         j.put(c.add(q[0], 0, q[1]), bench.getDefaultState().with(StairsBlock.FACING, df[q[2]]));
      }

      int[][] fl = new int[][]{{-1, -3}, {1, -3}, {-1, 3}, {1, 3}};

      for (int[] q : fl) {
         j.put(c.add(q[0], -1, q[1]), Blocks.GRASS_BLOCK.getDefaultState());
         j.put(c.add(q[0], 0, q[1]), (q[0] > 0 ? Blocks.CORNFLOWER : Blocks.POPPY).getDefaultState());
      }
   }

   static void town(ServerWorld w, Kingdoms.K k, BlockPos c) {
      Civ.City city = Civ.city(k);
      int era = city.era;
      TownBuilder.Job j = new TownBuilder.Job(w);
      List<Civ.Building> houses = new ArrayList<>();
      int cx = c.getX();
      int cz = c.getZ();
      TOWN_ERA.put(c.asLong(), era);
      long tkey = c.asLong();
      UNDER_BUILD.put(tkey, j);
      j.seen = w.getTime();
      List<Props.Prop> props = new ArrayList<>();
      plaza(j, k, era, c);
      int[][] plots = PLOTS;
      int n = Math.min(plots.length, 3 + era / 2);

      for (int i = 0; i < n; i++) {
         int type = plots[i][1];
         boolean north = plots[i][2] == 0;
         int wd = type == 2 ? 9 : 5;
         int dp = type == 1 ? 6 : 5;
         int x0 = cx + plots[i][0];
         int z0 = north ? cz - 3 - (dp - 1) : cz + 3;
         int x1 = x0 + wd - 1;
         int z1 = z0 + dp - 1;
         int mxp = (x0 + x1) / 2;
         int mzp = (z0 + z1) / 2;
         if (dry(w, mxp, mzp) && dry(w, x0, z0) && dry(w, x1, z1) && dry(w, x0, z1) && dry(w, x1, z0)) {
            int y = ground(w, mxp, mzp);
            if (Math.abs(y - c.getY()) <= 6) {
               int from = j.ps.size();
               switch (type) {
                  case 1:
                     tall(j, era, 2, x0, z0, y, north);
                     break;
                  case 2:
                     longhouse(j, era, 2, x0, z0, y, north);
                     break;
                  default:
                     cottage(j, era, 2, x0, z0, y, north);
               }

               for (int q = from; q < j.ss.size(); q++) {
                  j.ss.set(q, Races.style(k, j.ss.get(q)));
               }

               Civ.Building b = new Civ.Building(Civ.Kind.HOUSE, x0, z0, y, wd, dp);
               b.done = true;
               b.era = era;
               b.rev = 2;
               b.plot = type * 2 + (north ? 1 : 0);
               b.busy = true;
               houses.add(b);
               city.buildings.add(b);
               int gz0 = north ? z0 - 4 : z1 + 2;
               if (dry(w, x0, gz0) && dry(w, x1, gz0 + 2) && Math.abs(ground(w, mxp, gz0 + 1) - y) <= 2) {
                  garden(j, x0, gz0, x1, gz0 + 2, y, i + era);
               }

               pave(j, era, c.getY(), mxp, north ? z1 + 1 : z0 - 1);
            }
         }
      }

      if (dry(w, cx, cz - 5)) {
         prop(j, props, Props.Kind.WELL, k, cx, ground(w, cx, cz - 5), cz - 5, 0, era);
      }

      if (era >= 2) {
         if (dry(w, cx - 2, cz + 5)) {
            prop(j, props, Props.Kind.STALL, k, cx - 3, ground(w, cx - 2, cz + 5), cz + 4, cx + era, era);
         }

         if (dry(w, cx + 2, cz + 5)) {
            prop(j, props, Props.Kind.STALL, k, cx + 1, ground(w, cx + 2, cz + 5), cz + 4, cz + era + 1, era);
         }
      }

      street(j, era, c, cx - 20, cz - 1, cx + 20, cz + 1);
      int[][] lamps = new int[][]{{4, -2}, {-4, 2}, {-20, 0}, {20, 0}, {10, -2}, {-16, 2}, {-6, -2}, {-12, -2}};

      for (int[] q : lamps) {
         int lx = cx + q[0];
         int lz = cz + q[1];
         if (dry(w, lx, lz)) {
            prop(j, props, Props.Kind.LAMP, k, lx, ground(w, lx, lz), lz, 0, era);
         }
      }

      int fx = cx - 3;
      int fz = cz + 9;
      int fy = ground(w, fx + 3, fz + 3);
      boolean fdry = dry(w, fx + 3, fz + 3) && dry(w, fx, fz) && dry(w, fx + 6, fz + 6);
      if (fdry) {
         farm(j, fx, fz, fy);
         Civ.Building f = new Civ.Building(Civ.Kind.FARM, fx, fz, fy);
         f.done = true;
         city.buildings.add(f);
      }

      j.onDone = () -> {
         for (Civ.Building hb : houses) {
            hb.busy = false;
         }

         for (Props.Prop pp : props) {
            pp.busy = false;
         }

         UNDER_BUILD.remove(tkey);
      };
      Tasks.add(j);
   }

   private static void prop(TownBuilder.Job j, List<Props.Prop> props, Props.Kind kind, Kingdoms.K k, int x, int y, int z, int arg, int era) {
      Props.Prop p = Props.add(kind, k, x, y, z, arg, era);
      props.add(p);
      Props.build(j, p, era);
   }

   static boolean underBuild(long townKey) {
      TownBuilder.Job j = UNDER_BUILD.get(townKey);
      if (j == null) {
         return false;
      } else if (j.w.getTime() - j.seen > 200L && j.onDone != null) {
         Runnable r = j.onDone;
         j.onDone = null;
         r.run();
         return false;
      } else {
         return true;
      }
   }

   static void clearBuilds() {
      UNDER_BUILD.clear();
   }

   static int[] dims(int type) {
      return new int[]{type == 2 ? 9 : 5, type == 1 ? 6 : 5};
   }

   static void fit(Civ.Building b) {
      if (!b.fitted) {
         b.fitted = true;
         if (b.kind == Civ.Kind.HOUSE) {
            int pl = plotOf(b);
            if (pl >= 0) {
               int[] d = dims(pl >> 1);
               b.x1 = b.x0 + d[0] - 1;
               b.z1 = b.z0 + d[1] - 1;
            }
         }
      }
   }

   static int plotOf(Civ.Building b) {
      if (b.plot != -2) {
         return b.plot;
      } else {
         b.plot = -1;
         if (b.kind != Civ.Kind.HOUSE) {
            return -1;
         } else {
            for (List<BlockPos> ts : new ArrayList<>(Conquest.TOWNS.values())) {
               for (BlockPos c : new ArrayList<>(ts)) {
                  for (int[] pl : PLOTS) {
                     boolean north = pl[2] == 0;
                     int dp = pl[1] == 1 ? 6 : 5;
                     int z0 = north ? c.getZ() - 3 - (dp - 1) : c.getZ() + 3;
                     if (b.x0 == c.getX() + pl[0] && b.z0 == z0) {
                        b.plot = pl[1] * 2 + (north ? 1 : 0);
                        return b.plot;
                     }
                  }
               }
            }

            return -1;
         }
      }
   }

   static TownBuilder.Job houseBlueprint(Kingdoms.K k, Civ.Building b, ServerWorld w, int era) {
      int pl = plotOf(b);
      if (pl < 0) {
         return null;
      } else {
         TownBuilder.Job j = new TownBuilder.Job(w);
         j.dry = true;
         boolean south = (pl & 1) == 1;
         switch (pl >> 1) {
            case 1:
               tall(j, era, b.rev, b.x0, b.z0, b.y0, south);
               break;
            case 2:
               longhouse(j, era, b.rev, b.x0, b.z0, b.y0, south);
               break;
            default:
               cottage(j, era, b.rev, b.x0, b.z0, b.y0, south);
         }

         for (int q = 0; q < j.ss.size(); q++) {
            j.ss.set(q, Races.style(k, j.ss.get(q)));
         }

         return j;
      }
   }

   static boolean pavingBlock(BlockState s) {
      for (int e = 0; e <= 10; e++) {
         if (s.isOf(st(e, H, Blocks.STONE_BRICKS).getBlock()) || s.isOf(st(e, H, Blocks.COBBLESTONE).getBlock())) {
            return true;
         }
      }

      return s.isOf(Blocks.CRACKED_STONE_BRICKS) || s.isOf(Blocks.GRAVEL) || s.isOf(Blocks.DIRT_PATH) || s.isOf(Blocks.COARSE_DIRT);
   }

   static TownBuilder.Job townBlueprint(Kingdoms.K k, ServerWorld w, BlockPos c, int era) {
      if (!w.getBlockState(c).isOf(Blocks.OAK_FENCE)) {
         return null;
      } else {
         TownBuilder.Job j = new TownBuilder.Job(w);
         j.dry = true;
         plaza(j, k, era, c);
         street(j, era, c, c.getX() - 20, c.getZ() - 1, c.getX() + 20, c.getZ() + 1);
         return j;
      }
   }

   static final class Job implements Tasks.Task {
      final ServerWorld w;
      final List<BlockPos> ps = new ArrayList<>();
      final List<BlockState> ss = new ArrayList<>();
      int i;
      boolean dry;
      Runnable onDone;
      long seen;

      Job(ServerWorld w) {
         this.w = w;
      }

      void put(BlockPos p, BlockState s) {
         this.ps.add(p);
         this.ss.add(s);
      }

      @Override
      public boolean tick() {
         this.seen = this.w.getTime();
         boolean end = this.step();
         if (end && this.onDone != null) {
            Runnable r = this.onDone;
            this.onDone = null;
            r.run();
         }

         return end;
      }

      private boolean step() {
         int n = 0;

         while (n < 10 && this.i < this.ps.size()) {
            BlockPos p = this.ps.get(this.i);
            BlockState bs = this.ss.get(this.i);
            Block bb = bs.getBlock();
            if (bb instanceof FenceBlock || bb instanceof WallBlock || bb instanceof PaneBlock) {
               bs = Block.postProcessState(bs, this.w, p);
            }

            this.w.setBlockState(p, bs, 3);
            if (this.i % 15 == 0) {
               Fx.particles(this.w, ParticleTypes.CLOUD, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.01);
            }

            if (this.i % 40 == 0) {
               Fx.sound(this.w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_WOOD_PLACE, 0.6F, 1.0F);
            }

            n++;
            this.i++;
         }

         return this.i >= this.ps.size();
      }
   }
}
