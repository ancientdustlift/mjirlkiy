package com.vladik.godbox;

import java.util.ArrayList;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World.ExplosionSourceType;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class Disasters {
   private Disasters() {
   }

   public static final class Atomic implements Tasks.Task {
      private final ServerWorld w;
      private final double x;
      private final double z;
      private final double gy;
      private final String part;
      private final float scale;
      private int t = 0;
      private ItemDisplayEntity bomb;

      public Atomic(ServerWorld w, BlockPos c) {
         this(w, c, "atomic", 3.5F);
      }

      public Atomic(ServerWorld w, BlockPos c, String part, float scale) {
         this.w = w;
         this.x = c.getX() + 0.5;
         this.z = c.getZ() + 0.5;
         this.gy = Fx.surface(w, c.getX(), c.getZ()).getY();
         this.part = part;
         this.scale = scale;
      }

      @Override
      public boolean tick() {
         Random r = this.w.getRandom();
         if (this.t == 0) {
            Fx.sound(this.w, this.x, this.gy + 20.0, this.z, SoundEvents.ENTITY_WITHER_SPAWN, 8.0F, 0.5F);
         }

         if (this.t < 20) {
            double y = this.gy + 60.0 - this.t * 3;
            if (this.t == 0) {
               this.bomb = Models.prop(this.w, "bombs", this.part, this.x, y, this.z);
            }

            if (this.bomb != null) {
               this.bomb.setPosition(this.x, y, this.z);
               Models.pose(this.bomb, new Quaternionf().rotationY(this.t * 0.12F), this.scale);
            }

            Fx.particles(this.w, ParticleTypes.LARGE_SMOKE, this.x, y, this.z, 6, 0.2, 0.2, 0.2, 0.01);
            Fx.particles(this.w, ParticleTypes.FLAME, this.x, y, this.z, 6, 0.2, 0.2, 0.2, 0.01);
         }

         if (this.t == 20) {
            if (this.bomb != null) {
               this.bomb.discard();
               this.bomb = null;
            }

            Fx.particles(this.w, ParticleTypes.FLASH, this.x, this.gy + 3.0, this.z, 6, 3.0, 3.0, 3.0, 0.0);
            this.w.createExplosion(null, this.x, this.gy, this.z, 12.0F, true, ExplosionSourceType.TNT);
            Fx.particles(this.w, ParticleTypes.EXPLOSION_EMITTER, this.x, this.gy + 2.0, this.z, 25, 9.0, 4.0, 9.0, 0.0);
            Fx.sound(this.w, this.x, this.gy + 10.0, this.z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 12.0F, 0.4F);

            for (LivingEntity e : Fx.living(this.w, new Vec3d(this.x, this.gy, this.z), 40.0)) {
               e.setOnFireFor(10.0F);
               Vec3d d = e.getPos().subtract(this.x, this.gy, this.z);
               double l = Math.max(1.0, d.length());
               Fx.push(e, d.x / l * 2.5, 1.2, d.z / l * 2.5);
            }
         }

         if (this.t > 20 && this.t <= 28) {
            double a = (this.t - 21) * Math.PI / 4.0;
            double rx = this.x + Math.cos(a) * 12.0;
            double rz = this.z + Math.sin(a) * 12.0;
            BlockPos s = Fx.surface(this.w, MathHelper.floor(rx), MathHelper.floor(rz));
            this.w.createExplosion(null, rx, s.getY(), rz, 7.0F, true, ExplosionSourceType.TNT);
         }

         if (this.t > 28 && this.t <= 50) {
            for (int i = 0; i < 5; i++) {
               double a = r.nextDouble() * Math.PI * 2.0;
               double d = 10.0 + r.nextDouble() * 20.0;
               Fx.fire(this.w, Fx.surface(this.w, MathHelper.floor(this.x + Math.cos(a) * d), MathHelper.floor(this.z + Math.sin(a) * d)));
            }
         }

         if (this.t >= 20) {
            double h = Math.min(40.0, (this.t - 20) * 1.5);

            for (int i = 0; i < 12; i++) {
               Fx.particles(
                  this.w,
                  ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                  this.x + (r.nextDouble() - 0.5) * 5.0,
                  this.gy + r.nextDouble() * h,
                  this.z + (r.nextDouble() - 0.5) * 5.0,
                  1,
                  0.0,
                  0.0,
                  0.0,
                  0.01
               );
            }

            for (int i = 0; i < 18; i++) {
               double a = r.nextDouble() * Math.PI * 2.0;
               double d = r.nextDouble() * (4.0 + h * 0.35);
               Fx.particles(
                  this.w,
                  ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                  this.x + Math.cos(a) * d,
                  this.gy + h + (r.nextDouble() - 0.3) * 6.0,
                  this.z + Math.sin(a) * d,
                  1,
                  0.0,
                  0.0,
                  0.0,
                  0.01
               );
            }

            if (this.t % 3 == 0) {
               Fx.particles(this.w, ParticleTypes.LAVA, this.x, this.gy + h, this.z, 10, 6.0, 2.0, 6.0, 0.0);
            }
         }

         return ++this.t > 220;
      }
   }

   public static final class Light implements Tasks.Task {
      private final ServerWorld w;
      private final BlockPos c;
      private final int gy;
      private int t = 0;

      public Light(ServerWorld w, BlockPos c) {
         this.w = w;
         this.c = c;
         this.gy = Fx.surface(w, c.getX(), c.getZ()).getY();
      }

      @Override
      public boolean tick() {
         Random r = this.w.getRandom();
         double x = this.c.getX() + 0.5;
         double z = this.c.getZ() + 0.5;
         if (this.t == 0) {
            Fx.sound(this.w, x, this.gy + 10, z, SoundEvents.BLOCK_BEACON_ACTIVATE, 6.0F, 1.2F);
            Fx.sound(this.w, x, this.gy + 10, z, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 6.0F, 0.8F);
            Fx.particles(this.w, ParticleTypes.FLASH, x, this.gy + 1, z, 2, 0.0, 0.0, 0.0, 0.0);
         }

         for (int i = 0; i < 40; i++) {
            double a = r.nextDouble() * Math.PI * 2.0;
            double d = r.nextDouble() * 1.6;
            Fx.particles(this.w, ParticleTypes.END_ROD, x + Math.cos(a) * d, this.gy + r.nextDouble() * 60.0, z + Math.sin(a) * d, 1, 0.0, 0.0, 0.0, 0.0);
         }

         if (this.t % 2 == 0) {
            double ring = 2.5 + this.t % 20 * 0.25;

            for (int i = 0; i < 24; i++) {
               double a = i * Math.PI / 12.0 + this.t * 0.1;
               Fx.particles(this.w, ParticleTypes.GLOW, x + Math.cos(a) * ring, this.gy + 0.2, z + Math.sin(a) * ring, 1, 0.0, 0.0, 0.0, 0.0);
            }
         }

         if (this.t % 10 == 0) {
            for (LivingEntity e : Fx.living(this.w, new Vec3d(x, this.gy, z), 7.0)) {
               if (e.getType().isIn(EntityTypeTags.UNDEAD)) {
                  e.damage(this.w.getDamageSources().magic(), 8.0F);
                  e.setOnFireFor(5.0F);
               } else {
                  e.heal(6.0F);

                  for (StatusEffectInstance in : new ArrayList<>(e.getStatusEffects())) {
                     if (!in.getEffectType().value().isBeneficial()) {
                        e.removeStatusEffect(in.getEffectType());
                     }
                  }

                  e.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1));
                  e.extinguish();
               }
            }

            for (BlockPos p : Fx.brush(this.w, this.c, 6)) {
               if (this.w.getBlockState(p).isIn(BlockTags.FIRE)) {
                  this.w.setBlockState(p, Blocks.AIR.getDefaultState());
               }
            }
         }

         return ++this.t > 80;
      }
   }

   public static final class Meteor implements Tasks.Task {
      private static final int DURATION = 30;
      private static final float SCALE = 3.0F;
      private final ServerWorld w;
      private final BlockPos target;
      private final double sx;
      private final double sy;
      private final double sz;
      private int t = 0;
      private ItemDisplayEntity rock;
      private ItemDisplayEntity fire;
      private Vector3f spin;

      public Meteor(ServerWorld w, BlockPos target) {
         this.w = w;
         this.target = target;
         double a = w.getRandom().nextDouble() * Math.PI * 2.0;
         this.sx = target.getX() + 0.5 + Math.cos(a) * 30.0;
         this.sz = target.getZ() + 0.5 + Math.sin(a) * 30.0;
         this.sy = target.getY() + 80;
      }

      private void spawnModel() {
         this.rock = Models.prop(this.w, "meteor", "rock", this.sx, this.sy, this.sz);
         this.fire = Models.prop(this.w, "meteor", "fire", this.sx, this.sy, this.sz);
         Random r = this.w.getRandom();
         this.spin = new Vector3f(r.nextFloat() - 0.5F, r.nextFloat() - 0.5F, r.nextFloat() - 0.5F).add(0.1F, 0.1F, 0.1F).normalize();
         if (this.fire != null) {
            // The fire model trails along +Y; point that back along the flight path.
            Vector3f back = new Vector3f(
                  (float)(this.sx - (this.target.getX() + 0.5)), (float)(this.sy - (this.target.getY() + 1.0)), (float)(this.sz - (this.target.getZ() + 0.5))
               )
               .normalize();
            Models.pose(this.fire, new Quaternionf().rotationTo(new Vector3f(0.0F, 1.0F, 0.0F), back), SCALE);
         }
      }

      private void moveModel(double x, double y, double z) {
         if (this.rock != null) {
            this.rock.setPosition(x, y, z);
            Models.pose(this.rock, new Quaternionf().rotationAxis(this.t * 0.45F, this.spin), SCALE);
         }

         if (this.fire != null) {
            this.fire.setPosition(x, y, z);
         }
      }

      private void removeModel() {
         if (this.rock != null) {
            this.rock.discard();
         }

         if (this.fire != null) {
            this.fire.discard();
         }
      }

      @Override
      public boolean tick() {
         double k = this.t / 30.0;
         double x = MathHelper.lerp(k, this.sx, this.target.getX() + 0.5);
         double y = MathHelper.lerp(k, this.sy, this.target.getY() + 1.0);
         double z = MathHelper.lerp(k, this.sz, this.target.getZ() + 0.5);
         if (this.t == 0) {
            Fx.sound(this.w, this.target.getX(), this.target.getY() + 20, this.target.getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, 6.0F, 0.4F);
            this.spawnModel();
         }

         this.moveModel(x, y, z);
         Fx.particles(this.w, ParticleTypes.FLAME, x, y, z, 30, 0.7, 0.7, 0.7, 0.05);
         Fx.particles(this.w, ParticleTypes.LARGE_SMOKE, x, y, z, 15, 0.8, 0.8, 0.8, 0.02);
         Fx.particles(this.w, ParticleTypes.LAVA, x, y, z, 4, 0.5, 0.5, 0.5, 0.0);
         if (this.t < 30) {
            this.t++;
            return false;
         } else {
            this.removeModel();
            double cx = this.target.getX() + 0.5;
            double cy = this.target.getY() + 1;
            double cz = this.target.getZ() + 0.5;
            this.w.createExplosion(null, cx, cy, cz, 7.0F, true, ExplosionSourceType.TNT);
            Fx.particles(this.w, ParticleTypes.EXPLOSION_EMITTER, cx, cy, cz, 6, 3.0, 2.0, 3.0, 0.0);
            Random r = this.w.getRandom();

            for (BlockPos p : Fx.brush(this.w, this.target, 6)) {
               BlockPos g = p.down();
               if (r.nextFloat() < 0.45F && Fx.breakable(this.w, g, this.w.getBlockState(g), 50.0F)) {
                  this.w.setBlockState(g, r.nextFloat() < 0.7F ? Blocks.MAGMA_BLOCK.getDefaultState() : Blocks.OBSIDIAN.getDefaultState());
               } else if (r.nextFloat() < 0.2F) {
                  Fx.fire(this.w, p);
               }
            }

            return true;
         }
      }
   }

   public static final class Quake implements Tasks.Task {
      private static final int R = 18;
      private final ServerWorld w;
      private final BlockPos c;
      private int life = 140;

      public Quake(ServerWorld w, BlockPos c) {
         this.w = w;
         this.c = c;
      }

      @Override
      public boolean tick() {
         Random r = this.w.getRandom();

         for (int i = 0; i < 5; i++) {
            BlockPos top = Fx.surface(this.w, this.c.getX() + r.nextInt(37) - 18, this.c.getZ() + r.nextInt(37) - 18).down();
            BlockState s = this.w.getBlockState(top);
            if (Fx.breakable(this.w, top, s, 5.0F) && s.getFluidState().isEmpty() && !s.hasBlockEntity()) {
               Fx.particles(
                  this.w, new BlockStateParticleEffect(ParticleTypes.BLOCK, s), top.getX() + 0.5, top.getY() + 1, top.getZ() + 0.5, 12, 0.5, 0.2, 0.5, 0.1
               );
               FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(this.w, top, s);
               fb.setVelocity((r.nextDouble() - 0.5) * 0.2, 0.25 + r.nextDouble() * 0.35, (r.nextDouble() - 0.5) * 0.2);
               fb.velocityModified = true;
            }
         }

         if (this.life % 10 == 0) {
            double a = r.nextDouble() * Math.PI * 2.0;
            int sx = this.c.getX() + r.nextInt(37) - 18;
            int sz = this.c.getZ() + r.nextInt(37) - 18;

            for (int l = 0; l < 7; l++) {
               int bx = sx + (int)Math.round(Math.cos(a) * l);
               int bz = sz + (int)Math.round(Math.sin(a) * l);
               BlockPos top = Fx.surface(this.w, bx, bz).down();

               for (int d = 0; d < 3; d++) {
                  BlockPos q = top.down(d);
                  BlockState s = this.w.getBlockState(q);
                  if (Fx.breakable(this.w, q, s, 50.0F) && s.getFluidState().isEmpty()) {
                     this.w.setBlockState(q, Blocks.AIR.getDefaultState());
                  }
               }
            }

            Fx.sound(this.w, this.c.getX(), this.c.getY() + 5, this.c.getZ(), SoundEvents.BLOCK_STONE_BREAK, 8.0F, 0.4F);
         }

         if (this.life % 3 == 0) {
            for (LivingEntity e : Fx.living(this.w, Vec3d.ofCenter(this.c), 18.0)) {
               if (e.isOnGround()) {
                  Fx.push(e, (r.nextDouble() - 0.5) * 0.4, 0.15, (r.nextDouble() - 0.5) * 0.4);
               }
            }
         }

         return --this.life <= 0;
      }
   }

   public static final class Tornado implements Tasks.Task {
      private final ServerWorld w;
      private double x;
      private double z;
      private double vx;
      private double vz;
      private int life = 400;
      private ItemDisplayEntity funnel;
      private ItemDisplayEntity skirt;

      // The funnel model is 3 blocks tall at scale 1 and is centred 1.5 blocks above its base.
      private static final float FUNNEL_SCALE = 26.0F / 3.0F;

      private void model(int gy) {
         if (this.life == 400) {
            this.funnel = Models.prop(this.w, "tornado", "funnel", this.x, gy, this.z);
            this.skirt = Models.prop(this.w, "tornado", "skirt", this.x, gy, this.z);
         }

         double y = gy + 1.5 * FUNNEL_SCALE;
         float turn = (400 - this.life) * 0.5F;
         if (this.funnel != null) {
            this.funnel.setPosition(this.x, y, this.z);
            Models.pose(this.funnel, new Quaternionf().rotationY(turn), FUNNEL_SCALE);
         }

         if (this.skirt != null) {
            this.skirt.setPosition(this.x, y, this.z);
            Models.pose(this.skirt, new Quaternionf().rotationY(-turn * 1.6F), FUNNEL_SCALE);
         }

         if (this.life <= 1) {
            if (this.funnel != null) {
               this.funnel.discard();
            }

            if (this.skirt != null) {
               this.skirt.discard();
            }
         }
      }

      public Tornado(ServerWorld w, double x, double z) {
         this.w = w;
         this.x = x;
         this.z = z;
      }

      @Override
      public boolean tick() {
         Random r = this.w.getRandom();
         this.vx = MathHelper.clamp(this.vx + (r.nextDouble() - 0.5) * 0.06, -0.25, 0.25);
         this.vz = MathHelper.clamp(this.vz + (r.nextDouble() - 0.5) * 0.06, -0.25, 0.25);
         this.x = this.x + this.vx;
         this.z = this.z + this.vz;
         int gy = Fx.surface(this.w, MathHelper.floor(this.x), MathHelper.floor(this.z)).getY();
         this.model(gy);

         for (int i = 0; i < 30; i++) {
            double h = r.nextDouble() * 26.0;
            double a = r.nextDouble() * Math.PI * 2.0;
            double rad = 0.5 + h * 0.22;
            Fx.particles(this.w, ParticleTypes.CLOUD, this.x + Math.cos(a) * rad, gy + h, this.z + Math.sin(a) * rad, 1, 0.0, 0.0, 0.0, 0.0);
         }

         Box box = new Box(this.x - 10.0, gy - 4, this.z - 10.0, this.x + 10.0, gy + 32, this.z + 10.0);

         for (Entity e : this.w.getOtherEntities(null, box, en -> !GodManager.isGodEntity(en) && !en.isSpectator() && !(en instanceof DisplayEntity))) {
            double dx = this.x - e.getX();
            double dz = this.z - e.getZ();
            double d = Math.max(0.5, Math.sqrt(dx * dx + dz * dz));
            if (!(d > 10.0)) {
               double tx = -dz / d * 0.45;
               double tz = dx / d * 0.45;
               double up = e.getY() < gy + 24 ? 0.16 + (d < 3.0 ? 0.12 : 0.0) : -0.05;
               e.setVelocity(e.getVelocity().multiply(0.75).add(dx / d * 0.16 + tx, up, dz / d * 0.16 + tz));
               e.velocityModified = true;
            }
         }

         if (this.life % 2 == 0) {
            for (int k = 0; k < 2; k++) {
               BlockPos top = Fx.surface(this.w, MathHelper.floor(this.x + r.nextDouble() * 6.0 - 3.0), MathHelper.floor(this.z + r.nextDouble() * 6.0 - 3.0))
                  .down();
               BlockState s = this.w.getBlockState(top);
               if (Fx.breakable(this.w, top, s, 2.5F) && s.getFluidState().isEmpty() && !s.hasBlockEntity()) {
                  FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(this.w, top, s);
                  fb.dropItem = false;
                  fb.setVelocity(r.nextDouble() - 0.5, 0.6 + r.nextDouble() * 0.5, r.nextDouble() - 0.5);
                  fb.velocityModified = true;
               }
            }
         }

         if (this.life % 20 == 0) {
            Fx.sound(this.w, this.x, gy + 5, this.z, SoundEvents.ITEM_ELYTRA_FLYING, 5.0F, 0.6F);
         }

         return --this.life <= 0;
      }
   }
}
