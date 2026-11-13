package com.vladik.godbox;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

public final class Seasons {
   public static boolean on = true;
   public static final int SPRING = 0;
   public static final int SUMMER = 1;
   public static final int AUTUMN = 2;
   public static final int WINTER = 3;
   static final String[] NAMES = new String[]{"Весна", "Лето", "Осень", "Зима"};
   private static final String[] ICONS = new String[]{"\ud83c\udf31", "☀", "\ud83c\udf42", "❄"};
   private static final String[] HINTS = new String[]{
      "Наступила весна — посевы растут быстрее, в семьях прибавление",
      "Наступило лето — поля дают полный урожай, но выше риск засух и пожаров",
      "Наступила осень — богатый урожай, караваны ходят чаще",
      "Наступила зима — поля почти не родят, еды уходит больше, армии неохотно идут в поход"
   };
   private static final Formatting[] COLORS = new Formatting[]{Formatting.GREEN, Formatting.YELLOW, Formatting.GOLD, Formatting.AQUA};
   static int season;
   static int time;
   static int year = 1;
   private static int t;
   private static final Map<Kingdoms.K, Float> TEMP = new EnumMap<>(Kingdoms.K.class);
   private static final Map<Kingdoms.K, Boolean> DRY = new EnumMap<>(Kingdoms.K.class);

   private Seasons() {
   }

   public static void init() {
      SaveExt.register("seasons", () -> List.of((on ? 1 : 0) + "|" + season + "|" + time + "|" + year), a -> {
         try {
            if (a.length < 4) {
               return;
            }

            on = !a[0].equals("0");
            season = Math.floorMod(Integer.parseInt(a[1]), 4);
            time = Math.max(0, Integer.parseInt(a[2]));
            year = Math.max(1, Integer.parseInt(a[3]));
         } catch (Exception var2) {
         }
      }, () -> {
         on = true;
         reset();
      });
   }

   private static void reset() {
      season = 0;
      time = 0;
      year = 1;
      t = 0;
      TEMP.clear();
      DRY.clear();
   }

   public static int length() {
      return Math.max(200, Config.i("seasons.length", 6000));
   }

   public static int index() {
      return season;
   }

   public static int year() {
      return year;
   }

   public static String name() {
      return on ? NAMES[season] : "";
   }

   public static float progress() {
      return on ? Math.min(1.0F, (float)time / length()) : 0.0F;
   }

   public static String label() {
      return on ? NAMES[season] + ", год " + year : "";
   }

   public static boolean cold(Kingdoms.K k) {
      return k != null && TEMP.getOrDefault(k, 0.8F) < 0.2F;
   }

   public static boolean hot(Kingdoms.K k) {
      return k != null && (TEMP.getOrDefault(k, 0.8F) >= 1.5F || DRY.getOrDefault(k, false));
   }

   public static String climate(Kingdoms.K k) {
      return cold(k) ? "холодный" : (hot(k) ? "жаркий" : "умеренный");
   }

   public static boolean winter(Kingdoms.K k) {
      if (!on) {
         return false;
      } else if (season == 3) {
         return true;
      } else if (!cold(k)) {
         return false;
      } else {
         float p = progress();
         return season == 2 && p > 0.6F || season == 0 && p < 0.4F;
      }
   }

   public static double severity(Kingdoms.K k) {
      if (!on) {
         return 1.0;
      } else {
         double h = Math.max(1.0, Math.min(3.0, Config.d("seasons.harsh", 1.5)));
         if (cold(k) && winter(k)) {
            return h;
         } else {
            return hot(k) && season == 1 ? h : 1.0;
         }
      }
   }

   public static double food(Kingdoms.K k) {
      if (!on) {
         return 1.0;
      } else if (winter(k)) {
         return Math.max(0.1, Math.min(1.0, Config.d("seasons.winter.food", 0.5))) / severity(k);
      } else {
         return season == 2 ? Math.max(1.0, Math.min(2.0, Config.d("seasons.autumn.food", 1.25))) : 1.0;
      }
   }

