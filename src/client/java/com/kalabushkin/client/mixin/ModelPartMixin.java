package com.kalabushkin.client.mixin;

import com.kalabushkin.client.FistBumpAnimator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remembers each part's name, so a limb can be found in any model. */
@Mixin(ModelPart.class)
abstract class ModelPartMixin implements FistBumpAnimator.NamedPart {
	@Unique
	private String kalabushkin$name = "";

	@Inject(method = "<init>", at = @At("TAIL"))
	private void kalabushkin$nameChildren(List<ModelPart.Cuboid> cuboids, Map<String, ModelPart> children, CallbackInfo ci) {
		children.forEach((name, child) -> ((ModelPartMixin) (Object) child).kalabushkin$name = name);
	}

	@Override
	public String kalabushkin$getName() {
		return kalabushkin$name;
	}
}
