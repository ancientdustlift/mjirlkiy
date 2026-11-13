package com.kalabushkin.client;

import com.kalabushkin.Kalabushkin;
import com.kalabushkin.entity.VasilyEntity;
import com.kalabushkin.item.NicotineItem;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class VasilyRenderer extends MobEntityRenderer<VasilyEntity, VasilyModel> {
	private static final Identifier TEXTURE = Kalabushkin.id("textures/entity/vasily.png");
	private static final double OFFER_LABEL_RANGE = 8.0;

	public VasilyRenderer(EntityRendererFactory.Context context) {
		super(context, new VasilyModel(context.getPart(VasilyModel.LAYER)), 0.5f);
		addFeature(new HeldItemFeatureRenderer<>(this, context.getHeldItemRenderer()));
	}

	/** What he is offering floats above his head for anyone standing close enough to be offered it. */
	@Override
	public void render(VasilyEntity entity, float yaw, float tickDelta, MatrixStack matrices,
			VertexConsumerProvider vertexConsumers, int light) {
		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
		NicotineItem.Kind offer = entity.getOffer();
		if (offer != null && dispatcher.getSquaredDistanceToCamera(entity) < OFFER_LABEL_RANGE * OFFER_LABEL_RANGE) {
			matrices.push();
			// Clear of his own name tag, if he has been given one.
			matrices.translate(0.0f, 0.3f, 0.0f);
			renderLabelIfPresent(entity, Text.translatable("entity.kalabushkin.vasily.offer." + offer.id()), matrices,
					vertexConsumers, light, tickDelta);
			matrices.pop();
		}
	}

	@Override
	public Identifier getTexture(VasilyEntity entity) {
		return TEXTURE;
	}
}
