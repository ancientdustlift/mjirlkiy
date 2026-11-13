package com.vladik.godbox;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

public final class Treaties {
   public static boolean on = true;
   static boolean union;
   static final String CLAIM = "gbclaim:";
   private static final int N = Kingdoms.K.values().length;
   private static final String[] PLACES = new String[]{
      "Старом Броду",
      "Белокамне",
      "Ольховой Роще",
      "Трёх Дубах",
      "Красном Холме",
      "Тихой Заводи",
      "Вороньем Камне",
      "Медвежьем Логу",
      "Соколиной Горе",
      "Ясном Поле",
      "Сером Мосту",
      "Золотом Ключе",
      "Волчьей Пади",
      "Дальнем Плёсе",
      "Чёрном Бору",
      "Светлом Яру"
   };
   private static final Treaties.War[][] WARS = new Treaties.War[N][N];
   private static final int[][] TRUCE = new int[N][N];
   private static final int[][] PAY = new int[N][N];
   private static final int[][] PCT = new int[N][N];
   private static final Map<Kingdoms.K, Integer> EXH = new EnumMap<>(Kingdoms.K.class);
   private static final Map<Kingdoms.K, Integer> PREV = new EnumMap<>(Kingdoms.K.class);
   private static final List<Treaties.Marriage> MARRIAGES = new ArrayList<>();
   private static int t;

   private Treaties() {
   }

   public static void init() {
   }

   private static Treaties.War war(Kingdoms.K a, Kingdoms.K b) {
      return WARS[Math.min(a.ordinal(), b.ordinal())][Math.max(a.ordinal(), b.ordinal())];
   }

   static int exhaustion(Kingdoms.K k) {
      return EXH.getOrDefault(k, 0);
   }

   private static void tire(Kingdoms.K k, int n) {
      EXH.put(k, MathHelper.clamp(exhaustion(k) + n, 0, 100));
   }

   public static boolean truce(Kingdoms.K a, Kingdoms.K b) {
      return on && a != null && b != null && a != b && TRUCE[a.ordinal()][b.ordinal()] > t;
   }

   private static void setTruce(Kingdoms.K a, Kingdoms.K b, int until) {
      TRUCE[a.ordinal()][b.ordinal()] = TRUCE[b.ordinal()][a.ordinal()] = until;
   }

   private static int mins(int ticks) {
      return Math.max(1, ticks / 1200);
   }

   private static boolean ticking(Kingdoms.K k) {
      return k.alive && k.world != null && k.capital != null && k.world.shouldTickEntity(k.capital);
   }

   private static int points(Treaties.War w, boolean sideA, Kingdoms.K enemy) {
      return (sideA ? w.lossB : w.lossA) * 2 + (sideA ? w.townsA : w.townsB) * 12 + exhaustion(enemy) / 5;
   }

   public static void tick(MinecraftServer s) {
      if (!s.getTickManager().isFrozen()) {
         if (Perf.go) {
            t++;
            if (on) {
               if (t % 20 == 0) {
                  sync();
               }

               if (t % 100 == 0) {
                  losses();
               }

               if (t % 600 == 0) {
                  weariness(s);
                  expire(s);
                  marriages(s);
               }
            }
         }
      }
   }

   private static void sync() {
      Kingdoms.K[] ks = Kingdoms.K.values();

      for (int i = 0; i < N; i++) {
         for (int j = i + 1; j < N; j++) {
            boolean h = ks[i].alive && ks[j].alive && Diplomacy.hostile(ks[i], ks[j]);
            if (h && WARS[i][j] == null) {
               Treaties.War w = new Treaties.War();
               w.start = t;
               WARS[i][j] = w;
               TRUCE[i][j] = TRUCE[j][i] = 0;
               PAY[i][j] = PAY[j][i] = 0;
            } else if (!h && WARS[i][j] != null) {
               WARS[i][j] = null;
            }
         }
      }
   }

