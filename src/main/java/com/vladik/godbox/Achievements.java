package com.vladik.godbox;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class Achievements {
   static final EnumSet<Achievements.A> GOT = EnumSet.noneOf(Achievements.A.class);
   static final Map<Category, Integer> USES = new EnumMap<>(Category.class);
   static int total;
   static boolean possessed;
   private static int t;

   private Achievements() {
   }

   public static void used(Power p) {
      if (p != null && p != Power.CAMERA_TP && p != Power.CIV_STATS) {
         total++;
         if (p.category != null) {
            USES.merge(p.category, 1, Integer::sum);
         }
      }
   }

   static int uses(Category c) {
      return USES.getOrDefault(c, 0);
   }

   public static void tick(MinecraftServer s) {
      if (++t % 100 == 0) {
         int alive = 0;
         int heroes = 0;
         int roads = 0;
         boolean emp = false;
         boolean mag = false;
         boolean steam = false;
         boolean gold = false;
         boolean holy = false;

         for (Kingdoms.K k : Kingdoms.K.values()) {
            if (k.alive) {
               alive++;
               heroes += Heroes.count(k);
               roads += Highways.count(k);
               if (Vassals.count(k) >= 2) {
                  emp = true;
               }

               int e = Civ.city(k).era;
               if (e >= 9) {
                  mag = true;
               }

               if (e >= 10) {
                  steam = true;
               }

               if (Economy.gold(k) >= 500) {
                  gold = true;
               }

               if (Prayers.faith(k) >= 100) {
                  holy = true;
               }
            }
         }

         check(s, Achievements.A.FIRST, total > 0);
         check(s, Achievements.A.BOMBER, uses(Category.BOMBS) >= 100);
         check(s, Achievements.A.WRATH, uses(Category.DISASTERS) >= 50);
         check(s, Achievements.A.CREATOR, uses(Category.LIFE) >= 100);
         check(s, Achievements.A.FOUR, alive >= 4);
         check(s, Achievements.A.EMPIRE, emp);
         check(s, Achievements.A.MAGIC, mag);
         check(s, Achievements.A.STEAM, steam);
         check(s, Achievements.A.HEROES, heroes >= 5);
         check(s, Achievements.A.ROADS, roads >= 5);
         check(s, Achievements.A.GOLD, gold);
         check(s, Achievements.A.PROPHET, Prayers.answered >= 5);
         check(s, Achievements.A.HOLY, holy);
         check(s, Achievements.A.AVATAR, possessed);
         check(s, Achievements.A.LAWGIVER, uses(Category.LAWS) >= 10);
         check(s, Achievements.A.SHEPHERD, Herds.tamed >= 20);
         check(s, Achievements.A.CIRCLE, Ecosystem.hunts >= 25);
         check(s, Achievements.A.FIREMOUNT, Tectonics.erupted);
         check(s, Achievements.A.APOCALYPSE, total >= 1000);
         check(s, Achievements.A.MORTAL, Mortal.wins() >= 1);
         check(s, Achievements.A.TRIALS, Mortal.wins() >= Mortal.S.values().length - 1);
      }
   }

   static void check(MinecraftServer s, Achievements.A a, boolean cond) {
      if (cond && GOT.add(a)) {
         MutableText msg = Text.literal("\ud83c\udfc6 Достижение бога: ")
            .formatted(Formatting.GOLD)
            .append(Text.literal(a.title).formatted(Formatting.YELLOW, Formatting.BOLD))
            .append(Text.literal(" — " + a.desc + " (" + GOT.size() + "/" + Achievements.A.values().length + ")").formatted(Formatting.GRAY));

         for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
            p.sendMessage(msg, false);
            Fx.sound(p.getServerWorld(), p.getX(), p.getY(), p.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
         }

         Chronicle.log("Бог получил достижение «" + a.title + "»");
      }
   }

   public static void show(ServerPlayerEntity god) {
      god.sendMessage(
         Text.literal("\ud83c\udfc6 Достижения бога: " + GOT.size() + "/" + Achievements.A.values().length + "   (сил применено: " + total + ")")
            .formatted(Formatting.GOLD, Formatting.BOLD),
         false
      );

      for (Achievements.A a : Achievements.A.values()) {
         boolean got = GOT.contains(a);
         god.sendMessage(
            Text.literal((got ? " ✔ " : " ✘ ") + a.title)
               .formatted(got ? Formatting.GREEN : Formatting.DARK_GRAY)
               .append(Text.literal(" — " + a.desc).formatted(got ? Formatting.GRAY : Formatting.DARK_GRAY)),
            false
         );
      }
   }

   public static void clear() {
      GOT.clear();
      USES.clear();
      total = 0;
      possessed = false;
      t = 0;
   }

   static enum A {
      FIRST("Первое чудо", "Применить любую силу"),
      BOMBER("Подрывник", "Взорвать бомбы 100 раз"),
      WRATH("Гнев небес", "Вызвать 50 стихийных бедствий"),
      CREATOR("Творец жизни", "Создать существ 100 раз"),
      FOUR("Четыре короны", "4 королевства живут одновременно"),
      EMPIRE("Император", "Королевство стало империей"),
      MAGIC("Век разума", "Город дошёл до эпохи Просвещения"),
      STEAM("Пар и сталь", "Город вступил в эпоху пара"),
      HEROES("Зал славы", "5 героев живут одновременно"),
      ROADS("Все дороги ведут…", "Построено 5 великих трактов"),
      GOLD("Золотой век", "В казне королевства 500 золота"),
      PROPHET("Пророк", "Исполнить 5 молитв"),
      HOLY("Святая земля", "Вера королевства достигла 100%"),
      AVATAR("Аватар", "Вселиться в существо"),
      LAWGIVER("Законодатель", "Переключить законы 10 раз"),
      SHEPHERD("Пастырь", "Королевства приручили 20 животных"),
      CIRCLE("Круг жизни", "Хищники поймали добычу 25 раз"),
      FIREMOUNT("Огненная гора", "Увидеть извержение живого вулкана"),
      APOCALYPSE("Апокалипсис", "Применить силы 1000 раз"),
      MORTAL("Смертный, но бог", "Пройти испытание смертного бога"),
      TRIALS("Все испытания", "Пройти все пять испытаний смертного бога");

      final String title;
      final String desc;

      private A(String title, String desc) {
         this.title = title;
         this.desc = desc;
      }
   }
}
