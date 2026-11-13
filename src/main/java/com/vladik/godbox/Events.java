package com.vladik.godbox;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.VindicatorEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap.Type;

public final class Events {
   public static boolean on = true;
   private static int timer;
   private static final int MAX_LEN = 12;
   private static final Map<Kingdoms.K, EnumMap<Events.Cond, Events.St>> COND = new EnumMap<>(Kingdoms.K.class);
   private static final Map<Kingdoms.K, Integer> COOL = new EnumMap<>(Kingdoms.K.class);
   private static final Map<Kingdoms.K, UUID> HERO = new EnumMap<>(Kingdoms.K.class);
   private static final Map<Kingdoms.K, String> HERO_NAME = new EnumMap<>(Kingdoms.K.class);
   private static final Map<Kingdoms.K, Integer> HERO_AGE = new EnumMap<>(Kingdoms.K.class);
   private static final Map<Kingdoms.K, MobEntity> HERO_REF = new EnumMap<>(Kingdoms.K.class);

   private Events() {
   }

   public static void init() {
      SaveExt.register("events", Events::saveLines, Events::loadLine, Events::reset);
   }

   private static List<String> saveLines() {
      List<String> out = new ArrayList<>();

      for (Entry<Kingdoms.K, EnumMap<Events.Cond, Events.St>> e : COND.entrySet()) {
         for (Entry<Events.Cond, Events.St> c : e.getValue().entrySet()) {
            out.add("C|" + e.getKey().name() + "|" + c.getKey().name() + "|" + c.getValue().left + "|" + c.getValue().stage);
         }
      }

      for (Entry<Kingdoms.K, UUID> e : HERO.entrySet()) {
         String n = HERO_NAME.getOrDefault(e.getKey(), "").replace('|', ' ').replace('\n', ' ');
         out.add("H|" + e.getKey().name() + "|" + e.getValue() + "|" + HERO_AGE.getOrDefault(e.getKey(), 0) + "|" + n);
      }

      return out;
   }

   private static void loadLine(String[] a) {
      try {
         if (a.length < 5) {
            return;
         }

         Kingdoms.K k = Kingdoms.K.valueOf(a[1]);
         if (a[0].equals("C")) {
            Events.St st = new Events.St(Math.max(1, Math.min(12, Integer.parseInt(a[3]))), Integer.parseInt(a[4]));
            st.fresh = false;
            COND.computeIfAbsent(k, x -> new EnumMap<>(Events.Cond.class)).put(Events.Cond.valueOf(a[2]), st);
         } else if (a[0].equals("H")) {
            HERO.put(k, UUID.fromString(a[2]));
            HERO_AGE.put(k, Integer.parseInt(a[3]));
            HERO_NAME.put(k, a[4]);
         }
      } catch (Exception var3) {
      }
   }

   private static void reset() {
      COND.clear();
      COOL.clear();
      HERO.clear();
      HERO_NAME.clear();
      HERO_AGE.clear();
      HERO_REF.clear();
   }

   public static boolean has(Kingdoms.K k, Events.Cond c) {
      EnumMap<Events.Cond, Events.St> m = COND.get(k);
      return m != null && m.containsKey(c);
   }

   public static int left(Kingdoms.K k, Events.Cond c) {
      EnumMap<Events.Cond, Events.St> m = COND.get(k);
      Events.St st = m == null ? null : m.get(c);
      return st == null ? 0 : st.left;
   }

   public static Events.Cond condition(Kingdoms.K k) {
      EnumMap<Events.Cond, Events.St> m = COND.get(k);
      if (m == null) {
         return null;
      } else {
         for (Events.Cond c : Events.Cond.values()) {
            if (m.containsKey(c)) {
               return c;
            }
         }

         return null;
      }
   }

   public static List<Events.Cond> conditions(Kingdoms.K k) {
      List<Events.Cond> out = new ArrayList<>();
      EnumMap<Events.Cond, Events.St> m = COND.get(k);
      if (m != null) {
         for (Events.Cond c : Events.Cond.values()) {
            if (m.containsKey(c)) {
               out.add(c);
            }
         }
      }

      return out;
   }

   public static String status(Kingdoms.K k) {
      StringBuilder s = new StringBuilder();

      for (Events.Cond c : conditions(k)) {
         if (s.length() > 0) {
            s.append(", ");
         }

         s.append(c.title);
      }

      return s.toString();
   }

