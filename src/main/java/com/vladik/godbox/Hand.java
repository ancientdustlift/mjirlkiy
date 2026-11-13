package com.vladik.godbox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class Hand {
   private static final Map<UUID, Hand.Grip> GRIPS = new HashMap<>();
   private static long now;

   private Hand() {
   }

   private static boolean grabbable(Entity e) {
      if (!e.isAlive() || e.isSpectator() || GodManager.isGodEntity(e)) {
         return false;
      } else {
         return e instanceof ServerPlayerEntity
            ? false
            : e instanceof LivingEntity || e instanceof ItemEntity || e instanceof TntEntity || e instanceof FallingBlockEntity;
      }
   }

   public static void use(ServerPlayerEntity god, BlockPos pos, int brush, boolean first) {
      ServerWorld w = god.getServerWorld();
      int b = MathHelper.clamp(brush % 100, 1, 10);
      Vec3d at = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
      Vec3d target = at.add(0.0, 4.5, 0.0);
      Hand.Grip g = GRIPS.get(god.getUuid());
      if (first && g != null) {
         release(g);
         GRIPS.remove(god.getUuid());
         g = null;
      }

      if (g == null || g.world != w) {
         double r = 1.5 + b * 0.6;
         Box box = new Box(at.x - r, at.y - 3.0, at.z - r, at.x + r, at.y + 4.0, at.z + r);
         List<Entity> list = w.getOtherEntities(god, box, Hand::grabbable);
         if (list.isEmpty()) {
            if (first) {
               god.sendMessage(Text.literal("✋ Здесь некого схватить"), true);
            }

            return;
         }

         list.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(at)));
         int max = 1 + (b - 1) * 2;
         g = new Hand.Grip();
         g.world = w;
         g.target = target;

         for (int i = 0; i < Math.min(max, list.size()); i++) {
            g.held.add(list.get(i));
         }

         GRIPS.put(god.getUuid(), g);
         Fx.particles(w, ParticleTypes.END_ROD, at.x, at.y + 1.0, at.z, 25, 1.0, 1.0, 1.0, 0.05);
         Fx.sound(w, at.x, at.y, at.z, SoundEvents.ENTITY_ITEM_PICKUP, 1.5F, 0.5F);
         god.sendMessage(Text.literal("✋ Схвачено: " + g.held.size() + " — веди мышью и отпусти, чтобы бросить"), true);
      }

      Vec3d d = target.subtract(g.target);
      if (d.lengthSquared() > 400.0) {
         d = Vec3d.ZERO;
      }

      g.vel = g.vel.multiply(0.5).add(d.multiply(0.5));
      g.target = target;
      g.last = now;
   }

   private static void release(Hand.Grip g) {
      Vec3d v = g.vel.multiply(1.6);
      double len = v.length();
      if (len > 3.5) {
         v = v.multiply(3.5 / len);
      }

      boolean gentle = v.length() < 0.35;

      for (Entity e : g.held) {
         if (e.isAlive()) {
            if (gentle) {
               e.setVelocity(Vec3d.ZERO);
               if (e instanceof LivingEntity le) {
                  le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 60, 0, false, false));
               }
            } else {
               e.setVelocity(v.add((e.getRandom().nextDouble() - 0.5) * 0.15, 0.2, (e.getRandom().nextDouble() - 0.5) * 0.15));
            }

            e.velocityModified = true;
            e.fallDistance = 0.0F;
         }
      }

      if (g.target != null && g.world != null) {
         Fx.sound(
            g.world,
            g.target.x,
            g.target.y,
            g.target.z,
            gentle ? SoundEvents.ENTITY_ITEM_PICKUP : SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
            1.5F,
            gentle ? 1.2F : 0.7F
         );
         if (!gentle) {
            Fx.particles(g.world, ParticleTypes.CLOUD, g.target.x, g.target.y, g.target.z, 15, 0.5, 0.5, 0.5, 0.1);
         }
      }
   }

   public static void tick(MinecraftServer server) {
      now = server.getTicks();
      if (!GRIPS.isEmpty()) {
         Iterator<Entry<UUID, Hand.Grip>> it = GRIPS.entrySet().iterator();

         while (it.hasNext()) {
            Hand.Grip g = it.next().getValue();
            g.held.removeIf(ex -> !ex.isAlive() || ex.getWorld() != g.world);
            if (g.held.isEmpty()) {
               it.remove();
            } else if (now - g.last > 4L) {
               release(g);
               it.remove();
            } else {
               int n = g.held.size();
               double rad = n == 1 ? 0.0 : 0.6 + n * 0.15;

               for (int i = 0; i < n; i++) {
                  Entity e = g.held.get(i);
                  double ang = i * Math.PI * 2.0 / n + now * 0.08;
                  Vec3d want = g.target.add(Math.cos(ang) * rad, Math.sin(now * 0.2 + i) * 0.25, Math.sin(ang) * rad);
                  Vec3d d = want.subtract(e.getPos());
                  if (d.lengthSquared() > 900.0) {
                     e.requestTeleport(want.x, want.y, want.z);
                     d = Vec3d.ZERO;
                  }

                  e.setVelocity(d.multiply(0.45));
                  e.velocityModified = true;
                  e.fallDistance = 0.0F;
                  if (e instanceof MobEntity m) {
                     m.getNavigation().stop();
                  }
               }

               if (now % 3L == 0L) {
                  Fx.particles(g.world, ParticleTypes.END_ROD, g.target.x, g.target.y + 0.5, g.target.z, 2, rad + 0.3, 0.4, rad + 0.3, 0.01);
               }
            }
         }
      }
   }

   public static void clear() {
      GRIPS.clear();
   }

   private static final class Grip {
      final List<Entity> held = new ArrayList<>();
      ServerWorld world;
      Vec3d target;
      Vec3d vel = Vec3d.ZERO;
      long last;
   }
}
