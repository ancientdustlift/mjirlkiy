package com.kalabushkin;

import com.kalabushkin.network.FistBumpPayload;
import com.kalabushkin.spirit.SpiritManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.util.Identifier;

public class Kalabushkin implements ModInitializer {
	public static final String MOD_ID = "kalabushkin";

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModEntities.init();
		ModItems.init();
		SpiritManager.init();
		PayloadTypeRegistry.playS2C().register(FistBumpPayload.ID, FistBumpPayload.CODEC);
	}
}
