package com.kalabushkin.client;

import com.kalabushkin.Kalabushkin;
import com.kalabushkin.entity.VladLogutovEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class VladLogutovRenderer extends MobEntityRenderer<VladLogutovEntity, VladLogutovModel> {
	private static final Identifier TEXTURE = Kalabushkin.id("textures/entity/vlad_logutov.png");
	private static final Text QUESTION = Text.translatable("entity.kalabushkin.vlad_logutov.ask");

	public VladLogutovRenderer(EntityRendererFactory.Context context) {
		super(context, new VladLogutovModel(context.getPart(VladLogutovModel.LAYER)), 0.7f);
	}

	/** His question floats above his head, like a name tag, for as long as he is stopped to ask it. */
	@Override
	public void render(VladLogutovEntity entity, float yaw, float tickDelta, MatrixStack matrices,
			VertexConsumerProvider vertexConsumers, int light) {
		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
		if (entity.isStopped()) {
			matrices.push();
			// Clear of his own name tag, if he has been given one.
			matrices.translate(0.0f, 0.3f, 0.0f);
			renderLabelIfPresent(entity, QUESTION, matrices, vertexConsumers, light, tickDelta);
			matrices.pop();
		}
	}

	@Override
	public Identifier getTexture(VladLogutovEntity entity) {
		return TEXTURE;
	}
}
