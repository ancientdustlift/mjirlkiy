package com.vladik.godbox;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.Heightmap.Type;

public final class Civ {
   public static boolean economy = true;
   static final int FIND = 0;
   static final int GO = 1;
   static final int WORK = 2;
   static final int RETURN = 3;
   static final int REV = 2;
   private static final Map<Kingdoms.K, Civ.City> CITIES = new EnumMap<>(Kingdoms.K.class);
   private static final Map<VillagerEntity, Civ.W> WORKERS = new WeakHashMap<>();
   private static int timer;
   static final int GRID = 11;

   private Civ() {
   }

   static Civ.City city(Kingdoms.K k) {
      Civ.City c = CITIES.get(k);
      if (c == null) {
         c = new Civ.City();
         CITIES.put(k, c);
      }

      if (c.quarry == null && k.world != null && k.capital != null && c.cycle % 6 == 0) {
         Civ.Building q = site(k, c, Civ.Kind.QUARRY, k.world);
         if (q != null) {
            q.done = true;
            c.buildings.add(q);
            c.quarry = q;
         }
      }

      return c;
   }

   public static void found(Kingdoms.K k) {
      CITIES.remove(k);
      city(k);
   }

   public static void remove(Kingdoms.K k) {
      CITIES.remove(k);

      for (MobEntity m : k.members) {
         if (m instanceof VillagerEntity v) {
            WORKERS.remove(v);
         }
      }
   }

   public static void clear() {
      CITIES.clear();
      WORKERS.clear();
      timer = 0;
   }

   public static String summary(Kingdoms.K k) {
      Civ.City c = CITIES.get(k);
      if (c == null) {
         return "";
      } else {
         String s = "склад: дерево "
            + c.wood
            + ", камень "
            + c.stone
            + ", еда "
            + c.food
            + ", руда "
            + c.ore
            + " | домов "
            + c.count(Civ.Kind.HOUSE)
            + ", ферм "
            + c.count(Civ.Kind.FARM)
            + ", казарм "
            + c.count(Civ.Kind.BARRACKS)
            + ", башен "
            + c.count(Civ.Kind.TOWER)
            + CivPlus.extra(c);
         int[] n = counts(k);
         s = s + " | лесорубов " + n[0] + ", шахтёров " + n[1] + ", фермеров " + n[2] + ", строителей " + n[3] + ", рыбаков " + n[4];
         if (c.project != null) {
            s = s + " | строится: " + c.project.b.kind.title + " (" + c.project.i * 100 / Math.max(1, c.project.pos.size()) + "%)";
         }

         if (c.hungry) {
            s = s + " | ГОЛОД";
         }

         return s + Events.summary(k);
      }
   }

   public static void tick(MinecraftServer server) {
      if (economy) {
         if (++timer % 5 == 0) {
            boolean slow = timer % 100 == 0;

            for (Kingdoms.K k : Kingdoms.K.values()) {
               if (k.alive && k.world != null && k.capital != null && k.world.shouldTickEntity(k.capital)) {
                  ServerWorld w = k.world;
                  Civ.City c = city(k);
                  growCrops(w, c);
                  CivPlus.tickCity(k, c, w, timer);

                  for (MobEntity m : new ArrayList<>(k.members)) {
                     if (m instanceof VillagerEntity v && v.isAlive() && !v.isBaby()) {
                        Civ.W s = WORKERS.get(v);
                        if (s == null) {
                           s = new Civ.W();
                           WORKERS.put(v, s);
                        }

                        if (s.job == null) {
                           setJob(k, v, s, pick(k, c));
                        }

                        step(k, c, w, v, s);
                     }
                  }

                  if (slow) {
                     economyTick(k, c, w);
                  }
               }
            }
         }
      }
   }

   static int[] counts(Kingdoms.K k) {
      int[] n = new int[Civ.Job.values().length];

      for (MobEntity m : k.members) {
         if (m instanceof VillagerEntity v) {
            Civ.W s = WORKERS.get(v);
            if (s != null && s.job != null) {
               n[s.job.ordinal()]++;
            }
         }
      }

      return n;
   }

   static Civ.Job pick(Kingdoms.K k, Civ.City c) {
      int[] n = counts(k);
      int total = n[0] + n[1] + n[2] + n[3];
      if (c.project != null && n[3] < Math.max(1, (total + 1) / 5)) {
         return Civ.Job.BUILDER;
      } else if (c.water && n[4] < Math.max(1, total / 6) && c.food < 60) {
         return Civ.Job.FISHER;
      } else {
         double fw = c.food < 20 ? 1.8 : 1.0;
         double ww = c.wood < 30 ? 1.4 : 1.0;
         double sw = c.stone < 25 ? 1.1 : 0.7;
         if (c.quarry == null) {
            sw = 0.01;
         }

         double a = (n[0] + 0.5) / ww;
         double b = (n[1] + 0.5) / sw;
         double f = (n[2] + 0.5) / fw;
         if (a <= b && a <= f) {
            return Civ.Job.LUMBER;
         } else {
            return b <= f ? Civ.Job.MINER : Civ.Job.FARMER;
         }
      }
   }

   static String jobTitle(VillagerEntity v) {
      Civ.W s = WORKERS.get(v);
      return s != null && s.job != null ? s.job.title : "";
   }

   static void setJob(Kingdoms.K k, VillagerEntity v, Civ.W s, Civ.Job j) {
      s.job = j;
      s.state = 0;
      s.target = null;
      s.idle = 0;
      v.setVillagerData(v.getVillagerData().withProfession(j.prof));
      v.setExperience(1);
      v.setCustomName(Life.label(k, v, j.title));
      v.setCustomNameVisible(false);
      hold(v, new ItemStack(j.tool));
   }

   static void hold(VillagerEntity v, ItemStack st) {
      v.equipStack(EquipmentSlot.MAINHAND, st);
      v.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
   }

   static void step(Kingdoms.K k, Civ.City c, ServerWorld w, VillagerEntity v, Civ.W s) {
      if (!CivPlus.night(k, c, w, v, s)) {
         Civ.Building q = c.quarry;
         if (q != null && q.inside(v.getBlockX(), v.getBlockZ(), 0) && v.getY() < q.y0 - 1.5) {
            BlockPos e = front(k, q, w);
            v.requestTeleport(e.getX() + 0.5, e.getY(), e.getZ() + 0.5);
         }

         switch (s.state) {
            case 0:
               find(k, c, w, v, s);
               break;
            case 1:
               go(k, c, w, v, s);
               break;
            case 2:
               work(k, c, w, v, s);
               break;
            default:
               ret(k, c, w, v, s);
         }
      }
   }

