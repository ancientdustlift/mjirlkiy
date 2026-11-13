package com.vladik.godbox;

import com.vladik.godbox.net.Payloads;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockPos.Mutable;

public final class Blast {
   private static final Map<ServerWorld, Blast.State> STATES = new WeakHashMap<>();

   private Blast() {
   }

   private static Blast.State state(ServerWorld w) {
      Blast.State s = STATES.computeIfAbsent(w, k -> new Blast.State());
      long t = w.getTime();
      if (s.tick != t) {
         s.tick = t;
         s.used = 0;
         s.fullFx = 0;
         s.scorches = 0;
      }

      return s;
   }

   private static int take(ServerWorld w, int want) {
      Blast.State s = state(w);
      int left = Config.i("fx.blast.budget", 500) - s.used;
      int n = Math.max(0, Math.min(want, left));
      s.used += n;
      return n;
   }

   private static void send(ServerWorld w, ParticleEffect e, boolean far, double x, double y, double z, double vx, double vy, double vz) {
      double range = far ? 160.0 : 48.0;

      for (ServerPlayerEntity p : w.getPlayers()) {
         if (p.squaredDistanceTo(x, y, z) <= range * range && ServerPlayNetworking.canSend(p, Payloads.State.ID)) {
            w.spawnParticles(p, e, far, x, y, z, 0, vx, vy, vz, 1.0);
         }
      }
   }

