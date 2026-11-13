package com.kalabushkin.client;

import com.kalabushkin.Kalabushkin;
import com.kalabushkin.entity.VladLogutovEntity;
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
 * Rider and mountain bike as one model. Everything hangs off "all", whose origin is the ground point
 * under the bike: -y is up, -z is forward, +x is the rider's left, units are model pixels.
 */
public class VladLogutovModel extends SinglePartEntityModel<VladLogutovEntity> {
	public static final EntityModelLayer LAYER = new EntityModelLayer(Kalabushkin.id("vlad_logutov"), "main");

	private static final float WHEEL_RADIUS = 6.0f;
	private static final float AXLE_Y = -WHEEL_RADIUS;
	private static final float REAR_Z = 11.5f;
	private static final float FRONT_Z = -11.0f;
	private static final float BRACKET_Y = -5.5f;
	private static final float BRACKET_Z = 1.5f;
	private static final float CRANK = 2.0f;
	private static final float STEER_Y = -14.5f;
	private static final float STEER_Z = -5.5f;
	private static final float GRIP_Y = -15.5f;
	private static final float GRIP_Z = -6.0f;
	private static final float HIP_X = 3.0f;
	private static final float HIP_Y = -14.5f;
	private static final float HIP_Z = 4.0f;
	private static final float FOOT_X = 3.5f;
	private static final float LEG_SEGMENT = 6.0f;
	private static final float TORSO = 12.0f;
	private static final float SHOULDER = 10.0f;

	// limbAngle advances 4 per block (16 px) travelled; the gearing is 2.5:1.
	private static final float WHEEL_TURN = 4.0f / WHEEL_RADIUS;
	private static final float CRANK_TURN = WHEEL_TURN / 2.5f;

	private static final float RIDE_LEAN = 30.0f * MathHelper.RADIANS_PER_DEGREE;
	private static final float STOP_LEAN = 8.0f * MathHelper.RADIANS_PER_DEGREE;
	// Stopped: the bike tips to the left and he slides off the saddle to stand over the top tube.
	private static final float STOP_ROLL = 8.0f * MathHelper.RADIANS_PER_DEGREE;
	private static final float STOP_DROP = 2.5f;
	private static final float STOP_SHIFT = -1.5f;
	private static final float GROUND_FOOT_X = 4.6f;
	private static final float GROUND_FOOT_Z = 2.0f;

	private final ModelPart root;
	private final ModelPart all;
	private final ModelPart rider;
	private final ModelPart waist;
	private final ModelPart head;
	private final ModelPart rightArm;
	private final ModelPart leftArm;
	private final ModelPart rightLeg;
	private final ModelPart rightShin;
	private final ModelPart leftLeg;
	private final ModelPart leftShin;
	private final ModelPart rearWheel;
	private final ModelPart frontWheel;
	private final ModelPart crank;
	private final ModelPart rightPedal;
	private final ModelPart leftPedal;
	private float tickDelta;

	public VladLogutovModel(ModelPart root) {
		this.root = root;
		this.all = root.getChild("all");
		ModelPart bike = all.getChild("bike");
		this.rearWheel = bike.getChild("rear_wheel");
		this.frontWheel = bike.getChild("steer").getChild("front_wheel");
		this.crank = bike.getChild("crank");
		this.rightPedal = crank.getChild("right_pedal");
		this.leftPedal = crank.getChild("left_pedal");
		this.rider = all.getChild("rider");
		this.waist = rider.getChild("waist");
		this.head = rider.getChild("head");
		this.rightArm = waist.getChild("right_arm");
		this.leftArm = waist.getChild("left_arm");
		this.rightLeg = rider.getChild("right_leg");
		this.rightShin = rightLeg.getChild("right_shin");
		this.leftLeg = rider.getChild("left_leg");
		this.leftShin = leftLeg.getChild("left_shin");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData data = new ModelData();
		ModelPartData all = data.getRoot().addChild("all", ModelPartBuilder.create(), ModelTransform.pivot(0.0f, 24.0f, 0.0f));
		addBike(all.addChild("bike", ModelPartBuilder.create(), ModelTransform.NONE));
		addRider(all.addChild("rider", ModelPartBuilder.create(), ModelTransform.NONE));
		return TexturedModelData.of(data, 128, 128);
	}

