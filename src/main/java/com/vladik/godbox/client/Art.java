package com.vladik.godbox.client;

import com.vladik.godbox.Kingdoms;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

public final class Art {
   static final Identifier TEX = Identifier.of("godbox", "textures/gui/ui.png");
   private static final int T = 256;
   private static final int[] PANEL = new int[]{0, 0, 24, 24, 6};
   private static final int[] INSET = new int[]{24, 0, 16, 16, 3};
   private static final int[] TIP = new int[]{40, 0, 16, 16, 4};
   private static final int[] TRACK = new int[]{56, 0, 6, 16, 2};
   private static final int[] THUMB = new int[]{62, 0, 6, 16, 2};
   private static final int[] RIBBON = new int[]{72, 0, 32, 16, 8};
   private static final int[][] BTN = new int[][]{{0, 24, 16, 16, 3}, {16, 24, 16, 16, 3}, {32, 24, 16, 16, 3}, {48, 24, 16, 16, 3}};
   private static final int[] TAB_ON = new int[]{64, 24, 16, 16, 3};
   private static final int[] TAB_OFF = new int[]{80, 24, 16, 16, 3};
   private static final int[] SLOT_SEL = new int[]{96, 24, 16, 16, 3};
   private static final int[] SLOT_HOT = new int[]{112, 24, 16, 16, 3};
   private static final int[] THIN = new int[]{128, 24, 12, 12, 3};
   public static final int CLOSE = 0;
   public static final int SEARCH = 1;
   public static final int STAR = 2;
   public static final int STAR_OFF = 3;
   public static final int LEFT = 4;
   public static final int RIGHT = 5;
   public static final int UP = 6;
   public static final int DOWN = 7;
   public static final int PIN = 8;
   public static final int CHECK = 9;
   public static final int PLUS = 10;
   private static boolean failed;
   private static boolean checked;
   private static boolean enabled = true;
   private static final Kingdoms.K[] KS = Kingdoms.K.values();

   private Art() {
   }