   static void find(Kingdoms.K k, Civ.City c, ServerWorld w, VillagerEntity v, Civ.W s) {
      if (s.job == Civ.Job.BUILDER && c.project == null) {
         s.job = null;
         setJob(k, v, s, pick(k, c));
      } else if (s.job != Civ.Job.BUILDER && c.project != null && counts(k)[3] == 0) {
         setJob(k, v, s, Civ.Job.BUILDER);
      }

      if (s.job == Civ.Job.BUILDER && s.bagSum() > 0) {
         s.state = 3;
      } else {
         BlockPos t = null;
         BlockPos to = null;
         switch (s.job) {
            case LUMBER:
               t = findTree(k, c, w, v);
               to = t;
               break;
            case MINER:
               t = Ores.prospect(k, c, w, v);
               if (t != null) {
                  to = t.up();
               } else {
                  t = nextMine(k, c, w);
                  if (t != null) {
                     to = front(k, c.quarry, w);
                  }
               }
               break;
            case FARMER:
               t = findFarm(c, w, v);
               if (t == null) {
                  t = findForage(k, c, w, v);
               }

               to = t;
               break;
            case BUILDER:
               if (c.project != null) {
                  t = c.project.b.center();
                  to = front(k, c.project.b, w);
               }
               break;
            case FISHER:
               t = CivPlus.findWater(k, c, w, v);
               to = t == null ? null : CivPlus.shore(w, t);
               if (to == null) {
                  t = null;
               }
         }

         if (t == null) {
            if (s.bagSum() > 0) {
               s.state = 3;
            } else {
               if (s.idle % 4 == 0) {
                  v.setHeadRollingTimeLeft(25);
               }

               if (++s.idle > 12) {
                  s.job = null;
                  setJob(k, v, s, pick(k, c));
               }
            }
         } else {
            s.idle = 0;
            s.target = t;
            s.walk = to;
            s.state = 1;
            s.stuck = 0;
            s.best = 1.0E9;
         }
      }
   }

   static void go(Kingdoms.K k, Civ.City c, ServerWorld w, VillagerEntity v, Civ.W s) {
      if (s.walk != null && s.target != null) {
         double d = hdist(v, s.walk);
         double reach = s.job == Civ.Job.LUMBER ? 2.8 : 2.3;
         if (d <= reach && Math.abs(v.getY() - s.walk.getY()) < 4.0) {
            s.state = 2;
            s.work = 0;
         } else {
            walk(v, s.walk);
            if (d < s.best - 0.4) {
               s.best = d;
               s.stuck = 0;
            } else if (++s.stuck > 50) {
               s.stuck = 0;
               s.best = 1.0E9;
               if (s.job != Civ.Job.BUILDER && s.job != Civ.Job.MINER) {
                  s.state = 0;
                  s.target = null;
               } else {
                  BlockPos g = ground(w, s.walk.getX(), s.walk.getZ());
                  Fx.particles(w, ParticleTypes.CLOUD, v.getX(), v.getY() + 1.0, v.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
                  v.requestTeleport(g.getX() + 0.5, g.getY(), g.getZ() + 0.5);
               }
            }
         }
      } else {
         s.state = 0;
      }
   }

   static void work(Kingdoms.K k, Civ.City c, ServerWorld w, VillagerEntity v, Civ.W s) {
      BlockPos t = s.target;
      if (t == null) {
         s.state = 0;
      } else {
         halt(v);
         v.getLookControl().lookAt(t.getX() + 0.5, t.getY() + 0.5, t.getZ() + 0.5);
         if (s.job == Civ.Job.BUILDER) {
            build(k, c, w, v, s);
         } else if (s.job == Civ.Job.FISHER) {
            CivPlus.fish(k, c, w, v, s);
         } else {
            BlockState bs = w.getBlockState(t);
            if (!valid(s.job, w, t, bs)) {
               w.setBlockBreakingInfo(v.getId(), t, -1);
               s.state = 0;
               s.target = null;
            } else {
               s.work++;
               int need = duration(s.job, w, t, bs);
               CivPlus.workFx(v, s.work);
               if (!bs.isAir()) {
                  w.setBlockBreakingInfo(v.getId(), t, Math.min(9, s.work * 10 / need));
                  Fx.particles(
                     w, new BlockStateParticleEffect(ParticleTypes.BLOCK, bs), t.getX() + 0.5, t.getY() + 0.7, t.getZ() + 0.5, 4, 0.25, 0.25, 0.25, 0.05
                  );
                  if (s.work % 2 == 0) {
                     Fx.sound(
                        w, t.getX() + 0.5, t.getY() + 0.5, t.getZ() + 0.5, bs.getSoundGroup().getHitSound(), 0.8F, 0.8F + w.getRandom().nextFloat() * 0.3F
                     );
                  }
               }

               if (s.work >= need) {
                  w.setBlockBreakingInfo(v.getId(), t, -1);
                  s.work = 0;
                  s.target = null;
                  switch (s.job) {
                     case LUMBER:
                        s.bag[0] = s.bag[0] + Math.max(1, chop(w, t));
                        s.state = 3;
                        hold(v, new ItemStack(Items.OAK_LOG));
                        break;
                     case MINER:
                        mineYield(bs, s);
                        w.breakBlock(t, false);
                        if (s.bagSum() >= 5) {
                           s.state = 3;
                           hold(v, new ItemStack(s.bag[3] > 0 ? Items.RAW_IRON : Items.COBBLESTONE));
                        } else {
                           s.state = 0;
                        }
                        break;
                     case FARMER:
                        farmAct(w, t, bs, s);
                        if (c.count(Civ.Kind.MILL) > 0 && w.getRandom().nextBoolean()) {
                           s.bag[2]++;
                        }

                        if (s.bag[2] >= 4) {
                           s.state = 3;
                           hold(v, new ItemStack(Items.WHEAT));
                        } else {
                           s.state = 0;
                        }
                        break;
                     default:
                        s.state = 0;
                  }
               }
            }
         }
      }
   }

   static void ret(Kingdoms.K k, Civ.City c, ServerWorld w, VillagerEntity v, Civ.W s) {
      BlockPos home = CivPlus.depot(k, c, w, v);
      double d = hdist(v, home);
      if (!(d <= 3.5)) {
         walk(v, home);
         if (d < s.best - 0.4) {
            s.best = d;
            s.stuck = 0;
         } else if (++s.stuck > 60) {
            s.stuck = 0;
            s.best = 1.0E9;
            BlockPos g = ground(w, home.getX(), home.getZ() + 5);
            v.requestTeleport(g.getX() + 0.5, g.getY(), g.getZ() + 0.5);
         }
      } else {
         s.bag[2] = Events.harvest(k, s.bag[2]);
         c.wood = c.wood + Races.yield(k, 0, s.bag[0]);
         c.stone = c.stone + Races.yield(k, 1, s.bag[1]);
         c.food = c.food + Races.yield(k, 2, s.bag[2]);
         c.ore = c.ore + Races.yield(k, 3, s.bag[3]);
         Storage.deposit(k, c, w, v);

         for (int i = 0; i < 4; i++) {
            s.bag[i] = 0;
         }

         Fx.particles(w, ParticleTypes.HAPPY_VILLAGER, v.getX(), v.getY() + 1.2, v.getZ(), 6, 0.4, 0.4, 0.4, 0.02);
         Fx.sound(w, v.getX(), v.getY(), v.getZ(), SoundEvents.ENTITY_ITEM_PICKUP, 0.6F, 0.8F);
         CivPlus.hop(v);
         s.state = 0;
         s.stuck = 0;
         s.best = 1.0E9;
         if (w.getRandom().nextInt(4) == 0) {
            s.job = null;
            setJob(k, v, s, pick(k, c));
         } else {
            hold(v, new ItemStack(s.job.tool));
         }
      }
   }

   static BlockPos findTree(Kingdoms.K k, Civ.City c, ServerWorld w, VillagerEntity v) {
      Random r = w.getRandom();
      int R = radius(c);
      BlockPos best = null;
      double bd = Double.MAX_VALUE;

      for (int i = 0; i < 40; i++) {
         int x = Conquest.cx(k, r) + r.nextInt(R * 2 + 1) - R;
         int z = Conquest.cz(k) + r.nextInt(R * 2 + 1) - R;
         if (!inCity(x, z, 1)) {
            BlockPos top = w.getTopPosition(Type.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z)).down();
            if (w.getBlockState(top).isIn(BlockTags.LOGS)) {
               BlockPos b = top;
               int g = 0;

               while (w.getBlockState(b.down()).isIn(BlockTags.LOGS) && g++ < 30) {
                  b = b.down();
               }

               if (hasLeaves(w, top) && !claimed(b)) {
                  double d = b.getSquaredDistance(v.getBlockPos());
                  if (d < bd) {
                     bd = d;
                     best = b.toImmutable();
                  }
               }
            }
         }
      }

      if (best == null && r.nextInt(3) == 0) {
         int px = Conquest.cx(k, r) + r.nextInt(R * 2 + 1) - R;
         int pz = Conquest.cz(k) + r.nextInt(R * 2 + 1) - R;
         if (!inCity(px, pz, 2)) {
            BlockPos g = w.getTopPosition(Type.MOTION_BLOCKING_NO_LEAVES, new BlockPos(px, 0, pz));
            if (w.getBlockState(g.down()).isIn(BlockTags.DIRT) && w.getBlockState(g).isAir()) {
               w.setBlockState(g, Blocks.OAK_SAPLING.getDefaultState());
            }
         }
      }

      return best;
   }

