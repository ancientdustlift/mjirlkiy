package com.vladik.godbox;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.VillagerType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

public final class Races {
   private static final Map<Kingdoms.K, Races.Race> RACE = new EnumMap<>(Kingdoms.K.class);
   private static final Identifier MOD = Identifier.of("godbox", "race");
   private static final Map<Races.Race, Map<Block, Block>> STYLE = new EnumMap<>(Races.Race.class);
   static final String[] ELF_NAMES = new String[]{
      "Аэлин",
      "Таурион",
      "Лориэн",
      "Элендил",
      "Галадор",
      "Миривэн",
      "Финрод",
      "Келеборн",
      "Амариэль",
      "Иторил",
      "Сильвана",
      "Нимлот",
      "Эларион",
      "Лютиэн",
      "Аранэль",
      "Тинувиэль",
      "Халдир",
      "Фаэлар",
      "Исильмэ",
      "Риннон"
   };
   static final String[] ORC_NAMES = new String[]{
      "Грым",
      "Углук",
      "Шаграт",
      "Борзаг",
      "Гхаш",
      "Дурбаг",
      "Мургаш",
      "Зогар",
      "Кражд",
      "Ургал",
      "Грыжда",
      "Шарга",
      "Болдог",
      "Назгра",
      "Ражак",
      "Гортук",
      "Харгаш",
      "Лугдуш",
      "Брогда",
      "Тракк"
   };
   static final String[] DWARF_NAMES = new String[]{
      "Торин",
      "Балин",
      "Двалин",
      "Гимли",
      "Дурин",
      "Глоин",
      "Бофур",
      "Нарви",
      "Фундин",
      "Траин",
      "Борин",
      "Хильда",
      "Дагна",
      "Брунгильда",
      "Фрерин",
      "Оин",
      "Гроин",
      "Торбьорг",
      "Кили",
      "Сигрун"
   };

   private Races() {
   }

   public static void init() {
   }

   private static List<String> save() {
      List<String> o = new ArrayList<>();

      for (Entry<Kingdoms.K, Races.Race> e : RACE.entrySet()) {
         o.add(e.getKey().name() + "|" + e.getValue().name());
      }

      return o;
   }

   private static void load(String[] a) {
      try {
         RACE.put(Kingdoms.K.valueOf(a[0]), Races.Race.valueOf(a[1]));
      } catch (Exception var2) {
      }
   }

   public static Races.Race of(Kingdoms.K k) {
      return k == null ? Races.Race.HUMAN : RACE.getOrDefault(k, Races.Race.HUMAN);
   }

   static void set(Kingdoms.K k, Races.Race r) {
      if (k != null && r != null) {
         if (r == Races.Race.HUMAN) {
            RACE.remove(k);
         } else {
            RACE.put(k, r);
         }
      }
   }

   static void forget(Kingdoms.K k) {
      RACE.remove(k);
   }

   public static void clear() {
      RACE.clear();
   }

   public static String describe(Kingdoms.K k) {
      Races.Race r = of(k);
      return r.title + " — " + r.desc;
   }

   static Races.Race pick(ServerWorld w, BlockPos pos) {
      int bonus = Math.max(0, Config.i("races.biomeWeight", 6));
      int[] wt = new int[]{3, 2, 2, 2};

      try {
         RegistryEntry<Biome> b = w.getBiome(pos);
         if (b.isIn(BiomeTags.IS_FOREST) || b.isIn(BiomeTags.IS_JUNGLE) || b.isIn(BiomeTags.IS_TAIGA)) {
            wt[1] += bonus;
         }

         if (b.isIn(BiomeTags.IS_BADLANDS)
            || b.isIn(BiomeTags.IS_SAVANNA)
            || b.matchesKey(BiomeKeys.SWAMP)
            || b.matchesKey(BiomeKeys.MANGROVE_SWAMP)
            || b.matchesKey(BiomeKeys.DESERT)) {
            wt[2] += bonus;
         }

         if (b.isIn(BiomeTags.IS_MOUNTAIN)
            || b.isIn(BiomeTags.IS_HILL)
            || b.matchesKey(BiomeKeys.DRIPSTONE_CAVES)
            || b.matchesKey(BiomeKeys.LUSH_CAVES)
            || b.matchesKey(BiomeKeys.DEEP_DARK)) {
            wt[3] += bonus;
         }
      } catch (Exception var6) {
      }

      int roll = w.getRandom().nextInt(wt[0] + wt[1] + wt[2] + wt[3]);

      for (int i = 0; i < wt.length; i++) {
         roll -= wt[i];
         if (roll < 0) {
            return Races.Race.values()[i];
         }
      }

      return Races.Race.HUMAN;
   }

