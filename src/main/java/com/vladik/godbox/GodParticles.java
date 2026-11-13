package com.vladik.godbox;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class GodParticles {
   public static SimpleParticleType FIREBALL;
   public static SimpleParticleType SHOCKWAVE;
   public static SimpleParticleType PLUME;
   public static SimpleParticleType EMBER;
   public static SimpleParticleType DEBRIS;

   private GodParticles() {
   }

   public static void init() {
      FIREBALL = reg("fireball");
      SHOCKWAVE = reg("shockwave");
      PLUME = reg("plume");
      EMBER = reg("ember");
      DEBRIS = reg("debris");
   }

   private static SimpleParticleType reg(String id) {
      return Registry.register(Registries.PARTICLE_TYPE, Identifier.of("godbox", id), FabricParticleTypes.simple());
   }
}
