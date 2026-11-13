package com.vladik.godbox.mixin;

import com.vladik.godbox.Blast;
import com.vladik.godbox.GodBox;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World.ExplosionSourceType;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ServerWorld.class})
public abstract class ServerWorldMixin {
   @Inject(
      method = {"createExplosion(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/damage/DamageSource;Lnet/minecraft/world/explosion/ExplosionBehavior;DDDFZLnet/minecraft/world/World$ExplosionSourceType;Lnet/minecraft/particle/ParticleEffect;Lnet/minecraft/particle/ParticleEffect;Lnet/minecraft/registry/entry/RegistryEntry;)Lnet/minecraft/world/explosion/Explosion;"},
      at = {@At("RETURN")}
   )
   private void godbox$blast(
      Entity entity,
      DamageSource source,
      ExplosionBehavior behavior,
      double x,
      double y,
      double z,
      float power,
      boolean createFire,
      ExplosionSourceType type,
      ParticleEffect small,
      ParticleEffect large,
      RegistryEntry<SoundEvent> sound,
      CallbackInfoReturnable<Explosion> cir
   ) {
      try {
         Blast.fx((ServerWorld)(Object)this, x, y, z, power);
      } catch (RuntimeException var18) {
         GodBox.LOG.warn("GodBox blast fx failed", (Throwable)var18);
      }
   }
}
