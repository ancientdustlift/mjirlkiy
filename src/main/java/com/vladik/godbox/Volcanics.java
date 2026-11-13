package com.vladik.godbox;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World.ExplosionSourceType;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Shared eruption effects: lava bombs and the ash plume. */
final class Volcanics {
   private static final double GRAVITY = 0.045;

   private Volcanics() {
   }

   /**
    * Throws a lava bomb on a ballistic arc. It sets fire to what it hits and leaves magma behind; a {@code blast} bomb also
    * explodes. Without custom models it falls back to the falling magma block used before.
    */
   static void bomb(ServerWorld w, double x, double y, double z, double vx, double vy, double vz, boolean blast) {
      Random r = w.getRandom();
      float scale = blast ? 1.6F : 1.1F;
      ItemDisplayEntity e = Models.prop(w, "volcano", "bomb", x, y, z);
      if (e == null) {
         BlockPos at = BlockPos.ofFloored(x, y, z);
         if (w.getBlockState(at).isAir()) {
            FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(w, at, Blocks.MAGMA_BLOCK.getDefaultState());
            fb.setVelocity(vx, vy, vz);
            fb.velocityModified = true;
            fb.dropItem = false;
            fb.setHurtEntities(2.0F, 20);
         }
      } else {
         Vector3f axis = new Vector3f(r.nextFloat() - 0.5F, r.nextFloat() - 0.5F, r.nextFloat() - 0.5F).add(0.1F, 0.1F, 0.1F).normalize();
         double[] p = new double[]{x, y, z, vx, vy, vz};
         int[] age = new int[]{0};
         Tasks.add(() -> {
            age[0]++;
            double px = p[0];
            double py = p[1];
            double pz = p[2];
            p[0] += p[3];
            p[1] += p[4];
            p[2] += p[5];
            p[4] -= GRAVITY;
            BlockPos at = BlockPos.ofFloored(p[0], p[1], p[2]);
            BlockState st = w.isChunkLoaded(at) ? w.getBlockState(at) : Blocks.AIR.getDefaultState();
            boolean hit = age[0] > 3 && (!st.getCollisionShape(w, at).isEmpty() || !st.getFluidState().isEmpty());
            if (!hit && age[0] < 240 && p[1] > w.getBottomY() && w.isChunkLoaded(at)) {
               e.setPosition(p[0], p[1], p[2]);
               Models.pose(e, new Quaternionf().rotationAxis(age[0] * 0.4F, axis), scale);
               if (age[0] % 2 == 0) {
                  Fx.particles(w, ParticleTypes.FLAME, p[0], p[1], p[2], 2, 0.15, 0.15, 0.15, 0.01);
                  Fx.particles(w, ParticleTypes.SMOKE, p[0], p[1], p[2], 2, 0.15, 0.15, 0.15, 0.01);
               }

               return false;
            } else {
               e.discard();
               if (hit) {
                  BlockPos land = BlockPos.ofFloored(px, py, pz);
                  Fx.particles(w, ParticleTypes.LAVA, px, py, pz, 10, 0.5, 0.3, 0.5, 0.0);
                  if (st.getFluidState().isEmpty()) {
                     if (blast) {
                        w.createExplosion(null, px, py, pz, 1.6F, true, ExplosionSourceType.TNT);
                     } else {
                        Fx.sound(w, px, py, pz, SoundEvents.BLOCK_LAVA_POP, 2.0F, 0.6F);
                     }

                     if (w.getBlockState(land).isReplaceable() && w.getFluidState(land).isEmpty()) {
                        w.setBlockState(land, Blocks.MAGMA_BLOCK.getDefaultState(), 3);
                     }

                     Fx.fire(w, land.up());

                     for (LivingEntity le : Fx.living(w, new Vec3d(px, py, pz), 2.0)) {
                        le.damage(w.getDamageSources().lava(), 4.0F);
                        le.setOnFireFor(4.0F);
                     }
                  } else {
                     // Quenched in water.
                     Fx.particles(w, ParticleTypes.CLOUD, px, py + 0.5, pz, 12, 0.4, 0.4, 0.4, 0.03);
                     Fx.sound(w, px, py, pz, SoundEvents.BLOCK_LAVA_EXTINGUISH, 2.0F, 0.8F);
                  }
               }

               return true;
            }
         });
      }
   }

   /** The ash column standing over a crater. The model is three blocks tall at scale 1, with its base 1.5 below centre. */
   static final class Plume {
      private ItemDisplayEntity e;
      private float pulse;

      void puff() {
         this.pulse = 0.25F;
      }

      void update(ServerWorld w, double x, double baseY, double z, float scale, int age) {
         if (this.e == null) {
            this.e = Models.prop(w, "volcano", "plume", x, baseY, z);
            if (this.e == null) {
               return;
            }

            this.e.setTeleportDuration(3);
            this.e.setInterpolationDuration(3);
         }

         this.pulse *= 0.9F;
         float s = scale * (1.0F + this.pulse);
         this.e.setPosition(x, baseY + 1.5 * s, z);
         Models.pose(this.e, new Quaternionf().rotationY(age * 0.02F), new Vector3f(s, s, s));
      }

      void discard() {
         if (this.e != null) {
            this.e.discard();
            this.e = null;
         }
      }
   }
}
