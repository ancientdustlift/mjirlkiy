package com.kalabushkin.entity;

import java.util.UUID;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.FlyingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** The ghost left behind by Kalabushkin. Floats behind its owner; cannot be touched or hurt. */
public class KalabushkinSpiritEntity extends FlyingEntity {
	// Position relative to the owner: slightly to the left, above the ground, behind.
	private static final Vec3d FOLLOW_OFFSET = new Vec3d(1.1, 0.6, -1.6);
	private static final double MAX_SPEED = 1.5;

	public final AnimationState floatAnimationState = new AnimationState();

	private UUID ownerUuid;

	public KalabushkinSpiritEntity(EntityType<? extends KalabushkinSpiritEntity> type, World world) {
		super(type, world);
		noClip = true;
		setNoGravity(true);
		setInvulnerable(true);
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0);
	}

	public void setOwner(PlayerEntity owner) {
		ownerUuid = owner.getUuid();
	}

	@Override
	public void tick() {
		noClip = true;
		PlayerEntity owner = null;
		if (!getWorld().isClient) {
			owner = ownerUuid == null ? null : getWorld().getPlayerByUuid(ownerUuid);
			if (owner == null || owner.isRemoved()) {
				discard();
				return;
			}
			Vec3d target = owner.getPos().add(FOLLOW_OFFSET.rotateY(-owner.getYaw() * MathHelper.RADIANS_PER_DEGREE));
			Vec3d step = target.subtract(getPos()).multiply(0.18);
			if (step.lengthSquared() > MAX_SPEED * MAX_SPEED) {
				step = step.normalize().multiply(MAX_SPEED);
			}
			setVelocity(step);
		}

		super.tick();

		if (getWorld().isClient) {
			floatAnimationState.startIfNotRunning(age);
			if (random.nextInt(12) == 0) {
				getWorld().addParticle(ParticleTypes.SOUL,
						getParticleX(0.4), getY() + random.nextDouble() * 0.6, getParticleZ(0.4),
						0.0, 0.02, 0.0);
			}
		} else {
			faceOwner(owner);
		}
	}

	private void faceOwner(PlayerEntity owner) {
		double dx = owner.getX() - getX();
		double dz = owner.getZ() - getZ();
		double dy = owner.getEyeY() - getEyeY();
		float yaw = (float) (MathHelper.atan2(dz, dx) * MathHelper.DEGREES_PER_RADIAN) - 90.0f;
		float pitch = (float) -(MathHelper.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * MathHelper.DEGREES_PER_RADIAN);
		setYaw(yaw);
		setPitch(MathHelper.clamp(pitch, -40.0f, 40.0f));
		bodyYaw = yaw;
		headYaw = yaw;
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		return !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY);
	}

	@Override
	public boolean canHit() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public void pushAwayFrom(Entity entity) {
	}

	@Override
	protected void pushAway(Entity entity) {
	}

	@Override
	protected void tickCramming() {
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}

	@Override
	public boolean cannotDespawn() {
		return true;
	}

	@Override
	public boolean canUsePortals(boolean allowVehicles) {
		return false;
	}

	@Override
	public boolean isAffectedBySplashPotions() {
		return false;
	}

	@Override
	public boolean doesRenderOnFire() {
		return false;
	}
}
