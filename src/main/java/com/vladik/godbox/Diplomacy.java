package com.vladik.godbox;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.TraderLlamaEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

public final class Diplomacy {
   public static final int PEACE = 0;
   public static final int WAR = 1;
   public static final int ALLY = 2;
   public static boolean auto = true;
   private static final int N = Kingdoms.K.values().length;
   static final int[][] REL = new int[N][N];
   static final int[][] OP = new int[N][N];
   private static final int[][] TIME = new int[N][N];
   private static final List<Diplomacy.Caravan> CARAVANS = new ArrayList<>();
   static final String[] RES = new String[]{"дерево", "камень", "еда", "руда"};
   private static int timer;

   private Diplomacy() {
   }

   public static int rel(Kingdoms.K a, Kingdoms.K b) {
      return a == b ? 2 : REL[a.ordinal()][b.ordinal()];
   }

   public static boolean hostile(Kingdoms.K a, Kingdoms.K b) {
      return a != null && b != null && a != b && REL[a.ordinal()][b.ordinal()] == 1;
   }

   public static boolean allied(Kingdoms.K a, Kingdoms.K b) {
      return a != null && b != null && a != b && REL[a.ordinal()][b.ordinal()] == 2;
   }

   public static boolean atWar(Kingdoms.K k) {
      for (Kingdoms.K o : Kingdoms.K.values()) {
         if (o.alive && hostile(k, o)) {
            return true;
         }
      }

      return false;
   }

   public static boolean anyWar() {
      for (Kingdoms.K k : Kingdoms.K.values()) {
         if (k.alive && atWar(k)) {
            return true;
         }
      }

      return false;
   }

   static void set(Kingdoms.K a, Kingdoms.K b, int r) {
      int i = a.ordinal();
      int j = b.ordinal();
      REL[i][j] = REL[j][i] = r;
      TIME[i][j] = TIME[j][i] = 0;
   }

   static void op(Kingdoms.K a, Kingdoms.K b, int d) {
      int i = a.ordinal();
      int j = b.ordinal();
      OP[i][j] = OP[j][i] = MathHelper.clamp(OP[i][j] + d, -100, 100);
   }

   public static void setAll(boolean war) {
      for (Kingdoms.K a : Kingdoms.K.values()) {
         for (Kingdoms.K b : Kingdoms.K.values()) {
            if (a.ordinal() < b.ordinal()) {
               set(a, b, war ? 1 : 0);
               if (!war) {
                  OP[a.ordinal()][b.ordinal()] = OP[b.ordinal()][a.ordinal()] = 0;
               }
            }
         }
      }
   }

   public static void forget(Kingdoms.K k) {
      for (Kingdoms.K o : Kingdoms.K.values()) {
         if (o != k) {
            set(k, o, 0);
            OP[k.ordinal()][o.ordinal()] = OP[o.ordinal()][k.ordinal()] = 0;
         }
      }
   }

