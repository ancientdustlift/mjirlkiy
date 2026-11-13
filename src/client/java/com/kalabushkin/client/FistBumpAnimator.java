package com.kalabushkin.client;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Plays the fist bump on any mob's model: its right arm, front leg, wing or fin is raised towards
 * the player. Models without such a limb lean in instead.
 */
public final class FistBumpAnimator {
	/** Implemented by ModelPart through a mixin: the name the part has in its parent. */
	public interface NamedPart {
		String kalabushkin$getName();
	}

	private static final float DURATION_TICKS = 20.0f;
	private static final float RAISED_PITCH = -MathHelper.HALF_PI;
	private static final float JAB_PIXELS = 3.0f;
	private static final float LEAN_WITH_LIMB = 8.0f;
	private static final float LEAN_WITHOUT_LIMB = 25.0f;
	/** Limbs that can give a fist, best first. */
	private static final List<String> LIMBS = List.of(
			"right_arm", "arms", "right_front_leg", "right_front_foot", "right_wing", "right_fin", "right_leg");

	private static final Map<Integer, Integer> START_AGE = new HashMap<>();
	private static final Map<EntityModel<?>, List<ModelPart>> ROOTS = new WeakHashMap<>();

	private static ModelPart posedPart;
	private static ModelTransform originalTransform;

	private FistBumpAnimator() {
	}

	public static void start(Entity entity) {
		START_AGE.put(entity.getId(), entity.age);
	}

	/** Called right after the model has been posed for this frame. */
	public static void apply(LivingEntity entity, EntityModel<?> model, float tickDelta, MatrixStack matrices) {
		if (START_AGE.isEmpty()) {
			return;
		}
		Integer start = START_AGE.get(entity.getId());
		if (start == null) {
			return;
		}
		float progress = (entity.age + tickDelta - start) / DURATION_TICKS;
		if (progress < 0.0f || progress >= 1.0f) {
			START_AGE.remove(entity.getId());
			return;
		}
		float raise = raise(progress);
		Optional<ModelPart> limb = findLimb(model);
		if (limb.isPresent()) {
			ModelPart part = limb.get();
			posedPart = part;
			originalTransform = part.getTransform();
			part.pitch = MathHelper.lerp(raise, part.pitch, RAISED_PITCH);
			part.yaw = MathHelper.lerp(raise, part.yaw, 0.0f);
			part.roll = MathHelper.lerp(raise, part.roll, 0.0f);
			// The jab keeps the bump readable on mobs whose arms are already out, such as zombies.
			part.pivotZ -= JAB_PIXELS * raise;
		}
		// Model space is flipped here: the feet are at y = 1.501 and the front faces -z.
		float lean = (limb.isPresent() ? LEAN_WITH_LIMB : LEAN_WITHOUT_LIMB) * raise;
		matrices.translate(0.0f, 1.501f, 0.0f);
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(lean));
		matrices.translate(0.0f, -1.501f, 0.0f);
	}

	/** Called when the entity has been drawn, so the pose does not leak into models that never reset it. */
	public static void restore() {
		if (posedPart != null) {
			posedPart.setTransform(originalTransform);
			posedPart = null;
			originalTransform = null;
		}
	}

	/** 0 at rest, 1 with the fist out; overshoots slightly at the moment of the bump. */
	private static float raise(float t) {
		if (t < 0.3f) {
			return smooth(t / 0.3f);
		} else if (t < 0.45f) {
			return 1.0f + 0.12f * MathHelper.sin((t - 0.3f) / 0.15f * MathHelper.PI);
		} else if (t < 0.65f) {
			return 1.0f;
		}
		return 1.0f - smooth((t - 0.65f) / 0.35f);
	}

	private static float smooth(float t) {
		return t * t * (3.0f - 2.0f * t);
	}

	private static Optional<ModelPart> findLimb(EntityModel<?> model) {
		List<ModelPart> roots = ROOTS.computeIfAbsent(model, FistBumpAnimator::partFields);
		ModelPart best = null;
		int bestRank = LIMBS.size();
		for (ModelPart root : roots) {
			for (ModelPart part : (Iterable<ModelPart>) root.traverse()::iterator) {
				int rank = LIMBS.indexOf(((NamedPart) (Object) part).kalabushkin$getName());
				// Skips limbs the model currently hides, such as an illager's uncrossed arms.
				if (rank >= 0 && rank < bestRank && part.visible) {
					best = part;
					bestRank = rank;
				}
			}
		}
		return Optional.ofNullable(best);
	}

	/** Vanilla models share no accessor for their parts, so the fields are read by type. */
	private static List<ModelPart> partFields(EntityModel<?> model) {
		List<ModelPart> parts = new ArrayList<>();
		for (Class<?> type = model.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
			for (Field field : type.getDeclaredFields()) {
				if (field.getType() != ModelPart.class || Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				try {
					field.setAccessible(true);
					if (field.get(model) instanceof ModelPart part) {
						parts.add(part);
					}
				} catch (ReflectiveOperationException | RuntimeException ignored) {
					// An unreadable field only means this model falls back to leaning in.
				}
			}
		}
		return parts;
	}
}
