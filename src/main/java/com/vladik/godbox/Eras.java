package com.vladik.godbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BellBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.LightningRodBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TransparentBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;

public final class Eras {
   public static final String[] NAMES = new String[]{
      "Первобытная эпоха",
      "Каменный век",
      "Бронзовый век",
      "Железный век",
      "Античность",
      "Раннее Средневековье",
      "Эпоха замков",
      "Возрождение",
      "Имперская эпоха",
      "Эпоха Просвещения",
      "Эпоха пара"
   };
   public static final int CASTLE = 6;
   private static final Map<Block, Block[]> MAP = new HashMap<>();
   private static int forced = -1;
   private static Set<Block> known;
   private static final Set<Civ.Building> RUNNING;
   private static final Set<Kingdoms.K> BUSY;
   private static final Map<String, int[]> PENDING;
   private static final Map<Long, Integer> PENDING_TOWN;
   private static final Map<Long, Long> TOWN_RETRY;
   private static final Map<Kingdoms.K, Integer> GEN;
   private static int timer;
   private static int guessedAt;
   private static final int RADIUS = 11;
   private static final int GUESS = -3;
   private static final int SAMPLES = 48;

   private Eras() {
   }

   static int eraOf(Kingdoms.K k) {
      return forced >= 0 ? forced : Civ.city(k).era;
   }

   static boolean known(BlockState s) {
      if (known == null) {
         Set<Block> t = new HashSet<>(MAP.keySet());

         for (Block[] a : MAP.values()) {
            for (Block b : a) {
               if (b != null) {
                  t.add(b);
               }
            }
         }

         known = t;
      }

      return known.contains(s.getBlock());
   }

   private static Block[] of(Block... b) {
      return b;
   }

   private static void row(Block[] from, Block... to) {
      for (Block f : from) {
         MAP.put(f, to);
      }
   }

   static BlockState copy(BlockState from, Block to) {
      BlockState s = to.getDefaultState();

      for (Property p : from.getProperties()) {
         if (s.contains(p)) {
            s = s.with(p, from.get(p));
         }
      }

      return s;
   }

   static BlockState style(int era, Civ.Kind kind, BlockState s) {
      Block[] to = MAP.get(s.getBlock());
      if (to == null) {
         return s;
      } else if (kind != Civ.Kind.MILL || !s.isOf(Blocks.OAK_LOG) && !s.isOf(Blocks.OAK_PLANKS)) {
         Block t = to[Math.max(0, Math.min(to.length - 1, era))];
         return t == null ? s : copy(s, t);
      } else {
         return s;
      }
   }

   static void apply(Kingdoms.K k, Civ.Kind kind, List<BlockState> ss) {
      int e = eraOf(k);

      for (int i = 0; i < ss.size(); i++) {
         ss.set(i, Races.style(k, style(e, kind, ss.get(i))));
      }
   }

   static void decor(Kingdoms.K k, Civ.Building b, Civ.Project p) {
      int e = eraOf(k);
      int y0 = b.y0;
      if (b.kind == Civ.Kind.HOUSE) {
         int top = y0 + 3 + Arch.gableHeight(b.x0, b.z0, b.x1, b.z1);
         if (e >= 4) {
            BlockState l = Arch.hanging(Blocks.LANTERN);
            p.add(new BlockPos(b.x0 - 1, y0 + 2, b.z0 - 1), l);
            p.add(new BlockPos(b.x1 + 1, y0 + 2, b.z0 - 1), l);
            p.add(new BlockPos(b.x0 - 1, y0 + 2, b.z1 + 1), l);
            p.add(new BlockPos(b.x1 + 1, y0 + 2, b.z1 + 1), l);
         }

         if (e >= 10) {
            p.add(new BlockPos(b.x0 - 1, top + 1, b.cz()), Blocks.LIGHTNING_ROD.getDefaultState());
            p.add(new BlockPos(b.x1 + 1, top + 1, b.cz()), Blocks.LIGHTNING_ROD.getDefaultState());
         }
      } else if (b.kind == Civ.Kind.TOWER && e >= 8) {
         p.add(new BlockPos(b.cx(), y0 + 7, b.cz()), (e == 9 ? Blocks.LANTERN : Blocks.BELL).getDefaultState());
      } else if (b.kind == Civ.Kind.BARRACKS && e >= 6) {
         p.add(new BlockPos(b.x0, y0 + 6, b.z0), Blocks.LANTERN.getDefaultState());
         p.add(new BlockPos(b.x1, y0 + 6, b.z1), Blocks.LANTERN.getDefaultState());
      }
   }