   static String summary(Kingdoms.K k) {
      String s = status(k);
      return s.isEmpty() ? "" : " | " + s.toLowerCase();
   }

   static String statusLine(Kingdoms.K k) {
      StringBuilder s = new StringBuilder();
      if (Seasons.on) {
         s.append("§7Сезон: §f").append(Seasons.label()).append("§7, климат ").append(Seasons.climate(k));
         if (Seasons.frozen(k)) {
            s.append(", §bпорт во льду");
         }

         s.append('\n');
      }

      s.append("§7Состояние: ");
      List<Events.Cond> l = conditions(k);
      if (l.isEmpty()) {
         s.append("§aспокойно");
      }

      for (int i = 0; i < l.size(); i++) {
         if (i > 0) {
            s.append("§7, ");
         }

         s.append('§').append(l.get(i).color).append(l.get(i).title);
      }

      return s.append('\n').toString();
   }

   static int harvest(Kingdoms.K k, int n) {
      if (n > 0 && k != null) {
         double m = Seasons.food(k);
         if (on && has(k, Events.Cond.DROUGHT)) {
            m *= Math.max(0.1, Math.min(1.0, Config.d("events.drought.yield", 0.5)));
         }

         if (on && has(k, Events.Cond.GOLDEN_AGE)) {
            m *= 1.15;
         }

         if (m == 1.0) {
            return n;
         } else {
            double v = n * m;
            int whole = (int)Math.floor(v);
            return whole + (Math.random() < v - whole ? 1 : 0);
         }
      } else {
         return n;
      }
   }

   static int birthPeriod(Kingdoms.K k) {
      int p = Seasons.birthPeriod();
      if (!on) {
         return p;
      } else if (has(k, Events.Cond.FAMINE)) {
         return p * 2;
      } else {
         return has(k, Events.Cond.BOOM) ? Math.max(1, p - 1) : p;
      }
   }

   public static void tick(MinecraftServer s) {
      if (on && ++timer % Math.max(200, Config.i("events.period", 1800)) == 0) {
         for (Kingdoms.K k : Kingdoms.K.values()) {
            if (!k.alive) {
               drop(k);
            } else if (k.world != null && k.capital != null && k.world.shouldTickEntity(k.capital)) {
               Random r = k.world.getRandom();

               try {
                  step(s, k);
               } catch (Exception var7) {
                  GodBox.LOG.error("GodBox: цепочка событий {} не удалась", k.name(), var7);
               }

               if (k.alive && k.world != null && k.capital != null && r.nextInt(Math.max(1, Config.i("events.chance", 4))) == 0) {
                  fire(s, k, pick(k, r));
               }
            }
         }
      }
   }

   public static void force(ServerPlayerEntity god, ServerWorld w, BlockPos pos) {
      Kingdoms.K k = Diplomacy.nearest(w, pos);
      if (k == null) {
         god.sendMessage(Text.literal("Рядом нет королевства"), true);
      } else {
         fire(god.getServer(), k, pick(k, w.getRandom()));
      }
   }

   private static int pick(Kingdoms.K k, Random r) {
      double[] wt = new double[9];
      double sum = 0.0;

      for (int e = 0; e < wt.length; e++) {
         double x = Seasons.weight(k, e);
         if (e == 1 && (has(k, Events.Cond.DROUGHT) || has(k, Events.Cond.FAMINE))) {
            x = 0.0;
         }

         if (e == 0 && has(k, Events.Cond.DROUGHT)) {
            x *= 0.3;
         }

         if (e == 7 && has(k, Events.Cond.REBUILD)) {
            x = 0.0;
         }

         if (e == 6 && HERO.containsKey(k)) {
            x = 0.0;
         }

         if (e == 5 && has(k, Events.Cond.FAMINE)) {
            x = 0.0;
         }

         wt[e] = Math.max(0.0, x);
         sum += wt[e];
      }

      if (sum <= 0.0) {
         return r.nextInt(9);
      } else {
         double xx = r.nextDouble() * sum;

         for (int e = 0; e < wt.length; e++) {
            xx -= wt[e];
            if (xx < 0.0) {
               return e;
            }
         }

         return 8;
      }
   }

   private static void drop(Kingdoms.K k) {
      COND.remove(k);
      COOL.remove(k);
      HERO.remove(k);
      HERO_NAME.remove(k);
      HERO_AGE.remove(k);
      HERO_REF.remove(k);
   }

