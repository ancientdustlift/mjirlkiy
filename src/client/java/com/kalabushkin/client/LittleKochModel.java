package com.kalabushkin.client;

import com.kalabushkin.Kalabushkin;
import com.kalabushkin.entity.LittleKochEntity;
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

/** 22 px tall (about 1.4 blocks): short legs, a wide torso with a belly, a full-size head. */
public class LittleKochModel extends SinglePartEntityModel<LittleKochEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Kalabushkin.id("little_koch"), "main");

	private static final float LEG = 6.0f;
	private static final float TORSO = 8.0f;

	private final ModelPart root;
	private final ModelPart head;

	public LittleKochModel(ModelPart root) {
		this.root = root;
		this.head = root.getChild("waist").getChild("head");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		float hipY = 24.0f - LEG;
		ModelPartData waist = root.addChild("waist", ModelPartBuilder.create(), ModelTransform.pivot(0.0f, hipY, 0.0f));
		waist.addChild("body",
				ModelPartBuilder.create().uv(0, 16).cuboid(-6.0f, -TORSO, -3.0f, 12.0f, TORSO, 6.0f),
				ModelTransform.NONE);
		// Pivot on the front of the torso so that scaling pushes the belly outwards only.
		waist.addChild("belly",
				ModelPartBuilder.create().uv(36, 16).cuboid(-5.0f, -2.5f, -2.0f, 10.0f, 5.0f, 2.0f),
				ModelTransform.pivot(0.0f, -3.0f, -3.0f));
		waist.addChild("head",
				ModelPartBuilder.create()
						.uv(0, 0).cuboid(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f)
						.uv(32, 0).cuboid(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, new Dilation(0.25f)),
				ModelTransform.pivot(0.0f, -TORSO, 0.0f));
		waist.addChild("right_arm",
				ModelPartBuilder.create().uv(0, 32).cuboid(-3.0f, -1.0f, -2.0f, 4.0f, 9.0f, 4.0f),
				ModelTransform.pivot(-7.0f, -TORSO + 1.0f, 0.0f));
		waist.addChild("left_arm",
				ModelPartBuilder.create().uv(16, 32).cuboid(-1.0f, -1.0f, -2.0f, 4.0f, 9.0f, 4.0f),
				ModelTransform.pivot(7.0f, -TORSO + 1.0f, 0.0f));
		root.addChild("right_leg",
				ModelPartBuilder.create().uv(32, 32).cuboid(-2.5f, 0.0f, -2.5f, 5.0f, LEG, 5.0f),
				ModelTransform.pivot(-3.0f, hipY, 0.0f));
		root.addChild("left_leg",
				ModelPartBuilder.create().uv(0, 48).cuboid(-2.5f, 0.0f, -2.5f, 5.0f, LEG, 5.0f),
				ModelTransform.pivot(3.0f, hipY, 0.0f));
		return TexturedModelData.of(data, 64, 64);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(LittleKochEntity entity, float limbAngle, float limbDistance, float animationProgress,
			float headYaw, float headPitch) {
		getPart().traverse().forEach(ModelPart::resetTransform);
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
		animateMovement(LittleKochAnimations.WADDLE, limbAngle, limbDistance, 2.0f, 2.5f);
		updateAnimation(entity.idleAnimationState, LittleKochAnimations.IDLE, animationProgress, 1.0f);
	}
}
