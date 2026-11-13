package com.kalabushkin.client;

import com.kalabushkin.Kalabushkin;
import com.kalabushkin.ModEntities;
import com.kalabushkin.ModItems;
import com.kalabushkin.network.FistBumpPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ClampedModelPredicateProvider;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.entity.Entity;

public class KalabushkinClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityModelLayerRegistry.registerModelLayer(KalabushkinModel.LAYER, KalabushkinModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(KalabushkinSpiritModel.LAYER, KalabushkinSpiritModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(VladLogutovModel.LAYER, VladLogutovModel::getTexturedModelData);
		EntityRendererRegistry.register(ModEntities.KALABUSHKIN, KalabushkinRenderer::new);
		EntityRendererRegistry.register(ModEntities.SPIRIT, KalabushkinSpiritRenderer::new);
		EntityRendererRegistry.register(ModEntities.VLAD_LOGUTOV, VladLogutovRenderer::new);

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

		ClientPlayNetworking.registerGlobalReceiver(FistBumpPayload.ID, (payload, context) -> {
			Entity entity = context.client().world == null ? null : context.client().world.getEntityById(payload.entityId());
			if (entity != null) {
				FistBumpAnimator.start(entity);
			}
		});
	}
}