   private static Events.St start(Kingdoms.K k, Events.Cond c, int len) {
      Events.St st = new Events.St(Math.max(1, Math.min(12, len)), 0);
      COND.computeIfAbsent(k, x -> new EnumMap<>(Events.Cond.class)).put(c, st);
      return st;
   }

   private static void end(Kingdoms.K k, Events.Cond c) {
      EnumMap<Events.Cond, Events.St> m = COND.get(k);
      if (m != null) {
         m.remove(c);
      }
   }

   private static void step(MinecraftServer s, Kingdoms.K k) {
      ServerWorld w = k.world;
      Civ.City c = Civ.city(k);
      Random r = w.getRandom();
      COOL.computeIfPresent(k, (x, v) -> v <= 1 ? null : v - 1);
      heroStep(s, k, w);
      EnumMap<Events.Cond, Events.St> m = COND.get(k);
      if (m != null) {
         for (Events.Cond cd : Events.Cond.values()) {
            Events.St st = m.get(cd);
            if (st != null) {
               if (st.fresh) {
                  st.fresh = false;
               } else {
                  during(s, k, c, w, r, cd, st);
                  if (m.get(cd) == st && --st.left <= 0) {
                     m.remove(cd);
                     expire(s, k, c, r, cd, st);
                  }
               }
            }
         }
      }

      if (!has(k, Events.Cond.FAMINE) && !COOL.containsKey(k) && c.hungry && c.food <= 0) {
         boolean drought = has(k, Events.Cond.DROUGHT);
         if (drought || Seasons.winter(k) && r.nextInt(2) == 0) {
            start(k, Events.Cond.FAMINE, Config.i("events.famine.max", 6));
            end(k, Events.Cond.BOOM);
            end(k, Events.Cond.ABUNDANCE);
            Chronicle.say(s, k, drought ? "— из-за засухи начался голод: амбары пусты" : "— из-за суровой зимы начался голод: запасы кончились");
         }
      }
   }

   private static void during(MinecraftServer s, Kingdoms.K k, Civ.City c, ServerWorld w, Random r, Events.Cond cd, Events.St st) {
      BlockPos cap = k.capital;
      switch (cd) {
         case FAMINE:
            if (!c.hungry && c.food >= Config.i("events.famine.end", 15)) {
               end(k, cd);
               COOL.put(k, 3);
               Chronicle.say(s, k, "— голод отступил: амбары снова наполняются");
               return;
            }

            st.stage++;
            MobEntity king = Life.king(k);
            List<VillagerEntity> vill = villagers(k);
            if (vill.size() > 4) {
               VillagerEntity v = vill.get(r.nextInt(vill.size()));
               if (v != king) {
                  v.damage(w.getDamageSources().starve(), 4.0F);
               }
            }

            Unrest.add(k, Math.max(0, Math.min(15, Config.i("events.famine.unrest", 4))));
            if (st.stage == 2) {
               boolean desperate = Life.kingTrait(k) == 1 || Unrest.unrest(k) >= 50;
               if (desperate && r.nextInt(100) < Config.i("events.raid.chance", 60) && raid(s, k, c, r)) {
                  return;
               }
            }

            if (st.stage >= 2 && st.stage < 2 + Math.max(0, Math.min(6, Config.i("events.refugees.waves", 3)))) {
               leave(s, k, 1 + r.nextInt(Math.max(1, Math.min(6, Config.i("events.refugees.size", 3)))), true);
            }
            break;
         case DROUGHT:
            if (w.isRaining() && !Seasons.hot(k) && r.nextInt(2) == 0) {
               end(k, cd);
               Chronicle.say(s, k, "— дожди прекратили засуху");
               return;
            }

            int n = Math.min(c.food, (int)Math.round(Config.i("events.drought.drain", 4) * Seasons.severity(k)));
            if (n > 0) {
               c.food -= n;
            }

            Fx.particles(w, ParticleTypes.WHITE_ASH, cap.getX() + 0.5, cap.getY() + 3, cap.getZ() + 0.5, 60, 8.0, 3.0, 8.0, 0.02);
            break;
         case MOURNING:
            Unrest.add(k, 2);
            break;
         case REBUILD:
            int nw = Math.max(0, Config.i("events.rebuild.wood", 6));
            int ns = Math.max(0, Config.i("events.rebuild.stone", 3));
            if (c.wood >= nw && c.stone >= ns) {
               c.wood -= nw;
               c.stone -= ns;
            } else {
               c.wood = Math.max(0, c.wood - nw);
               c.stone = Math.max(0, c.stone - ns);
               Unrest.add(k, 5);
               if (st.stage == 0) {
                  Chronicle.say(s, k, "— из-за нехватки дерева и камня восстановление после пожара встало: погорельцы ропщут");
               }

               st.stage = 1;
            }
            break;
         case GOLDEN_AGE:
            Unrest.add(k, -3);
            c.faith = Math.min(999, c.faith + 3);
            Fx.particles(w, ParticleTypes.HAPPY_VILLAGER, cap.getX() + 0.5, cap.getY() + 2, cap.getZ() + 0.5, 40, 6.0, 2.0, 6.0, 0.1);
            break;
         case GOLD_RUSH:
            if (Economy.on) {
               Economy.add(k, Math.max(0, Math.min(200, Config.i("events.rush.gold", 12))));
            }

            c.ore += 2;
            if (st.stage == 0 && r.nextInt(100) < Config.i("events.rush.bandits", 35)) {
               st.stage = 1;
               Chronicle.say(s, k, "— слухи о золоте привлекли разбойников");
               fire(s, k, 3);
            }
            break;
         case BOOM:
            if (st.stage == 0 && villagers(k).size() >= CivPlus.cap(c)) {
               st.stage = 1;
               emigrate(s, k, w);
            }
      }
   }

