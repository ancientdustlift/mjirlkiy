package com.kalabushkin.entity;

import java.util.EnumSet;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

/**
 * A big, fat cat that cannot be hurt and spends his life looking for somewhere to hide.
 * Falling more than three blocks turns him into a paratrooper: goggles on, parachute out, soft landing.
 */
public class BarsikEntity extends PathAwareEntity {
	private static final TrackedData<Boolean> PARACHUTING =
			DataTracker.registerData(BarsikEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final TrackedData<Boolean> HIDING =
			DataTracker.registerData(BarsikEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final byte LAND_STATUS = 92;
	private static final float OPEN_PARACHUTE_AFTER_BLOCKS = 3.0f;
	// Blocks per tick under the canopy: two blocks a second.
	private static final double DESCENT_SPEED = 0.1;
	private static final double SOLITUDE_RADIUS = 96.0;
	private static final int COVER_SEARCH_RANGE = 8;
	private static final int COVER_SEARCH_TRIES = 32;
	private static final int MIN_COVER_SCORE = 2;
	private static final double SPOOK_DISTANCE = 2.5;
	private static final int GIVE_UP_TICKS = 300;

	public final AnimationState idleAnimationState = new AnimationState();
	public final AnimationState hideAnimationState = new AnimationState();
	public final AnimationState parachuteAnimationState = new AnimationState();
	public final AnimationState deployAnimationState = new AnimationState();
	public final AnimationState landAnimationState = new AnimationState();

	public BarsikEntity(EntityType<? extends BarsikEntity> type, World world) {
		super(type, world);
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
	}

	/** Natural spawns: lit, open-sky grass, and only one Barsik per neighbourhood. */
	public static boolean canSpawnHere(EntityType<BarsikEntity> type, ServerWorldAccess world,
			SpawnReason reason, BlockPos pos, Random random) {
		if (!world.getBlockState(pos.down()).isIn(BlockTags.ANIMALS_SPAWNABLE_ON)
				|| world.getBaseLightLevel(pos, 0) <= 8
				|| !world.isSkyVisible(pos)) {
			return false;
		}
		return world.getEntitiesByClass(BarsikEntity.class, new Box(pos).expand(SOLITUDE_RADIUS), e -> true)
				.isEmpty();
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(PARACHUTING, false);
		builder.add(HIDING, false);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(1, new HideGoal());
		goalSelector.add(2, new FleeEntityGoal<>(this, PlayerEntity.class, 6.0f, 1.0, 1.4));
		goalSelector.add(3, new WanderAroundFarGoal(this, 0.8));
		goalSelector.add(4, new LookAroundGoal(this));
	}

	public boolean isParachuting() {
		return dataTracker.get(PARACHUTING);
	}

	public boolean isHiding() {
		return dataTracker.get(HIDING);
	}

	/** Nothing hurts him except what removes any entity: the void and /kill. */
	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		return !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY);
	}

