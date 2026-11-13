package com.vladik.godbox.client;

import com.vladik.godbox.Category;
import com.vladik.godbox.Power;
import com.vladik.godbox.net.Payloads;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Vector2ic;
import org.lwjgl.glfw.GLFW;

public class GodScreen extends Screen {
   private static Category category = Category.BOMBS;
   private static Power selected = Power.BOMB;
   private static int brush = 3;
   private static int shape = 0;
   private static String query = "";
   private static boolean searching;
   private static boolean codex;
   private static boolean help;
   private static int codexScroll;
   private static final String[] SHAPES = new String[]{"круг", "квадрат", "кольцо", "спрей"};
   private static final Set<String> DANGER = Set.of("ATOMIC", "CZAR_BOMBA", "TSAR_BOMBA", "ANTIMATTER", "GREY_GOO", "BLACK_HOLE", "WORLD_BANISH", "NUKE");
   private static float pitch = 90.0F;
   private static BlockPos preview;
   private static final Map<Category, Integer> SCROLLS = new EnumMap<>(Category.class);
   private static int searchScroll;
   private static double scrollPx;
   private static Object scrollView;
   private static int cursor;
   private static Object cacheKey;
   private static List<Power> cacheList = List.of();
   private static final int BAR_H = 14;
   private static final int INFO_H = 24;
   private static final int SB = 4;
   private static final int RAIL_TOP = 4;
   private boolean initialized;
   private boolean sentOff;
   private boolean leftDown;
   private int repeatTimer;
   private double targetY;
   private boolean hudWas;
   private double fovEffectWas;
   private BlockHitResult hover;
   private Entity follow;
   private Power armed;
   private long armedAt;
   private String toast = "";
   private long toastUntil;
   private int toastColor = -1;
   private boolean dragBar;
   private double dragOff;

   public GodScreen() {
      super(Text.literal("GodBox"));
   }

   @Override
   protected void init() {
      if (!this.initialized && this.client != null && this.client.player != null) {
         this.initialized = true;
         this.hudWas = this.client.options.hudHidden;
         this.client.options.hudHidden = true;
         this.fovEffectWas = this.client.options.getFovEffectScale().getValue();
         this.client.options.getFovEffectScale().setValue(0.0);
         this.targetY = this.client.player.getY();
         this.toast("GodBox 3.2 — I: справка, ПКМ по силе: в избранное", -10496);
      }
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
   }

   static BlockPos previewPos() {
      return preview;
   }

   static int previewRadius() {
      return selected != null && selected.category != null ? brush : 0;
   }

   static int previewShape() {
      return shape;
   }

   static int previewColor() {
      return selected != null && DANGER.contains(selected.name()) ? 16728128 : 6349055;
   }

   private void toast(String s, int color) {
      this.toast = s;
      this.toastColor = color;
      this.toastUntil = System.currentTimeMillis() + 2600L;
   }

   void note(String s, int color) {
      if (s != null && !s.isEmpty()) {
         this.toast(s, color);
      }
   }

   private int slot() {
      return MathHelper.clamp(this.width / 28, 20, 26);
   }

   private int railCell() {
      int n = Category.values().length;
      return MathHelper.clamp((this.height - 4 - 8) / n, 9, 22);
   }

   private int railW() {
      return this.railCell() + 6;
   }

   private int mapSize() {
      return MathHelper.clamp(Math.min(110, this.width / 2 - 95 - 14), 40, 110);
   }

   private int mapX() {
      return this.width - 8 - this.mapSize();
   }

   private static boolean listMode() {
      return searching && !query.isEmpty();
   }

   static boolean typing() {
      return searching;
   }

   private static int target() {
      return listMode() ? searchScroll : SCROLLS.getOrDefault(category, 0);
   }

   private static void setTarget(int v) {
      if (listMode()) {
         searchScroll = v;
      } else {
         SCROLLS.put(category, v);
      }
   }

   private static boolean danger(Power p) {
      return DANGER.contains(p.name());
   }

   private static boolean brushApplies(Power p) {
      return p.category != null && p.category != Category.LAWS && !p.name().startsWith("TIME_") && !p.name().startsWith("AGE_");
   }

   private static String hint(Power p) {
      String h = p.category == Category.LAWS
         ? "ЛКМ — переключить закон"
         : (danger(p) ? "ЛКМ дважды — подтвердить" : (p.continuous ? "Удерживайте ЛКМ — рисовать" : "ЛКМ — применить"));
      return brushApplies(p) ? h + " · [ ] кисть · B форма" : h;
   }

   static List<Text> tooltipLines(Power p) {
      List<Text> lines = new ArrayList<>();
      lines.add(Text.literal(p.title).formatted(Formatting.GOLD));
      if (p.category != null) {
         lines.add(Text.literal("Вкладка: " + p.category.title).formatted(Formatting.YELLOW));
      }

      lines.add(Text.literal(p.desc).formatted(Formatting.GRAY));
      lines.add(
         Text.literal((p.continuous ? "Удерживайте ЛКМ" : "Одиночное применение") + (danger(p) ? " · опасно: двойной клик" : ""))
            .formatted(danger(p) ? Formatting.RED : Formatting.DARK_AQUA)
      );
      lines.add(Text.literal(brushApplies(p) ? "Кисть: размер " + brush + ", " + SHAPES[shape] : "Кисть не используется").formatted(Formatting.DARK_AQUA));
      if (p.category == Category.LAWS) {
         Boolean on = LawStates.get(p);
         lines.add(
            on == null
               ? Text.literal("Состояние станет видно после переключения").formatted(Formatting.DARK_GRAY)
               : Text.literal(on ? "Сейчас: ВКЛ" : "Сейчас: ВЫКЛ").formatted(on ? Formatting.GREEN : Formatting.RED)
         );
      }

      lines.add(Text.literal(Favorites.has(p) ? "ПКМ — убрать из избранного" : "ПКМ — в избранное").formatted(Formatting.DARK_GRAY));
      return MortalClient.tooltip(p, lines);
   }

   private void drawTip(DrawContext ctx, List<Text> lines, int mx, int my) {
      int maxW = Math.max(60, Math.min(220, this.width - 40));
      List<OrderedText> out = new ArrayList<>();

      for (Text t : lines) {
         out.addAll(this.textRenderer.wrapLines(t, maxW));
      }

      if (!Art.on()) {
         ctx.drawTooltip(this.textRenderer, out, Ui.TIP, mx, my);
      } else {
         int tw = 0;

         for (OrderedText o : out) {
            tw = Math.max(tw, this.textRenderer.getWidth(o));
         }

         int w = tw + 12;
         int h = out.size() * 10 + (out.size() > 1 ? 2 : 0) + 8;
         Vector2ic p = Ui.TIP.getPosition(this.width, this.height, mx, my, w, h);
         ctx.getMatrices().push();
         ctx.getMatrices().translate(0.0F, 0.0F, 400.0F);
         Art.tooltip(ctx, p.x(), p.y(), w, h);

         for (int i = 0; i < out.size(); i++) {
            ctx.drawText(this.textRenderer, out.get(i), p.x() + 6, p.y() + 5 + i * 10 + (i > 0 ? 2 : 0), -1, true);
         }

         ctx.getMatrices().pop();
      }
   }

