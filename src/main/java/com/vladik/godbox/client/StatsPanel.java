package com.vladik.godbox.client;

import com.vladik.godbox.Kingdoms;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

public final class StatsPanel {
   public static String text = "";
   public static boolean on;
   public static int timer;

   private StatsPanel() {
   }

   public static boolean inside(double mx, double my, int width, int height) {
      return on && mx >= 20.0 && mx < width - 20 && my >= 34.0 && my < height - 30;
   }

   public static void render(DrawContext ctx, TextRenderer tr, int width, int height) {
      if (on) {
         int x0 = 20;
         int y0 = 34;
         int x1 = width - 20;
         int y1 = height - 30;
         if (Art.on()) {
            Art.frame(ctx, x0, y0, x1 - x0, y1 - y0);
         } else {
            ctx.fill(x0, y0, x1, y1, -804253672);
            ctx.drawBorder(x0, y0, x1 - x0, y1 - y0, -3628992);
         }

         ctx.drawText(tr, Text.literal("§6§lСтатистика королевств"), x0 + 8, y0 + 6, 16777215, true);
         ctx.drawText(tr, Text.literal("§8K — закрыть"), x1 - 70, y0 + 6, 16777215, false);
         String t = text == null ? "" : text;
         int sep = t.indexOf(2);
         String kings = sep >= 0 ? t.substring(0, sep) : t;
         String chron = sep >= 0 ? t.substring(sep + 1) : "";
         String[] blocks = kings.split("\u0001");
         int pages = Math.max(1, (blocks.length + 3) / 4);
         int page = timer / 160 % pages;
         int from = page * 4;
         int cols = Math.max(1, Math.min(blocks.length - from, 4));
         int colW = (x1 - x0 - 16) / cols;
         int chronH = Math.min(110, (y1 - y0) / 3);
         int bottom = y1 - chronH - 6;
         if (pages > 1) {
            String pg = "§7стр. " + (page + 1) + "/" + pages + " §8· королевств: " + blocks.length;
            ctx.drawText(tr, Text.literal(pg), x1 - 78 - tr.getWidth(Text.literal(pg)), y0 + 6, 16777215, false);
         }

         for (int n = 0; n < cols; n++) {
            int i = from + n;
            int cx = x0 + 8 + n * colW;
            int cy = y0 + 22;
            Kingdoms.K kk = Art.forText(blocks[i]);
            if (kk != null) {
               Art.emblem(ctx, kk, cx + colW - 26, cy - 2, 16);
            }

            for (String line : blocks[i].split("\n")) {
               for (OrderedText o : tr.wrapLines(Text.literal(line), colW - 8)) {
                  if (cy > bottom - 9) {
                     break;
                  }

                  ctx.drawText(tr, o, cx, cy, 16777215, true);
                  cy += 10;
               }

               cy++;
            }
         }

         int cy = bottom + 2;
         ctx.fill(x0 + 6, cy, x1 - 6, cy + 1, -2134335424);
         cy += 4;
         ctx.drawText(tr, Text.literal("§6Летопись мира"), x0 + 8, cy, 16777215, true);
         cy += 12;

         for (String line : chron.split("\n")) {
            for (OrderedText o : tr.wrapLines(Text.literal(line), x1 - x0 - 20)) {
               if (cy > y1 - 10) {
                  return;
               }

               ctx.drawText(tr, o, x0 + 8, cy, 16777215, false);
               cy += 10;
            }
         }
      }
   }
}
