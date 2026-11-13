package com.vladik.godbox;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

public final class Plague {
   public static boolean on = true;
   static final Map<Kingdoms.K, Plague.St> STATE = new EnumMap<>(Kingdoms.K.class);
   private static final Map<String, Double> CARRIERS = new HashMap<>();
   private static final Random RNG = new Random();
   private static final String[][] ADJ = new String[][]{
      {"Красный", "Красная", "Красное"},
      {"Чёрный", "Чёрная", "Чёрное"},
      {"Серый", "Серая", "Серое"},
      {"Болотный", "Болотная", "Болотное"},
      {"Багровый", "Багровая", "Багровое"},
      {"Ледяной", "Ледяная", "Ледяное"},
      {"Пепельный", "Пепельная", "Пепельное"},
      {"Гнилой", "Гнилая", "Гнилое"},
      {"Бледный", "Бледная", "Бледное"},
      {"Жёлтый", "Жёлтая", "Жёлтое"},
      {"Тихий", "Тихая", "Тихое"},
      {"Ночной", "Ночная", "Ночное"},
      {"Костяной", "Костяная", "Костяное"},
      {"Южный", "Южная", "Южное"}
   };
   private static final String[] NOUN = new String[]{"мор", "лихорадка", "чума", "хворь", "поветрие", "язва", "горячка", "недуг"};
   private static final int[] GENDER = new int[]{0, 1, 1, 1, 2, 1, 1, 0};
   private static MinecraftServer server;
   private static int t;

   private Plague() {
   }

   public static void init() {
      SaveExt.register("plague", Plague::save, Plague::load, Plague::reset);
   }

   private static List<String> save() {
      List<String> o = new ArrayList<>();
      o.add("law|" + on);

      for (Entry<Kingdoms.K, Plague.St> e : STATE.entrySet()) {
         Plague.St st = e.getValue();
         o.add(
            e.getKey().name()
               + "|"
               + (int)Math.round(st.level * 10.0)
               + "|"
               + (int)Math.round(st.immunity * 10.0)
               + "|"
               + st.quarantine
               + "|"
               + st.forced
               + "|"
               + st.qTimer
               + "|"
               + st.age
               + "|"
               + st.dead
               + "|"
               + st.strain.replace('|', ' ').replace('\n', ' ')
         );
      }

      return o;
   }

   private static void load(String[] a) {
      try {
         if (a.length >= 2 && a[0].equals("law")) {
            on = Boolean.parseBoolean(a[1]);
            return;
         }

         if (a.length < 9) {
            return;
         }

         Kingdoms.K k = Kingdoms.K.valueOf(a[0]);
         Plague.St st = new Plague.St();
         st.level = clamp(Integer.parseInt(a[1]) / 10.0);
         st.immunity = clamp(Integer.parseInt(a[2]) / 10.0);
         st.quarantine = Boolean.parseBoolean(a[3]);
         st.forced = Boolean.parseBoolean(a[4]);
         st.qTimer = Integer.parseInt(a[5]);
         st.age = Integer.parseInt(a[6]);
         st.dead = Integer.parseInt(a[7]);
         st.strain = a[8];
         STATE.put(k, st);
      } catch (IllegalArgumentException var3) {
      }
   }

   private static void reset() {
      STATE.clear();
      CARRIERS.clear();
      on = Config.b("plague.enabled", true);
      t = 0;
   }

   public static void clear() {
      STATE.clear();
      CARRIERS.clear();
      t = 0;
      server = null;
   }

   private static double clamp(double v) {
      return Math.max(0.0, Math.min(100.0, v));
   }

   private static Plague.St st(Kingdoms.K k) {
      return STATE.computeIfAbsent(k, x -> new Plague.St());
   }

   private static boolean ok(Kingdoms.K k) {
      return k != null && k.alive && k.world != null && k.capital != null;
   }