	private static void addBike(ModelPartData bike) {
		tube(bike, "seat_tube", 0.0f, BRACKET_Y, BRACKET_Z, -12.5f, 4.6f, 1.0f);
		tube(bike, "down_tube", 0.0f, BRACKET_Y - 0.5f, BRACKET_Z - 0.3f, -12.3f, -5.8f, 2.0f);
		tube(bike, "top_tube", 0.0f, -12.2f, 4.4f, -14.0f, -5.4f, 1.0f);
		for (int side = -1; side <= 1; side += 2) {
			String suffix = side < 0 ? "_right" : "_left";
			tube(bike, "chain_stay" + suffix, side * 1.5f, BRACKET_Y, BRACKET_Z, AXLE_Y, REAR_Z, 1.0f);
			tube(bike, "seat_stay" + suffix, side * 1.5f, AXLE_Y, REAR_Z, -12.0f, 4.8f, 1.0f);
		}
		bike.addChild("saddle",
				ModelPartBuilder.create().uv(16, 88).cuboid(-1.5f, -12.9f, 2.0f, 3.0f, 1.0f, 5.0f),
				ModelTransform.NONE);
		addWheel(bike, "rear_wheel", AXLE_Y, REAR_Z);

		ModelPartData steer = bike.addChild("steer", ModelPartBuilder.create(), ModelTransform.pivot(0.0f, STEER_Y, STEER_Z));
		steer.addChild("handlebar",
				ModelPartBuilder.create()
						.uv(16, 84).cuboid(-6.0f, GRIP_Y - STEER_Y - 0.5f, GRIP_Z - STEER_Z - 0.5f, 12.0f, 1.0f, 1.0f)
						.uv(0, 84).cuboid(-0.5f, -1.0f, -0.9f, 1.0f, 1.5f, 1.0f),
				ModelTransform.NONE);
		tube(steer, "head_tube", 0.0f, 0.0f, 0.0f, 2.5f, -0.8f, 1.0f);
		steer.addChild("fork_crown",
				ModelPartBuilder.create().uv(16, 96).cuboid(-2.0f, 2.0f, -1.4f, 4.0f, 1.0f, 1.0f),
				ModelTransform.NONE);
		float axleY = AXLE_Y - STEER_Y;
		float axleZ = FRONT_Z - STEER_Z;
		tube(steer, "fork_right", -1.5f, 2.5f, -0.9f, axleY, axleZ, 1.0f);
		tube(steer, "fork_left", 1.5f, 2.5f, -0.9f, axleY, axleZ, 1.0f);
		addWheel(steer, "front_wheel", axleY, axleZ);

		// Drive side is the rider's right. The right crank arm points up in the rest pose.
		ModelPartData crank = bike.addChild("crank",
				ModelPartBuilder.create()
						.uv(0, 84).cuboid(-3.0f, -CRANK - 0.5f, -0.5f, 1.0f, CRANK + 1.0f, 1.0f)
						.uv(0, 84).cuboid(2.0f, -0.5f, -0.5f, 1.0f, CRANK + 1.0f, 1.0f)
						.uv(48, 88).cuboid(-2.5f, -1.5f, -1.5f, 1.0f, 3.0f, 3.0f, new Dilation(-0.3f, 0.0f, 0.0f)),
				ModelTransform.pivot(0.0f, BRACKET_Y, BRACKET_Z));
		crank.addChild("right_pedal",
				ModelPartBuilder.create().uv(36, 88).cuboid(-1.0f, -0.5f, -1.5f, 2.0f, 1.0f, 3.0f),
				ModelTransform.pivot(-4.0f, -CRANK, 0.0f));
		crank.addChild("left_pedal",
				ModelPartBuilder.create().uv(36, 88).cuboid(-1.0f, -0.5f, -1.5f, 2.0f, 1.0f, 3.0f),
				ModelTransform.pivot(4.0f, CRANK, 0.0f));
	}

