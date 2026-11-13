package com.kalabushkin.client;

import static com.kalabushkin.client.KalabushkinAnimations.k;
import static com.kalabushkin.client.KalabushkinAnimations.rotate;
import static com.kalabushkin.client.KalabushkinAnimations.s;
import static com.kalabushkin.client.KalabushkinAnimations.scale;
import static com.kalabushkin.client.KalabushkinAnimations.t;
import static com.kalabushkin.client.KalabushkinAnimations.translate;

import net.minecraft.client.render.entity.animation.Animation;

/**
 * All values are offsets from the zombie stance set by the model: both arms straight out in front.
 * A positive arm pitch therefore lowers the arm, a negative one raises it towards the face.
 */
public final class VasilyAnimations {
	public static final Animation IDLE = Animation.Builder.create(4.0f).looping()
			.addBoneAnimation("body", scale(s(0.0f, 1.0f, 1.0f, 1.0f), s(2.0f, 1.02f, 1.0f, 1.05f), s(4.0f, 1.0f, 1.0f, 1.0f)))
			.addBoneAnimation("head", rotate(k(0.0f, 0, 0, 0), k(1.0f, 3, 0, -6), k(3.0f, -2, 0, 4), k(4.0f, 0, 0, 0)))
			.addBoneAnimation("waist", rotate(k(0.0f, 0, 0, -1.5f), k(2.0f, 0, 0, 1.5f), k(4.0f, 0, 0, -1.5f)))
			.build();

	// Stiff-legged shuffle: the body pitches forward and rocks, the outstretched arms bob out of step.
	public static final Animation SHAMBLE = Animation.Builder.create(1.2f).looping()
			.addBoneAnimation("right_leg", rotate(k(0.0f, 22, 0, 0), k(0.6f, -22, 0, 0), k(1.2f, 22, 0, 0)))
			.addBoneAnimation("left_leg", rotate(k(0.0f, -22, 0, 0), k(0.6f, 22, 0, 0), k(1.2f, -22, 0, 0)))
			.addBoneAnimation("waist", rotate(k(0.0f, 9, 0, -5), k(0.6f, 9, 0, 5), k(1.2f, 9, 0, -5)))
			.addBoneAnimation("right_arm", rotate(k(0.0f, -6, 0, 0), k(0.6f, 8, 0, 0), k(1.2f, -6, 0, 0)))
			.addBoneAnimation("left_arm", rotate(k(0.0f, 8, 0, 0), k(0.6f, -6, 0, 0), k(1.2f, 8, 0, 0)))
			.addBoneAnimation("head", rotate(k(0.0f, -6, 0, 6), k(0.6f, -6, 0, -8), k(1.2f, -6, 0, 6)))
			.build();

	// Left arm drops, right hand comes down to waist height and pumps three times.
	public static final Animation HANDSHAKE = Animation.Builder.create(1.5f)
			.addBoneAnimation("left_arm", rotate(k(0.0f, 0, 0, 0), k(0.3f, 72, 0, -4), k(1.2f, 72, 0, -4), k(1.5f, 0, 0, 0)))
			.addBoneAnimation("right_arm", rotate(k(0.0f, 0, 0, 0), k(0.3f, 24, 0, 0), k(0.45f, 12, 0, 0),
					k(0.6f, 30, 0, 0), k(0.75f, 12, 0, 0), k(0.9f, 30, 0, 0), k(1.05f, 12, 0, 0),
					k(1.2f, 26, 0, 0), k(1.5f, 0, 0, 0)))
			.addBoneAnimation("right_arm", translate(t(0.0f, 0, 0, 0), t(0.3f, 0, 0, -1.5f), t(1.2f, 0, 0, -1.5f),
					t(1.5f, 0, 0, 0)))
			.addBoneAnimation("waist", rotate(k(0.0f, 0, 0, 0), k(0.3f, 8, 0, 0), k(1.2f, 8, 0, 0), k(1.5f, 0, 0, 0)))
			.addBoneAnimation("head", rotate(k(0.0f, 0, 0, 0), k(0.45f, 10, 0, -8), k(0.75f, -4, 0, -8),
					k(1.05f, 10, 0, -8), k(1.5f, 0, 0, 0)))
			.build();

	// Twists the can open with the left hand, then holds it out.
	private static final Animation GIVE_SNUS = Animation.Builder.create(2.2f)
			.addBoneAnimation("right_arm", rotate(k(0.0f, 0, 0, 0), k(0.3f, 20, -22, 0), k(1.1f, 20, -22, 0),
					k(1.5f, -6, 0, 0), k(1.9f, -6, 0, 0), k(2.2f, 0, 0, 0)))
			.addBoneAnimation("left_arm", rotate(k(0.0f, 0, 0, 0), k(0.3f, 12, 26, 0), k(0.5f, 12, 26, 25),
					k(0.7f, 12, 26, -25), k(0.9f, 12, 26, 25), k(1.1f, 12, 26, 0), k(1.5f, 72, 0, -4),
					k(1.9f, 72, 0, -4), k(2.2f, 0, 0, 0)))
			.addBoneAnimation("right_arm", translate(t(0.0f, 0, 0, 0), t(1.1f, 0, 0, 0), t(1.5f, 0, 0, -2),
					t(1.9f, 0, 0, -2), t(2.2f, 0, 0, 0)))
			.addBoneAnimation("head", rotate(k(0.0f, 0, 0, 0), k(0.4f, 22, 0, -10), k(1.1f, 22, 0, -10),
					k(1.5f, -4, 0, 0), k(2.2f, 0, 0, 0)))
			.build();

