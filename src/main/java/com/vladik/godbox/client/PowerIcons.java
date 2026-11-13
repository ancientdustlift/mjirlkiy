package com.vladik.godbox.client;

import com.vladik.godbox.Category;
import com.vladik.godbox.Power;
import java.io.InputStream;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

public final class PowerIcons {
   private static final int CELL = 16;
   static final int COLS = 20;
   static final int POWER_COUNT = 307;
   static final int POWER_HASH = 1923478904;
   static final int CAT_COUNT = 13;
   static final int CAT_HASH = -1152260766;
   private static final Identifier POWERS = Identifier.of("godbox", "textures/gui/power_icons.png");
   private static final Identifier CATS = Identifier.of("godbox", "textures/gui/category_icons.png");
   private static boolean checked;
   private static boolean powersOk;
   private static boolean catsOk;
   private static int powerW;
   private static int powerH;
   private static int catW;

   private PowerIcons() {
   }

   private static int[] size(Identifier id) {
      try {
         Optional<Resource> res = MinecraftClient.getInstance().getResourceManager().getResource(id);
         if (res.isEmpty()) {
            return null;
         } else {
            int[] var4;
            try (
               InputStream in = res.get().getInputStream();
               NativeImage img = NativeImage.read(in);
            ) {
               var4 = new int[]{img.getWidth(), img.getHeight()};
            }

            return var4;
         }
      } catch (Throwable var10) {
         return null;
      }
   }

   private static int hashOf(Enum<?>[] values) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < values.length; i++) {
         if (i > 0) {
            sb.append(',');
         }

         sb.append(values[i].name());
      }

      return sb.toString().hashCode();
   }

   private static void check() {
      checked = true;
      Power[] ps = Power.values();
      Category[] cs = Category.values();
      int[] a = size(POWERS);
      int rows = (ps.length + 20 - 1) / 20;
      powersOk = a != null && ps.length == 307 && hashOf(ps) == 1923478904 && a[0] == 320 && a[1] == rows * 16;
      powerW = powersOk ? a[0] : 0;
      powerH = powersOk ? a[1] : 0;
      int[] b = size(CATS);
      catsOk = b != null && cs.length == 13 && hashOf(cs) == -1152260766 && b[0] == cs.length * 16 && b[1] == 16;
      catW = catsOk ? b[0] : 0;
   }

   public static boolean draw(DrawContext ctx, Power power, int x, int y, int size) {
      if (!checked) {
         check();
      }

      if (powersOk && power != null) {
         int i = power.ordinal();
         ctx.drawTexture(POWERS, x, y, size, size, i % 20 * 16, i / 20 * 16, 16, 16, powerW, powerH);
         return true;
      } else {
         return false;
      }
   }

   public static boolean drawCategory(DrawContext ctx, Category category, int x, int y, int size) {
      if (!checked) {
         check();
      }

      if (catsOk && category != null) {
         ctx.drawTexture(CATS, x, y, size, size, category.ordinal() * 16, 0.0F, 16, 16, catW, 16);
         return true;
      } else {
         return false;
      }
   }
}
