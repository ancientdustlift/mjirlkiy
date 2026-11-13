package com.kalabushkin.client;

import com.kalabushkin.Kalabushkin;
import com.kalabushkin.entity.VladLogutovEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class VladLogutovRenderer extends MobEntityRenderer<VladLogutovEntity, VladLogutovModel> {
	private static final Identifier TEXTURE = Kalabushkin.id("textures/entity/vlad_logutov.png");

	public VladLogutovRenderer(EntityRendererFactory.Context context) {
		super(context, new VladLogutovModel(context.getPart(VladLogutovModel.LAYER)), 0.7f);
	}

	@Override
	public Identifier getTexture(VladLogutovEntity entity) {
		return TEXTURE;
	}
}
