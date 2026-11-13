import java.io.PrintWriter;
import java.util.Locale;

import com.kalabushkin.client.VladLogutovAnimations;
import com.kalabushkin.client.VladLogutovModel;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.AnimationHelper;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector3f;

/**
 * Poses the Vlad Logutov model with the real Minecraft model classes and writes its quads to a text
 * file, so tools/preview/render_preview.py can draw it without starting the game.
 * Line format: pose name, then one "x y z u v nx ny nz" line per vertex, four per quad.
 */
public final class ModelDump {
	private static PrintWriter out;

	public static void main(String[] args) throws Exception {
		VladLogutovModel model = new VladLogutovModel(VladLogutovModel.getTexturedModelData().createModel());
		try (PrintWriter writer = new PrintWriter(args[0])) {
			out = writer;
			for (int i = 0; i < 4; i++) {
				float limbAngle = 3.0f + i * 2.45f;
				model.pose(limbAngle, 0.0f, 0.0f, 0.0f);
				animate(model, VladLogutovAnimations.RIDE, (long) (limbAngle * 50.0f * 2.0f), 1.0f);
				dump("ride" + i, model);
			}
			model.pose(3.0f, 0.5f, 20.0f, 0.0f);
			dump("stopping", model);
			float[] askTimes = {0.3f, 1.0f, 1.6f, 3.5f};
			for (float time : askTimes) {
				model.pose(3.0f, 1.0f, 35.0f, -5.0f);
				animate(model, VladLogutovAnimations.ASK, (long) (time * 1000.0f), 1.0f);
				dump("ask" + time, model);
			}
		}
	}

	private static void animate(VladLogutovModel model, Animation animation, long timeMs, float scale) {
		AnimationHelper.animate(model, animation, timeMs, scale, new Vector3f());
	}

	private static void dump(String name, VladLogutovModel model) {
		out.println("pose " + name);
		model.getPart().render(new MatrixStack(), new VertexConsumer() {
			@Override
			public void vertex(float x, float y, float z, int color, float u, float v, int overlay, int light,
					float nx, float ny, float nz) {
				out.println(String.format(Locale.ROOT, "%f %f %f %f %f %f %f %f", x, y, z, u, v, nx, ny, nz));
			}

			@Override
			public VertexConsumer vertex(float x, float y, float z) {
				return this;
			}

			@Override
			public VertexConsumer color(int red, int green, int blue, int alpha) {
				return this;
			}

			@Override
			public VertexConsumer texture(float u, float v) {
				return this;
			}

			@Override
			public VertexConsumer overlay(int u, int v) {
				return this;
			}

			@Override
			public VertexConsumer light(int u, int v) {
				return this;
			}

			@Override
			public VertexConsumer normal(float x, float y, float z) {
				return this;
			}
		}, 0xF000F0, 0);
	}

	private ModelDump() {
	}
}
