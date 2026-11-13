package com.kalabushkin.entity;

import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

/** Walks at Alexey's side and never makes a sound; when he speaks to her she stops and listens. */
public class RitaEntity extends PathAwareEntity {
	private static final byte LISTEN_STATUS = 93;
	private static final double SEARCH_RANGE = 32.0;
	private static final double START_FOLLOW_DISTANCE = 2.5;
	private static final double STOP_FOLLOW_DISTANCE = 1.5;
	private static final double FOLLOW_SPEED = 1.1;

	public final AnimationState idleAnimationState = new AnimationState();
	public final AnimationState listenAnimationState = new AnimationState();

	private AlexeyEntity speaker;
	private int listenTicks;

	public RitaEntity(EntityType<? extends RitaEntity> type, World world) {
		super(type, world);
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(1, new EscapeDangerGoal(this, 1.4));
		goalSelector.add(2, new ListenGoal());
		goalSelector.add(3, new FollowAlexeyGoal());
		goalSelector.add(4, new WanderAroundFarGoal(this, 0.6));
		goalSelector.add(5, new LookAtEntityGoal(this, AlexeyEntity.class, 6.0f));
		goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
		goalSelector.add(7, new LookAroundGoal(this));
	}

	/** Called by Alexey when he starts speaking to her. */
	public void listenTo(AlexeyEntity alexey, int ticks) {
		speaker = alexey;
		listenTicks = ticks;
		getWorld().sendEntityStatus(this, LISTEN_STATUS);
	}

	/** No voice, no hurt or death sounds, no footsteps. */
	@Override
	public boolean isSilent() {
		return true;
	}

	@Override
	public void handleStatus(byte status) {
		if (status == LISTEN_STATUS) {
			listenAnimationState.start(age);
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
		if (listenTicks > 0 && --listenTicks == 0) {
			speaker = null;
		}
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}

	private class ListenGoal extends Goal {
		ListenGoal() {
			setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
		}

		@Override
		public boolean canStart() {
			return listenTicks > 0 && speaker != null && speaker.isAlive();
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
			if (speaker != null) {
				getLookControl().lookAt(speaker, 30.0f, 30.0f);
				lookAtEntity(speaker, 30.0f, 30.0f);
			}
		}
	}

	private class FollowAlexeyGoal extends Goal {
		private AlexeyEntity alexey;
		private int repathTicks;
		private int searchCooldown;

		FollowAlexeyGoal() {
			setControls(EnumSet.of(Control.MOVE, Control.LOOK));
		}

		@Override
		public boolean canStart() {
			// A counter rather than the entity's age: goals are only checked on alternate ticks.
			if (searchCooldown-- > 0) {
				return false;
			}
			searchCooldown = 5;
			alexey = getWorld().getEntitiesByClass(AlexeyEntity.class, getBoundingBox().expand(SEARCH_RANGE),
							Entity::isAlive).stream()
					.min(Comparator.comparingDouble(RitaEntity.this::squaredDistanceTo))
					.orElse(null);
			return alexey != null && squaredDistanceTo(alexey) > START_FOLLOW_DISTANCE * START_FOLLOW_DISTANCE;
		}

		@Override
		public boolean shouldContinue() {
			return alexey != null && alexey.isAlive()
					&& squaredDistanceTo(alexey) > STOP_FOLLOW_DISTANCE * STOP_FOLLOW_DISTANCE
					&& squaredDistanceTo(alexey) < SEARCH_RANGE * SEARCH_RANGE * 4.0;
		}

		@Override
		public void start() {
			repathTicks = 0;
		}

		@Override
		public void stop() {
			alexey = null;
			getNavigation().stop();
		}

		@Override
		public boolean shouldRunEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			getLookControl().lookAt(alexey, 30.0f, 30.0f);
			if (repathTicks-- <= 0) {
				repathTicks = 10;
				getNavigation().startMovingTo(alexey, FOLLOW_SPEED);
			}
		}
	}
}
