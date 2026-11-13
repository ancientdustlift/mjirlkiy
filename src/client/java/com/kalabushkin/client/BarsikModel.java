package com.kalabushkin.client;

import com.kalabushkin.Kalabushkin;
import com.kalabushkin.entity.BarsikEntity;
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

/**
 * A fat tabby: wide body with a sagging belly on four stubby legs. The goggles, harness and parachute
 * are part of the model and only shown while he is parachuting.
 */
public class BarsikModel extends SinglePartEntityModel<BarsikEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Kalabushkin.id("barsik"), "main");

	private static final float BODY_TOP = 11.0f;
	private static final float CANOPY_HEIGHT = 22.0f;
	private static final float CANOPY_HALF_WIDTH = 11.0f;

	private final ModelPart root;
	private final ModelPart head;
	private final ModelPart goggles;
	private final ModelPart harness;
	private final ModelPart parachute;

	public BarsikModel(ModelPart root) {
		this.root = root;
		ModelPart body = root.getChild("body");
		this.head = body.getChild("head");
		this.goggles = head.getChild("goggles");
		this.harness = body.getChild("harness");
		this.parachute = root.getChild("parachute");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData root = data.getRoot();
		ModelPartData body = root.addChild("body",
				ModelPartBuilder.create().uv(0, 0).cuboid(-6.0f, -5.0f, -7.0f, 12.0f, 9.0f, 14.0f),
				ModelTransform.pivot(0.0f, 16.0f, 0.0f));
		body.addChild("belly",
				ModelPartBuilder.create().uv(0, 36).cuboid(-5.5f, 3.5f, -5.0f, 11.0f, 2.0f, 10.0f),
				ModelTransform.NONE);

		ModelPartData head = body.addChild("head",
				ModelPartBuilder.create()
						.uv(0, 23).cuboid(-4.0f, -4.0f, -6.0f, 8.0f, 7.0f, 6.0f)
						.uv(52, 0).cuboid(-4.0f, -6.0f, -3.0f, 2.0f, 2.0f, 1.0f)
						.uv(52, 3).cuboid(2.0f, -6.0f, -3.0f, 2.0f, 2.0f, 1.0f)
						.uv(52, 6).cuboid(-2.0f, -0.5f, -7.0f, 4.0f, 2.0f, 1.0f),
				ModelTransform.pivot(0.0f, -1.5f, -7.0f));
		head.addChild("goggles",
				ModelPartBuilder.create()
						.uv(14, 48).cuboid(-4.0f, -2.5f, -6.0f, 8.0f, 2.0f, 6.0f, new Dilation(0.25f))
						.uv(42, 48).cuboid(-3.5f, -3.0f, -6.8f, 3.0f, 3.0f, 1.0f)
						.uv(50, 48).cuboid(0.5f, -3.0f, -6.8f, 3.0f, 3.0f, 1.0f),
				ModelTransform.NONE);

		addLeg(body, "leg_front_right", 28, 23, -3.5f, -5.0f);
		addLeg(body, "leg_front_left", 40, 23, 3.5f, -5.0f);
		addLeg(body, "leg_hind_right", 28, 29, -3.5f, 5.0f);
		addLeg(body, "leg_hind_left", 40, 29, 3.5f, 5.0f);

		ModelPartData tail = body.addChild("tail",
				ModelPartBuilder.create().uv(42, 36).cuboid(-1.0f, -1.0f, 0.0f, 2.0f, 2.0f, 7.0f),
				ModelTransform.of(0.0f, -3.0f, 7.0f, 0.6f, 0.0f, 0.0f));
		tail.addChild("tail2",
				ModelPartBuilder.create().uv(0, 48).cuboid(-1.0f, -1.0f, 0.0f, 2.0f, 2.0f, 5.0f, new Dilation(-0.05f)),
				ModelTransform.of(0.0f, 0.0f, 7.0f, 0.5f, 0.0f, 0.0f));

		body.addChild("harness",
				ModelPartBuilder.create()
						.uv(88, 64).cuboid(-6.0f, -5.0f, -2.0f, 12.0f, 9.0f, 3.0f, new Dilation(0.3f))
						.uv(88, 76).cuboid(-3.0f, -7.0f, -2.0f, 6.0f, 2.0f, 4.0f),
				ModelTransform.NONE);

		ModelPartData parachute = root.addChild("parachute", ModelPartBuilder.create(),
				ModelTransform.pivot(0.0f, BODY_TOP, 0.0f));
		parachute.addChild("canopy",
				ModelPartBuilder.create()
						.uv(0, 64).cuboid(-CANOPY_HALF_WIDTH, -2.0f, -CANOPY_HALF_WIDTH, 22.0f, 4.0f, 22.0f)
						.uv(0, 90).cuboid(-8.0f, -4.0f, -8.0f, 16.0f, 2.0f, 16.0f),
				ModelTransform.pivot(0.0f, -CANOPY_HEIGHT, 0.0f));
		for (int sideX = -1; sideX <= 1; sideX += 2) {
			for (int sideZ = -1; sideZ <= 1; sideZ += 2) {
				addLine(parachute, "line" + (sideX + 1) + (sideZ + 1), sideX * 5.0f, sideZ * 2.0f,
						sideX * (CANOPY_HALF_WIDTH - 1.0f), sideZ * (CANOPY_HALF_WIDTH - 1.0f));
			}
		}
		return TexturedModelData.of(data, 128, 128);
	}

	private static void addLeg(ModelPartData body, String name, int u, int v, float x, float z) {
		body.addChild(name,
				ModelPartBuilder.create().uv(u, v).cuboid(-1.5f, 0.0f, -1.5f, 3.0f, 3.0f, 3.0f),
				ModelTransform.pivot(x, 5.0f, z));
	}

	/** A suspension line from a harness point on the cat's back up to the underside of the canopy. */
	private static void addLine(ModelPartData parachute, String name, float x0, float z0, float x1, float z1) {
		float dx = x1 - x0;
		float dz = z1 - z0;
		float rise = CANOPY_HEIGHT - 2.0f;
		float run = MathHelper.sqrt(dx * dx + dz * dz);
		float length = MathHelper.sqrt(run * run + rise * rise);
		// The bar points straight up; pitch tips it over by the lean angle, yaw then aims it at the corner.
		parachute.addChild(name,
				ModelPartBuilder.create().uv(120, 64).cuboid(-0.5f, -length, -0.5f, 1.0f, length, 1.0f, new Dilation(-0.25f)),
				ModelTransform.of(x0, 0.0f, z0, (float) Math.atan2(run, rise), (float) Math.atan2(-dx, -dz), 0.0f));
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void setAngles(BarsikEntity entity, float limbAngle, float limbDistance, float animationProgress,
			float headYaw, float headPitch) {
		boolean parachuting = entity.isParachuting();
		pose(parachuting, headYaw, headPitch);
		if (parachuting) {
			updateAnimation(entity.parachuteAnimationState, BarsikAnimations.PARACHUTE, animationProgress, 1.0f);
			updateAnimation(entity.deployAnimationState, BarsikAnimations.DEPLOY, animationProgress, 1.0f);
			return;
		}
		if (!entity.isHiding()) {
			animateMovement(BarsikAnimations.WALK, limbAngle, limbDistance, 3.0f, 2.5f);
		}
		updateAnimation(entity.idleAnimationState, BarsikAnimations.IDLE, animationProgress, 1.0f);
		updateAnimation(entity.hideAnimationState, BarsikAnimations.HIDE, animationProgress, 1.0f);
		updateAnimation(entity.landAnimationState, BarsikAnimations.LAND, animationProgress, 1.0f);
	}

	/** Rest pose, with the paratrooper gear shown or hidden. */
	public void pose(boolean parachuting, float headYaw, float headPitch) {
		getPart().traverse().forEach(ModelPart::resetTransform);
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
		goggles.visible = parachuting;
		harness.visible = parachuting;
		parachute.visible = parachuting;
	}
}