   private static void losses() {
      for (Kingdoms.K k : Kingdoms.K.values()) {
         if (!ticking(k)) {
            PREV.remove(k);
         } else {
            int n = Vassals.pop(k);
            Integer p = PREV.put(k, n);
            if (p != null && n < p) {
               int d = p - n;
               if (p <= 6 || d * 10 <= p * 6) {
                  List<Treaties.War> ws = new ArrayList<>();
                  List<Boolean> side = new ArrayList<>();

                  for (Kingdoms.K o : Kingdoms.K.values()) {
                     if (o != k && o.alive) {
                        Treaties.War w = war(k, o);
                        if (w != null) {
                           ws.add(w);
                           side.add(k.ordinal() < o.ordinal());
                        }
                     }
                  }

                  if (!ws.isEmpty()) {
                     for (int i = 0; i < ws.size(); i++) {
                        int part = d / ws.size() + (i < d % ws.size() ? 1 : 0);
                        if (side.get(i)) {
                           ws.get(i).lossA += part;
                        } else {
                           ws.get(i).lossB += part;
                        }
                     }

                     tire(k, d * Config.i("treaties.exhaustionPerLoss", 2));
                  }
               }
            }
         }
      }
   }

   static void captured(Kingdoms.K a, Kingdoms.K d) {
      if (on && a != null && d != null && a != d) {
         PREV.remove(d);
         Treaties.War w = war(a, d);
         if (w != null) {
            if (a.ordinal() < d.ordinal()) {
               w.townsA++;
            } else {
               w.townsB++;
            }

            tire(d, 8);
         }
      }
   }

   private static void weariness(MinecraftServer s) {
      for (Kingdoms.K k : Kingdoms.K.values()) {
         if (!k.alive) {
            EXH.remove(k);
         } else {
            tire(k, Diplomacy.atWar(k) ? Config.i("treaties.exhaustionPerInterval", 2) : -4);
            int e = exhaustion(k);
            if (e >= 40) {
               Unrest.add(k, e >= 70 ? 2 : 1);
            }
         }
      }

      if (Diplomacy.auto && !Kingdoms.eternalPeace) {
         int limit = Config.i("treaties.exhaustionPeace", 80);
         Kingdoms.K[] ks = Kingdoms.K.values();

         for (int i = 0; i < N; i++) {
            for (int j = i + 1; j < N; j++) {
               Treaties.War w = WARS[i][j];
               if (w != null && t - w.start >= 2400 && Diplomacy.hostile(ks[i], ks[j]) && Math.max(exhaustion(ks[i]), exhaustion(ks[j])) >= limit) {
                  conclude(s, ks[i], ks[j]);
               }
            }
         }
      }
   }

   private static void expire(MinecraftServer s) {
      Kingdoms.K[] ks = Kingdoms.K.values();

      for (int i = 0; i < N; i++) {
         for (int j = 0; j < N; j++) {
            if (TRUCE[i][j] != 0 && TRUCE[i][j] <= t) {
               TRUCE[i][j] = 0;
            }

            if (PAY[i][j] != 0 && PAY[i][j] <= t) {
               PAY[i][j] = 0;
               if (ks[i].alive && ks[j].alive) {
                  Chronicle.say(s, ks[i], "выплатило дань по договору — «" + ks[j].title + "» больше ничего не получает");
               }
            }
         }
      }
   }

   static boolean peace(MinecraftServer s, Kingdoms.K a, Kingdoms.K b) {
      if (!on) {
         return false;
      } else {
         conclude(s, a, b);
         return true;
      }
   }

   static void godPeace(MinecraftServer s) {
      godPeaceTerms(s);
   }

   static boolean[][] godPeaceTerms(MinecraftServer s) {
      boolean[][] done = new boolean[N][N];
      if (!on) {
         return done;
      } else {
         Kingdoms.K[] ks = Kingdoms.K.values();

         for (int i = 0; i < N; i++) {
            for (int j = i + 1; j < N; j++) {
               if (WARS[i][j] != null && ks[i].alive && ks[j].alive && Diplomacy.hostile(ks[i], ks[j])) {
                  conclude(s, ks[i], ks[j]);
                  done[i][j] = true;
               }
            }
         }

         return done;
      }
   }

   private static boolean areaLoaded(ServerWorld w, BlockPos p, int r) {
      for (int dx = -r; dx <= r; dx += r) {
         for (int dz = -r; dz <= r; dz += r) {
            if (!w.isChunkLoaded(p.add(dx, 0, dz))) {
               return false;
            }
         }
      }

      return true;
   }

