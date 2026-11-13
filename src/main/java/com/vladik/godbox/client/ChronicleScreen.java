package com.vladik.godbox.client;

import com.vladik.godbox.Chronicle;
import com.vladik.godbox.Kingdoms;
import com.vladik.godbox.net.ChroniclePayloads;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

public class ChronicleScreen extends Screen {
   private static final String[] SEASONS = new String[]{"Весна", "Лето", "Осень", "Зима"};
   private static final Kingdoms.K[] KS = Kingdoms.K.values();
   private static final Chronicle.Type[] TYPES = Chronicle.Type.values();
   private static final int WORLD_BIT = 12;
   static List<ChroniclePayloads.Dto> pins = List.of();
   static long pinsAt;
   private static int kMask;
   private static int tMask;
   private static int tab;
   private static boolean majorOnly;
   private static String query = "";
   private final boolean fromGod;
   private TextFieldWidget field;
   private final List<ChronicleScreen.Chip> chips = new ArrayList<>();
   private int px;
   private int py;
   private int pw;
   private int ph;
   private int listX;
   private int listY;
   private int listW;
   private int listH;
   private int tabY;
   private final List<ChroniclePayloads.Dto> loaded = new ArrayList<>();
   private List<ChronicleScreen.Row> rows = new ArrayList<>();
   private int contentH;
   private int scroll;
   private int pages = 1;
   private int total;
   private int nextPage;
   private boolean loading;
   private boolean dragBar;
   private long sentAt;
   private long dirtyAt;
   private long sumSentAt;
   private boolean noNet;
   private List<String> sum = List.of();
   private String sumFor = "";
   private boolean sumWait;
   private List<OrderedText> sumLines = List.of();

   private ChronicleScreen(boolean fromGod) {
      super(Text.literal("Летопись"));
      this.fromGod = fromGod;
   }

