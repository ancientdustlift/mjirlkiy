package com.kalabushkin.client;

import com.kalabushkin.ModEffects;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.math.MathHelper;

/** What the trip effect looks like: a slowly colour-cycling wash over the screen and a breathing field of view. */
public final class TripVisuals {
	private static final float HUE_CYCLE_TICKS = 80.0f;
	private static final float MAX_TINT_ALPHA = 0.28f;
	private static final float FOV_SWING = 0.14f;
	private static final float FOV_PERIOD_TICKS = 50.0f;
	// The visuals ease out over the last seconds instead of snapping off.
	private static final float FADE_OUT_TICKS = 60.0f;

	private TripVisuals() {
	}

	public static void init() {
		HudRenderCallback.EVENT.register((context, tickCounter) -> {
			float strength = strength();
			if (strength <= 0.0f) {
				return;
			}
			float time = time(tickCounter.getTickDelta(true));
			float pulse = 0.75f + 0.25f * MathHelper.sin(time / 11.0f);
			int alpha = (int) (255.0f * MAX_TINT_ALPHA * strength * pulse);
			int rgb = MathHelper.hsvToRgb(time / HUE_CYCLE_TICKS % 1.0f, 0.9f, 1.0f) & 0xFFFFFF;
			context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), alpha << 24 | rgb);
		});
	}

	/** Multiplier applied to the camera's field of view while tripping. */
	public static double fovScale(float tickDelta) {
		float strength = strength();
		if (strength <= 0.0f) {
			return 1.0;
		}
		return 1.0 + FOV_SWING * strength * MathHelper.sin(time(tickDelta) * MathHelper.TAU / FOV_PERIOD_TICKS);
	}

	private static float time(float tickDelta) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		return player == null ? 0.0f : player.age + tickDelta;
	}

	private static float strength() {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		StatusEffectInstance trip = player == null ? null : player.getStatusEffect(ModEffects.TRIP);
		if (trip == null) {
			return 0.0f;
		}
		return trip.isInfinite() ? 1.0f : MathHelper.clamp(trip.getDuration() / FADE_OUT_TICKS, 0.0f, 1.0f);
	}
}
