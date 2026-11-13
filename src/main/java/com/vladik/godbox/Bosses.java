package com.vladik.godbox;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World.ExplosionSourceType;
import org.joml.Vector3f;

public final class Bosses {
   private static final List<Bosses.B> LIST = new ArrayList<>();
   private static final DustParticleEffect RED = new DustParticleEffect(new Vector3f(1.0F, 0.1F, 0.1F), 2.0F);
   private static final DustParticleEffect CYAN = new DustParticleEffect(new Vector3f(0.2F, 0.9F, 1.0F), 2.0F);

   private Bosses() {
   }

   private static void attr(MobEntity m, RegistryEntry<EntityAttribute> a, double v) {
      EntityAttributeInstance i = m.getAttributeInstance(a);
      if (i != null) {
         i.setBaseValue(v);
      }
   }

   public static void spawn(ServerPlayerEntity god, String kind, ServerWorld w, BlockPos pos) {
      BlockPos at = Fx.surface(w, pos.getX(), pos.getZ());
      MobEntity m;
      String name;
      Formatting color;
      switch (kind) {
         case "crab":
            m = EntityType.RAVAGER.spawn(w, at, SpawnReason.TRIGGERED);
            name = "Крабзилла";
            color = Formatting.RED;
            break;
         case "robot":
            m = EntityType.IRON_GOLEM.spawn(w, at, SpawnReason.TRIGGERED);
            name = "Мехагигант";
            color = Formatting.AQUA;
            break;
         case "dragon":
            m = EntityType.PHANTOM.spawn(w, at.up(24), SpawnReason.TRIGGERED);
            name = "Дракон";
            color = Formatting.GOLD;
            break;
         default:
            return;
      }

      if (m != null) {
         m.addCommandTag("godbox_boss");
         m.targetSelector.clear(g -> true);
         m.setPersistent();
         m.setCustomName(Text.literal(name).formatted(color, Formatting.BOLD));
         m.setCustomNameVisible(true);
         switch (kind) {
            case "crab":
               attr(m, EntityAttributes.GENERIC_SCALE, 3.0);
               attr(m, EntityAttributes.GENERIC_MAX_HEALTH, 500.0);
               attr(m, EntityAttributes.GENERIC_ATTACK_DAMAGE, 25.0);
               attr(m, EntityAttributes.GENERIC_STEP_HEIGHT, 3.0);
               attr(m, EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0);
               attr(m, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
               break;
            case "robot":
               attr(m, EntityAttributes.GENERIC_SCALE, 4.0);
               attr(m, EntityAttributes.GENERIC_MAX_HEALTH, 700.0);
               attr(m, EntityAttributes.GENERIC_ATTACK_DAMAGE, 30.0);
               attr(m, EntityAttributes.GENERIC_STEP_HEIGHT, 3.0);
               attr(m, EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0);
               attr(m, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
               break;
            default:
               if (m instanceof PhantomEntity ph) {
                  ph.setPhantomSize(10);
               }

               m.goalSelector.clear(g -> true);
               m.setAiDisabled(true);
               m.setNoGravity(true);
               attr(m, EntityAttributes.GENERIC_SCALE, 2.5);
               attr(m, EntityAttributes.GENERIC_MAX_HEALTH, 400.0);
               m.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, -1, 0, false, false));
         }

         m.setHealth(m.getMaxHealth());
         BossModels.attach(m, kind, w);
         LIST.add(new Bosses.B(m, kind, at));
         Fx.sound(w, at.getX(), at.getY(), at.getZ(), SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 6.0F, kind.equals("crab") ? 0.6F : 0.9F);
         w.getServer().getPlayerManager().broadcast(Text.literal("⚠ Появился босс: " + name + "!").formatted(color, Formatting.BOLD), false);
      }
   }

   public static void command(ServerPlayerEntity god, ServerWorld w, BlockPos pos) {
      int n = 0;
      BlockPos target = Fx.surface(w, pos.getX(), pos.getZ());

      for (Bosses.B b : LIST) {
         if (b.m.getWorld() == w) {
            b.order = target;
            b.orderAge = 600;
            b.m.setTarget(null);
            n++;
         }
      }

      god.sendMessage(Text.literal(n == 0 ? "Боссов нет" : "Боссы идут к цели: " + n).formatted(Formatting.GOLD), true);
   }

