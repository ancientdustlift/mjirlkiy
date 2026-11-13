package com.vladik.godbox;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;

public final class Life {
   public static final int YEAR = 600;
   static final int OLD = 50;
   static final String[] NAMES = new String[]{
      "Ратибор",
      "Мирослав",
      "Всеслав",
      "Добрыня",
      "Светозар",
      "Ярополк",
      "Велимир",
      "Любомир",
      "Радомир",
      "Остап",
      "Тихон",
      "Гордей",
      "Елисей",
      "Мирон",
      "Савва",
      "Фрол",
      "Ждан",
      "Будимир",
      "Злата",
      "Милана",
      "Василиса",
      "Забава",
      "Любава",
      "Ярослава",
      "Дарина",
      "Веселина",
      "Снежана",
      "Агния",
      "Богдана",
      "Варвара",
      "Есения",
      "Олеся",
      "Услада",
      "Белослава",
      "Влада",
      "Лада"
   };
   static final String[] NICK = new String[]{
      "Грозный", "Смелый", "Железный", "Быстрый", "Хмурый", "Лютый", "Стойкий", "Рыжий", "Седой", "Меткий", "Бородач", "Удалой"
   };
   public static final String[] TRAITS = new String[]{"Миролюбивый", "Воинственный", "Торговец", "Мудрый"};
   private static final Map<Kingdoms.K, UUID> KINGS = new EnumMap<>(Kingdoms.K.class);
   private static final Map<MobEntity, Integer> SHOWN = new WeakHashMap<>();
   private static int timer;

   private Life() {
   }

   static String tag(MobEntity m, String key) {
      for (String t : m.getCommandTags()) {
         if (t.startsWith(key)) {
            return t.substring(key.length());
         }
      }

      return null;
   }

   static void setTag(MobEntity m, String key, String val) {
      String old = tag(m, key);
      if (old != null) {
         m.removeCommandTag(key + old);
      }

      m.addCommandTag(key + val);
   }

   static void ensure(MobEntity m) {
      if (tag(m, "gbn:") == null) {
         Random r = m.getRandom();
         String n = Races.name(Kingdoms.of(m), r, NAMES);
         if (!(m instanceof VillagerEntity)) {
            n = n + " " + NICK[r.nextInt(NICK.length)];
         }

         setTag(m, "gbn:", n);
         int years = m.isBaby() ? 0 : 18 + r.nextInt(14);
         setTag(m, "gbb:", Long.toString(m.getWorld().getTime() - years * 600L));
         setTag(m, "gbt:", Integer.toString(r.nextInt(TRAITS.length)));
      }
   }

   public static String name(MobEntity m) {
      ensure(m);
      return tag(m, "gbn:");
   }

   public static int age(MobEntity m) {
      ensure(m);

      long b;
      try {
         b = Long.parseLong(tag(m, "gbb:"));
      } catch (Exception var4) {
         b = m.getWorld().getTime();
      }

      return (int)Math.max(0L, (m.getWorld().getTime() - b) / 600L);
   }

   public static int trait(MobEntity m) {
      ensure(m);

      try {
         return Integer.parseInt(tag(m, "gbt:"));
      } catch (Exception var2) {
         return 0;
      }
   }

   static String years(int a) {
      int d = a % 10;
      int h = a % 100;
      if (d == 1 && h != 11) {
         return "год";
      } else {
         return d < 2 || d > 4 || h >= 12 && h <= 14 ? "лет" : "года";
      }
   }

   public static void birth(MobEntity m) {
      if (m instanceof VillagerEntity v) {
         v.setBreedingAge(-3600);
         ensure(v);
         setTag(v, "gbb:", Long.toString(v.getWorld().getTime()));
         MobEntity par = Dynasty.born(Kingdoms.of(v), v);
         if (par != null) {
            Heredity.inherit(par, v);
         }

         Inspect.born(v, par);
         v.setCustomName(label(Kingdoms.of(v), v, ""));
      }
   }

   public static Text label(Kingdoms.K k, MobEntity m, String job) {
      String n = name(m);
      int a = age(m);
      boolean king = k != null && isKing(k, m);
      String rn = tag(m, "gbr:");
      if (king && rn != null) {
         n = n + " " + rn;
      }

      String s = (king ? "♔ " : "") + n + ", " + a;
      if (m.isBaby()) {
         s = s + " · ребёнок";
      } else if (king) {
         s = s + " · правитель";
      } else if (job != null && !job.isEmpty()) {
         s = s + " · " + job;
      }

      if (a - Races.ageBonus(k) >= 50 && !king) {
         s = s + " (стар.)";
      }

      SHOWN.put(m, a);
      return Text.literal(s).formatted(king ? Formatting.GOLD : (k == null ? Formatting.WHITE : k.color));
   }

