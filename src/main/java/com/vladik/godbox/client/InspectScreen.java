package com.vladik.godbox.client;

import com.vladik.godbox.Kingdoms;
import com.vladik.godbox.net.InspectPayloads;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class InspectScreen extends Screen {
   static String followName;
   private static final int LW = 150;
   private static final int BW = 88;
   private static final int BH = 22;
   private static final int GAP = 5;
   private static final int ROW = 28;
   private static final int LINE = -7701926;
   private static final int LINE_SELF = -3629008;
   private static final String[] BANDS = new String[]{"Деды и бабки", "Родители", "Он сам, братья и сёстры", "Дети"};
   private static final String[] RELS = new String[]{"Дед или бабка", "Родитель", "Осматриваемый", "Брат или сестра", "Ребёнок"};
   private InspectPayloads.Profile p;
   private final boolean god;
   private final Deque<UUID> back = new ArrayDeque<>();
   private UUID cameFrom;
   private boolean goingBack;
   private int px;
   private int py;
   private int pw;
   private int ph;
   private int tx;
   private int ty;
   private int tw;
   private int th;
   private int linesTop;
   private int linesH;
   private final List<InspectScreen.Slot> slots = new ArrayList<>();
   private final List<int[]> wires = new ArrayList<>();
   private final List<int[]> captions = new ArrayList<>();
   private int treeH;
   private int treeScroll;
   private int linesScroll;
   private int linesTotal;
   private ButtonWidget backButton;

   private InspectScreen(InspectPayloads.Profile p, boolean god) {
      super(Text.literal("Осмотр существа"));
      this.p = p;
      this.god = god;
   }

   static void register() {
      ClientPlayNetworking.registerGlobalReceiver(InspectPayloads.Profile.ID, (payload, ctx) -> ctx.client().execute(() -> open(payload)));
      ClientPlayNetworking.registerGlobalReceiver(
         InspectPayloads.FollowState.ID, (payload, ctx) -> ctx.client().execute(() -> followName = payload.on() ? payload.name() : null)
      );
      ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> followName = null);
      ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
         if (screen instanceof GodScreen) {
            ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> followBadge(ctx, client.textRenderer, s.width));
            ScreenKeyboardEvents.afterKeyPress(screen).register((s, key, scan, mods) -> {
               if (key == 86 && followName != null && !GodScreen.typing()) {
                  stopFollow();
               }
            });
         }
      });
   }

   private static void stopFollow() {
      followName = null;
      if (ClientPlayNetworking.canSend(InspectPayloads.Follow.ID)) {
         ClientPlayNetworking.send(new InspectPayloads.Follow(new UUID(0L, 0L), false));
      }
   }

   private static void followBadge(DrawContext ctx, TextRenderer tr, int width) {
      if (followName != null) {
         String s = "\ud83d\udcf7 Слежка: " + followName + "  ·  V — прекратить";
         int w = tr.getWidth(s) + 14;
         int y = WorldPanels.bottom() + 24;
         if (Art.on()) {
            Art.tooltip(ctx, width / 2 - w / 2, y, w, 14);
         } else {
            ctx.fill(width / 2 - w / 2, y, width / 2 + w / 2, y + 14, -804253672);
            ctx.fill(width / 2 - w / 2, y + 13, width / 2 + w / 2, y + 14, -8323073);
         }

         ctx.drawCenteredTextWithShadow(tr, s, width / 2, y + 3, -8323073);
      }
   }

   private static void open(InspectPayloads.Profile p) {
      MinecraftClient c = MinecraftClient.getInstance();
      if (c.player != null) {
         if (c.currentScreen instanceof InspectScreen s) {
            s.set(p);
         } else {
            boolean god = c.currentScreen instanceof GodScreen;
            if (god || c.currentScreen == null) {
               double mx = c.mouse.getX();
               double my = c.mouse.getY();
               if (c.currentScreen instanceof GodScreen gs) {
                  gs.closeSilently();
               }

               c.setScreen(new InspectScreen(p, god));
               InputUtil.setCursorParameters(c.getWindow().getHandle(), 212993, mx, my);
            }
         }
      }
   }

   private void set(InspectPayloads.Profile np) {
      if (!this.goingBack && this.cameFrom != null && !this.cameFrom.equals(np.id())) {
         this.back.push(this.cameFrom);

         while (this.back.size() > 30) {
            this.back.removeLast();
         }
      }

      this.goingBack = false;
      this.cameFrom = null;
      this.p = np;
      this.treeScroll = 0;
      this.linesScroll = 0;
      this.clearAndInit();
   }

   private void ask(UUID id, boolean isBack) {
      if (ClientPlayNetworking.canSend(InspectPayloads.Ask.ID)) {
         this.goingBack = isBack;
         this.cameFrom = this.p.id();
         ClientPlayNetworking.send(new InspectPayloads.Ask(id));
      }
   }

   @Override
   protected void init() {
      this.pw = Math.min(this.width - 12, 640);
      this.ph = Math.min(this.height - 12, 300);
      this.px = (this.width - this.pw) / 2;
      this.py = (this.height - this.ph) / 2;
      this.tx = this.px + 6 + 150 + 8;
      this.ty = this.py + 22;
      this.tw = this.px + this.pw - 6 - this.tx;
      this.th = this.py + this.ph - 6 - this.ty;
      int by = this.py + this.ph - 24;
      ButtonWidget follow = ButtonWidget.builder(Text.literal(this.p.following() ? "Не следить" : "Следить"), b -> {
         if (ClientPlayNetworking.canSend(InspectPayloads.Follow.ID)) {
            ClientPlayNetworking.send(new InspectPayloads.Follow(this.p.id(), !this.p.following()));
         }

         this.close();
      }).dimensions(this.px + 6, by, 73, 18).build();
      follow.active = this.p.following() || this.p.alive() && this.p.entityId() >= 0 && this.god;
      this.addDrawableChild(follow);
      this.addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> this.close()).dimensions(this.px + 6 + 77, by, 73, 18).build());
      this.backButton = ButtonWidget.builder(Text.literal("← Назад"), b -> {
         if (!this.back.isEmpty()) {
            this.ask(this.back.pop(), true);
         }
      }).dimensions(this.px + this.pw - 6 - 60, this.py + 4, 60, 15).build();
      this.backButton.visible = !this.back.isEmpty();
      this.addDrawableChild(this.backButton);
      this.layoutTree();
      this.treeScroll = Math.max(0, Math.min(this.treeScroll, this.treeH - this.th));
   }

   private InspectScreen.Slot slot(UUID id) {
      for (InspectScreen.Slot s : this.slots) {
         if (s.n.id().equals(id)) {
            return s;
         }
      }

      return null;
   }

   private void wire(int x1, int y1, int x2, int y2, int color) {
      this.wires.add(new int[]{Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2), color});
   }

   private List<List<InspectScreen.Slot>> place(List<InspectPayloads.Node> nodes, int y, int cols) {
      List<List<InspectScreen.Slot>> rows = new ArrayList<>();
      int i = 0;

      while (i < nodes.size()) {
         int n = Math.min(cols, nodes.size() - i);
         int x = (this.tw - (n * 88 + (n - 1) * 5)) / 2;
         List<InspectScreen.Slot> row = new ArrayList<>();

         for (int j = 0; j < n; j++) {
            InspectScreen.Slot s = new InspectScreen.Slot(nodes.get(i + j), x + j * 93, y);
            row.add(s);
            this.slots.add(s);
         }

         rows.add(row);
         y += 28;
         i += cols;
      }

      return rows;
   }

   private void hang(List<Integer> xs, int bus, List<List<InspectScreen.Slot>> rows, int color) {
      if (!rows.isEmpty() && !xs.isEmpty()) {
         int lo = Integer.MAX_VALUE;
         int hi = Integer.MIN_VALUE;

         for (int x : xs) {
            lo = Math.min(lo, x);
            hi = Math.max(hi, x);
         }

         for (InspectScreen.Slot s : rows.get(0)) {
            int cx = s.x + 44;
            lo = Math.min(lo, cx);
            hi = Math.max(hi, cx);
            this.wire(cx, bus, cx, s.y, color);
         }

         if (rows.size() > 1) {
            lo = 3;
         }

         this.wire(lo, bus, hi, bus, color);

         for (int i = 1; i < rows.size(); i++) {
            List<InspectScreen.Slot> row = rows.get(i);
            int y = row.get(0).y - 3;
            int max = 3;

            for (InspectScreen.Slot s : row) {
               int cx = s.x + 44;
               max = Math.max(max, cx);
               this.wire(cx, y, cx, s.y, color);
            }

            this.wire(3, y, max, y, color);
            if (i == rows.size() - 1) {
               this.wire(3, bus, 3, y, color);
            }
         }
      }
   }

   private void layoutTree() {
      this.slots.clear();
      this.wires.clear();
      this.captions.clear();
      this.treeH = 0;
      if (this.tw >= 100) {
         List<List<InspectPayloads.Node>> gen = new ArrayList<>();

         for (int i = 0; i < 4; i++) {
            gen.add(new ArrayList<>());
         }

         for (InspectPayloads.Node n : this.p.tree()) {
            switch (n.rel()) {
               case 0:
                  gen.get(0).add(n);
                  break;
               case 1:
                  gen.get(1).add(n);
                  break;
               case 2:
                  gen.get(2).add(0, n);
                  break;
               case 3:
                  gen.get(2).add(n);
                  break;
               case 4:
                  gen.get(3).add(n);
            }
         }

         int cols = Math.max(1, (this.tw - 12 + 5) / 93);
         int y = 2;
         int[] bus = new int[4];
         List<List<List<InspectScreen.Slot>>> rows = new ArrayList<>();

         for (int g = 0; g < 4; g++) {
            if (gen.get(g).isEmpty()) {
               rows.add(new ArrayList<>());
            } else {
               this.captions.add(new int[]{g, y});
               y += 11;
               bus[g] = y + 3;
               y += 8;
               List<List<InspectScreen.Slot>> r = this.place(gen.get(g), y, cols);
               rows.add(r);
               y += r.size() * 28;
            }
         }

         this.treeH = y;

         for (List<InspectScreen.Slot> row : rows.get(0)) {
            for (InspectScreen.Slot s : row) {
               InspectScreen.Slot par = this.slot(s.n.link());
               if (par != null) {
                  int cx = s.x + 44;
                  int pc = par.x + 44;
                  this.wire(cx, s.y + 22, cx, bus[1], -7701926);
                  this.wire(cx, bus[1], pc, bus[1], -7701926);
                  this.wire(pc, bus[1], pc, par.y, -7701926);
               }
            }
         }

         List<Integer> xs = new ArrayList<>();

         for (List<InspectScreen.Slot> row : rows.get(1)) {
            for (InspectScreen.Slot sx : row) {
               int cx = sx.x + 44;
               xs.add(cx);
               this.wire(cx, sx.y + 22, cx, bus[2], -7701926);
            }
         }

         this.hang(xs, bus[2], rows.get(2), -7701926);
         InspectScreen.Slot self = this.slot(this.p.id());
         if (self != null && !rows.get(3).isEmpty()) {
            int cx = self.x + 44;
            xs = new ArrayList<>();
            if (rows.get(2).size() <= 1) {
               this.wire(cx, self.y + 22, cx, bus[3], -3629008);
               xs.add(cx);
            } else {
               int side = this.tw - 3;
               this.wire(cx, self.y + 22, cx, self.y + 22 + 1, -3629008);
               this.wire(cx, self.y + 22 + 1, side, self.y + 22 + 1, -3629008);
               this.wire(side, self.y + 22 + 1, side, bus[3], -3629008);
               xs.add(side);
            }

            this.hang(xs, bus[3], rows.get(3), -3629008);
         }
      }
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   private static String years(int a) {
      int d = a % 10;
      int h = a % 100;
      if (d == 1 && h != 11) {
         return "год";
      } else {
         return d < 2 || d > 4 || h >= 12 && h <= 14 ? "лет" : "года";
      }
   }

   private static String yearText(int y) {
      return y >= 0 ? "год " + y : "за " + -y + " " + years(-y) + " до летописи";
   }

   private LivingEntity entity() {
      if (this.client != null && this.client.world != null && this.p.entityId() >= 0) {
         return this.client.world.getEntityById(this.p.entityId()) instanceof LivingEntity le && le.isAlive() && le.getUuid().equals(this.p.id()) ? le : null;
      } else {
         return null;
      }
   }

   private InspectScreen.Slot slotAt(double mx, double my) {
      if (!(mx < this.tx) && !(mx >= this.tx + this.tw) && !(my < this.ty) && !(my >= this.ty + this.th)) {
         int x = (int)mx - this.tx;
         int y = (int)my - this.ty + this.treeScroll;

         for (InspectScreen.Slot s : this.slots) {
            if (x >= s.x && x < s.x + 88 && y >= s.y && y < s.y + 22) {
               return s;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   @Override
   public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
      ctx.fill(0, 0, this.width, this.height, 1879048192);
      if (Art.on()) {
         Art.panel(ctx, this.px - 2, this.py - 2, this.pw + 4, this.ph + 4);
      } else {
         ctx.fill(this.px - 2, this.py - 2, this.px + this.pw + 2, this.py + this.ph + 2, -9744862);
         ctx.fill(this.px, this.py, this.px + this.pw, this.py + this.ph, -267119592);
      }

      this.renderLeft(ctx, mx, my);
      this.renderTree(ctx, mx, my);
   }

   private void renderLeft(DrawContext ctx, int mx, int my) {
      TextRenderer tr = this.textRenderer;
      int x = this.px + 6;
      int y = this.py + 6;
      int rgb = this.p.kingdom().isEmpty() ? 16777215 : this.p.rgb();
      LivingEntity e = this.entity();
      Kingdoms.K kg = this.p.kingdom().isEmpty() ? null : Art.forRgb(this.p.rgb());
      if (Art.on()) {
         Art.inset(ctx, x, y, 150, 62);
         ctx.fill(x + 3, y + 3, x + 5, y + 59, 0xFF000000 | rgb);
      } else {
         ctx.fill(x, y, x + 150, y + 62, -1071241680);
         ctx.fill(x, y, x + 2, y + 62, 0xFF000000 | rgb);
      }

      if (e != null) {
         InventoryScreen.drawEntity(ctx, x + 2, y + 1, x + 150, y + 61, 26, 0.0625F, mx, my, e);
      } else {
         String s = this.p.alive() ? "не на виду" : "†";
         ctx.drawCenteredTextWithShadow(tr, s, x + 75, y + 27, -7697782);
      }

      y += 66;
      String name = (this.p.alive() ? "" : "† ") + this.p.name();
      ctx.drawTextWithShadow(tr, tr.trimToWidth(name, 150), x, y, this.p.alive() ? 0xFF000000 | brighten(rgb) : -6645094);
      y += 10;
      if (kg != null) {
         Art.emblem(ctx, kg, x, y - 2, 12);
         ctx.drawTextWithShadow(tr, tr.trimToWidth(this.p.kingdom(), 135), x + 15, y, -6645094);
      } else {
         ctx.drawTextWithShadow(tr, tr.trimToWidth(this.p.kingdom().isEmpty() ? "без королевства" : this.p.kingdom(), 150), x, y, -6645094);
      }

      y += 13;
      List<OrderedText> out = new ArrayList<>();
      if (e != null) {
         this.addLine(out, "Вид: " + e.getType().getName().getString());
      }

      for (String s : this.p.lines()) {
         this.addLine(out, s);
      }

      this.linesTop = y;
      this.linesH = this.py + this.ph - 28 - y;
      this.linesTotal = out.size() * 10;
      this.linesScroll = Math.max(0, Math.min(this.linesScroll, this.linesTotal - this.linesH));
      if (this.linesH > 0) {
         ctx.enableScissor(x, this.linesTop, x + 150 + 2, this.linesTop + this.linesH);
         int ly = this.linesTop - this.linesScroll;

         for (OrderedText o : out) {
            if (ly > this.linesTop - 10 && ly < this.linesTop + this.linesH) {
               ctx.drawTextWithShadow(tr, o, x, ly, -1);
            }

            ly += 10;
         }

         ctx.disableScissor();
         this.bar(ctx, x + 150 + 2, this.linesTop, this.linesH, this.linesTotal, this.linesScroll);
      }
   }

   private void addLine(List<OrderedText> out, String s) {
      int i = s.indexOf(": ");
      MutableText t = i < 0
         ? Text.literal(s)
         : Text.literal(s.substring(0, i + 2)).formatted(Formatting.GRAY).append(Text.literal(s.substring(i + 2)).formatted(Formatting.WHITE));
      out.addAll(this.textRenderer.wrapLines(t, 148));
   }

   private static int brighten(int rgb) {
      int r = rgb >> 16 & 0xFF;
      int g = rgb >> 8 & 0xFF;
      int b = rgb & 0xFF;
      r += (255 - r) * 2 / 5;
      g += (255 - g) * 2 / 5;
      b += (255 - b) * 2 / 5;
      return r << 16 | g << 8 | b;
   }

   private void bar(DrawContext ctx, int x, int y, int h, int total, int scroll) {
      if (total > h && h > 0) {
         int len = Math.max(10, h * h / total);
         int by = y + (h - len) * scroll / Math.max(1, total - h);
         ctx.fill(x, y, x + 2, y + h, 1090519039);
         ctx.fill(x, by, x + 2, by + len, -5601212);
      }
   }

   private void renderTree(DrawContext ctx, int mx, int my) {
      TextRenderer tr = this.textRenderer;
      ctx.drawTextWithShadow(tr, "Родословная", this.tx, this.py + 8, -10496);
      ctx.fill(this.tx - 5, this.py + 6, this.tx - 4, this.py + this.ph - 6, -12963808);
      if (this.slots.size() <= 1) {
         String s = this.p.tree().isEmpty() ? "У этого существа нет родословной" : "Родичи неизвестны";
         int hy = this.ty + this.treeH + 6;

         for (OrderedText o : tr.wrapLines(Text.literal(s + " — летопись рода ведётся у жителей королевств с момента рождения."), this.tw - 8)) {
            ctx.drawTextWithShadow(tr, o, this.tx + 2, hy, -7697782);
            hy += 10;
         }
      }

      InspectScreen.Slot hov = this.slotAt(mx, my);
      ctx.enableScissor(this.tx, this.ty, this.tx + this.tw, this.ty + this.th);
      ctx.getMatrices().push();
      ctx.getMatrices().translate((float)this.tx, (float)(this.ty - this.treeScroll), 0.0F);

      for (int[] w : this.wires) {
         ctx.fill(w[0], w[1], w[2] + 1, w[3] + 1, w[4]);
      }

      for (int[] c : this.captions) {
         ctx.drawTextWithShadow(tr, BANDS[c[0]], 10, c[1], -9475492);
      }

      for (InspectScreen.Slot s : this.slots) {
         this.box(ctx, tr, s, s == hov);
      }

      ctx.getMatrices().pop();
      ctx.disableScissor();
      this.bar(ctx, this.tx + this.tw + 1, this.ty, this.th, this.treeH, this.treeScroll);
   }

   private void box(DrawContext ctx, TextRenderer tr, InspectScreen.Slot s, boolean hov) {
      InspectPayloads.Node n = s.n;
      boolean self = n.rel() == 2;
      int x = s.x;
      int y = s.y;
      int border = self ? -10496 : (hov ? -1 : (n.alive() ? -10855834 : -12961222));
      Kingdoms.K kg = Art.on() ? Art.forRgb(n.rgb()) : null;
      if (Art.on()) {
         if (n.alive()) {
            Art.button(ctx, x, y, 88, 22, hov, false);
         } else {
            Art.buttonOff(ctx, x, y, 88, 22);
         }

         if (self) {
            Art.slotSelected(ctx, x, y, 88, 22);
         }

         ctx.fill(x + 1, y + 3, x + 3, y + 22 - 3, n.alive() ? 0xFF000000 | n.rgb() : -11184811);
         if (kg != null) {
            Art.emblem(ctx, kg, x + 88 - 11, y + 22 - 11, 8);
         }
      } else {
         ctx.fill(x, y, x + 88, y + 22, border);
         ctx.fill(x + 1, y + 1, x + 88 - 1, y + 22 - 1, n.alive() ? (hov ? -13355969 : -14277072) : (hov ? -14277082 : -15066598));
         ctx.fill(x + 1, y + 1, x + 3, y + 22 - 1, n.alive() ? 0xFF000000 | n.rgb() : -11184811);
      }

      String name = (n.royal() ? "♔ " : "") + n.name();
      ctx.drawTextWithShadow(tr, tr.trimToWidth(name, 80), x + 5, y + 3, n.alive() ? (n.royal() ? -6528 : -1) : -7697782);
      int a = Math.max(0, (n.alive() ? this.p.year() : n.died()) - n.born());
      String sub = (n.alive() ? "" : "† ") + a + " " + years(a);
      ctx.drawTextWithShadow(tr, sub, x + 5, y + 12, n.alive() ? -5592406 : -9803158);
   }

   @Override
   public void render(DrawContext ctx, int mx, int my, float delta) {
      super.render(ctx, mx, my, delta);
      InspectScreen.Slot s = this.slotAt(mx, my);
      if (s != null && (this.backButton == null || !this.backButton.visible || !this.backButton.isMouseOver(mx, my))) {
         InspectPayloads.Node n = s.n;
         List<Text> tip = new ArrayList<>();
         tip.add(Text.literal((n.royal() ? "♔ " : "") + n.name()).formatted(n.alive() ? Formatting.WHITE : Formatting.GRAY));
         tip.add(Text.literal(RELS[Math.max(0, Math.min(RELS.length - 1, n.rel()))]).formatted(Formatting.GOLD));
         tip.add(Text.literal("Родился: " + yearText(n.born())).formatted(Formatting.GRAY));
         if (n.alive()) {
            int a = Math.max(0, this.p.year() - n.born());
            tip.add(Text.literal("Жив, " + a + " " + years(a)).formatted(Formatting.GREEN));
         } else {
            int a = Math.max(0, n.died() - n.born());
            tip.add(Text.literal("† Умер: " + yearText(n.died()) + " (в " + a + " " + years(a) + ")").formatted(Formatting.GRAY));
         }

         if (n.royal()) {
            tip.add(Text.literal("Королевская кровь").formatted(Formatting.YELLOW));
         }

         if (n.rel() != 2) {
            tip.add(Text.literal("Клик — открыть карточку").formatted(Formatting.DARK_AQUA));
         }

         ctx.drawTooltip(this.textRenderer, tip, mx, my);
      }
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      if (super.mouseClicked(mx, my, button)) {
         return true;
      } else if (button != 0) {
         return false;
      } else {
         InspectScreen.Slot s = this.slotAt(mx, my);
         if (s != null && s.n.rel() != 2) {
            this.ask(s.n.id(), false);
            return true;
         } else {
            return false;
         }
      }
   }

   @Override
   public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
      int d = vertical > 0.0 ? -28 : 28;
      if (mx >= this.tx) {
         this.treeScroll = Math.max(0, Math.min(this.treeScroll + d, this.treeH - this.th));
      } else {
         this.linesScroll = Math.max(0, Math.min(this.linesScroll + (d > 0 ? 20 : -20), this.linesTotal - this.linesH));
      }

      return true;
   }

   @Override
   public boolean keyPressed(int key, int scan, int mods) {
      if (key == 259 && !this.back.isEmpty()) {
         this.ask(this.back.pop(), true);
         return true;
      } else {
         return super.keyPressed(key, scan, mods);
      }
   }

   @Override
   public void close() {
      if (this.client != null) {
         this.client.setScreen(this.god && this.client.player != null ? new GodScreen() : null);
      }
   }

   private record Slot(InspectPayloads.Node n, int x, int y) {
   }
}
