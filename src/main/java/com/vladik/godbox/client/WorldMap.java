package com.vladik.godbox.client;

import com.vladik.godbox.Kingdoms;
import com.vladik.godbox.net.ChroniclePayloads;
import com.vladik.godbox.net.HistoryPayloads;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.block.MapColor.Brightness;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.Heightmap.Type;
import org.lwjgl.glfw.GLFW;

final class WorldMap {
   static boolean on;
   static final List<WorldMap.Zone> ZONES = new ArrayList<>();
   static boolean pinsOn = true;
   private static int[] pinChip;
   static final List<int[]> WARS = new ArrayList<>();
   static final List<int[]> ALLIES = new ArrayList<>();
   static final List<String> LEGEND = new ArrayList<>();
   static final List<Integer> LEGEND_RGB = new ArrayList<>();
   private static final int TEX = 192;
   private static final Identifier ID = Identifier.of("godbox", "worldmap");
   private static NativeImageBackedTexture tex;
   private static int texRow;
   private static int texKey;
   private static int texDone = -1;
   private static double cX;
   private static double cZ;
   private static double span = 512.0;
   private static boolean fitted;
   private static int lx;
   private static int ly;
   private static int ls;
   private static double lminX;
   private static double lminZ;
   private static double lscale;
   static boolean replay;
   private static final List<WorldMap.Frame> FRAMES = new ArrayList<>();
   private static final List<WorldMap.Frame> INCOMING = new ArrayList<>();
   private static final int[] BASE = new int[36864];
   private static int tintKey;
   private static int fi;
   private static int speed = 1;
   private static boolean playing;
   private static boolean loading;
   private static boolean unsupported;
   private static boolean scrubbing;
   private static double playAcc;
   private static long lastRender;
   private static long lastAsk;
   private static long capSince;
   private static List<String> caps = List.of();
   private static int capFrame = -1;
   private static int barY = -100;
   private static int hbX0;
   private static int hbX1;
   private static int pbX0;
   private static int pbX1;
   private static int spX0;
   private static int spX1;
   private static int sx0;
   private static int sx1;

   private WorldMap() {
   }