   private static void expire(MinecraftServer s, Kingdoms.K k, Civ.City c, Random r, Events.Cond cd, Events.St st) {
      switch (cd) {
         case FAMINE:
            COOL.put(k, 3);
            c.food += 6;
            Chronicle.say(s, k, "— голод пошёл на убыль: выжившие делят последние запасы");
            break;
         case DROUGHT:
            Chronicle.say(s, k, "— засуха отступила, поля снова зеленеют");
         case MOURNING:
         case BOOM:
         default:
            break;
         case REBUILD:
            Chronicle.say(s, k, st.stage == 0 ? "— город отстроен после пожара" : "— восстановление после пожара сорвано: следы огня остались");
            break;
         case GOLDEN_AGE:
            Chronicle.say(s, k, "— золотой век подошёл к концу");
            break;
         case GOLD_RUSH:
            Chronicle.say(s, k, "— золотая лихорадка утихла: жила истощилась");
            break;
         case FESTIVAL:
            pilgrimage(s, k, c, r);
            break;
         case ABUNDANCE:
            if (c.food >= Config.i("events.boom.food", 30) && !has(k, Events.Cond.FAMINE)) {
               start(k, Events.Cond.BOOM, Config.i("events.boom.length", 3));
               Chronicle.say(s, k, "— благодаря богатому урожаю начался бум рождаемости");
            }
            break;
         case LODE:
            start(k, Events.Cond.GOLD_RUSH, Config.i("events.rush.length", 4));
            Chronicle.say(s, k, "— весть о золотой жиле разнеслась: началась золотая лихорадка, казна растёт");
      }
   }

   private static List<VillagerEntity> villagers(Kingdoms.K k) {
      List<VillagerEntity> vill = new ArrayList<>();

      for (MobEntity m : k.members) {
         if (m instanceof VillagerEntity v && v.isAlive()) {
            vill.add(v);
         }
      }

      return vill;
   }

   private static Kingdoms.K neighbour(Kingdoms.K k) {
      Kingdoms.K best = null;
      double bd = Double.MAX_VALUE;

      for (Kingdoms.K o : Kingdoms.K.values()) {
         if (o != k && o.alive && o.world == k.world && o.capital != null && !Diplomacy.hostile(k, o) && o.members.size() < 240) {
            double d = o.capital.getSquaredDistance(k.capital);
            if (d < bd) {
               bd = d;
               best = o;
            }
         }
      }

      return best;
   }

