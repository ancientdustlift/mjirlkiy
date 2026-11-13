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
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/** Short and stout; keeps close to the nearest Vlad Logutov and only wanders when there is none around. */
public class LittleKochEntity extends PathAwareEntity {
	private static final double SEARCH_RANGE = 32.0;
	private static final double START_FOLLOW_DISTANCE = 4.0;
	private static final double STOP_FOLLOW_DISTANCE = 2.5;
	// Fast enough to keep up with the bike when Vlad rides up to a player.
	private static final double FOLLOW_SPEED = 1.6;

	public final AnimationState idleAnimationState = new AnimationState();

	public LittleKochEntity(EntityType<? extends LittleKochEntity> type, World world) {
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
		goalSelector.add(1, new EscapeDangerGoal(this, 1.5));
		goalSelector.add(2, new FollowVladGoal());
		goalSelector.add(3, new WanderAroundFarGoal(this, 0.7));
		goalSelector.add(4, new LookAtEntityGoal(this, VladLogutovEntity.class, 8.0f));
		goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
		goalSelector.add(6, new LookAroundGoal(this));
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			idleAnimationState.startIfNotRunning(age);
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

	private class FollowVladGoal extends Goal {
		private VladLogutovEntity vlad;
		private int repathTicks;
		private int searchCooldown;

		FollowVladGoal() {
			setControls(EnumSet.of(Control.MOVE, Control.LOOK));
		}

		@Override
		public boolean canStart() {
			// Searching for entities on every check would be wasteful. A counter rather than the entity's
			// age, because goals are only checked on alternate ticks and could miss a fixed age forever.
			if (searchCooldown-- > 0) {
				return false;
			}
			searchCooldown = 5;
			vlad = getWorld().getEntitiesByClass(VladLogutovEntity.class, getBoundingBox().expand(SEARCH_RANGE),
							Entity::isAlive).stream()
					.min(Comparator.comparingDouble(LittleKochEntity.this::squaredDistanceTo))
					.orElse(null);
			return vlad != null && squaredDistanceTo(vlad) > START_FOLLOW_DISTANCE * START_FOLLOW_DISTANCE;
		}

		@Override
		public boolean shouldContinue() {
			return vlad != null && vlad.isAlive()
					&& squaredDistanceTo(vlad) > STOP_FOLLOW_DISTANCE * STOP_FOLLOW_DISTANCE
					&& squaredDistanceTo(vlad) < SEARCH_RANGE * SEARCH_RANGE * 4.0;
		}

		@Override
		public void start() {
			repathTicks = 0;
		}

		@Override
		public void stop() {
			vlad = null;
			getNavigation().stop();
		}

		@Override
		public boolean shouldRunEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			getLookControl().lookAt(vlad, 30.0f, 30.0f);
			if (repathTicks-- <= 0) {
				repathTicks = 10;
				getNavigation().startMovingTo(vlad, FOLLOW_SPEED);
			}
		}
	}
}
