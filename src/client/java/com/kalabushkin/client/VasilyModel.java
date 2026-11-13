package com.kalabushkin.client;

import com.kalabushkin.Kalabushkin;
import com.kalabushkin.entity.VasilyEntity;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.ModelWithArms;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;

/** Player-shaped, but posed like a zombie: arms held straight out, head lolling to one side. */
public class VasilyModel extends SinglePartEntityModel<VasilyEntity> implements ModelWithArms {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Kalabushkin.id("vasily"), "main");

	private static final float ARMS_OUT = -80.0f * MathHelper.RADIANS_PER_DEGREE;
	private static final float HEAD_LOLL = 10.0f * MathHelper.RADIANS_PER_DEGREE;

	private final ModelPart root;
	private final ModelPart waist;
	private final ModelPart head;
	private final ModelPart rightArm;
	private final ModelPart leftArm;

	public VasilyModel(ModelPart root) {
		this.root = root;
		this.waist = root.getChild("waist");
		this.head = waist.getChild("head");
		this.rightArm = waist.getChild("right_arm");
		this.leftArm = waist.getChild("left_arm");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		ModelPartData waist = root.addChild("waist", ModelPartBuilder.create(), ModelTransform.pivot(0.0f, 12.0f, 0.0f));
		waist.addChild("body",
				ModelPartBuilder.create().uv(16, 16).cuboid(-4.0f, -12.0f, -2.0f, 8.0f, 12.0f, 4.0f),
				ModelTransform.NONE);
		waist.addChild("head",
				ModelPartBuilder.create()
						.uv(0, 0).cuboid(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f)
						.uv(32, 0).cuboid(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, new Dilation(0.35f)),
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
	public void setAngles(VasilyEntity entity, float limbAngle, float limbDistance, float animationProgress,
			float headYaw, float headPitch) {
		basePose(animationProgress, headYaw, headPitch);
		animateMovement(VasilyAnimations.SHAMBLE, limbAngle, limbDistance, 2.0f, 2.5f);
		updateAnimation(entity.idleAnimationState, VasilyAnimations.IDLE, animationProgress, 1.0f);
		updateAnimation(entity.handshakeAnimationState, VasilyAnimations.HANDSHAKE, animationProgress, 1.0f);
		for (int i = 0; i < entity.giveAnimationStates.length; i++) {
			updateAnimation(entity.giveAnimationStates[i], VasilyAnimations.GIVE[i], animationProgress, 1.0f);
		}
	}

	/** The zombie stance every keyframe animation is an offset from. */
	public void basePose(float animationProgress, float headYaw, float headPitch) {
		getPart().traverse().forEach(ModelPart::resetTransform);
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
		head.roll = HEAD_LOLL;
		float sway = MathHelper.sin(animationProgress * 0.09f) * 0.05f;
		rightArm.pitch = ARMS_OUT + sway;
		leftArm.pitch = ARMS_OUT - sway;
	}

	/** Lets the held-item layer follow the hand: the item he is offering is drawn in his right fist. */
	@Override
	public void setArmAngle(Arm arm, MatrixStack matrices) {
		root.rotate(matrices);
		waist.rotate(matrices);
		(arm == Arm.RIGHT ? rightArm : leftArm).rotate(matrices);
	}
}