   static int need(Civ.City c) {
      return c.era + 1;
   }

   static boolean ready(Civ.City c, int vill, int built, int sage) {
      int n = need(c);
      return n >= NAMES.length
         ? false
         : vill >= Math.min(32, 3 + 3 * n) - sage * 2
            && built >= 2 + 3 * n - sage * 2
            && c.wood >= 10 * n
            && c.stone >= 8 * n
            && c.ore >= 3 * (n - 1)
            && c.food >= 4 * n;
   }

   static void pay(Civ.City c) {
      int n = need(c);
      c.wood -= 6 * n;
      c.stone -= 5 * n;
      c.ore -= 2 * (n - 1);
   }

   static Item axe(int e) {
      if (e <= 0) {
         return Items.WOODEN_AXE;
      } else if (e == 1) {
         return Items.STONE_AXE;
      } else if (e == 2) {
         return Items.GOLDEN_AXE;
      } else if (e <= 5) {
         return Items.IRON_AXE;
      } else {
         return e <= 7 ? Items.DIAMOND_AXE : Items.NETHERITE_AXE;
      }
   }

   private static boolean skip(BlockState s) {
      Block b = s.getBlock();
      return s.isAir() || !s.getFluidState().isEmpty() || s.hasBlockEntity() || b instanceof BedBlock || b instanceof CropBlock || b instanceof DoorBlock;
   }

   private static int gen(Kingdoms.K k) {
      return GEN.getOrDefault(k, 0);
   }

   static void init() {
      Props.init();
      SaveExt.register("bera", Eras::saveBuildings, Eras::loadBuilding, Eras::reset);
      SaveExt.register("tera", Eras::saveTowns, Eras::loadTown, () -> {});
   }

   static void clear() {
      Props.clear();
      TownBuilder.clearBuilds();
      RUNNING.clear();
      BUSY.clear();
      TOWN_RETRY.clear();
      timer = 0;
   }

   private static void reset() {
      PENDING.clear();
      PENDING_TOWN.clear();
      TownBuilder.TOWN_ERA.clear();
   }

   private static String key(Civ.Building b) {
      return b.kind.name() + "|" + b.x0 + "|" + b.z0 + "|" + b.y0;
   }

   static void resolve(Civ.Building b) {
      if (b.era == -2) {
         int[] r = PENDING.remove(key(b));
         if (r != null) {
            b.era = r[0];
            b.rev = r[1];
         } else {
            b.era = -3;
         }
      }
   }

   static boolean stale(Civ.Building b, Civ.City c) {
      if (b.kind != Civ.Kind.QUARRY && b.kind != Civ.Kind.FARM) {
         resolve(b);
         return b.era != c.era;
      } else {
         return false;
      }
   }

   private static boolean unknown(Civ.Building b) {
      return b.era == -3;
   }

   private static List<String> saveBuildings() {
      List<String> out = new ArrayList<>();

      for (Kingdoms.K k : Kingdoms.K.values()) {
         if (k.alive && k.world != null && k.capital != null) {
            for (Civ.Building b : new ArrayList<>(Civ.city(k).buildings)) {
               if (b.done && b.kind != Civ.Kind.QUARRY) {
                  resolve(b);
                  out.add(key(b) + "|" + b.era + "|" + b.rev);
               }
            }
         }
      }

      return out;
   }

   private static void loadBuilding(String[] a) {
      if (a.length >= 6) {
         PENDING.put(a[0] + "|" + a[1] + "|" + a[2] + "|" + a[3], new int[]{Integer.parseInt(a[4]), Integer.parseInt(a[5])});
      }
   }

