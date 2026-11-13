package com.vladik.godbox.client;

import com.vladik.godbox.GodParticles;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class GodParticlesClient {
   private static final int FULL_BRIGHT = 15728880;

   private GodParticlesClient() {
   }

   public static void init() {
      ParticleFactoryRegistry r = ParticleFactoryRegistry.getInstance();
      r.register(GodParticles.FIREBALL, sp -> (t, w, x, y, z, vx, vy, vz) -> new GodParticlesClient.Fireball(w, x, y, z, vx, vy, sp));
      r.register(GodParticles.SHOCKWAVE, sp -> (t, w, x, y, z, vx, vy, vz) -> new GodParticlesClient.Shockwave(w, x, y, z, vx, vy, sp));
      r.register(GodParticles.PLUME, sp -> (t, w, x, y, z, vx, vy, vz) -> new GodParticlesClient.Plume(w, x, y, z, vx, vy, vz, sp));
      r.register(GodParticles.EMBER, sp -> (t, w, x, y, z, vx, vy, vz) -> new GodParticlesClient.Ember(w, x, y, z, vx, vy, vz, sp));
      r.register(GodParticles.DEBRIS, sp -> (t, w, x, y, z, vx, vy, vz) -> new GodParticlesClient.Debris(w, x, y, z, vx, vy, vz, sp));
   }

   private static float frac(int age, int max) {
      return max <= 0 ? 1.0F : Math.min(1.0F, (float)age / max);
   }

   static final class Debris extends SpriteBillboardParticle {
      private final float spin;

      Debris(ClientWorld w, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sp) {
         super(w, x, y, z);
         this.velocityX = vx;
         this.velocityY = vy;
         this.velocityZ = vz;
         this.scale = 0.07F + this.random.nextFloat() * 0.1F;
         this.maxAge = 28 + this.random.nextInt(30);
         this.gravityStrength = 1.0F;
         this.collidesWithWorld = true;
         this.setBoundingBoxSpacing(0.06F, 0.06F);
         float g = 0.45F + this.random.nextFloat() * 0.4F;
         this.setColor(g * 1.05F, g, g * 0.9F);
         this.spin = (this.random.nextFloat() - 0.5F) * 0.5F;
         this.angle = this.random.nextFloat() * 6.28F;
         this.setSprite(sp);
      }

      @Override
      public void tick() {
         this.prevPosX = this.x;
         this.prevPosY = this.y;
         this.prevPosZ = this.z;
         this.prevAngle = this.angle;
         if (this.age++ >= this.maxAge) {
            this.markDead();
         } else {
            this.velocityY = this.velocityY - 0.04 * this.gravityStrength;
            this.move(this.velocityX, this.velocityY, this.velocityZ);
            this.velocityX *= 0.96;
            this.velocityY *= 0.98;
            this.velocityZ *= 0.96;
            if (this.onGround) {
               this.velocityX *= 0.5;
               this.velocityZ *= 0.5;
            } else {
               this.angle = this.angle + this.spin;
            }

            this.alpha = this.age > this.maxAge - 8 ? Math.max(0.0F, (this.maxAge - this.age) / 8.0F) : 1.0F;
         }
      }

      @Override
      public ParticleTextureSheet getType() {
         return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
      }
   }

   static final class Ember extends SpriteBillboardParticle {
      private final SpriteProvider sprites;

      Ember(ClientWorld w, double x, double y, double z, double vx, double vy, double vz, SpriteProvider sp) {
         super(w, x, y, z);
         this.sprites = sp;
         this.velocityX = vx;
         this.velocityY = vy;
         this.velocityZ = vz;
         this.scale = 0.09F + this.random.nextFloat() * 0.07F;
         this.maxAge = 30 + this.random.nextInt(40);
         this.gravityStrength = 0.9F;
         this.collidesWithWorld = true;
         this.setBoundingBoxSpacing(0.05F, 0.05F);
         this.setSpriteForAge(sp);
      }

      @Override
      public void tick() {
         this.prevPosX = this.x;
         this.prevPosY = this.y;
         this.prevPosZ = this.z;
         if (this.age++ >= this.maxAge) {
            this.markDead();
         } else {
            this.setSpriteForAge(this.sprites);
            float k = GodParticlesClient.frac(this.age, this.maxAge);
            this.alpha = k < 0.67F ? 1.0F : Math.max(0.0F, (1.0F - k) / 0.33F);
            this.velocityY = this.velocityY - 0.04 * this.gravityStrength;
            this.move(this.velocityX, this.velocityY, this.velocityZ);
            this.velocityX *= 0.97;
            this.velocityY *= 0.98;
            this.velocityZ *= 0.97;
            if (this.onGround) {
               this.velocityX *= 0.6;
               this.velocityZ *= 0.6;
               this.velocityY = Math.abs(this.velocityY) * 0.35;
            }
         }
      }

      @Override
      protected int getBrightness(float td) {
         return GodParticlesClient.frac(this.age, this.maxAge) < 0.7F ? 15728880 : super.getBrightness(td);
      }

      @Override
      public ParticleTextureSheet getType() {
         return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
      }
   }

   static final class Fireball extends SpriteBillboardParticle {
      private final SpriteProvider sprites;
      private final float size0;

      Fireball(ClientWorld w, double x, double y, double z, double size, double life, SpriteProvider sp) {
         super(w, x, y, z);
         this.sprites = sp;
         this.size0 = (float)MathHelper.clamp(size, 0.2, 40.0);
         this.scale = this.size0;
         this.maxAge = MathHelper.clamp((int)life, 4, 60);
         this.collidesWithWorld = false;
         this.velocityY = 0.012 + this.size0 * 0.004;
         this.setSpriteForAge(sp);
      }

      @Override
      public void tick() {
         this.prevPosX = this.x;
         this.prevPosY = this.y;
         this.prevPosZ = this.z;
         if (this.age++ >= this.maxAge) {
            this.markDead();
         } else {
            this.setSpriteForAge(this.sprites);
            this.y = this.y + this.velocityY;
            float k = GodParticlesClient.frac(this.age, this.maxAge);
            this.alpha = k < 0.65F ? 1.0F : Math.max(0.0F, 1.0F - (k - 0.65F) / 0.35F);
         }
      }

      @Override
      public float getSize(float td) {
         float k = GodParticlesClient.frac(this.age, this.maxAge);
         return this.size0 * (0.75F + 0.35F * (float)Math.sin(Math.min(1.0F, k * 1.6F) * Math.PI / 2.0));
      }

      @Override
      protected int getBrightness(float td) {
         return 15728880;
      }

      @Override
      public ParticleTextureSheet getType() {
         return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
      }
   }

   static final class Plume extends SpriteBillboardParticle {
      private final float size0;
      private final float spin;

      Plume(ClientWorld w, double x, double y, double z, double size, double rise, double life, SpriteProvider sp) {
         super(w, x, y, z);
         this.size0 = (float)MathHelper.clamp(size, 0.3, 14.0);
         this.scale = this.size0;
         this.maxAge = MathHelper.clamp((int)life, 10, 400);
         this.collidesWithWorld = false;
         this.velocityY = rise;
         this.velocityX = 0.012 + (this.random.nextDouble() - 0.5) * 0.01;
         this.velocityZ = 0.006 + (this.random.nextDouble() - 0.5) * 0.01;
         float g = 0.8F + this.random.nextFloat() * 0.2F;
         this.setColor(g, g, g);
         this.spin = (this.random.nextFloat() - 0.5F) * 0.03F;
         this.angle = this.random.nextFloat() * 6.28F;
         this.setSprite(sp);
      }

      @Override
      public void tick() {
         this.prevPosX = this.x;
         this.prevPosY = this.y;
         this.prevPosZ = this.z;
         this.prevAngle = this.angle;
         if (this.age++ >= this.maxAge) {
            this.markDead();
         } else {
            this.x = this.x + this.velocityX;
            this.y = this.y + this.velocityY;
            this.z = this.z + this.velocityZ;
            this.velocityY *= 0.985;
            this.angle = this.angle + this.spin;
            float k = GodParticlesClient.frac(this.age, this.maxAge);
            this.alpha = k < 0.5F ? Math.min(1.0F, k * 8.0F) * 0.92F : 0.92F * (1.0F - (k - 0.5F) / 0.5F);
         }
      }

      @Override
      public float getSize(float td) {
         return this.size0 * (1.0F + 0.6F * GodParticlesClient.frac(this.age, this.maxAge));
      }

      @Override
      public ParticleTextureSheet getType() {
         return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
      }
   }

   static final class Shockwave extends SpriteBillboardParticle {
      private final SpriteProvider sprites;
      private final float radius;

      Shockwave(ClientWorld w, double x, double y, double z, double radius, double life, SpriteProvider sp) {
         super(w, x, y, z);
         this.sprites = sp;
         this.radius = (float)MathHelper.clamp(radius, 0.5, 60.0);
         this.maxAge = MathHelper.clamp((int)life, 4, 50);
         this.collidesWithWorld = false;
         this.setSpriteForAge(sp);
      }

      @Override
      public void tick() {
         this.prevPosX = this.x;
         this.prevPosY = this.y;
         this.prevPosZ = this.z;
         if (this.age++ >= this.maxAge) {
            this.markDead();
         } else {
            this.setSpriteForAge(this.sprites);
         }
      }

      @Override
      public void buildGeometry(VertexConsumer vc, Camera camera, float td) {
         float t = Math.min(1.0F, (this.age + td) / this.maxAge);
         float ease = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
         float s = Math.max(0.1F, this.radius * ease);
         float a = 0.92F * (float)Math.pow(1.0F - t, 1.3);
         if (!(a <= 0.01F)) {
            Vec3d cam = camera.getPos();
            float cx = (float)(MathHelper.lerp((double)td, this.prevPosX, this.x) - cam.x);
            float cy = (float)(MathHelper.lerp((double)td, this.prevPosY, this.y) - cam.y);
            float cz = (float)(MathHelper.lerp((double)td, this.prevPosZ, this.z) - cam.z);
            float u0 = this.getMinU();
            float u1 = this.getMaxU();
            float v0 = this.getMinV();
            float v1 = this.getMaxV();
            int l = this.getBrightness(td);
            float[][] c = new float[][]{{-s, -s, u0, v0}, {-s, s, u0, v1}, {s, s, u1, v1}, {s, -s, u1, v0}};

            for (int i = 0; i < 4; i++) {
               float[] p = c[i];
               vc.vertex(cx + p[0], cy, cz + p[1]).texture(p[2], p[3]).color(this.red, this.green, this.blue, a).light(l);
            }

            for (int i = 3; i >= 0; i--) {
               float[] p = c[i];
               vc.vertex(cx + p[0], cy, cz + p[1]).texture(p[2], p[3]).color(this.red, this.green, this.blue, a).light(l);
            }
         }
      }

      @Override
      public ParticleTextureSheet getType() {
         return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
      }
   }
}
