package com.kalabushkin.mixin;

import com.kalabushkin.spirit.FistBumps;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
abstract class MobEntityMixin {
	// Only when the mob has no use for the click, so trading, riding, feeding etc. keep working.
	@Inject(method = "interact", at = @At("RETURN"), cancellable = true)
	private void kalabushkin$fistBump(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
		if (!cir.getReturnValue().isAccepted() && FistBumps.tryBump((MobEntity) (Object) this, player, hand)) {
			cir.setReturnValue(ActionResult.SUCCESS);
		}
	}
}
