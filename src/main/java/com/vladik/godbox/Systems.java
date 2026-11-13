package com.vladik.godbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;

public final class Systems {
   private static final List<Systems.Sys> TICK = new ArrayList<>();
   private static final List<Runnable> CLEAR = new ArrayList<>();
   private static final long[] NANOS = new long[128];
   private static boolean frozen;

   private Systems() {
   }

   public static void tick(String name, Consumer<MinecraftServer> tick) {
      tick(name, tick, () -> true);
   }

   public static void tick(String name, Consumer<MinecraftServer> tick, BooleanSupplier enabled) {
      if (frozen) {
         throw new IllegalStateException("Systems already frozen: " + name);
      } else {
         TICK.add(new Systems.Sys(name, tick, enabled));
      }
   }

   public static void clear(Runnable... rs) {
      Collections.addAll(CLEAR, rs);
   }

   public static void freeze() {
      frozen = true;
   }

   public static List<Systems.Sys> all() {
      return Collections.unmodifiableList(TICK);
   }

   public static long micros(int i) {
      return i < NANOS.length ? NANOS[i] / 1000L : 0L;
   }

   public static void tickAll(MinecraftServer s) {
      for (int i = 0; i < TICK.size(); i++) {
         Systems.Sys y = TICK.get(i);
         if (y.enabled().getAsBoolean()) {
            long t0 = System.nanoTime();

            try {
               y.tick().accept(s);
            } catch (Exception var6) {
               GodBox.LOG.error("GodBox system '{}' failed", y.name(), var6);
            }

            if (i < NANOS.length) {
               NANOS[i] = (NANOS[i] * 31L + (System.nanoTime() - t0)) / 32L;
            }
         }
      }
   }

   public static void clearAll() {
      for (Runnable r : CLEAR) {
         try {
            r.run();
         } catch (Exception var3) {
            GodBox.LOG.error("GodBox clear failed", (Throwable)var3);
         }
      }
   }

   static void registerDefaults() {
      tick("perf", Perf::tick);
      tick("tasks", s -> Tasks.tick());
      tick("undo", s -> Undo.tick());
      tick("kingdoms", Kingdoms::tick);
      tick("bosses", s -> Bosses.tick());
      tick("bossModels", BossModels::tick);
      tick("civ", Civ::tick, () -> Perf.go);
      tick("life", Life::tick, () -> Perf.go);
      tick("diplomacy", Diplomacy::tick);
      tick("chronicle", Chronicle::tick);
      tick("ages", Ages::tick);
      tick("faith", Faith::tick);
      tick("events", Events::tick);
      tick("conquest", Conquest::tick);
      tick("save", Save::tick);
      tick("creatures", Creatures::tick);
      tick("worldAges", WorldAges::tick);
      tick("kings", Kings::tick);
      tick("ships", Ships::tick);
      tick("colonies", Colonies::tick);
      tick("tech", Tech::tick, () -> Tech.on);
      tick("wonders", Wonders::tick, () -> Wonders.on);
      tick("magic", Magic::tick);
      tick("guns", Guns::tick);
      tick("siegeEngines", SiegeEngines::tick);
      tick("navy", Navy::tick);
      tick("roads", Roads::tick);
      tick("siege", Siege::tick);
      tick("economy", Economy::tick);
      tick("vassals", Vassals::tick);
      tick("unrest", Unrest::tick);
      tick("heroes", Heroes::tick);
      tick("highways", Highways::tick);
      tick("storage", Storage::tick);
      tick("decor", Decor::tick);
      tick("prayers", Prayers::tick);
      tick("possess", Possess::tick);
      tick("borders", Borders::tick);
      tick("hud", Hud::tick);
      tick("achievements", Achievements::tick);
      tick("ecosystem", Ecosystem::tick);
      tick("herds", Herds::tick);
      tick("tectonics", Tectonics::tick);
      tick("banners", Banners::tick);
      tick("hand", Hand::tick);
      tick("repair", Repair::tick);
      tick("seasons", Seasons::tick);
      clear(Seasons::clear);
      tick("history", History::tick);
      clear(History::clear);
      tick("inspect", Inspect::tick);
      clear(Inspect::clear);
      tick("plague", Plague::tick);
      clear(Plague::clear);
      tick("treaties", Treaties::tick);
      clear(Treaties::clear);
      Treaties.init();
      tick("mortal", Mortal::tick);
      clear(Mortal::clear);
      SiegeEngines.init();
      KingdomNames.init();
      GodManager.init();
      tick("godCheck", GodManager::tick);
      tick("victory", Victory::tick);
      Victory.init();
      clear(Victory::clear);
      clear(
         Creatures::clear,
         Kings::clear,
         WorldAges::clear,
         Ships::clear,
         Colonies::clear,
         Tech::clear,
         Wonders::clear,
         Magic::clear,
         Guns::clear,
         SiegeEngines::clear,
         Navy::clear,
         Roads::clear,
         Siege::clear,
         Economy::clear,
         Vassals::clear,
         Unrest::clear,
         Heroes::clear,
         Highways::clear,
         Storage::clear,
         Decor::clear,
         Prayers::clear,
         Possess::clear,
         Achievements::clear,
         Ecosystem::clear,
         Herds::clear,
         Tectonics::clear,
         Banners::clear,
         Hand::clear,
         Dynasty::clear,
         Repair::clear,
         Perf::clear,
         Tasks::clear,
         Life::clear,
         Diplomacy::clear,
         Ores::clear,
         Conquest::clear,
         Ages::clear,
         Faith::clear,
         Events::clear,
         Chronicle::clear,
         Sleep::clear,
         GodManager::clear,
         Undo::clear,
         Kingdoms::clear,
         Bosses::clear,
         BossModels::clear
      );
   }

   public record Sys(String name, Consumer<MinecraftServer> tick, BooleanSupplier enabled) {
   }
}
