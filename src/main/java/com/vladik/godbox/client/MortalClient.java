package com.vladik.godbox.client;

import com.vladik.godbox.Mortal;
import com.vladik.godbox.Power;
import com.vladik.godbox.net.MortalPayloads;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class MortalClient {
   public static boolean on;
   public static int pool;
   public static int cap = 1000;
   public static int state;
   public static int scale = 100;
   public static int perMin;
   public static String title = "";
   public static String progress = "";
   private static long updated;

   private MortalClient() {
   }

   public static void set(MortalPayloads.MortalHud h) {
      on = h.on();
      pool = h.pool();
      cap = Math.max(1, h.cap());
      title = h.title();
      progress = h.progress();
      state = h.state();
      scale = h.scale();
      perMin = h.perMin();
      updated = System.currentTimeMillis();
   }

   public static boolean active() {
      return on && System.currentTimeMillis() - updated < 5000L;
   }

   public static int costOf(Power p) {
      return (int)Math.ceil(Mortal.cost(p) * scale / 100.0);
   }

   public static boolean affordable(Power p) {
      return !active() || costOf(p) <= pool;
   }

   public static List<Text> tooltip(Power p, List<Text> lines) {
      if (!active()) {
         return lines;
      } else {
         List<Text> out = new ArrayList<>(lines);
         int c = costOf(p);
         Text line;
         if (c <= 0) {
            line = Text.literal("✦ Бесплатно").formatted(Formatting.GREEN);
         } else {
            String s = "✦ Цена: " + c + " божьей силы";
            if (p.continuous) {
               s = s + " (+" + Math.max(1, (int)Math.ceil(c * 0.25)) + " за повтор)";
            }

            line = c <= pool ? Text.literal(s).formatted(Formatting.GOLD) : Text.literal(s + " — не хватает " + (c - pool)).formatted(Formatting.RED);
         }

         out.add(Math.min(2, out.size()), line);
         return out;
      }
   }
}
