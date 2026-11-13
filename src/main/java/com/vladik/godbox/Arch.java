package com.vladik.godbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;

final class Arch {
   private static final Map<String, Block> CACHE = new HashMap<>();
   static final Block[] POTS = new Block[]{
      Blocks.POTTED_POPPY,
      Blocks.POTTED_DANDELION,
      Blocks.POTTED_CORNFLOWER,
      Blocks.POTTED_ALLIUM,
      Blocks.POTTED_AZURE_BLUET,
      Blocks.POTTED_RED_TULIP,
      Blocks.POTTED_FERN,
      Blocks.POTTED_AZALEA_BUSH
   };

   private Arch() {
   }

   static Block variant(Block full, String suffix) {
      Identifier id = Registries.BLOCK.getId(full);
      String key = id + "#" + suffix;
      if (CACHE.containsKey(key)) {
         return CACHE.get(key);
      } else {
         String p = id.getPath();
         List<String> cand = new ArrayList<>();
         if (p.endsWith("_planks")) {
            cand.add(p.substring(0, p.length() - 7) + "_" + suffix);
         }

         if (p.endsWith("_block")) {
            cand.add(p.substring(0, p.length() - 6) + "_" + suffix);
         }

         if (p.endsWith("bricks") || p.endsWith("tiles")) {
            cand.add(p.substring(0, p.length() - 1) + "_" + suffix);
         }

         cand.add(p + "_" + suffix);
         Block found = null;

         for (String c : cand) {
            Identifier i = Identifier.of(id.getNamespace(), c);
            if (Registries.BLOCK.containsId(i)) {
               found = Registries.BLOCK.get(i);
               break;
            }
         }

         CACHE.put(key, found);
         return found;
      }
   }

   static BlockState stairs(BlockState full, Direction facing, boolean top) {
      Block s = variant(full.getBlock(), "stairs");
      return !(s instanceof StairsBlock)
         ? null
         : s.getDefaultState().with(StairsBlock.FACING, facing).with(StairsBlock.HALF, top ? BlockHalf.TOP : BlockHalf.BOTTOM);
   }

   static BlockState stairsOr(BlockState full, Direction facing) {
      BlockState s = stairs(full, facing, false);
      return s == null ? full : s;
   }

   static BlockState slabOr(BlockState full) {
      Block s = variant(full.getBlock(), "slab");
      return s instanceof SlabBlock ? s.getDefaultState() : full;
   }

   static BlockState beam(BlockState log, Axis axis) {
      return log.contains(PillarBlock.AXIS) ? log.with(PillarBlock.AXIS, axis) : log;
   }

   static void gable(Arch.Put o, int x0, int z0, int x1, int z1, int y, BlockState roof, BlockState wall, BlockState ridge) {
      if (ridge == null) {
         ridge = roof;
      }

      boolean alongX = x1 - x0 >= z1 - z0;
      int i = 0;

      while (true) {
         int yy = y + i;
         if (alongX) {
            int a = z0 - 1 + i;
            int b = z1 + 1 - i;
            if (a > b) {
               break;
            }

            if (a == b) {
               for (int x = x0 - 1; x <= x1 + 1; x++) {
                  o.put(new BlockPos(x, yy, a), ridge);
               }

               return;
            }

            BlockState sa = stairsOr(roof, Direction.SOUTH);
            BlockState sb = stairsOr(roof, Direction.NORTH);

            for (int x = x0 - 1; x <= x1 + 1; x++) {
               o.put(new BlockPos(x, yy, a), sa);
               o.put(new BlockPos(x, yy, b), sb);
            }

            if (i >= 1) {
               for (int z = a + 1; z <= b - 1; z++) {
                  o.put(new BlockPos(x0, yy, z), wall);
                  o.put(new BlockPos(x1, yy, z), wall);
               }
            }
         } else {
            int ax = x0 - 1 + i;
            int bx = x1 + 1 - i;
            if (ax > bx) {
               break;
            }

            if (ax == bx) {
               for (int z = z0 - 1; z <= z1 + 1; z++) {
                  o.put(new BlockPos(ax, yy, z), ridge);
               }

               return;
            }

            BlockState sa = stairsOr(roof, Direction.EAST);
            BlockState sb = stairsOr(roof, Direction.WEST);

            for (int z = z0 - 1; z <= z1 + 1; z++) {
               o.put(new BlockPos(ax, yy, z), sa);
               o.put(new BlockPos(bx, yy, z), sb);
            }

            if (i >= 1) {
               for (int x = ax + 1; x <= bx - 1; x++) {
                  o.put(new BlockPos(x, yy, z0), wall);
                  o.put(new BlockPos(x, yy, z1), wall);
               }
            }
         }

         i++;
      }
   }

