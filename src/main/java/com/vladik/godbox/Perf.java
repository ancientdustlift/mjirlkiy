package com.vladik.godbox;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class Perf {
   public static boolean on = true;
   static boolean slow;
   public static boolean go = true;
   private static long last;
   private static double avg = 50.0;
   private static int t;

   private Perf() {
   }

   public static void tick(MinecraftServer s) {
      long now = System.nanoTime();
      if (last != 0L) {
         double ms = (now - last) / 1000000.0;
         if (ms < 2000.0) {
            avg = avg * 0.95 + ms * 0.05;
         }
      }

      last = now;
      t++;
      boolean was = slow;
      slow = on && (slow ? avg > 54.0 : avg > 62.0);
      go = !slow || (t & 1) == 0;
      if (slow != was && t > 400) {
         Text m = Text.literal(slow ? "⚙ Мир тормозит (" + tps() + " TPS) — GodBox включил экономный режим" : "⚙ Экономный режим выключен")
            .formatted(Formatting.GRAY);

         for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
            if (GodManager.isGod(p)) {
               p.sendMessage(m, true);
            }
         }
      }

      if (on && t % 600 == 0) {
         cleanup(s);
      }
   }

   static String tps() {
      return String.valueOf(Math.round(Math.min(20.0, 1000.0 / Math.max(1.0, avg))));
   }

   static void cleanup(MinecraftServer s) {
      for (ServerWorld w : s.getWorlds()) {
         List<ItemEntity> items = new ArrayList<>();
         List<ExperienceOrbEntity> orbs = new ArrayList<>();

         for (Entity e : w.iterateEntities()) {
            if (e instanceof ItemEntity it && it.getItemAge() > 1200) {
               items.add(it);
            } else if (e instanceof ExperienceOrbEntity o) {
               orbs.add(o);
            }
         }

         int limit = slow ? 60 : 200;
         if (items.size() > limit) {
            for (int i = limit / 2; i < items.size(); i++) {
               items.get(i).discard();
            }
         }

         if (orbs.size() > limit) {
            for (int i = limit / 2; i < orbs.size(); i++) {
               orbs.get(i).discard();
            }
         }
      }
   }

   public static void clear() {
      last = 0L;
      avg = 50.0;
      slow = false;
      go = true;
      t = 0;
   }
}
