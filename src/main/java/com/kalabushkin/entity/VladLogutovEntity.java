package com.kalabushkin.entity;

import java.util.EnumSet;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
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
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

/** Rides his bike around; on spotting a player he brakes, puts a foot down and asks his question. */
public class VladLogutovEntity extends PathAwareEntity {
	private static final TrackedData<Boolean> STOPPED =
			DataTracker.registerData(VladLogutovEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final byte ASK_STATUS = 89;
	// Long enough for the whole ask animation (4.5 s).
	private static final int ASK_TICKS = 100;
	private static final int ASK_COOLDOWN_TICKS = 400;
	private static final double SIGHT_RANGE = 8.0;
	private static final double GIVE_UP_RANGE = 16.0;
	private static final double SOLITUDE_RADIUS = 96.0;
	private static final float STOP_STEP = 0.125f;

	public final AnimationState idleAnimationState = new AnimationState();
	public final AnimationState askAnimationState = new AnimationState();

	private int askTicks;
	private int askCooldown;
	private PlayerEntity asked;

	// Client-side blend between the riding pose (0) and the foot-down pose (1).
	private float stopProgress;
	private float prevStopProgress;

	public VladLogutovEntity(EntityType<? extends VladLogutovEntity> type, World world) {
		super(type, world);
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3);
	}

	/** Natural spawns: lit, open-sky grass, and only one Vlad per neighbourhood. */
	public static boolean canSpawnHere(EntityType<VladLogutovEntity> type, ServerWorldAccess world,
			SpawnReason reason, BlockPos pos, Random random) {
		if (!world.getBlockState(pos.down()).isIn(BlockTags.ANIMALS_SPAWNABLE_ON)
				|| world.getBaseLightLevel(pos, 0) <= 8
				|| !world.isSkyVisible(pos)) {
			return false;
		}
		return world.getEntitiesByClass(VladLogutovEntity.class, new Box(pos).expand(SOLITUDE_RADIUS), e -> true)
				.isEmpty();
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(STOPPED, false);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(1, new AskGoal());
		goalSelector.add(2, new EscapeDangerGoal(this, 1.4));
		goalSelector.add(3, new WanderAroundFarGoal(this, 1.0));
		goalSelector.add(4, new LookAroundGoal(this));
	}

	public boolean isStopped() {
		return dataTracker.get(STOPPED);
	}

	public float getStopProgress(float tickDelta) {
		return MathHelper.lerp(tickDelta, prevStopProgress, stopProgress);
	}

	@Override
	public void handleStatus(byte status) {
		if (status == ASK_STATUS) {
			askAnimationState.start(age);
		} else {
			super.handleStatus(status);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			idleAnimationState.startIfNotRunning(age);
			prevStopProgress = stopProgress;
			stopProgress = MathHelper.clamp(stopProgress + (isStopped() ? STOP_STEP : -STOP_STEP), 0.0f, 1.0f);
		}
	}

	@Override
	protected void mobTick() {
		super.mobTick();
		if (askTicks > 0) {
			askTicks--;
			if (askTicks == 0 || asked == null || !asked.isAlive()
					|| squaredDistanceTo(asked) > GIVE_UP_RANGE * GIVE_UP_RANGE) {
				rideOn();
			}
		} else if (askCooldown > 0) {
			askCooldown--;
		} else if (age % 10 == 0) {
			PlayerEntity player = getWorld().getClosestPlayer(this, SIGHT_RANGE);
			if (player != null && canSee(player)) {
				ask(player);
			}
		}
	}

	private void ask(PlayerEntity player) {
		askTicks = ASK_TICKS;
		asked = player;
		dataTracker.set(STOPPED, true);
		getNavigation().stop();
		getWorld().sendEntityStatus(this, ASK_STATUS);
		// Bike bell.
		playSound(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.0f, 1.8f);
		player.sendMessage(Text.translatable("chat.type.text", getDisplayName(),
				Text.translatable("entity.kalabushkin.vlad_logutov.ask")), false);
	}

	private void rideOn() {
		askTicks = 0;
		asked = null;
		askCooldown = ASK_COOLDOWN_TICKS;
		dataTracker.set(STOPPED, false);
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

	/** Holds the bike still and keeps his eyes on the player while he asks. */
	private class AskGoal extends Goal {
		AskGoal() {
			setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
		}

		@Override
		public boolean canStart() {
			return askTicks > 0 && asked != null;
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
			if (asked != null) {
				getLookControl().lookAt(asked, 30.0f, 30.0f);
			}
		}
	}
}
