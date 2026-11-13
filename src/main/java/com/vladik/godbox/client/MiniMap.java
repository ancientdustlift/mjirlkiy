package com.vladik.godbox.client;

import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.block.MapColor.Brightness;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.Heightmap.Type;

final class MiniMap {
   static final int SIZE = 128;
   static boolean on;
   private static NativeImageBackedTexture tex;
   private static final Identifier ID = Identifier.of("godbox", "minimap");
   private static int row;
   private static int cx;
   private static int cz;
   private static int ncx;
   private static int ncz;
   private static boolean ready;

   private MiniMap() {
   }

   static void tick(MinecraftClient mc) {
      if (on && mc.world != null && mc.player != null) {
         if (tex == null) {
            tex = new NativeImageBackedTexture(128, 128, true);
            mc.getTextureManager().registerTexture(ID, tex);
         }

         NativeImage img = tex.getImage();
         if (img != null) {
            ClientWorld w = mc.world;
            if (row == 0) {
               ncx = MathHelper.floor(mc.player.getX());
               ncz = MathHelper.floor(mc.player.getZ());
            }

            Mutable m = new Mutable();

            for (int n = 0; n < 32 && row < 128; row++) {
               int z = ncz - 64 + row;

               for (int i = 0; i < 128; i++) {
                  int x = ncx - 64 + i;
                  int color;
                  if (!w.isChunkLoaded(x >> 4, z >> 4)) {
                     color = -15395563;
                  } else {
                     int y = w.getTopY(Type.WORLD_SURFACE, x, z) - 1;
                     m.set(x, y, z);
                     BlockState s = w.getBlockState(m);
                     MapColor mcol = s.getMapColor(w, m);
                     int yn = w.getTopY(Type.WORLD_SURFACE, x, z - 1) - 1;
                     Brightness b = y > yn ? Brightness.HIGH : (y < yn ? Brightness.LOW : Brightness.NORMAL);
                     if (!s.getFluidState().isEmpty()) {
                        b = Brightness.NORMAL;
                     }

                     color = mcol == MapColor.CLEAR ? -16777216 : mcol.getRenderColor(b);
                  }

                  img.setColor(i, row, WorldMap.tint(color, x, z));
               }

               n++;
            }

            if (row >= 128) {
               row = 0;
               cx = ncx;
               cz = ncz;
               ready = true;
            }

            tex.upload();
         }
      }
   }

   static void render(DrawContext ctx, MinecraftClient mc, int x, int y, int s) {
      if (on && mc.world != null && mc.player != null) {
         if (tex != null && ready) {
            if (Art.on()) {
               Art.frame(ctx, x - 3, y - 3, s + 6, s + 6);
            } else {
               ctx.fill(x - 2, y - 2, x + s + 2, y + s + 2, -12965360);
            }

            ctx.drawTexture(ID, x, y, 0.0F, 0.0F, s, s, s, s);
            double k = s / 128.0;
            double ox = cx - 64.0;
            double oz = cz - 64.0;

            for (Entity e : mc.world.getEntities()) {
               Team t = e.getScoreboardTeam();
               if (t != null && t.getName().startsWith("godbox_")) {
                  double px = (e.getX() - ox) * k;
                  double pz = (e.getZ() - oz) * k;
                  if (!(px < 1.0) && !(pz < 1.0) && !(px >= s - 1) && !(pz >= s - 1)) {
                     Integer rgb = t.getColor().getColorValue();
                     int c = 0xFF000000 | (rgb == null ? 16777215 : rgb);
                     int ix = x + (int)px;
                     int iz = y + (int)pz;
                     ctx.fill(ix - 1, iz - 1, ix + 1, iz + 1, c);
                  }
               }
            }

            double px = (mc.player.getX() - ox) * k;
            double pz = (mc.player.getZ() - oz) * k;
            int ix = x + MathHelper.clamp((int)px, 2, s - 2);
            int iz = y + MathHelper.clamp((int)pz, 2, s - 2);
            ctx.fill(ix - 2, iz - 2, ix + 2, iz + 2, -1);
            double yr = Math.toRadians(mc.player.getYaw());
            int dx = (int)(-Math.sin(yr) * 6.0);
            int dz = (int)(Math.cos(yr) * 6.0);
            ctx.fill(ix + dx - 1, iz + dz - 1, ix + dx + 1, iz + dz + 1, -49088);
            if (!Art.on()) {
               ctx.drawBorder(x - 1, y - 1, s + 2, s + 2, -10496);
            }

            ctx.drawTextWithShadow(mc.textRenderer, "N", x + s / 2 - 2, y + 2, -1);
         } else {
            if (Art.on()) {
               Art.inset(ctx, x, y, s, s);
            } else {
               ctx.fill(x, y, x + s, y + s, -1073741824);
            }

            ctx.drawCenteredTextWithShadow(mc.textRenderer, "карта…", x + s / 2, y + s / 2 - 4, -5592406);
         }
      }
   }

   static int[] click(double mx, double my, int x, int y, int s) {
      if (on && ready && !(mx < x) && !(my < y) && !(mx >= x + s) && !(my >= y + s)) {
         double k = 128.0 / s;
         return new int[]{(int)Math.floor(cx - 64.0 + (mx - x) * k), (int)Math.floor(cz - 64.0 + (my - y) * k)};
      } else {
         return null;
      }
   }
}
