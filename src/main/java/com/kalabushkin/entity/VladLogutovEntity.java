package com.kalabushkin.entity;

import com.kalabushkin.ModEntities;
import java.util.EnumSet;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.EntityData;
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
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** Rides his bike around; on spotting a player he rides up, brakes, puts a foot down and asks his question. */
public class VladLogutovEntity extends PathAwareEntity {
	private static final TrackedData<Boolean> STOPPED =
			DataTracker.registerData(VladLogutovEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final byte ASK_STATUS = 89;
	// Long enough for the whole ask animation (4.5 s).
	private static final int ASK_TICKS = 100;
	// Also applied when he gives up the chase, so he is not glued to the player.
	private static final int ASK_COOLDOWN_TICKS = 300;
	private static final int CHASE_TICKS = 300;
	private static final double SIGHT_RANGE = 16.0;
	private static final double ASK_RANGE = 3.0;
	private static final double GIVE_UP_RANGE = 28.0;
	private static final double CHASE_SPEED = 1.3;
	private static final double SOLITUDE_RADIUS = 96.0;
	private static final float STOP_STEP = 0.125f;

	public final AnimationState idleAnimationState = new AnimationState();
	public final AnimationState askAnimationState = new AnimationState();

	// The player he is riding up to or, once askTicks is running, talking to.
	private PlayerEntity target;
	private int chaseTicks;
	private int askTicks;
	private int askCooldown;

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

	/** Little Koch comes along whenever Vlad appears in the world on his own or from a spawn egg. */
	@Override
	public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
			@Nullable EntityData entityData) {
		if (spawnReason == SpawnReason.NATURAL || spawnReason == SpawnReason.CHUNK_GENERATION
				|| spawnReason == SpawnReason.SPAWN_EGG) {
			LittleKochEntity koch = ModEntities.LITTLE_KOCH.create(world.toServerWorld());
			if (koch != null) {
				koch.refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(), 0.0f);
				world.spawnEntityAndPassengers(koch);
			}
		}
		return super.initialize(world, difficulty, spawnReason, entityData);
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
		if (target != null) {
			if (!target.isAlive() || target.isSpectator() || target.getWorld() != getWorld()
					|| squaredDistanceTo(target) > GIVE_UP_RANGE * GIVE_UP_RANGE) {
				rideOn();
			} else if (askTicks > 0) {
				if (--askTicks == 0) {
					rideOn();
				}
			} else if (squaredDistanceTo(target) <= ASK_RANGE * ASK_RANGE && canSee(target)) {
				ask();
			} else if (++chaseTicks > CHASE_TICKS) {
				rideOn();
			}
		} else if (askCooldown > 0) {
			askCooldown--;
		} else if (age % 10 == 0) {
			PlayerEntity player = getWorld().getClosestPlayer(this, SIGHT_RANGE);
			if (player != null && canSee(player)) {
				target = player;
				chaseTicks = 0;
			}
		}
	}

	private void ask() {
		askTicks = ASK_TICKS;
		dataTracker.set(STOPPED, true);
		getNavigation().stop();
		getWorld().sendEntityStatus(this, ASK_STATUS);
		// Bike bell.
		playSound(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 1.0f, 1.8f);
		// Also shown above his head by the renderer while STOPPED is set.
		target.sendMessage(Text.translatable("chat.type.text", getDisplayName(),
				Text.translatable("entity.kalabushkin.vlad_logutov.ask")), false);
	}

	private void rideOn() {
		target = null;
		askTicks = 0;
		askCooldown = ASK_COOLDOWN_TICKS;
		dataTracker.set(STOPPED, false);
		getNavigation().stop();
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

	/** Rides up to the player he has spotted, then holds the bike still and keeps his eyes on them. */
	private class AskGoal extends Goal {
		AskGoal() {
			setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
		}

		@Override
		public boolean canStart() {
			return target != null;
		}

		@Override
		public void stop() {
			getNavigation().stop();
		}

		@Override
		public boolean shouldRunEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			if (target == null) {
				return;
			}
			getLookControl().lookAt(target, 30.0f, 30.0f);
			if (askTicks > 0) {
				getNavigation().stop();
			} else if (chaseTicks % 10 == 0) {
				getNavigation().startMovingTo(target, CHASE_SPEED);
			}
		}
	}
}
