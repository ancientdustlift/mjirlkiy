package com.kalabushkin.entity;

import com.kalabushkin.ModEntities;
import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
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
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** Strolls around with Rita and, every so often, stops to pay her a compliment. */
public class AlexeyEntity extends PathAwareEntity {
	public static final int PHRASE_COUNT = 7;
	private static final int NO_PHRASE = -1;
	private static final TrackedData<Integer> PHRASE =
			DataTracker.registerData(AlexeyEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final int SPEAK_TICKS = 80;
	private static final int FIRST_PHRASE_DELAY = 100;
	private static final int MIN_PHRASE_DELAY = 240;
	private static final int PHRASE_DELAY_SPREAD = 240;
	private static final double RITA_RANGE = 6.0;
	private static final double CHAT_RANGE = 16.0;
	private static final double SOLITUDE_RADIUS = 96.0;

	public final AnimationState idleAnimationState = new AnimationState();
	public final AnimationState complimentAnimationState = new AnimationState();

	private RitaEntity listener;
	private int speakTicks;
	private int speakCooldown = FIRST_PHRASE_DELAY;
	private int lastPhrase = NO_PHRASE;

	public AlexeyEntity(EntityType<? extends AlexeyEntity> type, World world) {
		super(type, world);
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
	}

	/** Natural spawns: lit, open-sky grass, and only one Alexey per neighbourhood. */
	public static boolean canSpawnHere(EntityType<AlexeyEntity> type, ServerWorldAccess world,
			SpawnReason reason, BlockPos pos, Random random) {
		if (!world.getBlockState(pos.down()).isIn(BlockTags.ANIMALS_SPAWNABLE_ON)
				|| world.getBaseLightLevel(pos, 0) <= 8
				|| !world.isSkyVisible(pos)) {
			return false;
		}
		return world.getEntitiesByClass(AlexeyEntity.class, new Box(pos).expand(SOLITUDE_RADIUS), e -> true)
				.isEmpty();
	}

	/** Rita comes along whenever Alexey appears in the world on his own or from a spawn egg. */
	@Override
	public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
			@Nullable EntityData entityData) {
		if (spawnReason == SpawnReason.NATURAL || spawnReason == SpawnReason.CHUNK_GENERATION
				|| spawnReason == SpawnReason.SPAWN_EGG) {
			RitaEntity rita = ModEntities.RITA.create(world.toServerWorld());
			if (rita != null) {
				rita.refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(), 0.0f);
				world.spawnEntityAndPassengers(rita);
			}
		}
		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(PHRASE, NO_PHRASE);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(1, new EscapeDangerGoal(this, 1.4));
		goalSelector.add(2, new ComplimentGoal());
		// Slow enough for Rita to stay at his side.
		goalSelector.add(3, new WanderAroundFarGoal(this, 0.6));
		goalSelector.add(4, new LookAtEntityGoal(this, RitaEntity.class, 6.0f));
		goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
		goalSelector.add(6, new LookAroundGoal(this));
	}

	/** Index of the phrase he is saying right now, or a negative number when he is quiet. */
	public int getPhrase() {
		return dataTracker.get(PHRASE);
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);
		if (PHRASE.equals(data) && getWorld().isClient && getPhrase() >= 0) {
			complimentAnimationState.start(age);
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
		if (speakTicks > 0) {
			if (--speakTicks == 0 || listener == null || !listener.isAlive()) {
				speakTicks = 0;
				listener = null;
				dataTracker.set(PHRASE, NO_PHRASE);
				speakCooldown = MIN_PHRASE_DELAY + random.nextInt(PHRASE_DELAY_SPREAD);
			}
		} else if (--speakCooldown <= 0) {
			RitaEntity rita = getWorld().getEntitiesByClass(RitaEntity.class, getBoundingBox().expand(RITA_RANGE),
							Entity::isAlive).stream()
					.min(Comparator.comparingDouble(this::squaredDistanceTo))
					.orElse(null);
			if (rita == null) {
				// Nobody to say it to; look again in a couple of seconds.
				speakCooldown = 40;
			} else {
				speakTo(rita);
			}
		}
	}

	private void speakTo(RitaEntity rita) {
		int phrase;
		if (lastPhrase == NO_PHRASE) {
			phrase = random.nextInt(PHRASE_COUNT);
		} else {
			// Any phrase but the one he said last time.
			phrase = random.nextInt(PHRASE_COUNT - 1);
			if (phrase >= lastPhrase) {
				phrase++;
			}
		}
		lastPhrase = phrase;
		listener = rita;
		speakTicks = SPEAK_TICKS;
		dataTracker.set(PHRASE, phrase);
		rita.listenTo(this, SPEAK_TICKS);
		// Also shown above his head by the renderer while PHRASE is set.
		if (getWorld() instanceof ServerWorld world) {
			Text line = Text.translatable("chat.type.text", getDisplayName(),
					Text.translatable("entity.kalabushkin.alexey_srpava.phrase." + phrase));
			world.getPlayers(player -> player.squaredDistanceTo(this) < CHAT_RANGE * CHAT_RANGE)
					.forEach(player -> player.sendMessage(line, false));
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

	/** Stops and turns to Rita for as long as he is speaking. */
	private class ComplimentGoal extends Goal {
		ComplimentGoal() {
			setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
		}

		@Override
		public boolean canStart() {
			return speakTicks > 0 && listener != null;
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
			if (listener != null) {
				getLookControl().lookAt(listener, 30.0f, 30.0f);
				lookAtEntity(listener, 30.0f, 30.0f);
			}
		}
	}
}