   public static int level(Kingdoms.K k) {
      Plague.St st = k == null ? null : STATE.get(k);
      return st == null ? 0 : (int)Math.ceil(st.level);
   }

   public static boolean infected(Kingdoms.K k) {
      return level(k) > 0;
   }

   public static boolean quarantined(Kingdoms.K k) {
      Plague.St st = k == null ? null : STATE.get(k);
      return st != null && st.quarantine;
   }

   public static boolean blocked(Kingdoms.K a, Kingdoms.K b) {
      return on && (quarantined(a) || quarantined(b));
   }

   public static String status(Kingdoms.K k) {
      Plague.St st = k == null ? null : STATE.get(k);
      if (st == null) {
         return "здорово";
      } else if (st.level > 0.0) {
         return st.strain + ": " + level(k) + "%" + (st.quarantine ? " (карантин)" : "");
      } else if (st.quarantine) {
         return "здорово (карантин)";
      } else {
         return st.immunity >= 10.0 ? "здорово (иммунитет " + (int)st.immunity + "%)" : "здорово";
      }
   }

   static String line(Kingdoms.K k) {
      Plague.St st = STATE.get(k);
      String c = st != null && (!(st.level <= 0.0) || st.quarantine) ? (st.level >= 50.0 ? "§4" : (st.level > 0.0 ? "§c" : "§e")) : "§a";
      return c + status(k);
   }

   static String strain() {
      int n = RNG.nextInt(NOUN.length);
      return ADJ[RNG.nextInt(ADJ.length)][GENDER[n]] + " " + NOUN[n];
   }

   private static String came(String strain) {
      return !strain.endsWith("а") && !strain.endsWith("ь") ? (strain.endsWith("е") ? "пришло" : "пришёл") : "пришла";
   }

