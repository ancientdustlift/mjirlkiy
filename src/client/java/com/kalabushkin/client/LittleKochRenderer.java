package com.kalabushkin.client;

import com.kalabushkin.Kalabushkin;
import com.kalabushkin.entity.LittleKochEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class LittleKochRenderer extends MobEntityRenderer<LittleKochEntity, LittleKochModel> {
	private static final Identifier TEXTURE = Kalabushkin.id("textures/entity/little_koch.png");

	public LittleKochRenderer(EntityRendererFactory.Context context) {
		super(context, new LittleKochModel(context.getPart(LittleKochModel.LAYER)), 0.55f);
	}

	@Override
	public Identifier getTexture(LittleKochEntity entity) {
		return TEXTURE;
	}
}