   private void drawSlot(DrawContext ctx, Power p, int x, int y, int w, boolean hov, boolean cur, String num, boolean starHot) {
      int s = this.slot();
      boolean danger = danger(p);
      Boolean law = LawStates.get(p);
      if (Art.on()) {
         Art.button(ctx, x, y, w, s, hov, false);
         if (!hov && law != null) {
            ctx.fill(x + 2, y + 2, x + w - 2, y + s - 2, law ? -1072416228 : -1069935082);
         } else if (!hov && p == selected) {
            ctx.fill(x + 2, y + 2, x + w - 2, y + s - 2, -532664304);
         }

         if (law != null) {
            ctx.fill(x + 3, y + s - 4, x + w - 3, y + s - 2, law ? -11149200 : -2080704);
         } else {
            ctx.fill(x + 3, y + s - 3, x + w - 3, y + s - 2, danger ? -2080704 : (p.continuous ? -12541728 : -11513760));
         }

         if (p == selected) {
            Art.slotSelected(ctx, x - 1, y - 1, w + 2, s + 2);
         } else if (cur) {
            ctx.drawBorder(x - 1, y - 1, w + 2, s + 2, -1);
         }
      } else {
         ctx.fill(x, y, x + w, y + s, hov ? -532133800 : (p == selected ? -532664304 : (law != null ? (law ? -1072416228 : -1069935082) : -1072557544)));
         if (law != null) {
            ctx.fill(x, y + s - 2, x + w, y + s, law ? -11149200 : -2080704);
         } else {
            ctx.fill(x, y + s - 1, x + w, y + s, danger ? -2080704 : (p.continuous ? -12541728 : -11513760));
         }

         if (p == selected) {
            ctx.drawBorder(x - 1, y - 1, w + 2, s + 2, -10496);
         } else if (cur) {
            ctx.drawBorder(x - 1, y - 1, w + 2, s + 2, -1);
         }
      }

      if (!PowerIcons.draw(ctx, p, x + (s - 16) / 2, y + (s - 16) / 2, 16)) {
         ctx.drawItem(new ItemStack(p.icon), x + (s - 16) / 2, y + (s - 16) / 2);
      }

      if (!MortalClient.affordable(p)) {
         ctx.fill(x, y, x + s, y + s - 1, 200, -1341124584);
      }

      if (w > s && p.category != null) {
         int ty = y + (s - 8) / 2;
         String cat = Ui.ellipsis(this.textRenderer, p.category.title, Math.max(0, w / 3));
         int cwid = this.textRenderer.getWidth(cat);
         ctx.drawTextWithShadow(this.textRenderer, cat, x + w - 11 - cwid, ty, -8355696);
         ctx.drawTextWithShadow(
            this.textRenderer, Ui.ellipsis(this.textRenderer, p.title, w - s - 6 - cwid - 13), x + s + 2, ty, p == selected ? -10496 : -2236963
         );
      }

      ctx.getMatrices().push();
      ctx.getMatrices().translate(0.0F, 0.0F, 200.0F);
      if (num != null) {
         ctx.drawText(this.textRenderer, num, x + 1, y + 1, -5592406, true);
      }

      boolean fav = Favorites.has(p);
      if (hov) {
         ctx.drawText(this.textRenderer, fav ? "★" : "☆", x + w - 7, y + 1, starHot ? -1 : (fav ? -10496 : -4473925), true);
      } else if (fav) {
         ctx.drawText(this.textRenderer, "★", x + w - 7, y + 1, -10496, true);
      }

      if (law != null) {
         ctx.drawText(this.textRenderer, law ? "✔" : "✖", x + w - 8, y + s - 10, law ? -11149200 : -2080704, true);
      }

      ctx.getMatrices().pop();
   }

   private boolean overlay() {
      return help || codex || WorldMap.on || WorldPanels.popup;
   }

