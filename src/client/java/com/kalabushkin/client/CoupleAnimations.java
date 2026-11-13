package com.kalabushkin.client;

import static com.kalabushkin.client.KalabushkinAnimations.k;
import static com.kalabushkin.client.KalabushkinAnimations.rotate;
import static com.kalabushkin.client.KalabushkinAnimations.s;
import static com.kalabushkin.client.KalabushkinAnimations.scale;
import static com.kalabushkin.client.KalabushkinAnimations.t;
import static com.kalabushkin.client.KalabushkinAnimations.translate;

import net.minecraft.client.render.entity.animation.Animation;

/** Animations for Alexey and Rita. Bones a model does not have (Rita's hair on Alexey) are simply skipped. */
public final class CoupleAnimations {
	// An unhurried stroll, shared by both; Rita's hair swings with each step.
	public static final Animation WALK = Animation.Builder.create(1.0f).looping()
			.addBoneAnimation("right_leg", rotate(k(0.0f, 28, 0, 0), k(0.5f, -28, 0, 0), k(1.0f, 28, 0, 0)))
			.addBoneAnimation("left_leg", rotate(k(0.0f, -28, 0, 0), k(0.5f, 28, 0, 0), k(1.0f, -28, 0, 0)))
			.addBoneAnimation("right_arm", rotate(k(0.0f, -22, 0, 0), k(0.5f, 22, 0, 0), k(1.0f, -22, 0, 0)))
			.addBoneAnimation("left_arm", rotate(k(0.0f, 22, 0, 0), k(0.5f, -22, 0, 0), k(1.0f, 22, 0, 0)))
			.addBoneAnimation("waist", rotate(k(0.0f, 1, -3, 0), k(0.5f, 1, 3, 0), k(1.0f, 1, -3, 0)))
			.addBoneAnimation("waist", translate(t(0.0f, 0, 0, 0), t(0.25f, 0, 0.4f, 0), t(0.5f, 0, 0, 0),
					t(0.75f, 0, 0.4f, 0), t(1.0f, 0, 0, 0)))
			.addBoneAnimation("hair_back", rotate(k(0.0f, 6, 0, -3), k(0.5f, 10, 0, 3), k(1.0f, 6, 0, -3)))
			.addBoneAnimation("hair_front_right", rotate(k(0.0f, -4, 0, 0), k(0.5f, 3, 0, 0), k(1.0f, -4, 0, 0)))
			.addBoneAnimation("hair_front_left", rotate(k(0.0f, 3, 0, 0), k(0.5f, -4, 0, 0), k(1.0f, 3, 0, 0)))
			.build();

	public static final Animation ALEXEY_IDLE = Animation.Builder.create(3.0f).looping()
			.addBoneAnimation("body", scale(s(0.0f, 1.0f, 1.0f, 1.0f), s(1.5f, 1.02f, 1.0f, 1.05f), s(3.0f, 1.0f, 1.0f, 1.0f)))
			.addBoneAnimation("right_arm", rotate(k(0.0f, 0, 0, 2), k(1.5f, 0, 0, 4), k(3.0f, 0, 0, 2)))
			.addBoneAnimation("left_arm", rotate(k(0.0f, 0, 0, -2), k(1.5f, 0, 0, -4), k(3.0f, 0, 0, -2)))
			.addBoneAnimation("head", rotate(k(0.0f, 0, 0, 0), k(1.5f, -1.5f, 0, 0), k(3.0f, 0, 0, 0)))
			.build();

	// Weight shifting from foot to foot, hair drifting, chin slightly raised.
	public static final Animation RITA_IDLE = Animation.Builder.create(4.0f).looping()
			.addBoneAnimation("body", scale(s(0.0f, 1.0f, 1.0f, 1.0f), s(2.0f, 1.02f, 1.0f, 1.04f), s(4.0f, 1.0f, 1.0f, 1.0f)))
			.addBoneAnimation("waist", rotate(k(0.0f, 0, 0, -1.5f), k(2.0f, 0, 0, 1.5f), k(4.0f, 0, 0, -1.5f)))
			.addBoneAnimation("head", rotate(k(0.0f, -4, 0, 2), k(2.0f, -5, 0, -2), k(4.0f, -4, 0, 2)))
			.addBoneAnimation("hair_back", rotate(k(0.0f, 2, 0, -2), k(2.0f, 4, 0, 2), k(4.0f, 2, 0, -2)))
			.addBoneAnimation("right_arm", rotate(k(0.0f, 0, 0, 3), k(4.0f, 0, 0, 3)))
			.addBoneAnimation("left_arm", rotate(k(0.0f, 0, 0, -3), k(4.0f, 0, 0, -3)))
			.build();

	// Alexey: hand to his heart, then an open gesture towards her, with a small bow of the head.
	public static final Animation COMPLIMENT = Animation.Builder.create(4.0f)
			.addBoneAnimation("right_arm", rotate(k(0.0f, 0, 0, 0), k(0.5f, -68, -42, 0), k(1.4f, -68, -42, 0),
					k(2.0f, -72, 0, 0), k(3.2f, -66, 0, 0), k(4.0f, 0, 0, 0)))
			.addBoneAnimation("left_arm", rotate(k(0.0f, 0, 0, 0), k(2.0f, -18, 0, -10), k(3.2f, -18, 0, -10),
					k(4.0f, 0, 0, 0)))
			.addBoneAnimation("waist", rotate(k(0.0f, 0, 0, 0), k(0.5f, 5, 0, 0), k(2.0f, 3, 0, 0), k(4.0f, 0, 0, 0)))
			.addBoneAnimation("head", rotate(k(0.0f, 0, 0, 0), k(0.5f, 10, 0, 4), k(1.4f, 8, 0, 4), k(2.0f, -6, 0, -4),
					k(3.2f, -4, 0, -4), k(4.0f, 0, 0, 0)))
			.build();

	// Rita: tilts her head, tucks a lock of hair behind her ear, looks down and back up. Says nothing.
	public static final Animation LISTEN = Animation.Builder.create(4.0f)
			.addBoneAnimation("head", rotate(k(0.0f, 0, 0, 0), k(0.6f, 6, 0, 10), k(1.6f, 12, 0, 8), k(2.6f, -4, 0, 10),
					k(3.4f, -4, 0, 6), k(4.0f, 0, 0, 0)))
			.addBoneAnimation("left_arm", rotate(k(0.0f, 0, 0, 0), k(0.8f, -150, 0, -22), k(1.1f, -158, 0, -14),
					k(1.4f, -150, 0, -22), k(2.2f, 0, 0, -3), k(4.0f, 0, 0, 0)))
			.addBoneAnimation("hair_front_left", rotate(k(0.0f, 0, 0, 0), k(1.1f, 0, 0, -14), k(2.4f, 0, 0, -8),
					k(4.0f, 0, 0, 0)))
			.addBoneAnimation("waist", rotate(k(0.0f, 0, 0, 0), k(1.0f, 0, 6, 2), k(3.0f, 0, -4, 2), k(4.0f, 0, 0, 0)))
			.addBoneAnimation("hair_back", rotate(k(0.0f, 0, 0, 0), k(1.0f, 4, 0, 5), k(3.0f, 3, 0, -4), k(4.0f, 0, 0, 0)))
			.build();

	private CoupleAnimations() {
	}
}
