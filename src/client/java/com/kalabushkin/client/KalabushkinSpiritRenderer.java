package com.kalabushkin.client;

import com.kalabushkin.Kalabushkin;
import com.kalabushkin.entity.KalabushkinSpiritEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class KalabushkinSpiritRenderer extends MobEntityRenderer<KalabushkinSpiritEntity, KalabushkinSpiritModel> {
	private static final Identifier TEXTURE = Kalabushkin.id("textures/entity/kalabushkin_spirit.png");

	public KalabushkinSpiritRenderer(EntityRendererFactory.Context context) {
		super(context, new KalabushkinSpiritModel(context.getPart(KalabushkinSpiritModel.LAYER)), 0.0f);
	}

	@Override
	public Identifier getTexture(KalabushkinSpiritEntity entity) {
		return TEXTURE;
	}

	@Override
	protected RenderLayer getRenderLayer(KalabushkinSpiritEntity entity, boolean showBody, boolean translucent,
			boolean showOutline) {
		return RenderLayer.getEntityTranslucent(TEXTURE);
	}

	// The spirit glows: always rendered at full brightness.
	@Override
	protected int getBlockLight(KalabushkinSpiritEntity entity, BlockPos pos) {
		return 15;
	}
}
