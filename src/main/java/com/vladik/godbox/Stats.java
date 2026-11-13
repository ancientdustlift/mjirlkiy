package com.vladik.godbox;

import com.vladik.godbox.net.Payloads;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public final class Stats {
   private Stats() {
   }

   static String c(Kingdoms.K k) {
      return "§" + k.color.getCode();
   }

   static String block(Kingdoms.K k) {
      Civ.City c = Civ.city(k);
      int vill = 0;
      int kids = 0;
      int war = 0;

      for (MobEntity m : k.members) {
         if (m.isAlive()) {
            if (m instanceof VillagerEntity v) {
               vill++;
               if (v.isBaby()) {
                  kids++;
               }
            } else {
               war++;
            }
         }
      }

      int[] j = Civ.counts(k);
      int built = 0;

      for (Civ.Building b : c.buildings) {
         if (b.done && b.kind != Civ.Kind.QUARRY) {
            built++;
         }
      }

      StringBuilder s = new StringBuilder();
      s.append(c(k)).append("§l").append(k.title).append('\n');
      s.append("§7Раса: §f").append(Races.describe(k)).append('\n');
      s.append("§7Эпоха: §f").append(Ages.name(c)).append(c.castle ? " §6(замок)" : "").append('\n');
      s.append("§7").append(Life.describeKing(k)).append('\n');
      s.append("§7Наука: §b").append(Tech.info(k)).append('\n');
      s.append("§7Жителей: §f").append(vill).append("§7 (детей ").append(kids).append("), воинов: §f").append(war).append('\n');
      s.append("§7Лесорубы ")
         .append(j[0])
         .append(", шахтёры ")
         .append(j[1])
         .append(", фермеры ")
         .append(j[2])
         .append(", строители ")
         .append(j[3])
         .append(", рыбаки ")
         .append(j[4])
         .append('\n');
      s.append("§7Склад: §6дерево ")
         .append(c.wood)
         .append(" §7камень ")
         .append(c.stone)
         .append(" §aеда ")
         .append(c.food)
         .append(" §bруда ")
         .append(c.ore)
         .append('\n');
      s.append("§7Построек: §f")
         .append(built)
         .append("§7 (домов ")
         .append(c.count(Civ.Kind.HOUSE) + c.count(Civ.Kind.MANOR))
         .append(", ферм ")
         .append(c.count(Civ.Kind.FARM))
         .append(", храмов ")
         .append(c.count(Civ.Kind.TEMPLE))
         .append(")\n");
      s.append("§7Вера: §d").append(Faith.religion(c)).append(c.religion >= 0 ? " §7(" + c.faith + ")" : "").append('\n');
      s.append("§7Культура: §e").append(Faith.culture(c)).append('\n');
      s.append("§7Казна: §6").append(Economy.info(k)).append("§7, §f").append(Vassals.info(k)).append('\n');
      s.append("§7Смута: §c")
         .append(Unrest.unrest(k))
         .append("%§7, героев: §e")
         .append(Heroes.count(k))
         .append("§7, трактов: §f")
         .append(Highways.count(k))
         .append('\n');
      s.append("§7Вера: §b").append(Prayers.info(k)).append("§7, стадо: §a").append(Herds.count(k)).append('\n');
      s.append(Events.statusLine(k));
      s.append("§7Здоровье: ").append(Plague.line(k)).append('\n');
      s.append(Treaties.stats(k));
      StringBuilder rel = new StringBuilder();

      for (Kingdoms.K o : Kingdoms.K.values()) {
         if (o != k && o.alive) {
            int r = Diplomacy.rel(k, o);
            String st = r == 1 ? "§cвойна" : (r == 2 ? "§aсоюз" : "§7мир");
            if (rel.length() > 0) {
               rel.append("§7, ");
            }

            rel.append(c(o)).append(o.title.split(" ")[0]).append(' ').append(st);
         }
      }

      s.append("§7Отношения: ").append(rel.length() == 0 ? "§8соседей нет" : rel.toString());
      return s.toString();
   }

   public static String text() {
      StringBuilder s = new StringBuilder();
      boolean any = false;

      for (Kingdoms.K k : Kingdoms.K.values()) {
         if (k.alive && k.world != null) {
            if (any) {
               s.append('\u0001');
            }

            s.append(block(k));
            any = true;
         }
      }

      if (!any) {
         s.append("§7Королевств пока нет. Создай их во вкладке «Королевства».");
      }

      s.append('\u0002');
      s.append("§e⚑ ").append(Victory.status()).append('\n');
      List<String> l = Chronicle.last(12);
      if (l.isEmpty()) {
         s.append("§8Летопись пока пуста");
      }

      for (int i = l.size() - 1; i >= 0; i--) {
         s.append(l.get(i));
         if (i > 0) {
            s.append('\n');
         }
      }

      String out = s.toString();
      return out.length() > 30000 ? out.substring(0, 30000) : out;
   }

   public static void send(ServerPlayerEntity god, boolean open) {
      if (ServerPlayNetworking.canSend(god, Payloads.Stats.ID)) {
         ServerPlayNetworking.send(god, new Payloads.Stats(open, text()));
      }
   }
}