   @Override
   public void render(DrawContext ctx, int mx, int my, float delta) {
      super.render(ctx, mx, my, delta);
      Power hoverPower = null;
      List<Text> tip = null;
      boolean blocked = this.overlay() || StatsPanel.inside(mx, my, this.width, this.height);
      int hx = blocked ? -9999 : mx;
      int hy = blocked ? -9999 : my;
      Category[] cs = Category.values();
      int rc = this.railCell();
      int rt = 4;
      if (Art.on()) {
         Art.inset(ctx, 2, rt - 2, rc + 4, cs.length * rc + 4);
      } else {
         ctx.fill(2, rt - 2, 4 + rc + 2, rt + cs.length * rc + 2, -1609560040);
      }

      for (int i = 0; i < cs.length; i++) {
         Category c = cs[i];
         int y = rt + i * rc;
         boolean sel = c == category && !listMode();
         boolean hov = hx >= 2 && hx < 4 + rc + 2 && hy >= y && hy < y + rc;
         if (Art.on()) {
            if (sel || hov) {
               Art.button(ctx, 4, y, rc, rc - 1, hov, sel);
            }

            if (sel) {
               ctx.fill(2, y + 1, 4, y + rc - 2, -6528);
            }
         } else if (sel) {
            ctx.fill(4, y, 4 + rc, y + rc - 1, -524777973);
            ctx.fill(2, y, 4, y + rc - 1, -6528);
         } else if (hov) {
            ctx.fill(4, y, 4 + rc, y + rc - 1, -1068478368);
         }

         if (rc >= 16) {
            if (!PowerIcons.drawCategory(ctx, c, 4 + (rc - 16) / 2, y + (rc - 16) / 2, 16)) {
               ctx.drawItem(new ItemStack(c.icon), 4 + (rc - 16) / 2, y + (rc - 16) / 2);
            }
         } else {
            ctx.drawCenteredTextWithShadow(this.textRenderer, c.title.substring(0, 1), 4 + rc / 2, y + (rc - 8) / 2, -1);
         }

         if (hov) {
            tip = List.of(
               Text.literal(c.title).formatted(Formatting.GOLD),
               Text.literal(Power.of(c).size() + " сил").formatted(Formatting.GRAY),
               Text.literal("Tab / Shift+Tab — листать вкладки").formatted(Formatting.DARK_GRAY)
            );
         }
      }

      MiniMap.render(ctx, this.client, this.mapX(), 4, this.mapSize());
      GodScreen.Dock d = new GodScreen.Dock();
      int s = d.s;
      int n = d.list.size();
      Object view = d.rows ? "search" : category;
      int tgt = target();
      if (view == scrollView && !this.dragBar) {
         scrollPx = scrollPx + (tgt - scrollPx) * Math.min(1.0, delta * 0.5);
         if (Math.abs(tgt - scrollPx) < 0.5) {
            scrollPx = tgt;
         }
      } else {
         scrollView = view;
         scrollPx = tgt;
      }

      if (Art.on()) {
         Art.frame(ctx, d.x0, d.top, d.x1 - d.x0, d.bottom - d.top);
         ctx.fill(d.x0 + 3, d.top + 3, d.x0 + 5, d.bottom - 3, danger(selected) ? -2080704 : -4684277);
      } else {
         ctx.fill(d.x0, d.top, d.x1, d.bottom, -1072689128);
         ctx.fill(d.x0, d.top, d.x0 + 2, d.bottom, danger(selected) ? -2080704 : -4684277);
      }

      ctx.fill(d.x0 + 2, d.stripY, d.x1, d.stripY + 1, 822083583);
      ctx.fill(d.x0 + 2, d.gridY - 1, d.x1, d.gridY, 822083583);
      int[] hit = blocked ? null : d.hit(mx, my);
      int ix = d.x0 + 4 + 2;
      int iy = d.infoY;
      int tx = ix + 24;
      int right = d.x1 - 4;
      if (Art.on()) {
         Art.inset(ctx, ix, iy + 2, 20, 20);
      } else {
         ctx.fill(ix, iy + 2, ix + 20, iy + 22, -1071241680);
      }

      if (!PowerIcons.draw(ctx, selected, ix + 2, iy + 4, 16)) {
         ctx.drawItem(new ItemStack(selected.icon), ix + 2, iy + 4);
      }

      boolean ba = brushApplies(selected);
      String br;
      if (right - tx > 260) {
         StringBuilder dots = new StringBuilder();

         for (int i = 1; i <= 10; i++) {
            dots.append((char)(i <= brush ? '●' : '○'));
         }

         br = dots + " " + SHAPES[shape];
      } else {
         br = "Кисть " + brush + " · " + SHAPES[shape];
      }

      int bw = this.textRenderer.getWidth(br);
      ctx.drawTextWithShadow(this.textRenderer, br, right - bw, iy + 3, ba ? -6631169 : -8355696);
      Boolean law = LawStates.get(selected);
      String st = law == null ? "" : (law ? "  ВКЛ" : "  ВЫКЛ");
      int stW = this.textRenderer.getWidth(st);
      String title = Ui.ellipsis(this.textRenderer, selected.title, right - bw - 8 - tx - stW);
      int tw = this.textRenderer.getWidth(title);
      ctx.drawTextWithShadow(this.textRenderer, title, tx, iy + 3, -10496);
      if (law != null) {
         ctx.drawTextWithShadow(this.textRenderer, st, tx + tw, iy + 3, law ? -11149200 : -2080704);
      }

      if (this.hover != null) {
         String tg = this.hover.getBlockPos().toShortString();
         int gw = this.textRenderer.getWidth(tg);
         if (tx + tw + stW + 10 + gw <= right - bw - 8) {
            ctx.drawTextWithShadow(this.textRenderer, tg, right - bw - 8 - gw, iy + 3, -8355696);
         }
      }

      String hint = Ui.ellipsis(this.textRenderer, hint(selected), Math.max(0, right - tx - 60));
      int hw = this.textRenderer.getWidth(hint);
      ctx.drawTextWithShadow(this.textRenderer, hint, right - hw, iy + 13, -12541728);
      String line2 = this.follow != null
         ? "Слежка: "
            + this.follow.getName().getString()
            + (this.follow instanceof LivingEntity le ? " ♥" + (int)le.getHealth() + "/" + (int)le.getMaxHealth() : "")
         : selected.desc;
      ctx.drawTextWithShadow(
         this.textRenderer, Ui.ellipsis(this.textRenderer, line2, right - hw - 8 - tx), tx, iy + 13, this.follow != null ? -8323073 : -4473925
      );
      if (Ui.in(hx, hy, d.x0, d.top, d.x1 - d.x0, d.stripY - d.top)) {
         hoverPower = selected;
      }

      int sy = d.stripSlotY();
      int gly = sy + (s - 8) / 2;
      if (d.fav.isEmpty()) {
         ctx.drawTextWithShadow(
            this.textRenderer,
            Ui.ellipsis(this.textRenderer, "★ ПКМ по силе или ☆ — в избранное", (d.rec.isEmpty() ? d.x1 : d.recX - 12) - d.x0 - 8),
            d.x0 + 4 + 2,
            gly,
            -8355696
         );
      } else {
         ctx.drawTextWithShadow(this.textRenderer, "★", d.x0 + 4 + 2, gly, -10496);
         if (Ui.in(hx, hy, d.x0 + 4, sy, 12, s)) {
            tip = List.of(
               Text.literal("Избранное").formatted(Formatting.GOLD), Text.literal("Shift+1–9 — выбрать, Shift+ПКМ — сдвинуть влево").formatted(Formatting.GRAY)
            );
         }
      }

      for (int i = 0; i < d.fav.size(); i++) {
         boolean hovx = hit != null && hit[0] == 1 && hit[1] == i;
         this.drawSlot(ctx, d.fav.get(i), d.favX + i * d.cell, sy, s, hovx, false, i < 9 ? "⇧" + (i + 1) : null, hovx && d.onStar(hit, mx, my));
      }

      if (!d.rec.isEmpty()) {
         ctx.drawTextWithShadow(this.textRenderer, "↺", d.recX - 10, gly, -6631169);
         if (Ui.in(hx, hy, d.recX - 12, sy, 12, s)) {
            tip = List.of(Text.literal("Недавние").formatted(Formatting.GOLD), Text.literal("Последние применённые силы").formatted(Formatting.GRAY));
         }
      }

      for (int i = 0; i < d.rec.size(); i++) {
         boolean hovx = hit != null && hit[0] == 2 && hit[1] == i;
         this.drawSlot(ctx, d.rec.get(i), d.recX + i * d.cell, sy, s, hovx, false, null, hovx && d.onStar(hit, mx, my));
      }

      String header = d.rows ? "Поиск: «" + query + "» — " + n : category.title + " — " + n + " сил";
      ctx.drawTextWithShadow(this.textRenderer, Ui.ellipsis(this.textRenderer, header, d.searchX - 6 - (d.x0 + 4 + 2)), d.x0 + 4 + 2, d.barY + 3, -6528);
      boolean sHov = d.inSearch(hx, hy);
      if (Art.on()) {
         Art.inset(ctx, d.searchX, d.barY + 1, d.searchW, 12);
         if (searching || sHov) {
            ctx.drawBorder(d.searchX, d.barY + 1, d.searchW, 12, searching ? -10496 : -4473925);
         }
      } else {
         ctx.fill(d.searchX, d.barY + 1, d.searchX + d.searchW, d.barY + 13, !sHov && !searching ? -536870912 : -266330072);
         ctx.drawBorder(d.searchX, d.barY + 1, d.searchW, 12, searching ? -10496 : (sHov ? -4473925 : -11513760));
      }

      if (searching) {
         String caret = System.currentTimeMillis() / 400L % 2L == 0L ? "_" : "";
         ctx.drawTextWithShadow(this.textRenderer, this.textRenderer.trimToWidth(query, d.searchW - 12, true) + caret, d.searchX + 3, d.barY + 3, -171);
      } else {
         ctx.drawTextWithShadow(this.textRenderer, Ui.ellipsis(this.textRenderer, "Поиск: Ctrl+F", d.searchW - 6), d.searchX + 3, d.barY + 3, -8355696);
      }

      if (sHov) {
         tip = List.of(
            Text.literal("Поиск силы").formatted(Formatting.GOLD),
            Text.literal("Ctrl+F или / — искать по названию и описанию во всех вкладках").formatted(Formatting.GRAY),
            Text.literal("Стрелки — выбор, Enter — выбрать, Esc — закрыть").formatted(Formatting.DARK_GRAY)
         );
      }

      boolean qHov = d.inHelp(hx, hy);
      if (Art.on()) {
         Art.button(ctx, d.helpX, d.barY + 1, 12, 12, qHov, false);
      } else {
         ctx.fill(d.helpX, d.barY + 1, d.helpX + 12, d.barY + 13, qHov ? -532133800 : -1071241680);
         ctx.drawBorder(d.helpX, d.barY + 1, 12, 12, qHov ? -10496 : -11513760);
      }

      ctx.drawCenteredTextWithShadow(this.textRenderer, "?", d.helpX + 6, d.barY + 3, qHov ? -10496 : -1);
      if (qHov) {
         tip = List.of(Text.literal("I — справка").formatted(Formatting.GOLD), Text.literal("F1 — тоже справка, H — энциклопедия").formatted(Formatting.GRAY));
      }

      if (n == 0) {
         ctx.drawCenteredTextWithShadow(this.textRenderer, "Ничего не найдено", (d.gx0 + d.gridR) / 2, d.gridY + (d.gridH - 8) / 2, -8355696);
      }

      ctx.enableScissor(d.gx0, d.gridY, d.gridR, d.gridY + d.gridH);

      for (int i = 0; i < n; i++) {
         int x = d.cellX(i);
         int yx = d.cellY(i);
         if (yx + d.cell > d.gridY && yx - 1 < d.gridY + d.gridH) {
            boolean hovx = hit != null && hit[0] == 0 && hit[1] == i;
            this.drawSlot(
               ctx, d.list.get(i), x, yx, d.slotW, hovx, d.rows && i == cursor, !d.rows && i < 9 ? String.valueOf(i + 1) : null, hovx && d.onStar(hit, mx, my)
            );
         }
      }

      ctx.disableScissor();
      if (d.maxScroll > 0) {
         boolean bHov = this.dragBar || d.inBar(hx, hy);
         if (Art.on()) {
            Art.scrollTrack(ctx, d.sbX, d.gridY, 4, d.gridH);
            Art.scrollThumb(ctx, d.sbX, d.thumbY(), 4, d.thumbH());
            if (bHov) {
               ctx.fill(d.sbX, d.thumbY(), d.sbX + 4, d.thumbY() + d.thumbH(), 1090512512);
            }
         } else {
            ctx.fill(d.sbX, d.gridY, d.sbX + 4, d.gridY + d.gridH, 1610612736);
            ctx.fill(d.sbX, d.thumbY(), d.sbX + 4, d.thumbY() + d.thumbH(), bHov ? -6528 : -4684277);
         }
      }

      if (hit != null) {
         hoverPower = d.power(hit);
         if (d.onStar(hit, mx, my)) {
            tip = List.of(Text.literal(Favorites.has(hoverPower) ? "Убрать из избранного" : "В избранное").formatted(Formatting.GOLD));
            hoverPower = null;
         }
      }

      ctx.getMatrices().push();
      ctx.getMatrices().translate(0.0F, 0.0F, 300.0F);
      WorldPanels.render(ctx, this.textRenderer, this.width, this.height, mx, my);
      long now = System.currentTimeMillis();
      if (now < this.toastUntil) {
         String ts = Ui.ellipsis(this.textRenderer, this.toast, this.width - 24);
         int tw2 = this.textRenderer.getWidth(ts) + 14;
         int ty = WorldPanels.bottom() + 8;
         if (Art.on()) {
            Art.tooltip(ctx, this.width / 2 - tw2 / 2, ty, tw2, 14);
         } else {
            ctx.fill(this.width / 2 - tw2 / 2, ty, this.width / 2 + tw2 / 2, ty + 14, -804253672);
            ctx.fill(this.width / 2 - tw2 / 2, ty + 13, this.width / 2 + tw2 / 2, ty + 14, this.toastColor);
         }

         ctx.drawCenteredTextWithShadow(this.textRenderer, ts, this.width / 2, ty + 3, this.toastColor);
      }

      StatsPanel.render(ctx, this.textRenderer, this.width, this.height);
      if (help) {
         this.renderHelp(ctx);
      }

      if (WorldMap.on) {
         WorldMap.render(ctx, this.client, this.width, this.height, mx, my);
      }

      if (codex) {
         Codex.render(ctx, this.textRenderer, this.width, this.height, category, codexScroll);
      }

      ctx.getMatrices().pop();
      if (!blocked) {
         if (tip != null) {
            this.drawTip(ctx, tip, mx, my);
         } else if (hoverPower != null) {
            this.drawTip(ctx, tooltipLines(hoverPower), mx, my);
         }
      }
   }