   static void set(String d) {
      List<WorldMap.Zone> zs = new ArrayList<>();
      WARS.clear();
      ALLIES.clear();
      LEGEND.clear();
      LEGEND_RGB.clear();

      for (String line : d.split("\n")) {
         String[] a = line.split("\\|", -1);

         try {
            if (a[0].equals("T") && a.length >= 7) {
               zs.add(
                  new WorldMap.Zone(
                     Integer.parseInt(a[1]),
                     Integer.parseInt(a[2]),
                     Integer.parseInt(a[3]),
                     Integer.parseInt(a[4]),
                     a[5],
                     a[6].equals("1"),
                     a.length > 7 ? a[7] : "",
                     a.length > 8 ? Integer.parseInt(a[8]) : 0
                  )
               );
            } else if ((a[0].equals("W") || a[0].equals("A")) && a.length >= 5) {
               (a[0].equals("W") ? WARS : ALLIES)
                  .add(new int[]{Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]), Integer.parseInt(a[4])});
            } else if (a[0].equals("L") && a.length >= 3) {
               LEGEND_RGB.add(Integer.parseInt(a[1]));
               LEGEND.add(a[2]);
            }
         } catch (NumberFormatException var8) {
         }
      }

      if (!zs.equals(ZONES)) {
         ZONES.clear();
         ZONES.addAll(zs);
         texKey = 0;
      }
   }

   static int own(int x, int z) {
      return own(ZONES, x, z);
   }

   private static int own(List<WorldMap.Zone> zones, int x, int z) {
      int best = -1;
      double bd = 1.0;

      for (WorldMap.Zone zn : zones) {
         double dx = x - zn.x;
         double dz = z - zn.z;
         double q = (dx * dx + dz * dz) / ((double)zn.r * zn.r);
         if (q < bd) {
            bd = q;
            best = zn.rgb;
         }
      }

      return best;
   }

   static int abgr(int rgb) {
      return 0xFF000000 | (rgb & 0xFF) << 16 | rgb & 0xFF00 | rgb >> 16 & 0xFF;
   }

   static int mix(int a, int b, double k) {
      int r = (int)((a >> 16 & 0xFF) * (1.0 - k) + (b >> 16 & 0xFF) * k);
      int g = (int)((a >> 8 & 0xFF) * (1.0 - k) + (b >> 8 & 0xFF) * k);
      int bl = (int)((a & 0xFF) * (1.0 - k) + (b & 0xFF) * k);
      return 0xFF000000 | r << 16 | g << 8 | bl;
   }

   static int tint(int color, int x, int z) {
      return tint(color, x, z, 1);
   }

   static int tint(int color, int x, int z, int step) {
      return tint(ZONES, color, x, z, step);
   }

   private static int tint(List<WorldMap.Zone> zones, int color, int x, int z, int step) {
      if (zones.isEmpty()) {
         return color;
      } else {
         int o = own(zones, x, z);
         if (o < 0) {
            return color;
         } else {
            int c = abgr(o);
            return own(zones, x - step, z) == o && own(zones, x + step, z) == o && own(zones, x, z - step) == o && own(zones, x, z + step) == o
               ? mix(color, c, 0.32)
               : c;
         }
      }
   }

   static void center(double px, double pz) {
      double minX = px - 96.0;
      double maxX = px + 96.0;
      double minZ = pz - 96.0;
      double maxZ = pz + 96.0;

      for (WorldMap.Zone zn : ZONES) {
         minX = Math.min(minX, (double)(zn.x - zn.r));
         maxX = Math.max(maxX, (double)(zn.x + zn.r));
         minZ = Math.min(minZ, (double)(zn.z - zn.r));
         maxZ = Math.max(maxZ, (double)(zn.z + zn.r));
      }

      cX = (minX + maxX) / 2.0;
      cZ = (minZ + maxZ) / 2.0;
      span = MathHelper.clamp(Math.max(maxX - minX, maxZ - minZ) * 1.15, 96.0, 8192.0);
      fitted = true;
   }

   static void scroll(double mx, double my, double v) {
      if (ls != 0) {
         if (replay && FRAMES.size() >= 2 && my >= barY - 3) {
            playing = false;
            go(fi + (v > 0.0 ? -1 : 1));
         } else {
            double wx = lminX + (mx - lx) / lscale;
            double wz = lminZ + (my - ly) / lscale;
            boolean in = mx >= lx && my >= ly && mx < lx + ls && my < ly + ls;
            double ns = MathHelper.clamp(span * (v > 0.0 ? 0.8 : 1.25), 48.0, 8192.0);
            if (in) {
               double k = ns / span;
               cX = wx + (cX - wx) * k;
               cZ = wz + (cZ - wz) * k;
            }

            span = ns;
         }
      }
   }

   static void drag(double dx, double dy) {
      if (!(lscale <= 0.0) && !scrubbing) {
         cX = cX - dx / lscale;
         cZ = cZ - dy / lscale;
      }
   }

   private static void bake(MinecraftClient mc, double minX, double minZ, List<WorldMap.Zone> zones) {
      if (tex == null) {
         tex = new NativeImageBackedTexture(192, 192, true);
         mc.getTextureManager().registerTexture(ID, tex);
      }

      NativeImage img = tex.getImage();
      if (img != null) {
         int key = (int)(Math.round(minX) * 31L + Math.round(minZ)) * 31 + (int)Math.round(span) + (replay ? 0 : ZONES.hashCode() * 17);
         if (key != texKey) {
            texKey = key;
            texRow = 0;
         }

         double bpp = span / 192.0;
         int step = Math.max(1, (int)bpp);
         int zk = zones.hashCode() * 17 + 1;
         boolean retint = zk != tintKey && texRow > 0;
         tintKey = zk;
         if (retint) {
            for (int row = 0; row < texRow; row++) {
               int z = MathHelper.floor(minZ + (row + 0.5) * bpp);

               for (int i = 0; i < 192; i++) {
                  img.setColor(i, row, tint(zones, BASE[row * 192 + i], MathHelper.floor(minX + (i + 0.5) * bpp), z, step));
               }
            }
         }

         if (texRow >= 192) {
            if (retint) {
               tex.upload();
            }
         } else {
            ClientWorld w = mc.world;
            Mutable m = new Mutable();

            for (long until = System.nanoTime() + 4000000L; texRow < 192 && System.nanoTime() < until; texRow++) {
               int z = MathHelper.floor(minZ + (texRow + 0.5) * bpp);

               for (int i = 0; i < 192; i++) {
                  int x = MathHelper.floor(minX + (i + 0.5) * bpp);
                  int color;
                  if (!w.isChunkLoaded(x >> 4, z >> 4)) {
                     boolean chk = ((x >> 6) + (z >> 6) & 1) == 0;
                     color = chk ? -14015976 : -14410476;
                  } else {
                     int y = w.getTopY(Type.WORLD_SURFACE, x, z) - 1;
                     m.set(x, y, z);
                     BlockState s = w.getBlockState(m);
                     MapColor mcol = s.getMapColor(w, m);
                     int yn = w.getTopY(Type.WORLD_SURFACE, x, z - step) - 1;
                     Brightness b = y > yn ? Brightness.HIGH : (y < yn ? Brightness.LOW : Brightness.NORMAL);
                     if (!s.getFluidState().isEmpty()) {
                        b = y > 50 ? Brightness.NORMAL : Brightness.LOW;
                     }

                     color = mcol == MapColor.CLEAR ? -16777216 : mcol.getRenderColor(b);
                  }

                  BASE[texRow * 192 + i] = color;
                  img.setColor(i, texRow, tint(zones, color, x, z, step));
               }
            }

            tex.upload();
         }
      }
   }

   private static void small(DrawContext ctx, TextRenderer tr, String s, int x, int y, int color, float k) {
      ctx.getMatrices().push();
      ctx.getMatrices().translate((float)x, (float)y, 0.0F);
      ctx.getMatrices().scale(k, k, 1.0F);
      ctx.drawTextWithShadow(tr, s, -tr.getWidth(s) / 2, 0, color);
      ctx.getMatrices().pop();
   }

   private static void line(DrawContext ctx, int x1, int y1, int x2, int y2, int color, int dash, int x0, int y0, int s) {
      int len = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
      if (len != 0) {
         for (int i = 0; i <= len; i++) {
            if (dash <= 0 || i / dash % 2 != 1) {
               int x = x1 + (x2 - x1) * i / len;
               int y = y1 + (y2 - y1) * i / len;
               if (x >= x0 && y >= y0 && x < x0 + s && y < y0 + s) {
                  ctx.fill(x, y, x + 1, y + 1, color);
               }
            }
         }
      }
   }

   static void render(DrawContext ctx, MinecraftClient mc, int width, int height, int mx, int my) {
      if (mc.player != null && mc.world != null) {
         if (!fitted) {
            center(mc.player.getX(), mc.player.getZ());
         }

         TextRenderer tr = mc.textRenderer;
         long now = System.currentTimeMillis();
         if (replay && now - lastRender > 1500L) {
            exitReplay();
         }

         long dt = Math.min(250L, Math.max(0L, now - lastRender));
         lastRender = now;
         boolean ready = replay && FRAMES.size() >= 2;
         if (ready && scrubbing) {
            if (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) == 1) {
               go(frameAt(mx));
            } else {
               scrubbing = false;
            }
         } else {
            scrubbing = false;
         }

         if (ready && playing) {
            playAcc = playAcc + dt * 4.0 * speed / 1000.0;

            while (playAcc >= 1.0) {
               playAcc--;
               if (fi >= FRAMES.size() - 1) {
                  playing = false;
                  playAcc = 0.0;
                  break;
               }

               go(fi + 1);
            }
         }

         WorldMap.Frame fr = ready ? FRAMES.get(MathHelper.clamp(fi, 0, FRAMES.size() - 1)) : null;
         List<WorldMap.Zone> ZONES = fr != null ? fr.zones : WorldMap.ZONES;
         List<String> LEGEND = fr != null ? fr.legend : WorldMap.LEGEND;
         List<Integer> LEGEND_RGB = fr != null ? fr.legendRgb : WorldMap.LEGEND_RGB;
         List<int[]> WARS = fr != null ? List.of() : WorldMap.WARS;
         List<int[]> ALLIES = fr != null ? List.of() : WorldMap.ALLIES;
         int legendW = width > 560 && !LEGEND.isEmpty() ? 170 : 0;
         int s = Math.min(width - 30 - legendW, height - 70);
         if (s >= 80) {
            int x0 = (width - s - legendW) / 2;
            int y0 = 32;
            double minX = cX - span / 2.0;
            double minZ = cZ - span / 2.0;
            double scale = s / span;
            lx = x0;
            ly = y0;
            ls = s;
            lminX = minX;
            lminZ = minZ;
            lscale = scale;
            ctx.fill(0, 0, width, height, -1879048192);
            ctx.fill(x0 - 5, y0 - 5, x0 + s + 5, y0 + s + 5, -9744860);
            ctx.fill(x0 - 3, y0 - 3, x0 + s + 3, y0 + s + 3, -3562148);
            ctx.fill(x0 - 1, y0 - 1, x0 + s + 1, y0 + s + 1, -12965360);
            bake(mc, minX, minZ, ZONES);
            ctx.drawTexture(ID, x0, y0, s, s, 0.0F, 0.0F, 192, 192, 192, 192);
            if (texRow < 192) {
               small(ctx, tr, "рисую рельеф… " + texRow * 100 / 192 + "%", x0 + s / 2, y0 + s - 10, -5592406, 0.75F);
            }

            int[] steps = new int[]{16, 32, 64, 128, 256, 512, 1024, 2048};
            int gs = steps[steps.length - 1];

            for (int st : steps) {
               if (span / st <= 8.0) {
                  gs = st;
                  break;
               }
            }

            for (double gx = Math.ceil(minX / gs) * gs; gx < minX + span; gx += gs) {
               int sx = x0 + (int)((gx - minX) * scale);
               ctx.fill(sx, y0, sx + 1, y0 + s, 654311423);
               small(ctx, tr, String.valueOf((int)gx), sx, y0 + 2, -1862270977, 0.6F);
            }

            for (double gz = Math.ceil(minZ / gs) * gs; gz < minZ + span; gz += gs) {
               int sy = y0 + (int)((gz - minZ) * scale);
               ctx.fill(x0, sy, x0 + s, sy + 1, 654311423);
               small(ctx, tr, String.valueOf((int)gz), x0 + 12, sy + 2, -1862270977, 0.6F);
            }

            for (int[] a : ALLIES) {
               line(
                  ctx,
                  x0 + (int)((a[0] - minX) * scale),
                  y0 + (int)((a[1] - minZ) * scale),
                  x0 + (int)((a[2] - minX) * scale),
                  y0 + (int)((a[3] - minZ) * scale),
                  -1067396865,
                  2,
                  x0,
                  y0,
                  s
               );
            }

            for (int[] a : WARS) {
               int ax = x0 + (int)((a[0] - minX) * scale);
               int ay = y0 + (int)((a[1] - minZ) * scale);
               int bx = x0 + (int)((a[2] - minX) * scale);
               int by = y0 + (int)((a[3] - minZ) * scale);
               line(ctx, ax, ay, bx, by, -53200, 4, x0, y0, s);
               int mxp = (ax + bx) / 2;
               int myp = (ay + by) / 2;
               if (mxp > x0 && myp > y0 && mxp < x0 + s && myp < y0 + s) {
                  ctx.fill(mxp - 6, myp - 5, mxp + 7, myp + 6, -801112064);
                  ctx.drawCenteredTextWithShadow(tr, "⚔", mxp, myp - 4, -40864);
               }
            }

            WorldMap.Zone hov = null;
            double hd = 64.0;
            boolean names = span <= 1600.0;

            for (WorldMap.Zone zn : ZONES) {
               int zx = x0 + (int)((zn.x - minX) * scale);
               int zy = y0 + (int)((zn.z - minZ) * scale);
               if (zx >= x0 - 40 && zy >= y0 - 20 && zx <= x0 + s + 40 && zy <= y0 + s + 20) {
                  double d = (mx - zx) * (mx - zx) + (my - zy) * (my - zy);
                  if (d < hd) {
                     hd = d;
                     hov = zn;
                  }

                  if (zx >= x0 && zy >= y0 && zx < x0 + s && zy < y0 + s) {
                     int col = 0xFF000000 | zn.rgb;
                     if (zn.cap) {
                        ctx.fill(zx - 4, zy - 4, zx + 5, zy + 5, -16777216);
                        ctx.fill(zx - 3, zy - 3, zx + 4, zy + 4, -10496);
                        ctx.fill(zx - 2, zy - 2, zx + 3, zy + 3, col);
                        String n = "♛ " + zn.name;
                        int w = tr.getWidth(n) + 6;
                        int ny = Math.max(y0 + 1, zy - 19);
                        ctx.fill(zx - w / 2, ny, zx + w / 2, ny + 11, -1072689136);
                        ctx.fill(zx - w / 2, ny + 10, zx + w / 2, ny + 11, col);
                        ctx.drawCenteredTextWithShadow(tr, n, zx, ny + 2, -6528);
                        if (!zn.kingdom.isEmpty()) {
                           small(ctx, tr, zn.kingdom, zx, zy + 7, col, 0.7F);
                        }
                     } else {
                        ctx.fill(zx - 2, zy - 2, zx + 3, zy + 3, -16777216);
                        ctx.fill(zx - 1, zy - 1, zx + 2, zy + 2, col);
                        if (names && !zn.name.isEmpty()) {
                           int w = (int)(tr.getWidth(zn.name) * 0.75F) + 4;
                           ctx.fill(zx - w / 2, zy - 11, zx + w / 2, zy - 3, -1877995504);
                           small(ctx, tr, zn.name, zx, zy - 10, -1118482, 0.75F);
                        }
                     }
                  }
               }
            }

            pinChip = null;
            List<Text> pinTip = null;
            if (!replay) {
               if (pinsOn && now - ChronicleScreen.pinsAt > 20000L && ClientPlayNetworking.canSend(ChroniclePayloads.PinReq.ID)) {
                  ChronicleScreen.pinsAt = now;
                  ClientPlayNetworking.send(new ChroniclePayloads.PinReq());
               }

               String pl = "События: " + (pinsOn ? "вкл" : "выкл");
               int pcw = tr.getWidth(pl) + 10;
               pinChip = new int[]{x0 + 6, y0 + 6, pcw, 14};
               Art.button(ctx, pinChip[0], pinChip[1], pcw, 14, Ui.in(mx, my, pinChip[0], pinChip[1], pcw, 14), pinsOn);
               ctx.drawCenteredTextWithShadow(tr, pl, pinChip[0] + pcw / 2, pinChip[1] + 3, pinsOn ? -6528 : -4473925);
               if (pinsOn) {
                  double best = 49.0;

                  for (ChroniclePayloads.Dto e : ChronicleScreen.pins) {
                     if (e.hasPos()) {
                        int ex = x0 + (int)((e.x() - minX) * scale);
                        int ey = y0 + (int)((e.z() - minZ) * scale);
                        if (ex >= x0 + 3 && ey >= y0 + 3 && ex < x0 + s - 3 && ey < y0 + s - 3) {
                           Kingdoms.K ek = Kingdoms.byId(e.k());
                           int pc = ek != null ? 0xFF000000 | ek.rgb : -10496;
                           ctx.fill(ex - 4, ey - 1, ex + 5, ey + 2, -16777216);
                           ctx.fill(ex - 1, ey - 4, ex + 2, ey + 5, -16777216);
                           ctx.fill(ex - 3, ey, ex + 4, ey + 1, pc);
                           ctx.fill(ex, ey - 3, ex + 1, ey + 4, pc);
                           ctx.fill(ex - 1, ey - 1, ex + 2, ey + 2, -6528);
                           double dd = (mx - ex) * (mx - ex) + (my - ey) * (my - ey);
                           if (dd < best) {
                              best = dd;
                              List<Text> tl = new ArrayList<>();
                              tl.add(Text.literal((e.year() > 0 ? "Год " + e.year() + ", " : "") + "день " + e.day()).formatted(Formatting.GOLD));
                              if (ek != null) {
                                 tl.add(Text.literal(ek.title).withColor(ek.rgb));
                              }

                              for (StringVisitable sv : tr.getTextHandler().wrapLines(e.text(), 220, Style.EMPTY)) {
                                 tl.add(Text.literal(sv.getString()));
                              }

                              pinTip = tl;
                           }
                        }
                     }
                  }
               }
            }

            int ppx = x0 + (int)((mc.player.getX() - minX) * scale);
            int ppy = y0 + (int)((mc.player.getZ() - minZ) * scale);
            if (ppx >= x0 && ppy >= y0 && ppx < x0 + s && ppy < y0 + s) {
               double yr = Math.toRadians(mc.player.getYaw());

               for (int i = 0; i < 7; i++) {
                  int ax = ppx + (int)(-Math.sin(yr) * i);
                  int az = ppy + (int)(Math.cos(yr) * i);
                  ctx.fill(ax - 1, az - 1, ax + 2, az + 2, -49088);
               }

               ctx.fill(ppx - 2, ppy - 2, ppx + 3, ppy + 3, -1);
               small(ctx, tr, "Вы", ppx, ppy + 5, -28528, 0.75F);
            }

            int rx = x0 + s - 22;
            int ry = y0 + 22;
            ctx.fill(rx - 15, ry - 15, rx + 16, ry + 16, -1877995504);
            ctx.fill(rx, ry - 12, rx + 1, ry + 13, -3562148);
            ctx.fill(rx - 12, ry, rx + 13, ry + 1, -3562148);
            ctx.fill(rx - 1, ry - 12, rx + 2, ry - 2, -49088);
            small(ctx, tr, "С", rx + 1, ry - 13 - 1, -40864, 0.6F);
            small(ctx, tr, "Ю", rx + 1, ry + 9, -1, 0.6F);
            small(ctx, tr, "З", rx - 11, ry - 2, -1, 0.6F);
            small(ctx, tr, "В", rx + 12, ry - 2, -1, 0.6F);
            int[] nice = new int[]{10, 25, 50, 100, 250, 500, 1000, 2500};
            int blocks = nice[nice.length - 1];

            for (int nb : nice) {
               if (nb * scale >= 40.0) {
                  blocks = nb;
                  break;
               }
            }

            int bw = (int)(blocks * scale);
            int bx0 = x0 + 8;
            int by0 = y0 + s - 14;
            ctx.fill(bx0 - 3, by0 - 10, bx0 + bw + 4, by0 + 6, -1877995504);
            ctx.fill(bx0, by0, bx0 + bw, by0 + 2, -1);
            ctx.fill(bx0, by0 - 3, bx0 + 1, by0 + 3, -1);
            ctx.fill(bx0 + bw - 1, by0 - 3, bx0 + bw, by0 + 3, -1);
            small(ctx, tr, blocks + " бл.", bx0 + bw / 2, by0 - 9, -1, 0.7F);
            String title = "Карта мира — колесо: зум, перетаскивание: сдвиг, клик: перелететь, N/Esc: закрыть";
            if (replay) {
               title = "История мира — пробел: пуск/пауза, ←/→ или колесо на шкале: кадры, Esc: назад к карте";
            }

            ctx.drawCenteredTextWithShadow(tr, tr.trimToWidth(title, width - 10), width / 2, y0 - 18, -10496);
            history(ctx, tr, fr, x0, y0, s, mx, my, now);
            if (ZONES.isEmpty() && !replay) {
               ctx.drawCenteredTextWithShadow(tr, "Королевств пока нет — создайте их во вкладке «Королевства»", x0 + s / 2, y0 + s / 2, -2236963);
            }

            int lgx = legendW > 0 ? x0 + s + 10 : x0 + 4;
            int lgy = legendW > 0 ? y0 : y0 + 40;
            int lgw = legendW > 0 ? legendW - 6 : Math.min(s - 8, 200);
            if (!LEGEND.isEmpty()) {
               int rowH = LEGEND.size() > 6 ? 13 : 24;
               int shown = LEGEND.size();
               if (legendW == 0) {
                  int room = y0 + s - 4 - lgy - 48;
                  if (shown * rowH > room) {
                     rowH = 13;
                  }

                  shown = Math.max(0, Math.min(shown, room / rowH));
               }

               int lh = 14 + shown * rowH + 34;
               ctx.fill(lgx - 2, lgy - 2, lgx + lgw + 2, lgy + lh + 2, -9744860);
               ctx.fill(lgx, lgy, lgx + lgw, lgy + lh, -535555568);
               ctx.drawTextWithShadow(tr, "Королевства", lgx + 5, lgy + 4, -10496);
               int yy = lgy + 16;

               for (int i = 0; i < shown; i++) {
                  String l = LEGEND.get(i);
                  int col = 0xFF000000 | LEGEND_RGB.get(i);
                  ctx.fill(lgx + 4, yy, lgx + 7, yy + rowH - 4, col);
                  List<OrderedText> ls2 = tr.wrapLines(Text.literal(l), (int)((lgw - 14) / 0.75F));

                  for (int k = 0; k < Math.min(rowH > 13 ? 2 : 1, ls2.size()); k++) {
                     ctx.getMatrices().push();
                     ctx.getMatrices().translate((float)(lgx + 10), (float)(yy + k * 9), 0.0F);
                     ctx.getMatrices().scale(0.75F, 0.75F, 1.0F);
                     ctx.drawTextWithShadow(tr, ls2.get(k), 0, 0, k == 0 ? -1 : -5592406);
                     ctx.getMatrices().pop();
                  }

                  yy += rowH;
               }

               ctx.fill(lgx + 4, yy + 4, lgx + 14, yy + 5, -53200);
               small(ctx, tr, "война", lgx + 34, yy + 1, -32640, 0.7F);
               ctx.fill(lgx + 4, yy + 16, lgx + 14, yy + 17, -10432257);
               small(ctx, tr, "союз", lgx + 32, yy + 13, -8333057, 0.7F);
            }

            if (mx >= x0 && my >= y0 && mx < x0 + s && my < y0 + s) {
               int wx = (int)Math.floor(minX + (mx - x0) / scale);
               int wz = (int)Math.floor(minZ + (my - y0) / scale);
               if (pinTip != null) {
                  ctx.drawTooltip(tr, pinTip, mx, my);
               } else if (hov != null) {
                  ctx.drawTooltip(
                     tr,
                     List.of(
                        Text.literal((hov.cap ? "♛ Столица " : "Город ") + hov.name).formatted(Formatting.GOLD),
                        Text.literal(hov.kingdom).withColor(hov.rgb),
                        Text.literal("Эра развития: " + (hov.era + 1) + "   радиус " + hov.r + " бл.").formatted(Formatting.GRAY),
                        Text.literal(hov.x + ", " + hov.z + (replay ? "" : " — клик: перелететь")).formatted(Formatting.DARK_GRAY)
                     ),
                     mx,
                     my
                  );
               } else {
                  String owner = "ничья земля";
                  int o = own(ZONES, wx, wz);

                  for (WorldMap.Zone znx : ZONES) {
                     if (znx.cap && znx.rgb == o) {
                        owner = znx.kingdom.isEmpty() ? znx.name : znx.kingdom;
                     }
                  }

                  String info = wx + ", " + wz + " — " + owner;
                  int iw = tr.getWidth(info) + 8;
                  ctx.fill(x0 + s / 2 - iw / 2, y0 + s - 30, x0 + s / 2 + iw / 2, y0 + s - 18, -1072689136);
                  ctx.drawCenteredTextWithShadow(tr, info, x0 + s / 2, y0 + s - 28, -1);
               }
            }
         }
      }
   }

   static int[] click(double mx, double my) {
      if (pinChip != null && !replay && mx >= pinChip[0] && mx < pinChip[0] + pinChip[2] && my >= pinChip[1] && my < pinChip[1] + pinChip[3]) {
         pinsOn = !pinsOn;
         ChronicleScreen.pinsAt = 0L;
         return null;
      } else if (ls > 0 && my >= barY && my < barY + 14) {
         if (mx >= hbX0 && mx < hbX1) {
            if (replay) {
               exitReplay();
            } else {
               enterReplay();
            }
         } else if (replay && FRAMES.size() >= 2) {
            if (mx >= pbX0 && mx < pbX1) {
               togglePlay();
            } else if (mx >= spX0 && mx < spX1) {
               speed = speed >= 4 ? 1 : speed * 2;
            } else if (mx >= sx0 - 3 && mx < sx1 + 3 && sx1 > sx0) {
               scrubbing = true;
               playing = false;
               go(frameAt(mx));
            }
         }

         return null;
      } else if (replay) {
         return null;
      } else {
         return ls != 0 && !(mx < lx) && !(my < ly) && !(mx >= lx + ls) && !(my >= ly + ls)
            ? new int[]{(int)Math.floor(lminX + (mx - lx) / lscale), (int)Math.floor(lminZ + (my - ly) / lscale)}
            : null;
      }
   }

   static void openReplay() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (!on && mc.player != null) {
         center(mc.player.getX(), mc.player.getZ());
      }

      on = true;
      enterReplay();
   }

   private static void enterReplay() {
      replay = true;
      playing = false;
      scrubbing = false;
      fi = 0;
      caps = List.of();
      capFrame = -1;
      long now = System.currentTimeMillis();
      lastRender = now;
      unsupported = !ClientPlayNetworking.canSend(HistoryPayloads.HistoryRequest.ID);
      if (!unsupported) {
         if (now - lastAsk < 2000L) {
            if (!loading) {
               playing = FRAMES.size() >= 2;
               go(0);
            }
         } else {
            FRAMES.clear();
            loading = true;
            lastAsk = now;
            ClientPlayNetworking.send(new HistoryPayloads.HistoryRequest());
         }
      }
   }

   private static void exitReplay() {
      replay = false;
      playing = false;
      scrubbing = false;
   }

   static boolean key(int key) {
      if (!replay) {
         return false;
      } else {
         switch (key) {
            case 32:
               togglePlay();
               break;
            case 256:
               exitReplay();
               break;
            case 262:
               playing = false;
               go(fi + 1);
               break;
            case 263:
               playing = false;
               go(fi - 1);
               break;
            case 268:
               playing = false;
               go(0);
               break;
            case 269:
               playing = false;
               go(FRAMES.size() - 1);
               break;
            default:
               return false;
         }

         return true;
      }
   }

   private static void togglePlay() {
      if (FRAMES.size() >= 2) {
         playing = !playing;
         playAcc = 0.0;
         if (playing && fi >= FRAMES.size() - 1) {
            go(0);
         }
      }
   }

   private static void go(int i) {
      if (!FRAMES.isEmpty()) {
         fi = MathHelper.clamp(i, 0, FRAMES.size() - 1);
         WorldMap.Frame f = FRAMES.get(fi);
         if (!f.notes.isEmpty() && capFrame != fi) {
            caps = f.notes;
            capFrame = fi;
            capSince = System.currentTimeMillis();
         }
      }
   }

   private static int frameAt(double mx) {
      return sx1 > sx0 && FRAMES.size() >= 2 ? (int)Math.round(MathHelper.clamp((mx - sx0) / (sx1 - sx0), 0.0, 1.0) * (FRAMES.size() - 1)) : 0;
   }

   static void history(int index, int total, String data) {
      if (index == 0) {
         INCOMING.clear();
      }

      for (String line : data.split("\n")) {
         WorldMap.Frame f = parse(line);
         if (f != null) {
            INCOMING.add(f);
         }
      }

      if (index >= total - 1) {
         FRAMES.clear();
         FRAMES.addAll(INCOMING);
         INCOMING.clear();
         loading = false;
         fi = 0;
         caps = List.of();
         capFrame = -1;
         playAcc = 0.0;
         playing = replay && FRAMES.size() >= 2;
         go(0);
         if (replay && FRAMES.size() >= 2) {
            fitHistory();
         }
      }
   }

   private static WorldMap.Frame parse(String line) {
      String[] a = line.split("\\|", -1);

      try {
         if (a.length < 4) {
            return null;
         } else {
            int n = Integer.parseInt(a[3]);
            if (n >= 0 && 4 + n <= a.length) {
               List<WorldMap.Zone> zs = new ArrayList<>();
               List<String> leg = new ArrayList<>();
               List<Integer> legRgb = new ArrayList<>();

               for (int i = 0; i < n; i++) {
                  String[] k = a[4 + i].split(";", -1);
                  if (k.length >= 7) {
                     int rgb = Integer.parseInt(k[1]);
                     int pop = Integer.parseInt(k[2]);
                     int flags = Integer.parseInt(k[3]);
                     int era = Integer.parseInt(k[4]);
                     String title = k[5].isEmpty() ? k[0] : k[5];
                     int cities = 0;

                     for (int j = 6; j < k.length; j++) {
                        String[] c = k[j].split(",", -1);
                        if (c.length >= 3) {
                           zs.add(
                              new WorldMap.Zone(
                                 Integer.parseInt(c[0]),
                                 Integer.parseInt(c[1]),
                                 Math.max(1, Integer.parseInt(c[2])),
                                 rgb,
                                 c.length > 3 ? c[3] : "",
                                 cities == 0,
                                 title,
                                 era
                              )
                           );
                           cities++;
                        }
                     }

                     if (cities != 0) {
                        legRgb.add(rgb);
                        leg.add(
                           title
                              + ": эра "
                              + (era + 1)
                              + ", жителей "
                              + pop
                              + ", городов "
                              + cities
                              + ((flags & 2) != 0 ? ", вассал" : ((flags & 4) != 0 ? ", ИМПЕРИЯ" : ""))
                              + ((flags & 1) != 0 ? ", ⚔ война" : "")
                        );
                     }
                  }
               }

               List<String> notes = new ArrayList<>();

               for (int ix = 4 + n; ix < a.length; ix++) {
                  if (!a[ix].isBlank()) {
                     notes.add(a[ix]);
                  }
               }

               return new WorldMap.Frame(Long.parseLong(a[1]), Long.parseLong(a[2]), List.copyOf(zs), List.copyOf(leg), List.copyOf(legRgb), List.copyOf(notes));
            } else {
               return null;
            }
         }
      } catch (NumberFormatException var16) {
         return null;
      }
   }

   private static void fitHistory() {
      double minX = cX - span / 2.0;
      double maxX = cX + span / 2.0;
      double minZ = cZ - span / 2.0;
      double maxZ = cZ + span / 2.0;
      double x1 = minX;
      double x2 = maxX;
      double z1 = minZ;
      double z2 = maxZ;

      for (WorldMap.Frame f : FRAMES) {
         for (WorldMap.Zone zn : f.zones) {
            x1 = Math.min(x1, (double)(zn.x - zn.r));
            x2 = Math.max(x2, (double)(zn.x + zn.r));
            z1 = Math.min(z1, (double)(zn.z - zn.r));
            z2 = Math.max(z2, (double)(zn.z + zn.r));
         }
      }

      if (x1 != minX || x2 != maxX || z1 != minZ || z2 != maxZ) {
         cX = (x1 + x2) / 2.0;
         cZ = (z1 + z2) / 2.0;
         span = MathHelper.clamp(Math.max(x2 - x1, z2 - z1) * 1.05, 96.0, 8192.0);
      }
   }

   private static void button(DrawContext ctx, TextRenderer tr, int x0, int x1, String label, boolean hover, boolean active) {
      ctx.fill(x0, barY, x1, barY + 14, active ? -10496 : (hover ? -3562148 : -9744860));
      ctx.fill(x0 + 1, barY + 1, x1 - 1, barY + 13, active ? -11912684 : (hover ? -13424358 : -267120112));
      ctx.drawCenteredTextWithShadow(tr, label, (x0 + x1) / 2, barY + 3, !active && !hover ? -2236963 : -6528);
   }

   private static void history(DrawContext ctx, TextRenderer tr, WorldMap.Frame fr, int x0, int y0, int s, int mx, int my, long now) {
      barY = y0 + s + 8;
      ctx.fill(x0 - 5, y0 + s + 5, x0 + s + 5, barY + 16, -267120112);
      boolean row = my >= barY && my < barY + 14;
      String hl = replay ? "✖ Карта" : "⏳ История";
      hbX0 = x0 - 5;
      hbX1 = hbX0 + tr.getWidth(hl) + 12;
      button(ctx, tr, hbX0, hbX1, hl, row && mx >= hbX0 && mx < hbX1, replay);
      if (!replay) {
         if (row && mx >= hbX0 && mx < hbX1) {
            ctx.drawTooltip(
               tr,
               List.of(
                  Text.literal("История мира").formatted(Formatting.GOLD), Text.literal("Показать, как менялись границы королевств").formatted(Formatting.GRAY)
               ),
               mx,
               my
            );
         }
      } else if (fr == null) {
         String m = unsupported ? "Сервер не поддерживает историю мира" : (loading ? "Загружаю историю мира…" : "История ещё не записана…");
         String m2 = !unsupported && !loading ? "Мир запоминает границы раз в минуту, пока живо хотя бы одно королевство" : "";
         int w = Math.min(s - 8, Math.max(tr.getWidth(m), (int)(tr.getWidth(m2) * 0.75F)) + 16);
         int cy = y0 + s / 2;
         ctx.fill(x0 + s / 2 - w / 2, cy - 14, x0 + s / 2 + w / 2, cy + 16, -804253680);
         ctx.drawCenteredTextWithShadow(tr, m, x0 + s / 2, cy - 9, -6528);
         if (!m2.isEmpty()) {
            small(ctx, tr, tr.trimToWidth(m2, (int)((s - 16) / 0.75F)), x0 + s / 2, cy + 4, -3355444, 0.75F);
         }
      } else {
         int n = FRAMES.size();
         pbX0 = hbX1 + 3;
         pbX1 = pbX0 + 16;
         button(ctx, tr, pbX0, pbX1, playing ? "⏸" : "▶", row && mx >= pbX0 && mx < pbX1, playing);
         spX0 = pbX1 + 3;
         spX1 = spX0 + 22;
         button(ctx, tr, spX0, spX1, "×" + speed, row && mx >= spX0 && mx < spX1, false);
         sx0 = spX1 + 9;
         sx1 = x0 + s;
         if (sx1 - sx0 >= 16) {
            ctx.fill(sx0 - 5, barY, sx1 + 5, barY + 14, -9744860);
            ctx.fill(sx0 - 4, barY + 1, sx1 + 4, barY + 13, -267120112);
            int kx = sx0 + (int)((sx1 - sx0) * ((double)fi / (n - 1)));
            ctx.fill(sx0, barY + 6, sx1, barY + 8, -11912672);
            ctx.fill(sx0, barY + 6, kx, barY + 8, -3562148);
            if (n <= (sx1 - sx0) / 2) {
               for (int i = 0; i < n; i++) {
                  if (!FRAMES.get(i).notes.isEmpty()) {
                     int tx = sx0 + (int)((sx1 - sx0) * ((double)i / (n - 1)));
                     ctx.fill(tx, barY + 3, tx + 1, barY + 5, -10496);
                  }
               }
            }

            ctx.fill(kx - 3, barY + 1, kx + 3, barY + 13, -16777216);
            ctx.fill(kx - 2, barY + 2, kx + 2, barY + 12, scrubbing ? -10496 : -1);
         }

         String date = "Год " + fr.year + " · день " + fr.day;
         String sub = "кадр " + (fi + 1) + " из " + n + (playing ? "  ▶ ×" + speed : "  пауза");
         int dw = Math.max(tr.getWidth(date), (int)(tr.getWidth(sub) * 0.7F)) + 12;
         int dx = x0 + 4;
         int dy = y0 + 10;
         ctx.fill(dx, dy, dx + dw, dy + 23, -804253680);
         ctx.fill(dx, dy + 22, dx + dw, dy + 23, -3562148);
         ctx.drawTextWithShadow(tr, date, dx + 6, dy + 3, -6528);
         small(ctx, tr, sub, dx + dw / 2, dy + 14, -4473925, 0.7F);
         long age = now - capSince;
         double al = Math.min(1.0, age / 300.0);
         if (capFrame != fi && age > 3500L) {
            al = Math.max(0.0, 1.0 - (age - 3500L) / 800.0);
         }

         int alpha = (int)(al * 255.0);
         if (!caps.isEmpty() && alpha >= 16) {
            int cw = Math.min(s - 60, 250);
            List<OrderedText> lines = new ArrayList<>();

            for (String c : caps) {
               List<OrderedText> wl = tr.wrapLines(Text.literal(c), (int)((cw - 8) / 0.75F));

               for (int k = 0; k < Math.min(2, wl.size()); k++) {
                  lines.add(wl.get(k));
               }
            }

            int cy = dy + 27;
            ctx.fill(dx, cy, dx + cw, cy + lines.size() * 8 + 5, alpha * 3 / 4 << 24 | 1052688);

            for (int k = 0; k < lines.size(); k++) {
               ctx.getMatrices().push();
               ctx.getMatrices().translate((float)(dx + 4), (float)(cy + 3 + k * 8), 0.0F);
               ctx.getMatrices().scale(0.75F, 0.75F, 1.0F);
               ctx.drawTextWithShadow(tr, lines.get(k), 0, 0, alpha << 24 | 16777215);
               ctx.getMatrices().pop();
            }
         }

         if (row && mx >= sx0 - 3 && mx < sx1 + 3 && sx1 - sx0 >= 16) {
            WorldMap.Frame h = FRAMES.get(frameAt(mx));
            ctx.drawTooltip(tr, Text.literal("Год " + h.year + " · день " + h.day), mx, my - 12);
         }
      }
   }

   record Frame(long year, long day, List<WorldMap.Zone> zones, List<String> legend, List<Integer> legendRgb, List<String> notes) {
   }

   record Zone(int x, int z, int r, int rgb, String name, boolean cap, String kingdom, int era) {
   }
}
