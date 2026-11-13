package com.vladik.godbox;

import com.vladik.godbox.net.Payloads;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public final class Borders {
   private static int t;
   private static final String[] PRE = new String[]{
      "Бел", "Вер", "Гор", "Дуб", "Зар", "Кам", "Лес", "Мир", "Нов", "Озер", "Рад", "Свет", "Тур", "Яр", "Вол", "Ель", "Сокол", "Злат", "Крас", "Ставр"
   };
   private static final String[] MID = new String[]{"", "о", "а", "и", "е"};
   private static final String[] SUF = new String[]{"град", "бург", "поль", "дол", "мир", "ск", "ица", "ов", "гор", "холм", "брод", "стан"};

   private Borders() {
   }

   static int radius(Civ.City c, boolean cap) {
      return (cap ? 44 : 30) + Math.min(10, Math.max(0, c.era)) * 2;
   }

   public static void tick(MinecraftServer s) {
      if (++t % 40 == 0) {
         String d = null;

         for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
            if (GodManager.isGod(p) && ServerPlayNetworking.canSend(p, Payloads.Borders.ID)) {
               if (d == null) {
                  d = data();
               }

               ServerPlayNetworking.send(p, new Payloads.Borders(d));
            }
         }
      }
   }

   static String cityName(Kingdoms.K k, BlockPos p) {
      long h = k.ordinal() * 73856093L ^ (p.getX() >> 3) * 19349663L ^ (p.getZ() >> 3) * 83492791L;
      h ^= h >>> 17;
      h *= -7046029254386353131L;
      int a = (int)(h >>> 8 & 2147483647L);
      int c = (int)(h >>> 36 & 134217727L);
      String pre = PRE[a % PRE.length];
      String suf = SUF[c % SUF.length];
      String mid = MID[a / PRE.length % MID.length];
      if (pre.matches(".*[аеиоуыэюяь]$")) {
         mid = "";
      }

      return pre + mid + suf;
   }

   static String clean(String s) {
      return s == null ? "" : s.replace('|', '/').replace('\n', ' ');
   }

   static String data() {
      StringBuilder b = new StringBuilder();
      List<Kingdoms.K> alive = new ArrayList<>();

      for (Kingdoms.K k : Kingdoms.K.values()) {
         if (k.alive && k.capital != null) {
            alive.add(k);
         }
      }

      for (int i = 0; i < alive.size(); i++) {
         for (int j = i + 1; j < alive.size(); j++) {
            Kingdoms.K a = alive.get(i);
            Kingdoms.K c = alive.get(j);
            int r = Diplomacy.rel(a, c);
            if (r == 1 || r == 2) {
               b.append(r == 1 ? "W|" : "A|")
                  .append(a.capital.getX())
                  .append('|')
                  .append(a.capital.getZ())
                  .append('|')
                  .append(c.capital.getX())
                  .append('|')
                  .append(c.capital.getZ())
                  .append('\n');
            }
         }
      }

      for (Kingdoms.K kx : Kingdoms.K.values()) {
         if (kx.alive && kx.capital != null) {
            Civ.City c = Civ.city(kx);
            int rc = radius(c, true);
            int rt = radius(c, false);
            b.append("T|")
               .append(kx.capital.getX())
               .append('|')
               .append(kx.capital.getZ())
               .append('|')
               .append(rc)
               .append('|')
               .append(kx.rgb)
               .append('|')
               .append(clean(cityName(kx, kx.capital)))
               .append("|1|")
               .append(clean(kx.title))
               .append('|')
               .append(Math.max(0, c.era))
               .append('\n');

            for (BlockPos p : Conquest.towns(kx)) {
               b.append("T|")
                  .append(p.getX())
                  .append('|')
                  .append(p.getZ())
                  .append('|')
                  .append(rt)
                  .append('|')
                  .append(kx.rgb)
                  .append('|')
                  .append(clean(cityName(kx, p)))
                  .append("|0|")
                  .append(clean(kx.title))
                  .append('|')
                  .append(Math.max(0, c.era))
                  .append('\n');
            }

            int pop = 0;

            for (MobEntity m : kx.members) {
               if (m.isAlive()) {
                  pop++;
               }
            }

            Kingdoms.K lord = Vassals.lord(kx);
            int era = Math.max(0, Math.min(c.era, Eras.NAMES.length - 1));
            String leg = kx.title
               + ": "
               + Eras.NAMES[era]
               + ", жителей "
               + pop
               + ", городов "
               + (1 + Conquest.towns(kx).size())
               + ", золото "
               + Economy.gold(kx)
               + ", вера "
               + Prayers.faith(kx)
               + "%"
               + (lord != null ? ", вассал: " + lord.title : (Vassals.count(kx) >= 2 ? ", ИМПЕРИЯ" : ""))
               + (Diplomacy.atWar(kx) ? ", ⚔ война" : "");
            b.append("L|").append(kx.rgb).append('|').append(leg.replace('|', '/')).append('\n');
         }
      }

      return b.toString();
   }
}