   private void renderHelp(DrawContext ctx) {
      String[][] rows = new String[][]{
         {"ЛКМ", "применить силу"},
         {"ПКМ по силе", "добавить/убрать из избранного"},
         {"WASD / Shift", "летать / быстрее"},
         {"Колесо", "высота камеры"},
         {"Ctrl+колесо, [ ]", "размер кисти"},
         {"B", "форма кисти"},
         {"Q/E, R/F", "поворот / наклон"},
         {"1–9 / Shift+1–9", "сила / избранное"},
         {"Tab", "следующая вкладка"},
         {"Ctrl+F или /", "поиск силы"},
         {"Ctrl+Z", "отменить своё последнее действие"},
         {"P", "пауза времени"},
         {"J", "выбор эпохи"},
         {"M / N", "миникарта / карта мира"},
         {"H", "энциклопедия"},
         {"K", "статистика"},
         {"C", "следить за существом"},
         {"Esc / G", "выйти из режима бога"},
         {"Стрелки", "выбор силы в сетке"},
         {"Shift+Tab", "предыдущая вкладка"},
         {"Enter", "выбрать найденную силу"},
         {"Колесо над сеткой", "прокрутка сил"},
         {"☆ на значке силы", "в избранное / убрать"},
         {"Shift+ПКМ по ★", "сдвинуть избранное влево"},
         {"F1 или ?", "эта справка"}
      };
      int kw = 0;
      int dw = 0;

      for (String[] r : rows) {
         kw = Math.max(kw, this.textRenderer.getWidth(r[0]));
         dw = Math.max(dw, this.textRenderer.getWidth(r[1]));
      }

      int w = Math.max(300, kw + dw + 34);
      int h = rows.length * 11 + 26;
      float k = Math.min(1.0F, Math.min((float)(this.width - 8) / (w + 12), (float)(this.height - 8) / (h + 12)));
      ctx.getMatrices().push();
      ctx.getMatrices().translate(this.width / 2.0F, this.height / 2.0F, 0.0F);
      ctx.getMatrices().scale(k, k, 1.0F);
      int x = -w / 2;
      int y = -h / 2;
      if (Art.on()) {
         Art.panel(ctx, x - 6, y - 6, w + 12, h + 12);
         Art.ribbon(ctx, -90, y + 3, 180, 14);
      } else {
         ctx.fill(x - 2, y - 2, x + w + 2, y + h + 2, -4684277);
         ctx.fill(x, y, x + w, y + h, -267251176);
      }

      ctx.drawCenteredTextWithShadow(this.textRenderer, "GodBox 3.2 — управление", 0, y + 6, Art.on() ? -1 : -10496);

      for (int i = 0; i < rows.length; i++) {
         ctx.drawTextWithShadow(this.textRenderer, rows[i][0], x + 10, y + 22 + i * 11, -6631169);
         ctx.drawTextWithShadow(this.textRenderer, rows[i][1], x + kw + 24, y + 22 + i * 11, -2236963);
      }

      ctx.getMatrices().pop();
   }