	/** A 1 px wide bar from (y0, z0) to (y1, z1) in the parent's vertical plane at the given x. */
	private static void tube(ModelPartData parent, String name, float x, float y0, float z0, float y1, float z1,
			float depth) {
		float dy = y1 - y0;
		float dz = z1 - z0;
		float length = MathHelper.sqrt(dy * dy + dz * dz);
		ModelPartBuilder builder = depth > 1.0f ? ModelPartBuilder.create().uv(8, 84) : ModelPartBuilder.create().uv(0, 84);
		parent.addChild(name,
				builder.cuboid(-0.5f, 0.0f, -depth / 2.0f, 1.0f, length, depth),
				ModelTransform.of(x, y0, z0, (float) Math.atan2(dz, dy), 0.0f, 0.0f));
	}

	/**
	 * An octagonal wheel built from four slabs at 45 degree steps. Each slab's texture only keeps its two
	 * end caps (tread), the tyre segments next to them and one spoke; the rest is cut out.
	 */
	private static void addWheel(ModelPartData parent, String name, float y, float z) {
		ModelPartData wheel = parent.addChild(name,
				ModelPartBuilder.create().uv(24, 64).cuboid(-1.0f, -1.0f, -1.0f, 2.0f, 2.0f, 2.0f, new Dilation(0.2f)),
				ModelTransform.pivot(0.0f, y, z));
		for (int i = 0; i < 4; i++) {
			wheel.addChild(name + "_segment" + i,
					ModelPartBuilder.create().uv(0, 64)
							.cuboid(-1.0f, -WHEEL_RADIUS, -2.5f, 2.0f, 2.0f * WHEEL_RADIUS, 5.0f),
					ModelTransform.rotation(i * MathHelper.PI / 4.0f, 0.0f, 0.0f));
		}
	}

	private static void addRider(ModelPartData rider) {
		ModelPartData waist = rider.addChild("waist", ModelPartBuilder.create(), ModelTransform.pivot(0.0f, HIP_Y, HIP_Z));
		waist.addChild("body",
				ModelPartBuilder.create()
						.uv(16, 16).cuboid(-4.0f, -TORSO, -2.0f, 8.0f, 12.0f, 4.0f)
						// Hood bunched up behind the neck.
						.uv(0, 32).cuboid(-4.5f, -TORSO - 0.5f, -2.5f, 9.0f, 3.0f, 5.0f),
				ModelTransform.NONE);
		waist.addChild("right_arm",
				ModelPartBuilder.create()
						.uv(40, 16).cuboid(-3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f)
						.uv(0, 40).cuboid(-3.0f, 7.0f, -2.0f, 4.0f, 3.0f, 4.0f, new Dilation(0.3f)),
				ModelTransform.pivot(-5.0f, -SHOULDER, 0.0f));
		waist.addChild("left_arm",
				ModelPartBuilder.create()
						.uv(32, 48).cuboid(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f)
						.uv(16, 40).cuboid(-1.0f, 7.0f, -2.0f, 4.0f, 3.0f, 4.0f, new Dilation(0.3f)),
				ModelTransform.pivot(5.0f, -SHOULDER, 0.0f));
		// Not a child of the leaning waist, so that looking around turns the head about a vertical axis.
		rider.addChild("head",
				ModelPartBuilder.create()
						.uv(0, 0).cuboid(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f)
						.uv(32, 0).cuboid(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, new Dilation(0.3f)),
				ModelTransform.pivot(0.0f, HIP_Y - TORSO, HIP_Z));
		addLeg(rider, "right", -HIP_X, 64);
		addLeg(rider, "left", HIP_X, 80);
	}

