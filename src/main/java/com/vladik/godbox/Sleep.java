package com.vladik.godbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos.Mutable;

public final class Sleep {
   public static boolean on = true;
   private static final Map<VillagerEntity, BlockPos> CLAIM = new WeakHashMap<>();
   private static final Map<VillagerEntity, Integer> TRIES = new WeakHashMap<>();
   private static final Direction[] DIRS = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

   private Sleep() {
   }

   static boolean isBed(ServerWorld w, BlockPos p) {
      return p != null && w.getBlockState(p).getBlock() instanceof BedBlock;
   }

   private static boolean claimed(VillagerEntity v, BlockPos p) {
      for (Entry<VillagerEntity, BlockPos> e : CLAIM.entrySet()) {
         if (e.getKey() != v && e.getKey().isAlive() && p.equals(e.getValue())) {
            return true;
         }
      }

      return false;
   }

   static BlockPos find(Kingdoms.K k, List<Civ.Building> homes, ServerWorld w, VillagerEntity v) {
      if (on && !homes.isEmpty()) {
         List<Civ.Building> l = new ArrayList<>(homes);
         Collections.shuffle(l);

         for (Civ.Building b : l) {
            BlockPos p = bed(k, b, w, v, false);
            if (p != null) {
               return claim(v, p);
            }
         }

         for (Civ.Building bx : l) {
            BlockPos p = bed(k, bx, w, v, true);
            if (p != null) {
               return claim(v, p);
            }
         }

         return null;
      } else {
         return null;
      }
   }

   static BlockPos claim(VillagerEntity v, BlockPos p) {
      CLAIM.put(v, p);
      TRIES.put(v, 0);
      return p;
   }

   private static boolean air(ServerWorld w, BlockPos p) {
      return w.getBlockState(p).isAir();
   }

   private static boolean solid(ServerWorld w, BlockPos p) {
      return w.getBlockState(p).isSolidBlock(w, p);
   }

   private static boolean nearDoor(ServerWorld w, BlockPos p) {
      for (Direction d : DIRS) {
         if (w.getBlockState(p.offset(d)).getBlock() instanceof DoorBlock) {
            return true;
         }

         if (w.getBlockState(p.offset(d).up()).getBlock() instanceof DoorBlock) {
            return true;
         }
      }

      return w.getBlockState(p).getBlock() instanceof DoorBlock;
   }

   static BlockPos bed(Kingdoms.K k, Civ.Building b, ServerWorld w, VillagerEntity v, boolean place) {
      int beds = 0;
      Mutable m = new Mutable();

      for (int x = b.x0; x <= b.x1; x++) {
         for (int z = b.z0; z <= b.z1; z++) {
            for (int y = b.y0; y <= b.y0 + 3; y++) {
               m.set(x, y, z);
               BlockState s = w.getBlockState(m);
               if (s.getBlock() instanceof BedBlock && s.get(BedBlock.PART) == BedPart.HEAD) {
                  beds++;
                  BlockPos p = m.toImmutable();
                  if (!s.get(BedBlock.OCCUPIED) && !claimed(v, p)) {
                     return p;
                  }
               }
            }
         }
      }

      if (!place) {
         return null;
      } else {
         int max = b.kind == Civ.Kind.MANOR ? 4 : 2;
         if (beds >= max) {
            return null;
         } else {
            Block bedBlock = k.bed();

            for (int yx = b.y0; yx <= b.y0 + 2; yx++) {
               for (int x = b.x0 + 1; x <= b.x1 - 1; x++) {
                  for (int z = b.z0 + 1; z <= b.z1 - 1; z++) {
                     BlockPos f = new BlockPos(x, yx, z);
                     if (air(w, f) && air(w, f.up()) && solid(w, f.down()) && !nearDoor(w, f)) {
                        for (Direction d : DIRS) {
                           BlockPos h = f.offset(d);
                           if (h.getX() > b.x0
                              && h.getX() < b.x1
                              && h.getZ() > b.z0
                              && h.getZ() < b.z1
                              && air(w, h)
                              && air(w, h.up())
                              && solid(w, h.down())
                              && !nearDoor(w, h)) {
                              BlockState foot = bedBlock.getDefaultState().with(BedBlock.FACING, d).with(BedBlock.PART, BedPart.FOOT);
                              w.setBlockState(f, foot, 3);
                              w.setBlockState(h, foot.with(BedBlock.PART, BedPart.HEAD), 3);
                              return h;
                           }
                        }
                     }
                  }
               }
            }

            return null;
         }
      }
   }

   static boolean night(ServerWorld w, VillagerEntity v, BlockPos bed) {
      if (v.isSleeping()) {
         return true;
      } else if (on && isBed(w, bed)) {
         BlockState s = w.getBlockState(bed);
         if (s.get(BedBlock.OCCUPIED)) {
            return false;
         } else {
            double dx = v.getX() - (bed.getX() + 0.5);
            double dz = v.getZ() - (bed.getZ() + 0.5);
            double d = Math.sqrt(dx * dx + dz * dz);
            int t = TRIES.getOrDefault(v, 0) + 1;
            TRIES.put(v, t);
            if (d <= 1.7 && Math.abs(v.getY() - bed.getY()) < 1.6) {
               Civ.halt(v);
               v.sleep(bed);
               return true;
            } else if (t > 500) {
               v.requestTeleport(bed.getX() + 0.5, bed.getY() + 0.6, bed.getZ() + 0.5);
               v.sleep(bed);
               return true;
            } else {
               Civ.walk(v, bed);
               return true;
            }
         }
      } else {
         return false;
      }
   }

   static void wake(VillagerEntity v) {
      if (v.isSleeping()) {
         v.wakeUp();
      }

      CLAIM.remove(v);
      TRIES.remove(v);
   }

   public static void clear() {
      CLAIM.clear();
      TRIES.clear();
   }
}
