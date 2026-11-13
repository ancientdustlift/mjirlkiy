package com.kalabushkin;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;

public final class ModEffects {
	/** Does nothing on the server; the client tints the screen in shifting colours and pulses the field of view. */
	public static final RegistryEntry<StatusEffect> TRIP = Registry.registerReference(Registries.STATUS_EFFECT,
			Kalabushkin.id("trip"), new TripEffect());

	private ModEffects() {
	}

	public static void init() {
	}

	private static final class TripEffect extends StatusEffect {
		TripEffect() {
			super(StatusEffectCategory.HARMFUL, 0xD048E0);
		}
	}
}
