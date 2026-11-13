package com.kalabushkin.item;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/** A carpenter's tool used as a melee weapon: the fast plane or the heavy wooden mallet. */
public class CarpenterWeaponItem extends Item {
	public enum Kind {
		PLANE(5, -1.6f, 320, Blocks.BIRCH_PLANKS, SoundEvents.ENTITY_SHEEP_SHEAR),
		MALLET(8, -3.2f, 384, Blocks.OAK_PLANKS, SoundEvents.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR);

		private final int damage;
		private final float attackSpeed;
		private final int durability;
		private final Block chips;
		private final SoundEvent hitSound;

		Kind(int damage, float attackSpeed, int durability, Block chips, SoundEvent hitSound) {
			this.damage = damage;
			this.attackSpeed = attackSpeed;
			this.durability = durability;
			this.chips = chips;
			this.hitSound = hitSound;
		}
	}

	private final Kind kind;

	public CarpenterWeaponItem(Kind kind, Settings settings) {
		// The modifiers are added to the player's base damage of 1.
		super(settings.maxDamage(kind.durability).attributeModifiers(
				SwordItem.createAttributeModifiers(ToolMaterials.WOOD, kind.damage - 1, kind.attackSpeed)));
		this.kind = kind;
	}

	@Override
	public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
		return !miner.isCreative();
	}

	@Override
	public int getEnchantability() {
		return 15;
	}

	@Override
	public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (target.getWorld() instanceof ServerWorld world) {
			boolean mallet = kind == Kind.MALLET;
			world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, kind.chips.getDefaultState()),
					target.getX(), target.getBodyY(0.6), target.getZ(), mallet ? 14 : 20, 0.2, 0.2, 0.2, 0.05);
			world.playSound(null, target.getX(), target.getY(), target.getZ(), kind.hitSound,
					SoundCategory.PLAYERS, 0.7f, mallet ? 0.7f : 1.5f);
			if (mallet) {
				// A heavy blow: throws the target back and leaves it dazed for a moment.
				float yaw = attacker.getYaw() * MathHelper.RADIANS_PER_DEGREE;
				target.takeKnockback(1.0, MathHelper.sin(yaw), -MathHelper.cos(yaw));
				target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 3), attacker);
			}
		}
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