   static int gableHeight(int x0, int z0, int x1, int z1) {
      int span = (x1 - x0 >= z1 - z0 ? z1 - z0 : x1 - x0) + 3;
      return (span - 1) / 2;
   }

   static Block woodenDoor(BlockState wallMat) {
      Block d = variant(wallMat.getBlock(), "door");
      return d instanceof DoorBlock && d.getDefaultState().isIn(BlockTags.WOODEN_DOORS) ? d : Blocks.SPRUCE_DOOR;
   }

   static Block woodenTrapdoor(BlockState wallMat) {
      Block d = variant(wallMat.getBlock(), "trapdoor");
      return d instanceof TrapdoorBlock && d.getDefaultState().isIn(BlockTags.WOODEN_TRAPDOORS) ? d : Blocks.SPRUCE_TRAPDOOR;
   }

   static void door(Arch.Put o, BlockPos lower, Direction out, Block door) {
      BlockState d = door.getDefaultState().with(DoorBlock.FACING, out.getOpposite()).with(DoorBlock.HINGE, DoorHinge.LEFT).with(DoorBlock.OPEN, false);
      o.put(lower, d.with(DoorBlock.HALF, DoubleBlockHalf.LOWER));
      o.put(lower.up(), d.with(DoorBlock.HALF, DoubleBlockHalf.UPPER));
   }

   static void shutters(Arch.Put o, BlockPos window, Direction out, Block trap) {
      BlockState s = trap.getDefaultState().with(TrapdoorBlock.FACING, out).with(TrapdoorBlock.OPEN, true).with(TrapdoorBlock.HALF, BlockHalf.TOP);
      Direction side = out.rotateYClockwise();
      o.put(window.offset(side).offset(out), s);
      o.put(window.offset(side.getOpposite()).offset(out), s);
   }

   static void wallTorch(Arch.Put o, BlockPos wallBlock, Direction out) {
      o.put(wallBlock.offset(out), Blocks.WALL_TORCH.getDefaultState().with(WallTorchBlock.FACING, out));
   }

   static void chimney(Arch.Put o, int x, int z, int yFrom, int yTo, BlockState brick) {
      for (int y = yFrom; y <= yTo; y++) {
         o.put(new BlockPos(x, y, z), brick);
      }

      o.put(new BlockPos(x, yTo + 1, z), Blocks.CAMPFIRE.getDefaultState().with(CampfireBlock.SIGNAL_FIRE, true));
   }

   static void bed(Arch.Put o, BlockPos head, Direction toHead, Block bed) {
      BlockState s = bed.getDefaultState().with(BedBlock.FACING, toHead);
      o.put(head.offset(toHead.getOpposite()), s.with(BedBlock.PART, BedPart.FOOT));
      o.put(head, s.with(BedBlock.PART, BedPart.HEAD));
   }

   static BlockState hanging(Block lantern) {
      return lantern.getDefaultState().with(LanternBlock.HANGING, true);
   }

   static BlockState pot(int seed) {
      return POTS[Math.floorMod(seed, POTS.length)].getDefaultState();
   }

   interface Put {
      void put(BlockPos var1, BlockState var2);
   }
}
