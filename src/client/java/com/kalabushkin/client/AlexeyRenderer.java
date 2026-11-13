package com.kalabushkin.client;

import com.kalabushkin.Kalabushkin;
import com.kalabushkin.entity.AlexeyEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class AlexeyRenderer extends MobEntityRenderer<AlexeyEntity, AlexeyModel> {
	private static final Identifier TEXTURE = Kalabushkin.id("textures/entity/alexey_srpava.png");

	public AlexeyRenderer(EntityRendererFactory.Context context) {
		super(context, new AlexeyModel(context.getPart(AlexeyModel.LAYER)), 0.45f);
	}

	/** The phrase he is saying to Rita floats above his head, like a name tag, while he says it. */
	@Override
	public void render(AlexeyEntity entity, float yaw, float tickDelta, MatrixStack matrices,
			VertexConsumerProvider vertexConsumers, int light) {
		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
		int phrase = entity.getPhrase();
		if (phrase >= 0 && phrase < AlexeyEntity.PHRASE_COUNT) {
			matrices.push();
			// Clear of his own name tag, if he has been given one.
			matrices.translate(0.0f, 0.3f, 0.0f);
			renderLabelIfPresent(entity, Text.translatable("entity.kalabushkin.alexey_srpava.phrase." + phrase),
					matrices, vertexConsumers, light, tickDelta);
			matrices.pop();
		}
	}

	@Override
	protected void scale(AlexeyEntity entity, MatrixStack matrices, float amount) {
		matrices.scale(AlexeyModel.SCALE, AlexeyModel.SCALE, AlexeyModel.SCALE);
	}

	@Override
	public Identifier getTexture(AlexeyEntity entity) {
		return TEXTURE;
	}
}
