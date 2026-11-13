package com.kalabushkin.spirit;

import com.kalabushkin.entity.KalabushkinSpiritEntity;
import com.kalabushkin.network.FistBumpPayload;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/** While Kalabushkin's spirit follows a player, that player can fist-bump any mob with an empty hand. */
public final class FistBumps {
	private static final int COOLDOWN_TICKS = 15;
	private static final Map<MobEntity, Long> LAST_BUMP = new WeakHashMap<>();

	private FistBumps() {
	}

	/** Called for interactions the mob itself did not use. Returns true if the click was a fist bump. */
	public static boolean tryBump(MobEntity mob, PlayerEntity player, Hand hand) {
		if (hand != Hand.MAIN_HAND || !(player instanceof ServerPlayerEntity serverPlayer)
				|| !SpiritManager.isHaunted(serverPlayer) || !player.getMainHandStack().isEmpty()
				|| mob instanceof KalabushkinSpiritEntity || !mob.isAlive()) {
			return false;
		}
		ServerWorld world = serverPlayer.getServerWorld();
		Long last = LAST_BUMP.get(mob);
		if (last != null && world.getTime() - last < COOLDOWN_TICKS) {
			return true;
		}
		LAST_BUMP.put(mob, world.getTime());

		mob.getNavigation().stop();
		mob.getLookControl().lookAt(player, 60.0f, 60.0f);
		FistBumpPayload payload = new FistBumpPayload(mob.getId());
		PlayerLookup.tracking(mob).forEach(watcher -> ServerPlayNetworking.send(watcher, payload));

		Vec3d chest = new Vec3d(mob.getX(), mob.getBodyY(0.6), mob.getZ());
		Vec3d fist = player.getEyePos().add(0.0, -0.3, 0.0).lerp(chest, 0.5);
		world.spawnParticles(ParticleTypes.CRIT, fist.x, fist.y, fist.z, 8, 0.08, 0.08, 0.08, 0.15);
		world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, mob.getX(), mob.getBodyY(1.0) + 0.3, mob.getZ(),
				5, mob.getWidth() * 0.4, 0.15, mob.getWidth() * 0.4, 0.0);
		world.playSound(null, fist.x, fist.y, fist.z, SoundEvents.ENTITY_PLAYER_ATTACK_STRONG,
				SoundCategory.PLAYERS, 0.8f, 1.2f);
		return true;
	}
}