   private static List<Power> list() {
      Object key = listMode() ? query : category;
      if (key.equals(cacheKey)) {
         return cacheList;
      } else {
         List<Power> r;
         if (!listMode()) {
            r = Power.of(category);
         } else {
            String q = Ui.norm(query);
            r = new ArrayList<>();
            List<Power> byDesc = new ArrayList<>();

            for (Power p : Power.values()) {
               if (p.category != null) {
                  if (Ui.norm(p.title).contains(q)) {
                     r.add(p);
                  } else if (Ui.norm(p.desc).contains(q)) {
                     byDesc.add(p);
                  }
               }
            }

            r.addAll(byDesc);
         }

         cacheKey = key;
         cacheList = r;
         return r;
      }
   }

   private void setQuery(String q) {
      query = q.length() > 30 ? q.substring(0, 30) : q;
      cursor = 0;
      searchScroll = 0;
   }

   private void openSearch() {
      searching = true;
      this.setQuery("");
      codex = false;
      WorldMap.on = false;
      help = false;
   }

   private void closeSearch() {
      searching = false;
      this.setQuery("");
   }

   private void select(Power p) {
      if (searching && p.category != null) {
         this.closeSearch();
         category = p.category;
      }

      if (p.category != null && !searching) {
         category = p.category;
      }

      selected = p;
      this.leftDown = false;
      this.armed = null;
      GodScreen.Dock d = new GodScreen.Dock();
      d.reveal(d.list.indexOf(p));
   }

   private void setCategory(Category c) {
      this.closeSearch();
      category = c;
      List<Power> l = Power.of(c);
      if (!l.isEmpty() && (selected == null || selected.category != c)) {
         int row = SCROLLS.getOrDefault(c, 0) / (this.slot() + 2);
         GodScreen.Dock d = new GodScreen.Dock();
         this.select(l.get(MathHelper.clamp(row * d.cols, 0, l.size() - 1)));
      }
   }

   private void toggleFav(Power p) {
      Favorites.toggle(p);
      this.toast(Favorites.has(p) ? "★ " + p.title + " — в избранном" : p.title + " убрана из избранного", -10496);
      Ui.click();
   }

   private void moveSel(int dx, int dy) {
      GodScreen.Dock d = new GodScreen.Dock();
      int n = d.list.size();
      if (n != 0) {
         int i = d.rows ? cursor : d.list.indexOf(selected);
         if (i < 0) {
            i = 0;
         } else if (dx != 0) {
            i = MathHelper.clamp(i + dx, 0, n - 1);
         } else {
            int j = i + dy * d.cols;
            if (j >= 0 && j < n) {
               i = j;
            } else if (dy > 0 && i / d.cols < (n - 1) / d.cols) {
               i = n - 1;
            }
         }

         if (d.rows) {
            cursor = i;
            d.reveal(i);
         } else {
            this.select(d.list.get(i));
         }
      }
   }

   private void sendNamed(String names) {
      for (String nm : names.split(";")) {
         Power p = Power.byName(nm);
         if (p != null) {
            this.sendPower(p);
         }
      }
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      if (button == 1) {
         if (!this.overlay() && !StatsPanel.inside(mx, my, this.width, this.height)) {
            GodScreen.Dock d = new GodScreen.Dock();
            int[] h = d.hit(mx, my);
            if (h == null) {
               return super.mouseClicked(mx, my, button);
            } else {
               Power p = d.power(h);
               if (h[0] == 1 && hasShiftDown()) {
                  Favorites.moveLeft(p);
                  Ui.click();
               } else {
                  this.toggleFav(p);
               }

               return true;
            }
         } else {
            return true;
         }
      } else if (button != 0) {
         return super.mouseClicked(mx, my, button);
      } else if (help) {
         help = false;
         return true;
      } else if (WorldMap.on) {
         int[] wm = WorldMap.click(mx, my);
         if (wm != null && ClientPlayNetworking.canSend(Payloads.Use.ID)) {
            this.follow = null;
            ClientPlayNetworking.send(new Payloads.Use(Power.CAMERA_TP.name(), new BlockPos(wm[0], 64, wm[1]), brush, true));
            WorldMap.on = false;
         }

         return true;
      } else if (codex) {
         codex = false;
         return true;
      } else {
         String wp = WorldPanels.click(mx, my, this.width, this.height);
         if (wp != null) {
            if (!wp.isEmpty()) {
               this.sendNamed(wp);
            }

            Ui.click();
            return true;
         } else if (StatsPanel.inside(mx, my, this.width, this.height)) {
            return true;
         } else {
            Category[] cs = Category.values();
            int rc = this.railCell();
            int rt = 4;

            for (int i = 0; i < cs.length; i++) {
               int y = rt + i * rc;
               if (mx >= 2.0 && mx < 4 + rc + 2 && my >= y && my < y + rc) {
                  this.setCategory(cs[i]);
                  Ui.click();
                  return true;
               }
            }

            GodScreen.Dock d = new GodScreen.Dock();
            if (d.inside(mx, my)) {
               if (d.inHelp(mx, my)) {
                  help = true;
                  codex = false;
                  Ui.click();
               } else if (d.inSearch(mx, my)) {
                  if (!searching) {
                     this.openSearch();
                  }

                  Ui.click();
               } else if (d.inBar(mx, my)) {
                  int ty = d.thumbY();
                  int th = d.thumbH();
                  this.dragOff = my >= ty && my < ty + th ? my - ty : th / 2.0;
                  this.dragBar = true;
                  this.dragTo(d, my);
               } else {
                  int[] h = d.hit(mx, my);
                  if (h != null) {
                     Power p = d.power(h);
                     if (d.onStar(h, mx, my)) {
                        this.toggleFav(p);
                     } else {
                        this.select(p);
                        Ui.click();
                     }
                  }
               }

               return true;
            } else if (searching) {
               this.closeSearch();
               return true;
            } else {
               int[] mp = MiniMap.click(mx, my, this.mapX(), 4, this.mapSize());
               if (mp != null) {
                  this.follow = null;
                  if (ClientPlayNetworking.canSend(Payloads.Use.ID)) {
                     ClientPlayNetworking.send(new Payloads.Use(Power.CAMERA_TP.name(), new BlockPos(mp[0], 64, mp[1]), brush, true));
                  }

                  return true;
               } else {
                  this.hover = this.raycast(mx, my);
                  if (DANGER.contains(selected.name())) {
                     long now = System.currentTimeMillis();
                     if (this.armed != selected || now - this.armedAt > 3000L) {
                        this.armed = selected;
                        this.armedAt = now;
                        this.toast("⚠ " + selected.title + ": нажмите ещё раз для подтверждения", -43691);
                        return true;
                     }

                     this.armed = null;
                  }

                  if (selected == Power.POSSESS && this.follow == null) {
                     this.follow = this.pickEntity();
                  }

                  this.leftDown = true;
                  if (this.hover != null) {
                     Recent.push(selected);
                  }

                  this.usePower(true);
                  this.repeatTimer = selected.repeat;
                  return true;
               }
            }
         }
      }
   }