   public static void remove(ServerPlayerEntity god) {
      int n = 0;

      for (Bosses.B b : LIST) {
         if (!b.m.isRemoved()) {
            ServerWorld w = (ServerWorld)b.m.getWorld();
            Fx.particles(w, ParticleTypes.POOF, b.m.getX(), b.m.getY() + 1.0, b.m.getZ(), 60, 2.0, 2.0, 2.0, 0.05);
            b.m.discard();
            n++;
         }
      }

      LIST.clear();
      god.sendMessage(Text.literal("Убрано боссов: " + n).formatted(Formatting.GRAY), true);
   }

   public static void clear() {
      LIST.clear();
   }

   public static void tick() {
      Iterator<Bosses.B> it = LIST.iterator();

      while (it.hasNext()) {
         Bosses.B b = it.next();
         MobEntity m = b.m;
         if (!m.isRemoved() && m.isAlive()) {
            ServerWorld w = (ServerWorld)m.getWorld();
            b.t++;
            if (b.orderAge > 0) {
               b.orderAge--;
            }

            boolean dragon = b.kind.equals("dragon");
            if (dragon) {
               fly(b, w);
            } else if (b.t % 10 == 0) {
               think(b, w);
            }

            if (!dragon && b.t % 5 == 0) {
               stomp(b, w);
            }

            if (b.t % (dragon ? 30 : 60) == 0 && b.orderAge <= 0) {
               attack(b, w);
            }
         } else {
            it.remove();
         }
      }
   }

   private static LivingEntity target(Bosses.B b, ServerWorld w, double r) {
      LivingEntity best = null;
      double bd = Double.MAX_VALUE;

      for (LivingEntity e : Fx.living(w, b.m.getPos(), r)) {
         if (e != b.m
            && e.isAlive()
            && !e.getCommandTags().contains("godbox_boss")
            && !GodManager.isGodEntity(e)
            && !e.isSpectator()
            && !(e instanceof PlayerEntity p && p.isCreative())) {
            double d = e.squaredDistanceTo(b.m);
            if (d < bd) {
               bd = d;
               best = e;
            }
         }
      }

      return best;
   }

   private static void think(Bosses.B b, ServerWorld w) {
      MobEntity m = b.m;
      if (b.orderAge > 0 && b.order != null) {
         m.setTarget(null);
         if (m.getBlockPos().getSquaredDistance(b.order) < 16.0) {
            b.orderAge = 0;
         } else {
            double x = b.order.getX() + 0.5;
            double y = b.order.getY();
            double z = b.order.getZ() + 0.5;
            if (!m.getNavigation().startMovingTo(x, y, z, 1.2)) {
               m.getMoveControl().moveTo(x, y, z, 1.2);
            }
         }
      } else {
         LivingEntity t = m.getTarget();
         if (t == null || !t.isAlive() || t.squaredDistanceTo(m) > 2304.0 || GodManager.isGodEntity(t)) {
            t = target(b, w, 40.0);
            m.setTarget(t);
         }

         if (t != null && m.getNavigation().isIdle()) {
            m.getMoveControl().moveTo(t.getX(), t.getY(), t.getZ(), 1.2);
         }
      }
   }

   private static void stomp(Bosses.B b, ServerWorld w) {
      MobEntity m = b.m;
      Box bb = m.getBoundingBox().expand(0.6, 0.0, 0.6);
      int n = 0;

      for (BlockPos p : BlockPos.iterate(
         MathHelper.floor(bb.minX),
         MathHelper.floor(bb.minY + 0.05),
         MathHelper.floor(bb.minZ),
         MathHelper.floor(bb.maxX),
         MathHelper.floor(bb.maxY),
         MathHelper.floor(bb.maxZ)
      )) {
         BlockState s = w.getBlockState(p);
         if (!s.isAir() && s.getFluidState().isEmpty()) {
            float h = s.getHardness(w, p);
            if (!(h < 0.0F) && !(h > 3.0F)) {
               w.breakBlock(p, false, m);
               if (++n >= 60) {
                  break;
               }
            }
         }
      }
   }

