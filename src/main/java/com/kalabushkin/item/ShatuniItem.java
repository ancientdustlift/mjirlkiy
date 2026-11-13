package com.kalabushkin.item;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * A bike crankset used as a two-handed weapon. Besides ordinary hits, holding use with an empty
 * off hand spins it like a circular saw and keeps cutting whatever is in front of the wielder.
 */
public class ShatuniItem extends Item {
	private static final int DURABILITY = 500;
	private static final float SAW_DAMAGE = 6.0f;
	private static final double SAW_REACH = 2.5;
	// A hit landing inside a mob's 10-tick invulnerability window would be ignored.
	private static final int SAW_HIT_INTERVAL = 10;
	private static final int MAX_USE_TICKS = 72000;
	// Cosine of the half-angle of the cone in front of the wielder that the saw reaches.
	private static final double SAW_CONE = 0.5;

	public ShatuniItem(Settings settings) {
		// 6 attack damage in total: the player's base 1, iron's 2 and these 3.
		super(settings.maxDamage(DURABILITY).attributeModifiers(
				SwordItem.createAttributeModifiers(ToolMaterials.IRON, 3, -2.4f)));
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (hand != Hand.MAIN_HAND || !user.getOffHandStack().isEmpty()) {
			if (!world.isClient) {
				user.sendMessage(Text.translatable(getTranslationKey() + ".two_handed"), true);
			}
			return TypedActionResult.fail(stack);
		}
		user.setCurrentHand(hand);
		return TypedActionResult.consume(stack);
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.NONE;
	}

	@Override
	public int getMaxUseTime(ItemStack stack, LivingEntity user) {
		return MAX_USE_TICKS;
	}

	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		if (!(world instanceof ServerWorld serverWorld)) {
			return;
		}
		if (!user.getOffHandStack().isEmpty()) {
			user.stopUsingItem();
			return;
		}
		int usedTicks = MAX_USE_TICKS - remainingUseTicks;
		if (usedTicks % SAW_HIT_INTERVAL != SAW_HIT_INTERVAL / 2) {
			return;
		}
		world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLOCK_GRINDSTONE_USE,
				SoundCategory.PLAYERS, 0.5f, 1.7f);
		Vec3d eyes = user.getEyePos();
		Vec3d look = user.getRotationVec(1.0f);
		DamageSource source = user instanceof PlayerEntity player
				? user.getDamageSources().playerAttack(player)
				: user.getDamageSources().mobAttack(user);
		List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class,
				user.getBoundingBox().expand(SAW_REACH + 1.0), target -> target != user && canSaw(user, target));
		for (LivingEntity target : targets) {
			Vec3d toTarget = target.getBoundingBox().getCenter().subtract(eyes);
			double reach = SAW_REACH + target.getWidth() / 2.0;
			if (toTarget.lengthSquared() > reach * reach || toTarget.normalize().dotProduct(look) < SAW_CONE
					|| !user.canSee(target)) {
				continue;
			}
			if (target.damage(source, SAW_DAMAGE)) {
				serverWorld.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getBodyY(0.6), target.getZ(),
						10, 0.2, 0.2, 0.2, 0.3);
				world.playSound(null, target.getX(), target.getY(), target.getZ(),
						SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.7f, 1.6f);
				stack.damage(1, user, EquipmentSlot.MAINHAND);
				if (stack.isEmpty()) {
					return;
				}
			}
		}
	}

	private static boolean canSaw(LivingEntity user, LivingEntity target) {
		return target.isAlive() && !target.isSpectator() && !user.isTeammate(target)
				&& !(target instanceof Tameable pet && pet.getOwner() == user);
	}

	@Override
	public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
		return !miner.isCreative();
	}

	@Override
	public int getEnchantability() {
		return 14;
	}

	@Override
	public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		return true;
	}

	@Override
	public void postDamageEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		stack.damage(1, attacker, EquipmentSlot.MAINHAND);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable(getTranslationKey() + ".tooltip").formatted(Formatting.GRAY, Formatting.ITALIC));
	}
}