   static boolean hasLeaves(ServerWorld w, BlockPos top) {
      for (int dx = -2; dx <= 2; dx++) {
         for (int dy = -1; dy <= 2; dy++) {
            for (int dz = -2; dz <= 2; dz++) {
               if (w.getBlockState(top.add(dx, dy, dz)).isIn(BlockTags.LEAVES)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   static int chop(ServerWorld w, BlockPos base) {
      ArrayDeque<BlockPos> q = new ArrayDeque<>();
      Set<BlockPos> seen = new HashSet<>();
      List<BlockPos> logs = new ArrayList<>();
      q.add(base);
      seen.add(base);

      while (!q.isEmpty() && logs.size() < 64) {
         BlockPos p = q.poll();
         if (w.getBlockState(p).isIn(BlockTags.LOGS)) {
            logs.add(p);

            for (int dx = -1; dx <= 1; dx++) {
               for (int dy = 0; dy <= 1; dy++) {
                  for (int dz = -1; dz <= 1; dz++) {
                     BlockPos n = p.add(dx, dy, dz);
                     if (Math.abs(n.getX() - base.getX()) <= 5 && Math.abs(n.getZ() - base.getZ()) <= 5 && seen.add(n)) {
                        q.add(n);
                     }
                  }
               }
            }
         }
      }

      Block sap = sapling(w.getBlockState(base).getBlock());

      for (int i = logs.size() - 1; i >= 0; i--) {
         w.breakBlock(logs.get(i), false);
      }

      CivPlus.leaves(w, logs);
      if (sap != null && w.getBlockState(base).isAir() && w.getBlockState(base.down()).isIn(BlockTags.DIRT)) {
         w.setBlockState(base, sap.getDefaultState());
      }

      return logs.size();
   }

   static Block sapling(Block log) {
      String path = Registries.BLOCK.getId(log).getPath();
      if (path.endsWith("_log") && !path.startsWith("stripped_")) {
         Block s = Registries.BLOCK.get(Identifier.of("minecraft", path.replace("_log", "_sapling")));
         return s == Blocks.AIR ? Blocks.OAK_SAPLING : s;
      } else {
         return null;
      }
   }

   static BlockPos nextMine(Kingdoms.K k, Civ.City c, ServerWorld w) {
      Civ.Building q = c.quarry;
      if (q == null) {
         return null;
      } else {
         int bottom = Math.max(q.y0 - 18, w.getBottomY() + 4);

         for (int y = q.y0 + 2; y >= bottom; y--) {
            for (int x = q.x0; x <= q.x1; x++) {
               for (int z = q.z0; z <= q.z1; z++) {
                  BlockPos p = new BlockPos(x, y, z);
                  BlockState s = w.getBlockState(p);
                  if (!s.isAir() && s.getFluidState().isEmpty() && !(s.getHardness(w, p) < 0.0F) && !claimed(p)) {
                     return p;
                  }
               }
            }
         }

         c.quarry = null;
         Civ.Building nq = site(k, c, Civ.Kind.QUARRY, w);
         if (nq != null) {
            nq.done = true;
            c.buildings.add(nq);
            c.quarry = nq;
         }

         return null;
      }
   }

   static void mineYield(BlockState s, Civ.W st) {
      String p = Registries.BLOCK.getId(s.getBlock()).getPath();
      if (p.equals("ancient_debris")) {
         st.bag[3] = st.bag[3] + 5;
      } else {
         if (p.endsWith("_ore")) {
            st.bag[3] = st.bag[3] + (!p.contains("diamond") && !p.contains("emerald") && !p.contains("gold") ? 1 : 3);
            st.bag[1]++;
         } else if (s.isIn(BlockTags.BASE_STONE_OVERWORLD) || p.contains("stone") || p.contains("deepslate")) {
            st.bag[1]++;
         } else if (s.isIn(BlockTags.LOGS)) {
            st.bag[0]++;
         }
      }
   }

   static BlockPos findFarm(Civ.City c, ServerWorld w, VillagerEntity v) {
      BlockPos best = null;
      double bd = Double.MAX_VALUE;

      for (Civ.Building b : c.buildings) {
         if (b.kind == Civ.Kind.FARM && b.done) {
            for (int x = b.x0; x <= b.x1; x++) {
               for (int z = b.z0; z <= b.z1; z++) {
                  if (x != b.cx() || z != b.cz()) {
                     BlockPos p = new BlockPos(x, b.y0, z);
                     BlockState below = w.getBlockState(p.down());
                     if (below.isOf(Blocks.FARMLAND) || below.isOf(Blocks.DIRT) || below.isOf(Blocks.GRASS_BLOCK)) {
                        BlockState s = w.getBlockState(p);
                        boolean need = s.isAir() || s.getBlock() instanceof CropBlock cb && cb.isMature(s);
                        if (need && !claimed(p)) {
                           double d = p.getSquaredDistance(v.getBlockPos());
                           if (d < bd) {
                              bd = d;
                              best = p;
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return best;
   }

   static BlockPos findForage(Kingdoms.K k, Civ.City c, ServerWorld w, VillagerEntity v) {
      Random r = w.getRandom();
      int R = radius(c);
      BlockPos best = null;
      double bd = Double.MAX_VALUE;

      for (int i = 0; i < 30; i++) {
         int x = Conquest.cx(k, r) + r.nextInt(R * 2 + 1) - R;
         int z = Conquest.cz(k) + r.nextInt(R * 2 + 1) - R;
         if (!inCity(x, z, 0)) {
            BlockPos top = w.getTopPosition(Type.WORLD_SURFACE, new BlockPos(x, 0, z)).down();
            BlockState s = w.getBlockState(top);
            if (forage(s) && !claimed(top)) {
               double d = top.getSquaredDistance(v.getBlockPos());
               if (d < bd) {
                  bd = d;
                  best = top.toImmutable();
               }
            }
         }
      }

      return best;
   }

   static boolean forage(BlockState s) {
      return s.isOf(Blocks.SWEET_BERRY_BUSH)
         ? s.get(SweetBerryBushBlock.AGE) >= 2
         : s.isOf(Blocks.SHORT_GRASS) || s.isOf(Blocks.TALL_GRASS) || s.isOf(Blocks.FERN) || s.isIn(BlockTags.SMALL_FLOWERS);
   }

   static void farmAct(ServerWorld w, BlockPos t, BlockState bs, Civ.W s) {
      CropBlock wheat = (CropBlock)Blocks.WHEAT;
      if (bs.getBlock() instanceof CropBlock cb) {
         if (cb.isMature(bs)) {
            s.bag[2] = s.bag[2] + 2;
            w.syncWorldEvent(2001, t, Block.getRawIdFromState(bs));
            w.setBlockState(t, wheat.withAge(0));
         }
      } else if (bs.isAir()) {
         if (!w.getBlockState(t.down()).isOf(Blocks.FARMLAND)) {
            w.setBlockState(t.down(), Blocks.FARMLAND.getDefaultState().with(FarmlandBlock.MOISTURE, 7));
         }

         w.setBlockState(t, wheat.withAge(0));
         Fx.sound(w, t.getX() + 0.5, t.getY(), t.getZ() + 0.5, SoundEvents.ITEM_CROP_PLANT, 0.8F, 1.0F);
      } else if (bs.isOf(Blocks.SWEET_BERRY_BUSH)) {
         s.bag[2] = s.bag[2] + 2;
         w.setBlockState(t, bs.with(SweetBerryBushBlock.AGE, 1));
         Fx.sound(w, t.getX() + 0.5, t.getY(), t.getZ() + 0.5, SoundEvents.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 0.8F, 1.0F);
      } else {
         w.breakBlock(t, false);
         s.bag[2]++;
      }
   }

   static void growCrops(ServerWorld w, Civ.City c) {
      Random r = w.getRandom();

      for (Civ.Building b : c.buildings) {
         if (b.kind == Civ.Kind.FARM && b.done) {
            for (int i = 0; i < 3 + 2 * c.count(Civ.Kind.WELL); i++) {
               BlockPos p = new BlockPos(b.x0 + r.nextInt(b.kind.w), b.y0, b.z0 + r.nextInt(b.kind.d));
               BlockState s = w.getBlockState(p);
               if (s.getBlock() instanceof CropBlock cb && !cb.isMature(s) && r.nextDouble() * 3.0 < Seasons.growth()) {
                  w.setBlockState(p, cb.withAge(cb.getAge(s) + 1), 2);
               }
            }
         }
      }
   }

   static boolean valid(Civ.Job job, ServerWorld w, BlockPos t, BlockState bs) {
      return switch (job) {
         case LUMBER -> bs.isIn(BlockTags.LOGS);
         case MINER -> !bs.isAir() && bs.getFluidState().isEmpty();
         case FARMER -> {
            if (bs.isAir()) {
               BlockState below = w.getBlockState(t.down());
               yield below.isOf(Blocks.FARMLAND) || below.isOf(Blocks.DIRT) || below.isOf(Blocks.GRASS_BLOCK);
            } else {
               yield bs.getBlock() instanceof CropBlock cb ? cb.isMature(bs) : forage(bs);
            }
         }
         default -> true;
      };
   }

   static int duration(Civ.Job job, ServerWorld w, BlockPos t, BlockState bs) {
      return switch (job) {
         case LUMBER -> 14;
         case MINER -> Math.max(2, Math.min(12, 2 + (int)(bs.getHardness(w, t) * 3.0F)));
         case FARMER -> 4;
         default -> 1;
      };
   }

   static void build(Kingdoms.K k, Civ.City c, ServerWorld w, VillagerEntity v, Civ.W s) {
      Civ.Project p = c.project;
      if (p == null) {
         s.state = 0;
      } else {
         Civ.Building b = p.b;
         if (hdist(v, b.center()) > 10.0) {
            s.state = 1;
            s.target = b.center();
            s.walk = front(k, b, w);
            s.best = 1.0E9;
            s.stuck = 0;
         } else {
            int skipped = 0;
            int fast = 0;

            while (true) {
               if (p.i >= p.pos.size()) {
                  finish(k, c, w);
                  return;
               }

               BlockPos q = p.pos.get(p.i);
               BlockState st = p.st.get(p.i);
               p.i++;
               BlockState cur = w.getBlockState(q);
               if (!cur.equals(st)) {
                  v.getLookControl().lookAt(q.getX() + 0.5, q.getY() + 0.5, q.getZ() + 0.5);
                  if (!st.isAir()) {
                     boolean bed = st.getBlock() instanceof BedBlock;
                     w.setBlockState(q, st, bed ? 2 : 3);
                     hold(v, new ItemStack(st.getBlock().asItem()));
                     if (w.getRandom().nextInt(3) == 0) {
                        CivPlus.bob(v);
                     }

                     Fx.sound(
                        w, q.getX() + 0.5, q.getY() + 0.5, q.getZ() + 0.5, st.getSoundGroup().getPlaceSound(), 0.7F, 0.9F + w.getRandom().nextFloat() * 0.2F
                     );
                     Fx.particles(
                        w, new BlockStateParticleEffect(ParticleTypes.BLOCK, st), q.getX() + 0.5, q.getY() + 0.5, q.getZ() + 0.5, 5, 0.3, 0.3, 0.3, 0.05
                     );
                     break;
                  }

                  boolean soft = cur.isIn(BlockTags.LEAVES) || cur.isReplaceable();
                  if (cur.isIn(BlockTags.LOGS)) {
                     c.wood++;
                     hold(v, new ItemStack(Items.IRON_AXE));
                  } else if (!soft) {
                     hold(v, new ItemStack(Items.IRON_SHOVEL));
                  }

                  boolean wet = !cur.getFluidState().isEmpty();
                  if (!wet && !(cur.getBlock() instanceof AbstractFireBlock)) {
                     w.breakBlock(q, false);
                  } else {
                     w.setBlockState(q, st, 3);
                  }

                  if (!soft && !wet || ++fast >= 6) {
                     break;
                  }
               } else if (++skipped > 16) {
                  return;
               }
            }
         }
      }
   }

   static void finish(Kingdoms.K k, Civ.City c, ServerWorld w) {
      Civ.Project p = c.project;
      if (p != null) {
         c.project = null;
         Civ.Building b = p.b;
         b.done = true;
         if (!p.repair) {
            b.era = p.era;
         }

         BlockPos cc = b.center();
         if (p.repair) {
            Repair.snapshot(w, b);
            Fx.particles(w, ParticleTypes.HAPPY_VILLAGER, cc.getX() + 0.5, cc.getY() + 2, cc.getZ() + 0.5, 20, 2.0, 1.5, 2.0, 0.05);
            tell(w, Text.literal("\ud83d\udee0 " + k.title + " восстановило: " + b.kind.title).formatted(k.color));
         } else {
            Fx.particles(w, ParticleTypes.HAPPY_VILLAGER, cc.getX() + 0.5, cc.getY() + 2, cc.getZ() + 0.5, 30, 2.0, 1.5, 2.0, 0.05);
            Fx.sound(w, cc.getX() + 0.5, cc.getY() + 1, cc.getZ() + 0.5, SoundEvents.ENTITY_VILLAGER_CELEBRATE, 1.5F, 1.0F);
            CivPlus.onFinish(k, c, w, b);
            Banners.onBuilt(k, w, b);
            Repair.snapshot(w, b);
            if (b.kind == Civ.Kind.TOWER) {
               Kingdoms.spawn(k, EntityType.PILLAGER, new BlockPos(b.cx(), b.y0 + 7, b.cz()));
            }

            tell(w, Text.literal(k.title + " построило: " + b.kind.title).formatted(k.color));
         }
      }
   }

   static void economyTick(Kingdoms.K k, Civ.City c, ServerWorld w) {
      c.cycle++;
      if (c.project == null) {
         plan(k, c, w);
      }

      int vill = 0;
      int sold = 0;

      for (MobEntity m : k.members) {
         if (m instanceof VillagerEntity) {
            vill++;
         } else {
            sold++;
         }
      }

      if (c.cycle % 10 == 0) {
         c.buildings.removeIf(b -> b.done && b.kind == Civ.Kind.HOUSE && ruined(w, b));
      }

      if (c.cycle % 5 == 0 && c.wood < 25) {
         c.wood += 2;
      }

      int houses = c.count(Civ.Kind.HOUSE);
      int cap = CivPlus.cap(c);
      Random r = w.getRandom();
      if (c.cycle % Events.birthPeriod(k) == 0 && vill < cap && c.food >= 8 && k.members.size() < 260 && Races.breeds(k, r)) {
         c.food -= 8;
         BlockPos at = birthPlace(k, c, w);
         MobEntity mx = Kingdoms.spawn(k, EntityType.VILLAGER, at);
         if (mx != null) {
            Life.birth(mx);
            Fx.particles(w, ParticleTypes.HEART, at.getX() + 0.5, at.getY() + 1.5, at.getZ() + 0.5, 6, 0.4, 0.4, 0.4, 0.02);
            Fx.sound(w, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, SoundEvents.ENTITY_VILLAGER_CELEBRATE, 1.0F, 1.3F);
         }
      }

      int barracks = c.count(Civ.Kind.BARRACKS);
      int towers = c.count(Civ.Kind.TOWER);
      int scap = Races.army(k, 2 + barracks * 6 + towers * 2);
      if (c.cycle % 4 == 0 && barracks > 0 && sold < scap && c.food >= 6 && c.wood >= 2 && c.stone >= 3) {
         Civ.Building bb = null;

         for (Civ.Building b : c.buildings) {
            if (b.kind == Civ.Kind.BARRACKS && b.done) {
               bb = b;
            }
         }

         if (bb != null) {
            c.food -= 6;
            c.wood -= 2;
            c.stone -= 3;
            MobEntity mx = Kingdoms.spawn(k, Kingdoms.soldier(r), front(k, bb, w));
            CivPlus.arm(c, mx, w);
            if (mx != null && c.ore >= 2) {
               c.ore -= 2;
               mx.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
               mx.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
               mx.setEquipmentDropChance(EquipmentSlot.CHEST, 0.0F);
               mx.setEquipmentDropChance(EquipmentSlot.LEGS, 0.0F);
            }
         }
      }

      if (c.cycle % 12 == 0) {
         int eat = Seasons.eat(k, (k.members.size() + 4) / (c.count(Civ.Kind.GRANARY) > 0 ? 8 : 5));
         c.food -= eat;
         if (c.food < 0) {
            c.food = 0;
            if (!c.hungry) {
               tell(w, Text.literal(k.title + ": голод!").formatted(Formatting.GOLD));
            }

            c.hungry = true;

            for (MobEntity mx : k.members) {
               if (mx instanceof VillagerEntity v) {
                  v.damage(w.getDamageSources().starve(), 3.0F);
                  break;
               }
            }
         } else {
            c.hungry = false;
         }
      }
   }

   static boolean ruined(ServerWorld w, Civ.Building b) {
      if (!w.isChunkLoaded(b.cx() >> 4, b.cz() >> 4)) {
         return false;
      } else {
         int air = 0;
         int[][] cs = new int[][]{{b.x0, b.z0}, {b.x1, b.z0}, {b.x0, b.z1}, {b.x1, b.z1}};

         for (int[] q : cs) {
            for (int dy = 0; dy <= 1; dy++) {
               if (w.getBlockState(new BlockPos(q[0], b.y0 + dy, q[1])).isAir()) {
                  air++;
               }
            }
         }

         return air >= 6;
      }
   }

   static BlockPos birthPlace(Kingdoms.K k, Civ.City c, ServerWorld w) {
      Random r = w.getRandom();
      List<Civ.Building> hs = new ArrayList<>();

      for (Civ.Building b : c.buildings) {
         if (b.kind == Civ.Kind.HOUSE && b.done) {
            hs.add(b);
         }
      }

      return !hs.isEmpty() ? front(k, hs.get(r.nextInt(hs.size())), w) : ground(w, k.capital.getX() + r.nextInt(5) - 2, k.capital.getZ() + 2 + r.nextInt(2));
   }

   static void plan(Kingdoms.K k, Civ.City c, ServerWorld w) {
      int vill = 0;

      for (MobEntity m : k.members) {
         if (m instanceof VillagerEntity) {
            vill++;
         }
      }

      int houses = c.count(Civ.Kind.HOUSE);
      int farms = c.count(Civ.Kind.FARM);
      int bar = c.count(Civ.Kind.BARRACKS);
      int tow = c.count(Civ.Kind.TOWER);
      Civ.Kind want = CivPlus.choose(c, vill, w.getRandom());
      if (want != null) {
         if (c.wood >= want.wood && c.stone >= want.stone) {
            Civ.Building b = site(k, c, want, w);
            if (b != null) {
               c.wood = c.wood - want.wood;
               c.stone = c.stone - want.stone;
               c.buildings.add(b);
               c.project = blueprint(k, b, w);
               int cleared = Clearing.prepend(c.project, w);
               tell(w, Text.literal(k.title + ": заложено — " + want.title + (cleared > 10 ? " (сначала расчищают площадку)" : "")).formatted(k.color));
            }
         }
      }
   }

   static Civ.Building site(Kingdoms.K k, Civ.City c, Civ.Kind kind, ServerWorld w) {
      Random r = w.getRandom();
      int base = kind == Civ.Kind.QUARRY ? 14 : 9;
      int spread = Math.min(150, 6 + c.buildings.size() * 2);
      boolean grid = kind != Civ.Kind.QUARRY && kind.w <= 7 && kind.d <= 7;

      for (int i = 0; i < 90; i++) {
         BlockPos cap0 = Conquest.center(k, r);
         double a = r.nextDouble() * Math.PI * 2.0;
         double dist = base + r.nextDouble() * Math.min(spread, 14 + i * 2) + i * 0.3;
         int cx = cap0.getX() + (int)Math.round(Math.cos(a) * dist);
         int cz = cap0.getZ() + (int)Math.round(Math.sin(a) * dist);
         if (grid) {
            cx = k.capital.getX() + Math.round((cx - k.capital.getX()) / 11.0F) * 11;
            cz = k.capital.getZ() + Math.round((cz - k.capital.getZ()) / 11.0F) * 11;
         }

         int x0 = cx - kind.w / 2;
         int z0 = cz - kind.d / 2;
         int x1 = x0 + kind.w - 1;
         int z1 = z0 + kind.d - 1;
         if (x1 < k.capital.getX() - 6 || x0 > k.capital.getX() + 6 || z1 < k.capital.getZ() - 6 || z0 > k.capital.getZ() + 6) {
            boolean bad = false;

            label89:
            for (Civ.City oc : CITIES.values()) {
               Iterator max = oc.buildings.iterator();

               while (true) {
                  if (max.hasNext()) {
                     Civ.Building o = (Civ.Building)max.next();
                     TownBuilder.fit(o);
                     if (x1 < o.x0 - 2 || x0 > o.x1 + 2 || z1 < o.z0 - 2 || z0 > o.z1 + 2) {
                        continue;
                     }

                     bad = true;
                  }

                  if (bad) {
                     break label89;
                  }
                  break;
               }
            }

            if (!bad) {
               int[][] pts = new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}, {cx, cz}};
               int min = Integer.MAX_VALUE;
               int max = Integer.MIN_VALUE;

               for (int[] pt : pts) {
                  BlockPos g = soil(w, pt[0], pt[1]);
                  BlockState gs = w.getBlockState(g.down());
                  if (!gs.getFluidState().isEmpty() || !w.getFluidState(g).isEmpty()) {
                     bad = true;
                     break;
                  }

                  min = Math.min(min, g.getY());
                  max = Math.max(max, g.getY());
               }

               if (!bad && max - min <= 2) {
                  Civ.Building nb = new Civ.Building(kind, x0, z0, soil(w, cx, cz).getY());
                  nb.rev = 2;
                  return nb;
               }
            }
         }
      }

      return null;
   }

   static Civ.Project blueprint(Kingdoms.K k, Civ.Building b, ServerWorld w) {
      Civ.Project p = new Civ.Project(b);
      p.era = Eras.eraOf(k);

      int H = switch (b.kind) {
         case HOUSE -> 9;
         case FARM -> 3;
         case BARRACKS -> 8;
         case TOWER -> 10;
         case WELL -> 5;
         case MILL, TEMPLE -> 11;
         case SMITHY, GRANARY -> 7;
         case MANOR -> b.rev >= 2 ? 12 : 10;
         default -> 0;
         case WAREHOUSE -> 8;
      };
      BlockState air = Blocks.AIR.getDefaultState();

      for (int y = b.y0; y < b.y0 + H; y++) {
         for (int x = b.x0; x <= b.x1; x++) {
            for (int z = b.z0; z <= b.z1; z++) {
               BlockPos q = new BlockPos(x, y, z);
               if (!w.getBlockState(q).isAir()) {
                  p.add(q, air);
               }
            }
         }
      }

      BlockState fill = Blocks.COBBLESTONE.getDefaultState();

      for (int x = b.x0; x <= b.x1; x++) {
         for (int zx = b.z0; zx <= b.z1; zx++) {
            int d = 1;

            while (d < 6 && !solid(w, new BlockPos(x, b.y0 - 1 - d, zx))) {
               d++;
            }

            for (int y = b.y0 - d; y <= b.y0 - 2; y++) {
               p.add(new BlockPos(x, y, zx), fill);
            }

            p.add(new BlockPos(x, b.y0 - 1, zx), floor(b, x, zx));
         }
      }

      int doorZ;
      int doorX;
      int dx = k.capital.getX() - b.cx();
      int dz = k.capital.getZ() - b.cz();
      boolean axisX = Math.abs(dx) >= Math.abs(dz);
      doorX = axisX ? (dx > 0 ? b.x1 : b.x0) : b.cx();
      doorZ = axisX ? b.cz() : (dz > 0 ? b.z1 : b.z0);
      int y0 = b.y0;
      label520:
      switch (b.kind) {
         case HOUSE:
            BlockState[] sty = CivPlus.houseStyle(b);
            BlockState log = sty[0];
            BlockState plank = sty[1];
            BlockState base = Blocks.COBBLESTONE.getDefaultState();
            BlockState glass = Blocks.GLASS.getDefaultState();
            int era = p.era;
            BlockState roof = Eras.style(era, Civ.Kind.HOUSE, sty[2]);
            int cx = b.cx();
            int cz = b.cz();

            for (int y = y0; y <= y0 + 2; y++) {
               for (int x = b.x0; x <= b.x1; x++) {
                  for (int zxxxxx = b.z0; zxxxxx <= b.z1; zxxxxx++) {
                     boolean ex = x == b.x0 || x == b.x1;
                     boolean ez = zxxxxx == b.z0 || zxxxxx == b.z1;
                     if ((ex || ez) && (x != doorX || zxxxxx != doorZ || y > y0 + 1)) {
                        BlockState s;
                        if (ex && ez) {
                           s = log;
                        } else if (y != y0 + 1 || x != cx && zxxxxx != cz) {
                           if (y == y0) {
                              s = base;
                           } else {
                              s = plank;
                           }
                        } else {
                           s = glass;
                        }

                        p.add(new BlockPos(x, y, zxxxxx), s);
                     }
                  }
               }
            }

            for (int x = b.x0; x <= b.x1; x++) {
               for (int zxxxxxx = b.z0; zxxxxxx <= b.z1; zxxxxxx++) {
                  boolean ex = x == b.x0 || x == b.x1;
                  boolean ez = zxxxxxx == b.z0 || zxxxxxx == b.z1;
                  BlockState s = ez ? Arch.beam(log, Axis.X) : (ex ? Arch.beam(log, Axis.Z) : plank);
                  p.add(new BlockPos(x, y0 + 3, zxxxxxx), s);
               }
            }

            Arch.gable(p::add, b.x0, b.z0, b.x1, b.z1, y0 + 3, roof, plank, roof);
            int top = y0 + 3 + Arch.gableHeight(b.x0, b.z0, b.x1, b.z1);
            p.add(new BlockPos(cx, top, cz), k.wool.getDefaultState());
            Arch.chimney(p::add, b.x1 - 1, b.z1 - 1, y0 + 4, top, Blocks.BRICKS.getDefaultState());
            Direction out = axisX ? (doorX == b.x1 ? Direction.EAST : Direction.WEST) : (doorZ == b.z1 ? Direction.SOUTH : Direction.NORTH);
            Arch.door(p::add, new BlockPos(doorX, y0, doorZ), out, Arch.woodenDoor(plank));
            Block trap = Arch.woodenTrapdoor(plank);
            Object[][] wins = new Object[][]{
               {new BlockPos(cx, y0 + 1, b.z0), Direction.NORTH},
               {new BlockPos(cx, y0 + 1, b.z1), Direction.SOUTH},
               {new BlockPos(b.x0, y0 + 1, cz), Direction.WEST},
               {new BlockPos(b.x1, y0 + 1, cz), Direction.EAST}
            };

            for (Object[] wv : wins) {
               BlockPos wp = (BlockPos)wv[0];
               if (wp.getX() != doorX || wp.getZ() != doorZ) {
                  Arch.shutters(p::add, wp, (Direction)wv[1], trap);
               }
            }

            Arch.wallTorch(p::add, new BlockPos(doorX, y0 + 1, doorZ).offset(out.rotateYClockwise()), out);
            Block bed = k.bed();
            BlockPos foot;
            BlockPos head;
            Direction face;
            if (axisX) {
               int bx = doorX == b.x0 ? b.x1 - 1 : b.x0 + 1;
               foot = new BlockPos(bx, y0, b.z0 + 2);
               head = new BlockPos(bx, y0, b.z0 + 1);
               face = Direction.NORTH;
            } else {
               int bz = doorZ == b.z0 ? b.z1 - 1 : b.z0 + 1;
               foot = new BlockPos(b.x0 + 2, y0, bz);
               head = new BlockPos(b.x0 + 1, y0, bz);
               face = Direction.WEST;
            }

            p.add(foot, bed.getDefaultState().with(BedBlock.FACING, face).with(BedBlock.PART, BedPart.FOOT));
            p.add(head, bed.getDefaultState().with(BedBlock.FACING, face).with(BedBlock.PART, BedPart.HEAD));
            int[][] corners = new int[][]{{b.x0 + 1, b.z0 + 1}, {b.x1 - 1, b.z0 + 1}, {b.x0 + 1, b.z1 - 1}, {b.x1 - 1, b.z1 - 1}};
            int furn = 0;

            for (int[] cn : corners) {
               BlockPos cp = new BlockPos(cn[0], y0, cn[1]);
               if (!cp.equals(foot) && !cp.equals(head) && Math.abs(cn[0] - doorX) + Math.abs(cn[1] - doorZ) > 1) {
                  if (furn == 0) {
                     p.add(cp, Blocks.CRAFTING_TABLE.getDefaultState());
                     p.add(cp.up(), Arch.pot(b.x0 * 7 + b.z0));
                  } else {
                     p.add(cp, Blocks.BARREL.getDefaultState());
                  }

                  if (++furn >= 2) {
                     break label520;
                  }
               }
            }
            break;
         case FARM:
            BlockState wheat = ((CropBlock)Blocks.WHEAT).withAge(0);

            for (int x = b.x0; x <= b.x1; x++) {
               for (int zxxxx = b.z0; zxxxx <= b.z1; zxxxx++) {
                  if (x != b.cx() || zxxxx != b.cz()) {
                     p.add(new BlockPos(x, y0, zxxxx), wheat);
                  }
               }
            }
            break;
         case BARRACKS:
            BlockState brick = Blocks.STONE_BRICKS.getDefaultState();
            BlockState corner = Blocks.CHISELED_STONE_BRICKS.getDefaultState();
            BlockState bars = Blocks.IRON_BARS.getDefaultState();

            for (int y = y0; y <= y0 + 3; y++) {
               for (int x = b.x0; x <= b.x1; x++) {
                  for (int zx = b.z0; zx <= b.z1; zx++) {
                     boolean ex = x == b.x0 || x == b.x1;
                     boolean ez = zx == b.z0 || zx == b.z1;
                     if ((ex || ez) && (x != doorX || zx != doorZ || y > y0 + 1)) {
                        BlockState s;
                        if (ex && ez) {
                           s = corner;
                        } else if (y != y0 + 2 || (!ez || (x - b.x0) % 2 != 1) && (!ex || zx != b.cz())) {
                           s = brick;
                        } else {
                           s = bars;
                        }

                        p.add(new BlockPos(x, y, zx), s);
                     }
                  }
               }
            }

            for (int x = b.x0; x <= b.x1; x++) {
               for (int zxx = b.z0; zxx <= b.z1; zxx++) {
                  p.add(new BlockPos(x, y0 + 4, zxx), brick);
                  boolean edge = x == b.x0 || x == b.x1 || zxx == b.z0 || zxx == b.z1;
                  if (edge && (x + zxx & 1) == 0) {
                     p.add(new BlockPos(x, y0 + 5, zxx), brick);
                  }
               }
            }

            BlockState fence = Blocks.OAK_FENCE.getDefaultState();
            BlockState wool = k.wool.getDefaultState();
            p.add(new BlockPos(b.cx(), y0 + 5, b.cz()), fence);
            p.add(new BlockPos(b.cx(), y0 + 6, b.cz()), fence);
            p.add(new BlockPos(b.cx(), y0 + 7, b.cz()), wool);
            p.add(new BlockPos(b.cx() + 1, y0 + 7, b.cz()), wool);
            p.add(new BlockPos(b.cx() + 1, y0 + 6, b.cz()), wool);
            BlockPos st = new BlockPos(b.x0 + 1, y0, b.z0 + 1);
            if (Math.abs(st.getX() - doorX) + Math.abs(st.getZ() - doorZ) > 1) {
               p.add(st, Blocks.SMITHING_TABLE.getDefaultState());
            }
            break;
         case TOWER:
            BlockState cob = Blocks.COBBLESTONE.getDefaultState();
            BlockState moss = Blocks.MOSSY_COBBLESTONE.getDefaultState();

            for (int y = y0; y <= y0 + 5; y++) {
               for (int x = b.x0; x <= b.x1; x++) {
                  for (int zxxx = b.z0; zxxx <= b.z1; zxxx++) {
                     p.add(new BlockPos(x, y, zxxx), Math.floorMod(x + y + zxxx, 5) == 0 ? moss : cob);
                  }
               }
            }

            for (int x = b.x0; x <= b.x1; x++) {
               for (int zxxx = b.z0; zxxx <= b.z1; zxxx++) {
                  boolean c0 = x == b.cx() && zxxx == b.cz();
                  p.add(new BlockPos(x, y0 + 6, zxxx), c0 ? k.wool.getDefaultState() : Blocks.STONE_BRICKS.getDefaultState());
                  if (!c0) {
                     p.add(new BlockPos(x, y0 + 7, zxxx), Blocks.COBBLESTONE_WALL.getDefaultState());
                  }
               }
            }

            int[][] cs = new int[][]{{b.x0, b.z0}, {b.x1, b.z0}, {b.x0, b.z1}, {b.x1, b.z1}};

            for (int[] cn : cs) {
               p.add(new BlockPos(cn[0], y0 + 8, cn[1]), Blocks.LANTERN.getDefaultState());
            }
      }

      CivPlus.extras(k, b, w, p, doorX, doorZ);
      List<BlockState> orig = new ArrayList<>(p.st);
      Eras.apply(k, b.kind, p.st);

      for (int i = 0; i < p.pos.size(); i++) {
         if (p.raw.contains(p.pos.get(i))) {
            p.st.set(i, orig.get(i));
         }
      }

      Eras.decor(k, b, p);
      return p;
   }

   static BlockState floor(Civ.Building b, int x, int z) {
      return switch (b.kind) {
         case HOUSE -> Blocks.SPRUCE_PLANKS.getDefaultState();
         case FARM -> x == b.cx() && z == b.cz()
            ? Blocks.WATER.getDefaultState()
            : (BlockState)Blocks.FARMLAND.getDefaultState().with(FarmlandBlock.MOISTURE, 7);
         case BARRACKS -> Blocks.STONE_BRICKS.getDefaultState();
         default -> Blocks.COBBLESTONE.getDefaultState();
         case GRANARY, MANOR, WAREHOUSE -> Blocks.SPRUCE_PLANKS.getDefaultState();
         case TEMPLE -> Blocks.SMOOTH_STONE.getDefaultState();
      };
   }

   static boolean solid(ServerWorld w, BlockPos p) {
      BlockState s = w.getBlockState(p);
      return !s.isAir() && s.getFluidState().isEmpty() && !s.isReplaceable();
   }

   static BlockPos ground(ServerWorld w, int x, int z) {
      return w.getTopPosition(Type.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
   }

   static BlockPos front(Kingdoms.K k, Civ.Building b, ServerWorld w) {
      int dx = k.capital.getX() - b.cx();
      int dz = k.capital.getZ() - b.cz();
      int x;
      int z;
      if (Math.abs(dx) >= Math.abs(dz)) {
         x = dx > 0 ? b.x1 + 2 : b.x0 - 2;
         z = b.cz();
      } else {
         z = dz > 0 ? b.z1 + 2 : b.z0 - 2;
         x = b.cx();
      }

      return ground(w, x, z);
   }

   static double hdist(VillagerEntity v, BlockPos p) {
      double ax = v.getX() - (p.getX() + 0.5);
      double az = v.getZ() - (p.getZ() + 0.5);
      return Math.sqrt(ax * ax + az * az);
   }

   static void walk(VillagerEntity v, BlockPos p) {
      v.getBrain().remember(MemoryModuleType.WALK_TARGET, new WalkTarget(p, 0.75F, 1));
   }

   static void halt(VillagerEntity v) {
      v.getBrain().forget(MemoryModuleType.WALK_TARGET);
      v.getNavigation().stop();
   }

   static int radius(Civ.City c) {
      return Math.min(80, 24 + 2 * c.buildings.size());
   }

   static BlockPos soil(ServerWorld w, int x, int z) {
      BlockPos g = ground(w, x, z);

      for (int bottom = w.getBottomY(); g.getY() > bottom + 1; g = g.down()) {
         BlockState s = w.getBlockState(g.down());
         if (!s.isIn(BlockTags.LOGS) && !s.isIn(BlockTags.LEAVES) && (!Clearing.vegetation(s) || s.isAir())) {
            break;
         }
      }

      return g;
   }

   static boolean foreign(Civ.Building self, int x, int z) {
      for (Civ.City c : CITIES.values()) {
         for (Civ.Building b : c.buildings) {
            if (b != self && b.inside(x, z, 0)) {
               return true;
            }
         }
      }

      return false;
   }

   static boolean inCity(int x, int z, int m) {
      for (Civ.City c : CITIES.values()) {
         for (Civ.Building b : c.buildings) {
            if (b.inside(x, z, m)) {
               return true;
            }
         }
      }

      return false;
   }

   static boolean claimed(BlockPos p) {
      for (Civ.W o : WORKERS.values()) {
         if (p.equals(o.target)) {
            return true;
         }
      }

      return false;
   }

   static void tell(ServerWorld w, Text t) {
      w.getServer().getPlayerManager().broadcast(t, true);
   }

   static final class Building {
      final Civ.Kind kind;
      final int x0;
      final int z0;
      final int y0;
      int x1;
      int z1;
      boolean done;
      boolean fitted;
      int era = -2;
      int rev = 1;
      boolean busy;
      int plot = -2;
      long retryAt;

      Building(Civ.Kind kind, int x0, int z0, int y0) {
         this.kind = kind;
         this.x0 = x0;
         this.z0 = z0;
         this.x1 = x0 + kind.w - 1;
         this.z1 = z0 + kind.d - 1;
         this.y0 = y0;
      }

      Building(Civ.Kind kind, int x0, int z0, int y0, int w, int d) {
         this(kind, x0, z0, y0);
         this.x1 = x0 + w - 1;
         this.z1 = z0 + d - 1;
         this.fitted = true;
      }

      int cx() {
         return (this.x0 + this.x1) / 2;
      }

      int cz() {
         return (this.z0 + this.z1) / 2;
      }

      BlockPos center() {
         return new BlockPos(this.cx(), this.y0, this.cz());
      }

      boolean inside(int x, int z, int m) {
         return x >= this.x0 - m && x <= this.x1 + m && z >= this.z0 - m && z <= this.z1 + m;
      }
   }

   static final class City {
      int wood = 15;
      int stone = 5;
      int food = 25;
      int ore = 0;
      final List<Civ.Building> buildings = new ArrayList<>();
      Civ.Project project;
      Civ.Building quarry;
      int cycle;
      boolean hungry;
      int era;
      int faith;
      int culture = -1;
      int religion = -1;
      boolean castle;
      boolean water = true;
      BlockPos fireAt;

      int count(Civ.Kind k) {
         int n = 0;

         for (Civ.Building b : this.buildings) {
            if (b.kind == k && b.done) {
               n++;
            }
         }

         return n;
      }
   }

   static enum Job {
      LUMBER("Лесоруб", VillagerProfession.FLETCHER, Items.IRON_AXE),
      MINER("Шахтёр", VillagerProfession.TOOLSMITH, Items.IRON_PICKAXE),
      FARMER("Фермер", VillagerProfession.FARMER, Items.IRON_HOE),
      BUILDER("Строитель", VillagerProfession.MASON, Items.IRON_SHOVEL),
      FISHER("Рыбак", VillagerProfession.FISHERMAN, Items.FISHING_ROD);

      final String title;
      final VillagerProfession prof;
      final Item tool;

      private Job(String title, VillagerProfession prof, Item tool) {
         this.title = title;
         this.prof = prof;
         this.tool = tool;
      }
   }

   static enum Kind {
      HOUSE("Дом", 5, 5, 10, 3),
      FARM("Ферма", 7, 7, 6, 0),
      BARRACKS("Казарма", 7, 5, 15, 30),
      TOWER("Сторожевая башня", 3, 3, 4, 25),
      WELL("Колодец", 3, 3, 4, 10),
      MILL("Мельница", 5, 5, 25, 12),
      SMITHY("Кузница", 5, 5, 12, 22),
      GRANARY("Амбар", 5, 5, 22, 6),
      TEMPLE("Храм", 7, 7, 20, 40),
      MANOR("Большой дом", 7, 5, 34, 14),
      QUARRY("Карьер", 5, 5, 0, 0),
      CASTLE("Замок", 15, 15, 0, 0),
      WAREHOUSE("Склад", 7, 5, 18, 10);

      final String title;
      final int w;
      final int d;
      final int wood;
      final int stone;

      private Kind(String title, int w, int d, int wood, int stone) {
         this.title = title;
         this.w = w;
         this.d = d;
         this.wood = wood;
         this.stone = stone;
      }
   }

   static final class Project {
      final Civ.Building b;
      final List<BlockPos> pos = new ArrayList<>();
      final List<BlockState> st = new ArrayList<>();
      int i;
      boolean repair;
      int era;
      final Set<BlockPos> raw = new HashSet<>();

      Project(Civ.Building b) {
         this.b = b;
      }

      void add(BlockPos p, BlockState s) {
         this.pos.add(p);
         this.st.add(s);
      }
   }

   static final class W {
      Civ.Job job;
      int state;
      BlockPos target;
      BlockPos walk;
      int work;
      int stuck;
      double best = 1.0E9;
      int idle;
      BlockPos rest;
      final int[] bag = new int[4];

      int bagSum() {
         return this.bag[0] + this.bag[1] + this.bag[2] + this.bag[3];
      }
   }
}