	private static void addLeg(ModelPartData rider, String side, float x, int u) {
		ModelPartData thigh = rider.addChild(side + "_leg",
				ModelPartBuilder.create().uv(u, 0).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, LEG_SEGMENT, 4.0f),
				ModelTransform.pivot(x, HIP_Y, HIP_Z));
		thigh.addChild(side + "_shin",
				// Slightly slimmer than the thigh so the two never z-fight at the knee.
				ModelPartBuilder.create().uv(u, 10).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, LEG_SEGMENT, 4.0f, new Dilation(-0.05f)),
				ModelTransform.pivot(0.0f, LEG_SEGMENT, 0.0f));
	}

	@Override
	public ModelPart getPart() {
		return root;
	}

	@Override
	public void animateModel(VladLogutovEntity entity, float limbAngle, float limbDistance, float tickDelta) {
		this.tickDelta = tickDelta;
	}

	@Override
	public void setAngles(VladLogutovEntity entity, float limbAngle, float limbDistance, float animationProgress,
			float headYaw, float headPitch) {
		pose(limbAngle, entity.getStopProgress(tickDelta), headYaw, headPitch);
		animateMovement(VladLogutovAnimations.RIDE, limbAngle, limbDistance, 2.0f, 2.5f);
		updateAnimation(entity.idleAnimationState, VladLogutovAnimations.IDLE, animationProgress, 1.0f);
		updateAnimation(entity.askAnimationState, VladLogutovAnimations.ASK, animationProgress, 1.0f);
	}

	/**
	 * Procedural base pose: wheels and cranks turn with the distance travelled, and the limbs are solved
	 * so hands stay on the grips and feet on the pedals. {@code stop} blends in the foot-down pose.
	 */
	public void pose(float limbAngle, float stop, float headYaw, float headPitch) {
		getPart().traverse().forEach(ModelPart::resetTransform);
		stop = stop * stop * (3.0f - 2.0f * stop);

		float wheelAngle = limbAngle * WHEEL_TURN;
		rearWheel.pitch = wheelAngle;
		frontWheel.pitch = wheelAngle;
		float crankAngle = limbAngle * CRANK_TURN;
		crank.pitch = crankAngle;
		rightPedal.pitch = -crankAngle;
		leftPedal.pitch = -crankAngle;

		float roll = stop * STOP_ROLL;
		all.roll = roll;
		float riderY = stop * STOP_DROP;
		float riderZ = stop * STOP_SHIFT;
		rider.pivotY = riderY;
		rider.pivotZ = riderZ;

		float lean = MathHelper.lerp(stop, RIDE_LEAN, STOP_LEAN);
		waist.pitch = lean;
		head.pivotY = HIP_Y - TORSO * MathHelper.cos(lean);
		head.pivotZ = HIP_Z - TORSO * MathHelper.sin(lean);
		head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;

		// Arms: aim from the shoulder at the grip, expressed in the leaning waist's frame.
		float shoulderY = riderY + HIP_Y - SHOULDER * MathHelper.cos(lean);
		float shoulderZ = riderZ + HIP_Z - SHOULDER * MathHelper.sin(lean);
		float armPitch = (float) Math.atan2(GRIP_Z - shoulderZ, GRIP_Y - shoulderY) - lean;
		rightArm.pitch = armPitch;
		leftArm.pitch = armPitch;

		float pedalY = -CRANK * MathHelper.cos(crankAngle);
		float pedalZ = -CRANK * MathHelper.sin(crankAngle);
		// Soles rest half a pixel above the pedal axle; the left pedal is half a turn behind the right.
		float soleY = BRACKET_Y - 0.5f - riderY;
		float soleZ = BRACKET_Z - riderZ;
		poseLeg(rightLeg, rightShin, -HIP_X, -FOOT_X, soleY + pedalY, soleZ + pedalZ);
		// The ground point is fixed in the world, so undo the bike's roll to find it in model space.
		float groundX = GROUND_FOOT_X * MathHelper.cos(roll);
		float groundY = -GROUND_FOOT_X * MathHelper.sin(roll) - riderY;
		poseLeg(leftLeg, leftShin, HIP_X,
				MathHelper.lerp(stop, FOOT_X, groundX),
				MathHelper.lerp(stop, soleY - pedalY, groundY),
				MathHelper.lerp(stop, soleZ - pedalZ, GROUND_FOOT_Z - riderZ));
	}

	/** Two-bone IK with the knee bending forward: puts the sole at the target, as far as the leg reaches. */
	private static void poseLeg(ModelPart thigh, ModelPart shin, float hipX, float x, float y, float z) {
		float dx = x - hipX;
		float dy = y - HIP_Y;
		float dz = z - HIP_Z;
		float down = MathHelper.sqrt(dx * dx + dy * dy);
		float reach = Math.min(MathHelper.sqrt(down * down + dz * dz), 2.0f * LEG_SEGMENT - 0.01f);
		float bend = (float) Math.acos(reach / (2.0f * LEG_SEGMENT));
		thigh.roll = (float) Math.atan2(-dx, dy);
		thigh.pitch = (float) Math.atan2(dz, down) - bend;
		shin.pitch = 2.0f * bend;
	}
}
