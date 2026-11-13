import java.io.PrintWriter;
import java.util.Locale;

import com.kalabushkin.client.BarsikAnimations;
import com.kalabushkin.client.BarsikModel;
import com.kalabushkin.client.LittleKochAnimations;
import com.kalabushkin.client.LittleKochModel;
import com.kalabushkin.client.VasilyAnimations;
import com.kalabushkin.client.VasilyModel;
import com.kalabushkin.client.VladLogutovAnimations;
import com.kalabushkin.client.VladLogutovModel;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.AnimationHelper;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector3f;

/**
 * Poses the Vlad Logutov, Little Koch, Vasily and Barsik models with the real Minecraft model classes and writes their quads to text
 * files, so tools/preview/render_preview.py can draw it without starting the game.
 * Line format: pose name, then one "x y z u v nx ny nz" line per vertex, four per quad.
 */
public final class ModelDump {
	private static PrintWriter out;

	public static void main(String[] args) throws Exception {
		LittleKochModel koch = new LittleKochModel(LittleKochModel.getTexturedModelData().createModel());
		try (PrintWriter writer = new PrintWriter(args[0] + "/little_koch.txt")) {
			out = writer;
			float[] idleTimes = {0.0f, 1.25f};
			for (float time : idleTimes) {
				koch.getPart().traverse().forEach(ModelPart::resetTransform);
				animate(koch, LittleKochAnimations.IDLE, (long) (time * 1000.0f), 1.0f);
				dump("idle" + time, koch);
			}
			float[] walkTimes = {0.0f, 0.25f, 0.5f};
			for (float time : walkTimes) {
				koch.getPart().traverse().forEach(ModelPart::resetTransform);
				animate(koch, LittleKochAnimations.WADDLE, (long) (time * 1000.0f), 1.0f);
				dump("waddle" + time, koch);
			}
		}

		BarsikModel barsik = new BarsikModel(BarsikModel.getTexturedModelData().createModel());
		try (PrintWriter writer = new PrintWriter(args[0] + "/barsik.txt")) {
			out = writer;
			barsik.pose(false, 0.0f, 0.0f);
			dump("stand", barsik);
			barsik.pose(false, 0.0f, 0.0f);
			animate(barsik, BarsikAnimations.WALK, 250L, 1.0f);
			dump("walk", barsik);
			barsik.pose(false, 0.0f, 0.0f);
			animate(barsik, BarsikAnimations.HIDE, 1000L, 1.0f);
			dump("hide", barsik);
			barsik.pose(false, 0.0f, 0.0f);
			animate(barsik, BarsikAnimations.HIDE, 2800L, 1.0f);
			dump("peek", barsik);
			barsik.pose(true, 0.0f, 0.0f);
			animate(barsik, BarsikAnimations.PARACHUTE, 0L, 1.0f);
			animate(barsik, BarsikAnimations.DEPLOY, 150L, 1.0f);
			dump("deploy", barsik);
			barsik.pose(true, 0.0f, 0.0f);
			animate(barsik, BarsikAnimations.PARACHUTE, 500L, 1.0f);
			dump("parachute", barsik);
			barsik.pose(false, 0.0f, 0.0f);
			animate(barsik, BarsikAnimations.LAND, 120L, 1.0f);
			dump("land", barsik);
		}

		VasilyModel vasily = new VasilyModel(VasilyModel.getTexturedModelData().createModel());
		try (PrintWriter writer = new PrintWriter(args[0] + "/vasily.txt")) {
			out = writer;
			vasily.basePose(0.0f, 0.0f, 0.0f);
			dump("stance", vasily);
			vasily.basePose(0.0f, 0.0f, 0.0f);
			animate(vasily, VasilyAnimations.SHAMBLE, 300L, 1.0f);
			dump("shamble", vasily);
			vasily.basePose(0.0f, 0.0f, 0.0f);
			animate(vasily, VasilyAnimations.HANDSHAKE, 600L, 1.0f);
			dump("handshake", vasily);
			String[] gives = {"snus", "vape", "cigarette", "dyadkovo"};
			long[] showTimes = {700L, 600L, 600L, 1100L};
			for (int i = 0; i < gives.length; i++) {
				vasily.basePose(0.0f, 0.0f, 0.0f);
				animate(vasily, VasilyAnimations.GIVE[i], showTimes[i], 1.0f);
				dump(gives[i], vasily);
			}
			vasily.basePose(0.0f, 0.0f, 0.0f);
			animate(vasily, VasilyAnimations.GIVE[0], 1700L, 1.0f);
			dump("handover", vasily);
		}

		VladLogutovModel model = new VladLogutovModel(VladLogutovModel.getTexturedModelData().createModel());
		try (PrintWriter writer = new PrintWriter(args[0] + "/vlad_logutov.txt")) {
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

	private static void animate(SinglePartEntityModel<?> model, Animation animation, long timeMs, float scale) {
		AnimationHelper.animate(model, animation, timeMs, scale, new Vector3f());
	}

	private static void dump(String name, SinglePartEntityModel<?> model) {
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
