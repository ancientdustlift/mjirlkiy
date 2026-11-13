package com.kalabushkin.client;

import com.kalabushkin.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class KalabushkinClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityModelLayerRegistry.registerModelLayer(KalabushkinModel.LAYER, KalabushkinModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(KalabushkinSpiritModel.LAYER, KalabushkinSpiritModel::getTexturedModelData);
		EntityRendererRegistry.register(ModEntities.KALABUSHKIN, KalabushkinRenderer::new);
		EntityRendererRegistry.register(ModEntities.SPIRIT, KalabushkinSpiritRenderer::new);
	}
}
