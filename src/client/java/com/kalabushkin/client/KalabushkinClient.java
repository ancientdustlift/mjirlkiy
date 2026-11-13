package com.kalabushkin.client;

import com.kalabushkin.Kalabushkin;
import com.kalabushkin.ModEntities;
import com.kalabushkin.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ClampedModelPredicateProvider;
import net.minecraft.client.item.ModelPredicateProviderRegistry;

public class KalabushkinClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityModelLayerRegistry.registerModelLayer(KalabushkinModel.LAYER, KalabushkinModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(KalabushkinSpiritModel.LAYER, KalabushkinSpiritModel::getTexturedModelData);
		EntityRendererRegistry.register(ModEntities.KALABUSHKIN, KalabushkinRenderer::new);
		EntityRendererRegistry.register(ModEntities.SPIRIT, KalabushkinSpiritRenderer::new);

		// Drives the weapons' swing animation: item model overrides pick a frame from this value.
		ClampedModelPredicateProvider swing = (stack, world, entity, seed) -> {
			if (entity == null || !entity.handSwinging || entity.getMainHandStack() != stack) {
				return 0.0f;
			}
			float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true);
			return Math.max(entity.getHandSwingProgress(tickDelta), 0.001f);
		};
		ModelPredicateProviderRegistry.register(ModItems.CARPENTER_PLANE, Kalabushkin.id("swing"), swing);
		ModelPredicateProviderRegistry.register(ModItems.WOODEN_MALLET, Kalabushkin.id("swing"), swing);
	}
}
