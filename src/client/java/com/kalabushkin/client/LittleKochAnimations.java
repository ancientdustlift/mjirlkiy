package com.kalabushkin.client;

import static com.kalabushkin.client.KalabushkinAnimations.k;
import static com.kalabushkin.client.KalabushkinAnimations.rotate;
import static com.kalabushkin.client.KalabushkinAnimations.s;
import static com.kalabushkin.client.KalabushkinAnimations.scale;
import static com.kalabushkin.client.KalabushkinAnimations.t;
import static com.kalabushkin.client.KalabushkinAnimations.translate;

import net.minecraft.client.render.entity.animation.Animation;

public final class LittleKochAnimations {
	// Slow breathing: the belly swells forward while the arms drift out a little.
	public static final Animation IDLE = Animation.Builder.create(2.5f).looping()
			.addBoneAnimation("belly", scale(s(0.0f, 1.0f, 1.0f, 1.0f), s(1.25f, 1.03f, 1.03f, 1.3f), s(2.5f, 1.0f, 1.0f, 1.0f)))
			.addBoneAnimation("right_arm", rotate(k(0.0f, 0, 0, 5), k(1.25f, 0, 0, 9), k(2.5f, 0, 0, 5)))
			.addBoneAnimation("left_arm", rotate(k(0.0f, 0, 0, -5), k(1.25f, 0, 0, -9), k(2.5f, 0, 0, -5)))
			.addBoneAnimation("head", rotate(k(0.0f, 0, 0, 0), k(1.25f, -2, 0, 0), k(2.5f, 0, 0, 0)))
			.build();

	// A waddle: short steps, the whole body rocking from side to side and the belly bouncing.
	public static final Animation WADDLE = Animation.Builder.create(1.0f).looping()
			.addBoneAnimation("right_leg", rotate(k(0.0f, 26, 0, 0), k(0.5f, -26, 0, 0), k(1.0f, 26, 0, 0)))
			.addBoneAnimation("left_leg", rotate(k(0.0f, -26, 0, 0), k(0.5f, 26, 0, 0), k(1.0f, -26, 0, 0)))
			.addBoneAnimation("right_arm", rotate(k(0.0f, -20, 0, 8), k(0.5f, 20, 0, 8), k(1.0f, -20, 0, 8)))
			.addBoneAnimation("left_arm", rotate(k(0.0f, 20, 0, -8), k(0.5f, -20, 0, -8), k(1.0f, 20, 0, -8)))
			.addBoneAnimation("waist", rotate(k(0.0f, 3, 0, -6), k(0.5f, 3, 0, 6), k(1.0f, 3, 0, -6)))
			.addBoneAnimation("waist", translate(t(0.0f, 0, 0, 0), t(0.25f, 0, 0.6f, 0), t(0.5f, 0, 0, 0),
					t(0.75f, 0, 0.6f, 0), t(1.0f, 0, 0, 0)))
			.addBoneAnimation("belly", translate(t(0.0f, 0, 0.3f, 0), t(0.25f, 0, -0.4f, 0), t(0.5f, 0, 0.3f, 0),
					t(0.75f, 0, -0.4f, 0), t(1.0f, 0, 0.3f, 0)))
			.addBoneAnimation("head", rotate(k(0.0f, 0, 0, 4), k(0.5f, 0, 0, -4), k(1.0f, 0, 0, 4)))
			.build();

	private LittleKochAnimations() {
	}
}
