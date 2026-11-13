package com.kalabushkin.item;

import com.kalabushkin.ModEffects;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The things Vasily hands out. Each is used by holding the use button; finishing it leaves the user
 * with severe weakness, nausea and the trip effect.
 */
public class NicotineItem extends Item {
	public enum Kind {
		// Tucked under the lip: no smoke at all.
		SNUS("snus", 24, UseAction.EAT, 5, 400, null, 0, SoundEvents.ITEM_BUNDLE_REMOVE_ONE),
		VAPE("vape", 30, UseAction.TOOT_HORN, 8, 300, ParticleTypes.CLOUD, 24, SoundEvents.ENTITY_PLAYER_BREATH),
		CIGARETTE("cigarette", 40, UseAction.TOOT_HORN, 0, 400, ParticleTypes.SMOKE, 10,
				SoundEvents.ITEM_FLINTANDSTEEL_USE),
		// Harsher than a normal one: thick smoke, a coughing fit and twice as long to wear off.
		DYADKOVO_CIGARETTE("dyadkovo_cigarette", 50, UseAction.TOOT_HORN, 0, 800, ParticleTypes.LARGE_SMOKE, 14,
				SoundEvents.ENTITY_PANDA_SNEEZE);

		private final String id;
		private final int useTicks;
		private final UseAction action;
		// Number of uses for reusable kinds, 0 for kinds that are consumed.
		private final int uses;
		private final int effectTicks;
		private final ParticleEffect smoke;
		private final int smokeCount;
		private final SoundEvent sound;

		Kind(String id, int useTicks, UseAction action, int uses, int effectTicks, ParticleEffect smoke,
				int smokeCount, SoundEvent sound) {
			this.id = id;
			this.useTicks = useTicks;
			this.action = action;
			this.uses = uses;
			this.effectTicks = effectTicks;
			this.smoke = smoke;
			this.smokeCount = smokeCount;
			this.sound = sound;
		}

		public String id() {
			return id;
		}

		public SoundEvent sound() {
			return sound;
		}
	}

	private static final int SEVERE_WEAKNESS = 3;
	private static final int PUFF_INTERVAL = 10;

	private final Kind kind;

	public NicotineItem(Kind kind, Settings settings) {
		super(kind.uses > 0 ? settings.maxDamage(kind.uses) : settings.maxCount(20));
		this.kind = kind;
	}

	public Kind kind() {
		return kind;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		user.setCurrentHand(hand);
		return TypedActionResult.consume(user.getStackInHand(hand));
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return kind.action;
	}

	@Override
	public int getMaxUseTime(ItemStack stack, LivingEntity user) {
		return kind.useTicks;
	}

	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		// Vapour only comes out on the exhale, in finishUsing; cigarettes smoulder while held to the mouth.
		if (world instanceof ServerWorld serverWorld && kind.smoke != null && kind != Kind.VAPE
				&& remainingUseTicks % PUFF_INTERVAL == 0) {
			puff(serverWorld, user, 2, 0.01);
		}
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		if (world instanceof ServerWorld serverWorld) {
			if (kind.smoke != null) {
				puff(serverWorld, user, kind.smokeCount, 0.04);
			}
			world.playSound(null, user.getX(), user.getY(), user.getZ(), kind.sound, SoundCategory.PLAYERS, 0.8f,
					kind == Kind.DYADKOVO_CIGARETTE ? 0.7f : 1.0f);
			user.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, kind.effectTicks, SEVERE_WEAKNESS));
			user.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, kind.effectTicks, 0));
			user.addStatusEffect(new StatusEffectInstance(ModEffects.TRIP, kind.effectTicks, 0));
			if (kind == Kind.DYADKOVO_CIGARETTE) {
				user.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, kind.effectTicks / 2, 1));
			}
			if (kind.uses > 0) {
				stack.damage(1, user, LivingEntity.getSlotForHand(user.getActiveHand()));
			} else {
				stack.decrementUnlessCreative(1, user);
			}
		}
		return stack;
	}

	private void puff(ServerWorld world, LivingEntity user, int count, double speed) {
		Vec3d mouth = user.getEyePos().add(user.getRotationVec(1.0f).multiply(0.4)).add(0.0, -0.15, 0.0);
		world.spawnParticles(kind.smoke, mouth.x, mouth.y, mouth.z, count, 0.08, 0.08, 0.08, speed);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("item.kalabushkin.nicotine.tooltip").formatted(Formatting.GRAY, Formatting.ITALIC));
	}
}
