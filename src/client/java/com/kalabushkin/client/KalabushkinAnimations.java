package com.kalabushkin.client;

import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.AnimationHelper;
import net.minecraft.client.render.entity.animation.Keyframe;
import net.minecraft.client.render.entity.animation.Transformation;

/** Keyframe animations; every keyframe uses Catmull-Rom interpolation so motion eases in and out. */
public final class KalabushkinAnimations {
	public static final Animation IDLE = Animation.Builder.create(3.0f).looping()
			.addBoneAnimation("right_arm", rotate(k(0.0f, 0, 0, 2), k(1.5f, 2, 0, 5), k(3.0f, 0, 0, 2)))
			.addBoneAnimation("left_arm", rotate(k(0.0f, 0, 0, -2), k(1.5f, -2, 0, -5), k(3.0f, 0, 0, -2)))
			.addBoneAnimation("body", scale(s(0.0f, 1.0f, 1.0f, 1.0f), s(1.5f, 1.02f, 1.0f, 1.06f), s(3.0f, 1.0f, 1.0f, 1.0f)))
			.addBoneAnimation("head", rotate(k(0.0f, 0, 0, 0), k(1.5f, -1.5f, 0, 0), k(3.0f, 0, 0, 0)))
			.build();

	public static final Animation WALK = Animation.Builder.create(1.0f).looping()
			.addBoneAnimation("right_leg", rotate(k(0.0f, 32, 0, 0), k(0.5f, -32, 0, 0), k(1.0f, 32, 0, 0)))
			.addBoneAnimation("left_leg", rotate(k(0.0f, -32, 0, 0), k(0.5f, 32, 0, 0), k(1.0f, -32, 0, 0)))
			.addBoneAnimation("right_arm", rotate(k(0.0f, -26, 0, 0), k(0.5f, 26, 0, 0), k(1.0f, -26, 0, 0)))
			.addBoneAnimation("left_arm", rotate(k(0.0f, 26, 0, 0), k(0.5f, -26, 0, 0), k(1.0f, 26, 0, 0)))
			.addBoneAnimation("waist", rotate(k(0.0f, 2, -4, 0), k(0.5f, 2, 4, 0), k(1.0f, 2, -4, 0)))
			.addBoneAnimation("waist", translate(t(0.0f, 0, 0, 0), t(0.25f, 0, 0.5f, 0), t(0.5f, 0, 0, 0),
					t(0.75f, 0, 0.5f, 0), t(1.0f, 0, 0, 0)))
			.build();

	// Wind up, extend the right fist, hold for the bump, recoil, relax.
	public static final Animation FIST_BUMP = Animation.Builder.create(1.5f)
			.addBoneAnimation("right_arm", rotate(k(0.0f, 0, 0, 0), k(0.22f, 28, 0, 6), k(0.5f, -92, -8, 0),
					k(0.62f, -84, -8, 0), k(0.9f, -90, -8, 0), k(1.5f, 0, 0, 0)))
			.addBoneAnimation("right_arm", translate(t(0.0f, 0, 0, 0), t(0.22f, 0, 0, 1), t(0.5f, 0, 0, -2.5f),
					t(0.62f, 0, 0, -1.5f), t(0.9f, 0, 0, -2), t(1.5f, 0, 0, 0)))
			.addBoneAnimation("left_arm", rotate(k(0.0f, 0, 0, 0), k(0.5f, 14, 0, -4), k(0.9f, 12, 0, -4), k(1.5f, 0, 0, 0)))
			.addBoneAnimation("waist", rotate(k(0.0f, 0, 0, 0), k(0.22f, -3, -8, 0), k(0.5f, 7, 10, 0),
					k(0.9f, 6, 9, 0), k(1.5f, 0, 0, 0)))
			.addBoneAnimation("head", rotate(k(0.0f, 0, 0, 0), k(0.5f, -5, -9, 0), k(0.7f, 8, -9, 0),
					k(0.9f, -4, -9, 0), k(1.5f, 0, 0, 0)))
			.build();

	public static final Animation SPIRIT_FLOAT = Animation.Builder.create(4.0f).looping()
			.addBoneAnimation("waist", translate(t(0.0f, 0, 0, 0), t(2.0f, 0, 2.5f, 0), t(4.0f, 0, 0, 0)))
			.addBoneAnimation("waist", rotate(k(0.0f, 4, 0, -2), k(2.0f, 8, 0, 2), k(4.0f, 4, 0, -2)))
			.addBoneAnimation("right_arm", rotate(k(0.0f, -12, 0, 8), k(2.0f, -4, 0, 16), k(4.0f, -12, 0, 8)))
			.addBoneAnimation("left_arm", rotate(k(0.0f, -4, 0, -16), k(2.0f, -12, 0, -8), k(4.0f, -4, 0, -16)))
			.addBoneAnimation("tail1", rotate(k(0.0f, 10, 0, -6), k(1.0f, 16, 0, 0), k(2.0f, 10, 0, 6),
					k(3.0f, 4, 0, 0), k(4.0f, 10, 0, -6)))
			.addBoneAnimation("tail2", rotate(k(0.0f, 14, 0, 0), k(1.0f, 10, 0, -12), k(2.0f, 14, 0, 0),
					k(3.0f, 18, 0, 12), k(4.0f, 14, 0, 0)))
			.addBoneAnimation("tail3", rotate(k(0.0f, 10, 0, 14), k(1.0f, 16, 0, 0), k(2.0f, 10, 0, -14),
					k(3.0f, 16, 0, 0), k(4.0f, 10, 0, 14)))
			.build();

	private KalabushkinAnimations() {
	}

	private static Transformation rotate(Keyframe... keyframes) {
		return new Transformation(Transformation.Targets.ROTATE, keyframes);
	}

	private static Transformation translate(Keyframe... keyframes) {
		return new Transformation(Transformation.Targets.TRANSLATE, keyframes);
	}

	private static Transformation scale(Keyframe... keyframes) {
		return new Transformation(Transformation.Targets.SCALE, keyframes);
	}

	/** Rotation keyframe, degrees. */
	private static Keyframe k(float time, float x, float y, float z) {
		return new Keyframe(time, AnimationHelper.createRotationalVector(x, y, z), Transformation.Interpolations.CUBIC);
	}

	/** Translation keyframe, model pixels, +y up. */
	private static Keyframe t(float time, float x, float y, float z) {
		return new Keyframe(time, AnimationHelper.createTranslationalVector(x, y, z), Transformation.Interpolations.CUBIC);
	}

	private static Keyframe s(float time, float x, float y, float z) {
		return new Keyframe(time, AnimationHelper.createScalingVector(x, y, z), Transformation.Interpolations.CUBIC);
	}
}