   private static boolean leave(MinecraftServer s, Kingdoms.K k, int n, boolean famine) {
      int keep = Math.max(4, Config.i("events.refugees.keep", 6));
      n = Math.min(n, villagers(k).size() - keep);
      if (n <= 0) {
         return false;
      } else {
         List<MobEntity> g = Colonies.settlers(k, n);
         if (g.isEmpty()) {
            return false;
         } else {
            Kingdoms.K o = neighbour(k);
            if (o == null) {
               if (!famine) {
                  return false;
               } else {
                  for (MobEntity m : g) {
                     k.members.remove(m);
                     m.discard();
                  }

                  Chronicle.say(s, k, "— из-за голода жители (" + g.size() + ") покинули королевство и разбрелись по свету: приюта им никто не дал");
                  return true;
               }
            } else {
               for (MobEntity m : g) {
                  k.members.remove(m);
                  Kingdoms.join(o, m);
                  o.members.add(m);
                  m.setTarget(null);
               }

               Plague.carry(k, o, 0.5, "с беженцами из");
               Civ.City oc = Civ.city(o);
               boolean welcome = !oc.hungry && oc.food >= 40;
               Diplomacy.op(k, o, welcome ? 6 : -6);
               Unrest.add(o, Math.min(6, g.size() + (welcome ? 0 : 2)));
               Chronicle.say(s, k, (famine ? "— из-за голода беженцы (" : "— из-за тесноты переселенцы (") + g.size() + ") уходят к соседям: " + o.title);
               walk(s, o, g, welcome);
               return true;
            }
         }
      }
   }

