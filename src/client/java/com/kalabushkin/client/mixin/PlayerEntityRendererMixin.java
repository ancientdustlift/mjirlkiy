package com.kalabushkin.client.mixin;

import com.kalabushkin.item.ShatuniItem;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityRenderer.class)
abstract class PlayerEntityRendererMixin {
	// The crossbow pose brings both arms forward, so the Shatuni is gripped with two hands.
	@Inject(method = "getArmPose", at = @At("HEAD"), cancellable = true)
	private static void kalabushkin$holdShatuniWithBothHands(AbstractClientPlayerEntity player, Hand hand,
			CallbackInfoReturnable<BipedEntityModel.ArmPose> cir) {
		if (hand == Hand.MAIN_HAND && player.getMainHandStack().getItem() instanceof ShatuniItem
				&& player.getOffHandStack().isEmpty()) {
			cir.setReturnValue(BipedEntityModel.ArmPose.CROSSBOW_HOLD);
		}
	}
}