   static void broadcast(MinecraftServer s, Text t) {
      Chronicle.log(t.getString());
      if (s != null) {
         for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
            p.sendMessage(t, false);
         }
      }
   }

   static void declare(MinecraftServer s, Kingdoms.K a, Kingdoms.K b) {
      if (!Kingdoms.eternalPeace && !hostile(a, b) && !Treaties.truce(a, b)) {
         set(a, b, 1);
         Kingdoms.war = true;
         broadcast(s, Text.literal("⚔ " + a.title + " объявляет войну: " + b.title + "!").formatted(Formatting.RED, Formatting.BOLD));

         for (Kingdoms.K c : Kingdoms.K.values()) {
            if (c.alive && c != a && c != b) {
               if (allied(c, a) && !allied(c, b) && !hostile(c, b)) {
                  set(c, b, 1);
                  broadcast(s, Text.literal("\ud83d\udee1 " + c.title + " вступает в войну на стороне союзника").formatted(c.color));
               } else if (allied(c, b) && !allied(c, a) && !hostile(c, a)) {
                  set(c, a, 1);
                  broadcast(s, Text.literal("\ud83d\udee1 " + c.title + " защищает союзника: " + b.title).formatted(c.color));
               }
            }
         }
      }
   }

   static int mood(Kingdoms.K k) {
      return switch (Life.kingTrait(k)) {
         case 0 -> 2;
         case 1 -> -3;
         case 2 -> 1;
         default -> 0;
      };
   }

   public static void tick(MinecraftServer s) {
      timer++;
      if (timer % 20 == 0) {
         if (Kingdoms.eternalPeace && anyWar()) {
            setAll(false);
            Kingdoms.calm();
         }

         Kingdoms.war = anyWar();
         tickCaravans();
      }

      if (timer % 600 == 0 && auto && !Kingdoms.eternalPeace) {
         think(s);
      }

      if (timer % Seasons.tradePeriod() == 300 && Civ.economy) {
         trade();
      }
   }

   static void think(MinecraftServer s) {
      Kingdoms.K[] ks = Kingdoms.K.values();

      for (int i = 0; i < ks.length; i++) {
         for (int j = i + 1; j < ks.length; j++) {
            Kingdoms.K a = ks[i];
            Kingdoms.K b = ks[j];
            if (a.alive && b.alive && a.world != null && a.world == b.world && a.capital != null && b.capital != null) {
               Random r = a.world.getRandom();
               double dist = Math.sqrt(a.capital.getSquaredDistance(b.capital));
               int delta = r.nextInt(13) - 6 + (dist < 140.0 ? -3 : 1) + mood(a) + mood(b) + Races.opinion(a, b);
               int pa = a.members.size();
               int pb = b.members.size();
               if (pa > pb * 2 + 4 || pb > pa * 2 + 4) {
                  delta -= 2;
               }

               int rel = REL[i][j];
               TIME[i][j]++;
               TIME[j][i]++;
               if (rel == 2) {
                  delta += 2;
               }

               op(a, b, delta);
               int o = OP[i][j];
               if (rel == 0) {
                  if (o <= -55 - Seasons.warBias() && r.nextInt(3) == 0) {
                     declare(s, a, b);
                  } else if (o >= 55 && r.nextInt(3) == 0) {
                     set(a, b, 2);
                     broadcast(s, Text.literal("\ud83e\udd1d " + a.title + " и " + b.title + " заключили союз!").formatted(Formatting.GREEN, Formatting.BOLD));
                  }
               } else if (rel == 2) {
                  if (o < 5) {
                     set(a, b, 0);
                     broadcast(s, Text.literal("\ud83d\udc94 Союз " + a.title + " и " + b.title + " распался").formatted(Formatting.GRAY));
                  }
               } else {
                  int t = TIME[i][j];
                  if (t >= 6 && r.nextInt(100) < 10 + t * 3 && !Treaties.peace(s, a, b)) {
                     set(a, b, 0);
                     OP[i][j] = OP[j][i] = -10;
                     Kingdoms.calm();
                     broadcast(s, Text.literal("\ud83d\udd4a " + a.title + " и " + b.title + " подписали мирный договор").formatted(Formatting.WHITE));
                  }
               }
            }
         }
      }
   }

   static int get(Civ.City c, int t) {
      return switch (t) {
         case 0 -> c.wood;
         case 1 -> c.stone;
         case 2 -> c.food;
         default -> c.ore;
      };
   }

   static void add(Civ.City c, int t, int n) {
      switch (t) {
         case 0:
            c.wood = Math.max(0, c.wood + n);
            break;
         case 1:
            c.stone = Math.max(0, c.stone + n);
            break;
         case 2:
            c.food = Math.max(0, c.food + n);
            break;
         default:
            c.ore = Math.max(0, c.ore + n);
      }
   }

   static boolean busy(Kingdoms.K a, Kingdoms.K b) {
      for (Diplomacy.Caravan c : CARAVANS) {
         if (c.from == a && c.to == b) {
            return true;
         }
      }

      return false;
   }

   static boolean canTrade(Kingdoms.K a, Kingdoms.K b) {
      return a != b
         && a.alive
         && b.alive
         && a.world != null
         && a.world == b.world
         && a.capital != null
         && b.capital != null
         && !hostile(a, b)
         && a.capital.getSquaredDistance(b.capital) < 202500.0
         && a.world.shouldTickEntity(a.capital)
         && a.world.shouldTickEntity(b.capital);
   }

   static int pick(Civ.City ca, Civ.City cb, boolean force) {
      int best = -1;
      int bv = force ? Integer.MIN_VALUE : 0;

      for (int t = 0; t < 4; t++) {
         int have = get(ca, t);
         int min = t == 3 ? 4 : 30;
         if (have >= (force ? (t == 3 ? 2 : 8) : min)) {
            int v = have - get(cb, t) * (t == 3 ? 1 : 1);
            if (v > bv) {
               bv = v;
               best = t;
            }
         }
      }

      return best;
   }

   static void trade() {
      if (CARAVANS.size() < 6) {
         for (Kingdoms.K a : Kingdoms.K.values()) {
            for (Kingdoms.K b : Kingdoms.K.values()) {
               if (canTrade(a, b) && !busy(a, b) && !Plague.blocked(a, b)) {
                  Random r = a.world.getRandom();
                  int chance = (allied(a, b) ? 70 : 40) + (Life.kingTrait(a) == 2 ? 25 : 0);
                  if (r.nextInt(100) < chance) {
                     Civ.City ca = Civ.city(a);
                     Civ.City cb = Civ.city(b);
                     int t = pick(ca, cb, false);
                     if (t >= 0) {
                        send(a, b, t);
                     }
                  }
               }
            }
         }
      }
   }

   static boolean send(Kingdoms.K a, Kingdoms.K b, int t) {
      ServerWorld w = a.world;
      Civ.City ca = Civ.city(a);
      int have = get(ca, t);
      int amount = t == 3 ? Math.max(1, have / 3) : Math.max(4, Math.min(30, have / 3));
      if (have < amount) {
         return false;
      } else {
         BlockPos at = Civ.ground(w, a.capital.getX() + 3, a.capital.getZ() + 3);
         TraderLlamaEntity e = EntityType.TRADER_LLAMA.create(w);
         if (e == null) {
            return false;
         } else {
            e.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
            e.setHasChest(true);
            e.setTame(true);
            e.setPersistent();
            e.setCustomName(Text.literal("Караван → " + b.title + " (" + RES[t] + " " + amount + ")").formatted(a.color));
            e.setCustomNameVisible(true);
            w.spawnEntity(e);
            add(ca, t, -amount);
            CARAVANS.add(new Diplomacy.Caravan(e, a, b, t, amount));
            Plague.depart(a, b);
            Fx.sound(w, e.getX(), e.getY(), e.getZ(), SoundEvents.ENTITY_LLAMA_CHEST, 1.0F, 1.0F);
            Fx.particles(w, ParticleTypes.HAPPY_VILLAGER, e.getX(), e.getY() + 1.0, e.getZ(), 8, 0.5, 0.5, 0.5, 0.02);
            Civ.tell(w, Text.literal("\ud83d\udc2a " + a.title + " отправило караван в " + b.title + ": " + RES[t] + " " + amount).formatted(a.color));
            return true;
         }
      }
   }

   static void deliver(Diplomacy.Caravan c) {
      if (c.to.alive) {
         Civ.City ct = Civ.city(c.to);
         add(ct, c.type, c.amount);
         if (c.from.alive) {
            add(Civ.city(c.from), 3, 1 + c.amount / 10);
         }

         op(c.from, c.to, 6);
         Tech.caravan(c.from, c.to);
         Plague.arrive(c.from, c.to, 0.6, "с караваном из");
         ServerWorld w = c.to.world;
         if (w != null) {
            TraderLlamaEntity e = c.e;
            Fx.particles(w, ParticleTypes.HAPPY_VILLAGER, e.getX(), e.getY() + 1.0, e.getZ(), 16, 0.6, 0.6, 0.6, 0.02);
            Fx.sound(w, e.getX(), e.getY(), e.getZ(), SoundEvents.ENTITY_VILLAGER_YES, 1.0F, 1.0F);
            Civ.tell(w, Text.literal("\ud83d\udc2a Караван доставил в " + c.to.title + ": +" + c.amount + " (" + RES[c.type] + ")").formatted(c.to.color));
         }
      }
   }

   static void tickCaravans() {
      Iterator<Diplomacy.Caravan> it = CARAVANS.iterator();

      while (it.hasNext()) {
         Diplomacy.Caravan c = it.next();
         TraderLlamaEntity e = c.e;
         if (!c.to.alive || c.to.capital == null) {
            e.discard();
            it.remove();
         } else if (!e.isAlive()) {
            if (e.isDead()) {
               op(c.from, c.to, -6);
               if (c.from.world != null) {
                  Civ.tell(c.from.world, Text.literal("\ud83d\udcb0 Караван из " + c.from.title + " разграблен!").formatted(Formatting.GOLD));
               }
            } else {
               deliver(c);
            }

            it.remove();
         } else {
            c.age += 20;
            double dx = c.to.capital.getX() + 0.5 - e.getX();
            double dz = c.to.capital.getZ() + 0.5 - e.getZ();
            double d = Math.sqrt(dx * dx + dz * dz);
            if (!(d < 6.0) && c.age <= 7200) {
               ServerWorld w = (ServerWorld)e.getWorld();
               double step = Math.min(18.0, d);
               BlockPos g = Civ.ground(w, MathHelper.floor(e.getX() + dx / d * step), MathHelper.floor(e.getZ() + dz / d * step));
               e.getNavigation().startMovingTo(g.getX() + 0.5, g.getY(), g.getZ() + 0.5, 1.4);
               if (d < c.best - 0.5) {
                  c.best = d;
                  c.stuck = 0;
               } else if (++c.stuck >= 6) {
                  c.stuck = 0;
                  c.best = 1.0E9;
                  Fx.particles(w, ParticleTypes.CLOUD, e.getX(), e.getY() + 1.0, e.getZ(), 8, 0.4, 0.5, 0.4, 0.02);
                  e.requestTeleport(g.getX() + 0.5, g.getY(), g.getZ() + 0.5);
               }
            } else {
               deliver(c);
               e.discard();
               it.remove();
            }
         }
      }
   }

   static Kingdoms.K nearest(ServerWorld w, BlockPos p) {
      Kingdoms.K best = null;
      double bd = 62500.0;

      for (Kingdoms.K k : Kingdoms.K.values()) {
         if (k.alive && k.world == w && k.capital != null) {
            double d = k.capital.getSquaredDistance(p);
            if (d < bd) {
               bd = d;
               best = k;
            }
         }
      }

      return best;
   }

   public static void sway(ServerPlayerEntity god, ServerWorld w, BlockPos pos, int d) {
      Kingdoms.K k = nearest(w, pos);
      if (k == null) {
         god.sendMessage(Text.literal("Рядом нет королевств").formatted(Formatting.GRAY), true);
      } else {
         for (Kingdoms.K o : Kingdoms.K.values()) {
            if (o.alive && o != k) {
               op(k, o, d);
            }
         }

         BlockPos c = k.capital;
         Fx.particles(w, d > 0 ? ParticleTypes.HEART : ParticleTypes.ANGRY_VILLAGER, c.getX() + 0.5, c.getY() + 3, c.getZ() + 0.5, 40, 4.0, 2.0, 4.0, 0.05);
         god.sendMessage(Text.literal(k.title + (d > 0 ? " теперь дружелюбнее к соседям" : " теперь ненавидит соседей")).formatted(k.color), true);
         if (auto) {
            think(god.getServer());
         }
      }
   }

   public static void forceCaravan(ServerPlayerEntity god, ServerWorld w, BlockPos pos) {
      Kingdoms.K a = nearest(w, pos);
      if (a == null) {
         god.sendMessage(Text.literal("Рядом нет королевств").formatted(Formatting.GRAY), true);
      } else {
         Kingdoms.K best = null;
         double bd = 1.0E18;

         for (Kingdoms.K b : Kingdoms.K.values()) {
            if (canTrade(a, b)) {
               double d = a.capital.getSquaredDistance(b.capital);
               if (d < bd) {
                  bd = d;
                  best = b;
               }
            }
         }

         if (best == null) {
            god.sendMessage(Text.literal("Нет соседей для торговли (нужно второе королевство без войны)").formatted(Formatting.YELLOW), true);
         } else {
            int t = pick(Civ.city(a), Civ.city(best), true);
            if (t < 0 || !send(a, best, t)) {
               god.sendMessage(Text.literal(a.title + ": склад пуст, нечем торговать").formatted(Formatting.YELLOW), true);
            }
         }
      }
   }

   public static void info(ServerPlayerEntity god) {
      MutableText t = Text.literal("\ud83d\udcdc Дипломатия (авто: " + (auto ? "вкл" : "выкл") + ", караванов в пути: " + CARAVANS.size() + ")")
         .formatted(Formatting.GOLD);
      Kingdoms.K[] ks = Kingdoms.K.values();
      int alive = 0;

      for (Kingdoms.K k : ks) {
         if (k.alive) {
            alive++;
            t.append(Text.literal("\n" + k.title + " — " + Life.describeKing(k)).formatted(k.color));
         }
      }

      for (int i = 0; i < ks.length; i++) {
         for (int j = i + 1; j < ks.length; j++) {
            if (ks[i].alive && ks[j].alive) {
               int r = REL[i][j];
               String s = r == 1 ? "⚔ война" : (r == 2 ? "\ud83e\udd1d союз" : "мир");
               Formatting f = r == 1 ? Formatting.RED : (r == 2 ? Formatting.GREEN : Formatting.WHITE);
               t.append(Text.literal("\n  " + ks[i].title + " ↔ " + ks[j].title + ": " + s + " (отношение " + OP[i][j] + ")").formatted(f));
            }
         }
      }

      Treaties.info(t);
      if (alive == 0) {
         t.append(Text.literal("\nКоролевств пока нет").formatted(Formatting.GRAY));
      }

      god.sendMessage(t, false);
   }

   public static void clear() {
      for (Diplomacy.Caravan c : CARAVANS) {
         if (c.e.isAlive()) {
            c.e.discard();
         }
      }

      CARAVANS.clear();

      for (int i = 0; i < N; i++) {
         for (int j = 0; j < N; j++) {
            REL[i][j] = 0;
            OP[i][j] = 0;
            TIME[i][j] = 0;
         }
      }

      timer = 0;
   }

   static final class Caravan {
      final TraderLlamaEntity e;
      final Kingdoms.K from;
      final Kingdoms.K to;
      final int type;
      final int amount;
      int age;
      int stuck;
      double best = 1.0E9;

      Caravan(TraderLlamaEntity e, Kingdoms.K from, Kingdoms.K to, int type, int amount) {
         this.e = e;
         this.from = from;
         this.to = to;
         this.type = type;
         this.amount = amount;
      }
   }
}