   private static void announce(String msg) {
      Chronicle.log("§2" + msg);
      if (server != null) {
         Text tx = Text.literal("[Летопись] ").formatted(Formatting.GOLD).append(Text.literal("☣ " + msg).formatted(Formatting.DARK_GREEN));

         for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            p.sendMessage(tx, false);
         }
      }
   }

   private static List<VillagerEntity> villagers(Kingdoms.K k) {
      List<VillagerEntity> l = new ArrayList<>();

      for (MobEntity m : k.members) {
         if (m instanceof VillagerEntity v && v.isAlive()) {
            l.add(v);
         }
      }

      return l;
   }

   static boolean outbreak(Kingdoms.K k, String strain, double level, String how) {
      if (!ok(k)) {
         return false;
      } else {
         if (k.world.getServer() != null) {
            server = k.world.getServer();
         }

         Plague.St st = st(k);
         boolean fresh = st.level <= 0.0;
         if (fresh) {
            st.strain = strain != null && !strain.isBlank() ? strain : strain();
            st.age = 0;
            st.dead = 0;
         }

         st.level = clamp(Math.max(st.level, level));
         if (fresh) {
            BlockPos p = k.capital;
            Fx.particles(k.world, ParticleTypes.SNEEZE, p.getX() + 0.5, p.getY() + 2, p.getZ() + 0.5, 60, 4.0, 1.5, 4.0, 0.02);
            if (how != null) {
               announce(how);
            }
         }

         return fresh;
      }
   }

   public static boolean carry(Kingdoms.K from, Kingdoms.K to, double chance, String via) {
      return carry(from, to, chance, via, level(from), from != null && STATE.containsKey(from) ? STATE.get(from).strain : null);
   }

   private static boolean carry(Kingdoms.K from, Kingdoms.K to, double chance, String via, double src, String strain) {
      if (on && from != null && from != to && ok(to) && !(src <= 0.0) && strain != null && !strain.isEmpty()) {
         Plague.St ts = st(to);
         if (ts.level > 0.0) {
            return false;
         } else {
            double p = chance * Math.min(1.0, src / Config.d("plague.fullSpreadLevel", 50.0)) * (1.0 - ts.immunity / 100.0);
            if (quarantined(from) || ts.quarantine) {
               p *= Config.d("plague.quarantineLeak", 0.1);
            }

            return RNG.nextDouble() >= p
               ? false
               : outbreak(
                  to, strain, Config.d("plague.startLevel", 10.0), "«" + strain + "» " + came(strain) + " в " + to.title + " " + via + " «" + from.title + "»"
               );
         }
      } else {
         return false;
      }
   }

   static void depart(Kingdoms.K a, Kingdoms.K b) {
      if (on && a != null && b != null) {
         String key = a.name() + ">" + b.name();
         if (level(a) > 0) {
            CARRIERS.put(key, (double)level(a));
         } else {
            CARRIERS.remove(key);
         }
      }
   }

   static void arrive(Kingdoms.K a, Kingdoms.K b, double chance, String via) {
      if (a != null && b != null) {
         Double lv = CARRIERS.remove(a.name() + ">" + b.name());
         Plague.St fs = STATE.get(a);
         if (fs != null && !fs.strain.isEmpty()) {
            double src = Math.max(lv == null ? 0.0 : lv, fs.level);
            carry(a, b, chance, via, src, fs.strain);
         }
      }
   }

   static void conquered(Kingdoms.K a, Kingdoms.K d) {
      if (d != null) {
         carry(d, a, Config.d("plague.conquestChance", 0.8), "с покорёнными жителями из");
         STATE.remove(d);
      }
   }

   static void seed(Kingdoms.K k, String how) {
      if (on && ok(k)) {
         Plague.St st = st(k);
         if (!(st.level > 0.0) && !(RNG.nextDouble() * 100.0 < st.immunity)) {
            String s = strain();
            outbreak(k, s, Config.d("plague.startLevel", 10.0) + 5.0, "В " + k.title + " началась эпидемия: «" + s + "» (" + how + ")");
         }
      }
   }

   public static void tick(MinecraftServer s) {
      server = s;
      if (!s.getTickManager().isFrozen()) {
         t++;
         int interval = Math.max(100, Config.i("plague.interval", 600));
         if (on && t % interval == 0) {
            if (!Perf.go) {
               t--;
            } else {
               for (Kingdoms.K k : Kingdoms.K.values()) {
                  if (!k.alive) {
                     STATE.remove(k);
                  }
               }

               observe();

               for (Kingdoms.K kx : Kingdoms.K.values()) {
                  if (ok(kx)) {
                     Plague.St st = st(kx);
                     if (st.level > 0.0) {
                        progress(s, kx, st);
                     } else {
                        idle(kx, st);
                     }

                     decide(s, kx, st);
                  }
               }

               spread();
            }
         }
      }
   }

   private static void observe() {
      int need = Math.max(1, Config.i("plague.seedCarriers", 4));

      for (Kingdoms.K k : Kingdoms.K.values()) {
         if (ok(k) && level(k) <= 0) {
            Powers2.Epidemic ep = Powers2.PLAGUES.get(k.world);
            if (ep != null && ep.sick.size() >= need) {
               int n = 0;

               for (MobEntity m : k.members) {
                  if (m.isAlive() && ep.sick.containsKey(m.getUuid())) {
                     n++;
                  }
               }

               if (n >= need) {
                  seed(k, "чума среди жителей");
               }
            }
         }
      }
   }

   private static double crowding(Civ.City c, int pop) {
      int houses = c.count(Civ.Kind.HOUSE) + c.count(Civ.Kind.MANOR);
      return Math.max(0.5, Math.min(2.0, (double)pop / Math.max(1, houses * 3)));
   }

   private static void idle(Kingdoms.K k, Plague.St st) {
      st.immunity = clamp(st.immunity - Config.d("plague.immunityDecay", 0.5));
      ServerWorld w = k.world;
      BlockPos p = k.capital;
      if (w.shouldTickEntity(p) && w.isChunkLoaded(p.getX() >> 4, p.getZ() >> 4)) {
         Civ.City c = Civ.city(k);
         int pop = villagers(k).size();
         if (pop >= 8) {
            double ch = Config.d("plague.spontaneous", 0.004) * crowding(c, pop) * (1.0 - st.immunity / 100.0);
            RegistryEntry<Biome> biome = w.getBiome(p);
            if (biome.matchesKey(BiomeKeys.SWAMP) || biome.matchesKey(BiomeKeys.MANGROVE_SWAMP)) {
               ch *= 3.0;
            }

            if (c.hungry || c.food < 5) {
               ch *= 2.0;
            }

            if (Tech.has(k, Tech.T.MEDICINE)) {
               ch *= 0.5;
            }

            if (!(RNG.nextDouble() >= ch)) {
               String s = strain();
               outbreak(k, s, Config.d("plague.startLevel", 10.0), "В " + k.title + " вспыхнула болезнь: «" + s + "»");
            }
         }
      }
   }

   private static int healers(Kingdoms.K k) {
      ServerWorld w = k.world;
      if (!w.shouldTickEntity(k.capital)) {
         return 0;
      } else {
         Box box = new Box(k.capital).expand(64.0, 40.0, 64.0);
         return w.getEntitiesByClass(
               MobEntity.class, box, m -> m.isAlive() && (m.getCommandTags().contains("gbc_white_mage") || m.getCommandTags().contains("gbc_plague_doctor"))
            )
            .size();
      }
   }

   private static void progress(MinecraftServer s, Kingdoms.K k, Plague.St st) {
      ServerWorld w = k.world;
      Civ.City c = Civ.city(k);
      List<VillagerEntity> vill = villagers(k);
      int pop = vill.size();
      st.age++;
      double grow = Config.d("plague.growth", 6.0) * crowding(c, pop) * Math.max(0.4, Math.min(1.5, pop / 20.0)) * (1.0 - st.immunity / 100.0);
      double heal = st.age * Config.d("plague.burnout", 0.5)
         + c.count(Civ.Kind.TEMPLE) * 2
         + c.count(Civ.Kind.WELL) * 0.5
         + (Tech.has(k, Tech.T.MEDICINE) ? 4 : 0)
         + (Tech.has(k, Tech.T.MAGIC) ? 2 : 0)
         + Math.min(3, healers(k)) * 5
         + (st.quarantine ? 3 : 0);
      st.level = clamp(st.level + grow - heal);
      st.immunity = clamp(st.immunity + st.level * 0.04);
      if (!(st.level <= 0.0) && pop != 0) {
         int lv = (int)st.level;
         c.food = Math.max(0, c.food - Math.max(1, lv / 20));
         Economy.add(k, -(lv / 25));
         Unrest.add(k, lv >= 50 ? 2 : 1);
         if (w.shouldTickEntity(k.capital)) {
            int sick = Math.min(8, Math.max(1, pop * lv / 250));

            for (int i = 0; i < sick && !vill.isEmpty(); i++) {
               VillagerEntity v = vill.get(RNG.nextInt(vill.size()));
               v.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 400, 0));
               v.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 400, 0));
               Fx.particles(w, ParticleTypes.SNEEZE, v.getX(), v.getY() + v.getHeight() * 0.8, v.getZ(), 6, 0.2, 0.2, 0.2, 0.01);
            }

            int max = Math.max(0, Config.i("plague.maxDeaths", 2));
            int floor = Math.max(1, Config.i("plague.minSurvivors", 6));
            int n = lv >= 30 ? Math.min(max, 1 + lv / 45) : (RNG.nextInt(30) < lv ? Math.min(max, 1) : 0);
            MobEntity king = Life.king(k);
            int alive = pop;

            for (int i = 0; i < 10 && n > 0 && alive > floor; i++) {
               VillagerEntity v = vill.get(RNG.nextInt(vill.size()));
               if (v != king && v.isAlive()) {
                  Fx.particles(w, ParticleTypes.SNEEZE, v.getX(), v.getY() + 1.5, v.getZ(), 30, 0.4, 0.4, 0.4, 0.05);
                  v.kill();
                  st.dead++;
                  alive--;
                  n--;
               }
            }
         }
      } else {
         st.level = 0.0;
         st.immunity = clamp(Math.max(st.immunity, Config.d("plague.immunityAfter", 80.0)));
         Chronicle.say(s, k, "пережило болезнь «" + st.strain + "»: умерло " + st.dead + " жителей");
      }
   }

   private static void decide(MinecraftServer s, Kingdoms.K k, Plague.St st) {
      if (st.quarantine) {
         Economy.add(k, -Math.max(0, Config.i("plague.quarantineGold", 3)));
         if (++st.qCost % 3 == 0) {
            Unrest.add(k, 1);
         }
      }

      if (!st.forced) {
         int trait = Life.kingTrait(k);
         boolean careful = trait == 0 || trait == 3;
         if (st.quarantine) {
            if (--st.qTimer <= 0) {
               if (!(st.level >= 10.0) && (trait != 3 || !threat(k))) {
                  st.quarantine = false;
                  Chronicle.say(s, k, "снимает карантин и открывает границы");
               } else {
                  st.qTimer = 2;
               }
            }
         } else {
            int at = Config.i("plague.quarantineAt", 35);
            if (trait == 2) {
               at += 25;
            }

            if (careful) {
               at = Math.max(8, at / 3);
            }

            boolean close = st.level >= at || trait == 3 && st.level <= 0.0 && st.immunity < 40.0 && threat(k);
            if (close) {
               st.quarantine = true;
               st.qTimer = Math.max(2, Config.i("plague.quarantineTime", 12));
               Chronicle.say(s, k, st.level > 0.0 ? "закрывает границы на карантин: «" + st.strain + "»" : "закрывает границы, опасаясь болезни у соседей");
            }
         }
      }
   }

   private static boolean threat(Kingdoms.K k) {
      for (Kingdoms.K o : Kingdoms.K.values()) {
         if (o != k && ok(o) && o.world == k.world && level(o) >= 40) {
            return true;
         }
      }

      return false;
   }

   private static List<BlockPos> nodes(Kingdoms.K k) {
      List<BlockPos> l = new ArrayList<>();
      l.add(k.capital);
      l.addAll(Conquest.towns(k));
      return l;
   }

   static boolean road(Kingdoms.K a, Kingdoms.K b) {
      if (!ok(a) || !ok(b) || a.world != b.world) {
         return false;
      } else if (Roads.DONE.isEmpty() && Highways.DONE.isEmpty()) {
         return false;
      } else {
         List<BlockPos> nb = nodes(b);

         for (BlockPos p : nodes(a)) {
            for (BlockPos q : nb) {
               String key = Roads.key(p, q);
               if (Roads.DONE.contains(key) || Highways.DONE.contains(key)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private static boolean fighting(Kingdoms.K a, Kingdoms.K b) {
      return Diplomacy.rel(a, b) != 1 ? false : near(a, b.capital) || near(b, a.capital);
   }

   private static boolean near(Kingdoms.K k, BlockPos p) {
      for (MobEntity m : k.members) {
         if (m.isAlive() && !(m instanceof VillagerEntity) && m.squaredDistanceTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5) < 3600.0) {
            return true;
         }
      }

      return false;
   }

   private static void spread() {
      double roadCh = Config.d("plague.roadChance", 0.1);
      double warCh = Config.d("plague.warChance", 0.15);

      for (Kingdoms.K a : Kingdoms.K.values()) {
         if (ok(a) && level(a) >= 10) {
            for (Kingdoms.K b : Kingdoms.K.values()) {
               if (b != a && ok(b) && b.world == a.world && level(b) <= 0 && (!road(a, b) || !carry(a, b, roadCh, "по дороге из")) && fighting(a, b)) {
                  carry(a, b, warCh, "с войском из");
               }
            }
         }
      }
   }

   private static void msg(ServerPlayerEntity god, String s, Formatting f) {
      god.sendMessage(Text.literal(s).formatted(f), true);
   }

   private static Kingdoms.K nearest(ServerWorld w, BlockPos pos, double r) {
      Kingdoms.K best = null;
      double bd = r * r;

      for (Kingdoms.K k : Kingdoms.K.values()) {
         if (ok(k) && k.world == w) {
            for (BlockPos p : nodes(k)) {
               double dx = p.getX() - pos.getX();
               double dz = p.getZ() - pos.getZ();
               double d = dx * dx + dz * dz;
               if (d < bd) {
                  bd = d;
                  best = k;
               }
            }
         }
      }

      return best;
   }

   public static void power(ServerPlayerEntity god, ServerWorld w, BlockPos pos) {
      if (!on) {
         msg(god, "Закон «Эпидемии» выключен", Formatting.GRAY);
      } else {
         Kingdoms.K k = nearest(w, pos, 250.0);
         if (k == null) {
            msg(god, "Рядом нет королевств", Formatting.GRAY);
         } else {
            Plague.St st = st(k);
            if (st.level <= 0.0) {
               String s = strain();
               st.immunity = Math.min(st.immunity, 20.0);
               outbreak(k, s, Config.d("plague.powerLevel", 25.0), "Бог наслал болезнь на " + k.title + ": «" + s + "»");
            } else {
               st.level = clamp(st.level + 20.0);
            }

            Fx.sound(w, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ENTITY_ZOMBIE_VILLAGER_AMBIENT, 2.0F, 0.6F);
            msg(god, "☣ " + k.title + " — " + status(k), Formatting.DARK_GREEN);
         }
      }
   }

   public static void quarantine(ServerPlayerEntity god, ServerWorld w, BlockPos pos) {
      if (!on) {
         msg(god, "Закон «Эпидемии» выключен", Formatting.GRAY);
      } else {
         Kingdoms.K k = nearest(w, pos, 250.0);
         if (k == null) {
            msg(god, "Рядом нет королевств", Formatting.GRAY);
         } else {
            Plague.St st = st(k);
            boolean v = !st.quarantine;
            st.quarantine = v;
            st.forced = v;
            st.qTimer = v ? Math.max(2, Config.i("plague.quarantineTime", 12)) : 0;
            Chronicle.say(w.getServer(), k, v ? "закрывает границы на карантин по воле бога" : "снимает карантин по воле бога");
            msg(god, "Карантин — " + k.title + ": " + (v ? "границы закрыты, торговля стоит" : "границы открыты"), v ? Formatting.YELLOW : Formatting.GREEN);
         }
      }
   }

   static void cure(ServerPlayerEntity god, ServerWorld w, BlockPos pos, int brush) {
      Kingdoms.K k = nearest(w, pos, 120.0);
      Plague.St st = k == null ? null : STATE.get(k);
      if (st != null && !(st.level <= 0.0)) {
         st.level = clamp(st.level - Config.d("plague.cureAmount", 20.0) - brush * 2);
         st.immunity = clamp(st.immunity + 5.0);
         if (st.level <= 0.0) {
            st.immunity = clamp(Math.max(st.immunity, Config.d("plague.immunityAfter", 80.0)));
            Chronicle.say(w.getServer(), k, "исцелено богом от болезни «" + st.strain + "»: умерло " + st.dead + " жителей");
            if (god != null) {
               msg(god, k.title + ": болезнь побеждена", Formatting.GREEN);
            }
         } else if (god != null) {
            msg(god, k.title + " — " + status(k), Formatting.GREEN);
         }
      }
   }

   static final class St {
      double level;
      double immunity;
      boolean quarantine;
      boolean forced;
      int qTimer;
      int qCost;
      int age;
      int dead;
      String strain = "";
   }
}
