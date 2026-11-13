package com.vladik.godbox;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public final class Highways {
   public static boolean on = true;
   static final Set<String> DONE = new HashSet<>();
   static final List<Highways.Way> WAYS = new ArrayList<>();
   static final Map<Kingdoms.K, Integer> BUILT = new EnumMap<>(Kingdoms.K.class);
   static final String[] SIDE = new String[]{
      "Восточный", "Юго-восточный", "Южный", "Юго-западный", "Западный", "Северо-западный", "Северный", "Северо-восточный"
   };
   static final String[] NOUN = new String[]{"тракт", "большак", "королевский путь", "торговый путь", "шлях", "старый путь"};
   private static final int NONE = Integer.MIN_VALUE;
   private static final int LOOK = 20;
   private static final int BATCH = 6;
   private static int t;
   private static boolean registered;

   private Highways() {
   }

   public static void init() {
      if (!registered) {
         registered = true;
         SaveExt.register("roadnet", Highways::saveLines, Highways::loadLine, WAYS::clear);
      }
   }

   static BlockState road(int era, boolean edge, int i, Random r) {
      Block b;
      if (era <= 1) {
         b = edge ? Blocks.COARSE_DIRT : Blocks.DIRT_PATH;
      } else if (era <= 3) {
         b = edge ? Blocks.COBBLESTONE : (r.nextInt(4) == 0 ? Blocks.COARSE_DIRT : Blocks.GRAVEL);
      } else if (era <= 5) {
         b = edge ? Blocks.STONE_BRICKS : (r.nextInt(5) == 0 ? Blocks.MOSSY_COBBLESTONE : Blocks.COBBLESTONE);
      } else if (era <= 7) {
         b = edge
            ? Blocks.POLISHED_ANDESITE
            : (r.nextInt(6) == 0 ? (r.nextBoolean() ? Blocks.CRACKED_STONE_BRICKS : Blocks.MOSSY_STONE_BRICKS) : Blocks.STONE_BRICKS);
      } else if (era == 8) {
         b = edge ? Blocks.STONE_BRICKS : (i % 6 == 0 ? Blocks.CHISELED_STONE_BRICKS : Blocks.SMOOTH_STONE);
      } else if (era == 9) {
         b = edge ? Blocks.POLISHED_DEEPSLATE : (i % 9 == 0 ? Blocks.AMETHYST_BLOCK : Blocks.DEEPSLATE_TILES);
      } else {
         b = edge ? Blocks.POLISHED_BLACKSTONE_BRICKS : (i % 8 == 0 ? Blocks.WAXED_CUT_COPPER : Blocks.BRICKS);
      }

      return b.getDefaultState();
   }

   static BlockState fill(int era) {
      if (era < 4) {
         return Blocks.DIRT.getDefaultState();
      } else if (era < 9) {
         return Blocks.STONE_BRICKS.getDefaultState();
      } else {
         return era == 9 ? Blocks.DEEPSLATE_BRICKS.getDefaultState() : Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
      }
   }

   static BlockState bridge(int era, boolean edge) {
      if (era < 4) {
         return (edge ? Blocks.STRIPPED_SPRUCE_LOG : Blocks.SPRUCE_PLANKS).getDefaultState();
      } else if (era < 8) {
         return (edge ? Blocks.CHISELED_STONE_BRICKS : Blocks.STONE_BRICKS).getDefaultState();
      } else if (era == 8) {
         return (edge ? Blocks.QUARTZ_BRICKS : Blocks.SMOOTH_STONE).getDefaultState();
      } else {
         return era == 9
            ? (edge ? Blocks.POLISHED_DEEPSLATE : Blocks.DEEPSLATE_TILES).getDefaultState()
            : (edge ? Blocks.WAXED_CUT_COPPER : Blocks.POLISHED_BLACKSTONE_BRICKS).getDefaultState();
      }
   }

   static BlockState rail(int era) {
      if (era < 4) {
         return Blocks.SPRUCE_FENCE.getDefaultState();
      } else if (era < 8) {
         return Blocks.STONE_BRICK_WALL.getDefaultState();
      } else if (era == 8) {
         return Blocks.STONE_BRICK_WALL.getDefaultState();
      } else {
         return era == 9 ? Blocks.DEEPSLATE_BRICK_WALL.getDefaultState() : Blocks.IRON_BARS.getDefaultState();
      }
   }

   static BlockState pillar(int era) {
      if (era < 4) {
         return Blocks.SPRUCE_LOG.getDefaultState();
      } else if (era < 9) {
         return Blocks.STONE_BRICKS.getDefaultState();
      } else {
         return era == 9 ? Blocks.DEEPSLATE_BRICKS.getDefaultState() : Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
      }
   }

   static Block banner(Kingdoms.K k) {
      return k.banner();
   }

   static boolean paved(BlockState s) {
      return s.isOf(Blocks.POLISHED_ANDESITE)
         || s.isOf(Blocks.SMOOTH_STONE)
         || s.isOf(Blocks.CHISELED_STONE_BRICKS)
         || s.isOf(Blocks.DEEPSLATE_TILES)
         || s.isOf(Blocks.POLISHED_DEEPSLATE)
         || s.isOf(Blocks.AMETHYST_BLOCK)
         || s.isOf(Blocks.BRICKS)
         || s.isOf(Blocks.POLISHED_BLACKSTONE_BRICKS)
         || s.isOf(Blocks.SPRUCE_PLANKS)
         || s.isOf(Blocks.COARSE_DIRT)
         || s.isOf(Blocks.WAXED_CUT_COPPER)
         || s.isOf(Blocks.STRIPPED_SPRUCE_LOG);
   }

   static String name(BlockPos a, BlockPos b) {
      double ang = Math.atan2(b.getZ() - a.getZ(), b.getX() - a.getX());
      int sec = Math.floorMod((int)Math.round(ang / (Math.PI / 4)), 8);
      int h = Math.floorMod(a.getX() * 7 + b.getZ() * 13 + b.getX(), NOUN.length);
      return SIDE[sec] + " " + NOUN[h] + " к «" + Colonies.name(b) + "»";
   }

   static RoadNet.Terrain terrain(ServerWorld w, BlockPos a, BlockPos b, List<Civ.Building> bs, int margin) {
      return new RoadNet.Terrain() {
         @Override
         public int height(int x, int z) {
            if (!w.isChunkLoaded(x >> 4, z >> 4)) {
               return Integer.MIN_VALUE;
            } else {
               int g = TownBuilder.ground(w, x, z);
               return w.getBlockState(new BlockPos(x, g - 1, z)).getFluidState().isEmpty() ? g - 1 : g;
            }
         }

         @Override
         public double pen(int x, int z) {
            if (!w.isChunkLoaded(x >> 4, z >> 4)) {
               return 0.2;
            } else if (Math.max(Math.abs(x - a.getX()), Math.abs(z - a.getZ())) >= 14 && Math.max(Math.abs(x - b.getX()), Math.abs(z - b.getZ())) >= 14) {
               for (Civ.Building q : bs) {
                  if (x >= q.x0 - margin && x <= q.x1 + margin && z >= q.z0 - margin && z <= q.z1 + margin) {
                     return -1.0;
                  }
               }

               int g = TownBuilder.ground(w, x, z);
               BlockState s = w.getBlockState(new BlockPos(x, g - 1, z));
               if (s.isOf(Blocks.LAVA)) {
                  return -1.0;
               } else if (!s.getFluidState().isEmpty()) {
                  return 2.0;
               } else {
                  return !Roads.terrain(s) && !Roads.fam(s) ? 3.0 : 0.0;
               }
            } else {
               return 0.0;
            }
         }
      };
   }

   static Highways.Way plan(ServerWorld w, Kingdoms.K k, BlockPos a, BlockPos b, String key, boolean narrow) {
      double dx = b.getX() - a.getX();
      double dz = b.getZ() - a.getZ();
      double len = Math.sqrt(dx * dx + dz * dz);
      if (!(len < (narrow ? 20 : 30)) && !(len > 2500.0)) {
         List<int[]> wp = RoadNet.route(terrain(w, a, b, allBuildings(), narrow ? 3 : 4), a.getX(), a.getZ(), b.getX(), b.getZ(), 3000);
         if (wp == null) {
            wp = List.of(new int[]{a.getX(), a.getZ()}, new int[]{b.getX(), b.getZ()});
         }

         List<int[]> pts = RoadNet.smooth(wp);
         return pts.size() < 10 ? null : new Highways.Way(k, w, key, name(a, b), narrow, pts, Civ.city(k).era);
      } else {
         return null;
      }
   }

   static int raw(Highways.Way wy, int j) {
      if (wy.hy[j] != Integer.MIN_VALUE) {
         return wy.hy[j];
      } else {
         int x = wy.xs[j];
         int z = wy.zs[j];
         if (wy.w != null && wy.w.isChunkLoaded(x >> 4, z >> 4)) {
            int g = TownBuilder.ground(wy.w, x, z);
            boolean fl = !wy.w.getBlockState(new BlockPos(x, g - 1, z)).getFluidState().isEmpty();
            wy.wt[j] = fl;
            wy.hy[j] = fl ? g : g - 1;
            return wy.hy[j];
         } else {
            return Integer.MIN_VALUE;
         }
      }
   }

   static void ensureDeck(Highways.Way wy, int from, int to) {
      to = Math.min(wy.n, to);

      for (int j = Math.max(0, from - 20); j < Math.min(wy.n, to + 20); j++) {
         raw(wy, j);
      }

      int j = from;

      while (j < to) {
         if (wy.deck[j] == Integer.MIN_VALUE && wy.hy[j] != Integer.MIN_VALUE) {
            int e = j;

            while (e < to && wy.deck[e] == Integer.MIN_VALUE && wy.hy[e] != Integer.MIN_VALUE) {
               e++;
            }

            RoadNet.solveDeck(wy.hy, wy.wt, wy.deck, j, e, 20);
            j = e;
         } else {
            j++;
         }
      }
   }

   static void clearUp(ServerWorld w, Highways.Place pl, BlockPos p) {
      BlockState u = w.getBlockState(p);
      if (!u.isAir() && u.getFluidState().isEmpty()) {
         if (u.isReplaceable() || u.isIn(BlockTags.LEAVES) || u.isIn(BlockTags.LOGS) || u.isOf(Blocks.SNOW) || u.isIn(BlockTags.FLOWERS)) {
            pl.put(p, Blocks.AIR);
         }
      }
   }

   static List<Civ.Building> allBuildings() {
      List<Civ.Building> bs = new ArrayList<>();

      for (Kingdoms.K o : Kingdoms.K.values()) {
         if (o.alive) {
            bs.addAll(Civ.city(o).buildings);
         }
      }

      return bs;
   }

   static Set<Long> corridor(Highways.Way wy) {
      if (wy.corr != null) {
         return wy.corr;
      } else {
         Set<Long> c = new HashSet<>();
         int rc = (int)Math.ceil(wy.rad()) + 1;

         for (int j = 0; j < wy.n; j++) {
            for (int dx = -rc; dx <= rc; dx++) {
               for (int dz = -rc; dz <= rc; dz++) {
                  c.add(BlockPos.asLong(wy.xs[j] + dx, 0, wy.zs[j] + dz));
               }
            }
         }

         return wy.corr = c;
      }
   }

   static boolean free(Highways.Way wy, List<Civ.Building> bs, int x, int z) {
      return wy.w.isChunkLoaded(x >> 4, z >> 4)
         && !Roads.inBuilding(bs, x, z)
         && !corridor(wy).contains(BlockPos.asLong(x, 0, z))
         && TownBuilder.dry(wy.w, x, z);
   }

   static void pillarDown(ServerWorld w, Highways.Place pl, int era, int x, int y, int z) {
      int yy = y;

      for (int c = 0; c < 48 && yy > w.getBottomY(); c++) {
         BlockPos p = new BlockPos(x, yy, z);
         BlockState st = w.getBlockState(p);
         if (st.getFluidState().isEmpty() && !st.isAir() && !st.isReplaceable()) {
            break;
         }

         pl.put(p, pillar(era));
         yy--;
      }
   }

   static boolean pillarBlock(BlockState s) {
      return s.isOf(Blocks.SPRUCE_LOG) || s.isOf(Blocks.STONE_BRICKS) || s.isOf(Blocks.DEEPSLATE_BRICKS) || s.isOf(Blocks.POLISHED_BLACKSTONE_BRICKS);
   }

   static boolean railBlock(BlockState s) {
      return s.isAir() || s.getBlock() instanceof FenceBlock || s.getBlock() instanceof WallBlock || s.isOf(Blocks.IRON_BARS);
   }

   static boolean point(Highways.Way wy, int i, int era, Highways.Place pl, List<Civ.Building> bs) {
      ServerWorld w = wy.w;
      int x = wy.xs[i];
      int z = wy.zs[i];
      int skip = wy.narrow ? 8 : 12;
      byte target = (byte)(era + 1);
      if (Math.max(Math.abs(x - wy.xs[0]), Math.abs(z - wy.zs[0])) >= skip && Math.max(Math.abs(x - wy.xs[wy.n - 1]), Math.abs(z - wy.zs[wy.n - 1])) >= skip) {
         int deck = wy.deck[i];
         if (deck == Integer.MIN_VALUE) {
            return false;
         } else {
            boolean redo = wy.pe[i] > 0;
            boolean complete = true;
            boolean bridge = wy.wt[i];
            double R = wy.rad();
            int rc = (int)Math.ceil(R);
            Random r = w.getRandom();

            for (int oz = -rc; oz <= rc; oz++) {
               for (int ox = -rc; ox <= rc; ox++) {
                  double d = Math.sqrt(ox * ox + oz * oz);
                  if (!(d > R)) {
                     int cx = x + ox;
                     int cz = z + oz;
                     int own = i;
                     double best = d;

                     for (int j = Math.max(0, i - 6); j <= Math.min(wy.n - 1, i + 6); j++) {
                        if (j != i) {
                           double dj = Math.sqrt((cx - wy.xs[j]) * (cx - wy.xs[j]) + (cz - wy.zs[j]) * (cz - wy.zs[j]));
                           if (dj < best - 1.0E-9 || Math.abs(dj - best) < 1.0E-9 && j < own) {
                              best = dj;
                              own = j;
                           }
                        }
                     }

                     if (own == i) {
                        if (!w.isChunkLoaded(cx >> 4, cz >> 4)) {
                           complete = false;
                        } else if (!Roads.inBuilding(bs, cx, cz)) {
                           boolean edge = best > wy.lane();
                           BlockPos top = new BlockPos(cx, deck, cz);
                           if (redo) {
                              if (Roads.fam(w.getBlockState(top))) {
                                 if (bridge) {
                                    pl.put(top, bridge(era, edge));
                                    if (edge && railBlock(w.getBlockState(top.up()))) {
                                       pl.put(top.up(), rail(era));
                                    }

                                    int yy = deck - 1;

                                    for (int c = 0; c < 48 && yy > w.getBottomY(); c++) {
                                       BlockPos p = new BlockPos(cx, yy, cz);
                                       if (!pillarBlock(w.getBlockState(p))) {
                                          break;
                                       }

                                       pl.put(p, pillar(era));
                                       yy--;
                                    }
                                 } else {
                                    pl.put(top, wy.narrow ? Roads.mat(era, r) : road(era, edge, i, r));
                                 }
                              }
                           } else {
                              int g = TownBuilder.ground(w, cx, cz);
                              int sy = g - 1;
                              BlockState s = w.getBlockState(new BlockPos(cx, sy, cz));
                              if (!s.isOf(Blocks.LAVA)) {
                                 if (!s.getFluidState().isEmpty()) {
                                    bridge = true;
                                    pl.put(top, bridge(era, edge));

                                    for (int yy = deck + 1; yy <= deck + 3; yy++) {
                                       clearUp(w, pl, new BlockPos(cx, yy, cz));
                                    }

                                    if (edge) {
                                       pl.put(top.up(), rail(era));
                                    }

                                    if (i % 6 == 0 && (edge || d < 0.5) || deck - g > 2 && i % 2 == 0 && edge) {
                                       pillarDown(w, pl, era, cx, deck - 1, cz);
                                    }
                                 } else if ((!edge || !wy.narrow)
                                    && (Roads.terrain(s) || Roads.fam(s))
                                    && !s.hasBlockEntity()
                                    && deck - sy <= 20
                                    && sy - deck <= 14) {
                                    boolean ok = true;

                                    for (int yy = deck + 1; yy <= sy && ok; yy++) {
                                       BlockState u = w.getBlockState(new BlockPos(cx, yy, cz));
                                       ok = !u.hasBlockEntity()
                                          && (u.isAir() || u.isReplaceable() || Roads.terrain(u) || u.isIn(BlockTags.LEAVES) || u.isIn(BlockTags.LOGS));
                                    }

                                    if (ok) {
                                       for (int yy = sy + 1; yy < deck; yy++) {
                                          pl.put(new BlockPos(cx, yy, cz), fill(era));
                                       }

                                       for (int yy = deck + 1; yy <= sy; yy++) {
                                          pl.put(new BlockPos(cx, yy, cz), Blocks.AIR);
                                       }

                                       pl.put(top, wy.narrow ? Roads.mat(era, r) : road(era, edge, i, r));
                                       if (deck <= sy) {
                                          BlockState below = w.getBlockState(top.down());
                                          if (below.isAir() || !below.getFluidState().isEmpty() || below.isReplaceable()) {
                                             pl.put(top.down(), fill(era));
                                          }
                                       }

                                       for (int yy = deck + 1; yy <= deck + 4; yy++) {
                                          clearUp(w, pl, new BlockPos(cx, yy, cz));
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }

            if (complete) {
               decor(wy, i, era, pl, bs, redo);
               wy.pe[i] = target;
            }

            return complete;
         }
      } else {
         wy.pe[i] = target;
         return true;
      }
   }

   static void decor(Highways.Way wy, int i, int era, Highways.Place pl, List<Civ.Building> bs, boolean redo) {
      ServerWorld w = wy.w;
      int x = wy.xs[i];
      int z = wy.zs[i];
      int deck = wy.deck[i];
      double px = -wy.tz[i];
      double pz = wy.tx[i];
      if (wy.wt[i]) {
         if (!redo && i % 8 == 4) {
            for (int sd = -1; sd <= 1; sd += 2) {
               int cx = (int)Math.round(x + px * 2.0 * sd);
               int cz = (int)Math.round(z + pz * 2.0 * sd);
               if (w.isChunkLoaded(cx >> 4, cz >> 4) && !Roads.inBuilding(bs, cx, cz)) {
                  pl.put(new BlockPos(cx, deck + 2, cz), (era >= 4 ? (era == 9 ? Blocks.SOUL_LANTERN : Blocks.LANTERN) : Blocks.TORCH).getDefaultState());
               }
            }
         }
      } else if (wy.narrow) {
         if (era >= 2 && i % 16 == 8) {
            anchor(
               wy,
               wy.lampY,
               i,
               bs,
               x + px * 2.0 * (i / 16 % 2 == 0 ? 1 : -1),
               z + pz * 2.0 * (i / 16 % 2 == 0 ? 1 : -1),
               redo,
               (cxx, y, czx) -> roadLamp(pl, era, cxx, y, czx)
            );
         }
      } else {
         if (i % 14 == 0) {
            int sdx = i / 14 % 2 == 0 ? 1 : -1;
            anchor(wy, wy.lampY, i, bs, x + px * 3.5 * sdx, z + pz * 3.5 * sdx, redo, (cxx, y, czx) -> lamp(pl, era, cxx, y, czx));
         }

         if (i % 64 == 32) {
            int sdx = i / 64 % 2 == 0 ? 1 : -1;
            anchor(wy, wy.mileY, i, bs, x + px * 3.5 * sdx, z + pz * 3.5 * sdx, redo, (cxx, y, czx) -> milestone(pl, era, wy.k, cxx, y, czx));
         }

         if (!redo) {
            if (i % 22 == 11) {
               int sdx = i / 22 % 2 == 0 ? -1 : 1;
               int cx = (int)Math.round(x + px * 5.5 * sdx);
               int cz = (int)Math.round(z + pz * 5.5 * sdx);
               if (free(wy, bs, cx, cz)) {
                  tree(w, pl, wy.k, cx, cz);
               }
            }

            if (!wy.inn && wy.n >= 400 && i >= wy.n / 2) {
               for (int sdx = 1; sdx >= -1 && !wy.inn; sdx -= 2) {
                  int cx = (int)Math.round(x + px * 9.0 * sdx);
                  int cz = (int)Math.round(z + pz * 9.0 * sdx);
                  if (w.isChunkLoaded(cx >> 4, cz >> 4) && TownBuilder.dryArea(w, cx, cz, 4)) {
                     boolean clash = false;

                     for (int dx = -4; dx <= 4 && !clash; dx += 2) {
                        for (int dz = -4; dz <= 4; dz += 2) {
                           if (Roads.inBuilding(bs, cx + dx, cz + dz) || corridor(wy).contains(BlockPos.asLong(cx + dx, 0, cz + dz))) {
                              clash = true;
                              break;
                           }
                        }
                     }

                     if (!clash) {
                        inn(wy, pl, era, cx, cz, -px * sdx, -pz * sdx);
                        wy.inn = true;
                     }
                  }
               }

               if (i > wy.n / 2 + 120) {
                  wy.inn = true;
               }
            }
         }
      }
   }

   private static void anchor(Highways.Way wy, Map<Integer, Integer> map, int i, List<Civ.Building> bs, double fx, double fz, boolean redo, Highways.Put put) {
      int cx = (int)Math.round(fx);
      int cz = (int)Math.round(fz);
      Integer y = map.get(i);
      if (y != null) {
         if (redo && wy.w.isChunkLoaded(cx >> 4, cz >> 4)) {
            put.at(cx, y, cz);
         }
      } else if (free(wy, bs, cx, cz)) {
         int gy = TownBuilder.ground(wy.w, cx, cz);
         if (Math.abs(gy - wy.deck[i]) <= 2) {
            map.put(i, gy);
            put.at(cx, gy, cz);
         }
      }
   }

   static void lamp(Highways.Place pl, int era, int x, int y, int z) {
      BlockPos b = new BlockPos(x, y, z);
      Block post;
      Block light;
      int h;
      if (era <= 1) {
         post = Blocks.OAK_FENCE;
         light = Blocks.TORCH;
         h = 1;
      } else if (era <= 3) {
         post = Blocks.SPRUCE_FENCE;
         light = Blocks.LANTERN;
         h = 2;
      } else if (era <= 5) {
         post = Blocks.COBBLESTONE_WALL;
         light = Blocks.LANTERN;
         h = 2;
      } else if (era <= 8) {
         post = Blocks.STONE_BRICK_WALL;
         light = Blocks.LANTERN;
         h = 3;
      } else if (era == 9) {
         post = Blocks.DEEPSLATE_BRICK_WALL;
         light = Blocks.SOUL_LANTERN;
         h = 3;
      } else {
         post = Blocks.POLISHED_BLACKSTONE_WALL;
         light = Blocks.LANTERN;
         h = 3;
      }

      if (era >= 6) {
         pl.put(b, era >= 10 ? Blocks.WAXED_CUT_COPPER : Blocks.POLISHED_ANDESITE);
         b = b.up();
         h--;
      }

      for (int k = 0; k < h; k++) {
         pl.put(b.up(k), post);
      }

      pl.put(b.up(h), light);
      if (era >= 9) {
         pl.put(b.up(h + 1), era == 9 ? Blocks.AMETHYST_CLUSTER : Blocks.LIGHTNING_ROD);
      }
   }

   static void roadLamp(Highways.Place pl, int era, int x, int y, int z) {
      pl.put(new BlockPos(x, y, z), era >= 4 ? Blocks.STONE_BRICK_WALL : Blocks.OAK_FENCE);
      pl.put(new BlockPos(x, y + 1, z), era >= 4 ? Blocks.LANTERN : Blocks.TORCH);
   }

   static void milestone(Highways.Place pl, int era, Kingdoms.K k, int x, int y, int z) {
      BlockPos b = new BlockPos(x, y, z);
      Block base = era < 4
         ? Blocks.COBBLESTONE
         : (era < 9 ? Blocks.CHISELED_STONE_BRICKS : (era == 9 ? Blocks.CHISELED_DEEPSLATE : Blocks.CHISELED_POLISHED_BLACKSTONE));
      pl.put(b, base);
      pl.put(b.up(), banner(k));
   }

   static void tree(ServerWorld w, Highways.Place pl, Kingdoms.K k, int x, int z) {
      int g = TownBuilder.ground(w, x, z);
      if (w.getBlockState(new BlockPos(x, g - 1, z)).isIn(BlockTags.DIRT)) {
         for (int h = 0; h < 6; h++) {
            BlockState s = w.getBlockState(new BlockPos(x, g + h, z));
            if (!s.isAir() && !s.isReplaceable()) {
               return;
            }
         }

         Block log;
         Block leaf;
         switch (k) {
            case RED:
               log = Blocks.CHERRY_LOG;
               leaf = Blocks.CHERRY_LEAVES;
               break;
            case BLUE:
               log = Blocks.SPRUCE_LOG;
               leaf = Blocks.SPRUCE_LEAVES;
               break;
            case GREEN:
               log = Blocks.OAK_LOG;
               leaf = Blocks.OAK_LEAVES;
               break;
            case ORANGE:
            case BROWN:
               log = Blocks.ACACIA_LOG;
               leaf = Blocks.ACACIA_LEAVES;
               break;
            case PURPLE:
            case BLACK:
               log = Blocks.DARK_OAK_LOG;
               leaf = Blocks.DARK_OAK_LEAVES;
               break;
            case CYAN:
            case LIGHT_BLUE:
               log = Blocks.JUNGLE_LOG;
               leaf = Blocks.JUNGLE_LEAVES;
               break;
            case PINK:
               log = Blocks.CHERRY_LOG;
               leaf = Blocks.FLOWERING_AZALEA_LEAVES;
               break;
            default:
               log = Blocks.BIRCH_LOG;
               leaf = Blocks.BIRCH_LEAVES;
         }

         BlockState lv = leaf.getDefaultState().with(LeavesBlock.PERSISTENT, true);

         for (int hx = 0; hx < 4; hx++) {
            pl.put(new BlockPos(x, g + hx, z), log);
         }

         for (int y = g + 2; y <= g + 5; y++) {
            int rad = y <= g + 3 ? 2 : 1;

            for (int dx = -rad; dx <= rad; dx++) {
               for (int dz = -rad; dz <= rad; dz++) {
                  if ((Math.abs(dx) != rad || Math.abs(dz) != rad || rad <= 1)
                     && (y != g + 5 || Math.abs(dx) + Math.abs(dz) <= 1)
                     && (dx != 0 || dz != 0 || y >= g + 4)) {
                     BlockPos p = new BlockPos(x + dx, y, z + dz);
                     if (w.getBlockState(p).isAir()) {
                        pl.put(p, lv);
                     }
                  }
               }
            }
         }
      }
   }

   static void inn(Highways.Way wy, Highways.Place pl, int era, int cx, int cz, double ux, double uz) {
      ServerWorld w = wy.w;
      int gy = TownBuilder.ground(w, cx, cz);
      int[][] doors = new int[][]{{0, -3}, {0, 3}, {-3, 0}, {3, 0}};
      int[] door = doors[0];
      double best = -9.0;

      for (int[] d : doors) {
         double v = d[0] * ux + d[1] * uz;
         if (v > best) {
            best = v;
            door = d;
         }
      }

      for (int dx = -3; dx <= 3; dx++) {
         for (int dz = -3; dz <= 3; dz++) {
            int x = cx + dx;
            int z = cz + dz;

            for (int yy = gy - 2; yy >= gy - 5; yy--) {
               BlockPos p = new BlockPos(x, yy, z);
               BlockState st = w.getBlockState(p);
               if (st.isAir() || st.isReplaceable()) {
                  pl.put(p, st(era, Blocks.COBBLESTONE));
               }
            }

            boolean in = Math.abs(dx) < 3 && Math.abs(dz) < 3;
            pl.put(new BlockPos(x, gy - 1, z), in ? st(era, Blocks.SPRUCE_PLANKS) : st(era, Blocks.COBBLESTONE));

            for (int h = 0; h <= 6; h++) {
               pl.put(new BlockPos(x, gy + h, z), Blocks.AIR.getDefaultState());
            }

            boolean wall = !in;
            boolean corner = Math.abs(dx) == 3 && Math.abs(dz) == 3;
            if (wall) {
               for (int h = 0; h < 4; h++) {
                  BlockPos p = new BlockPos(x, gy + h, z);
                  boolean isDoor = dx == door[0] && dz == door[1] && h < 2;
                  if (!isDoor) {
                     boolean window = !corner
                        && h == 1
                        && (dx == 0 || dz == 0 || Math.abs(dx) == 1 && Math.abs(dz) == 3 || Math.abs(dz) == 1 && Math.abs(dx) == 3);
                     pl.put(p, corner ? st(era, Blocks.OAK_LOG) : (window ? st(era, Blocks.GLASS_PANE) : st(era, Blocks.OAK_PLANKS)));
                  }
               }
            }

            pl.put(new BlockPos(x, gy + 4, z), st(era, Blocks.DARK_OAK_PLANKS));
            if (Math.abs(dx) <= 2 && Math.abs(dz) <= 2) {
               pl.put(new BlockPos(x, gy + 5, z), st(era, Blocks.DARK_OAK_PLANKS));
            }

            if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
               pl.put(new BlockPos(x, gy + 6, z), st(era, Blocks.DARK_OAK_SLAB));
            }
         }
      }

      pl.put(new BlockPos(cx + 2, gy, cz + 2), Blocks.LANTERN);
      pl.put(new BlockPos(cx - 2, gy, cz - 2), Blocks.BARREL);
      pl.put(new BlockPos(cx - 2, gy, cz + 2), Blocks.CRAFTING_TABLE);
      pl.put(new BlockPos(cx + 2, gy, cz - 2), Blocks.SMOKER);
      pl.put(new BlockPos(cx - 2, gy, cz), Blocks.WHITE_BED);
      pl.put(new BlockPos(cx + 2, gy + 6, cz - 2), Blocks.CAMPFIRE);
      pl.put(new BlockPos(cx + door[0] + (int)Math.signum((float)door[0]), gy, cz + door[1] + (int)Math.signum((float)door[1])), Blocks.AIR);
      int hx = cx + door[0] + (door[1] != 0 ? 2 : (int)Math.signum((float)door[0]));
      int hz = cz + door[1] + (door[0] != 0 ? 2 : (int)Math.signum((float)door[1]));
      pl.put(new BlockPos(hx, gy, hz), Blocks.HAY_BLOCK);
      pl.put(new BlockPos(hx, gy + 1, hz), Blocks.LANTERN);
      Chronicle.say(w.getServer(), wy.k, "открывает постоялый двор на дороге «" + wy.name + "»");
   }

   private static BlockState st(int era, Block b) {
      return Eras.style(era, Civ.Kind.HOUSE, b.getDefaultState());
   }

   static ServerWorld world(Highways.Way wy) {
      if (wy.w == null && wy.k.world != null) {
         wy.w = wy.k.world;
      }

      return wy.w;
   }

   static void step(Highways.Way wy) {
      if (wy.last == null || wy.last.finished()) {
         int end = Math.min(wy.n, wy.i + 6);
         ensureDeck(wy, wy.i, end);
         Highways.Place pl = new Highways.Place(wy.w);

         for (List<Civ.Building> bs = allBuildings(); wy.i < end; wy.i++) {
            point(wy, wy.i, wy.era, pl, bs);
         }

         if (!pl.ps.isEmpty()) {
            Tasks.add(pl);
            wy.last = pl;
         }
      }
   }

   static void retry(Highways.Way wy) {
      if (wy.last == null || wy.last.finished()) {
         Highways.Place pl = new Highways.Place(wy.w);
         List<Civ.Building> bs = null;
         int tgt = wy.era + 1;
         int done = 0;

         for (int j = 0; j < wy.n && done < 60; j++) {
            if (wy.pe[j] < tgt && wy.w.isChunkLoaded(wy.xs[j] >> 4, wy.zs[j] >> 4)) {
               if (wy.deck[j] == Integer.MIN_VALUE) {
                  ensureDeck(wy, j, j + 6);
               }

               if (bs == null) {
                  bs = allBuildings();
               }

               point(wy, j, wy.era, pl, bs);
               done++;
            }
         }

         if (!pl.ps.isEmpty()) {
            Tasks.add(pl);
            wy.last = pl;
         }
      }
   }

   static boolean active(String key) {
      for (Highways.Way wy : WAYS) {
         if (wy.key.equals(key)) {
            return true;
         }
      }

      return false;
   }

   static boolean busy(Kingdoms.K k) {
      for (Highways.Way wy : WAYS) {
         if (wy.k == k && !wy.narrow && !wy.fin) {
            return true;
         }
      }

      return false;
   }

   static void forget(String key) {
      WAYS.removeIf(wy -> wy.key.equals(key) && wy.fin);
   }

   static boolean start(ServerWorld w, Kingdoms.K k, BlockPos a, BlockPos b, boolean narrow) {
      String key = Roads.key(a, b);
      if (active(key)) {
         return false;
      } else {
         Highways.Way wy = plan(w, k, a, b, key, narrow);
         if (wy == null) {
            return false;
         } else {
            WAYS.add(wy);
            if (!narrow) {
               Chronicle.say(w.getServer(), k, "начинает прокладывать «" + wy.name + "» (" + (int)Math.sqrt(a.getSquaredDistance(b)) + " блоков)");
            }

            return true;
         }
      }
   }

   static void finish(MinecraftServer s, Highways.Way wy) {
      if (!wy.narrow) {
         DONE.add(wy.key);
         BUILT.merge(wy.k, 1, Integer::sum);
         Chronicle.say(s, wy.k, "завершает «" + wy.name + "»");
      }
   }

   static BlockPos[] choose(Kingdoms.K k) {
      List<BlockPos> nodes = new ArrayList<>();
      nodes.add(k.capital);
      nodes.addAll(Conquest.towns(k));
      BlockPos[] best = null;
      double bd = Double.MAX_VALUE;

      for (int a = 1; a < nodes.size(); a++) {
         BlockPos p = nodes.get(a);
         BlockPos q = null;
         double qd = Double.MAX_VALUE;

         for (int b = 0; b < nodes.size(); b++) {
            if (b != a) {
               double d = p.getSquaredDistance(nodes.get(b));
               if (d < qd) {
                  qd = d;
                  q = nodes.get(b);
               }
            }
         }

         for (BlockPos o : new BlockPos[]{q, k.capital}) {
            if (o != null && !o.equals(p)) {
               double d = p.getSquaredDistance(o);
               if (!(d < 10000.0) && !(d > 4000000.0)) {
                  String key = Roads.key(o, p);
                  if (!DONE.contains(key) && !Roads.DONE.contains(key) && !active(key) && d < bd) {
                     bd = d;
                     best = new BlockPos[]{o, p};
                  }
               }
            }
         }
      }

      if (best != null) {
         return best;
      } else {
         for (Kingdoms.K ox : Kingdoms.K.values()) {
            if (ox != k && ox.alive && ox.capital != null && ox.world == k.world && (Diplomacy.rel(k, ox) == 2 || Vassals.lord(k) == ox)) {
               double d = k.capital.getSquaredDistance(ox.capital);
               if (!(d < 10000.0) && !(d > 4000000.0)) {
                  String key = Roads.key(k.capital, ox.capital);
                  if (!DONE.contains(key) && !Roads.DONE.contains(key) && !active(key)) {
                     return new BlockPos[]{k.capital, ox.capital};
                  }
               }
            }
         }

         return null;
      }
   }

   public static void tick(MinecraftServer s) {
      t++;
      if (t % 10 == 0) {
         for (Highways.Way wy : new ArrayList<>(WAYS)) {
            if (!wy.k.alive) {
               if (!wy.fin) {
                  WAYS.remove(wy);
               }
            } else if (world(wy) != null) {
               if (wy.i < wy.n) {
                  step(wy);
               } else if (!wy.done) {
                  wy.done = true;
                  wy.corr = null;
                  if (!wy.fin) {
                     wy.fin = true;
                     finish(s, wy);
                  }
               } else if (t % 200 == 0) {
                  retry(wy);
                  wy.corr = null;
               }
            }
         }
      }

      if (t % 600 == 0) {
         for (Highways.Way wyx : WAYS) {
            if (wyx.done && wyx.k.alive && wyx.k.world != null && Civ.city(wyx.k).era != wyx.era) {
               wyx.era = Civ.city(wyx.k).era;
               wyx.i = 0;
               wyx.done = false;
            }
         }
      }

      if (on && t % 3000 == 0) {
         for (Kingdoms.K k : Kingdoms.K.values()) {
            if (k.alive && k.capital != null) {
               ServerWorld c = k.world;
               if (c instanceof ServerWorld) {
                  Civ.City cx = Civ.city(k);
                  if ((cx.era >= 2 || Tech.has(k, Tech.T.WHEEL)) && cx.stone >= 20 && cx.wood >= 10 && !busy(k)) {
                     BlockPos[] pair = choose(k);
                     if (pair != null && start(c, k, pair[0], pair[1], false)) {
                        cx.stone -= 20;
                        cx.wood -= 10;
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
         BlockPos to = TownBuilder.landOr(w, Fx.surface(w, pos.getX(), pos.getZ()));
         if (k.capital.getSquaredDistance(to) < 900.0) {
            god.sendMessage(Text.literal("Слишком близко к столице — укажи точку подальше").formatted(Formatting.GRAY), true);
         } else {
            String key = Roads.key(k.capital, to);
            DONE.remove(key);
            forget(key);
            if (start(w, k, k.capital, to, false)) {
               god.sendMessage(Text.literal("\ud83d\udee3 " + k.title + ": тракт прокладывается сюда").formatted(k.color), true);
            } else {
               god.sendMessage(Text.literal("Этот тракт уже строится или слишком длинный").formatted(Formatting.GRAY), true);
            }
         }
      } else {
         god.sendMessage(Text.literal("Рядом нет королевства").formatted(Formatting.GRAY), true);
      }
   }

   static int count(Kingdoms.K k) {
      return BUILT.getOrDefault(k, 0);
   }

   public static void clear() {
      DONE.clear();
      WAYS.clear();
      BUILT.clear();
      t = 0;
   }

   private static String enc(int[] a) {
      StringBuilder sb = new StringBuilder();

      for (int v : a) {
         sb.append(v == Integer.MIN_VALUE ? "--" : String.format("%2s", Integer.toString(v + 64, 36)).replace(' ', '0'));
      }

      return sb.toString();
   }

   private static String encMap(Map<Integer, Integer> m) {
      if (m.isEmpty()) {
         return "-";
      } else {
         StringBuilder sb = new StringBuilder();

         for (Entry<Integer, Integer> e : m.entrySet()) {
            sb.append(sb.length() == 0 ? "" : ";").append(e.getKey()).append(':').append(e.getValue());
         }

         return sb.toString();
      }
   }

   private static void decMap(String s, Map<Integer, Integer> m) {
      if (!s.equals("-") && !s.isEmpty()) {
         for (String p : s.split(";")) {
            String[] kv = p.split(":");
            m.put(Integer.parseInt(kv[0]), Integer.parseInt(kv[1]));
         }
      }
   }

   static List<String> saveLines() {
      List<String> out = new ArrayList<>();

      for (Highways.Way wy : WAYS) {
         StringBuilder path = new StringBuilder(wy.xs[0] + "," + wy.zs[0] + ",");

         for (int j = 1; j < wy.n; j++) {
            path.append((wy.xs[j] - wy.xs[j - 1] + 1) * 3 + wy.zs[j] - wy.zs[j - 1] + 1);
         }

         StringBuilder pe = new StringBuilder();
         StringBuilder wt = new StringBuilder();

         for (int j = 0; j < wy.n; j++) {
            pe.append(Character.forDigit(wy.pe[j], 36));
            wt.append((char)(wy.wt[j] ? '1' : '0'));
         }

         out.add(
            "W|"
               + wy.key
               + "|"
               + wy.name.replace('|', '/')
               + "|"
               + wy.k.name()
               + "|"
               + (wy.narrow ? 1 : 0)
               + "|"
               + wy.era
               + "|"
               + (wy.done ? 1 : 0)
               + (wy.fin ? 1 : 0)
               + (wy.inn ? 1 : 0)
               + "|"
               + wy.i
               + "|"
               + path
               + "|"
               + enc(wy.deck)
               + "|"
               + pe
               + "|"
               + wt
               + "|"
               + encMap(wy.lampY)
               + "|"
               + encMap(wy.mileY)
         );
      }

      return out;
   }

   static void loadLine(String[] a) {
      try {
         if (a.length < 14 || !a[0].equals("W")) {
            return;
         }

         String[] head = a[8].split(",", 3);
         int x = Integer.parseInt(head[0]);
         int z = Integer.parseInt(head[1]);
         List<int[]> pts = new ArrayList<>();
         pts.add(new int[]{x, z});

         for (char ch : head[2].toCharArray()) {
            int d = ch - '0';
            x += d / 3 - 1;
            z += d % 3 - 1;
            pts.add(new int[]{x, z});
         }

         Highways.Way wy = new Highways.Way(Kingdoms.K.valueOf(a[3]), null, a[1], a[2], a[4].equals("1"), pts, Integer.parseInt(a[5]));
         if (a[9].length() != wy.n * 2 || a[10].length() != wy.n || a[11].length() != wy.n) {
            return;
         }

         wy.done = a[6].charAt(0) == '1';
         wy.fin = a[6].charAt(1) == '1';
         wy.inn = a[6].charAt(2) == '1';
         wy.i = Math.min(wy.n, Integer.parseInt(a[7]));

         for (int j = 0; j < wy.n; j++) {
            String c = a[9].substring(2 * j, 2 * j + 2);
            wy.deck[j] = c.equals("--") ? Integer.MIN_VALUE : Integer.parseInt(c, 36) - 64;
            wy.pe[j] = (byte)Character.digit(a[10].charAt(j), 36);
            wy.wt[j] = a[11].charAt(j) == '1';
         }

         decMap(a[12], wy.lampY);
         decMap(a[13], wy.mileY);
         WAYS.add(wy);
      } catch (RuntimeException var10) {
         GodBox.LOG.warn("GodBox: трасса не прочитана из сохранения: {}", var10.toString());
      }
   }

   static final class Place implements Tasks.Task {
      final ServerWorld w;
      final List<BlockPos> ps = new ArrayList<>();
      final List<BlockState> ss = new ArrayList<>();
      int i;
      int fix;

      Place(ServerWorld w) {
         this.w = w;
      }

      void put(BlockPos p, BlockState s) {
         this.ps.add(p);
         this.ss.add(s);
      }

      void put(BlockPos p, Block b) {
         this.put(p, b.getDefaultState());
      }

      boolean finished() {
         return this.i >= this.ps.size() && this.fix >= this.ps.size();
      }

      @Override
      public boolean tick() {
         try {
            return this.step();
         } catch (Exception var2) {
            GodBox.LOG.error("GodBox: сбой укладки дороги", (Throwable)var2);
            this.i = this.fix = this.ps.size();
            return true;
         }
      }

      private boolean step() {
         int n = 0;

         while (n < 40 && this.i < this.ps.size()) {
            BlockPos p = this.ps.get(this.i);
            if (this.w.isChunkLoaded(p.getX() >> 4, p.getZ() >> 4) && (!this.w.getBlockState(p).hasBlockEntity() || this.ss.get(this.i).hasBlockEntity())) {
               this.w.setBlockState(p, this.ss.get(this.i), 3);
            }

            n++;
            this.i++;
         }

         if (this.i < this.ps.size()) {
            return false;
         } else {
            for (int nx = 0; nx < 120 && this.fix < this.ps.size(); this.fix++) {
               BlockPos p = this.ps.get(this.fix);
               if (this.w.isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)) {
                  BlockState s = this.w.getBlockState(p);
                  Block b = s.getBlock();
                  if (b instanceof WallBlock || b instanceof FenceBlock || b instanceof PaneBlock) {
                     this.w.setBlockState(p, Block.postProcessState(s, this.w, p), 2);
                  }
               }

               nx++;
            }

            return this.fix >= this.ps.size();
         }
      }
   }

   private interface Put {
      void at(int var1, int var2, int var3);
   }

   static final class Way {
      final Kingdoms.K k;
      ServerWorld w;
      final String key;
      final String name;
      final boolean narrow;
      final int n;
      final int[] xs;
      final int[] zs;
      final int[] hy;
      final int[] deck;
      final boolean[] wt;
      final byte[] pe;
      final double[] tx;
      final double[] tz;
      final Map<Integer, Integer> lampY = new HashMap<>();
      final Map<Integer, Integer> mileY = new HashMap<>();
      int i;
      int era;
      boolean inn;
      boolean done;
      boolean fin;
      Highways.Place last;
      Set<Long> corr;

      Way(Kingdoms.K k, ServerWorld w, String key, String name, boolean narrow, List<int[]> pts, int era) {
         this.k = k;
         this.w = w;
         this.key = key;
         this.name = name;
         this.narrow = narrow;
         this.era = era;
         this.n = pts.size();
         this.xs = new int[this.n];
         this.zs = new int[this.n];
         this.hy = new int[this.n];
         this.deck = new int[this.n];
         this.wt = new boolean[this.n];
         this.pe = new byte[this.n];
         this.tx = new double[this.n];
         this.tz = new double[this.n];

         for (int j = 0; j < this.n; j++) {
            this.xs[j] = pts.get(j)[0];
            this.zs[j] = pts.get(j)[1];
            this.hy[j] = Integer.MIN_VALUE;
            this.deck[j] = Integer.MIN_VALUE;
         }

         for (int j = 0; j < this.n; j++) {
            int a = Math.max(0, j - 3);
            int b = Math.min(this.n - 1, j + 3);
            double dx = this.xs[b] - this.xs[a];
            double dz = this.zs[b] - this.zs[a];
            double l = Math.sqrt(dx * dx + dz * dz);
            if (l < 1.0E-6) {
               dx = 1.0;
               dz = 0.0;
               l = 1.0;
            }

            this.tx[j] = dx / l;
            this.tz[j] = dz / l;
         }
      }

      double rad() {
         return this.narrow ? 2.0 : 2.7;
      }

      double lane() {
         return this.narrow ? 1.5 : 1.7;
      }
   }
}
