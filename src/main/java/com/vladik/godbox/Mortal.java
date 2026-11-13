package com.vladik.godbox;

import com.vladik.godbox.net.MortalPayloads;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public final class Mortal {
   public static final int ACTIVE = 0;
   public static final int WON = 1;
   public static final int FAILED = 2;
   private static final int INCOME_EVERY = 100;
   private static final int STEP = 20;
   private static final int[] BAD_EVENTS = new int[]{1, 3, 4, 7};
   public static boolean on;
   static boolean started;
   static double pool;
   static Mortal.S scen = Mortal.S.NONE;
   static int state;
   static int elapsed;
   static int warTicks;
   static int mile;
   static int wonMask;
   static int cursor;
   static Kingdoms.K target;
   static String progress = "";
   static final EnumSet<Kingdoms.K> START = EnumSet.noneOf(Kingdoms.K.class);
   private static final EnumSet<Kingdoms.K> WAS_ALIVE = EnumSet.noneOf(Kingdoms.K.class);
   private static final Map<Kingdoms.K, Integer> SULK = new EnumMap<>(Kingdoms.K.class);
   private static final Map<UUID, Long> WARNED = new HashMap<>();
   private static int t;
   private static int perMin;
   private static boolean sentOn;
   private static final Map<String, Integer> PRICE = new HashMap<>();
   public static final double REPEAT = 0.25;

   private Mortal() {
   }

   private static void price(int c, String... names) {
      for (String n : names) {
         PRICE.put(n, c);
      }
   }

   public static int cost(Power p) {
      if (p != null && p.category != null) {
         String n = p.name();
         Integer fixed = PRICE.get(n);
         if (fixed != null) {
            return fixed;
         } else if (n.contains("INFO")
            || n.contains("STATS")
            || n.contains("CHRONICLE")
            || n.contains("HISTORY")
            || n.contains("INSPECT")
            || n.contains("ACHIEVEMENT")
            || n.contains("CODEX")) {
            return 0;
         } else if (n.startsWith("GEN_")) {
            return 400;
         } else if (n.startsWith("BOSS_")) {
            return 250;
         } else if (n.startsWith("WONDER_")) {
            return 200;
         } else if (n.startsWith("GIFT_")) {
            return 20;
         } else if (n.startsWith("CR_")) {
            return 15;
         } else {
            boolean c = p.continuous;

            return switch (p.category) {
               case LAWS, WORLD -> 0;
               case BOMBS -> c ? 15 : 40;
               case DISASTERS -> c ? 12 : 60;
               case DIVINE -> c ? 8 : 25;
               case LIFE -> 5;
               case BOSSES -> 250;
               case KINGDOMS, CIVIL -> c ? 8 : 30;
               case TERRAIN -> 3;
               case BIOMES -> 4;
               case BUILD -> c ? 10 : 60;
               case GENERATE -> 6;
               default -> c ? 8 : 30;
            };
         }
      } else {
         return 0;
      }
   }

   static double scale() {
      return Math.max(0.0, Config.d("mortal.cost_scale", 1.0));
   }

   static int cap() {
      return Math.max(1, Config.i("mortal.cap", 1000));
   }

   public static boolean pay(ServerPlayerEntity god, Power p, boolean first) {
      if (!on || p == null || p == Power.UNDO || p == Power.CAMERA_TP || !GodManager.isGod(god)) {
         return true;
      } else if (!first && !p.continuous) {
         return true;
      } else {
         double c = cost(p) * scale() * (first ? 1.0 : 0.25);
         if (c <= 0.0) {
            return true;
         } else if (pool >= c) {
            pool -= c;
            return true;
         } else {
            long now = System.currentTimeMillis();
            Long last = WARNED.get(god.getUuid());
            if (last == null || now - last > 1500L) {
               WARNED.put(god.getUuid(), now);
               god.sendMessage(
                  Text.literal("Недостаточно божьей силы: нужно " + (int)Math.ceil(c) + " (есть " + (int)pool + ")").formatted(Formatting.RED), true
               );
            }

            return false;
         }
      }
   }

   static void gain(double n) {
      pool = Math.max(0.0, Math.min((double)cap(), pool + n));
   }

   public static void toggle(ServerPlayerEntity god) {
      on = !on;
      if (on && !started) {
         started = true;
         pool = Math.min(cap(), Config.i("mortal.start", 300));
      }

      god.sendMessage(
         Text.literal("Закон «Смертный бог»: ")
            .formatted(Formatting.GOLD)
            .append(Text.literal(on ? "ВКЛ" : "ВЫКЛ").formatted(on ? Formatting.GREEN : Formatting.RED, Formatting.BOLD))
            .append(Text.literal(on ? " — божьей силы: " + (int)pool : " — силы снова бесплатны").formatted(Formatting.GRAY)),
         true
      );
      if (on) {
         Chronicle.log("Бог стал смертным: теперь чудеса стоят божьей силы");
      }
   }

   static void onPrayerAnswered(MinecraftServer s, Kingdoms.K k) {
      if (on) {
         int n = Config.i("mortal.prayer", 60);
         gain(n);
         SULK.remove(k);
         tellGods(s, Text.literal("✦ Молитва услышана: +" + n + " божьей силы").formatted(Formatting.GOLD), true);
      }
   }

   static void onPrayerIgnored(Kingdoms.K k) {
      if (on) {
         SULK.put(k, Config.i("mortal.sulk_ticks", 2400));
      }
   }

   private static void income(MinecraftServer s) {
      double trickle = Config.d("mortal.trickle", 1.5);
      double sum = 0.0;

      for (Kingdoms.K k : Kingdoms.K.values()) {
         if (!k.alive) {
            if (WAS_ALIVE.remove(k)) {
               int loss = Config.i("mortal.death_loss", 40);
               gain(-loss);
               tellGods(s, Text.literal("✦ " + k.title + " пало: −" + loss + " божьей силы").formatted(Formatting.RED), false);
            }

            SULK.remove(k);
         } else {
            WAS_ALIVE.add(k);
            Civ.City c = Civ.city(k);
            double v = trickle * (Prayers.faith(k) / 50.0) * Math.min(2.0, 0.5 + Vassals.pop(k) / 30.0);
            v *= 1.0 + Math.min(50, Math.max(0, c.faith)) / 100.0;
            if (c.religion >= 0) {
               v *= 1.1;
            }

            Integer sulk = SULK.get(k);
            if (sulk != null) {
               v *= 0.5;
               if (sulk <= 100) {
                  SULK.remove(k);
               } else {
                  SULK.put(k, sulk - 100);
               }
            }

            sum += v;
         }
      }

      perMin = (int)Math.round(sum * 1200.0 / 100.0);
      gain(sum);
   }

   private static int years(String key, int def) {
      return Math.max(1, Config.i("mortal." + key, def));
   }

   private static int goldenEra() {
      return Math.max(1, Math.min(Eras.NAMES.length - 1, Config.i("mortal.golden.era", 7)));
   }

   private static int alive() {
      int n = 0;

      for (Kingdoms.K k : Kingdoms.K.values()) {
         if (k.alive) {
            n++;
         }
      }

      return n;
   }

   static String goal(Mortal.S x) {
      return switch (x) {
         case LONG_LIFE -> "сохраните " + (target != null ? target.title : "королевство") + " на " + years("long_life.years", 40) + " лет";
         case PEACE -> years("peace.years", 30)
            + " лет ни одно королевство не падёт и ни одна война не продлится дольше "
            + years("peace.war_years", 3)
            + " лет";
         case EMPIRE -> "одно королевство должно подчинить все остальные — захватом или вассалитетом";
         case GOLDEN -> "все королевства должны дойти до эпохи «" + Eras.NAMES[goldenEra()] + "», ни одно не должно пасть";
         case SURVIVAL -> "через " + years("survival.years", 30) + " лет должно уцелеть хотя бы 3 королевства; беды придут сами";
         default -> "";
      };
   }

   public static void cycle(ServerPlayerEntity god, ServerWorld w, BlockPos pos) {
      if (!on) {
         god.sendMessage(Text.literal("Сначала включите закон «Смертный бог»").formatted(Formatting.RED), true);
      } else {
         Mortal.S[] all = Mortal.S.values();
         cursor = (cursor + 1) % all.length;
         Mortal.S next = all[cursor];
         scen = Mortal.S.NONE;
         state = 0;
         mile = 0;
         warTicks = 0;
         elapsed = 0;
         target = null;
         progress = "";
         START.clear();
         if (next == Mortal.S.NONE) {
            god.sendMessage(Text.literal("Испытание снято").formatted(Formatting.GRAY), true);
         } else {
            int n = alive();
            String err = null;
            switch (next) {
               case LONG_LIFE:
                  target = Tech.nearest(w, pos);
                  if (target == null || !target.alive) {
                     err = "рядом нет королевства";
                  }
                  break;
               case PEACE:
                  if (n < 2) {
                     err = "нужно хотя бы 2 королевства";
                  } else if (Kingdoms.eternalPeace) {
                     err = "выключите закон «Вечный мир»";
                  }
                  break;
               case EMPIRE:
                  if (n < 2) {
                     err = "нужно хотя бы 2 королевства";
                  }
                  break;
               case GOLDEN:
                  if (n < 1) {
                     err = "нет ни одного королевства";
                  }
                  break;
               case SURVIVAL:
                  if (n < 3) {
                     err = "нужно хотя бы 3 королевства";
                  }
            }

            if (err != null) {
               target = null;
               god.sendMessage(
                  Text.literal("Испытание «" + next.title + "» недоступно: " + err + ". Нажмите ещё раз — следующее").formatted(Formatting.RED), true
               );
            } else {
               for (Kingdoms.K k : Kingdoms.K.values()) {
                  if (k.alive) {
                     START.add(k);
                  }
               }

               scen = next;
               if (next == Mortal.S.SURVIVAL) {
                  Events.on = true;
                  Tectonics.on = true;
               }

               tellGods(
                  god.getServer(),
                  Text.literal("✦ Испытание «" + next.title + "»: ").formatted(Formatting.GOLD).append(Text.literal(goal(next)).formatted(Formatting.GRAY)),
                  false
               );
               Chronicle.log("Бог принял испытание «" + next.title + "»");
            }
         }
      }
   }

   private static void milestone(MinecraftServer s, int value) {
      if (value > mile) {
         int n = Config.i("mortal.milestone", 50) * (value - mile);
         mile = value;
         gain(n);
         tellGods(s, Text.literal("✦ Испытание «" + scen.title + "»: веха пройдена, +" + n + " божьей силы").formatted(Formatting.GOLD), false);
      }
   }

   private static void finish(MinecraftServer s, boolean won, String why) {
      state = won ? 1 : 2;
      progress = won ? "пройдено" : "провалено: " + why;
      String sub;
      if (won) {
         int n = Config.i("mortal.win", 300);
         wonMask = wonMask | 1 << scen.ordinal();
         gain(n);
         sub = "«" + scen.title + "» — +" + n + " божьей силы";
         Chronicle.log("Бог прошёл испытание «" + scen.title + "»");
      } else {
         sub = "«" + scen.title + "»: " + why;
         Chronicle.log("Бог провалил испытание «" + scen.title + "»: " + why);
      }

      Text title = Text.literal(won ? "Испытание пройдено" : "Испытание провалено").formatted(won ? Formatting.GOLD : Formatting.RED);

      for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
         if (GodManager.isGod(p)) {
            p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(sub).formatted(Formatting.GRAY)));
            p.networkHandler.sendPacket(new TitleS2CPacket(title));
            p.sendMessage(Text.literal("✦ ").formatted(Formatting.GOLD).append(title).append(Text.literal(" — " + sub).formatted(Formatting.GRAY)), false);
         }
      }
   }

   static int wins() {
      return Integer.bitCount(wonMask);
   }

   private static void scenario(MinecraftServer s) {
      if (scen != Mortal.S.NONE && state == 0) {
         int alive = alive();
         Kingdoms.K fallen = null;

         for (Kingdoms.K k : START) {
            if (!k.alive) {
               fallen = k;
            }
         }

         int year = 600;
         switch (scen) {
            case LONG_LIFE:
               if (target != null && target.alive) {
                  int needxx = years("long_life.years", 40);
                  elapsed += 20;
                  int yx = elapsed / year;
                  progress = target.title + ": " + Math.min(yx, needxx) + "/" + needxx + " лет";
                  if (yx >= needxx) {
                     finish(s, true, "");
                  } else {
                     milestone(s, yx * 4 / needxx);
                  }
                  break;
               }

               finish(s, false, (target == null ? "королевство" : target.title) + " пало");
               return;
            case PEACE:
               if (fallen != null) {
                  finish(s, false, fallen.title + " пало");
                  return;
               }

               int needx = years("peace.years", 30);
               int maxWar = years("peace.war_years", 3);
               warTicks = !Kingdoms.war && !Diplomacy.anyWar() ? 0 : warTicks + 20;
               if (warTicks > maxWar * year) {
                  finish(s, false, "война длилась дольше " + maxWar + " лет");
                  return;
               }

               if (!Kingdoms.eternalPeace) {
                  elapsed += 20;
               }

               int yx = elapsed / year;
               progress = Math.min(yx, needx)
                  + "/"
                  + needx
                  + " лет"
                  + (
                     Kingdoms.eternalPeace
                        ? " · закон «Вечный мир» включён, годы не идут"
                        : (warTicks > 0 ? " · война " + warTicks / year + "/" + maxWar + " лет" : " · мир")
                  );
               if (yx >= needx) {
                  finish(s, true, "");
               } else {
                  milestone(s, yx * 4 / needx);
               }
               break;
            case EMPIRE:
               if (alive == 0) {
                  finish(s, false, "все королевства пали");
                  return;
               }

               int rivals = Integer.MAX_VALUE;

               for (Kingdoms.K kx : Kingdoms.K.values()) {
                  if (kx.alive) {
                     int r = 0;

                     for (Kingdoms.K o : Kingdoms.K.values()) {
                        if (o != kx && o.alive && Vassals.lord(o) != kx) {
                           r++;
                        }
                     }

                     rivals = Math.min(rivals, r);
                  }
               }

               int total = Math.max(1, START.size() - 1);
               int done = Math.max(0, Math.min(total, total - rivals));
               progress = "подчинено " + done + "/" + total;
               if (rivals == 0) {
                  finish(s, true, "");
               } else {
                  milestone(s, done);
               }
               break;
            case GOLDEN:
               if (fallen == null && alive != 0) {
                  int era = goldenEra();
                  int reached = 0;
                  int low = Integer.MAX_VALUE;

                  for (Kingdoms.K kxx : Kingdoms.K.values()) {
                     if (kxx.alive) {
                        int e = Civ.city(kxx).era;
                        if (e >= era) {
                           reached++;
                        }

                        low = Math.min(low, e);
                     }
                  }

                  progress = "«" + Eras.NAMES[era] + "»: " + reached + "/" + alive + " королевств · отстающее в эпохе " + (low + 1) + "/" + (era + 1);
                  if (reached == alive) {
                     finish(s, true, "");
                  } else {
                     milestone(s, reached);
                  }
                  break;
               }

               finish(s, false, (fallen == null ? "королевство" : fallen.title) + " пало");
               return;
            case SURVIVAL:
               if (alive < 3) {
                  finish(s, false, "осталось меньше трёх королевств");
                  return;
               }

               Events.on = true;
               Tectonics.on = true;
               int need = years("survival.years", 30);
               int strike = years("survival.strike_years", 2) * year;
               elapsed += 20;
               int y = elapsed / year;
               progress = Math.min(y, need) + "/" + need + " лет · королевств: " + alive;
               if (y >= need) {
                  finish(s, true, "");
                  return;
               }

               milestone(s, y * 4 / need);
               if (elapsed % strike < 20 && Perf.go) {
                  strike(s);
               }
         }
      }
   }

   private static void strike(MinecraftServer s) {
      List<Kingdoms.K> can = new ArrayList<>();

      for (Kingdoms.K k : Kingdoms.K.values()) {
         if (k.alive && k.world != null && k.capital != null && k.world.shouldTickEntity(k.capital)) {
            can.add(k);
         }
      }

      if (!can.isEmpty()) {
         Kingdoms.K kx = can.get(s.getOverworld().getRandom().nextInt(can.size()));

         try {
            Events.fire(s, kx, BAD_EVENTS[kx.world.getRandom().nextInt(BAD_EVENTS.length)]);
            tellGods(s, Text.literal("✦ Испытание шлёт беду: " + kx.title).formatted(Formatting.RED), true);
         } catch (Exception var6) {
            GodBox.LOG.error("GodBox: беда испытания не удалась", (Throwable)var6);
         }
      }
   }

   private static void tellGods(MinecraftServer s, Text msg, boolean bar) {
      if (s != null) {
         for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
            if (GodManager.isGod(p)) {
               p.sendMessage(msg, bar);
            }
         }
      }
   }

   public static void tick(MinecraftServer s) {
      if (on && !s.getTickManager().isFrozen()) {
         t++;
         if (t % 100 == 0) {
            income(s);
         }

         if (t % 20 == 0) {
            scenario(s);
         }
      }
   }

   public static MortalPayloads.MortalHud hud() {
      if (!on && !sentOn) {
         return null;
      } else {
         sentOn = on;
         return new MortalPayloads.MortalHud(on, (int)pool, cap(), scen.title, progress, state, (int)Math.round(scale() * 100.0), perMin);
      }
   }

   public static void init() {
      SaveExt.register("mortal", Mortal::save, Mortal::load, Mortal::clear);
   }

   private static List<String> save() {
      if (!on && !started && wonMask == 0) {
         return List.of();
      } else {
         StringBuilder st = new StringBuilder();

         for (Kingdoms.K k : START) {
            st.append(st.length() == 0 ? "" : ",").append(k.name());
         }

         return List.of(
            (on ? 1 : 0)
               + "|"
               + (started ? 1 : 0)
               + "|"
               + pool
               + "|"
               + scen.name()
               + "|"
               + state
               + "|"
               + elapsed
               + "|"
               + (target == null ? "-" : target.name())
               + "|"
               + warTicks
               + "|"
               + mile
               + "|"
               + wonMask
               + "|"
               + cursor
               + "|"
               + (st.length() == 0 ? "-" : st)
               + "|"
               + progress.replace('|', '/').replace('\n', ' ')
         );
      }
   }

   private static Kingdoms.K kingdom(String name) {
      try {
         return Kingdoms.K.valueOf(name);
      } catch (IllegalArgumentException var2) {
         return null;
      }
   }

   private static void load(String[] a) {
      if (a.length >= 12) {
         try {
            on = a[0].equals("1");
            started = a[1].equals("1");
            pool = Math.max(0.0, Double.parseDouble(a[2]));
            scen = Mortal.S.valueOf(a[3]);
            state = Integer.parseInt(a[4]);
            elapsed = Integer.parseInt(a[5]);
            target = kingdom(a[6]);
            warTicks = Integer.parseInt(a[7]);
            mile = Integer.parseInt(a[8]);
            wonMask = Integer.parseInt(a[9]);
            cursor = Math.floorMod(Integer.parseInt(a[10]), Mortal.S.values().length);
            START.clear();

            for (String n : a[11].split(",")) {
               Kingdoms.K k = kingdom(n);
               if (k != null) {
                  START.add(k);
               }
            }

            progress = a.length > 12 ? a[12] : "";
         } catch (IllegalArgumentException var6) {
            GodBox.LOG.warn("GodBox: состояние смертного бога повреждено, сброшено");
            clear();
         }
      }
   }

   public static void clear() {
      sentOn = false;
      started = false;
      on = false;
      pool = 0.0;
      scen = Mortal.S.NONE;
      perMin = 0;
      t = 0;
      cursor = 0;
      wonMask = 0;
      mile = 0;
      warTicks = 0;
      elapsed = 0;
      state = 0;
      target = null;
      progress = "";
      START.clear();
      WAS_ALIVE.clear();
      SULK.clear();
      WARNED.clear();
   }

   static {
      price(0, "PRAYER_ANSWER", "POSSESS", "GOD_HAND", "KINGDOM_DIPLO", "KINGDOM_CREST", "BOSS_REMOVE", "SCENARIO", "WORLD_CLEAR_ITEMS");
      price(5, "WORLD_DAY", "WORLD_NIGHT", "WORLD_CLEAR", "WORLD_RAIN", "WORLD_STORM", "BOSS_ORDER");
      price(25, "WORLD_BANISH");
      price(250, "ATOMIC");
      price(400, "CZAR_BOMBA");
      price(350, "ANTIMATTER", "BLACK_HOLE");
      price(300, "GREY_GOO", "ZOMBIE_APOCALYPSE", "ALIEN_INVASION", "ICE_AGE");
      price(200, "TSUNAMI", "LIFE_ERASER", "CIV_ANNEX", "LIVE_VOLCANO");
      price(150, "METEOR_RAIN", "SPAWN_WITHER", "SPAWN_DRAGON", "SPAWN_TITAN", "SPAWN_UFO", "AGE_UP", "VASSALIZE");
      price(120, "NECRO_SPAWN", "VOLCANO", "RIFT", "EARTHQUAKE");
      price(100, "KINGDOM_DISBAND", "CIVIL_WAR", "PLAGUE", "ZOMBIE_INFECTION");
      price(80, "TECH_GIFT");
      price(50, "KINGDOM_RED", "KINGDOM_BLUE", "KINGDOM_GREEN", "KINGDOM_YELLOW", "GIFT_ALL", "FAITH_MIRACLE");
      price(40, "KINGDOM_WAR", "KINGDOM_PEACE", "MAGE_SPAWN");
      price(20, "DEMOLISH", "DEMOLISH_AREA", "SPAWN_RAVAGER", "SPAWN_GOLEM");
      price(10, "PREDATORS");
   }

   public static enum S {
      NONE(""),
      LONG_LIFE("Долгая жизнь"),
      PEACE("Вечный мир"),
      EMPIRE("Империя"),
      GOLDEN("Золотой век"),
      SURVIVAL("Выживание");

      public final String title;

      private S(String title) {
         this.title = title;
      }
   }
}
