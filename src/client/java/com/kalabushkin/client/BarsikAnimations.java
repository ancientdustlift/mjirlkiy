package com.kalabushkin.client;

import static com.kalabushkin.client.KalabushkinAnimations.k;
import static com.kalabushkin.client.KalabushkinAnimations.rotate;
import static com.kalabushkin.client.KalabushkinAnimations.s;
import static com.kalabushkin.client.KalabushkinAnimations.scale;
import static com.kalabushkin.client.KalabushkinAnimations.t;
import static com.kalabushkin.client.KalabushkinAnimations.translate;

import net.minecraft.client.render.entity.animation.Animation;

public final class BarsikAnimations {
	public static final Animation IDLE = Animation.Builder.create(3.0f).looping()
			.addBoneAnimation("body", scale(s(0.0f, 1.0f, 1.0f, 1.0f), s(1.5f, 1.04f, 1.03f, 1.0f), s(3.0f, 1.0f, 1.0f, 1.0f)))
			.addBoneAnimation("tail", rotate(k(0.0f, 0, -12, 0), k(1.5f, 0, 12, 0), k(3.0f, 0, -12, 0)))
			.addBoneAnimation("tail2", rotate(k(0.0f, 0, 14, 0), k(1.5f, 0, -14, 0), k(3.0f, 0, 14, 0)))
			.build();

	// A heavy trot: diagonal leg pairs, the belly swinging from side to side under him.
	public static final Animation WALK = Animation.Builder.create(1.0f).looping()
			.addBoneAnimation("leg_front_right", rotate(k(0.0f, 30, 0, 0), k(0.5f, -30, 0, 0), k(1.0f, 30, 0, 0)))
			.addBoneAnimation("leg_hind_left", rotate(k(0.0f, 30, 0, 0), k(0.5f, -30, 0, 0), k(1.0f, 30, 0, 0)))
			.addBoneAnimation("leg_front_left", rotate(k(0.0f, -30, 0, 0), k(0.5f, 30, 0, 0), k(1.0f, -30, 0, 0)))
			.addBoneAnimation("leg_hind_right", rotate(k(0.0f, -30, 0, 0), k(0.5f, 30, 0, 0), k(1.0f, -30, 0, 0)))
			.addBoneAnimation("body", rotate(k(0.0f, 0, 0, -3), k(0.5f, 0, 0, 3), k(1.0f, 0, 0, -3)))
			.addBoneAnimation("belly", rotate(k(0.0f, 0, 0, 5), k(0.5f, 0, 0, -5), k(1.0f, 0, 0, 5)))
			.addBoneAnimation("head", rotate(k(0.0f, 3, 0, 3), k(0.5f, 3, 0, -3), k(1.0f, 3, 0, 3)))
			.addBoneAnimation("tail", rotate(k(0.0f, 10, -15, 0), k(0.5f, 10, 15, 0), k(1.0f, 10, -15, 0)))
			.build();

	// Flat on his belly with the legs tucked away and the tail wrapped round; every few seconds he peeks.
	public static final Animation HIDE = Animation.Builder.create(4.0f).looping()
			.addBoneAnimation("body", translate(t(0.0f, 0, -2, 0), t(4.0f, 0, -2, 0)))
			.addBoneAnimation("leg_front_right", scale(s(0.0f, 1, 0.35f, 1), s(4.0f, 1, 0.35f, 1)))
			.addBoneAnimation("leg_front_left", scale(s(0.0f, 1, 0.35f, 1), s(4.0f, 1, 0.35f, 1)))
			.addBoneAnimation("leg_hind_right", scale(s(0.0f, 1, 0.35f, 1), s(4.0f, 1, 0.35f, 1)))
			.addBoneAnimation("leg_hind_left", scale(s(0.0f, 1, 0.35f, 1), s(4.0f, 1, 0.35f, 1)))
			.addBoneAnimation("head", rotate(k(0.0f, 22, 0, 0), k(2.4f, 22, 0, 0), k(2.8f, -4, 32, 0),
					k(3.2f, -4, -32, 0), k(3.6f, 22, 0, 0), k(4.0f, 22, 0, 0)))
			.addBoneAnimation("head", translate(t(0.0f, 0, -1, 0), t(4.0f, 0, -1, 0)))
			.addBoneAnimation("tail", rotate(k(0.0f, -30, 75, 0), k(4.0f, -30, 75, 0)))
			.addBoneAnimation("tail2", rotate(k(0.0f, -25, 70, 0), k(4.0f, -25, 70, 0)))
			.build();

	// Hanging in the harness spread-eagled, swinging like a pendulum under the canopy.
	public static final Animation PARACHUTE = Animation.Builder.create(2.0f).looping()
			.addBoneAnimation("body", rotate(k(0.0f, -6, 0, -8), k(1.0f, -6, 0, 8), k(2.0f, -6, 0, -8)))
			.addBoneAnimation("parachute", rotate(k(0.0f, 0, 0, 3), k(1.0f, 0, 0, -3), k(2.0f, 0, 0, 3)))
			.addBoneAnimation("canopy", scale(s(0.0f, 1, 1, 1), s(0.5f, 1.03f, 0.9f, 1.03f), s(1.0f, 1, 1, 1),
					s(1.5f, 1.03f, 0.9f, 1.03f), s(2.0f, 1, 1, 1)))
			.addBoneAnimation("leg_front_right", rotate(k(0.0f, -55, 0, 40), k(1.0f, -45, 0, 30), k(2.0f, -55, 0, 40)))
			.addBoneAnimation("leg_front_left", rotate(k(0.0f, -45, 0, -30), k(1.0f, -55, 0, -40), k(2.0f, -45, 0, -30)))
			.addBoneAnimation("leg_hind_right", rotate(k(0.0f, 50, 0, 40), k(1.0f, 40, 0, 30), k(2.0f, 50, 0, 40)))
			.addBoneAnimation("leg_hind_left", rotate(k(0.0f, 40, 0, -30), k(1.0f, 50, 0, -40), k(2.0f, 40, 0, -30)))
			.addBoneAnimation("tail", rotate(k(0.0f, 30, -20, 0), k(1.0f, 30, 20, 0), k(2.0f, 30, -20, 0)))
			.addBoneAnimation("head", rotate(k(0.0f, 12, 0, 6), k(1.0f, 12, 0, -6), k(2.0f, 12, 0, 6)))
			.build();

	// The canopy bursts out of the pack, overshoots and settles.
	public static final Animation DEPLOY = Animation.Builder.create(0.6f)
			.addBoneAnimation("parachute", scale(s(0.0f, 0.1f, 0.25f, 0.1f), s(0.35f, 1.15f, 1.05f, 1.15f),
					s(0.6f, 1, 1, 1)))
			.build();

	// Touchdown: he squashes flat for a moment and springs back.
	public static final Animation LAND = Animation.Builder.create(0.5f)
			.addBoneAnimation("body", scale(s(0.0f, 1, 1, 1), s(0.12f, 1.18f, 0.7f, 1.1f), s(0.5f, 1, 1, 1)))
			.addBoneAnimation("body", translate(t(0.0f, 0, 0, 0), t(0.12f, 0, -1.5f, 0), t(0.5f, 0, 0, 0)))
			.addBoneAnimation("head", rotate(k(0.0f, 0, 0, 0), k(0.12f, 18, 0, 0), k(0.5f, 0, 0, 0)))
			.build();

	private BarsikAnimations() {
	}
}