   private static void attack(Bosses.B b, ServerWorld w) {
      MobEntity m = b.m;
      LivingEntity t = m.getTarget();
      if (t == null || !t.isAlive()) {
         t = target(b, w, b.kind.equals("dragon") ? 60.0 : 40.0);
      }

      if (t != null) {
         BossModels.strike(m);
         Vec3d eye = m.getEyePos();
         Vec3d to = t.getPos().add(0.0, t.getHeight() / 2.0F, 0.0);
         if (b.kind.equals("dragon")) {
            Vec3d dir = to.subtract(eye).normalize();
            FireballEntity f = new FireballEntity(w, m, dir, 2);
            f.setPosition(eye.add(dir.multiply(4.0)));
            w.spawnEntity(f);
            Fx.sound(w, eye.x, eye.y, eye.z, SoundEvents.ENTITY_ENDER_DRAGON_SHOOT, 5.0F, 0.8F);
         } else {
            boolean crab = b.kind.equals("crab");
            beam(w, eye, to, crab ? RED : CYAN);
            Fx.sound(w, eye.x, eye.y, eye.z, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 5.0F, crab ? 0.7F : 1.2F);
            w.createExplosion(m, to.x, to.y, to.z, crab ? 3.0F : 4.0F, crab, ExplosionSourceType.MOB);
         }
      }
   }

   private static void fly(Bosses.B b, ServerWorld w) {
      MobEntity m = b.m;
      if (b.t % 10 == 0 && b.orderAge <= 0) {
         LivingEntity t = m.getTarget();
         if (t == null || !t.isAlive() || t.squaredDistanceTo(m) > 6400.0 || GodManager.isGodEntity(t)) {
            m.setTarget(target(b, w, 60.0));
         }
      }

      double ang = b.t * 0.03;
      Vec3d goal;
      if (b.orderAge > 0 && b.order != null) {
         goal = Vec3d.ofCenter(b.order);
         if (goal.squaredDistanceTo(m.getX(), goal.y, m.getZ()) < 9.0) {
            b.orderAge = 0;
         }
      } else if (m.getTarget() != null) {
         LivingEntity t = m.getTarget();
         goal = new Vec3d(t.getX() + Math.cos(ang) * 18.0, t.getY(), t.getZ() + Math.sin(ang) * 18.0);
      } else {
         goal = new Vec3d(b.home.getX() + Math.cos(ang) * 30.0, b.home.getY(), b.home.getZ() + Math.sin(ang) * 30.0);
      }

      int gx = MathHelper.floor(goal.x);
      int gz = MathHelper.floor(goal.z);
      if (w.isChunkLoaded(gx >> 4, gz >> 4)) {
         double gy = Fx.surface(w, gx, gz).getY() + 24;
         goal = new Vec3d(goal.x, Math.max(gy, Math.min(goal.y + 24.0, gy + 20.0)), goal.z);
         Vec3d d = goal.subtract(m.getPos());
         double len = d.length();
         if (!(len < 0.01)) {
            Vec3d step = len < 0.9 ? d : d.multiply(0.9 / len);
            float yaw = (float)Math.toDegrees(Math.atan2(-step.x, step.z));
            m.setVelocity(Vec3d.ZERO);
            m.setPosition(m.getX() + step.x, m.getY() + step.y, m.getZ() + step.z);
            m.setYaw(yaw);
            m.setHeadYaw(yaw);
            m.setBodyYaw(yaw);
            if (b.t % 40 == 0) {
               Fx.sound(w, m.getX(), m.getY(), m.getZ(), SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 4.0F, 0.7F);
            }
         }
      }
   }

   public static void beam(ServerWorld w, Vec3d a, Vec3d b, ParticleEffect p) {
      Vec3d d = b.subtract(a);
      double len = d.length();
      int n = (int)Math.max(2.0, Math.min(200.0, len * 2.0));

      for (int i = 0; i <= n; i++) {
         Vec3d q = a.add(d.multiply((double)i / n));
         Fx.particles(w, p, q.x, q.y, q.z, 1, 0.0, 0.0, 0.0, 0.0);
      }
   }

   private static final class B {
      final MobEntity m;
      final String kind;
      final BlockPos home;
      BlockPos order;
      int orderAge;
      int t;

      B(MobEntity m, String kind, BlockPos home) {
         this.m = m;
         this.kind = kind;
         this.home = home;
      }
   }
}
