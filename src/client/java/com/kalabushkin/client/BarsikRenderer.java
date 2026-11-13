package com.kalabushkin.client;

import com.kalabushkin.Kalabushkin;
import com.kalabushkin.entity.BarsikEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class BarsikRenderer extends MobEntityRenderer<BarsikEntity, BarsikModel> {
	private static final Identifier TEXTURE = Kalabushkin.id("textures/entity/barsik.png");

	public BarsikRenderer(EntityRendererFactory.Context context) {
		super(context, new BarsikModel(context.getPart(BarsikModel.LAYER)), 0.55f);
	}

	@Override
	public Identifier getTexture(BarsikEntity entity) {
		return TEXTURE;
	}
}