   public static double growth() {
      if (!on) {
         return 1.0;
      } else {
         return season == 0 ? 1.5 : (season == 3 ? 0.4 : 1.0);
      }
   }

   public static int eat(Kingdoms.K k, int n) {
      return n > 0 && winter(k) ? (int)Math.ceil(n * (1.0 + 0.2 * severity(k))) : n;
   }

   public static int birthPeriod() {
      if (!on) {
         return 3;
      } else {
         return season == 0 ? 2 : (season == 3 ? 4 : 3);
      }
   }

   public static boolean frozen(Kingdoms.K k) {
      return on && cold(k) && winter(k);
   }

   public static int warBias() {
      return on && season == 3 ? Math.max(0, Math.min(40, Config.i("seasons.winter.war", 15))) : 0;
   }

   public static int tradePeriod() {
      return on && season == 2 ? 600 : 900;
   }

   public static double weight(Kingdoms.K k, int e) {
      if (!on) {
         return 1.0;
      } else {
         double sv = severity(k);

         return switch (season) {
            case 0 -> e == 0 ? 1.3 : 1.0;
            case 1 -> e == 1 ? 2.0 * sv : (e == 7 ? 2.0 : 1.0);
            case 2 -> e == 0 ? 2.5 : (e == 5 ? 1.5 : 1.0);
            default -> e == 0 ? 0.3 : (e == 1 ? 0.3 : (e == 4 ? 1.5 : (e == 7 ? 0.6 : 1.0)));
         };
      }
   }

   public static void tick(MinecraftServer s) {
      if (on && !s.getTickManager().isFrozen()) {
         t++;
         if (t % 200 == 1) {
            climate();
         }

         if (t % 40 == 0) {
            snow();
         }

         if (++time >= length()) {
            next(s);
         }
      }
   }

   private static void climate() {
      for (Kingdoms.K k : Kingdoms.K.values()) {
         if (k.alive && k.capital != null) {
            ServerWorld c = k.world;
            if (c instanceof ServerWorld) {
               ServerWorld w = c;
               BlockPos var8 = k.capital;
               if (c.isChunkLoaded(var8.getX() >> 4, var8.getZ() >> 4)) {
                  try {
                     Biome b = w.getBiome(var8).value();
                     TEMP.put(k, b.getTemperature());
                     DRY.put(k, !b.hasPrecipitation());
                  } catch (Exception var7) {
                  }
               }
               continue;
            }
         }

         TEMP.remove(k);
         DRY.remove(k);
      }
   }

   private static void snow() {
      for (Kingdoms.K k : Kingdoms.K.values()) {
         if (k.alive && k.capital != null) {
            ServerWorld c = k.world;
            if (c instanceof ServerWorld && winter(k) && !hot(k)) {
               BlockPos cx = k.capital;
               if (c.shouldTickEntity(cx) && !c.getPlayers().isEmpty()) {
                  Fx.particles(c, ParticleTypes.SNOWFLAKE, cx.getX() + 0.5, cx.getY() + 12, cx.getZ() + 0.5, cold(k) ? 60 : 30, 16.0, 4.0, 16.0, 0.01);
               }
            }
         }
      }
   }

   static void next(MinecraftServer s) {
      season = (season + 1) % 4;
      time = 0;
      if (season == 0) {
         year++;
         Chronicle.log("§eНаступил " + year + "-й год: зима позади, пришла весна");
      }

      if (s != null) {
         Text msg = Text.literal(ICONS[season] + " " + HINTS[season] + " (год " + year + ")").formatted(COLORS[season]);

         for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
            if (GodManager.isGod(p)) {
               p.sendMessage(msg, false);
            }
         }
      }
   }

   public static void power(ServerPlayerEntity god) {
      if (!on) {
         god.sendMessage(Text.literal("Закон «Времена года» выключен").formatted(Formatting.GRAY), true);
      } else {
         next(god.getServer());
         god.sendMessage(Text.literal(ICONS[season] + " " + NAMES[season] + ", год " + year).formatted(COLORS[season]), true);
      }
   }

   public static void clear() {
      reset();
   }
}
