package com.kalabushkin.entity;

import com.kalabushkin.ModItems;
import com.kalabushkin.item.NicotineItem;
import java.util.EnumSet;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
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
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

/**
 * Shambles around like a zombie with his arms out, holding whatever he is offering at the moment.
 * Interacting with him always starts with a handshake, after which he hands the offer over.
 */
public class VasilyEntity extends PathAwareEntity {
	private static final byte HANDSHAKE_STATUS = 90;
	// Followed by one status per NicotineItem.Kind, in ordinal order.
	private static final byte GIVE_STATUS = 91;
	private static final int HANDSHAKE_TICKS = 30;
	private static final int GIVE_TICKS = 44;
	// Ticks into the give animation at which he does his bit with the item, and at which he lets go of it.
	private static final int GIVE_FLOURISH_TICK = 14;
	private static final int GIVE_HANDOVER_TICK = 32;
	private static final int OFFER_CHANGE_TICKS = 100;
	private static final double APPROACH_RANGE = 10.0;
	private static final double STOP_DISTANCE = 2.5;
	private static final double SOLITUDE_RADIUS = 96.0;

	public final AnimationState idleAnimationState = new AnimationState();
	public final AnimationState handshakeAnimationState = new AnimationState();
	public final AnimationState[] giveAnimationStates = new AnimationState[NicotineItem.Kind.values().length];

	private PlayerEntity partner;
	private int handshakeTicks;
	private int giveTicks;
	private int offerTicks;