   private void dragTo(GodScreen.Dock d, double my) {
      int span = d.gridH - d.thumbH();
      int t = span <= 0 ? 0 : (int)Math.round((my - this.dragOff - d.gridY) * d.maxScroll / span);
      setTarget(MathHelper.clamp(t, 0, d.maxScroll));
      scrollPx = target();
   }

   @Override
   public boolean mouseReleased(double mx, double my, int button) {
      if (button == 0) {
         this.leftDown = false;
         this.dragBar = false;
      }

      return super.mouseReleased(mx, my, button);
   }

   @Override
   public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
      if (this.client == null || this.client.player == null) {
         return true;
      } else if (codex) {
         codexScroll = Math.max(0, Math.min(600, codexScroll + (vertical > 0.0 ? -3 : 3)));
         return true;
      } else if (WorldMap.on) {
         WorldMap.scroll(mx, my, vertical);
         return true;
      } else if (hasControlDown()) {
         brush = MathHelper.clamp(brush + (vertical > 0.0 ? 1 : -1), 1, 10);
         return true;
      } else {
         if (vertical != 0.0 && !help && !WorldPanels.popup) {
            GodScreen.Dock d = new GodScreen.Dock();
            if (d.maxScroll > 0 && (d.inGrid(mx, my) || d.inBar(mx, my))) {
               int t = target() + (vertical > 0.0 ? -d.cell : d.cell);
               setTarget(MathHelper.clamp(t / d.cell * d.cell, 0, d.maxScroll));
               return true;
            }
         }

         double height = this.client.player.getY() - this.groundY();
         this.targetY = this.targetY - vertical * Math.max(2.0, height * 0.15);
         return true;
      }
   }

   @Override
   public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
      if (WorldMap.on) {
         WorldMap.drag(dx, dy);
         return true;
      } else if (this.dragBar && button == 0) {
         this.dragTo(new GodScreen.Dock(), my);
         return true;
      } else {
         return super.mouseDragged(mx, my, button, dx, dy);
      }
   }

   private void sendPower(Power p) {
      if (this.client != null && this.client.player != null && ClientPlayNetworking.canSend(Payloads.Use.ID)) {
         BlockPos pos = this.hover != null ? this.hover.getBlockPos() : this.client.player.getBlockPos();
         ClientPlayNetworking.send(new Payloads.Use(p.name(), pos, brush + shape * 100, true));
      }
   }

   private void nextCategory(int mods) {
      Category[] cs = Category.values();
      int dir = (mods & 1) != 0 ? cs.length - 1 : 1;
      this.setCategory(cs[(category.ordinal() + dir) % cs.length]);
   }

   @Override
   public boolean keyPressed(int key, int scan, int mods) {
      if (searching) {
         if (key == 256) {
            this.closeSearch();
         } else if (key == 259) {
            if ((mods & 2) != 0) {
               this.setQuery("");
            } else if (!query.isEmpty()) {
               this.setQuery(query.substring(0, query.length() - 1));
            }
         } else if (isPaste(key) && this.client != null) {
            String clip = this.client.keyboard.getClipboard();
            if (clip != null) {
               this.setQuery(query + clip.replace('\n', ' ').replace('\r', ' ').trim());
            }
         } else if (key == 257 || key == 335) {
            List<Power> r = list();
            if (listMode() && !r.isEmpty()) {
               this.select(r.get(MathHelper.clamp(cursor, 0, r.size() - 1)));
            } else {
               this.closeSearch();
            }
         } else if (key == 263) {
            this.moveSel(-1, 0);
         } else if (key == 262) {
            this.moveSel(1, 0);
         } else if (key == 265) {
            this.moveSel(0, -1);
         } else if (key == 264) {
            this.moveSel(0, 1);
         } else if (key == 258) {
            this.nextCategory(mods);
         } else if (key == 290) {
            this.closeSearch();
            help = true;
         }

         return true;
      } else if ((key != 70 || (mods & 2) == 0) && key != 47) {
         if (WorldMap.on && WorldMap.key(key)) {
            return true;
         } else if (key != 256 || !codex && !WorldMap.on && !help && !WorldPanels.popup) {
            if (GodKeys.is(GodKeys.HELP, key, scan) || key == 290) {
               help = !help;
               codex = false;
               WorldMap.on = false;
               return true;
            } else if (GodKeys.is(GodKeys.CHRONICLE, key, scan)) {
               ChronicleScreen.open();
               return true;
            } else if (GodKeys.is(GodKeys.ERAS, key, scan)) {
               WorldPanels.popup = !WorldPanels.popup;
               return true;
            } else if (GodKeys.is(GodKeys.CODEX, key, scan)) {
               codex = !codex;
               codexScroll = 0;
               WorldMap.on = false;
               help = false;
               return true;
            } else if (GodKeys.is(GodKeys.WORLD_MAP, key, scan)) {
               WorldMap.on = !WorldMap.on;
               codex = false;
               help = false;
               if (WorldMap.on && this.client != null && this.client.player != null) {
                  WorldMap.center(this.client.player.getX(), this.client.player.getZ());
               }

               return true;
            } else if (!codex || key != 265 && key != 264) {
               if (this.overlay() || key != 263 && key != 262 && key != 265 && key != 264) {
                  if (key == 257 || key == 335) {
                     this.select(selected);
                     return true;
                  } else if (key == 90 && (mods & 2) != 0) {
                     this.sendPower(Power.UNDO);
                     return true;
                  } else if (key == 80) {
                     this.sendPower(Power.TIME_PAUSE);
                     return true;
                  } else if (GodKeys.is(GodKeys.STATS, key, scan)) {
                     StatsPanel.on = !StatsPanel.on;
                     if (StatsPanel.on) {
                        ClientPlayNetworking.send(new Payloads.Use(Power.CIV_STATS.name(), BlockPos.ORIGIN, -1, true));
                     }

                     return true;
                  } else if (GodKeys.is(GodKeys.MAP, key, scan)) {
                     MiniMap.on = !MiniMap.on;
                     return true;
                  } else if (key == 67) {
                     this.follow = this.follow != null ? null : this.pickEntity();
                     return true;
                  } else if (key == 66) {
                     shape = (shape + 1) % SHAPES.length;
                     this.toast("Форма кисти: " + SHAPES[shape], -6631169);
                     return true;
                  } else if (key == 71) {
                     this.close();
                     return true;
                  } else if (key == 91 || key == 45) {
                     brush = Math.max(1, brush - 1);
                     return true;
                  } else if (key == 93 || key == 61) {
                     brush = Math.min(10, brush + 1);
                     return true;
                  } else if (key == 258) {
                     this.nextCategory(mods);
                     return true;
                  } else if (key >= 49 && key <= 57) {
                     int i = key - 49;
                     List<Power> l = (mods & 1) != 0 ? Favorites.list() : list();
                     if (i < l.size()) {
                        this.select(l.get(i));
                     }

                     return true;
                  } else {
                     return super.keyPressed(key, scan, mods);
                  }
               } else {
                  this.moveSel(key == 263 ? -1 : (key == 262 ? 1 : 0), key == 265 ? -1 : (key == 264 ? 1 : 0));
                  return true;
               }
            } else {
               codexScroll = Math.max(0, Math.min(600, codexScroll + (key == 265 ? -3 : 3)));
               return true;
            }
         } else {
            codex = false;
            WorldMap.on = false;
            help = false;
            WorldPanels.popup = false;
            return true;
         }
      } else {
         this.openSearch();
         return true;
      }
   }

   @Override
   public boolean charTyped(char chr, int mods) {
      if (searching) {
         if (chr == '/' && query.isEmpty()) {
            return true;
         } else {
            if (chr >= ' ' && query.length() < 30) {
               this.setQuery(query + chr);
            }

            return true;
         }
      } else {
         return super.charTyped(chr, mods);
      }
   }

   private boolean down(int key) {
      return searching ? false : GLFW.glfwGetKey(this.client.getWindow().getHandle(), key) == 1;
   }

   private double groundY() {
      ClientPlayerEntity p = this.client.player;
      return this.client.world.getTopY(Type.MOTION_BLOCKING, MathHelper.floor(p.getX()), MathHelper.floor(p.getZ()));
   }

   @Override
   public void tick() {
      if (this.client != null) {
         ClientPlayerEntity p = this.client.player;
         if (p != null && this.client.world != null) {
            float yaw = p.getYaw();
            if (this.down(81)) {
               yaw -= 4.0F;
            }

            if (this.down(69)) {
               yaw += 4.0F;
            }

            if (this.down(82)) {
               pitch = Math.min(90.0F, pitch + 2.0F);
            }

            if (this.down(70)) {
               pitch = Math.max(40.0F, pitch - 2.0F);
            }

            double fwd = 0.0;
            double side = 0.0;
            if (this.down(87)) {
               fwd++;
            }

            if (this.down(83)) {
               fwd--;
            }

            if (this.down(68)) {
               side++;
            }

            if (this.down(65)) {
               side--;
            }

            double ground = this.groundY();
            this.targetY = MathHelper.clamp(this.targetY, ground + 6.0, ground + 220.0);
            double height = Math.max(6.0, p.getY() - ground);
            double speed = Math.min(2.5, 0.35 + height * 0.02);
            if (this.down(340) || this.down(344)) {
               speed = Math.min(4.0, speed * 2.5);
            }

            double rad = Math.toRadians(yaw);
            double s = Math.sin(rad);
            double c = Math.cos(rad);
            double vx = (-s * fwd - c * side) * speed;
            double vz = (c * fwd - s * side) * speed;
            if (this.follow != null && (this.follow.isRemoved() || !this.follow.isAlive() || InspectScreen.followName != null)) {
               this.follow = null;
            }

            if (this.follow != null && fwd == 0.0 && side == 0.0) {
               vx = MathHelper.clamp((this.follow.getX() - p.getX()) * 0.3, -3.0, 3.0);
               vz = MathHelper.clamp((this.follow.getZ() - p.getZ()) * 0.3, -3.0, 3.0);
            }

            double vy = MathHelper.clamp((this.targetY - p.getY()) * 0.25, -4.0, 4.0);
            p.setVelocity(vx, vy, vz);
            p.setYaw(yaw);
            p.setPitch(pitch);
            double mx = this.client.mouse.getX() * this.width / this.client.getWindow().getWidth();
            double my = this.client.mouse.getY() * this.height / this.client.getWindow().getHeight();
            this.hover = this.raycast(mx, my);
            boolean overUi = !this.leftDown
               && (new GodScreen.Dock().inside(mx, my) || mx < this.railW() + 2 && my < 4 + Category.values().length * this.railCell() + 2);
            preview = this.hover != null && !overUi ? this.hover.getBlockPos() : null;
            MiniMap.tick(this.client);
            if (StatsPanel.on && ++StatsPanel.timer % 40 == 0) {
               ClientPlayNetworking.send(new Payloads.Use(Power.CIV_STATS.name(), BlockPos.ORIGIN, -1, true));
            }

            if (this.leftDown && selected.continuous && --this.repeatTimer <= 0) {
               this.usePower(false);
               this.repeatTimer = Math.max(1, selected.repeat);
            }
         } else {
            this.close();
         }
      }
   }

   private BlockHitResult raycast(double mx, double my) {
      if (this.client != null && this.client.world != null && this.client.player != null) {
         Camera cam = this.client.gameRenderer.getCamera();
         Vec3d origin = cam.getPos();
         double ndcX = 2.0 * mx / this.width - 1.0;
         double ndcY = 1.0 - 2.0 * my / this.height;
         double fov = Math.toRadians(this.client.options.getFov().getValue().intValue());
         double tanH = Math.tan(fov / 2.0);
         double aspect = (double)this.client.getWindow().getFramebufferWidth() / Math.max(1, this.client.getWindow().getFramebufferHeight());
         float yaw = cam.getYaw();
         float pt = cam.getPitch();
         Vec3d forward = Vec3d.fromPolar(pt, yaw);
         double yr = Math.toRadians(yaw);
         Vec3d right = new Vec3d(-Math.cos(yr), 0.0, -Math.sin(yr));
         Vec3d up = right.crossProduct(forward);
         Vec3d dir = forward.add(right.multiply(ndcX * tanH * aspect)).add(up.multiply(ndcY * tanH)).normalize();
         Vec3d end = origin.add(dir.multiply(600.0));
         BlockHitResult hit = this.client.world.raycast(new RaycastContext(origin, end, ShapeType.OUTLINE, FluidHandling.ANY, this.client.player));
         return hit != null && hit.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK ? hit : null;
      } else {
         return null;
      }
   }

   private Entity pickEntity() {
      if (this.client != null && this.client.world != null && this.client.player != null) {
         Vec3d h = this.hover != null ? Vec3d.ofCenter(this.hover.getBlockPos()) : this.client.player.getPos();
         Entity best = null;
         double bd = Double.MAX_VALUE;

         for (Entity e : this.client
            .world
            .getOtherEntities(
               this.client.player, new Box(h, h).expand(8.0, 12.0, 8.0), x -> x instanceof LivingEntity && !(x instanceof PlayerEntity) && x.isAlive()
            )) {
            double d = e.squaredDistanceTo(h);
            if (d < bd) {
               bd = d;
               best = e;
            }
         }

         return best;
      } else {
         return null;
      }
   }

   private void usePower(boolean first) {
      if (this.hover != null && ClientPlayNetworking.canSend(Payloads.Use.ID)) {
         ClientPlayNetworking.send(new Payloads.Use(selected.name(), this.hover.getBlockPos(), brush + shape * 100, first));
      }
   }

   @Override
   public void close() {
      this.sendOff();
      super.close();
   }

   @Override
   public void removed() {
      preview = null;
      searching = false;
      query = "";
      cursor = 0;
      help = false;
      codex = false;
      WorldMap.on = false;
      WorldPanels.popup = false;
      if (this.client != null && this.initialized) {
         this.client.options.hudHidden = this.hudWas;
         this.client.options.getFovEffectScale().setValue(this.fovEffectWas);
      }

      this.sendOff();
   }

   public void closeSilently() {
      this.sentOff = true;
      if (this.client != null) {
         this.client.setScreen(null);
      }
   }

   private void sendOff() {
      if (!this.sentOff) {
         this.sentOff = true;
         if (ClientPlayNetworking.canSend(Payloads.Toggle.ID)) {
            ClientPlayNetworking.send(new Payloads.Toggle(false));
         }
      }
   }

   private final class Dock {
      final int s = GodScreen.this.slot();
      final int cell = this.s + 2;
      final boolean rows = GodScreen.listMode();
      final List<Power> list = GodScreen.list();
      final int x0;
      final int x1;
      final int top;
      final int bottom;
      final int infoY;
      final int stripY;
      final int barY;
      final int gridY;
      final int gridH;
      final int gx0;
      final int gridR;
      final int gx;
      final int cols;
      final int cw;
      final int slotW;
      final int rowsAll;
      final int maxScroll;
      final int sbX;
      final int helpX;
      final int searchX;
      final int searchW;
      final List<Power> fav;
      final List<Power> rec;
      final int favX;
      final int recX;

      Dock() {
         int margin = GodScreen.this.railW() + 8;
         int minW = this.cell + 8 + 4 + 2;
         int w = Math.max(minW, Math.min(GodScreen.this.width - 2 * margin, 28 * this.cell + 8 + 4 + 2));
         this.x0 = (GodScreen.this.width - w) / 2;
         this.x1 = this.x0 + w;
         this.gx0 = this.x0 + 4;
         this.gridR = this.x1 - 4 - 4 - 2;
         this.sbX = this.gridR + 2;
         int gridW = this.gridR - this.gx0;
         this.cols = Math.max(1, this.rows ? gridW / 150 : gridW / this.cell);
         this.cw = this.rows ? gridW / this.cols : this.cell;
         this.slotW = this.rows ? this.cw - 2 : this.s;
         this.gx = this.rows ? this.gx0 : this.gx0 + (gridW - this.cols * this.cell) / 2;
         int fixed = this.s + 49;
         int cap = MathHelper.clamp((GodScreen.this.height - 4 - fixed - (4 + GodScreen.this.mapSize() + 8)) / this.cell, 1, this.rows ? 5 : 3);
         this.rowsAll = (this.list.size() + this.cols - 1) / this.cols;
         this.gridH = MathHelper.clamp(this.rowsAll, 1, cap) * this.cell;
         this.maxScroll = Math.max(0, this.rowsAll * this.cell - this.gridH);
         this.bottom = GodScreen.this.height - 4;
         this.gridY = this.bottom - 4 - this.gridH;
         this.barY = this.gridY - 14 - 1;
         this.stripY = this.barY - (this.s + 4);
         this.infoY = this.stripY - 24;
         this.top = this.infoY - 2;
         this.helpX = this.x1 - 4 - 12;
         this.searchW = MathHelper.clamp(w / 3, 40, 140);
         this.searchX = this.helpX - 4 - this.searchW;
         int capStrip = Math.max(0, (w - 8 - 12 - 14) / this.cell);
         List<Power> f = Favorites.list();
         List<Power> r = Recent.list();
         this.fav = f.subList(0, Math.min(f.size(), capStrip));
         this.rec = r.subList(0, Math.min(r.size(), capStrip - this.fav.size()));
         this.favX = this.x0 + 4 + 12;
         this.recX = this.x1 - 4 - this.rec.size() * this.cell;
         int t = MathHelper.clamp(GodScreen.target(), 0, this.maxScroll);
         if (t != GodScreen.target()) {
            GodScreen.setTarget(t);
         }

         GodScreen.scrollPx = MathHelper.clamp(GodScreen.scrollPx, 0.0, (double)this.maxScroll);
      }

      int scroll() {
         return (int)Math.round(GodScreen.scrollPx);
      }

      int cellX(int i) {
         return this.gx + i % this.cols * this.cw + 1;
      }

      int cellY(int i) {
         return this.gridY + i / this.cols * this.cell + 1 - this.scroll();
      }

      int stripSlotY() {
         return this.stripY + 2;
      }

      boolean inside(double mx, double my) {
         return mx >= this.x0 && mx < this.x1 && my >= this.top && my < this.bottom;
      }

      boolean inGrid(double mx, double my) {
         return mx >= this.gx0 && mx < this.gridR && my >= this.gridY && my < this.gridY + this.gridH;
      }

      boolean inSearch(double mx, double my) {
         return Ui.in(mx, my, this.searchX, this.barY + 1, this.searchW, 12);
      }

      boolean inHelp(double mx, double my) {
         return Ui.in(mx, my, this.helpX, this.barY + 1, 12, 12);
      }

      boolean inBar(double mx, double my) {
         return this.maxScroll > 0 && Ui.in(mx, my, this.sbX - 1, this.gridY, 6, this.gridH);
      }

      int thumbH() {
         return Math.max(8, this.gridH * this.gridH / Math.max(1, this.rowsAll * this.cell));
      }

      int thumbY() {
         return this.maxScroll == 0 ? this.gridY : this.gridY + (int)((this.gridH - this.thumbH()) * GodScreen.scrollPx / this.maxScroll);
      }

      int[] hit(double mx, double my) {
         if (this.inGrid(mx, my)) {
            for (int i = 0; i < this.list.size(); i++) {
               int x = this.cellX(i);
               int y = this.cellY(i);
               if (Ui.in(mx, my, x, y, this.slotW, this.s)) {
                  return new int[]{0, i, x, y, this.slotW};
               }
            }

            return null;
         } else {
            int sy = this.stripSlotY();

            for (int ix = 0; ix < this.fav.size(); ix++) {
               if (Ui.in(mx, my, this.favX + ix * this.cell, sy, this.s, this.s)) {
                  return new int[]{1, ix, this.favX + ix * this.cell, sy, this.s};
               }
            }

            for (int ixx = 0; ixx < this.rec.size(); ixx++) {
               if (Ui.in(mx, my, this.recX + ixx * this.cell, sy, this.s, this.s)) {
                  return new int[]{2, ixx, this.recX + ixx * this.cell, sy, this.s};
               }
            }

            return null;
         }
      }

      Power power(int[] h) {
         return h[0] == 0 ? this.list.get(h[1]) : (h[0] == 1 ? this.fav.get(h[1]) : this.rec.get(h[1]));
      }

      boolean onStar(int[] h, double mx, double my) {
         return Ui.in(mx, my, h[2] + h[4] - 9, h[3], 9, 9);
      }

      void reveal(int i) {
         if (i >= 0) {
            int y = i / this.cols * this.cell;
            int t = GodScreen.target();
            if (y < t) {
               t = y;
            }

            if (y + this.cell > t + this.gridH) {
               t = y + this.cell - this.gridH;
            }

            GodScreen.setTarget(MathHelper.clamp(t, 0, this.maxScroll));
         }
      }
   }
}