   static void register() {
      ClientPlayNetworking.registerGlobalReceiver(ChroniclePayloads.Open.ID, (p, ctx) -> ctx.client().execute(ChronicleScreen::open));
      ClientPlayNetworking.registerGlobalReceiver(ChroniclePayloads.Page.ID, (p, ctx) -> ctx.client().execute(() -> {
         if (ctx.client().currentScreen instanceof ChronicleScreen s) {
            s.onPage(p);
         }
      }));
      ClientPlayNetworking.registerGlobalReceiver(ChroniclePayloads.Sum.ID, (p, ctx) -> ctx.client().execute(() -> {
         if (ctx.client().currentScreen instanceof ChronicleScreen s) {
            s.onSum(p);
         }
      }));
      ClientPlayNetworking.registerGlobalReceiver(ChroniclePayloads.Pins.ID, (p, ctx) -> ctx.client().execute(() -> pins = List.copyOf(p.entries())));
      ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> {
         pins = List.of();
         pinsAt = 0L;
      });
   }

   public static void open() {
      MinecraftClient c = MinecraftClient.getInstance();
      if (c.player != null && !(c.currentScreen instanceof ChronicleScreen)) {
         boolean god = c.currentScreen instanceof GodScreen;
         if (god || c.currentScreen == null) {
            double mx = c.mouse.getX();
            double my = c.mouse.getY();
            if (c.currentScreen instanceof GodScreen gs) {
               gs.closeSilently();
            }

            c.setScreen(new ChronicleScreen(god));
            InputUtil.setCursorParameters(c.getWindow().getHandle(), 212993, mx, my);
         }
      }
   }

   @Override
   public void close() {
      if (this.client != null) {
         this.client.setScreen(this.fromGod && this.client.player != null ? new GodScreen() : null);
      }
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   protected void init() {
      this.pw = Math.min(this.width - 16, 700);
      this.px = (this.width - this.pw) / 2;
      this.py = 8;
      this.ph = this.height - 16;
      this.field = new TextFieldWidget(this.textRenderer, this.px + 10, this.py + 22, Math.max(60, Math.min(160, this.pw / 3)), 14, Text.literal("Поиск"));
      this.field.setMaxLength(40);
      this.field.setPlaceholder(Text.literal("Поиск…").formatted(Formatting.DARK_GRAY));
      this.field.setText(query);
      this.field.setChangedListener(s -> {
         query = s;
         this.dirtyAt = System.currentTimeMillis();
      });
      this.addSelectableChild(this.field);
      this.chips.clear();
      int right = this.px + this.pw - 10;
      int tw = Math.min(70, (this.pw - 20) / 2 - 80);
      this.tabY = this.py + 5;
      if (tw > 30) {
         this.chips.add(new ChronicleScreen.Chip(right - tw * 2 - 2, this.tabY, tw, 14, 4, 0, "Записи"));
         this.chips.add(new ChronicleScreen.Chip(right - tw, this.tabY, tw, 14, 5, 0, "Сводка"));
      }

      int fx = this.field.getX() + this.field.getWidth() + 6;
      String maj = "★ Важные";
      int mw = this.textRenderer.getWidth(maj) + 12;
      this.chips.add(new ChronicleScreen.Chip(fx, this.py + 21, mw, 16, 2, 0, maj));
      int x = this.px + 10;
      int y = this.py + 41;
      int row = 18;

      for (int i = 0; i <= 12; i++) {
         if (x + 16 > right) {
            x = this.px + 10;
            y += row;
         }

         this.chips.add(new ChronicleScreen.Chip(x, y, 16, 16, 0, i, i == 12 ? "События мира" : KS[i].title));
         x += 18;
      }

      x += 4;

      for (int i = 0; i < TYPES.length; i++) {
         int w = this.textRenderer.getWidth(TYPES[i].label) + 10;
         if (x + w > right) {
            x = this.px + 10;
            y += row;
         }

         this.chips.add(new ChronicleScreen.Chip(x, y, w, 16, 1, i, TYPES[i].label));
         x += w + 2;
      }

      this.listX = this.px + 8;
      this.listY = y + row + 2;
      this.listW = this.pw - 16 - 8;
      this.listH = Math.max(20, this.py + this.ph - 8 - this.listY);
      if (this.loaded.isEmpty() && !this.loading) {
         this.refresh();
      } else {
         this.rebuild();
      }
   }

   private void refresh() {
      this.nextPage = 0;
      this.scroll = 0;
      if (tab == 1) {
         this.askSum();
      } else {
         this.loading = true;
         this.sendPage(0);
      }
   }

   private void sendPage(int page) {
      this.sentAt = System.currentTimeMillis();
      if (ClientPlayNetworking.canSend(ChroniclePayloads.PageReq.ID)) {
         ClientPlayNetworking.send(new ChroniclePayloads.PageReq(page, kMask, tMask, majorOnly, query));
      } else {
         this.loading = false;
         this.noNet = true;
      }
   }

   private int sumKingdom() {
      for (int i = 0; i < KS.length; i++) {
         if ((kMask & 1 << i) != 0) {
            return i;
         }
      }

      return -1;
   }

   private void askSum() {
      int i = this.sumKingdom();
      this.sumFor = i < 0 ? "" : KS[i].id;
      this.sum = List.of();
      this.sumLines = List.of();
      this.sumWait = i >= 0;
      this.scroll = 0;
      if (i >= 0) {
         this.sendSum();
      }
   }

   private void sendSum() {
      this.sumSentAt = System.currentTimeMillis();
      if (ClientPlayNetworking.canSend(ChroniclePayloads.SumReq.ID)) {
         ClientPlayNetworking.send(new ChroniclePayloads.SumReq(this.sumFor));
      } else {
         this.sumWait = false;
         this.noNet = true;
      }
   }

   private void onPage(ChroniclePayloads.Page p) {
      if (p.page() == 0) {
         this.loaded.clear();
      } else if (p.page() != this.nextPage) {
         return;
      }

      this.loaded.addAll(p.entries());
      this.pages = p.pages();
      this.total = p.total();
      this.nextPage = p.page() + 1;
      this.loading = false;
      this.rebuild();
   }

   private void onSum(ChroniclePayloads.Sum p) {
      if (p.k().equals(this.sumFor)) {
         this.sum = p.lines();
         this.sumWait = false;
         this.rebuild();
      }
   }

   private void rebuild() {
      int textW = this.listW - 8 - 18 - 34 - 6;
      List<ChronicleScreen.Row> out = new ArrayList<>();
      int y = 0;
      int lastYear = Integer.MIN_VALUE;
      int lastSeason = -2;

      for (ChroniclePayloads.Dto e : this.loaded) {
         if (e.year() != lastYear || e.season() != lastSeason) {
            lastYear = e.year();
            lastSeason = e.season();
            String h = e.year() <= 0 ? "Старые записи" : "Год " + e.year() + (e.season() >= 0 && e.season() < 4 ? " · " + SEASONS[e.season()] : "");
            out.add(new ChronicleScreen.Row(null, h, List.of(), y, 15));
            y += 15;
         }

         List<OrderedText> ls = new ArrayList<>(this.textRenderer.wrapLines(Text.literal(e.text()), Math.max(40, textW)));
         if (ls.size() > 4) {
            ls = new ArrayList<>(ls.subList(0, 4));
         }

         int h = Math.max(16, ls.size() * 10 + 6);
         out.add(new ChronicleScreen.Row(e, null, ls, y, h));
         y += h;
      }

      this.rows = out;
      int sumH = 0;
      if (!this.sum.isEmpty()) {
         List<OrderedText> sl = new ArrayList<>();

         for (String s : this.sum) {
            if (s.startsWith("#")) {
               sl.add(null);
            }

            sl.addAll(this.textRenderer.wrapLines(Text.literal(s.startsWith("#") ? s.substring(1) : s), Math.max(40, this.listW - 50)));
         }

         this.sumLines = sl;
         sumH = sl.size() * 11 + 8;
      }

      this.contentH = tab == 1 ? sumH : y;
      this.scroll = MathHelper.clamp(this.scroll, 0, Math.max(0, this.contentH - this.listH));
   }

   @Override
   public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
      ctx.fill(0, 0, this.width, this.height, -1879048192);
   }

   @Override
   public void render(DrawContext ctx, int mx, int my, float delta) {
      this.renderBackground(ctx, mx, my, delta);
      long now = System.currentTimeMillis();
      if (this.dirtyAt > 0L && now - this.dirtyAt > 350L) {
         this.dirtyAt = 0L;
         if (tab == 0) {
            this.refresh();
         }
      }

      if (tab == 0 && this.loading && now - this.sentAt > 1500L) {
         this.sendPage(this.nextPage);
      }

      if (tab == 1 && this.sumWait && now - this.sumSentAt > 1500L) {
         this.sendSum();
      }

      if (tab == 0 && !this.noNet && !this.loading && this.nextPage < this.pages && this.contentH - this.scroll - this.listH < 120) {
         this.loading = true;
         this.sendPage(this.nextPage);
      }

      Art.panel(ctx, this.px, this.py, this.pw, this.ph);
      ctx.drawTextWithShadow(this.textRenderer, "Летопись мира", this.px + 10, this.py + 8, -10496);
      if (tab == 0 && this.total > 0 && this.pw > 300) {
         ctx.drawTextWithShadow(this.textRenderer, this.total + " зап.", this.px + 10 + this.textRenderer.getWidth("Летопись мира") + 8, this.py + 8, -8355696);
      }

      this.field.render(ctx, mx, my, delta);
      List<Text> tip = null;

      for (ChronicleScreen.Chip c : this.chips) {
         boolean hov = Ui.in(mx, my, c.x, c.y, c.w, c.h);

         boolean act = switch (c.kind) {
            case 0 -> (kMask & 1 << c.idx) != 0;
            case 1 -> (tMask & 1 << c.idx) != 0;
            case 2 -> majorOnly;
            default -> tab == 1;
            case 4 -> tab == 0;
         };
         Art.button(ctx, c.x, c.y, c.w, c.h, hov, act);
         if (c.kind == 0) {
            if (c.idx == 12) {
               ctx.drawCenteredTextWithShadow(this.textRenderer, "М", c.x + c.w / 2, c.y + 4, act ? -6528 : -2236963);
            } else {
               Art.emblem(ctx, KS[c.idx], c.x + 2, c.y + 2, 12);
            }

            if (hov) {
               tip = List.of(Text.literal(c.label));
            }
         } else {
            int col = c.kind == 1 ? 0xFF000000 | TYPES[c.idx].rgb : (act ? -6528 : -2236963);
            ctx.drawCenteredTextWithShadow(this.textRenderer, c.label, c.x + c.w / 2, c.y + (c.h - 8) / 2, act && c.kind == 1 ? -1 : col);
         }
      }

      ctx.enableScissor(this.listX, this.listY, this.listX + this.listW + 8, this.listY + this.listH);
      ChroniclePayloads.Dto hover = null;
      if (tab == 0) {
         hover = this.renderEntries(ctx, mx, my);
      } else {
         this.renderSummary(ctx);
      }

      ctx.disableScissor();
      this.renderBar(ctx);
      if (hover != null) {
         List<Text> t = new ArrayList<>();
         t.add(Text.literal((hover.year() > 0 ? "Год " + hover.year() + ", " : "") + "день " + hover.day()).formatted(Formatting.GOLD));
         t.add(Text.literal(TYPES[MathHelper.clamp(hover.type(), 0, TYPES.length - 1)].label + (hover.major() ? " · важное" : "")).formatted(Formatting.GRAY));
         t.add(Text.literal(hover.hasPos() ? "Клик: перелететь (" + hover.x() + ", " + hover.z() + ")" : "Место не указано").formatted(Formatting.DARK_GRAY));
         ctx.drawTooltip(this.textRenderer, t, mx, my);
      } else if (tip != null) {
         ctx.drawTooltip(this.textRenderer, tip, mx, my);
      }
   }

   private ChroniclePayloads.Dto renderEntries(DrawContext ctx, int mx, int my) {
      ChroniclePayloads.Dto hover = null;
      if (this.rows.isEmpty()) {
         String m = this.noNet
            ? "Сервер не поддерживает летопись"
            : (this.loading ? "Загрузка…" : ((kMask | tMask) == 0 && !majorOnly && query.isEmpty() ? "Летопись пока пуста" : "Ничего не найдено"));
         ctx.drawCenteredTextWithShadow(this.textRenderer, m, this.listX + this.listW / 2, this.listY + Math.min(40, this.listH / 2), -8355696);
         return null;
      } else {
         for (ChronicleScreen.Row r : this.rows) {
            int y = this.listY + r.y - this.scroll;
            if (y + r.h >= this.listY && y <= this.listY + this.listH) {
               if (r.e == null) {
                  ctx.fill(this.listX, y + 2, this.listX + this.listW, y + 13, 1622705675);
                  ctx.drawTextWithShadow(this.textRenderer, Ui.ellipsis(this.textRenderer, r.header, this.listW - 8), this.listX + 5, y + 3, -6528);
               } else {
                  ChroniclePayloads.Dto e = r.e;
                  Kingdoms.K k = Kingdoms.byId(e.k());
                  int col = k != null ? 0xFF000000 | k.rgb : -8355696;
                  boolean hov = my >= this.listY && my < this.listY + this.listH && Ui.in(mx, my, this.listX, y, this.listW, r.h);
                  ctx.fill(this.listX, y, this.listX + this.listW, y + r.h - 1, hov && e.hasPos() ? 1358954495 : 671088640);
                  ctx.fill(this.listX, y, this.listX + 2, y + r.h - 1, col);
                  if (k != null) {
                     Art.emblem(ctx, k, this.listX + 5, y + 2, 12);
                  }

                  int tx = this.listX + 5 + 14 + 2;
                  ctx.drawTextWithShadow(this.textRenderer, "д." + e.day(), tx, y + 3, -8355696);
                  int textX = tx + 34;
                  int tc = e.major() ? -1 : -5723976;

                  for (int i = 0; i < r.lines.size(); i++) {
                     ctx.drawTextWithShadow(this.textRenderer, r.lines.get(i), textX + (i == 0 && e.major() ? 9 : 0), y + 3 + i * 10, tc);
                  }

                  if (e.major()) {
                     ctx.drawTextWithShadow(this.textRenderer, "★", textX, y + 3, -10496);
                  }

                  int tcol = 0xFF000000 | TYPES[MathHelper.clamp(e.type(), 0, TYPES.length - 1)].rgb;
                  ctx.fill(this.listX + this.listW - 4, y + 2, this.listX + this.listW - 1, y + r.h - 3, tcol);
                  if (hov) {
                     hover = e;
                  }
               }
            }
         }

         return hover;
      }
   }

   private void renderSummary(DrawContext ctx) {
      int i = this.sumKingdom();
      if (i < 0) {
         ctx.drawCenteredTextWithShadow(
            this.textRenderer, "Выберите королевство среди значков выше", this.listX + this.listW / 2, this.listY + Math.min(40, this.listH / 2), -8355696
         );
      } else if (!this.sumWait && !this.sumLines.isEmpty()) {
         Art.emblem(ctx, KS[i], this.listX + this.listW - 38, this.listY + 4 - this.scroll, 32);
         int y = this.listY + 4 - this.scroll;
         boolean head = false;

         for (OrderedText t : this.sumLines) {
            if (t == null) {
               head = true;
               y += 3;
            } else {
               if (y + 10 >= this.listY && y <= this.listY + this.listH) {
                  if (head) {
                     ctx.fill(this.listX, y - 1, this.listX + this.listW - 40, y + 10, 1622705675);
                  }

                  ctx.drawTextWithShadow(this.textRenderer, t, this.listX + 5, y, head ? -6528 : -2236963);
               }

               head = false;
               y += 11;
            }
         }
      } else {
         ctx.drawCenteredTextWithShadow(
            this.textRenderer,
            this.noNet ? "Сервер не поддерживает летопись" : "Загрузка…",
            this.listX + this.listW / 2,
            this.listY + Math.min(40, this.listH / 2),
            -8355696
         );
      }
   }

   private int[] bar() {
      int max = Math.max(0, this.contentH - this.listH);
      if (max == 0) {
         return null;
      } else {
         int th = Math.max(14, this.listH * this.listH / Math.max(this.listH, this.contentH));
         int ty = this.listY + (int)((long)(this.listH - th) * this.scroll / max);
         return new int[]{this.listX + this.listW + 2, ty, 6, th};
      }
   }

   private void renderBar(DrawContext ctx) {
      int[] b = this.bar();
      if (b != null) {
         Art.scrollTrack(ctx, this.listX + this.listW + 2, this.listY, 6, this.listH);
         Art.scrollThumb(ctx, b[0], b[1], b[2], b[3]);
      }
   }

   private void setScrollFromBar(double my) {
      int[] b = this.bar();
      int max = Math.max(0, this.contentH - this.listH);
      if (b != null) {
         double k = (my - this.listY - b[3] / 2.0) / Math.max(1, this.listH - b[3]);
         this.scroll = (int)MathHelper.clamp(k * max, 0.0, (double)max);
      }
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      if (button == 0) {
         int[] b = this.bar();
         if (b != null && Ui.in(mx, my, this.listX + this.listW, this.listY, 10, this.listH)) {
            this.dragBar = true;
            this.setScrollFromBar(my);
            return true;
         }

         for (ChronicleScreen.Chip c : this.chips) {
            if (Ui.in(mx, my, c.x, c.y, c.w, c.h)) {
               Ui.click();
               this.chip(c);
               this.field.setFocused(false);
               this.setFocused(null);
               return true;
            }
         }

         if (tab == 0 && Ui.in(mx, my, this.listX, this.listY, this.listW, this.listH)) {
            for (ChronicleScreen.Row r : this.rows) {
               int y = this.listY + r.y - this.scroll;
               if (r.e != null && my >= y && my < y + r.h) {
                  this.field.setFocused(false);
                  this.setFocused(null);
                  if (r.e.hasPos() && ClientPlayNetworking.canSend(ChroniclePayloads.Go.ID)) {
                     ClientPlayNetworking.send(new ChroniclePayloads.Go(r.e.id()));
                     Ui.click();
                     this.close();
                  }

                  return true;
               }
            }
         }
      }

      return super.mouseClicked(mx, my, button);
   }

   private void chip(ChronicleScreen.Chip c) {
      switch (c.kind) {
         case 0:
            kMask = tab == 1 ? (kMask == 1 << c.idx ? 0 : 1 << c.idx) : kMask ^ 1 << c.idx;
            this.refresh();
            break;
         case 1:
            tMask = tMask ^ 1 << c.idx;
            this.refresh();
            break;
         case 2:
            majorOnly = !majorOnly;
            this.refresh();
            break;
         case 3:
         default:
            if (tab != 1) {
               tab = 1;
               int first = this.sumKingdom();
               kMask = first < 0 ? 0 : 1 << first;
               this.refresh();
            }
            break;
         case 4:
            if (tab != 0) {
               tab = 0;
               this.refresh();
            }
      }
   }

   @Override
   public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
      if (this.dragBar) {
         this.setScrollFromBar(my);
         return true;
      } else {
         return super.mouseDragged(mx, my, button, dx, dy);
      }
   }

   @Override
   public boolean mouseReleased(double mx, double my, int button) {
      this.dragBar = false;
      return super.mouseReleased(mx, my, button);
   }

   @Override
   public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
      this.scroll = MathHelper.clamp(this.scroll - (int)(vertical * 24.0), 0, Math.max(0, this.contentH - this.listH));
      return true;
   }

   @Override
   public boolean keyPressed(int key, int scan, int mods) {
      if (key == 256) {
         this.close();
         return true;
      } else {
         if (!this.field.isFocused()) {
            if (GodKeys.is(GodKeys.CHRONICLE, key, scan)) {
               this.close();
               return true;
            }

            int step = key == 267 ? this.listH - 20 : (key == 266 ? -(this.listH - 20) : (key == 264 ? 20 : (key == 265 ? -20 : 0)));
            if (step != 0) {
               this.scroll = MathHelper.clamp(this.scroll + step, 0, Math.max(0, this.contentH - this.listH));
               return true;
            }
         }

         return super.keyPressed(key, scan, mods);
      }
   }

   private record Chip(int x, int y, int w, int h, int kind, int idx, String label) {
   }

   private record Row(ChroniclePayloads.Dto e, String header, List<OrderedText> lines, int y, int h) {
   }
}