   private static void mod(MobEntity m, RegistryEntry<EntityAttribute> attr, double v, Operation op) {
      EntityAttributeInstance a = m.getAttributeInstance(attr);
      if (a != null) {
         a.removeModifier(MOD);
         if (v != 0.0) {
            a.addPersistentModifier(new EntityAttributeModifier(MOD, v, op));
         }
      }
   }

   static void apply(Kingdoms.K k, MobEntity m) {
      if (k != null && m != null && (m instanceof VillagerEntity || m instanceof Monster)) {
         try {
            Races.Race r = of(k);
            double scale = 1.0;
            double hp = 0.0;
            double dmg = 0.0;
            switch (r) {
               case ELF:
                  scale = Config.d("races.elf.scale", 1.05);
                  break;
               case ORC:
                  scale = Config.d("races.orc.scale", 1.15);
                  hp = Config.d("races.orc.health", 0.1);
                  dmg = Config.d("races.orc.damage", 0.25);
                  break;
               case DWARF:
                  scale = Config.d("races.dwarf.scale", 0.8);
                  hp = Config.d("races.dwarf.health", 0.3);
            }

            boolean full = m.getHealth() >= m.getMaxHealth();
            mod(m, EntityAttributes.GENERIC_SCALE, scale - 1.0, Operation.ADD_MULTIPLIED_TOTAL);
            mod(m, EntityAttributes.GENERIC_MAX_HEALTH, hp, Operation.ADD_MULTIPLIED_BASE);
            mod(m, EntityAttributes.GENERIC_ATTACK_DAMAGE, dmg, Operation.ADD_MULTIPLIED_BASE);
            if (full || m.getHealth() > m.getMaxHealth()) {
               m.setHealth(m.getMaxHealth());
            }

            if (m instanceof VillagerEntity v) {
               VillagerType t = switch (r) {
                  case ELF -> VillagerType.JUNGLE;
                  case ORC -> VillagerType.SWAMP;
                  case DWARF -> VillagerType.SNOW;
                  default -> VillagerType.forBiome(v.getWorld().getBiome(v.getBlockPos()));
               };
               if (v.getVillagerData().getType() != t) {
                  v.setVillagerData(v.getVillagerData().withType(t));
               }
            }
         } catch (Exception var12) {
            GodBox.LOG.warn("GodBox: не удалось применить расу", (Throwable)var12);
         }
      }
   }

   static void applyAll(Kingdoms.K k) {
      for (MobEntity m : new ArrayList<>(k.members)) {
         if (m.isAlive()) {
            apply(k, m);
         }
      }
   }

   private static int pct(Kingdoms.K k, int n, int pct) {
      if (n > 0 && pct != 100) {
         int x = n * Math.max(0, pct);
         int rest = x % 100;
         return x / 100 + (rest > 0 && k.world != null && k.world.getRandom().nextInt(100) < rest ? 1 : 0);
      } else {
         return n;
      }
   }

   static int yield(Kingdoms.K k, int type, int n) {
      return switch (of(k)) {
         case ELF -> type == 0 ? pct(k, n, Config.i("races.elf.wood", 125)) : (type == 2 ? pct(k, n, Config.i("races.elf.food", 125)) : n);
         case DWARF -> type == 1 ? pct(k, n, Config.i("races.dwarf.stone", 130)) : (type == 3 ? pct(k, n, Config.i("races.dwarf.ore", 140)) : n);
         default -> n;
      };
   }

   static int research(Kingdoms.K k, int gain) {
      return of(k) == Races.Race.ORC ? Math.max(1, gain * Config.i("races.orc.research", 70) / 100) : gain;
   }

   static int army(Kingdoms.K k, int cap) {
      return of(k) == Races.Race.ORC ? cap * Config.i("races.orc.army", 140) / 100 : cap;
   }

   static boolean breeds(Kingdoms.K k, Random r) {
      return of(k) != Races.Race.DWARF || r.nextInt(100) < Config.i("races.dwarf.growth", 65);
   }

   public static int ageBonus(Kingdoms.K k) {
      return switch (of(k)) {
         case ELF -> Config.i("races.elf.years", 25);
         case ORC -> Config.i("races.orc.years", -5);
         case DWARF -> Config.i("races.dwarf.years", 10);
         default -> 0;
      };
   }

   static int gold(Kingdoms.K k, int income) {
      return of(k) == Races.Race.HUMAN ? income * Config.i("races.human.gold", 115) / 100 : income;
   }

   static int faith(Kingdoms.K k) {
      return of(k) == Races.Race.ELF ? Config.i("races.elf.faith", 2) : 0;
   }

