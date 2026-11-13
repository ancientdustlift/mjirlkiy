package com.kalabushkin.client;

import com.kalabushkin.Kalabushkin;
import com.kalabushkin.entity.KalabushkinEntity;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class KalabushkinModel extends SinglePartEntityModel<KalabushkinEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Kalabushkin.id("kalabushkin"), "main");

	private final ModelPart root;
	private final ModelPart head;

	public KalabushkinModel(ModelPart root) {
		this.root = root;
		this.head = root.getChild("waist").getChild("head");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		ModelPartData waist = root.addChild("waist", ModelPartBuilder.create(), ModelTransform.pivot(0.0f, 12.0f, 0.0f));
		addUpperBody(waist);
		root.addChild("right_leg",
				ModelPartBuilder.create().uv(0, 16).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f),
				ModelTransform.pivot(-1.9f, 12.0f, 0.0f));
		root.addChild("left_leg",
				ModelPartBuilder.create().uv(16, 48).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f),
				ModelTransform.pivot(1.9f, 12.0f, 0.0f));
		return TexturedModelData.of(data, 64, 64);
	}

	/** Torso with jacket collar, head with hair layer and nose, arms ending in fists. Shared with the spirit. */
	static void addUpperBody(ModelPartData waist) {
		waist.addChild("body",
				ModelPartBuilder.create()
						.uv(16, 16).cuboid(-4.0f, -12.0f, -2.0f, 8.0f, 12.0f, 4.0f)
						.uv(0, 32).cuboid(-4.5f, -12.5f, -2.5f, 9.0f, 2.0f, 5.0f),
				ModelTransform.NONE);
		waist.addChild("head",
				ModelPartBuilder.create()
						.uv(0, 0).cuboid(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f)
						.uv(32, 0).cuboid(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, new Dilation(0.3f))
						.uv(24, 0).cuboid(-1.0f, -4.0f, -5.0f, 2.0f, 2.0f, 1.0f),
				ModelTransform.pivot(0.0f, -12.0f, 0.0f));
		waist.addChild("right_arm",
				ModelPartBuilder.create()
						.uv(40, 16).cuboid(-3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f)
						.uv(0, 40).cuboid(-3.0f, 7.0f, -2.0f, 4.0f, 3.0f, 4.0f, new Dilation(0.3f)),
				ModelTransform.pivot(-5.0f, -10.0f, 0.0f));
		waist.addChild("left_arm",
				ModelPartBuilder.create()
						.uv(32, 48).cuboid(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f)
						.uv(16, 40).cuboid(-1.0f, 7.0f, -2.0f, 4.0f, 3.0f, 4.0f, new Dilation(0.3f)),
				ModelTransform.pivot(5.0f, -10.0f, 0.0f));
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(KalabushkinEntity entity, float limbAngle, float limbDistance, float animationProgress,
			float headYaw, float headPitch) {
		getPart().traverse().forEach(ModelPart::resetTransform);
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
		animateMovement(KalabushkinAnimations.WALK, limbAngle, limbDistance, 2.0f, 2.5f);
		updateAnimation(entity.idleAnimationState, KalabushkinAnimations.IDLE, animationProgress, 1.0f);
		updateAnimation(entity.fistBumpAnimationState, KalabushkinAnimations.FIST_BUMP, animationProgress, 1.0f);
	}
}
