package com.kalabushkin.client;

import com.kalabushkin.Kalabushkin;
import com.kalabushkin.entity.KalabushkinEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class KalabushkinRenderer extends MobEntityRenderer<KalabushkinEntity, KalabushkinModel> {
	private static final Identifier TEXTURE = Kalabushkin.id("textures/entity/kalabushkin.png");

	public KalabushkinRenderer(EntityRendererFactory.Context context) {
		super(context, new KalabushkinModel(context.getPart(KalabushkinModel.LAYER)), 0.5f);
	}

	@Override
	public Identifier getTexture(KalabushkinEntity entity) {
		return TEXTURE;
	}
}