   private static void walk(MinecraftServer s, Kingdoms.K o, List<MobEntity> group, boolean welcome) {
      final ServerWorld w = o.world;
      final BlockPos to = o.capital;
      Tasks.add(new Tasks.Task() {
         int t;

         @Override
         public boolean tick() {
            if (++this.t % 20 != 0) {
               return false;
            } else {
               boolean near = false;
               boolean any = false;

               for (MobEntity m : group) {
                  if (m.isAlive()) {
                     any = true;
                     double dx = m.getX() - to.getX();
                     double dz = m.getZ() - to.getZ();
                     if (dx * dx + dz * dz < 256.0) {
                        near = true;
                     } else {
                        m.getNavigation().startMovingTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5, 0.9);
                     }
                  }
               }

               if (any && !near && this.t < 1800) {
                  return false;
               } else if (any && o.alive && o.world == w) {
                  if (w.shouldTickEntity(to)) {
                     for (MobEntity mx : group) {
                        if (mx.isAlive() && mx.getWorld() == w) {
                           double dx = mx.getX() - to.getX();
                           double dz = mx.getZ() - to.getZ();
                           if (!(dx * dx + dz * dz < 576.0)) {
                              BlockPos g = Fx.surface(w, to.getX() + w.getRandom().nextInt(9) - 4, to.getZ() + w.getRandom().nextInt(9) - 4);
                              mx.refreshPositionAndAngles(g.getX() + 0.5, g.getY(), g.getZ() + 0.5, 0.0F, 0.0F);
                           }
                        }
                     }
                  }

                  Chronicle.say(s, o, welcome ? "приняло пришлых радушно: население растёт" : "приняло пришлых без радости: лишние рты, растёт недовольство");
                  return true;
               } else {
                  return true;
               }
            }
         }
      });
   }

   private static boolean raid(MinecraftServer s, Kingdoms.K k, Civ.City c, Random r) {
      Kingdoms.K t = null;
      int best = 9;

      for (Kingdoms.K o : Kingdoms.K.values()) {
         if (o != k && o.alive && o.world == k.world && o.capital != null && !Diplomacy.allied(k, o) && !Diplomacy.hostile(k, o)) {
            int f = Civ.city(o).food;
            if (f > best) {
               best = f;
               t = o;
            }
         }
      }

      if (t == null) {
         return false;
      } else {
         Civ.City tc = Civ.city(t);
         int loot = Math.min(tc.food / 4, 20);
         tc.food -= loot;
         c.food += loot;
         Diplomacy.op(k, t, -Math.max(0, Math.min(60, Config.i("events.raid.opinion", 30))));
         Chronicle.say(s, k, "— голод толкнул на набег за едой: " + t.title + " лишается припасов (" + loot + " еды)");
         if (Diplomacy.auto && !Kingdoms.eternalPeace && Diplomacy.rel(k, t) == 0 && (Diplomacy.OP[k.ordinal()][t.ordinal()] <= -40 || r.nextInt(2) == 0)) {
            Diplomacy.declare(s, k, t);
         }

         return true;
      }
   }

   private static void emigrate(MinecraftServer s, Kingdoms.K k, ServerWorld w) {
      if (Colonies.on && Perf.go && Conquest.towns(k).size() < 8 && villagers(k).size() >= 10) {
         Colonies.Site st = Colonies.site(w, k, 70, 150);
         List<MobEntity> g = st == null ? List.of() : Colonies.settlers(k, 4);
         if (st != null && g.size() >= 2 && (!st.sea() || Ships.on && Ships.colonize(w, k, st.pos(), g))) {
            Chronicle.say(s, k, "— из-за тесноты после бума рождаемости часть жителей уходит основать новый город");
            if (!st.sea()) {
               Colonies.march(w, k, st.pos(), g);
            }

            return;
         }
      }

      leave(s, k, 2 + w.getRandom().nextInt(2), false);
   }

   private static void festival(MinecraftServer s, Kingdoms.K k, Civ.City c) {
      start(k, Events.Cond.FESTIVAL, 2);
      if (c.religion >= 0) {
         int n = 0;

         for (Kingdoms.K o : Kingdoms.K.values()) {
            if (o != k && o.alive && o.world == k.world && !Diplomacy.hostile(k, o) && Civ.city(o).religion == c.religion) {
               Diplomacy.op(k, o, 8);
               n++;
            }
         }

         if (n > 0) {
            Chronicle.say(s, k, "— праздник сблизил единоверцев: соседи той же веры смотрят теплее");
         }
      }
   }

   private static void pilgrimage(MinecraftServer s, Kingdoms.K k, Civ.City c, Random r) {
      if (c.religion >= 0) {
         Kingdoms.K t = null;
         int best = Integer.MIN_VALUE;

         for (Kingdoms.K o : Kingdoms.K.values()) {
            if (o != k && o.alive && o.world == k.world && o.capital != null && !Diplomacy.hostile(k, o) && Civ.city(o).religion == c.religion) {
               int op = Diplomacy.OP[k.ordinal()][o.ordinal()];
               if (op > best) {
                  best = op;
                  t = o;
               }
            }
         }

         if (t != null) {
            Civ.City tc = Civ.city(t);
            Diplomacy.op(k, t, 10);
            c.faith = Math.min(999, c.faith + 5);
            tc.faith = Math.min(999, tc.faith + 5);
            Chronicle.say(s, k, "— после праздника паломники отправились к единоверцам: " + t.title);
            if (Diplomacy.auto && Diplomacy.rel(k, t) == 0 && Diplomacy.OP[k.ordinal()][t.ordinal()] >= 40 && r.nextInt(3) == 0) {
               Diplomacy.set(k, t, 2);
               Diplomacy.broadcast(
                  s, Text.literal("\ud83e\udd1d " + k.title + " и " + t.title + " заключили союз!").formatted(Formatting.GREEN, Formatting.BOLD)
               );
            }
         }
      }
   }

   private static void heroStep(MinecraftServer s, Kingdoms.K k, ServerWorld w) {
      UUID id = HERO.get(k);
      if (id != null) {
         int age = HERO_AGE.merge(k, 1, Integer::sum);
         String name = HERO_NAME.getOrDefault(k, "");
         MobEntity h = HERO_REF.get(k);
         boolean dead = h != null && (h.isDead() || h.getRemovalReason() == RemovalReason.KILLED);
         if (!dead && h != null && h.getRemovalReason() == RemovalReason.DISCARDED) {
            forget(k);
         } else {
            if (!dead && (h == null || h.isRemoved())) {
               h = w.getEntity(id) instanceof MobEntity m ? m : null;
               if (h == null) {
                  HERO_REF.remove(k);
                  if (age > 40) {
                     forget(k);
                  }

                  return;
               }

               HERO_REF.put(k, h);
               dead = h.isDead();
            }

            if (dead) {
               forget(k);
               end(k, Events.Cond.GOLDEN_AGE);
               start(k, Events.Cond.MOURNING, Config.i("events.mourning.length", 3));
               Unrest.add(k, 8);
               Chronicle.say(s, k, "— гибель героя " + name + " повергла народ в траур: растёт смута");
            } else if (Kingdoms.of(h) != k) {
               forget(k);
            } else {
               if (Heroes.KILLS.getOrDefault(id, 0) >= Math.max(1, Config.i("events.hero.kills", 5))) {
                  forget(k);
                  end(k, Events.Cond.MOURNING);
                  start(k, Events.Cond.GOLDEN_AGE, Config.i("events.golden.length", 5));
                  Unrest.add(k, -10);
                  Chronicle.say(s, k, "— победы героя " + name + " воодушевили народ: начался золотой век");
               } else if (age > 40) {
                  forget(k);
               }
            }
         }
      }
   }

   private static void forget(Kingdoms.K k) {
      HERO.remove(k);
      HERO_NAME.remove(k);
      HERO_AGE.remove(k);
      HERO_REF.remove(k);
   }

   static void fire(MinecraftServer s, Kingdoms.K k, int e) {
      ServerWorld w = k.world;
      Civ.City c = Civ.city(k);
      Random r = w.getRandom();
      BlockPos cap = k.capital;
      List<VillagerEntity> vill = new ArrayList<>();

      for (MobEntity m : k.members) {
         if (m instanceof VillagerEntity v && v.isAlive()) {
            vill.add(v);
         }
      }

      switch (e) {
         case 0:
            c.food += 30;
            Fx.particles(w, ParticleTypes.HAPPY_VILLAGER, cap.getX() + 0.5, cap.getY() + 2, cap.getZ() + 0.5, 80, 6.0, 2.0, 6.0, 0.1);
            Chronicle.say(s, k, "— урожайный год! Амбары полны (+30 еды)");
            if (!has(k, Events.Cond.FAMINE) && !has(k, Events.Cond.BOOM)) {
               start(k, Events.Cond.ABUNDANCE, 1);
            }
            break;
         case 1: {
            int n = Math.min(c.food, 20);
            c.food -= n;
            Fx.particles(w, ParticleTypes.WHITE_ASH, cap.getX() + 0.5, cap.getY() + 3, cap.getZ() + 0.5, 120, 8.0, 3.0, 8.0, 0.02);
            Chronicle.say(s, k, "постигла засуха (−" + n + " еды)");
            end(k, Events.Cond.ABUNDANCE);
            start(k, Events.Cond.DROUGHT, (int)Math.round(Config.i("events.drought.length", 4) * Seasons.severity(k)));
            break;
         }
         case 2:
            c.ore += 15;
            Chronicle.say(s, k, "— шахтёры нашли золотую жилу (+15 руды)");
            if (!has(k, Events.Cond.GOLD_RUSH)) {
               start(k, Events.Cond.LODE, 1);
            }
            break;
         case 3: {
            int n = 2 + r.nextInt(3);
            double a = r.nextDouble() * Math.PI * 2.0;
            BlockPos at = Civ.ground(w, cap.getX() + (int)(Math.cos(a) * 26.0), cap.getZ() + (int)(Math.sin(a) * 26.0));

            for (int ixx = 0; ixx < n; ixx++) {
               VindicatorEntity bx = EntityType.VINDICATOR.spawn(w, at.add(r.nextInt(3) - 1, 0, r.nextInt(3) - 1), SpawnReason.EVENT);
               if (bx != null) {
                  bx.setCustomName(Text.literal("Разбойник").formatted(Formatting.DARK_GRAY));
                  bx.addCommandTag("gbraider");
               }
            }

            w.playSound(null, at, SoundEvents.EVENT_RAID_HORN.value(), SoundCategory.HOSTILE, 4.0F, 1.0F);
            Chronicle.say(s, k, "атаковано бандой разбойников (" + n + ")!");
            break;
         }
         case 4: {
            if (vill.size() < 6) {
               fire(s, k, 0);
               return;
            }

            int n = 1 + r.nextInt(3);
            MobEntity king = Life.king(k);
            int dead = 0;

            for (int ix = 0; ix < 10 && dead < n; ix++) {
               VillagerEntity v = vill.get(r.nextInt(vill.size()));
               if (v != king && v.isAlive()) {
                  Fx.particles(w, ParticleTypes.SNEEZE, v.getX(), v.getY() + 1.5, v.getZ(), 30, 0.4, 0.4, 0.4, 0.05);
                  v.kill();
                  dead++;
               }
            }

            Chronicle.say(s, k, "поражено мором: умерло жителей — " + dead);
            Plague.seed(k, "мор");
            break;
         }
         case 5:
            for (VillagerEntity v : vill) {
               v.setHealth(v.getMaxHealth());
            }

            c.faith = Math.min(999, c.faith + 15);

            for (int ix = 0; ix < 6; ix++) {
               Tasks.delay(
                  ix * 8,
                  () -> {
                     double xx = cap.getX() + r.nextInt(17) - 8;
                     double zx = cap.getZ() + r.nextInt(17) - 8;
                     Fx.particles(w, ParticleTypes.FIREWORK, xx, cap.getY() + 14 + r.nextInt(6), zx, 70, 0.2, 0.2, 0.2, 0.25);
                     w.playSound(
                        null, BlockPos.ofFloored(xx, cap.getY() + 14, zx), SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.AMBIENT, 4.0F, 1.0F
                     );
                  }
               );
            }

            Chronicle.say(s, k, "празднует великий праздник!");
            festival(s, k, c);
            break;
         case 6: {
            MobEntity h = Kingdoms.spawn(k, EntityType.VINDICATOR, Civ.ground(w, cap.getX() + 2, cap.getZ() + 2));
            if (h == null) {
               return;
            }

            EntityAttributeInstance hp = h.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (hp != null) {
               hp.setBaseValue(80.0);
            }

            h.setHealth(h.getMaxHealth());
            h.setGlowing(true);
            h.addCommandTag("gbhero");
            String n = Life.name(h);
            h.setCustomName(Text.literal("Герой " + n).formatted(Formatting.GOLD));
            h.setCustomNameVisible(true);
            Fx.particles(w, ParticleTypes.TOTEM_OF_UNDYING, h.getX(), h.getY() + 1.0, h.getZ(), 60, 0.5, 1.0, 0.5, 0.4);
            Chronicle.say(s, k, "— родился герой " + n + "!");
            HERO.put(k, h.getUuid());
            HERO_NAME.put(k, n);
            HERO_AGE.put(k, 0);
            HERO_REF.put(k, h);
            break;
         }
         case 7:
            List<Civ.Building> bs = new ArrayList<>();

            for (Civ.Building b : c.buildings) {
               if (b.done && b.kind != Civ.Kind.QUARRY && b.kind != Civ.Kind.FARM && b.kind != Civ.Kind.WELL) {
                  bs.add(b);
               }
            }

            if (bs.isEmpty() || !w.getGameRules().getBoolean(GameRules.DO_FIRE_TICK)) {
               fire(s, k, 2);
               return;
            }

            Civ.Building bx = bs.get(r.nextInt(bs.size()));

            for (int i = 0; i < 4; i++) {
               int x = bx.x0 + r.nextInt(bx.x1 - bx.x0 + 1);
               int z = bx.z0 + r.nextInt(bx.z1 - bx.z0 + 1);
               BlockPos p = new BlockPos(x, w.getTopY(Type.WORLD_SURFACE, x, z), z);
               if (w.getBlockState(p).isAir()) {
                  w.setBlockState(p, Blocks.FIRE.getDefaultState(), 3);
               }
            }

            Chronicle.say(s, k, "— пожар! Горит " + bx.kind.title.toLowerCase());
            start(k, Events.Cond.REBUILD, Config.i("events.rebuild.length", 3));
            break;
         default:
            c.ore += 5;
            c.faith = Math.min(999, c.faith + 20);
            Fx.particles(w, ParticleTypes.ENCHANT, cap.getX() + 0.5, cap.getY() + 2, cap.getZ() + 0.5, 120, 3.0, 2.0, 3.0, 1.0);
            Chronicle.say(s, k, "— мудрецы сделали великое открытие");
      }
   }

   public static void clear() {
      timer = 0;
      reset();
   }

   public static enum Cond {
      FAMINE("Голод", 'c'),
      DROUGHT("Засуха", 'e'),
      MOURNING("Траур", '8'),
      REBUILD("Восстановление после пожара", '6'),
      GOLDEN_AGE("Золотой век", '6'),
      GOLD_RUSH("Золотая лихорадка", 'e'),
      BOOM("Бум рождаемости", 'a'),
      FESTIVAL("Праздник", 'd'),
      ABUNDANCE("Изобилие", 'a'),
      LODE("Золотая жила", 'e');

      public final String title;
      final char color;

      private Cond(String title, char color) {
         this.title = title;
         this.color = color;
      }
   }

   private static final class St {
      int left;
      int stage;
      boolean fresh = true;

      St(int left, int stage) {
         this.left = left;
         this.stage = stage;
      }
   }
}