   private static BlockPos cedable(Kingdoms.K win, Kingdoms.K lose) {
      if (Conquest.on && lose.world != null && win.capital != null) {
         List<BlockPos> towns = Conquest.TOWNS.get(lose);
         if (towns == null) {
            return null;
         } else {
            BlockPos best = null;
            double bd = Double.MAX_VALUE;

            for (BlockPos p : towns) {
               double d = p.getSquaredDistance(win.capital);
               if (d < bd && areaLoaded(lose.world, p, 45)) {
                  bd = d;
                  best = p;
               }
            }

            return best;
         }
      } else {
         return null;
      }
   }

   private static String join(List<String> parts) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < parts.size(); i++) {
         if (i > 0) {
            sb.append(i == parts.size() - 1 ? " и " : ", ");
         }

         sb.append(parts.get(i));
      }

      return sb.toString();
   }

   private static void conclude(MinecraftServer s, Kingdoms.K a, Kingdoms.K b) {
      Kingdoms.K x = a.ordinal() < b.ordinal() ? a : b;
      Kingdoms.K y = x == a ? b : a;
      int i = x.ordinal();
      int j = y.ordinal();
      Treaties.War w = WARS[i][j];
      if (w == null) {
         w = new Treaties.War();
      }

      WARS[i][j] = null;
      int sx = points(w, true, y);
      int sy = points(w, false, x);
      int total = sx + sy;
      double ratio = total < 6 ? 0.0 : (double)Math.abs(sx - sy) / total;
      Kingdoms.K win = sx >= sy ? x : y;
      Kingdoms.K lose = win == x ? y : x;
      int tier = ratio < Config.d("treaties.whiteMargin", 0.2)
         ? 0
         : (ratio < Config.d("treaties.cessionMargin", 0.45) ? 1 : (ratio < Config.d("treaties.crushMargin", 0.7) ? 2 : 3));
      int trait = Life.kingTrait(win);
      if (tier > 0) {
         if (trait == 0) {
            tier--;
         } else if (trait == 1 && tier < 3) {
            tier++;
         }
      }

      double mult = 1.0;
      if (trait == 2 && tier == 2) {
         tier = 1;
         mult = 1.5;
      }

      Random r = win.world != null ? win.world.getRandom() : Random.create();
      String place = PLACES[r.nextInt(PLACES.length)];
      int truce = Config.i("treaties.truceTicks", 24000);
      Diplomacy.set(x, y, 0);
      if (tier == 0) {
         Diplomacy.OP[i][j] = Diplomacy.OP[j][i] = 0;
         truce /= 2;
         setTruce(x, y, t + truce);
         finish(x, y);
         Diplomacy.broadcast(
            s,
            Text.literal(
                  "\ud83d\udcdc Перемирие в "
                     + place
                     + ": "
                     + x.title
                     + " и "
                     + y.title
                     + " расходятся без победителя — белый мир. Перемирие: "
                     + mins(truce)
                     + " мин"
               )
               .formatted(Formatting.WHITE)
         );
      } else {
         List<String> parts = new ArrayList<>();
         boolean vassal = false;
         if (tier == 3) {
            if (Vassals.on && !Vassals.LORD.containsKey(lose) && Vassals.LORD.get(win) != lose) {
               vassal = true;
            } else {
               tier = 2;
            }
         }

         BlockPos town = tier == 2 ? cedable(win, lose) : null;
         if (tier == 2 && town == null) {
            mult *= 1.5;
         }

         if (!vassal) {
            if (Economy.on) {
               int want = (int)((Config.i("treaties.goldBase", 40) + ratio * Config.i("treaties.goldScale", 160)) * mult);
               int paid = Math.min(want, Economy.gold(lose));
               if (paid > 0) {
                  Economy.add(lose, -paid);
                  Economy.add(win, paid);
                  parts.add("выплачивает " + paid + " золота");
               }

               int pct = MathHelper.clamp((int)(Config.i("treaties.tributeShare", 25) * mult), 0, 60);
               int len = (int)(Config.i("treaties.tributeTicks", 12000) * mult);
               if (pct > 0 && len > 0) {
                  PAY[lose.ordinal()][win.ordinal()] = t + len;
                  PCT[lose.ordinal()][win.ordinal()] = pct;
                  PAY[win.ordinal()][lose.ordinal()] = 0;
                  parts.add("отдаёт " + pct + "% дохода (" + mins(len) + " мин)");
               }
            } else if (lose.alive && win.alive) {
               Civ.City cl = Civ.city(lose);
               Civ.City cw = Civ.city(win);
               double share = Math.min(0.5, (0.2 + ratio * 0.3) * mult);
               int wd = (int)(cl.wood * share);
               int st = (int)(cl.stone * share);
               int fd = (int)(cl.food * share);
               int or = (int)(cl.ore * share);
               cl.wood -= wd;
               cw.wood += wd;
               cl.stone -= st;
               cw.stone += st;
               cl.food -= fd;
               cw.food += fd;
               cl.ore -= or;
               cw.ore += or;
               if (wd + st + fd + or > 0) {
                  parts.add("выплачивает контрибуцию со складов (дерево " + wd + ", камень " + st + ", еда " + fd + ", руда " + or + ")");
               }
            }

            if (town != null) {
               Conquest.takeTown(s, win, lose, town);
               parts.add("уступает город «" + Colonies.name(town) + "»");
            }

            int o = tier == 2 ? -25 : -15;
            Diplomacy.OP[i][j] = Diplomacy.OP[j][i] = o;
         } else {
            Vassals.submit(s, lose, win);
            parts.add("становится вассалом");
         }

         setTruce(x, y, t + truce);
         finish(x, y);
         String body = parts.isEmpty() ? "признаёт поражение" : join(parts);
         Diplomacy.broadcast(
            s,
            Text.literal(
                  "\ud83d\udcdc "
                     + (vassal ? "Капитуляция в " : "Мир в ")
                     + place
                     + ": "
                     + win.title
                     + " побеждает; "
                     + lose.title
                     + " "
                     + body
                     + ". Перемирие: "
                     + mins(truce)
                     + " мин"
               )
               .formatted(win.color)
         );
      }
   }

   private static void finish(Kingdoms.K x, Kingdoms.K y) {
      EXH.put(x, exhaustion(x) / 2);
      EXH.put(y, exhaustion(y) / 2);
      Kingdoms.calm();
   }

   static int income(Kingdoms.K k, int income) {
      if (on && income > 0) {
         int i = k.ordinal();
         int out = income;

         for (Kingdoms.K o : Kingdoms.K.values()) {
            int j = o.ordinal();
            if (PAY[i][j] > t && o.alive) {
               int cut = Math.min(out, income * PCT[i][j] / 100);
               if (cut > 0) {
                  out -= cut;
                  Economy.add(o, cut);
               }
            }
         }

         return out;
      } else {
         return income;
      }
   }

   private static List<Treaties.Marriage> marriagesOf(Kingdoms.K k) {
      List<Treaties.Marriage> out = new ArrayList<>();

      for (Treaties.Marriage m : MARRIAGES) {
         if (m.a == k || m.b == k) {
            out.add(m);
         }
      }

      return out;
   }

   private static boolean married(Kingdoms.K a, Kingdoms.K b) {
      for (Treaties.Marriage m : MARRIAGES) {
         if (m.a == a && m.b == b || m.a == b && m.b == a) {
            return true;
         }
      }

      return false;
   }

   private static boolean living(Kingdoms.K k, String uuid) {
      for (MobEntity m : k.members) {
         if (m.isAlive() && m.getUuidAsString().equals(uuid)) {
            return true;
         }
      }

      return false;
   }

   private static boolean claimant(Kingdoms.K in, Kingdoms.K other, int house) {
      String want = other.name() + ":" + house;

      for (MobEntity m : in.members) {
         if (m.isAlive() && want.equals(Life.tag(m, "gbclaim:"))) {
            return true;
         }
      }

      return false;
   }

   private static void flourish(Kingdoms.K k) {
      ServerWorld w = k.world;
      BlockPos c = k.capital;
      if (w != null && c != null && w.isChunkLoaded(c)) {
         Fx.particles(w, ParticleTypes.FIREWORK, c.getX() + 0.5, c.getY() + 7, c.getZ() + 0.5, 90, 4.0, 3.0, 4.0, 0.2);
         Fx.particles(w, ParticleTypes.HEART, c.getX() + 0.5, c.getY() + 3, c.getZ() + 0.5, 20, 3.0, 1.5, 3.0, 0.02);
         Fx.sound(w, c.getX(), c.getY(), c.getZ(), SoundEvents.BLOCK_BELL_USE, 4.0F, 1.2F);
         Fx.sound(w, c.getX(), c.getY(), c.getZ(), SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 3.0F, 1.0F);
      }
   }

   private static String marry(MinecraftServer s, Kingdoms.K a, Kingdoms.K b) {
      MobEntity ka = Kings.king(a);
      MobEntity kb = Kings.king(b);
      if (ka == null) {
         return "У королевства «" + a.title + "» нет правителя";
      } else if (kb == null) {
         return "У королевства «" + b.title + "» нет правителя";
      } else if (married(a, b)) {
         return a.title + " и " + b.title + " уже связаны браком";
      } else {
         Dynasty.House ha = Dynasty.house(a);
         Dynasty.House hb = Dynasty.house(b);
         if (ha.id != 0 && hb.id != 0) {
            Treaties.Marriage m = new Treaties.Marriage();
            m.a = a;
            m.b = b;
            m.ha = ha.id;
            m.hb = hb.id;
            m.ua = ka.getUuidAsString();
            m.ub = kb.getUuidAsString();
            m.since = t;
            MARRIAGES.add(m);
            if (Diplomacy.hostile(a, b)) {
               Diplomacy.set(a, b, 0);
               WARS[Math.min(a.ordinal(), b.ordinal())][Math.max(a.ordinal(), b.ordinal())] = null;
               Kingdoms.calm();
            }

            Diplomacy.op(a, b, Config.i("treaties.marriageOpinion", 40));
            boolean ally = Diplomacy.allied(a, b);
            if (!ally && Diplomacy.OP[a.ordinal()][b.ordinal()] >= 50) {
               Diplomacy.set(a, b, 2);
               ally = true;
            }

            int pact = Config.i("treaties.truceTicks", 24000) * 2;
            if (!ally) {
               setTruce(a, b, Math.max(TRUCE[a.ordinal()][b.ordinal()], t + pact));
            }

            flourish(a);
            flourish(b);
            Diplomacy.broadcast(
               s,
               Text.literal(
                     "\ud83d\udc8d Королевский брак: дом "
                        + ha.name
                        + " («"
                        + a.title
                        + "») и дом "
                        + hb.name
                        + " («"
                        + b.title
                        + "») породнились — "
                        + (ally ? "заключён союз" : "заключён пакт о ненападении на " + mins(pact) + " мин")
                        + ", их дети унаследуют права на оба трона"
                  )
                  .formatted(Formatting.LIGHT_PURPLE)
            );
            return null;
         } else {
            return "Правящие дома ещё не основаны";
         }
      }
   }

   private static void marriages(MinecraftServer s) {
      for (Treaties.Marriage m : new ArrayList<>(MARRIAGES)) {
         if (!m.a.alive || !m.b.alive) {
            MARRIAGES.remove(m);
         } else if (Dynasty.house(m.a).id != m.ha || Dynasty.house(m.b).id != m.hb) {
            MARRIAGES.remove(m);
            Chronicle.say(s, m.a, "династия сменилась — брачный союз с «" + m.b.title + "» расторгнут");
         } else if (ticking(m.a) && ticking(m.b) && !living(m.a, m.ua) && !living(m.b, m.ub) && !claimant(m.a, m.b, m.hb) && !claimant(m.b, m.a, m.ha)) {
            MARRIAGES.remove(m);
            Chronicle.say(s, m.a, "брачный союз с «" + m.b.title + "» угас: супруги умерли, общих наследников нет");
         }
      }

      if (Diplomacy.auto) {
         Kingdoms.K[] ks = Kingdoms.K.values();
         int minOp = Config.i("treaties.marriageMinOpinion", 25);
         int base = Config.i("treaties.marriageChance", 4);

         for (int i = 0; i < N; i++) {
            for (int j = i + 1; j < N; j++) {
               Kingdoms.K a = ks[i];
               Kingdoms.K b = ks[j];
               if (ticking(a)
                  && ticking(b)
                  && a.world == b.world
                  && !Diplomacy.hostile(a, b)
                  && Diplomacy.OP[i][j] >= minOp
                  && !married(a, b)
                  && marriagesOf(a).size() < 2
                  && marriagesOf(b).size() < 2
                  && Kings.king(a) != null
                  && Kings.king(b) != null) {
                  int chance = base;
                  int ta = Life.kingTrait(a);
                  int tb = Life.kingTrait(b);
                  if (ta == 0 || ta == 2) {
                     chance = base + 3;
                  }

                  if (tb == 0 || tb == 2) {
                     chance += 3;
                  }

                  if (ta == 1) {
                     chance -= 2;
                  }

                  if (tb == 1) {
                     chance -= 2;
                  }

                  int ra = Civ.city(a).religion;
                  int rb = Civ.city(b).religion;
                  if (ra >= 0 && ra == rb) {
                     chance += 3;
                  }

                  int pa = Vassals.pop(a);
                  int pb = Vassals.pop(b);
                  if (pa * 2 <= pb * 3 && pb * 2 <= pa * 3) {
                     chance += 2;
                  }

                  if (a.world.getRandom().nextInt(100) < chance && marry(s, a, b) == null) {
                     return;
                  }
               }
            }
         }
      }
   }

   static void born(Kingdoms.K k, VillagerEntity child) {
      if (on && k != null) {
         int hk = Dynasty.house(k).id;

         for (Treaties.Marriage m : MARRIAGES) {
            if ((m.a == k || m.b == k) && m.house(k) == hk) {
               Kingdoms.K o = m.other(k);
               if (o.alive && Dynasty.house(o).id == m.house(o)) {
                  Life.setTag(child, "gbclaim:", o.name() + ":" + m.house(o));
                  if (k.world != null) {
                     Civ.tell(
                        k.world,
                        Text.literal(
                              "\ud83d\udc8d "
                                 + k.title
                                 + ": "
                                 + Dynasty.first(child)
                                 + " несёт кровь двух домов — "
                                 + Dynasty.houseName(k)
                                 + " и "
                                 + Dynasty.houseName(o)
                           )
                           .formatted(Formatting.LIGHT_PURPLE)
                     );
                  }

                  return;
               }
            }
         }
      }
   }

   static boolean succession(MinecraftServer s, Kingdoms.K k, ServerWorld w) {
      if (!on) {
         return false;
      } else {
         int hk = Dynasty.house(k).id;
         List<Kingdoms.K> claim = new ArrayList<>();
         List<Treaties.Marriage> mine = marriagesOf(k);

         for (Treaties.Marriage m : mine) {
            Kingdoms.K p = m.other(k);
            if (p.alive && p.world == w && m.house(k) == hk && Dynasty.house(p).id == m.house(p)) {
               MobEntity pk = Kings.king(p);
               if (pk != null && (pk.getUuidAsString().equals(m.ruler(p)) || claimant(p, k, hk))) {
                  claim.add(p);
               }
            }
         }

         if (claim.isEmpty()) {
            return false;
         } else if (claim.size() >= 2) {
            Kingdoms.K c1 = claim.get(0);
            Kingdoms.K c2 = claim.get(1);
            Diplomacy.broadcast(
               s,
               Text.literal(
                     "⚔ Война за наследство: дома " + Dynasty.houseName(c1) + " и " + Dynasty.houseName(c2) + " оспаривают опустевший трон «" + k.title + "»"
                  )
                  .formatted(Formatting.RED, Formatting.BOLD)
            );
            setTruce(c1, c2, 0);
            Diplomacy.declare(s, c1, c2);
            return false;
         } else {
            Kingdoms.K p = claim.get(0);
            MobEntity pk = Kings.king(p);
            int accept = MathHelper.clamp(Config.i("treaties.unionAccept", 65) + Diplomacy.OP[k.ordinal()][p.ordinal()] / 2 - Unrest.unrest(k) / 3, 10, 95);
            if (ticking(k) && w.getRandom().nextInt(100) < accept) {
               String msg = "\ud83d\udc51 Личная уния: трон «"
                  + k.title
                  + "» наследует "
                  + Dynasty.first(pk)
                  + " из дома "
                  + Dynasty.houseName(p)
                  + " — королевство мирно объединяется с «"
                  + p.title
                  + "»";
               union = true;

               boolean ok;
               try {
                  ok = Conquest.annex(s, p, k);
               } finally {
                  union = false;
               }

               if (ok) {
                  Diplomacy.broadcast(s, Text.literal(msg).formatted(p.color, Formatting.BOLD));
                  flourish(p);
                  return true;
               } else {
                  return false;
               }
            } else {
               Diplomacy.broadcast(
                  s,
                  Text.literal(
                        "⚔ Война за наследство: знать «"
                           + k.title
                           + "» отвергла права дома "
                           + Dynasty.houseName(p)
                           + " — «"
                           + p.title
                           + "» идёт добывать трон силой"
                     )
                     .formatted(Formatting.RED, Formatting.BOLD)
               );

               for (Treaties.Marriage mx : mine) {
                  if (mx.other(k) == p) {
                     MARRIAGES.remove(mx);
                  }
               }

               setTruce(k, p, 0);
               Diplomacy.declare(s, p, k);
               return false;
            }
         }
      }
   }

   public static void power(ServerPlayerEntity god, ServerWorld w, BlockPos pos) {
      Kingdoms.K a = null;
      Kingdoms.K b = null;
      double da = Double.MAX_VALUE;
      double db = Double.MAX_VALUE;

      for (Kingdoms.K k : Kingdoms.K.values()) {
         if (k.alive && k.world == w && k.capital != null) {
            double d = k.capital.getSquaredDistance(pos);
            if (d < da) {
               b = a;
               db = da;
               a = k;
               da = d;
            } else if (d < db) {
               b = k;
               db = d;
            }
         }
      }

      if (a == null || b == null) {
         god.sendMessage(Text.literal("Для брака нужно два королевства").formatted(Formatting.RED), true);
      } else if (!on) {
         god.sendMessage(Text.literal("Закон «Договоры и браки» выключен").formatted(Formatting.YELLOW), true);
      } else {
         String err = marry(w.getServer(), a, b);
         if (err != null) {
            god.sendMessage(Text.literal(err).formatted(Formatting.YELLOW), true);
         }
      }
   }

   public static List<String> describe(Kingdoms.K k) {
      List<String> out = new ArrayList<>();
      if (on && k.alive) {
         int i = k.ordinal();
         int e = exhaustion(k);
         if (e > 0) {
            out.add("усталость от войны " + e + "%");
         }

         for (Kingdoms.K o : Kingdoms.K.values()) {
            if (o != k && o.alive) {
               int j = o.ordinal();
               Treaties.War w = war(k, o);
               if (w != null) {
                  boolean sideA = i < j;
                  out.add("война с «" + o.title + "»: счёт " + points(w, sideA, o) + ":" + points(w, !sideA, k));
               }

               if (TRUCE[i][j] > t) {
                  out.add("перемирие с «" + o.title + "» (ещё " + mins(TRUCE[i][j] - t) + " мин)");
               }

               if (PAY[i][j] > t) {
                  out.add("платит дань «" + o.title + "»: " + PCT[i][j] + "% дохода (ещё " + mins(PAY[i][j] - t) + " мин)");
               }

               if (PAY[j][i] > t) {
                  out.add("получает дань от «" + o.title + "»: " + PCT[j][i] + "% дохода (ещё " + mins(PAY[j][i] - t) + " мин)");
               }
            }
         }

         for (Treaties.Marriage m : MARRIAGES) {
            if (m.a == k || m.b == k) {
               Kingdoms.K ox = m.other(k);
               out.add("брак с домом " + Dynasty.houseName(ox) + " («" + ox.title + "»)");
            }
         }

         return out;
      } else {
         return out;
      }
   }

   static String stats(Kingdoms.K k) {
      List<String> l = describe(k);
      return l.isEmpty() ? "" : "§7Договоры: §f" + String.join("§7; §f", l) + "\n";
   }

   static void info(MutableText out) {
      if (on) {
         for (Kingdoms.K k : Kingdoms.K.values()) {
            List<String> l = describe(k);
            if (!l.isEmpty()) {
               out.append(Text.literal("\n  \ud83d\udcdc " + k.title + ": " + String.join("; ", l)).formatted(k.color));
            }
         }
      }
   }

   static void forget(Kingdoms.K k) {
      int i = k.ordinal();

      for (int j = 0; j < N; j++) {
         WARS[Math.min(i, j)][Math.max(i, j)] = null;
         TRUCE[i][j] = TRUCE[j][i] = 0;
         PAY[i][j] = PAY[j][i] = 0;
         PCT[i][j] = PCT[j][i] = 0;
      }

      EXH.remove(k);
      PREV.remove(k);
      MARRIAGES.removeIf(m -> m.a == k || m.b == k);
   }

   static List<String> save() {
      List<String> o = new ArrayList<>();
      o.add("S|" + on + "|" + t);
      Kingdoms.K[] ks = Kingdoms.K.values();

      for (int i = 0; i < N; i++) {
         for (int j = 0; j < N; j++) {
            Treaties.War w = i < j ? WARS[i][j] : null;
            if (w != null) {
               o.add("W|" + ks[i].name() + "|" + ks[j].name() + "|" + w.start + "|" + w.lossA + "|" + w.lossB + "|" + w.townsA + "|" + w.townsB);
            }

            if (i < j && TRUCE[i][j] > t) {
               o.add("T|" + ks[i].name() + "|" + ks[j].name() + "|" + TRUCE[i][j]);
            }

            if (PAY[i][j] > t) {
               o.add("P|" + ks[i].name() + "|" + ks[j].name() + "|" + PAY[i][j] + "|" + PCT[i][j]);
            }
         }
      }

      for (Entry<Kingdoms.K, Integer> e : EXH.entrySet()) {
         if (e.getValue() > 0) {
            o.add("X|" + e.getKey().name() + "|" + e.getValue());
         }
      }

      for (Treaties.Marriage m : MARRIAGES) {
         o.add("M|" + m.a.name() + "|" + m.b.name() + "|" + m.ha + "|" + m.hb + "|" + m.ua + "|" + m.ub + "|" + m.since);
      }

      return o;
   }

   static void load(String[] a) {
      try {
         String var1 = a[0];
         switch (var1) {
            case "S":
               on = Boolean.parseBoolean(a[1]);
               t = Integer.parseInt(a[2]);
               break;
            case "W":
               Kingdoms.K xx = Kingdoms.K.valueOf(a[1]);
               Kingdoms.K yx = Kingdoms.K.valueOf(a[2]);
               if (xx.ordinal() >= yx.ordinal()) {
                  return;
               }

               Treaties.War w = new Treaties.War();
               w.start = Integer.parseInt(a[3]);
               w.lossA = Integer.parseInt(a[4]);
               w.lossB = Integer.parseInt(a[5]);
               w.townsA = Integer.parseInt(a[6]);
               w.townsB = Integer.parseInt(a[7]);
               WARS[xx.ordinal()][yx.ordinal()] = w;
               break;
            case "T":
               Kingdoms.K x = Kingdoms.K.valueOf(a[1]);
               Kingdoms.K y = Kingdoms.K.valueOf(a[2]);
               if (x != y) {
                  setTruce(x, y, Integer.parseInt(a[3]));
               }
               break;
            case "P":
               Kingdoms.K px = Kingdoms.K.valueOf(a[1]);
               Kingdoms.K py = Kingdoms.K.valueOf(a[2]);
               if (px == py) {
                  return;
               }

               PAY[px.ordinal()][py.ordinal()] = Integer.parseInt(a[3]);
               PCT[px.ordinal()][py.ordinal()] = MathHelper.clamp(Integer.parseInt(a[4]), 0, 100);
               break;
            case "X":
               EXH.put(Kingdoms.K.valueOf(a[1]), MathHelper.clamp(Integer.parseInt(a[2]), 0, 100));
               break;
            case "M":
               Treaties.Marriage m = new Treaties.Marriage();
               m.a = Kingdoms.K.valueOf(a[1]);
               m.b = Kingdoms.K.valueOf(a[2]);
               if (m.a == m.b) {
                  return;
               }

               m.ha = Integer.parseInt(a[3]);
               m.hb = Integer.parseInt(a[4]);
               m.ua = a[5];
               m.ub = a[6];
               m.since = Integer.parseInt(a[7]);
               MARRIAGES.add(m);
         }
      } catch (Exception var6) {
      }
   }

   private static void reset() {
      clear();
      on = true;
   }

   public static void clear() {
      for (int i = 0; i < N; i++) {
         for (int j = 0; j < N; j++) {
            WARS[i][j] = null;
            TRUCE[i][j] = 0;
            PAY[i][j] = 0;
            PCT[i][j] = 0;
         }
      }

      EXH.clear();
      PREV.clear();
      MARRIAGES.clear();
      t = 0;
   }

   static {
      SaveExt.register("treaties", Treaties::save, Treaties::load, Treaties::reset);
   }

   static final class Marriage {
      Kingdoms.K a;
      Kingdoms.K b;
      int ha;
      int hb;
      int since;
      String ua = "-";
      String ub = "-";

      Kingdoms.K other(Kingdoms.K k) {
         return k == this.a ? this.b : this.a;
      }

      int house(Kingdoms.K k) {
         return k == this.a ? this.ha : this.hb;
      }

      String ruler(Kingdoms.K k) {
         return k == this.a ? this.ua : this.ub;
      }
   }

   static final class War {
      int start;
      int lossA;
      int lossB;
      int townsA;
      int townsB;
   }
}
