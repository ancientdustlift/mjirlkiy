package com.kalabushkin.client;

import com.kalabushkin.Kalabushkin;
import com.kalabushkin.entity.AlexeyEntity;
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

/** Player-shaped, with a bulky puffer vest over the torso and a hoodie's hood bunched behind the neck. */
public class AlexeyModel extends SinglePartEntityModel<AlexeyEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Kalabushkin.id("alexey_srpava"), "main");
	/** The model is 2 blocks tall; the renderer shrinks it to his 1.7. */
	public static final float SCALE = 0.85f;

	private final ModelPart root;
	private final ModelPart head;

	public AlexeyModel(ModelPart root) {
		this.root = root;
		this.head = root.getChild("waist").getChild("head");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		ModelPartData waist = root.addChild("waist", ModelPartBuilder.create(), ModelTransform.pivot(0.0f, 12.0f, 0.0f));
		waist.addChild("body",
				ModelPartBuilder.create()
						.uv(16, 16).cuboid(-4.0f, -12.0f, -2.0f, 8.0f, 12.0f, 4.0f)
						.uv(24, 32).cuboid(-4.0f, -12.0f, -2.0f, 8.0f, 12.0f, 4.0f, new Dilation(0.4f))
						.uv(0, 32).cuboid(-4.0f, -12.5f, 1.8f, 8.0f, 3.0f, 2.0f),
				ModelTransform.NONE);
		waist.addChild("head",
				ModelPartBuilder.create()
						.uv(0, 0).cuboid(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f)
						.uv(32, 0).cuboid(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, new Dilation(0.3f)),
				ModelTransform.pivot(0.0f, -12.0f, 0.0f));
		waist.addChild("right_arm",
				ModelPartBuilder.create().uv(40, 16).cuboid(-3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f),
				ModelTransform.pivot(-5.0f, -10.0f, 0.0f));
		waist.addChild("left_arm",
				ModelPartBuilder.create().uv(32, 48).cuboid(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f),
				ModelTransform.pivot(5.0f, -10.0f, 0.0f));
		root.addChild("right_leg",
				ModelPartBuilder.create().uv(0, 16).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f),
				ModelTransform.pivot(-1.9f, 12.0f, 0.0f));
		root.addChild("left_leg",
				ModelPartBuilder.create().uv(16, 48).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f),
				ModelTransform.pivot(1.9f, 12.0f, 0.0f));
		return TexturedModelData.of(data, 64, 64);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(AlexeyEntity entity, float limbAngle, float limbDistance, float animationProgress,
			float headYaw, float headPitch) {
		getPart().traverse().forEach(ModelPart::resetTransform);
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
		animateMovement(CoupleAnimations.WALK, limbAngle, limbDistance, 2.0f, 2.5f);
		updateAnimation(entity.idleAnimationState, CoupleAnimations.ALEXEY_IDLE, animationProgress, 1.0f);
		updateAnimation(entity.complimentAnimationState, CoupleAnimations.COMPLIMENT, animationProgress, 1.0f);
	}
}
