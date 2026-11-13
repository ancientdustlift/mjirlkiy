package com.vladik.godbox;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.VindicatorEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World.ExplosionSourceType;

public final class Tech {
   static final Map<Kingdoms.K, Integer> MASK = new EnumMap<>(Kingdoms.K.class);
   static final Map<Kingdoms.K, Integer> PTS = new EnumMap<>(Kingdoms.K.class);
   private static final Map<Kingdoms.K, Map<Kingdoms.K, Integer>> GOT = new EnumMap<>(Kingdoms.K.class);
   private static int t;
   public static boolean on = true;
   private static final Item[] CHEST = new Item[]{null, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE};
   private static final Item[] LEGS = new Item[]{null, Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS, Items.DIAMOND_LEGGINGS};
   private static final Item[] FEET = new Item[]{null, Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS, Items.DIAMOND_BOOTS};

   private Tech() {
   }

   public static boolean has(Kingdoms.K k, Tech.T x) {
      return (MASK.getOrDefault(k, 0) & 1 << x.ordinal()) != 0;
   }

   public static boolean has(Kingdoms.K k, String techName) {
      Tech.T x = byName(techName);
      return k != null && x != null && has(k, x);
   }

   public static Tech.T byName(String name) {
      if (name == null) {
         return null;
      } else {
         for (Tech.T x : Tech.T.values()) {
            if (x.name().equalsIgnoreCase(name) || x.title.equalsIgnoreCase(name)) {
               return x;
            }
         }

         return null;
      }
   }

   public static int count(Kingdoms.K k) {
      return Integer.bitCount(MASK.getOrDefault(k, 0));
   }

   static boolean ready(Kingdoms.K k, Tech.T x) {
      if (has(k, x)) {
         return false;
      } else {
         for (Tech.T n : x.need) {
            if (!has(k, n)) {
               return false;
            }
         }

         return true;
      }
   }

   public static Tech.T current(Kingdoms.K k) {
      Tech.T best = null;

      for (Tech.T x : Tech.T.values()) {
         if (!has(k, x)) {
            boolean ok = true;

            for (Tech.T n : x.need) {
               if (!has(k, n)) {
                  ok = false;
               }
            }

            if (ok && (best == null || x.cost < best.cost)) {
               best = x;
            }
         }
      }

      return best;
   }

   public static String info(Kingdoms.K k) {
      Tech.T c = current(k);
      if (c == null) {
         return count(k) + "/" + Tech.T.values().length + " (все открыты)";
      } else {
         int p = Math.max(0, PTS.getOrDefault(k, 0));
         return count(k) + "/" + Tech.T.values().length + ", изучает: " + c.title + " " + Math.min(99, p * 100 / c.cost) + "% (" + p + "/" + c.cost + ")";
      }
   }

   static void learn(MinecraftServer s, Kingdoms.K k, Tech.T x) {
      learn(s, k, x, "открыло технологию «" + x.title + "» — " + x.desc);
   }

