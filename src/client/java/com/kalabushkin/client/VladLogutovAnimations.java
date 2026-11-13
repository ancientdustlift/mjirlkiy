package com.kalabushkin.client;

import static com.kalabushkin.client.KalabushkinAnimations.k;
import static com.kalabushkin.client.KalabushkinAnimations.rotate;
import static com.kalabushkin.client.KalabushkinAnimations.s;
import static com.kalabushkin.client.KalabushkinAnimations.scale;
import static com.kalabushkin.client.KalabushkinAnimations.t;
import static com.kalabushkin.client.KalabushkinAnimations.translate;

import net.minecraft.client.render.entity.animation.Animation;

/** Keyframed layers on top of the model's procedural pedalling: all values are offsets from that pose. */
public final class VladLogutovAnimations {
	public static final Animation IDLE = Animation.Builder.create(3.0f).looping()
			.addBoneAnimation("body", scale(s(0.0f, 1.0f, 1.0f, 1.0f), s(1.5f, 1.02f, 1.0f, 1.06f), s(3.0f, 1.0f, 1.0f, 1.0f)))
			.addBoneAnimation("head", rotate(k(0.0f, 0, 0, 0), k(1.5f, -1.5f, 0, 0), k(3.0f, 0, 0, 0)))
			.build();

	// Rocking over the pedals with small steering corrections.
	public static final Animation RIDE = Animation.Builder.create(1.0f).looping()
			.addBoneAnimation("rider", rotate(k(0.0f, 0, 0, -2), k(0.5f, 0, 0, 2), k(1.0f, 0, 0, -2)))
			.addBoneAnimation("rider", translate(t(0.0f, 0, 0, 0), t(0.25f, 0, 0.4f, 0), t(0.5f, 0, 0, 0),
					t(0.75f, 0, 0.4f, 0), t(1.0f, 0, 0, 0)))
			.addBoneAnimation("steer", rotate(k(0.0f, 0, -4, 0), k(0.5f, 0, 4, 0), k(1.0f, 0, -4, 0)))
			.addBoneAnimation("head", rotate(k(0.0f, 2, 0, 0), k(0.5f, -1, 0, 0), k(1.0f, 2, 0, 0)))
			.build();

	// Waits for the foot to come down, then points at the player, wags the hand and ends on a shrug.
	public static final Animation ASK = Animation.Builder.create(4.5f)
			.addBoneAnimation("right_arm", rotate(k(0.0f, 0, 0, 0), k(0.4f, 0, 0, 0), k(0.8f, -40, 0, 0),
					k(1.2f, -30, 0, 10), k(1.6f, -42, 0, 0), k(2.0f, -30, 0, 10), k(2.6f, -40, 0, 0),
					k(3.2f, -20, -25, 0), k(3.8f, -20, -25, 0), k(4.5f, 0, 0, 0)))
			.addBoneAnimation("head", rotate(k(0.0f, 0, 0, 0), k(0.4f, 0, 0, 0), k(0.9f, -6, 0, 6),
					k(1.4f, 4, 0, 6), k(1.9f, -6, 0, 6), k(2.4f, 4, 0, 6), k(3.2f, -3, 0, -5),
					k(3.8f, -3, 0, -5), k(4.5f, 0, 0, 0)))
			.build();

	private VladLogutovAnimations() {
	}
}