   static boolean isKing(Kingdoms.K k, MobEntity m) {
      UUID u = KINGS.get(k);
      return u != null && u.equals(m.getUuid());
   }

   public static MobEntity king(Kingdoms.K k) {
      UUID u = KINGS.get(k);
      if (u == null) {
         return null;
      } else {
         for (MobEntity m : k.members) {
            if (m.isAlive() && m.getUuid().equals(u)) {
               return m;
            }
         }

         return null;
      }
   }

   public static int kingTrait(Kingdoms.K k) {
      MobEntity m = king(k);
      return m == null ? -1 : trait(m);
   }

   public static void tick(MinecraftServer server) {
      if (++timer % 100 == 0) {
         for (Kingdoms.K k : Kingdoms.K.values()) {
            if (k.alive && k.world != null && k.capital != null) {
               ServerWorld w = k.world;
               if (w.shouldTickEntity(k.capital)) {
                  Random r = w.getRandom();

                  for (MobEntity m : new ArrayList<>(k.members)) {
                     if (m.isAlive()) {
                        boolean fresh = tag(m, "gbn:") == null;
                        ensure(m);
                        int a = age(m);
                        Integer was = SHOWN.get(m);
                        if (m instanceof VillagerEntity v) {
                           if ((fresh || was == null || was != a) && (was == null || a <= was || !yearPassed(k, w, v, a, r))) {
                              v.setCustomName(label(k, v, Civ.jobTitle(v)));
                           }
                        } else if (fresh || was == null) {
                           m.setCustomName(Text.literal("⚔ " + name(m)).formatted(k.color));
                           SHOWN.put(m, a);
                        }
                     }
                  }

                  crown(k, w);
               }
            }
         }
      }
   }

   static boolean yearPassed(Kingdoms.K k, ServerWorld w, VillagerEntity v, int a, Random r) {
      a -= Races.ageBonus(k);
      if (a >= 50) {
         v.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 660, 0, false, false));
      }

      if (a >= 55 && r.nextInt(100) < (a - 52) * 5) {
         die(k, w, v, a);
         return true;
      } else {
         return false;
      }
   }

   static void die(Kingdoms.K k, ServerWorld w, VillagerEntity v, int a) {
      boolean king = isKing(k, v);
      Fx.particles(w, ParticleTypes.SOUL, v.getX(), v.getY() + 1.0, v.getZ(), 14, 0.3, 0.6, 0.3, 0.02);
      Fx.sound(w, v.getX(), v.getY(), v.getZ(), SoundEvents.ENTITY_VILLAGER_DEATH, 0.8F, 0.7F);
      Civ.tell(
         w,
         Text.literal("⚰ " + (king ? "Правитель " : "") + name(v) + " (" + k.title + ") умирает от старости в " + a + " " + years(a))
            .formatted(Formatting.GRAY)
      );
      if (king) {
         KINGS.remove(k);
      }

      Inspect.died(v, "умер от старости");
      k.members.remove(v);
      v.discard();
   }

   static void crown(Kingdoms.K k, ServerWorld w) {
      MobEntity kk = Kings.king(k);
      MobEntity cur = king(k);
      if (kk != null && cur != kk) {
         if (cur != null) {
            cur.removeCommandTag("gbking");
         }

         KINGS.put(k, kk.getUuid());
         kk.addCommandTag("gbking");
         if (!Models.equipHat(kk, "crown")) {
            kk.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.LIGHT_WEIGHTED_PRESSURE_PLATE));
            kk.setEquipmentDropChance(EquipmentSlot.HEAD, 0.0F);
         }

         kk.setCustomName(label(k, kk, ""));
         Fx.particles(w, ParticleTypes.TOTEM_OF_UNDYING, kk.getX(), kk.getY() + 1.5, kk.getZ(), 30, 0.4, 0.6, 0.4, 0.2);
         Fx.sound(w, kk.getX(), kk.getY(), kk.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.6F, 1.2F);
         Civ.tell(
            w,
            Text.literal(
                  "♔ "
                     + k.title
                     + ": правит "
                     + name(kk)
                     + ", "
                     + age(kk)
                     + " "
                     + years(age(kk))
                     + " ("
                     + TRAITS[trait(kk) % TRAITS.length]
                     + ") — "
                     + Dynasty.describe(k)
               )
               .formatted(k.color, Formatting.BOLD)
         );
      }
   }

   public static String describeKing(Kingdoms.K k) {
      MobEntity m = king(k);
      if (m == null) {
         return "правителя нет";
      } else {
         String rn = tag(m, "gbr:");
         return "правитель " + name(m) + (rn != null ? " " + rn : "") + ", " + age(m) + " (" + TRAITS[trait(m) % TRAITS.length] + ") · " + Dynasty.describe(k);
      }
   }

   public static void clear() {
      KINGS.clear();
      SHOWN.clear();
      timer = 0;
   }
}