   private static List<String> saveTowns() {
      List<String> out = new ArrayList<>();

      for (Kingdoms.K k : Kingdoms.K.values()) {
         List<BlockPos> ts = Conquest.TOWNS.get(k);
         if (ts != null) {
            for (BlockPos t : ts) {
               out.add(t.asLong() + "|" + townEra(t.asLong()));
            }
         }
      }

      return out;
   }

   private static void loadTown(String[] a) {
      if (a.length >= 2) {
         PENDING_TOWN.put(Long.parseLong(a[0]), Integer.parseInt(a[1]));
      }
   }

   private static int townEra(long key) {
      Integer te = TownBuilder.TOWN_ERA.get(key);
      if (te == null) {
         Integer pe = PENDING_TOWN.remove(key);
         te = pe != null ? pe : -1;
         TownBuilder.TOWN_ERA.put(key, te);
      }

      return te;
   }

   static void rebuild(Kingdoms.K k, Civ.City c, ServerWorld w) {
      if (w != null) {
         GEN.merge(k, 1, Integer::sum);
         boolean any = false;

         for (Civ.Building b : c.buildings) {
            if (b.done && b.kind != Civ.Kind.QUARRY && b.kind != Civ.Kind.FARM) {
               resolve(b);
               if (b.era == -3) {
                  b.retryAt = 0L;
                  any = true;
               } else {
                  b.era = -1;
                  b.retryAt = 0L;
                  any = true;
               }
            }
         }

         Props.markStale(k);
         List<BlockPos> ts = Conquest.TOWNS.get(k);
         if (ts != null) {
            for (BlockPos t : ts) {
               TownBuilder.TOWN_ERA.put(t.asLong(), -1);
               TOWN_RETRY.remove(t.asLong());
               any = true;
            }
         }

         if (any) {
            Chronicle.say(w.getServer(), k, "перестраивает города в стиле эпохи: " + NAMES[Math.min(NAMES.length - 1, c.era)]);
         }
      }
   }

