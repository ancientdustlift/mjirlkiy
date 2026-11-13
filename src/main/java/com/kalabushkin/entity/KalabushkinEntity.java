package com.kalabushkin.entity;

import com.kalabushkin.spirit.SpiritManager;
import java.util.EnumSet;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

public class KalabushkinEntity extends PathAwareEntity {
	private static final byte FIST_BUMP_STATUS = 88;
	private static final int FIST_BUMP_TICKS = 30;
	// Tick of the bump at which the arm is fully extended in the animation.
	private static final int FIST_CONTACT_TICK = 10;
	private static final double SOLITUDE_RADIUS = 96.0;

	public final AnimationState idleAnimationState = new AnimationState();
	public final AnimationState fistBumpAnimationState = new AnimationState();

	private int fistBumpTicks;
	private PlayerEntity fistBumpPartner;

	public KalabushkinEntity(EntityType<? extends KalabushkinEntity> type, World world) {
		super(type, world);
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
	}

	/** Natural spawns: lit, open-sky grass, and only one Kalabushkin per neighbourhood. */
	public static boolean canSpawnHere(EntityType<KalabushkinEntity> type, ServerWorldAccess world,
			SpawnReason reason, BlockPos pos, Random random) {
		if (!world.getBlockState(pos.down()).isIn(BlockTags.ANIMALS_SPAWNABLE_ON)
				|| world.getBaseLightLevel(pos, 0) <= 8
				|| !world.isSkyVisible(pos)) {
			return false;
		}
		return world.getEntitiesByClass(KalabushkinEntity.class, new Box(pos).expand(SOLITUDE_RADIUS), e -> true)
				.isEmpty();
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(1, new FistBumpGoal());
		goalSelector.add(2, new EscapeDangerGoal(this, 1.5));
		goalSelector.add(3, new WanderAroundFarGoal(this, 0.7));
		goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
		goalSelector.add(5, new LookAroundGoal(this));
	}

	@Override
	protected ActionResult interactMob(PlayerEntity player, Hand hand) {
		if (hand != Hand.MAIN_HAND || !isAlive()) {
			return super.interactMob(player, hand);
		}
		if (!getWorld().isClient && fistBumpTicks == 0) {
			fistBumpTicks = FIST_BUMP_TICKS;
			fistBumpPartner = player;
			getWorld().sendEntityStatus(this, FIST_BUMP_STATUS);
		}
		return ActionResult.success(getWorld().isClient);
	}

	@Override
	public void handleStatus(byte status) {
		if (status == FIST_BUMP_STATUS) {
			fistBumpAnimationState.start(age);
		} else {
			super.handleStatus(status);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			idleAnimationState.startIfNotRunning(age);
		}
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		if (fistBumpTicks > 0) {
			fistBumpTicks--;
			if (fistBumpTicks == FIST_BUMP_TICKS - FIST_CONTACT_TICK) {
				onFistContact();
			}
			if (fistBumpTicks == 0) {
				fistBumpPartner = null;
			}
		}
	}

	private void onFistContact() {
		if (!(getWorld() instanceof ServerWorld world)) {
			return;
		}
		float yaw = bodyYaw * MathHelper.RADIANS_PER_DEGREE;
		// Right fist, stretched out in front of the body.
		Vec3d fist = getPos().add(new Vec3d(-0.32, 1.35, 0.85).rotateY(-yaw));
		world.spawnParticles(ParticleTypes.CRIT, fist.x, fist.y, fist.z, 8, 0.08, 0.08, 0.08, 0.15);
		playSound(SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 1.2f);
	}

	@Override
	public void onDeath(DamageSource source) {
		super.onDeath(source);
		if (!(getWorld() instanceof ServerWorld world)) {
			return;
		}
		PlayerEntity haunted = source.getAttacker() instanceof PlayerEntity attacker
				? attacker
				: world.getClosestPlayer(this, 32.0);
		if (haunted instanceof ServerPlayerEntity player) {
			world.spawnParticles(ParticleTypes.SOUL, getX(), getY() + 1.0, getZ(), 30, 0.3, 0.6, 0.3, 0.03);
			playSound(SoundEvents.PARTICLE_SOUL_ESCAPE.value(), 2.0f, 0.8f);
			SpiritManager.haunt(player, getPos());
		}
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_PLAYER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_PLAYER_DEATH;
	}

	/** Stands still and faces the player for the duration of a fist bump. */
	private class FistBumpGoal extends Goal {
		FistBumpGoal() {
			setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
		}

		@Override
		public boolean canStart() {
			return fistBumpTicks > 0 && fistBumpPartner != null;
		}

		@Override
		public void start() {
			getNavigation().stop();
		}

		@Override
		public boolean shouldRunEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			if (fistBumpPartner != null) {
				getLookControl().lookAt(fistBumpPartner, 30.0f, 30.0f);
				lookAtEntity(fistBumpPartner, 30.0f, 30.0f);
			}
		}
	}
}
