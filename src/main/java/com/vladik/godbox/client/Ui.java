package com.vladik.godbox.client;

import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector2i;

final class Ui {
   static final int BG = -1072689128;
   static final int BG_SOLID = -267251176;
   static final int LINE = 1627389951;
   static final int CELL = -1072557544;
   static final int CELL_HOVER = -532133800;
   static final int CELL_SEL = -532664304;
   static final int CELL_SOLID = -1071241680;
   static final int GOLD = -10496;
   static final int GOLD_DARK = -4684277;
   static final int GOLD_LIGHT = -6528;
   static final int TEXT = -2236963;
   static final int MUTED = -4473925;
   static final int DIM = -8355696;
   static final int WHITE = -1;
   static final int BLUE = -6631169;
   static final int AQUA = -12541728;
   static final int RED = -2080704;
   static final int GREEN = -11149200;
   static final int LAW_ON = -1072416228;
   static final int LAW_OFF = -1069935082;
   static final int PAD = 4;
   static final int GAP = 2;
   static final TooltipPositioner TIP = (sw, sh, x, y, w, h) -> {
      int tx = x + 12;
      if (tx + w + 4 > sw) {
         tx = x - 12 - w;
      }

      tx = MathHelper.clamp(tx, 4, Math.max(4, sw - w - 4));
      int ty = MathHelper.clamp(y - 12, 4, Math.max(4, sh - h - 4));
      return new Vector2i(tx, ty);
   };

   private Ui() {
   }

   static boolean in(double mx, double my, int x, int y, int w, int h) {
      return mx >= x && mx < x + w && my >= y && my < y + h;
   }

   static String ellipsis(TextRenderer tr, String s, int w) {
      if (s != null && w > 0) {
         if (tr.getWidth(s) <= w) {
            return s;
         } else {
            int dots = tr.getWidth("…");
            return w <= dots ? "" : tr.trimToWidth(s, w - dots) + "…";
         }
      } else {
         return "";
      }
   }

   static String norm(String s) {
      return s == null ? "" : s.toLowerCase(Locale.ROOT).replace('ё', 'е');
   }

   static void click() {
      MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
   }
}
