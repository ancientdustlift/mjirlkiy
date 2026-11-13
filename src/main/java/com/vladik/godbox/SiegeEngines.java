package com.vladik.godbox;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public final class SiegeEngines {
   public static final String TAG = "gb_siege";
   public static final int NONE = 0;
   public static final int WAIT = 1;
   public static final int FIRED = 2;
   private static final List<SiegeEngines.Engine> ENGINES = new ArrayList<>();
   private static int t;
   private static final List<SiegeEngines.Rec> PENDING = new ArrayList<>();

   private SiegeEngines() {
   }

   static int request(ServerWorld w, Kingdoms.K k, MobEntity m, BlockPos target, SiegeEngines.Type type) {
      if (Models.custom() && ModelParts.has(type.model)) {
         SiegeEngines.Engine ready = null;
         SiegeEngines.Engine mover = null;
         boolean busy = false;
         boolean idle = false;
         int own = 0;
         SiegeEngines.Engine far = null;
         double reach = Config.d("siege.reach", 60.0);

         for (SiegeEngines.Engine e : ENGINES) {
            if (e.k == k && e.type == type && !e.stand.isRemoved() && e.stand.getWorld() == w) {
               own++;
               double dm = e.stand.squaredDistanceTo(m);
               if (far == null || dm > far.stand.squaredDistanceTo(m)) {
                  far = e;
               }

               if (e.crew != null && e.crew.isAlive()) {
                  double d = flat(e, target);
                  if (d >= type.min() && d <= type.range()) {
                     if (e.cool > 0) {
                        busy = true;
                     } else if (ready == null || d < flat(ready, target)) {
                        ready = e;
                     }
                  } else if (dm < reach * reach && (mover == null || dm < mover.stand.squaredDistanceTo(m))) {
                     mover = e;
                  }
               } else if (dm < reach * reach) {
                  idle = true;
               }
            }
         }

         if (ready != null) {
            fire(w, ready, target);
            return 2;
         } else if (busy) {
            return 1;
         } else if (mover != null) {
            push(mover, target);
            return 1;
         } else if (idle) {
            return 1;
         } else {
            if (own >= type.count() && far != null) {
               remove(far);
            }

            boolean first = true;

            for (SiegeEngines.Engine ex : ENGINES) {
               if (ex.k == k && !ex.stand.isRemoved()) {
                  first = false;
               }
            }

            SiegeEngines.Engine exx = deploy(w, k, m, target, type);
            if (exx == null) {
               return 0;
            } else {
               if (first) {
                  Chronicle.say(w.getServer(), k, "выкатывает " + type.ru + " к стенам врага");
               }

               exx.cool = 20;
               double d = flat(exx, target);
               if (d < type.min() || d > type.range()) {
                  push(exx, target);
               }

               return 1;
            }
         }
      } else {
         return 0;
      }
   }

   private static double flat(SiegeEngines.Engine e, BlockPos target) {
      double dx = target.getX() + 0.5 - e.stand.getX();
      double dz = target.getZ() + 0.5 - e.stand.getZ();
      return Math.sqrt(dx * dx + dz * dz);
   }

   private static float yawTo(ArmorStandEntity s, double tx, double tz) {
      return (float)(MathHelper.atan2(tz - s.getZ(), tx - s.getX()) * 180.0 / Math.PI) - 90.0F;
   }

   private static void fire(ServerWorld w, SiegeEngines.Engine e, BlockPos target) {
      ArmorStandEntity s = e.stand;
      double tx = target.getX() + 0.5;
      double tz = target.getZ() + 0.5;
      float yaw = yawTo(s, tx, tz);
      s.refreshPositionAndAngles(s.getX(), s.getY(), s.getZ(), yaw, 0.0F);
      e.go = false;
      e.cool = e.type.reload();
      e.idle = 0;
      double sp = e.type.spread() * (0.4 + 0.6 * flat(e, target) / e.type.range());
      int gx = MathHelper.floor(tx + w.getRandom().nextGaussian() * sp);
      int gz = MathHelper.floor(tz + w.getRandom().nextGaussian() * sp);
      BlockPos g = w.isChunkLoaded(gx >> 4, gz >> 4) ? Fx.surface(w, gx, gz) : target;
      double rad = yaw * Math.PI / 180.0;
      double fx = -Math.sin(rad);
      double fz = Math.cos(rad);
      Models.trigger(s);
      MobEntity crew = e.crew;
      if (e.type == SiegeEngines.Type.CANNON) {
         double ox = s.getX() + fx * 1.4;
         double oy = s.getY() + 1.15;
         double oz = s.getZ() + fz * 1.4;
         Tech.cannonball(w, crew, ox, oy, oz, g, Guns.ball(), e.type.power());
         Fx.particles(w, ParticleTypes.CAMPFIRE_COSY_SMOKE, ox, oy, oz, 6, 0.2, 0.2, 0.2, 0.02);
      } else {
         double ox = s.getX() + fx * 0.8;
         double oy = s.getY() + 3.0;
         double oz = s.getZ() + fz * 0.8;
         Fx.sound(w, s.getX(), s.getY() + 1.0, s.getZ(), SoundEvents.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.2F, 0.6F);
         Tasks.delay(5, () -> {
            if (!s.isRemoved() && crew != null && crew.isAlive() && crew.getWorld() == w) {
               ItemStack rock = Models.part("guns", "boulder");
               Siege.lob(w, crew, ox, oy, oz, g, rock != null ? rock : new ItemStack(Items.COBBLESTONE), e.type.power());
            }
         });
      }
   }

   private static void push(SiegeEngines.Engine e, BlockPos target) {
      double tx = target.getX() + 0.5;
      double tz = target.getZ() + 0.5;
      double dx = e.stand.getX() - tx;
      double dz = e.stand.getZ() - tz;
      double d = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
      double want = MathHelper.clamp(d, (double)(e.type.min() + 3), (double)(e.type.range() - 6));
      e.gx = tx + dx / d * want;
      e.gz = tz + dz / d * want;
      e.go = true;
      e.idle = 0;
      float yaw = yawTo(e.stand, tx, tz);
      e.stand.refreshPositionAndAngles(e.stand.getX(), e.stand.getY(), e.stand.getZ(), yaw, 0.0F);
   }

   private static void roll(ServerWorld w, SiegeEngines.Engine e, double step) {
      if (e.crew != null) {
         ArmorStandEntity s = e.stand;
         double dx = e.gx - s.getX();
         double dz = e.gz - s.getZ();
         double d = Math.sqrt(dx * dx + dz * dz);
         if (d < 0.3) {
            e.go = false;
         } else {
            double nx = s.getX() + dx / d * step;
            double nz = s.getZ() + dz / d * step;
            int y = groundY(w, nx, nz, s.getBlockY());
            if (y == Integer.MIN_VALUE) {
               e.go = false;
            } else {
               s.refreshPositionAndAngles(nx, y, nz, s.getYaw(), 0.0F);
               e.idle = 0;
            }
         }
      }
   }

   private static int groundY(ServerWorld w, double x, double z, int cy) {
      int bx = MathHelper.floor(x);
      int bz = MathHelper.floor(z);
      if (!w.isChunkLoaded(bx >> 4, bz >> 4)) {
         return Integer.MIN_VALUE;
      } else {
         for (int y = cy + 1; y >= cy - 2; y--) {
            BlockPos feet = new BlockPos(bx, y, bz);
            BlockPos below = feet.down();
            BlockState bs = w.getBlockState(below);
            if (bs.isSideSolidFullSquare(w, below, Direction.UP) && bs.getFluidState().isEmpty() && free(w, feet) && free(w, feet.up()) && free(w, feet.up(2))) {
               return y;
            }
         }

         return Integer.MIN_VALUE;
      }
   }

   private static boolean free(ServerWorld w, BlockPos p) {
      BlockState s = w.getBlockState(p);
      return s.getCollisionShape(w, p).isEmpty() && s.getFluidState().isEmpty();
   }

   private static SiegeEngines.Engine deploy(ServerWorld w, Kingdoms.K k, MobEntity m, BlockPos target, SiegeEngines.Type type) {
      double dx = target.getX() + 0.5 - m.getX();
      double dz = target.getZ() + 0.5 - m.getZ();
      double d = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
      double side = d - 2.0 >= type.min() + 1 ? 2.0 : -2.0;
      double x = m.getX() + dx / d * side;
      double z = m.getZ() + dz / d * side;
      int y = groundY(w, x, z, m.getBlockY());
      if (y == Integer.MIN_VALUE) {
         x = m.getX();
         z = m.getZ();
         y = groundY(w, x, z, m.getBlockY());
         if (y == Integer.MIN_VALUE) {
            return null;
         }
      }

      double sx = MathHelper.floor(x) + 0.5;
      double sz = MathHelper.floor(z) + 0.5;
      float yaw = (float)(MathHelper.atan2(target.getZ() + 0.5 - sz, target.getX() + 0.5 - sx) * 180.0 / Math.PI) - 90.0F;
      ArmorStandEntity s = spawnStand(w, type, sx, y, sz, yaw);
      if (s == null) {
         return null;
      } else {
         SiegeEngines.Engine e = new SiegeEngines.Engine(s, k, type);
         e.crew = m;
         ENGINES.add(e);
         Fx.sound(w, s.getX(), s.getY(), s.getZ(), SoundEvents.BLOCK_ANVIL_LAND, 0.6F, 0.7F);
         return e;
      }
   }

   private static ArmorStandEntity spawnStand(ServerWorld w, SiegeEngines.Type type, double x, double y, double z, float yaw) {
      ArmorStandEntity s = new ArmorStandEntity(w, x, y, z) {
         @Override
         public boolean shouldSave() {
            return false;
         }
      };
      s.setInvisible(true);
      s.setMarker(true);
      s.setNoGravity(true);
      s.setInvulnerable(true);
      s.setSilent(true);
      s.addCommandTag("gb_siege");
      s.refreshPositionAndAngles(x, y, z, yaw, 0.0F);
      if (!w.spawnEntity(s)) {
         return null;
      } else if (!Models.attach(s, type.model)) {
         s.discard();
         return null;
      } else {
         return s;
      }
   }

   private static void remove(SiegeEngines.Engine e) {
      ENGINES.remove(e);
      Models.detach(e.stand);
      e.stand.discard();
   }

   private static void destroy(ServerWorld w, SiegeEngines.Engine e) {
      ArmorStandEntity s = e.stand;
      Fx.particles(
         w, new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.getDefaultState()), s.getX(), s.getY() + 1.0, s.getZ(), 40, 0.8, 0.6, 0.8, 0.1
      );
      Fx.particles(w, ParticleTypes.LARGE_SMOKE, s.getX(), s.getY() + 1.0, s.getZ(), 10, 0.6, 0.4, 0.6, 0.03);
      Fx.sound(w, s.getX(), s.getY(), s.getZ(), SoundEvents.BLOCK_WOOD_BREAK, 1.5F, 0.7F);
      remove(e);
      Chronicle.say(w.getServer(), e.k, "теряет " + e.type.ru + ", разбитую врагом");
   }

   private static boolean atWar(Kingdoms.K k) {
      for (Kingdoms.K o : Kingdoms.K.values()) {
         if (o.alive && o.world == k.world && Diplomacy.hostile(k, o)) {
            return true;
         }
      }

      return false;
   }

   private static MobEntity crewOf(SiegeEngines.Engine e) {
      double r = Config.d("siege.crew", 6.0);
      double best = r * r;
      MobEntity c = null;

      for (MobEntity m : e.k.members) {
         if (m.isAlive() && !(m instanceof VillagerEntity) && m.getWorld() == e.stand.getWorld()) {
            double d = m.squaredDistanceTo(e.stand);
            if (d <= best) {
               best = d;
               c = m;
            }
         }
      }

      return c;
   }

   static void init() {
      SaveExt.register("siege", SiegeEngines::saveLines, SiegeEngines::loadLine, SiegeEngines::clear);
   }

   private static SiegeEngines.Rec recOf(SiegeEngines.Engine e) {
      ArmorStandEntity s = e.stand;
      return new SiegeEngines.Rec(e.type, e.k, s.getWorld().getRegistryKey().getValue().toString(), s.getX(), s.getY(), s.getZ(), s.getYaw(), e.hp);
   }

   private static List<String> saveLines() {
      List<SiegeEngines.Rec> all = new ArrayList<>(PENDING);

      for (SiegeEngines.Engine e : ENGINES) {
         boolean unloaded = e.stand.isRemoved() && e.stand.getRemovalReason() == RemovalReason.UNLOADED_TO_CHUNK;
         if ((!e.stand.isRemoved() || unloaded) && e.k.alive) {
            all.add(recOf(e));
         }
      }

      List<String> out = new ArrayList<>();

      for (SiegeEngines.Rec r : all) {
         out.add(r.type.name() + "|" + r.k.name() + "|" + r.world + "|" + r.x + "|" + r.y + "|" + r.z + "|" + r.yaw + "|" + r.hp);
      }

      return out;
   }

   private static void loadLine(String[] a) {
      if (PENDING.size() < 64) {
         try {
            PENDING.add(
               new SiegeEngines.Rec(
                  SiegeEngines.Type.valueOf(a[0]),
                  Kingdoms.K.valueOf(a[1]),
                  a[2],
                  Double.parseDouble(a[3]),
                  Double.parseDouble(a[4]),
                  Double.parseDouble(a[5]),
                  Float.parseFloat(a[6]),
                  Float.parseFloat(a[7])
               )
            );
         } catch (Exception var2) {
            GodBox.LOG.warn("GodBox: орудие в сохранении не разобрано: {}", String.join("|", a));
         }
      }
   }

   private static void restorePending(MinecraftServer srv) {
      Iterator<SiegeEngines.Rec> it = PENDING.iterator();

      while (it.hasNext()) {
         SiegeEngines.Rec r = it.next();
         ServerWorld w = null;
         Identifier id = Identifier.tryParse(r.world);
         if (id != null) {
            w = srv.getWorld(RegistryKey.of(RegistryKeys.WORLD, id));
         }

         if (w == null || !r.k.alive || r.k.world != w || !atWar(r.k) || !Models.custom() || !ModelParts.has(r.type.model)) {
            it.remove();
         } else if (w.isChunkLoaded(BlockPos.ofFloored(r.x, r.y, r.z))) {
            it.remove();
            ArmorStandEntity s = spawnStand(w, r.type, r.x, r.y, r.z, r.yaw);
            if (s != null) {
               SiegeEngines.Engine e = new SiegeEngines.Engine(s, r.k, r.type);
               e.hp = r.hp;
               ENGINES.add(e);
            }
         }
      }
   }

   public static void tick(MinecraftServer srv) {
      if (!ENGINES.isEmpty() || !PENDING.isEmpty()) {
         t++;
         if (!PENDING.isEmpty() && t % 20 == 0) {
            restorePending(srv);
         }

         if (!ENGINES.isEmpty()) {
            if (t % 2 == 0) {
               double step = Config.d("siege.speed", 1.2) / 10.0;

               for (SiegeEngines.Engine e : ENGINES) {
                  if (e.go && !e.stand.isRemoved() && e.stand.getWorld() instanceof ServerWorld w) {
                     roll(w, e, step);
                  }
               }
            }

            if (t % 10 == 0) {
               int timeout = Config.i("guns.cannon.idle", 1200);
               double dmg = Config.d("siege.damage", 2.0);

               for (SiegeEngines.Engine ex : new ArrayList<>(ENGINES)) {
                  if (ex.stand.isRemoved() && ex.stand.getRemovalReason() == RemovalReason.UNLOADED_TO_CHUNK && ex.k.alive) {
                     PENDING.add(recOf(ex));
                     ENGINES.remove(ex);
                     Models.detach(ex.stand);
                  } else if (ex.stand.getWorld() instanceof ServerWorld w && !ex.stand.isRemoved() && ex.k.alive && ex.k.world == w && atWar(ex.k)) {
                     ex.cool -= 10;
                     ex.idle += 10;
                     ex.crew = crewOf(ex);
                     ex.lonely = ex.crew == null ? ex.lonely + 10 : 0;
                     boolean obsolete = ex.type == SiegeEngines.Type.CATAPULT && Tech.has(ex.k, Tech.T.GUNPOWDER);
                     if (ex.idle <= timeout || ex.lonely <= timeout && !obsolete) {
                        int foes = 0;

                        for (LivingEntity le : Fx.living(w, ex.stand.getPos(), 2.5)) {
                           Kingdoms.K o = Kingdoms.of(le);
                           if (o != null && o != ex.k && !(le instanceof VillagerEntity) && Diplomacy.hostile(ex.k, o)) {
                              foes++;
                           }
                        }

                        if (foes != 0) {
                           ex.hp -= (float)(foes * dmg);
                           if (ex.hp <= 0.0F) {
                              destroy(w, ex);
                           } else {
                              Fx.particles(
                                 w,
                                 new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.getDefaultState()),
                                 ex.stand.getX(),
                                 ex.stand.getY() + 1.0,
                                 ex.stand.getZ(),
                                 6,
                                 0.5,
                                 0.4,
                                 0.5,
                                 0.05
                              );
                              Fx.sound(w, ex.stand.getX(), ex.stand.getY(), ex.stand.getZ(), SoundEvents.BLOCK_WOOD_HIT, 0.8F, 0.8F);
                           }
                        }
                     } else {
                        remove(ex);
                     }
                  } else {
                     remove(ex);
                  }
               }
            }
         }
      }
   }

   public static void clear() {
      for (SiegeEngines.Engine e : ENGINES) {
         Models.detach(e.stand);
         e.stand.discard();
      }

      ENGINES.clear();
      PENDING.clear();
      t = 0;
   }

   private static final class Engine {
      final ArmorStandEntity stand;
      final Kingdoms.K k;
      final SiegeEngines.Type type;
      float hp;
      int cool;
      int idle;
      int lonely;
      MobEntity crew;
      boolean go;
      double gx;
      double gz;

      Engine(ArmorStandEntity stand, Kingdoms.K k, SiegeEngines.Type type) {
         this.stand = stand;
         this.k = k;
         this.type = type;
         this.hp = type.hp();
      }
   }

   private record Rec(SiegeEngines.Type type, Kingdoms.K k, String world, double x, double y, double z, float yaw, float hp) {
   }

   public static enum Type {
      CATAPULT("catapult", "катапульту", "siege.catapult.", 160, 14, 55, 3.0, 40),
      CANNON("cannon", "пушку", "siege.cannon.", 80, 14, 70, 1.5, 60);

      final String model;
      final String ru;
      final String key;
      final int reload;
      final int min;
      final int range;
      final int hp;
      final double spread;

      private Type(String model, String ru, String key, int reload, int min, int range, double spread, int hp) {
         this.model = model;
         this.ru = ru;
         this.key = key;
         this.reload = reload;
         this.min = min;
         this.range = range;
         this.spread = spread;
         this.hp = hp;
      }

      int reload() {
         return Math.max(20, Config.i(this.key + "reload", this.reload));
      }

      int min() {
         return Config.i(this.key + "min", this.min);
      }

      int range() {
         return Math.max(this.min() + 10, Config.i(this.key + "range", this.range));
      }

      double spread() {
         return Config.d(this.key + "spread", this.spread);
      }

      float hp() {
         return (float)Config.d(this.key + "hp", this.hp);
      }

      int count() {
         return Math.max(1, this == CANNON ? Config.i("guns.cannon.max", 3) : Config.i("siege.catapult.count", 3));
      }

      float power() {
         return (float)(this == CANNON ? Config.d("tech.cannon.power", 3.5) : Config.d("siege.catapult.power", 2.5));
      }
   }
}