   static void init() {
      ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
         @Override
         public Identifier getFabricId() {
            return Identifier.of("godbox", "art");
         }

         @Override
         public void reload(ResourceManager manager) {
            Art.checked = false;
         }
      });
   }

   static void setEnabled(boolean on) {
      enabled = on;
   }

   static boolean on() {
      if (!enabled) {
         return false;
      } else {
         if (!checked) {
            checked = true;

            try {
               failed = MinecraftClient.getInstance().getResourceManager().getResource(TEX).isEmpty();
            } catch (Throwable var1) {
               failed = true;
            }
         }

         return !failed;
      }
   }

   private static void nine(DrawContext ctx, int[] r, int x, int y, int w, int h) {
      int b = r[4];
      if (w < 2 * b || h < 2 * b) {
         b = Math.max(1, Math.min(w, h) / 2);
      }

      int rb = r[4];
      int cw = r[2] - 2 * rb;
      int ch = r[3] - 2 * rb;
      int mw = w - 2 * b;
      int mh = h - 2 * b;
      int u = r[0];
      int v = r[1];
      int rx = u + r[2] - rb;
      int ry = v + r[3] - rb;
      ctx.drawTexture(TEX, x, y, b, b, u, v, rb, rb, 256, 256);
      ctx.drawTexture(TEX, x + w - b, y, b, b, rx, v, rb, rb, 256, 256);
      ctx.drawTexture(TEX, x, y + h - b, b, b, u, ry, rb, rb, 256, 256);
      ctx.drawTexture(TEX, x + w - b, y + h - b, b, b, rx, ry, rb, rb, 256, 256);
      if (mw > 0) {
         ctx.drawTexture(TEX, x + b, y, mw, b, u + rb, v, cw, rb, 256, 256);
         ctx.drawTexture(TEX, x + b, y + h - b, mw, b, u + rb, ry, cw, rb, 256, 256);
      }

      if (mh > 0) {
         ctx.drawTexture(TEX, x, y + b, b, mh, u, v + rb, rb, ch, 256, 256);
         ctx.drawTexture(TEX, x + w - b, y + b, b, mh, rx, v + rb, rb, ch, 256, 256);
      }

      if (mw > 0 && mh > 0) {
         ctx.drawTexture(TEX, x + b, y + b, mw, mh, u + rb, v + rb, cw, ch, 256, 256);
      }
   }

   public static void panel(DrawContext ctx, int x, int y, int w, int h) {
      if (on()) {
         nine(ctx, PANEL, x, y, w, h);
      } else {
         ctx.fill(x, y, x + w, y + h, -1072689128);
      }
   }

   static void frame(DrawContext ctx, int x, int y, int w, int h) {
      if (on()) {
         nine(ctx, THIN, x, y, w, h);
      } else {
         ctx.fill(x, y, x + w, y + h, -1072689128);
      }
   }

   static void inset(DrawContext ctx, int x, int y, int w, int h) {
      if (on()) {
         nine(ctx, INSET, x, y, w, h);
      } else {
         ctx.fill(x, y, x + w, y + h, -1072557544);
      }
   }

   static void tooltip(DrawContext ctx, int x, int y, int w, int h) {
      if (on()) {
         nine(ctx, TIP, x, y, w, h);
      } else {
         ctx.fill(x, y, x + w, y + h, -267251176);
      }
   }

   public static void button(DrawContext ctx, int x, int y, int w, int h, boolean hovered, boolean active) {
      if (on()) {
         nine(ctx, BTN[active ? 2 : (hovered ? 1 : 0)], x, y, w, h);
      } else {
         ctx.fill(x, y, x + w, y + h, active ? -532664304 : (hovered ? -532133800 : -1072557544));
      }
   }

   static void buttonOff(DrawContext ctx, int x, int y, int w, int h) {
      if (on()) {
         nine(ctx, BTN[3], x, y, w, h);
      } else {
         ctx.fill(x, y, x + w, y + h, -1072557544);
      }
   }

   static void tab(DrawContext ctx, int x, int y, int w, int h, boolean selected, boolean hovered) {
      if (on()) {
         nine(ctx, selected ? TAB_ON : (hovered ? BTN[1] : TAB_OFF), x, y, w, h);
      } else {
         ctx.fill(x, y, x + w, y + h, selected ? -532664304 : (hovered ? -532133800 : -1072557544));
      }
   }

   static void slotSelected(DrawContext ctx, int x, int y, int w, int h) {
      if (on()) {
         nine(ctx, SLOT_SEL, x, y, w, h);
      } else {
         ctx.drawBorder(x, y, w, h, -10496);
      }
   }

   static void slotHover(DrawContext ctx, int x, int y, int w, int h) {
      if (on()) {
         nine(ctx, SLOT_HOT, x, y, w, h);
      } else {
         ctx.fill(x, y, x + w, y + h, 822083583);
      }
   }

   static void ribbon(DrawContext ctx, int x, int y, int w, int h) {
      if (on()) {
         int b = Math.min(RIBBON[4], w / 2);
         ctx.drawTexture(TEX, x, y, b, h, RIBBON[0], RIBBON[1], b, RIBBON[3], 256, 256);
         ctx.drawTexture(TEX, x + w - b, y, b, h, RIBBON[0] + RIBBON[2] - b, RIBBON[1], b, RIBBON[3], 256, 256);
         if (w > 2 * b) {
            ctx.drawTexture(TEX, x + b, y, w - 2 * b, h, RIBBON[0] + b, RIBBON[1], RIBBON[2] - 2 * b, RIBBON[3], 256, 256);
         }
      } else {
         ctx.fill(x, y, x + w, y + h, -1069537264);
      }
   }

   static void scrollTrack(DrawContext ctx, int x, int y, int w, int h) {
      if (on()) {
         nine(ctx, TRACK, x, y, w, h);
      } else {
         ctx.fill(x, y, x + w, y + h, -1072557544);
      }
   }

   static void scrollThumb(DrawContext ctx, int x, int y, int w, int h) {
      if (on()) {
         nine(ctx, THUMB, x, y, w, h);
      } else {
         ctx.fill(x, y, x + w, y + h, -4684277);
      }
   }

   static boolean glyph(DrawContext ctx, int id, int x, int y, int size) {
      if (!on()) {
         return false;
      } else {
         ctx.drawTexture(TEX, x, y, size, size, id * 8, 48.0F, 8, 8, 256, 256);
         return true;
      }
   }

   static Kingdoms.K forRgb(int rgb) {
      rgb &= 16777215;

      for (Kingdoms.K k : KS) {
         if (k.rgb == rgb) {
            return k;
         }
      }

      return null;
   }

   static Kingdoms.K forText(String s) {
      if (s == null) {
         return null;
      } else {
         for (Kingdoms.K k : KS) {
            if (s.contains(k.title) || s.contains(k.defaultTitle)) {
               return k;
            }
         }

         return null;
      }
   }

   public static void emblem(DrawContext ctx, Kingdoms.K k, int x, int y, int size) {
      if (on()) {
         int i = k.ordinal();
         if (i < 12) {
            if (size <= 20) {
               ctx.drawTexture(TEX, x, y, size, size, i * 16, 128.0F, 16, 16, 256, 256);
            } else {
               ctx.drawTexture(TEX, x, y, size, size, i % 8 * 32, 64 + i / 8 * 32, 32, 32, 256, 256);
            }

            return;
         }
      }

      ctx.fill(x, y, x + size, y + size, 0xFF000000 | k.rgb);
   }
}