	public VasilyEntity(EntityType<? extends VasilyEntity> type, World world) {
		super(type, world);
		for (int i = 0; i < giveAnimationStates.length; i++) {
			giveAnimationStates[i] = new AnimationState();
		}
		setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2);
	}

	/** Natural spawns: lit, open-sky grass, and only one Vasily per neighbourhood. */
	public static boolean canSpawnHere(EntityType<VasilyEntity> type, ServerWorldAccess world,
			SpawnReason reason, BlockPos pos, Random random) {
		if (!world.getBlockState(pos.down()).isIn(BlockTags.ANIMALS_SPAWNABLE_ON)
				|| world.getBaseLightLevel(pos, 0) <= 8
				|| !world.isSkyVisible(pos)) {
			return false;
		}
		return world.getEntitiesByClass(VasilyEntity.class, new Box(pos).expand(SOLITUDE_RADIUS), e -> true)
				.isEmpty();
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(1, new DealGoal());
		goalSelector.add(2, new ApproachPlayerGoal());
		goalSelector.add(3, new WanderAroundFarGoal(this, 0.8));
		goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
		goalSelector.add(5, new LookAroundGoal(this));
	}

	/** What he is holding out, or null before the server has picked the first offer. */
	public NicotineItem.Kind getOffer() {
		return getMainHandStack().getItem() instanceof NicotineItem item ? item.kind() : null;
	}

	private void offer(NicotineItem.Kind kind) {
		equipStack(EquipmentSlot.MAINHAND, new ItemStack(ModItems.nicotine(kind)));
		offerTicks = OFFER_CHANGE_TICKS;
	}

	private void offerNext() {
		NicotineItem.Kind[] kinds = NicotineItem.Kind.values();
		NicotineItem.Kind current = getOffer();
		offer(current == null ? kinds[random.nextInt(kinds.length)] : kinds[(current.ordinal() + 1) % kinds.length]);
	}

	private boolean isBusy() {
		return handshakeTicks > 0 || giveTicks > 0;
	}

	@Override
	protected ActionResult interactMob(PlayerEntity player, Hand hand) {
		if (hand != Hand.MAIN_HAND || !isAlive()) {
			return super.interactMob(player, hand);
		}
		if (!getWorld().isClient && !isBusy() && getOffer() != null) {
			partner = player;
			handshakeTicks = HANDSHAKE_TICKS;
			getWorld().sendEntityStatus(this, HANDSHAKE_STATUS);
		}
		return ActionResult.success(getWorld().isClient);
	}

	@Override
	public void handleStatus(byte status) {
		int give = status - GIVE_STATUS;
		if (status == HANDSHAKE_STATUS) {
			handshakeAnimationState.start(age);
		} else if (give >= 0 && give < giveAnimationStates.length) {
			handshakeAnimationState.stop();
			giveAnimationStates[give].start(age);
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
		NicotineItem.Kind offer = getOffer();
		if (offer == null) {
			offerNext();
		} else if (handshakeTicks > 0) {
			if (--handshakeTicks == 0) {
				giveTicks = GIVE_TICKS;
				getWorld().sendEntityStatus(this, (byte) (GIVE_STATUS + offer.ordinal()));
			}
		} else if (giveTicks > 0) {
			giveTicks--;
			int elapsed = GIVE_TICKS - giveTicks;
			if (elapsed == GIVE_FLOURISH_TICK) {
				flourish(offer);
			} else if (elapsed == GIVE_HANDOVER_TICK) {
				handOver(offer);
			}
			if (giveTicks == 0) {
				partner = null;
				offerNext();
			}
		} else if (--offerTicks <= 0) {
			offerNext();
		}
	}

	/** The sound and smoke that go with his own little show for each item. */
	private void flourish(NicotineItem.Kind kind) {
		playSound(kind.sound(), 0.8f, kind == NicotineItem.Kind.DYADKOVO_CIGARETTE ? 0.7f : 1.0f);
		if (!(getWorld() instanceof ServerWorld world)) {
			return;
		}
		double y = getEyeY() - 0.1;
		switch (kind) {
			case VAPE -> world.spawnParticles(ParticleTypes.CLOUD, getX(), y, getZ(), 20, 0.15, 0.1, 0.15, 0.03);
			case CIGARETTE -> world.spawnParticles(ParticleTypes.SMOKE, getX(), y, getZ(), 8, 0.1, 0.1, 0.1, 0.01);
			case DYADKOVO_CIGARETTE ->
					world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), y, getZ(), 12, 0.15, 0.1, 0.15, 0.02);
			default -> {
			}
		}
	}

	private void handOver(NicotineItem.Kind kind) {
		if (partner == null || !partner.isAlive() || partner.getWorld() != getWorld()) {
			return;
		}
		ItemStack stack = new ItemStack(ModItems.nicotine(kind));
		if (!partner.giveItemStack(stack)) {
			partner.dropItem(stack, false);
		}
		playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.6f, 1.0f);
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_ZOMBIE_AMBIENT;
	}

	@Override
	public int getMinAmbientSoundDelay() {
		return 240;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_PLAYER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_PLAYER_DEATH;
	}

	/** Stands still and faces the player through the handshake and the hand-over. */
	private class DealGoal extends Goal {
		DealGoal() {
			setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
		}

		@Override
		public boolean canStart() {
			return isBusy() && partner != null;
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
			if (partner != null) {
				getLookControl().lookAt(partner, 30.0f, 30.0f);
				lookAtEntity(partner, 30.0f, 30.0f);
			}
		}
	}

	/** Shuffles up to the nearest player he can see and stops within arm's reach. */
	private class ApproachPlayerGoal extends Goal {
		private PlayerEntity target;
		private int searchCooldown;
		private int repathTicks;

		ApproachPlayerGoal() {
			setControls(EnumSet.of(Control.MOVE, Control.LOOK));
		}

		@Override
		public boolean canStart() {
			if (searchCooldown-- > 0) {
				return false;
			}
			searchCooldown = 10;
			target = getWorld().getClosestPlayer(VasilyEntity.this, APPROACH_RANGE);
			return target != null && !target.isSpectator() && canSee(target)
					&& squaredDistanceTo(target) > STOP_DISTANCE * STOP_DISTANCE;
		}

		@Override
		public boolean shouldContinue() {
			return target != null && target.isAlive() && !target.isSpectator()
					&& squaredDistanceTo(target) > STOP_DISTANCE * STOP_DISTANCE
					&& squaredDistanceTo(target) < APPROACH_RANGE * APPROACH_RANGE * 2.0;
		}

		@Override
		public void start() {
			repathTicks = 0;
		}

		@Override
		public void stop() {
			target = null;
			getNavigation().stop();
		}

		@Override
		public boolean shouldRunEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			getLookControl().lookAt(target, 30.0f, 30.0f);
			if (repathTicks-- <= 0) {
				repathTicks = 10;
				getNavigation().startMovingTo(target, 1.0);
			}
		}
	}
}