   static void learn(MinecraftServer s, Kingdoms.K k, Tech.T x, String msg) {
      MASK.put(k, MASK.getOrDefault(k, 0) | 1 << x.ordinal());
      GOT.remove(k);
      Chronicle.say(s, k, msg);
      ServerWorld var5 = k.world;
      if (var5 instanceof ServerWorld && k.capital != null) {
         Fx.particles(var5, ParticleTypes.ENCHANT, k.capital.getX() + 0.5, k.capital.getY() + 3, k.capital.getZ() + 0.5, 80, 2.0, 2.0, 2.0, 0.5);
         Fx.sound(var5, k.capital.getX(), k.capital.getY(), k.capital.getZ(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 3.0F, 0.8F);
         salute(var5, k.capital);
      }
   }

   private static void salute(ServerWorld w, BlockPos c) {
      Fx.sound(w, c.getX(), c.getY() + 2, c.getZ(), SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 3.0F, 1.0F);

      for (int i = 0; i < 3; i++) {
         int n = i;
         Tasks.delay(12 + i * 10, () -> {
            double x = c.getX() + 0.5 + (n - 1) * 4;
            double y = c.getY() + 12 + n * 2;
            double z = c.getZ() + 0.5 + (n == 1 ? 3 : -2);
            Fx.particles(w, ParticleTypes.FIREWORK, x, y, z, 50, 0.4, 0.4, 0.4, 0.18);
            Fx.sound(w, x, y, z, n == 2 ? SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE : SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 4.0F, 1.0F);
         });
      }
   }

   static boolean leak(Kingdoms.K k, Kingdoms.K o, int base) {
      if (on && base > 0 && k != null && o != null && k != o && k.alive && o.alive && Config.b("tech.leak.on", true)) {
         Tech.T cur = current(k);
         if (cur != null && has(o, cur)) {
            int pts = Math.max(1, (int)Math.round(base * Config.d("tech.leak.rate", 1.0)));
            PTS.merge(k, pts, Integer::sum);
            GOT.computeIfAbsent(k, x -> new EnumMap<>(Kingdoms.K.class)).merge(o, pts, Integer::sum);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static Kingdoms.K donor(Kingdoms.K k, Tech.T x) {
      Map<Kingdoms.K, Integer> g = GOT.get(k);
      if (g == null) {
         return null;
      } else {
         Kingdoms.K best = null;
         int sum = 0;
         int bp = 0;

         for (Entry<Kingdoms.K, Integer> e : g.entrySet()) {
            sum += e.getValue();
            if (e.getValue() > bp) {
               bp = e.getValue();
               best = e.getKey();
            }
         }

         return sum >= x.cost * Config.d("tech.leak.share", 0.5) ? best : null;
      }
   }

   static void caravan(Kingdoms.K from, Kingdoms.K to) {
      int n = Config.i("tech.leak.caravan", 6);
      leak(to, from, n);
      leak(from, to, n);
   }

   static void trophy(MinecraftServer s, Kingdoms.K a, Kingdoms.K d) {
      if (on && a != null && d != null && a != d && Config.b("tech.leak.on", true)) {
         Tech.T cur = current(a);
         Tech.T pick = cur != null && has(d, cur) ? cur : null;
         if (pick == null) {
            for (Tech.T x : Tech.T.values()) {
               if (has(d, x) && ready(a, x) && (pick == null || x.cost < pick.cost)) {
                  pick = x;
               }
            }
         }

         if (pick != null) {
            learn(s, a, pick, "захватило трофейные знания: «" + pick.title + "» из библиотек «" + d.title + "» — " + pick.desc);
         } else {
            int n = Config.i("tech.leak.conquest", 40);
            if (cur != null && n > 0 && count(d) > 0) {
               PTS.merge(a, n, Integer::sum);
            }
         }
      }
   }

   private static boolean road(Kingdoms.K k, Kingdoms.K o) {
      String key = Roads.key(k.capital, o.capital);
      return Roads.DONE.contains(key) || Highways.DONE.contains(key);
   }

   private static void diffuse(MinecraftServer s, Kingdoms.K k) {
      if (k.capital != null && Config.b("tech.leak.on", true)) {
         Tech.T cur = current(k);
         if (cur != null) {
            for (Kingdoms.K o : Kingdoms.K.values()) {
               if (o != k && o.alive && o.world == k.world && o.capital != null && has(o, cur)) {
                  int rel = Diplomacy.rel(k, o);
                  int n = 0;
                  if (rel == 2) {
                     n += Config.i("tech.leak.ally", 2);
                  }

                  if (Vassals.lord(k) == o || Vassals.lord(o) == k) {
                     n += Config.i("tech.leak.vassal", 2);
                  }

                  if (rel != 1 && road(k, o)) {
                     n += Config.i("tech.leak.road", 1);
                  }

                  leak(k, o, n);
                  if (rel == 1 && count(o) > count(k)) {
                     ServerWorld var10 = k.world;
                     if (var10 instanceof ServerWorld
                        && (Civ.city(k).culture == 0 || Life.kingTrait(k) == 1)
                        && var10.getRandom().nextInt(Math.max(1, Config.i("tech.spy.roll", 20))) == 0
                        && leak(k, o, Config.i("tech.spy.points", 20))) {
                        Chronicle.say(s, k, "— лазутчики выкрали у «" + o.title + "» секреты технологии «" + cur.title + "»");
                     }
                  }
               }
            }
         }
      }
   }

   static int armorTier(Kingdoms.K k) {
      if (has(k, Tech.T.METAL) && Config.b("tech.armor.on", true)) {
         int era = Civ.city(k).era;
         if (!has(k, Tech.T.GUNPOWDER) && (!has(k, Tech.T.ENGINEERING) || era < Config.i("tech.armor.diamondEra", 7))) {
            return !has(k, Tech.T.FORT) && era < Config.i("tech.armor.ironEra", 4) ? 1 : 2;
         } else {
            return 3;
         }
      } else {
         return 0;
      }
   }

   private static int tierOf(MobEntity m) {
      Item i = m.getEquippedStack(EquipmentSlot.CHEST).getItem();

      for (int n = 3; n > 0; n--) {
         if (i == CHEST[n]) {
            return n;
         }
      }

      return 0;
   }

   private static void wear(MobEntity m, EquipmentSlot slot, Item item) {
      m.equipStack(slot, new ItemStack(item));
      m.setEquipmentDropChance(slot, 0.0F);
   }

   static void equip(Kingdoms.K k, MobEntity m) {
      if (!(m instanceof VillagerEntity)) {
         int tier = armorTier(k);
         if (tier > 0 && tierOf(m) < tier) {
            wear(m, EquipmentSlot.CHEST, CHEST[tier]);
            if (tier >= 2) {
               wear(m, EquipmentSlot.LEGS, LEGS[tier]);
               wear(m, EquipmentSlot.FEET, FEET[tier]);
            }

            if (tier >= 3 && m instanceof VindicatorEntity) {
               wear(m, EquipmentSlot.MAINHAND, Items.DIAMOND_AXE);
            }
         }
      }
   }

   private static void rearm(Kingdoms.K k) {
      int tier = armorTier(k);
      int left = Config.i("tech.armor.batch", 4);
      if (tier > 0) {
         ServerWorld var4 = k.world;
         if (var4 instanceof ServerWorld) {
            ServerWorld w = var4;

            for (MobEntity m : k.members) {
               if (left <= 0) {
                  break;
               }

               if (m.isAlive() && !(m instanceof VillagerEntity) && tierOf(m) < tier) {
                  equip(k, m);
                  Fx.particles(w, ParticleTypes.CRIT, m.getX(), m.getY() + 1.0, m.getZ(), 10, 0.3, 0.5, 0.3, 0.1);
                  if (left-- == Config.i("tech.armor.batch", 4)) {
                     Fx.sound(w, m.getX(), m.getY(), m.getZ(), SoundEvents.BLOCK_ANVIL_USE, 0.8F, 1.2F);
                  }
               }
            }

            return;
         }
      }
   }

   static boolean cannon(ServerWorld w, Kingdoms.K k, MobEntity m, BlockPos target) {
      if (!has(k, Tech.T.GUNPOWDER)) {
         return false;
      } else {
         cannonball(w, m, m.getX(), m.getEyeY() + 1.0, m.getZ(), target, new ItemStack(Items.FIRE_CHARGE));
         return true;
      }
   }

   static void cannonball(ServerWorld w, MobEntity m, double ox, double oy, double oz, BlockPos target, ItemStack item) {
      cannonball(w, m, ox, oy, oz, target, item, (float)Config.d("tech.cannon.power", 3.5));
   }

   static void cannonball(ServerWorld w, MobEntity m, double ox, double oy, double oz, BlockPos target, ItemStack item, float power) {
      double dx = target.getX() + 0.5 - ox;
      double dz = target.getZ() + 0.5 - oz;
      double dy = target.getY() - oy;
      double d = Math.sqrt(dx * dx + dz * dz);
      double n = Math.max(12.0, d / 2.0);
      SnowballEntity sb = new SnowballEntity(w, m);
      sb.setItem(item);
      sb.setPosition(ox, oy, oz);
      sb.setVelocity(dx / n * (1.0 + 0.0055 * n), (dy + 0.015 * n * n) / n * (1.0 + 0.004 * n), dz / n * (1.0 + 0.0055 * n));
      w.spawnEntity(sb);
      Fx.sound(w, ox, oy, oz, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 4.0F, 1.4F);
      Fx.particles(w, ParticleTypes.EXPLOSION, ox, oy, oz, 1, 0.0, 0.0, 0.0, 0.0);
      Fx.particles(w, ParticleTypes.LARGE_SMOKE, ox, oy, oz, 14, 0.4, 0.4, 0.4, 0.04);
      Vec3d[] last = new Vec3d[]{sb.getPos()};
      int[] age = new int[]{0};
      Tasks.add(() -> {
         age[0]++;
         if (sb.isAlive() && age[0] < 120) {
            last[0] = sb.getPos();
            if (age[0] % 2 == 0) {
               Fx.particles(w, ParticleTypes.FLAME, last[0].x, last[0].y, last[0].z, 2, 0.05, 0.05, 0.05, 0.0);
            }

            return false;
         } else {
            if (sb.isAlive()) {
               sb.discard();
            }

            w.createExplosion(null, last[0].x, last[0].y, last[0].z, power, ExplosionSourceType.TNT);
            Fx.particles(w, ParticleTypes.LARGE_SMOKE, last[0].x, last[0].y + 1.0, last[0].z, 20, 1.0, 1.0, 1.0, 0.05);
            return true;
         }
      });
   }

   static int broadside(Kingdoms.K k) {
      return has(k, Tech.T.GUNPOWDER) ? 1 : 0;
   }

   private static void muskets(Kingdoms.K k, ServerWorld w) {
      int left = Config.i("tech.musket.shots", 3);
      int roll = Math.max(1, Config.i("tech.musket.roll", 3));
      float dmg = (float)Config.d("tech.musket.damage", 5.0);
      Random r = w.getRandom();

      for (MobEntity m : new ArrayList<>(k.members)) {
         if (left <= 0) {
            break;
         }

         if (m.isAlive() && !(m instanceof VillagerEntity) && m.getWorld() == w) {
            LivingEntity e = m.getTarget();
            if (e != null && e.isAlive() && e.getWorld() == w) {
               double d2 = m.squaredDistanceTo(e);
               boolean musket = Guns.isMusket(m.getMainHandStack());
               if ((musket || !Models.custom() || !ModelParts.has("guns")) && !(d2 < 16.0) && !(d2 > 400.0) && r.nextInt(roll) == 0 && m.canSee(e)) {
                  left--;
                  if (musket) {
                     Guns.fire(w, m, e, Vec3d.ZERO);
                  } else {
                     Vec3d a = new Vec3d(m.getX(), m.getEyeY(), m.getZ());
                     Vec3d v = new Vec3d(e.getX(), e.getBodyY(0.6), e.getZ()).subtract(a);

                     for (int i = 1; i <= 5; i++) {
                        Fx.particles(w, ParticleTypes.SMOKE, a.x + v.x * i / 6.0, a.y + v.y * i / 6.0, a.z + v.z * i / 6.0, 1, 0.0, 0.0, 0.0, 0.0);
                     }

                     Fx.particles(w, ParticleTypes.FLAME, a.x, a.y, a.z, 4, 0.1, 0.1, 0.1, 0.02);
                     Fx.particles(w, ParticleTypes.CRIT, e.getX(), e.getBodyY(0.6), e.getZ(), 6, 0.2, 0.3, 0.2, 0.1);
                     Fx.sound(w, a.x, a.y, a.z, SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 1.6F, 0.6F);
                     e.damage(w.getDamageSources().mobAttack(m), dmg);
                  }
               }
            }
         }
      }
   }

   static Ships.Voyage sail(Kingdoms.K k, Ships.Voyage v) {
      if (has(k, Tech.T.NAVIGATION)) {
         v.speed = v.speed * Math.max(0.5, Math.min(3.0, Config.d("tech.navigation.speed", 1.5)));
      }

      return v;
   }

   static int seaReach(Kingdoms.K k, int base) {
      return has(k, Tech.T.NAVIGATION) ? base * Config.i("tech.navigation.reach", 150) / 100 : base;
   }

   static int roadReach(Kingdoms.K k, int base) {
      return has(k, Tech.T.ENGINEERING) ? base * Config.i("tech.engineering.reach", 150) / 100 : base;
   }

   static int convertRoll(Kingdoms.K k, int base) {
      return has(k, Tech.T.PRINTING) ? Math.max(2, base * Config.i("tech.printing.convert", 60) / 100) : base;
   }

   static void books(MinecraftServer s, Kingdoms.K k, Kingdoms.K o) {
      if (has(k, Tech.T.PRINTING) && !has(o, Tech.T.PRINTING) && Diplomacy.rel(k, o) != 1 && k.world != null) {
         Civ.City c = Civ.city(k);
         Civ.City oc = Civ.city(o);
         if (c.culture >= 0 && oc.culture != c.culture) {
            if (k.world.getRandom().nextInt(Math.max(1, Config.i("tech.printing.cultureRoll", 60))) == 0) {
               oc.culture = c.culture;
               Diplomacy.op(k, o, 3);
               Chronicle.say(s, o, "перенимает культуру «" + Faith.culture(c) + "» из книг «" + k.title + "»");
            }
         }
      }
   }

   static boolean bastions(ServerWorld w, Kingdoms.K k) {
      if (has(k, Tech.T.ENGINEERING) && Siege.WALL.getOrDefault(k, 0) == 2 && k.capital != null) {
         Civ.City c = Civ.city(k);
         if (c.stone < 60) {
            return false;
         } else {
            TownBuilder.Job j = new TownBuilder.Job(w);
            int R = Siege.radius(k);
            int built = 0;

            for (int i = 0; i < 4; i++) {
               double a = (Math.PI / 4) + (Math.PI / 2) * i;
               int x = k.capital.getX() + (int)Math.round(Math.cos(a) * R);
               int z = k.capital.getZ() + (int)Math.round(Math.sin(a) * R);
               if (w.isChunkLoaded(x - 2 >> 4, z - 2 >> 4) && w.isChunkLoaded(x + 2 >> 4, z + 2 >> 4) && TownBuilder.dry(w, x, z)) {
                  int y = TownBuilder.ground(w, x, z);

                  for (int dx = -2; dx <= 2; dx++) {
                     for (int dz = -2; dz <= 2; dz++) {
                        boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;

                        for (int dy = -1; dy < 9; dy++) {
                           j.put(
                              new BlockPos(x + dx, y + dy, z + dz),
                              (dy % 4 == 3 && edge ? Blocks.CHISELED_STONE_BRICKS : Blocks.STONE_BRICKS).getDefaultState()
                           );
                        }

                        if (edge && (dx + dz & 1) == 0) {
                           j.put(new BlockPos(x + dx, y + 9, z + dz), Blocks.STONE_BRICK_WALL.getDefaultState());
                        }
                     }
                  }

                  j.put(new BlockPos(x, y + 9, z), Blocks.STONE_BRICK_WALL.getDefaultState());
                  j.put(new BlockPos(x, y + 10, z), Blocks.STONE_BRICK_WALL.getDefaultState());
                  j.put(new BlockPos(x, y + 11, z), k.wool.getDefaultState());
                  j.put(new BlockPos(x + 1, y + 9, z + 1), Blocks.LANTERN.getDefaultState());
                  j.put(new BlockPos(x - 1, y + 9, z - 1), Blocks.LANTERN.getDefaultState());
                  built++;
               }
            }

            if (built == 0) {
               return false;
            } else {
               c.stone -= 60;
               Tasks.add(j);
               Siege.WALL.put(k, 3);
               Chronicle.say(w.getServer(), k, "усиливает стены столицы бастионами");
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private static void irrigate(Kingdoms.K k, Civ.City c) {
      int farms = 0;

      for (Civ.Building b : c.buildings) {
         if (b.kind == Civ.Kind.FARM && b.done && farms++ < 4) {
            ServerWorld var6 = k.world;
            if (var6 instanceof ServerWorld && var6.isChunkLoaded(b.cx() >> 4, b.cz() >> 4)) {
               Fx.particles(var6, ParticleTypes.SPLASH, b.cx() + 0.5, b.y0 + 1.5, b.cz() + 0.5, 30, 2.5, 0.2, 2.5, 0.1);
            }
         }
      }

      c.food = c.food + Math.min(Config.i("tech.irrigation.max", 6), 1 + farms);
   }

   public static void tick(MinecraftServer s) {
      t++;
      if (t % 40 == 20 && Perf.go) {
         for (Kingdoms.K k : Kingdoms.K.values()) {
            if (k.alive) {
               ServerWorld var6 = k.world;
               if (var6 instanceof ServerWorld && has(k, Tech.T.GUNPOWDER) && Diplomacy.atWar(k)) {
                  muskets(k, var6);
               }
            }
         }
      }

      if (t % 400 == 0) {
         double mult = Config.d("tech.research.mult", 1.0);
         int heal = Config.i("tech.medicine.heal", 6);

         for (Kingdoms.K kx : Kingdoms.K.values()) {
            if (!kx.alive) {
               MASK.remove(kx);
               PTS.remove(kx);
               GOT.remove(kx);
            } else {
               Civ.City c = Civ.city(kx);
               int pop = 0;

               for (MobEntity m : kx.members) {
                  if (m.isAlive()) {
                     pop++;
                  }
               }

               int gain = 1
                  + pop / 4
                  + c.era
                  + (has(kx, Tech.T.WRITING) ? 2 : 0)
                  + (has(kx, Tech.T.ASTRONOMY) ? 2 : 0)
                  + (Wonders.has(kx, "library") ? 5 : 0)
                  + (has(kx, Tech.T.PRINTING) ? Config.i("tech.printing.research", 3) : 0);
               diffuse(s, kx);
               int p = PTS.getOrDefault(kx, 0) + Races.research(kx, (int)Math.round(gain * mult));
               Tech.T cur = current(kx);
               if (cur != null && p >= cur.cost) {
                  p -= cur.cost;
                  Kingdoms.K from = donor(kx, cur);
                  if (from == null) {
                     learn(s, kx, cur);
                  } else {
                     learn(s, kx, cur, "переняло «" + cur.title + "» у «" + from.title + "» — " + cur.desc);
                  }
               }

               PTS.put(kx, cur == null ? 0 : p);
               if (has(kx, Tech.T.FARMING)) {
                  c.food += 2;
               }

               if (has(kx, Tech.T.MASONRY)) {
                  c.stone += 2;
               }

               if (has(kx, Tech.T.ASTRONOMY)) {
                  c.faith++;
               }

               if (has(kx, Tech.T.IRRIGATION)) {
                  irrigate(kx, c);
               }

               if (has(kx, Tech.T.PRINTING) && t % 1200 == 0) {
                  Unrest.add(kx, -1);
               }

               int hearts = 0;

               for (MobEntity mx : kx.members) {
                  if (mx.isAlive()) {
                     if (has(kx, Tech.T.MEDICINE) && mx.getHealth() < mx.getMaxHealth()) {
                        mx.heal(mx instanceof VillagerEntity ? Math.max(3, heal) : 3.0F);
                        if (hearts++ < 3 && mx.getWorld() instanceof ServerWorld mw) {
                           Fx.particles(mw, ParticleTypes.HEART, mx.getX(), mx.getY() + 1.8, mx.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
                        }
                     }

                     if (has(kx, Tech.T.METAL) && !(mx instanceof VillagerEntity)) {
                        mx.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 440, 0, false, false));
                     }
                  }
               }

               rearm(kx);
            }
         }
      }
   }

   public static void gift(ServerPlayerEntity god, ServerWorld w, BlockPos pos) {
      Kingdoms.K k = nearest(w, pos);
      if (k == null) {
         god.sendMessage(Text.literal("Нет королевств").formatted(Formatting.RED), true);
      } else {
         Tech.T c = current(k);
         if (c == null) {
            god.sendMessage(Text.literal("«" + k.title + "» уже знает всё").formatted(k.color), true);
         } else {
            learn(god.getServer(), k, c);
            god.sendMessage(Text.literal("Дар знаний: «" + k.title + "» → " + c.title).formatted(k.color), true);
         }
      }
   }

   static Kingdoms.K nearest(ServerWorld w, BlockPos pos) {
      Kingdoms.K k = null;
      double best = Double.MAX_VALUE;

      for (Kingdoms.K o : Kingdoms.K.values()) {
         if (o.alive && o.capital != null && o.world == w) {
            double d = o.capital.getSquaredDistance(pos);
            if (d < best) {
               best = d;
               k = o;
            }
         }
      }

      return k;
   }

   public static void clear() {
      MASK.clear();
      PTS.clear();
      GOT.clear();
      t = 0;
   }

   public static enum T {
      FARMING("Земледелие", 20, "+еда каждую минуту"),
      MASONRY("Каменотёсы", 25, "+камень"),
      WHEEL("Колесо", 35, "дороги между городами", MASONRY),
      WRITING("Письменность", 45, "быстрее наука", FARMING),
      SAILING("Мореплавание", 50, "порты и колонии за морем", WHEEL),
      METAL("Металлургия", 60, "солдаты сильнее", MASONRY),
      MEDICINE("Медицина", 70, "жители лечатся", WRITING),
      FORT("Фортификация", 80, "стены и башни вокруг столицы", METAL),
      MECHANICS("Механика", 100, "катапульты на осадах", FORT, WHEEL),
      NAVIGATION("Навигация", 110, "военный флот", SAILING, METAL),
      ARCH("Архитектура", 130, "чудеса света", MECHANICS, WRITING),
      MAGIC("Магия", 150, "боевые маги и целители", MEDICINE, WRITING),
      ASTRONOMY("Астрономия", 170, "+вера и знания", NAVIGATION, MAGIC),
      IRRIGATION("Ирригация", 55, "каналы орошают фермы, больше еды", FARMING, MASONRY),
      ENGINEERING("Инженерия", 120, "бастионы на стенах, дороги строятся быстрее и дальше", MECHANICS),
      PRINTING("Книгопечатание", 140, "наука быстрее, вера и культура расходятся по соседям", WRITING, MECHANICS),
      GUNPOWDER("Порох", 200, "пушки вместо катапульт, мушкеты у солдат, корабельные орудия", ENGINEERING, METAL);

      public final String title;
      public final String desc;
      public final int cost;
      final Tech.T[] need;

      private T(String title, int cost, String desc, Tech.T... need) {
         this.title = title;
         this.cost = cost;
         this.desc = desc;
         this.need = need;
      }
   }
}
