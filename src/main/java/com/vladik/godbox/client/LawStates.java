package com.vladik.godbox.client;

import com.vladik.godbox.Category;
import com.vladik.godbox.Power;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

final class LawStates {
   private static final Map<Power, Boolean> STATE = new EnumMap<>(Power.class);
   private static boolean registered;

   private LawStates() {
   }

   static void init() {
      if (!registered) {
         registered = true;
         ClientReceiveMessageEvents.GAME.register(LawStates::onMessage);
         ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> STATE.clear());
      }
   }

   static Boolean get(Power p) {
      return p == null ? null : STATE.get(p);
   }

   static void sync(long mask) {
      int i = 0;

      for (Power p : Power.values()) {
         if (p.category == Category.LAWS) {
            if (i < 64) {
               STATE.put(p, (mask >> i & 1L) != 0L);
            }

            i++;
         }
      }
   }

   private static void onMessage(Text message, boolean overlay) {
      try {
         String s = message.getString();
         parse(s);
         if (overlay && MinecraftClient.getInstance().currentScreen instanceof GodScreen gs) {
            TextColor c = message.getStyle().getColor();
            gs.note(s, c == null ? -1 : 0xFF000000 | c.getRgb());
         }
      } catch (Exception var5) {
      }
   }

   private static void parse(String s) {
      String head = "Закон «";
      if (s.startsWith(head)) {
         int e = s.indexOf("»: ");
         if (e >= 0) {
            String title = s.substring(head.length(), e);
            String rest = s.substring(e + 3).toLowerCase(Locale.ROOT);
            Boolean on = rest.startsWith("вкл") ? Boolean.TRUE : (rest.startsWith("выкл") ? Boolean.FALSE : null);
            if (on != null) {
               for (Power p : Power.values()) {
                  if (p.category == Category.LAWS && p.title.equals(title)) {
                     STATE.put(p, on);
                  }
               }
            }
         }
      }
   }
}