   public static void fx(ServerWorld w, double x, double y, double z, float power) {
      if (Config.b("fx.blast.enabled", true)) {
         if (power >= 0.5F && !Float.isInfinite(power)) {
            if (!w.getPlayers().isEmpty()) {
               if (w.isChunkLoaded(MathHelper.floor(x) >> 4, MathHelper.floor(z) >> 4)) {
                  double scale = MathHelper.clamp(Config.d("fx.blast.scale", 1.0), 0.0, 3.0);
                  if (!(scale <= 0.0)) {
                     Blast.State st = state(w);
                     int near = 0;

                     for (int i = 0; i < Math.min(st.fullFx, st.xs.length); i++) {
                        double dx = st.xs[i] - x;
                        double dz = st.zs[i] - z;
                        if (dx * dx + dz * dz < 36.0) {
                           near++;
                        }
                     }

                     if (near < 3 && st.fullFx < Config.i("fx.blast.maxFull", 24)) {
                        if (st.fullFx < st.xs.length) {
                           st.xs[st.fullFx] = x;
                           st.zs[st.fullFx] = z;
                        }

                        st.fullFx++;
                        float p = Math.min(power, 60.0F);
                        boolean small = p < 2.5F;
                        boolean large = p > 8.0F;
                        boolean far = p >= 6.0F;
                        float half = (float)Math.min(26.0, (0.7 + 0.55 * p) * scale);
                        int life = (int)Math.min(30.0F, 10.0F + p * 0.5F);
                        send(w, GodParticles.FIREBALL, far, x, y, z, half * (small ? 0.8 : 1.0), life, 0.0);
                        if (!small) {
                           int blobs = take(w, Math.min(6, (int)(p / 1.5F)));
                           ThreadLocalRandom rnd = ThreadLocalRandom.current();

                           for (int ix = 0; ix < blobs; ix++) {
                              double r = half * 0.55;
                              send(
                                 w,
                                 GodParticles.FIREBALL,
                                 far,
                                 x + (rnd.nextDouble() - 0.5) * r * 2.0,
                                 y + rnd.nextDouble() * r * (large ? 2.0 : 1.0),
                                 z + (rnd.nextDouble() - 0.5) * r * 2.0,
                                 half * (0.45 + rnd.nextDouble() * 0.25),
                                 life - 2 + rnd.nextInt(6),
                                 0.0
                              );
                           }
                        }

                        if (!small) {
                           double gy = groundY(w, x, y, z);
                           if (gy > Double.NEGATIVE_INFINITY) {
                              float rad = (float)Math.min(40.0, (2.5 + p * 1.2) * scale);
                              int rl = (int)Math.min(30.0, 10.0 + rad * 0.4);
                              send(w, GodParticles.SHOCKWAVE, far, x, gy + 0.06, z, rad, rl, 0.0);
                              if (large) {
                                 double fy = gy + 0.08;
                                 Tasks.delay(4, () -> send(w, GodParticles.SHOCKWAVE, true, x, fy, z, rad * 1.5, rl + 6, 0.0));
                              }
                           }
                        }

                        ThreadLocalRandom rnd = ThreadLocalRandom.current();
                        int embers = take(w, (int)Math.min(40.0, (small ? 3 : 6) + p * 2.2 * scale));

                        for (int ix = 0; ix < embers; ix++) {
                           double a = rnd.nextDouble() * Math.PI * 2.0;
                           double sp = (0.12 + rnd.nextDouble() * 0.3) * (0.7 + p * 0.06);
                           send(w, GodParticles.EMBER, far, x, y + 0.3, z, Math.cos(a) * sp, 0.25 + rnd.nextDouble() * 0.45, Math.sin(a) * sp);
                        }

                        if (!small) {
                           int debris = take(w, (int)Math.min(26.0, 4.0 + p * 2.0F * scale));

                           for (int ix = 0; ix < debris; ix++) {
                              double a = rnd.nextDouble() * Math.PI * 2.0;
                              double sp = (0.1 + rnd.nextDouble() * 0.35) * (0.8 + p * 0.05);
                              send(w, GodParticles.DEBRIS, far, x, y + 0.2, z, Math.cos(a) * sp, 0.2 + rnd.nextDouble() * 0.5, Math.sin(a) * sp);
                           }
                        }

                        if (small) {
                           int puffs = take(w, 2);

                           for (int ix = 0; ix < puffs; ix++) {
                              send(
                                 w,
                                 GodParticles.PLUME,
                                 false,
                                 x + (rnd.nextDouble() - 0.5) * 0.6,
                                 y + 0.3,
                                 z + (rnd.nextDouble() - 0.5) * 0.6,
                                 0.7,
                                 0.03,
                                 30 + rnd.nextInt(15)
                              );
                           }
                        } else {
                           plume(w, x, y, z, p, far, scale);
                        }

                        if (large) {
                           rain(w, x, y, z, p, scale);
                        }

                        sounds(w, x, y, z, p);
                        if (p >= 3.0F && Config.b("fx.blast.scorch", true) && st.scorches++ < Config.i("fx.blast.maxScorch", 12)) {
                           scorch(w, x, y, z, p);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void plume(ServerWorld w, double x, double y, double z, float p, boolean far, double scale) {
      final boolean large = p > 8.0F;
      final int ticks = large ? (int)Math.min(80.0F, 40.0F + p) : 16;
      final int perTick = large ? 2 : 1;
      final float base = (float)((0.9 + p * 0.1) * scale);
      final double rad = Math.min(8.0, 0.4 + p * 0.12);
      Tasks.add(
         new Tasks.Task() {
            int t;

            @Override
            public boolean tick() {
               if (!w.isChunkLoaded(MathHelper.floor(x) >> 4, MathHelper.floor(z) >> 4)) {
                  return true;
               } else {
                  ThreadLocalRandom r = ThreadLocalRandom.current();
                  int n = Blast.take(w, perTick);

                  for (int i = 0; i < n; i++) {
                     boolean cap = large && this.t > ticks * 0.55;
                     double spread = cap ? rad * 2.8 : rad * (0.5 + 0.02 * this.t);
                     double a = r.nextDouble() * Math.PI * 2.0;
                     double d = Math.sqrt(r.nextDouble()) * spread;
                     double py = y + 0.5 + this.t * (large ? 0.4 : 0.2) + r.nextDouble();
                     Blast.send(
                        w,
                        GodParticles.PLUME,
                        far,
                        x + Math.cos(a) * d,
                        py,
                        z + Math.sin(a) * d,
                        base * (0.7 + r.nextDouble() * 0.6),
                        0.03 + r.nextDouble() * 0.03,
                        (double)((large ? 90 : 45) + r.nextInt(50))
                     );
                  }

                  return ++this.t >= ticks;
               }
            }
         }
      );
   }

   private static void rain(ServerWorld w, double x, double y, double z, float p, double scale) {
      final double rad = Math.min(24.0, 4.0 + p * 0.5);
      Tasks.add(
         new Tasks.Task() {
            int t;

            @Override
            public boolean tick() {
               if (!w.isChunkLoaded(MathHelper.floor(x) >> 4, MathHelper.floor(z) >> 4)) {
                  return true;
               } else {
                  ThreadLocalRandom r = ThreadLocalRandom.current();
                  int n = Blast.take(w, (int)(3.0 * scale));

                  for (int i = 0; i < n; i++) {
                     double a = r.nextDouble() * Math.PI * 2.0;
                     double d = Math.sqrt(r.nextDouble()) * rad;
                     Blast.send(
                        w,
                        GodParticles.EMBER,
                        true,
                        x + Math.cos(a) * d,
                        y + 8.0 + r.nextDouble() * 10.0,
                        z + Math.sin(a) * d,
                        (r.nextDouble() - 0.5) * 0.06,
                        -0.05,
                        (r.nextDouble() - 0.5) * 0.06
                     );
                  }

                  return ++this.t >= 50;
               }
            }
         }
      );
   }

   private static void sounds(ServerWorld w, double x, double y, double z, float p) {
      if (Config.b("fx.blast.sound", true)) {
         if (p >= 5.0F) {
            float v = Math.min(4.0F, 1.5F + p * 0.1F);
            Tasks.delay(5, () -> w.playSound(null, x, y, z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, v, 0.5F));
         }

         if (p >= 10.0F) {
            Tasks.delay(14, () -> w.playSound(null, x, y, z, SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.BLOCKS, 3.0F, 0.55F));
         }
      }
   }

   private static double groundY(ServerWorld w, double x, double y, double z) {
      int bx = MathHelper.floor(x);
      int bz = MathHelper.floor(z);
      int by = MathHelper.floor(y);
      if (!w.isChunkLoaded(bx >> 4, bz >> 4)) {
         return Double.NEGATIVE_INFINITY;
      } else {
         Mutable m = new Mutable();

         for (int dy = 0; dy <= 8; dy++) {
            m.set(bx, by - dy, bz);
            if (w.isOutOfHeightLimit(m)) {
               break;
            }

            BlockState s = w.getBlockState(m);
            if (!s.isAir() && s.getFluidState().isEmpty() && !s.getCollisionShape(w, m).isEmpty()) {
               return m.getY() + 1;
            }
         }

         return Double.NEGATIVE_INFINITY;
      }
   }

   private static BlockState charred(BlockState s) {
      Block b = s.getBlock();
      if (b == Blocks.GRASS_BLOCK || b == Blocks.DIRT || b == Blocks.PODZOL || b == Blocks.MYCELIUM) {
         return Blocks.COARSE_DIRT.getDefaultState();
      } else if (b == Blocks.SAND || b == Blocks.RED_SAND) {
         return Blocks.GRAVEL.getDefaultState();
      } else if (b == Blocks.STONE || b == Blocks.COBBLESTONE || b == Blocks.ANDESITE || b == Blocks.DIORITE || b == Blocks.GRANITE) {
         return Blocks.BLACKSTONE.getDefaultState();
      } else {
         return b == Blocks.DEEPSLATE ? Blocks.COBBLED_DEEPSLATE.getDefaultState() : null;
      }
   }

   private static void scorch(ServerWorld w, double x, double y, double z, float p) {
      final int rad = (int)Math.min(12.0F, 2.0F + p * 1.1F);
      final int total = (int)Math.min(90.0F, 6.0F + p * 5.0F);
      Tasks.add(new Tasks.Task() {
         int done;

         @Override
         public boolean tick() {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            Mutable m = new Mutable();
            int i = 0;

            while (i < 12 && this.done < total) {
               double a = r.nextDouble() * Math.PI * 2.0;
               double d = Math.sqrt(r.nextDouble()) * rad;
               int bx = MathHelper.floor(x + Math.cos(a) * d);
               int bz = MathHelper.floor(z + Math.sin(a) * d);
               if (w.isChunkLoaded(bx >> 4, bz >> 4)) {
                  for (int by = MathHelper.floor(y) + 3; by >= MathHelper.floor(y) - rad; by--) {
                     m.set(bx, by, bz);
                     if (w.isOutOfHeightLimit(m)) {
                        break;
                     }

                     BlockState s = w.getBlockState(m);
                     if (!s.isAir()) {
                        BlockState n = Blast.charred(s);
                        if (n != null && w.getBlockState(m.up()).isAir() && !w.getBlockState(m).hasBlockEntity()) {
                           w.setBlockState(m, n, 2);
                        }
                        break;
                     }
                  }
               }

               i++;
               this.done++;
            }

            return this.done >= total;
         }
      });
   }

   private static final class State {
      long tick = -1L;
      int used;
      int fullFx;
      int scorches;
      final double[] xs = new double[8];
      final double[] zs = new double[8];
   }
}