	// Takes a long pull with his head back, blows the cloud out, then holds the vape out.
	private static final Animation GIVE_VAPE = Animation.Builder.create(2.2f)
			.addBoneAnimation("right_arm", rotate(k(0.0f, 0, 0, 0), k(0.35f, -52, -24, 0), k(0.75f, -52, -24, 0),
					k(1.1f, 10, 0, 0), k(1.5f, -6, 0, 0), k(1.9f, -6, 0, 0), k(2.2f, 0, 0, 0)))
			.addBoneAnimation("left_arm", rotate(k(0.0f, 0, 0, 0), k(0.35f, 72, 0, -4), k(1.9f, 72, 0, -4), k(2.2f, 0, 0, 0)))
			.addBoneAnimation("right_arm", translate(t(0.0f, 0, 0, 0), t(1.1f, 0, 0, 0), t(1.5f, 0, 0, -2),
					t(1.9f, 0, 0, -2), t(2.2f, 0, 0, 0)))
			.addBoneAnimation("head", rotate(k(0.0f, 0, 0, 0), k(0.35f, -18, 0, -10), k(0.75f, -24, 0, -10),
					k(1.0f, 8, 0, -10), k(1.5f, 0, 0, 0), k(2.2f, 0, 0, 0)))
			.addBoneAnimation("body", scale(s(0.0f, 1, 1, 1), s(0.7f, 1.06f, 1.0f, 1.2f), s(1.0f, 1, 1, 1), s(2.2f, 1, 1, 1)))
			.build();

	// Cups the left hand around it to light it, takes one drag, then holds it out.
	private static final Animation GIVE_CIGARETTE = Animation.Builder.create(2.2f)
			.addBoneAnimation("right_arm", rotate(k(0.0f, 0, 0, 0), k(0.3f, -50, -24, 0), k(1.0f, -50, -24, 0),
					k(1.5f, -6, 0, 0), k(1.9f, -6, 0, 0), k(2.2f, 0, 0, 0)))
			.addBoneAnimation("left_arm", rotate(k(0.0f, 0, 0, 0), k(0.3f, -46, 24, 0), k(0.45f, -40, 24, 0),
					k(0.6f, -46, 24, 0), k(0.75f, -40, 24, 0), k(1.0f, -46, 24, 0), k(1.5f, 72, 0, -4),
					k(1.9f, 72, 0, -4), k(2.2f, 0, 0, 0)))
			.addBoneAnimation("right_arm", translate(t(0.0f, 0, 0, 0), t(1.0f, 0, 0, 0), t(1.5f, 0, 0, -2),
					t(1.9f, 0, 0, -2), t(2.2f, 0, 0, 0)))
			.addBoneAnimation("head", rotate(k(0.0f, 0, 0, 0), k(0.3f, 14, 0, -10), k(1.0f, 14, 0, -10),
					k(1.3f, -8, 0, -10), k(1.6f, 0, 0, 0), k(2.2f, 0, 0, 0)))
			.build();

	// One drag and he doubles over coughing, then holds it out with a shaking hand.
	private static final Animation GIVE_DYADKOVO = Animation.Builder.create(2.2f)
			.addBoneAnimation("right_arm", rotate(k(0.0f, 0, 0, 0), k(0.3f, -50, -24, 0), k(0.6f, -50, -24, 0),
					k(0.8f, 30, 0, 0), k(1.4f, 30, 0, 0), k(1.6f, -6, 0, 5), k(1.7f, -6, 0, -5), k(1.8f, -6, 0, 5),
					k(1.9f, -6, 0, -5), k(2.2f, 0, 0, 0)))
			.addBoneAnimation("left_arm", rotate(k(0.0f, 0, 0, 0), k(0.3f, 72, 0, -4), k(0.8f, 20, 30, 0),
					k(1.4f, 20, 30, 0), k(1.6f, 72, 0, -4), k(1.9f, 72, 0, -4), k(2.2f, 0, 0, 0)))
			.addBoneAnimation("waist", rotate(k(0.0f, 0, 0, 0), k(0.6f, -4, 0, 0), k(0.8f, 34, 0, 0), k(0.95f, 22, 0, 0),
					k(1.1f, 36, 0, 0), k(1.25f, 22, 0, 0), k(1.4f, 34, 0, 0), k(1.7f, 4, 0, 0), k(2.2f, 0, 0, 0)))
			.addBoneAnimation("head", rotate(k(0.0f, 0, 0, 0), k(0.3f, -14, 0, -10), k(0.6f, -14, 0, -10),
					k(0.8f, 20, 0, -10), k(0.95f, 4, 0, -10), k(1.1f, 22, 0, -10), k(1.25f, 4, 0, -10),
					k(1.4f, 20, 0, -10), k(1.7f, 0, 0, 0), k(2.2f, 0, 0, 0)))
			.addBoneAnimation("right_arm", translate(t(0.0f, 0, 0, 0), t(1.4f, 0, 0, 0), t(1.6f, 0, 0, -2),
					t(1.9f, 0, 0, -2), t(2.2f, 0, 0, 0)))
			.build();

	/** Indexed by NicotineItem.Kind ordinal. */
	public static final Animation[] GIVE = {GIVE_SNUS, GIVE_VAPE, GIVE_CIGARETTE, GIVE_DYADKOVO};

	private VasilyAnimations() {
	}
}
