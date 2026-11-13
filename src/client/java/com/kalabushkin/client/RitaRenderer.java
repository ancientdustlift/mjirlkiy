package com.kalabushkin.client;

import com.kalabushkin.Kalabushkin;
import com.kalabushkin.entity.RitaEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RitaRenderer extends MobEntityRenderer<RitaEntity, RitaModel> {
	private static final Identifier TEXTURE = Kalabushkin.id("textures/entity/rita.png");

	public RitaRenderer(EntityRendererFactory.Context context) {
		super(context, new RitaModel(context.getPart(RitaModel.LAYER)), 0.4f);
	}

	@Override
	protected void scale(RitaEntity entity, MatrixStack matrices, float amount) {
		matrices.scale(RitaModel.SCALE, RitaModel.SCALE, RitaModel.SCALE);
	}

	@Override
	public Identifier getTexture(RitaEntity entity) {
		return TEXTURE;
	}
}
