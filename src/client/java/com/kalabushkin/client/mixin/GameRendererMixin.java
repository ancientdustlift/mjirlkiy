package com.kalabushkin.client.mixin;

import com.kalabushkin.client.TripVisuals;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void kalabushkin$tripFov(Camera camera, float tickDelta, boolean changingFov,
			CallbackInfoReturnable<Double> cir) {
		double scale = TripVisuals.fovScale(tickDelta);
		if (scale != 1.0) {
			cir.setReturnValue(cir.getReturnValue() * scale);
		}
	}
}
