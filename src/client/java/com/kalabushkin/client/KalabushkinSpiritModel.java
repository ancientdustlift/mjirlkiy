package com.kalabushkin.client;

import com.kalabushkin.Kalabushkin;
import com.kalabushkin.entity.KalabushkinSpiritEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

public class KalabushkinSpiritModel extends SinglePartEntityModel<KalabushkinSpiritEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Kalabushkin.id("kalabushkin_spirit"), "main");
	private static final int ALPHA = 0xB0;

	private final ModelPart root;
	private final ModelPart head;

	public KalabushkinSpiritModel(ModelPart root) {
		super(RenderLayer::getEntityTranslucent);
		this.root = root;
		this.head = root.getChild("waist").getChild("head");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData waist = data.getRoot().addChild("waist", ModelPartBuilder.create(),
				ModelTransform.pivot(0.0f, 12.0f, 0.0f));
		KalabushkinModel.addUpperBody(waist);
		// A tapering wisp instead of legs.
		ModelPartData tail1 = waist.addChild("tail1",
				ModelPartBuilder.create().uv(0, 16).cuboid(-2.5f, 0.0f, -1.5f, 5.0f, 5.0f, 3.0f),
				ModelTransform.NONE);
		ModelPartData tail2 = tail1.addChild("tail2",
				ModelPartBuilder.create().uv(0, 24).cuboid(-1.5f, 0.0f, -1.0f, 3.0f, 4.0f, 2.0f),
				ModelTransform.pivot(0.0f, 5.0f, 0.0f));
		tail2.addChild("tail3",
				ModelPartBuilder.create().uv(10, 24).cuboid(-0.5f, 0.0f, -0.5f, 1.0f, 3.0f, 1.0f),
				ModelTransform.pivot(0.0f, 4.0f, 0.0f));
		return TexturedModelData.of(data, 64, 64);
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(KalabushkinSpiritEntity entity, float limbAngle, float limbDistance, float animationProgress,
			float headYaw, float headPitch) {
		getPart().traverse().forEach(ModelPart::resetTransform);
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
		updateAnimation(entity.floatAnimationState, KalabushkinAnimations.SPIRIT_FLOAT, animationProgress, 1.0f);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
		super.render(matrices, vertices, light, overlay, (color & 0x00FFFFFF) | (ALPHA << 24));
	}
}
