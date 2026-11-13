package com.vladik.godbox;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

final class Props {
   private static final List<Props.Prop> ALL = new ArrayList<>();

   private Props() {
   }

   static void init() {
      SaveExt.register("prop", Props::save, Props::load, ALL::clear);
   }

   static void clear() {
      ALL.clear();
   }

   static Props.Prop add(Props.Kind kind, Kingdoms.K k, int x, int y, int z, int arg, int era) {
      ALL.removeIf(px -> px.kind == kind && px.x == x && px.y == y && px.z == z);
      Props.Prop p = new Props.Prop(kind, k, x, y, z, arg, era);
      p.busy = true;
      ALL.add(p);
      return p;
   }

   static boolean has(Props.Prop p) {
      return ALL.contains(p);
   }

   static List<Props.Prop> of(Kingdoms.K k) {
      List<Props.Prop> out = new ArrayList<>();

      for (Props.Prop p : ALL) {
         if (p.k == k) {
            out.add(p);
         }
      }

      return out;
   }

   static void markStale(Kingdoms.K k) {
      for (Props.Prop p : ALL) {
         if (p.k == k && !p.busy) {
            p.era = -1;
            p.retryAt = 0L;
         }
      }
   }

   static void adopt(ServerWorld w) {
      for (Props.Prop p : ALL) {
         if (p.k == null || !p.k.alive || p.k.world != w) {
            Kingdoms.K best = null;
            double bd = Double.MAX_VALUE;

            for (Kingdoms.K o : Kingdoms.K.values()) {
               if (o.alive && o.world == w && o.capital != null) {
                  double d = Math.min(dist(o.capital, p), nearestTown(o, p));
                  if (d < bd) {
                     bd = d;
                     best = o;
                  }
               }
            }

            if (best != null && bd < 6400.0) {
               if (p.k != best) {
                  p.era = -1;
                  p.retryAt = 0L;
               }

               p.k = best;
            }
         }
      }
   }

   static void transfer(Kingdoms.K from, Kingdoms.K to, BlockPos town, int r) {
      for (Props.Prop p : ALL) {
         if (p.k == from) {
            double dx = p.x - town.getX();
            double dz = p.z - town.getZ();
            if (!(dx * dx + dz * dz > r * r)) {
               p.k = to;
               p.era = -1;
               p.retryAt = 0L;
            }
         }
      }
   }

   private static double dist(BlockPos c, Props.Prop p) {
      double dx = c.getX() - p.x;
      double dz = c.getZ() - p.z;
      return dx * dx + dz * dz;
   }

   private static double nearestTown(Kingdoms.K k, Props.Prop p) {
      double best = Double.MAX_VALUE;
      List<BlockPos> ts = Conquest.TOWNS.get(k);
      if (ts != null) {
         for (BlockPos t : new ArrayList<>(ts)) {
            best = Math.min(best, dist(t, p));
         }
      }

      return best;
   }

   static TownBuilder.Job blueprint(ServerWorld w, Props.Prop p, int era) {
      TownBuilder.Job j = new TownBuilder.Job(w);
      j.dry = true;
      build(j, p, era);
      return j;
   }

   static void build(TownBuilder.Job j, Props.Prop p, int era) {
      switch (p.kind) {
         case WELL:
            TownBuilder.well(j, era, p.x, p.z, p.y);
            break;
         case STALL:
            TownBuilder.stall(j, p.k, p.x, p.z, p.y, p.arg);
            break;
         case LAMP:
            TownBuilder.lamp(j, era, p.x, p.y, p.z);
            break;
         case PIER:
            Ships.pier(j, p.k, era, new BlockPos(p.x, p.y, p.z), Direction.fromHorizontal(p.arg));
            break;
         case MARKET:
            market(j, p, era);
      }
   }

   private static void market(TownBuilder.Job j, Props.Prop p, int era) {
      Block floor = Eras.style(era, Civ.Kind.HOUSE, Blocks.STONE_BRICKS.getDefaultState()).getBlock();
      Block post = Eras.style(era, Civ.Kind.HOUSE, Blocks.OAK_FENCE.getDefaultState()).getBlock();
      int x = p.x;
      int y = p.y;
      int z = p.z;

      for (int dx = -3; dx <= 3; dx++) {
         for (int dz = -3; dz <= 3; dz++) {
            j.put(new BlockPos(x + dx, y - 1, z + dz), floor.getDefaultState());

            for (int h = 0; h <= 4; h++) {
               j.put(new BlockPos(x + dx, y + h, z + dz), Blocks.AIR.getDefaultState());
            }

            if (Math.abs(dx) >= 1) {
               j.put(new BlockPos(x + dx, y + 3, z + dz), ((dz & 1) == 0 ? p.k.wool : Blocks.WHITE_WOOL).getDefaultState());
            }
         }
      }

      for (int px : new int[]{-3, -1, 1, 3}) {
         for (int pz : new int[]{-3, 3}) {
            for (int h = 0; h < 3; h++) {
               j.put(new BlockPos(x + px, y + h, z + pz), post.getDefaultState());
            }
         }
      }

      j.put(new BlockPos(x - 2, y, z - 2), Blocks.BARREL.getDefaultState());
      j.put(new BlockPos(x - 2, y, z + 1), Blocks.HAY_BLOCK.getDefaultState());
      j.put(new BlockPos(x + 2, y, z - 1), Blocks.BARREL.getDefaultState());
      j.put(new BlockPos(x + 2, y, z + 2), Blocks.COMPOSTER.getDefaultState());
      j.put(new BlockPos(x + 2, y, z), Blocks.SMITHING_TABLE.getDefaultState());
      j.put(new BlockPos(x - 2, y, z), Blocks.LOOM.getDefaultState());
      j.put(new BlockPos(x, y, z), Blocks.BELL.getDefaultState());
      j.put(new BlockPos(x, y + 4, z), Blocks.LANTERN.getDefaultState());
   }

   private static List<String> save() {
      List<String> out = new ArrayList<>();

      for (Props.Prop p : ALL) {
         if (!p.busy && p.k != null) {
            out.add(p.kind.name() + "|" + p.k.name() + "|" + p.x + "|" + p.y + "|" + p.z + "|" + p.arg + "|" + p.era);
         }
      }

      return out;
   }

   private static void load(String[] a) {
      if (a.length >= 7) {
         try {
            ALL.add(
               new Props.Prop(
                  Props.Kind.valueOf(a[0]),
                  Kingdoms.K.valueOf(a[1]),
                  Integer.parseInt(a[2]),
                  Integer.parseInt(a[3]),
                  Integer.parseInt(a[4]),
                  Integer.parseInt(a[5]),
                  Integer.parseInt(a[6])
               )
            );
         } catch (IllegalArgumentException var2) {
            GodBox.LOG.warn("GodBox: запись постройки пропущена: {}", String.join("|", a));
         }
      }
   }

   static enum Kind {
      WELL,
      STALL,
      LAMP,
      PIER,
      MARKET;
   }

   static final class Prop {
      final Props.Kind kind;
      Kingdoms.K k;
      final int x;
      final int y;
      final int z;
      final int arg;
      int era;
      boolean busy;
      long retryAt;

      Prop(Props.Kind kind, Kingdoms.K k, int x, int y, int z, int arg, int era) {
         this.kind = kind;
         this.k = k;
         this.x = x;
         this.y = y;
         this.z = z;
         this.arg = arg;
         this.era = era;
      }
   }
}
