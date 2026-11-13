package com.vladik.godbox.client;

import com.vladik.godbox.Power;
import com.vladik.godbox.WorldAges;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class WorldPanels {
   static final String[] TIME_LABEL = new String[]{"||", "x0.5", "x1", "x2", "x5"};
   static final String[] TIME_POWER = new String[]{"TIME_PAUSE", "TIME_SLOW", "TIME_NORMAL", "TIME_FAST", "TIME_FASTER"};
   static final float[] TIME_RATE = new float[]{0.0F, 10.0F, 20.0F, 40.0F, 100.0F};
   static final String[] AGE_POWER = new String[]{
      "AGE_HOPE", "AGE_SUN", "AGE_WONDERS", "AGE_CHAOS", "AGE_DARK", "AGE_MOON", "AGE_ICE", "AGE_ASH", "AGE_DESPAIR"
   };
   public static final int[] AGE_RGB = new int[]{5635925, 16777045, 16733695, 16733525, 9079434, 7244799, 5636095, 16755200, 11554009};
   static final int BW = 30;
   static final int BH = 14;
   static final int ERA_W = 190;
   static final int AGE_TOTAL = 1200;
   public static boolean popup;

   private WorldPanels() {
   }

   static int timeX(int width) {
      return width / 2 - (TIME_LABEL.length * 32 + 34) / 2;
   }

   static int active() {
      if (HudState.frozen) {
         return 0;
      } else {
         int best = 2;
         float bd = Float.MAX_VALUE;

         for (int i = 1; i < TIME_RATE.length; i++) {
            float d = Math.abs(TIME_RATE[i] - HudState.rate);
            if (d < bd) {
               bd = d;
               best = i;
            }
         }

         return best;
      }
   }

   public static int bottom() {
      int b = 44;
      if (!MortalClient.active()) {
         return b;
      } else {
         int[] r = mortalRect(MinecraftClient.getInstance().getWindow().getScaledWidth());
         return Math.max(b, r[1] + r[3] + 2);
      }
   }

   static int[] mortalRect(int width) {
      int h = MortalClient.title.isEmpty() ? 12 : 23;
      int card = 34;
      int map = width - 118 - 6;
      int half = Math.min(width / 2 - card, map - width / 2);
      if (half * 2 >= 120) {
         int w = Math.min(190, half * 2);
         return new int[]{width / 2 - w / 2, 48, w, h};
      } else {
         int x = 34;
         return new int[]{x, 74, Math.max(80, Math.min(190, map - x)), h};
      }
   }

   static void renderMortal(DrawContext ctx, TextRenderer tr, int width, int mx, int my) {
      int[] r = mortalRect(width);
      int x = r[0];
      int y = r[1];
      int w = r[2];
      int h = r[3];
      int pool = MortalClient.pool;
      int cap = MortalClient.cap;
      boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
      ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, -1341124584);
      if (hov) {
         ctx.fill(x, y, x + w, y + h, 822083583);
      }

      float part = Math.max(0.0F, Math.min(1.0F, (float)pool / cap));
      int col = part < 0.1F ? -2080704 : -1657822;
      ctx.drawTextWithShadow(tr, "✦", x + 4, y + 2, col);
      String num = w >= 150 ? pool + "/" + cap : String.valueOf(pool);
      int nw = tr.getWidth(num);
      ctx.drawTextWithShadow(tr, num, x + w - nw - 4, y + 2, -1);
      int bx = x + 16;
      int bw = w - 16 - nw - 9;
      if (bw > 8) {
         ctx.fill(bx, y + 3, bx + bw, y + 9, -14671840);
         ctx.fill(bx, y + 3, bx + (int)(bw * part), y + 9, col);
         ctx.fill(bx, y + 3, bx + (int)(bw * part), y + 4, 1627389951);
      }

      boolean scen = !MortalClient.title.isEmpty();
      if (scen) {
         int tc = MortalClient.state == 1 ? -11141291 : (MortalClient.state == 2 ? -43691 : -10496);
         String title = tr.trimToWidth(MortalClient.title, w - 8);
         ctx.drawTextWithShadow(tr, title, x + 4, y + 13, tc);
         int tw = tr.getWidth(title) + 5;
         if (w - 8 - tw > 20) {
            ctx.drawTextWithShadow(tr, tr.trimToWidth(MortalClient.progress, w - 8 - tw), x + 4 + tw, y + 13, -4473925);
         }
      }

      if (hov && !popup) {
         List<Text> tip = new ArrayList<>();
         tip.add(Text.literal("Божья сила: " + pool + " из " + cap).formatted(Formatting.GOLD));
         tip.add(Text.literal("Приток: +" + MortalClient.perMin + " в минуту — вера и население королевств").formatted(Formatting.GRAY));
         tip.add(Text.literal("Отвеченная молитва и вехи испытаний дают силу сразу").formatted(Formatting.GRAY));
         if (scen) {
            tip.add(Text.literal("Испытание «" + MortalClient.title + "»").formatted(Formatting.YELLOW));
            if (!MortalClient.progress.isEmpty()) {
               tip.add(Text.literal(MortalClient.progress).formatted(Formatting.GRAY));
            }
         } else {
            tip.add(Text.literal("Сила «Испытание» на вкладке «Мир» — выбрать цель").formatted(Formatting.DARK_GRAY));
         }

         ctx.drawTooltip(tr, tip, mx, my + 16);
      }
   }

   static int[] chronRect(TextRenderer tr, int width) {
      int w = tr.getWidth("Летопись") + 14;
      int x0 = timeX(width);
      int total = TIME_LABEL.length * 32 + 34;
      if (x0 + total + 6 + w <= width - 124) {
         return new int[]{x0 + total + 6, 3, w, 16};
      } else {
         return x0 - 6 - w >= 40 ? new int[]{x0 - 6 - w, 3, w, 16} : null;
      }
   }

   public static void render(DrawContext ctx, TextRenderer tr, int width, int height, int mx, int my) {
      int x0 = timeX(width);
      int y = 4;
      int total = TIME_LABEL.length * 32 + 34;
      ctx.fill(x0 - 3, y - 2, x0 + total + 1, y + 14 + 2, -1341124584);
      ctx.drawTextWithShadow(tr, "Время", x0 + 1, y + 3, -5192482);
      int act = active();

      for (int i = 0; i < TIME_LABEL.length; i++) {
         int bx = x0 + 34 + i * 32;
         boolean hov = mx >= bx && mx < bx + 30 && my >= y && my < y + 14;
         int bg = i == act ? (i == 0 ? -255837909 : -256342517) : (hov ? -800042912 : -1071241680);
         ctx.fill(bx, y, bx + 30, y + 14, bg);
         if (i == act) {
            ctx.fill(bx, y + 14 - 2, bx + 30, y + 14, -6528);
         }

         ctx.drawCenteredTextWithShadow(tr, TIME_LABEL[i], bx + 15, y + 3, -1);
      }

      int ex = width / 2 - 95;
      int ey = y + 14 + 4;
      boolean hov = mx >= ex && mx < ex + 190 && my >= ey && my < ey + 22;
      ctx.fill(ex - 1, ey - 1, ex + 190 + 1, ey + 23, -1341124584);
      if (hov) {
         ctx.fill(ex, ey, ex + 190, ey + 22, 822083583);
      }

      int age = Math.max(0, Math.min(AGE_RGB.length - 1, HudState.age));
      int col = AGE_RGB[age];
      String name = HudState.agesOn ? WorldAges.NAMES[age] : "Эпохи выключены";
      ctx.drawCenteredTextWithShadow(tr, name, width / 2, ey + 2, 0xFF000000 | col);
      float prog = HudState.agesOn ? 1.0F - Math.min(1.0F, HudState.ageLeft / 1200.0F) : 0.0F;
      int px = ex + 6;
      int pw = 178;
      int py = ey + 14;
      ctx.fill(px, py, px + pw, py + 4, -14671840);
      ctx.fill(px, py, px + (int)(pw * prog), py + 4, 0xFF000000 | col);
      if (hov) {
         int left = HudState.ageLeft;
         ctx.drawTooltip(
            tr,
            List.of(
               Text.literal(name).formatted(Formatting.GOLD),
               Text.literal(HudState.agesOn ? "До смены эпохи: " + left / 60 + " мин " + left % 60 + " с" : "Цикл эпох отключён").formatted(Formatting.GRAY),
               Text.literal("ЛКМ или J — выбрать эпоху").formatted(Formatting.DARK_GRAY)
            ),
            mx,
            my + 16
         );
      }

      int[] cb = chronRect(tr, width);
      if (cb != null) {
         boolean ch = Ui.in(mx, my, cb[0], cb[1], cb[2], cb[3]);
         Art.button(ctx, cb[0], cb[1], cb[2], cb[3], ch, false);
         ctx.drawCenteredTextWithShadow(tr, "Летопись", cb[0] + cb[2] / 2, cb[1] + 4, -2236963);
         if (ch) {
            ctx.drawTooltip(
               tr,
               List.of(
                  Text.literal("Летопись мира").formatted(Formatting.GOLD),
                  Text.literal("ЛКМ или " + GodKeys.CHRONICLE.getBoundKeyLocalizedText().getString()).formatted(Formatting.DARK_GRAY)
               ),
               mx,
               my + 16
            );
         }
      }

      if (MortalClient.active()) {
         renderMortal(ctx, tr, width, mx, my);
      }

      if (popup) {
         renderPopup(ctx, tr, width, height, mx, my);
      }
   }

   static int[] popupRect(int width, int height) {
      int w = Math.min(width - 20, 376);
      int h = 130;
      return new int[]{width / 2 - w / 2, bottom() + 6, w, h};
   }

   static void renderPopup(DrawContext ctx, TextRenderer tr, int width, int height, int mx, int my) {
      int[] r = popupRect(width, height);
      ctx.fill(r[0] - 2, r[1] - 2, r[0] + r[2] + 2, r[1] + r[3] + 2, -4684277);
      ctx.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], -267251176);
      ctx.drawCenteredTextWithShadow(tr, "Выбор эпохи мира", r[0] + r[2] / 2, r[1] + 5, -10496);
      int cw = (r[2] - 16) / 3;
      String hoverDesc = null;

      for (int i = 0; i < AGE_POWER.length; i++) {
         Power p = Power.byName(AGE_POWER[i]);
         int cx = r[0] + 8 + i % 3 * cw;
         int cy = r[1] + 20 + i / 3 * 34;
         boolean hov = mx >= cx && mx < cx + cw - 4 && my >= cy && my < cy + 30;
         boolean cur = i == HudState.age;
         ctx.fill(cx, cy, cx + cw - 4, cy + 30, cur ? -1069535216 : (hov ? -1070583744 : -1071899612));
         ctx.fill(cx, cy, cx + 2, cy + 30, 0xFF000000 | AGE_RGB[i]);
         if (p != null) {
            ctx.drawItem(new ItemStack(p.icon), cx + 5, cy + 7);
         }

         String n = WorldAges.NAMES[i].replace("Эпоха ", "");
         ctx.drawTextWithShadow(tr, n, cx + 24, cy + 6, 0xFF000000 | AGE_RGB[i]);
         if (p != null) {
            ctx.drawTextWithShadow(tr, tr.trimToWidth(p.desc, cw - 30), cx + 24, cy + 17, -6645080);
         }

         if (hov && p != null) {
            hoverDesc = p.desc;
         }
      }

      if (hoverDesc != null) {
         ctx.drawTooltip(tr, Text.literal(hoverDesc), mx, my);
      }
   }

   public static String click(double mx, double my, int width, int height) {
      if (!popup) {
         int[] cb = chronRect(MinecraftClient.getInstance().textRenderer, width);
         if (cb != null && Ui.in(mx, my, cb[0], cb[1], cb[2], cb[3])) {
            ChronicleScreen.open();
            return "";
         } else {
            int x0 = timeX(width);
            int y = 4;

            for (int i = 0; i < TIME_LABEL.length; i++) {
               int bx = x0 + 34 + i * 32;
               if (mx >= bx && mx < bx + 30 && my >= y && my < y + 14) {
                  if (i == 0) {
                     return TIME_POWER[0];
                  }

                  if (HudState.frozen) {
                     return TIME_POWER[0] + ";" + TIME_POWER[i];
                  }

                  return TIME_POWER[i];
               }
            }

            int ex = width / 2 - 95;
            int ey = y + 14 + 4;
            if (mx >= ex && mx < ex + 190 && my >= ey && my < ey + 22) {
               popup = true;
               return "";
            } else {
               return null;
            }
         }
      } else {
         int[] r = popupRect(width, height);
         int cw = (r[2] - 16) / 3;

         for (int ix = 0; ix < AGE_POWER.length; ix++) {
            int cx = r[0] + 8 + ix % 3 * cw;
            int cy = r[1] + 20 + ix / 3 * 34;
            if (mx >= cx && mx < cx + cw - 4 && my >= cy && my < cy + 30) {
               popup = false;
               return AGE_POWER[ix];
            }
         }

         boolean in = mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
         if (!in) {
            popup = false;
         }

         return "";
      }
   }
}