   static void tick(MinecraftServer s) {
      if (++timer % 10 == 0) {
         boolean adopt = timer % 200 == 0;

         for (Kingdoms.K k : Kingdoms.K.values()) {
            if (k.alive && k.world != null && k.capital != null) {
               ServerWorld w = k.world;
               if (adopt) {
                  Props.adopt(w);
               }

               if (!BUSY.contains(k)) {
                  Civ.City c = Civ.city(k);
                  boolean started = false;

                  for (Civ.Building b : new ArrayList<>(c.buildings)) {
                     if (b.done) {
                        TownBuilder.fit(b);
                        if (!b.busy
                           && b.kind != Civ.Kind.QUARRY
                           && b.kind != Civ.Kind.FARM
                           && stale(b, c)
                           && !RUNNING.contains(b)
                           && b.retryAt <= w.getTime()
                           && loadedAround(w, b.cx(), b.cz(), 11)) {
                           if (unknown(b)) {
                              if (guessedAt == timer) {
                                 break;
                              }

                              guessedAt = timer;

                              try {
                                 b.era = guess(k, c, w, b);
                              } catch (Exception var14) {
                                 GodBox.LOG.error("GodBox: не удалось определить эпоху здания", (Throwable)var14);
                                 b.era = c.era;
                              }
                           } else {
                              restyle(k, c, w, b);
                           }

                           started = true;
                           break;
                        }
                     }
                  }

                  if (!started) {
                     List<BlockPos> ts = Conquest.TOWNS.get(k);
                     if (ts != null) {
                        for (BlockPos t : new ArrayList<>(ts)) {
                           long tk = t.asLong();
                           if (townEra(tk) != c.era
                              && TOWN_RETRY.getOrDefault(tk, 0L) <= w.getTime()
                              && loadedAround(w, t.getX(), t.getZ(), 22)
                              && restyleTown(k, c, w, t)) {
                              started = true;
                              break;
                           }
                        }
                     }

                     if (!started) {
                        for (Props.Prop p : Props.of(k)) {
                           if (!p.busy && p.era != c.era && p.retryAt <= w.getTime() && loadedAround(w, p.x, p.z, 12) && restyleProp(k, c, w, p)) {
                              break;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean loadedAround(ServerWorld w, int x, int z, int r) {
      return w.isChunkLoaded(x - r >> 4, z - r >> 4)
         && w.isChunkLoaded(x + r >> 4, z - r >> 4)
         && w.isChunkLoaded(x - r >> 4, z + r >> 4)
         && w.isChunkLoaded(x + r >> 4, z + r >> 4);
   }

   private static boolean accent(BlockState s) {
      Block b = s.getBlock();
      return b instanceof TransparentBlock
         || b instanceof PaneBlock
         || b instanceof LanternBlock
         || b instanceof BellBlock
         || b instanceof LightningRodBlock
         || b instanceof TrapdoorBlock;
   }

   private static Eras.Plan diff(ServerWorld w, Map<BlockPos, BlockState> want, int cx, int cz, int rad, Predicate<BlockState> may) {
      Eras.Plan pl = new Eras.Plan();

      for (Entry<BlockPos, BlockState> e : want.entrySet()) {
         BlockPos p = e.getKey();
         BlockState s = e.getValue();
         if (!s.isAir() && !skip(s)) {
            if (!w.isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)) {
               if (Math.abs(p.getX() - cx) <= rad && Math.abs(p.getZ() - cz) <= rad) {
                  pl.complete = false;
               }
            } else {
               BlockState cur = w.getBlockState(p);
               if (cur.getBlock() != s.getBlock()) {
                  if (cur.isAir()) {
                     if (accent(s)) {
                        pl.ch.add(new Eras.Ch(p, s));
                     }
                  } else if (!skip(cur) && may.test(cur)) {
                     pl.ch.add(new Eras.Ch(p, s));
                  }
               }
            }
         }
      }

      pl.ch.sort(Comparator.comparingInt(c -> c.pos().getY()));
      return pl;
   }

   private static boolean coversLoaded(ServerWorld w, Set<BlockPos> ps) {
      if (ps.isEmpty()) {
         return true;
      } else {
         int x0 = Integer.MAX_VALUE;
         int x1 = Integer.MIN_VALUE;
         int z0 = Integer.MAX_VALUE;
         int z1 = Integer.MIN_VALUE;

         for (BlockPos p : ps) {
            x0 = Math.min(x0, p.getX());
            x1 = Math.max(x1, p.getX());
            z0 = Math.min(z0, p.getZ());
            z1 = Math.max(z1, p.getZ());
         }

         for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
            for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
               if (!w.isChunkLoaded(cx, cz)) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   private static Map<BlockPos, BlockState> blueprintMap(Kingdoms.K k, ServerWorld w, Civ.Building b, int era) {
      int prev = forced;
      forced = era;

      Object var14;
      try {
         List<BlockPos> bp;
         List<BlockState> bs;
         if (b.kind == Civ.Kind.CASTLE) {
            bp = new ArrayList<>();
            bs = new ArrayList<>();
            Ages.plan(k, b, w, bp, bs);
            apply(k, b.kind, bs);
         } else {
            TownBuilder.Job tj = TownBuilder.houseBlueprint(k, b, w, era);
            if (tj != null) {
               bp = tj.ps;
               bs = tj.ss;
            } else {
               Civ.Project p = Civ.blueprint(k, b, w);
               bp = p.pos;
               bs = p.st;
            }
         }

         Map<BlockPos, BlockState> want = new LinkedHashMap<>();

         for (int i = 0; i < bp.size(); i++) {
            want.put(bp.get(i), bs.get(i));
         }

         var14 = want;
      } finally {
         forced = prev;
      }

      return (Map<BlockPos, BlockState>)var14;
   }

   private static Block blockAt(Map<BlockPos, BlockState> m, BlockPos p) {
      BlockState s = m.get(p);
      return s == null ? Blocks.AIR : s.getBlock();
   }

   private static int guess(Kingdoms.K k, Civ.City c, ServerWorld w, Civ.Building b) {
      int cur = c.era;
      int n = NAMES.length;
      List<Map<BlockPos, BlockState>> maps = new ArrayList<>();

      for (int e = 0; e < n; e++) {
         maps.add(blueprintMap(k, w, b, e));
      }

      Set<BlockPos> all = new LinkedHashSet<>(maps.get(Math.min(cur, n - 1)).keySet());

      for (Map<BlockPos, BlockState> m : maps) {
         all.addAll(m.keySet());
      }

      List<BlockPos> sig = new ArrayList<>();

      for (BlockPos p : all) {
         if (w.isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)) {
            Block first = blockAt(maps.get(0), p);
            boolean differs = false;

            for (int e = 1; e < n && !differs; e++) {
               differs = blockAt(maps.get(e), p) != first;
            }

            if (differs) {
               sig.add(p);
            }
         }
      }

      int step = Math.max(1, sig.size() / 48);
      int[] score = new int[n];
      int total = 0;

      for (int i = 0; i < sig.size(); i += step) {
         BlockPos px = sig.get(i);
         Block have = w.getBlockState(px).getBlock();
         total++;

         for (int e = 0; e < n; e++) {
            if (blockAt(maps.get(e), px) == have) {
               score[e]++;
            }
         }
      }

      if (total < 4) {
         return cur;
      } else {
         int best = 0;

         for (int ex = 0; ex < n; ex++) {
            best = Math.max(best, score[ex]);
         }

         if (score[Math.min(cur, n - 1)] != best && best * 2 >= total) {
            int pick = -1;

            for (int ex = 0; ex < n; ex++) {
               if (score[ex] == best && (pick < 0 || Math.abs(ex - cur) < Math.abs(pick - cur))) {
                  pick = ex;
               }
            }

            return pick;
         } else {
            return cur;
         }
      }
   }

   private static void restyle(Kingdoms.K k, Civ.City c, ServerWorld w, Civ.Building b) {
      int era = c.era;
      Map<BlockPos, BlockState> want = blueprintMap(k, w, b, era);
      Eras.Plan pl = diff(w, want, b.cx(), b.cz(), 11, cur -> true);
      if (!coversLoaded(w, want.keySet())) {
         b.retryAt = w.getTime() + 200L;
      } else {
         RUNNING.add(b);
         BUSY.add(k);
         int g = gen(k);
         schedule(w, pl, () -> c.buildings.contains(b), ok -> {
            RUNNING.remove(b);
            BUSY.remove(k);
            if (ok) {
               b.era = g == gen(k) ? era : -1;
               Repair.update(b, want);
            } else {
               b.retryAt = w.getTime() + 200L;
            }
         });
      }
   }

   private static boolean propMay(BlockState s) {
      return known(s) || s.isIn(BlockTags.WOOL) || s.isOf(Blocks.TORCH) || s.isOf(Blocks.LANTERN) || s.getBlock() instanceof FenceBlock;
   }

   private static boolean restyleProp(Kingdoms.K k, Civ.City c, ServerWorld w, Props.Prop p) {
      int era = c.era;
      TownBuilder.Job tj = Props.blueprint(w, p, era);
      Map<BlockPos, BlockState> want = new LinkedHashMap<>();

      for (int i = 0; i < tj.ps.size(); i++) {
         want.put(tj.ps.get(i), tj.ss.get(i));
      }

      if (!coversLoaded(w, want.keySet())) {
         p.retryAt = w.getTime() + 200L;
         return false;
      } else {
         Eras.Plan pl = diff(w, want, p.x, p.z, 12, Eras::propMay);
         BUSY.add(k);
         int g = gen(k);
         schedule(w, pl, () -> Props.has(p), ok -> {
            BUSY.remove(k);
            if (ok) {
               p.era = g == gen(k) ? era : -1;
            } else {
               p.retryAt = w.getTime() + 200L;
            }
         });
         return true;
      }
   }

   private static boolean restyleTown(Kingdoms.K k, Civ.City c, ServerWorld w, BlockPos t) {
      long tk = t.asLong();
      if (TownBuilder.underBuild(tk)) {
         return false;
      } else {
         int era = c.era;
         TownBuilder.Job tj = TownBuilder.townBlueprint(k, w, t, era);
         if (tj == null) {
            TownBuilder.TOWN_ERA.put(tk, era);
            return false;
         } else {
            Map<BlockPos, BlockState> want = new LinkedHashMap<>();

            for (int i = 0; i < tj.ps.size(); i++) {
               want.put(tj.ps.get(i), tj.ss.get(i));
            }

            Eras.Plan pl = diff(
               w,
               want,
               t.getX(),
               t.getZ(),
               22,
               cur -> TownBuilder.pavingBlock(cur) || cur.isOf(Blocks.TORCH) || cur.isOf(Blocks.LANTERN) || cur.getBlock() instanceof StairsBlock
            );
            BUSY.add(k);
            int g = gen(k);
            schedule(w, pl, () -> true, ok -> {
               BUSY.remove(k);
               if (ok) {
                  TownBuilder.TOWN_ERA.put(tk, g == gen(k) ? era : -1);
               } else {
                  TOWN_RETRY.put(tk, w.getTime() + 200L);
               }
            });
            return true;
         }
      }
   }

   private static void schedule(ServerWorld w, Eras.Plan pl, BooleanSupplier valid, Consumer<Boolean> done) {
      final List<Eras.Ch> ch = pl.ch;
      if (ch.isEmpty()) {
         done.accept(pl.complete);
      } else {
         Tasks.add(new Tasks.Task() {
            int i;
            boolean ok = pl.complete;

            @Override
            public boolean tick() {
               try {
                  return this.run();
               } catch (Exception var2) {
                  GodBox.LOG.error("GodBox: сбой перекраски", (Throwable)var2);
                  done.accept(false);
                  return true;
               }
            }

            private boolean run() {
               if (!valid.getAsBoolean()) {
                  done.accept(false);
                  return true;
               } else {
                  for (int n = 0; n < 60 && this.i < ch.size(); this.i++) {
                     Eras.Ch c = ch.get(this.i);
                     BlockPos p = c.pos();
                     if (!w.isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)) {
                        this.ok = false;
                     } else {
                        BlockState s = c.st();
                        Block b = s.getBlock();
                        if (b instanceof FenceBlock || b instanceof WallBlock || b instanceof PaneBlock) {
                           s = Block.postProcessState(s, w, p);
                        }

                        w.setBlockState(p, s, 3);
                        if (this.i % 15 == 0) {
                           Fx.particles(w, ParticleTypes.CLOUD, p.getX() + 0.5, p.getY() + 0.8, p.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.02);
                        }
                     }

                     n++;
                  }

                  if (this.i < ch.size()) {
                     return false;
                  } else {
                     done.accept(this.ok);
                     return true;
                  }
               }
            }
         });
      }
   }

   static {
      Block N = null;
      row(
         of(Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS),
         Blocks.PACKED_MUD,
         Blocks.MUD_BRICKS,
         Blocks.TERRACOTTA,
         N,
         Blocks.SMOOTH_SANDSTONE,
         N,
         N,
         Blocks.CALCITE,
         Blocks.BRICKS,
         Blocks.BRICKS
      );
      row(
         of(Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.STRIPPED_OAK_LOG),
         N,
         N,
         N,
         N,
         Blocks.QUARTZ_PILLAR,
         N,
         Blocks.STONE_BRICKS,
         Blocks.STRIPPED_DARK_OAK_LOG,
         Blocks.POLISHED_BLACKSTONE_BRICKS,
         Blocks.DARK_OAK_LOG
      );
      row(
         of(Blocks.DARK_OAK_PLANKS),
         Blocks.HAY_BLOCK,
         Blocks.HAY_BLOCK,
         Blocks.ORANGE_TERRACOTTA,
         N,
         Blocks.RED_TERRACOTTA,
         N,
         N,
         Blocks.DEEPSLATE_TILES,
         Blocks.WAXED_OXIDIZED_CUT_COPPER,
         Blocks.SPRUCE_PLANKS
      );
      row(
         of(Blocks.SPRUCE_SLAB, Blocks.DARK_OAK_SLAB, Blocks.OAK_SLAB, Blocks.BRICK_SLAB),
         Blocks.MOSS_CARPET,
         Blocks.MOSS_CARPET,
         Blocks.WAXED_CUT_COPPER_SLAB,
         N,
         Blocks.SMOOTH_SANDSTONE_SLAB,
         N,
         N,
         Blocks.DEEPSLATE_TILE_SLAB,
         Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB,
         Blocks.BRICK_SLAB
      );
      row(of(Blocks.COBBLESTONE), N, N, N, N, Blocks.SANDSTONE, N, Blocks.STONE_BRICKS, N, Blocks.DEEPSLATE_BRICKS, Blocks.STONE_BRICKS);
      row(of(Blocks.MOSSY_COBBLESTONE), N, N, N, N, Blocks.CUT_SANDSTONE, N, Blocks.MOSSY_STONE_BRICKS, N, Blocks.CRACKED_DEEPSLATE_BRICKS, Blocks.STONE_BRICKS);
      row(
         of(Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE),
         Blocks.COBBLESTONE,
         Blocks.COBBLESTONE,
         Blocks.MUD_BRICKS,
         N,
         Blocks.CUT_SANDSTONE,
         N,
         N,
         Blocks.QUARTZ_BRICKS,
         Blocks.POLISHED_DEEPSLATE,
         Blocks.SMOOTH_STONE
      );
      row(
         of(Blocks.CHISELED_STONE_BRICKS),
         Blocks.MOSSY_COBBLESTONE,
         Blocks.MOSSY_COBBLESTONE,
         Blocks.TERRACOTTA,
         N,
         Blocks.CHISELED_SANDSTONE,
         N,
         N,
         Blocks.CHISELED_QUARTZ_BLOCK,
         Blocks.CHISELED_DEEPSLATE,
         Blocks.CHISELED_QUARTZ_BLOCK
      );
      row(of(Blocks.BRICKS), Blocks.PACKED_MUD, Blocks.MUD_BRICKS, N, N, N, N, N, N, N, N);
      row(of(Blocks.GLASS), Blocks.AIR, Blocks.AIR, N, N, N, N, N, N, Blocks.LIGHT_GRAY_STAINED_GLASS, Blocks.GLASS);
      row(of(Blocks.GLASS_PANE), Blocks.AIR, Blocks.AIR, N, N, N, N, N, N, Blocks.LIGHT_GRAY_STAINED_GLASS_PANE, Blocks.GLASS_PANE);
      row(of(Blocks.LANTERN), N, N, N, N, N, N, N, N, N, N);
      row(of(Blocks.TORCH), N, N, N, N, N, N, N, N, N, N);
      row(of(Blocks.OAK_FENCE, Blocks.DARK_OAK_FENCE), N, N, N, N, Blocks.BIRCH_FENCE, N, N, N, Blocks.NETHER_BRICK_FENCE, Blocks.DARK_OAK_FENCE);
      row(of(Blocks.COBBLESTONE_WALL), N, N, N, N, Blocks.SANDSTONE_WALL, N, Blocks.STONE_BRICK_WALL, N, Blocks.DEEPSLATE_BRICK_WALL, Blocks.STONE_BRICK_WALL);
      row(
         of(Blocks.STONE_BRICK_SLAB),
         Blocks.COBBLESTONE_SLAB,
         Blocks.COBBLESTONE_SLAB,
         Blocks.MUD_BRICK_SLAB,
         N,
         Blocks.SANDSTONE_SLAB,
         N,
         N,
         Blocks.QUARTZ_SLAB,
         Blocks.DEEPSLATE_BRICK_SLAB,
         Blocks.QUARTZ_SLAB
      );
      row(of(Blocks.SMOOTH_STONE), N, N, N, N, Blocks.SMOOTH_SANDSTONE, N, N, Blocks.SMOOTH_QUARTZ, Blocks.POLISHED_DEEPSLATE, Blocks.SMOOTH_QUARTZ);
      row(of(Blocks.QUARTZ_BLOCK), Blocks.MUD_BRICKS, Blocks.MUD_BRICKS, Blocks.WHITE_TERRACOTTA, Blocks.CALCITE, N, N, N, N, N, N);
      row(of(Blocks.IRON_BARS), Blocks.OAK_FENCE, Blocks.OAK_FENCE, N, N, N, N, N, N, N, N);
      row(of(Blocks.GOLD_BLOCK), Blocks.HAY_BLOCK, Blocks.BONE_BLOCK, Blocks.WAXED_COPPER_BLOCK, Blocks.IRON_BLOCK, N, N, N, N, N, N);
      Map<Block, Block> s = new HashMap<>();
      Block[][] pairs = new Block[][]{
         {Blocks.OAK_PLANKS, Blocks.BRICKS},
         {Blocks.SPRUCE_PLANKS, Blocks.BRICKS},
         {Blocks.BIRCH_PLANKS, Blocks.BRICKS},
         {Blocks.OAK_LOG, Blocks.POLISHED_BASALT},
         {Blocks.SPRUCE_LOG, Blocks.POLISHED_BASALT},
         {Blocks.BIRCH_LOG, Blocks.POLISHED_BASALT},
         {Blocks.STRIPPED_OAK_LOG, Blocks.POLISHED_BASALT},
         {Blocks.DARK_OAK_PLANKS, Blocks.WAXED_WEATHERED_CUT_COPPER},
         {Blocks.OAK_SLAB, Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB},
         {Blocks.SPRUCE_SLAB, Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB},
         {Blocks.DARK_OAK_SLAB, Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB},
         {Blocks.COBBLESTONE, Blocks.POLISHED_BLACKSTONE_BRICKS},
         {Blocks.MOSSY_COBBLESTONE, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS},
         {Blocks.STONE_BRICKS, Blocks.POLISHED_BLACKSTONE},
         {Blocks.CHISELED_STONE_BRICKS, Blocks.CHISELED_POLISHED_BLACKSTONE},
         {Blocks.GLASS, Blocks.ORANGE_STAINED_GLASS},
         {Blocks.GLASS_PANE, Blocks.ORANGE_STAINED_GLASS_PANE},
         {Blocks.OAK_FENCE, Blocks.IRON_BARS},
         {Blocks.DARK_OAK_FENCE, Blocks.IRON_BARS},
         {Blocks.SPRUCE_FENCE, Blocks.IRON_BARS},
         {Blocks.COBBLESTONE_WALL, Blocks.POLISHED_BLACKSTONE_BRICK_WALL},
         {Blocks.STONE_BRICK_SLAB, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB},
         {Blocks.SMOOTH_STONE, Blocks.POLISHED_BLACKSTONE},
         {Blocks.QUARTZ_BLOCK, Blocks.WAXED_CUT_COPPER},
         {Blocks.GOLD_BLOCK, Blocks.WAXED_COPPER_BLOCK}
      };

      for (Block[] p : pairs) {
         s.put(p[0], p[1]);
      }

      for (Entry<Block, Block[]> e : new ArrayList<>(MAP.entrySet())) {
         Block[] a = Arrays.copyOf(e.getValue(), 11);
         a[10] = s.containsKey(e.getKey()) ? s.get(e.getKey()) : a[8];
         MAP.put(e.getKey(), a);
      }

      RUNNING = Collections.newSetFromMap(new WeakHashMap<>());
      BUSY = new HashSet<>();
      PENDING = new HashMap<>();
      PENDING_TOWN = new HashMap<>();
      TOWN_RETRY = new HashMap<>();
      GEN = new HashMap<>();
      guessedAt = -1;
   }

   private record Ch(BlockPos pos, BlockState st) {
   }

   private static final class Plan {
      final List<Eras.Ch> ch = new ArrayList<>();
      boolean complete = true;
   }
}