	@Override
	public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
		return false;
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);
		if (PARACHUTING.equals(data) && getWorld().isClient && isParachuting()) {
			deployAnimationState.start(age);
		}
	}

	@Override
	public void handleStatus(byte status) {
		if (status == LAND_STATUS) {
			landAnimationState.start(age);
		} else {
			super.handleStatus(status);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			idleAnimationState.startIfNotRunning(age);
			parachuteAnimationState.setRunning(isParachuting(), age);
			hideAnimationState.setRunning(isHiding(), age);
		}
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		if (isParachuting()) {
			Vec3d velocity = getVelocity();
			if (velocity.y < -DESCENT_SPEED) {
				setVelocity(velocity.x, -DESCENT_SPEED, velocity.z);
			}
		}
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		boolean airborne = !isOnGround() && !isTouchingWater();
		if (!isParachuting()) {
			if (airborne && fallDistance > OPEN_PARACHUTE_AFTER_BLOCKS) {
				dataTracker.set(PARACHUTING, true);
				dataTracker.set(HIDING, false);
				// The canopy snapping open.
				playSound(SoundEvents.ENTITY_PHANTOM_FLAP, 1.0f, 0.6f);
			}
		} else if (!airborne) {
			dataTracker.set(PARACHUTING, false);
			getWorld().sendEntityStatus(this, LAND_STATUS);
			playSound(SoundEvents.BLOCK_WOOL_FALL, 1.0f, 0.8f);
		}
	}

	/** The canopy reaches far above the hitbox and must not be culled while the cat is at the screen's edge. */
	@Override
	public Box getVisibilityBoundingBox() {
		Box box = super.getVisibilityBoundingBox();
		return isParachuting() ? box.stretch(0.0, 2.5, 0.0).expand(0.8, 0.0, 0.8) : box;
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return isHiding() ? SoundEvents.ENTITY_CAT_PURR : SoundEvents.ENTITY_CAT_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_CAT_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_CAT_DEATH;
	}

	/** How sheltered a spot is: a point per wall around it, two more for a roof. */
	private int coverScore(BlockPos pos) {
		World world = getWorld();
		int score = world.isSkyVisible(pos) ? 0 : 2;
		for (Direction direction : Direction.Type.HORIZONTAL) {
			BlockPos side = pos.offset(direction);
			if (world.getBlockState(side).isSolidBlock(world, side)) {
				score++;
			}
		}
		return score;
	}

	private BlockPos findCover() {
		World world = getWorld();
		PlayerEntity player = world.getClosestPlayer(this, 16.0);
		BlockPos origin = getBlockPos();
		BlockPos best = null;
		int bestScore = MIN_COVER_SCORE - 1;
		for (int i = 0; i < COVER_SEARCH_TRIES; i++) {
			BlockPos pos = origin.add(random.nextInt(2 * COVER_SEARCH_RANGE + 1) - COVER_SEARCH_RANGE,
					random.nextInt(5) - 2, random.nextInt(2 * COVER_SEARCH_RANGE + 1) - COVER_SEARCH_RANGE);
			BlockPos floor = pos.down();
			if (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()
					|| !world.getBlockState(floor).isSolidBlock(world, floor)
					|| player != null && player.squaredDistanceTo(Vec3d.ofBottomCenter(pos)) < 25.0) {
				continue;
			}
			int score = coverScore(pos);
			if (score > bestScore) {
				best = pos;
				bestScore = score;
			}
		}
		return best;
	}

	/** Runs to the most sheltered spot nearby and loafs there until a player gets too close. */
	private class HideGoal extends Goal {
		private BlockPos spot;
		private int searchCooldown;
		private int repathTicks;
		private int travelTicks;
		private boolean spooked;

		HideGoal() {
			setControls(EnumSet.of(Control.MOVE, Control.JUMP));
		}

		@Override
		public boolean canStart() {
			if (isParachuting() || searchCooldown-- > 0) {
				return false;
			}
			searchCooldown = 10;
			spot = findCover();
			return spot != null;
		}

		@Override
		public boolean shouldContinue() {
			return spot != null && !spooked && !isParachuting() && travelTicks < GIVE_UP_TICKS;
		}

		@Override
		public void start() {
			repathTicks = 0;
			travelTicks = 0;
			spooked = false;
		}

		@Override
		public void stop() {
			spot = null;
			dataTracker.set(HIDING, false);
			getNavigation().stop();
			// Leaves time to run away before he looks for the next spot.
			searchCooldown = 30;
		}

		@Override
		public boolean shouldRunEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			Vec3d target = Vec3d.ofBottomCenter(spot);
			if (squaredDistanceTo(target) < 1.0) {
				if (!isHiding()) {
					getNavigation().stop();
					dataTracker.set(HIDING, true);
				}
				PlayerEntity player = getWorld().getClosestPlayer(BarsikEntity.this, SPOOK_DISTANCE);
				if (player != null && !player.isSpectator()) {
					playSound(SoundEvents.ENTITY_CAT_HISS, 1.0f, 0.8f);
					spooked = true;
				}
				return;
			}
			dataTracker.set(HIDING, false);
			travelTicks++;
			if (repathTicks-- <= 0) {
				repathTicks = 20;
				getNavigation().startMovingTo(target.x, target.y, target.z, 1.3);
			}
		}
	}
}