   static int opinion(Kingdoms.K a, Kingdoms.K b) {
      Races.Race x = of(a);
      Races.Race y = of(b);
      if (x == y) {
         return Config.i("races.opinion.same", 1);
      } else if ((x != Races.Race.ELF || y != Races.Race.ORC) && (x != Races.Race.ORC || y != Races.Race.ELF)) {
         return (x != Races.Race.ELF || y != Races.Race.DWARF) && (x != Races.Race.DWARF || y != Races.Race.ELF) ? 0 : Config.i("races.opinion.elfDwarf", -1);
      } else {
         return Config.i("races.opinion.elfOrc", -3);
      }
   }

   static String name(Kingdoms.K k, Random r, String[] human) {
      String[] a = switch (of(k)) {
         case ELF -> ELF_NAMES;
         case ORC -> ORC_NAMES;
         case DWARF -> DWARF_NAMES;
         default -> human;
      };
      return a[r.nextInt(a.length)];
   }

   private static void swap(Map<Block, Block> out, String path, String from, String to) {
      if (path.startsWith(from)) {
         Identifier id = Identifier.ofVanilla(to + path.substring(from.length()));
         if (Registries.BLOCK.containsId(id)) {
            Block b = Registries.BLOCK.get(id);
            if (!b.getDefaultState().hasBlockEntity()) {
               out.put(Registries.BLOCK.get(Identifier.ofVanilla(path)), b);
            }
         }
      }
   }

   private static Map<Block, Block> palette(Races.Race r) {
      Map<Block, Block> out = new HashMap<>();
      String wood = r == Races.Race.ELF ? "jungle_" : (r == Races.Race.ORC ? "dark_oak_" : "spruce_");
      String cobble = r == Races.Race.ELF ? "mossy_cobblestone" : (r == Races.Race.ORC ? "blackstone" : "cobbled_deepslate");
      String brick = r == Races.Race.ELF ? "mossy_stone_brick" : (r == Races.Race.ORC ? "polished_blackstone_brick" : "deepslate_brick");

      for (Identifier id : Registries.BLOCK.getIds()) {
         if (id.getNamespace().equals("minecraft")) {
            String p = id.getPath();
            if (!Registries.BLOCK.get(id).getDefaultState().hasBlockEntity()) {
               for (String src : new String[]{"oak_", "birch_", "spruce_"}) {
                  if (!wood.equals(src)) {
                     swap(out, p, src, wood);
                     swap(out, p, "stripped_" + src, "stripped_" + wood);
                  }
               }

               swap(out, p, "cobblestone", cobble);
               swap(out, p, "stone_brick", brick);
            }
         }
      }

      return out;
   }

   static BlockState style(Kingdoms.K k, BlockState s) {
      Races.Race r = of(k);
      if (r != Races.Race.HUMAN && s != null && Config.b("races.architecture", true)) {
         Block to = STYLE.computeIfAbsent(r, Races::palette).get(s.getBlock());
         return to == null ? s : Eras.copy(s, to);
      } else {
         return s;
      }
   }

   public static void power(ServerPlayerEntity god, ServerWorld w, BlockPos pos, Races.Race r) {
      Kingdoms.K k = Diplomacy.nearest(w, pos);
      if (k != null && k.alive) {
         if (of(k) == r) {
            god.sendMessage(Text.literal(k.title + ": раса уже " + r.title.toLowerCase()).formatted(k.color), true);
         } else {
            set(k, r);
            applyAll(k);
            if (k.world != null && Config.b("races.architecture", true)) {
               Eras.rebuild(k, Civ.city(k), k.world);
            }

            Chronicle.say(w.getServer(), k, "— теперь народ расы «" + r.title + "» (" + r.desc + ")");
            god.sendMessage(Text.literal(k.title + ": раса — " + r.title.toLowerCase()).formatted(k.color), true);
         }
      } else {
         god.sendMessage(Text.literal("Рядом нет королевств").formatted(Formatting.GRAY), true);
      }
   }

   static {
      SaveExt.register("race", Races::save, Races::load, RACE::clear);
      Inspect.EXTRA.add((e, out) -> {
         Kingdoms.K k = Kingdoms.of(e);
         if (k != null) {
            out.add("Раса: " + of(k).title);
         }
      });
   }

   public static enum Race {
      HUMAN("Люди", "торговцы: больше золота"),
      ELF("Эльфы", "долгожители: лес, еда и вера"),
      ORC("Орки", "воины: большая армия, слабая наука"),
      DWARF("Гномы", "рудокопы: камень и руда, крепкие, растут медленно");

      public final String title;
      public final String desc;

      private Race(String title, String desc) {
         this.title = title;
         this.desc = desc;
      }
   }
}
