package com.vladik.godbox;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public final class Guns {
   private static int t;

   private Guns() {
   }

   public static void init() {
      ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
         if (entity instanceof PersistentProjectileEntity p && p.getOwner() instanceof MobEntity m) {
            if (isMusket(m.getMainHandStack())) {
               Vec3d dir = p.getVelocity();
               p.setInvisible(true);
               p.setDamage(0.0);
               Tasks.delay(1, () -> {
                  p.discard();
                  if (m.isAlive() && m.getWorld() == world) {
                     fire(world, m, m.getTarget(), dir);
                  }
               });
            }
         }
      });
   }

   private static int cmd(String part) {
      ModelParts.Creature def = ModelParts.get("guns");
      if (def != null && Models.custom()) {
         for (ModelParts.Part p : def.parts()) {
            if (p.name().equals(part)) {
               return p.cmd();
            }
         }

         return 0;
      } else {
         return 0;
      }
   }

   private static ItemStack tagged(ItemStack stack, String part) {
      int id = cmd(part);
      if (id == 0) {
         return null;
      } else {
         stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(id));
         return stack;
      }
   }

   public static boolean isMusket(ItemStack stack) {
      if (!stack.isOf(Items.CROSSBOW)) {
         return false;
      } else {
         CustomModelDataComponent c = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
         return c != null && c.value() != 0 && c.value() == cmd("musket");
      }
   }

   static void fire(ServerWorld w, MobEntity m, LivingEntity aim, Vec3d fallback) {
      Vec3d eye = new Vec3d(m.getX(), m.getEyeY() - 0.15, m.getZ());
      Vec3d dir;
      if (aim != null && aim.isAlive() && aim.getWorld() == w) {
         double spread = Config.d("guns.musket.spread", 0.04);
         dir = new Vec3d(aim.getX(), aim.getBodyY(0.6), aim.getZ())
            .subtract(eye)
            .normalize()
            .add(w.getRandom().nextGaussian() * spread, w.getRandom().nextGaussian() * spread * 0.5, w.getRandom().nextGaussian() * spread);
      } else {
         dir = fallback;
      }

      if (!(dir.lengthSquared() < 1.0E-6)) {
         dir = dir.normalize();
         Vec3d from = eye.add(dir.multiply(1.1));
         Vec3d to = from.add(dir.multiply(Config.d("guns.musket.range", 40.0)));
         BlockHitResult bh = w.raycast(new RaycastContext(from, to, ShapeType.COLLIDER, FluidHandling.NONE, m));
         if (bh.getType() != Type.MISS) {
            to = bh.getPos();
         }

         Kingdoms.K own = Kingdoms.of(m);
         EntityHitResult eh = ProjectileUtil.getEntityCollision(
            w,
            m,
            from,
            to,
            new Box(from, to).expand(1.0),
            e -> e instanceof LivingEntity le && le.isAlive() && !le.isSpectator() && e != m && (own == null || Kingdoms.of(e) != own)
         );
         if (eh != null) {
            to = eh.getPos();
         }

         Fx.sound(w, from.x, from.y, from.z, SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 0.9F, 1.5F);
         Fx.sound(w, from.x, from.y, from.z, SoundEvents.BLOCK_DISPENSER_LAUNCH, 0.8F, 1.7F);
         Fx.sound(w, from.x, from.y, from.z, SoundEvents.ENTITY_ARROW_SHOOT, 0.5F, 1.3F);
         Fx.particles(w, ParticleTypes.FLAME, from.x, from.y, from.z, 5, 0.06, 0.06, 0.06, 0.03);
         Fx.particles(w, ParticleTypes.CAMPFIRE_COSY_SMOKE, from.x, from.y, from.z, 3, 0.1, 0.1, 0.1, 0.01);
         Fx.particles(w, ParticleTypes.LARGE_SMOKE, from.x, from.y, from.z, 4, 0.15, 0.1, 0.15, 0.02);
         double len = to.distanceTo(from);

         for (double d = 1.5; d < len; d += 2.5) {
            Fx.particles(w, ParticleTypes.CRIT, from.x + dir.x * d, from.y + dir.y * d, from.z + dir.z * d, 1, 0.0, 0.0, 0.0, 0.0);
         }

         if (eh != null && eh.getEntity() instanceof LivingEntity hit) {
            hit.damage(w.getDamageSources().mobProjectile(m, m), (float)Config.d("guns.musket.damage", 8.0));
            Fx.particles(w, ParticleTypes.CRIT, to.x, to.y, to.z, 6, 0.2, 0.3, 0.2, 0.1);
         } else if (bh.getType() != Type.MISS) {
            Fx.particles(w, ParticleTypes.SMOKE, to.x, to.y, to.z, 4, 0.1, 0.1, 0.1, 0.01);
         }
      }
   }

   private static void arm() {
      if (cmd("musket") != 0) {
         for (Kingdoms.K k : Kingdoms.K.values()) {
            if (k.alive && k.world instanceof ServerWorld && Tech.has(k, Tech.T.GUNPOWDER)) {
               for (MobEntity m : k.members) {
                  if (m instanceof PillagerEntity && m.isAlive()) {
                     ItemStack held = m.getMainHandStack();
                     if (held.isOf(Items.CROSSBOW) && !isMusket(held)) {
                        ItemStack musket = tagged(new ItemStack(Items.CROSSBOW), "musket");
                        if (musket == null) {
                           return;
                        }

                        m.equipStack(EquipmentSlot.MAINHAND, musket);
                        m.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
                     }
                  }
               }
            }
         }
      }
   }

   static ItemStack ball() {
      ItemStack s = Models.part("guns", "ball");
      return s != null ? s : new ItemStack(Items.FIRE_CHARGE);
   }

   public static void tick(MinecraftServer s) {
      if (++t % 100 == 0) {
         arm();
      }
   }

   public static void clear() {
      t = 0;
   }
}
