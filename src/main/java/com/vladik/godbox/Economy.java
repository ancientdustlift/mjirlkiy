package com.vladik.godbox;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public final class Economy {
   public static boolean on = true;
   static final Map<Kingdoms.K, Integer> GOLD = new EnumMap<>(Kingdoms.K.class);
   static final Map<Kingdoms.K, Integer> TAX = new EnumMap<>(Kingdoms.K.class);
   static final Map<Kingdoms.K, BlockPos> MARKET = new EnumMap<>(Kingdoms.K.class);
   static final String[] TAXN = new String[]{"низкие", "обычные", "высокие"};
   private static int t;

   private Economy() {
   }

   static int gold(Kingdoms.K k) {
      return GOLD.getOrDefault(k, 0);
   }

   static void add(Kingdoms.K k, int n) {
      GOLD.put(k, Math.max(0, gold(k) + n));
   }

   public static void tick(MinecraftServer s) {
      t++;
      if (t % 40 == 0) {
         smoke();
      }

      if (t % 600 == 0 && on) {
         for (Kingdoms.K k : Kingdoms.K.values()) {
            if (!k.alive) {
               GOLD.remove(k);
               TAX.remove(k);
               MARKET.remove(k);
            } else if (k.capital != null) {
               ServerWorld c = k.world;
               if (c instanceof ServerWorld) {
                  ServerWorld w = c;
                  Civ.City cx = Civ.city(k);
                  int vill = 0;
                  int war = 0;

                  for (MobEntity m : k.members) {
                     if (m.isAlive()) {
                        if (m instanceof VillagerEntity) {
                           vill++;
                        } else {
                           war++;
                        }
                     }
                  }

                  int g = gold(k);
                  int tax = TAX.getOrDefault(k, 1);
                  int want = g < 20 ? 2 : (g > 250 ? 0 : 1);
                  if (Unrest.unrest(k) > 55) {
                     want = 0;
                  }

                  if (want != tax) {
                     tax = want;
                     TAX.put(k, want);
                     Chronicle.say(s, k, "устанавливает " + TAXN[want] + " налоги");
                  }

                  int income = vill * (1 + tax) / 2 + cx.era / 2 + (MARKET.containsKey(k) ? 4 + Conquest.towns(k).size() * 2 : 0);
                  if (Tech.has(k, Tech.T.WRITING)) {
                     income += income / 5;
                  }

                  income = Races.gold(k, income);
                  income = Treaties.income(k, income);
                  if (cx.era >= 10) {
                     income += 6;
                     cx.wood += 4;
                     cx.stone += 4;
                     cx.ore += 2;
                  }

                  g += income - war / 3;
                  if (g <= 0) {
                     g = 0;
                     Unrest.add(k, 3);
                  }

                  if (tax == 2) {
                     Unrest.add(k, 2);
                  } else if (tax == 0) {
                     Unrest.add(k, -2);
                  }

                  if (MARKET.containsKey(k)) {
                     if (cx.wood > 90) {
                        cx.wood -= 20;
                        g += 5;
                     }

                     if (cx.stone > 90) {
                        cx.stone -= 20;
                        g += 5;
                     }

                     if (cx.ore > 40) {
                        cx.ore -= 10;
                        g += 8;
                     }

                     if (cx.food < 15 && g >= 10) {
                        cx.food += 20;
                        g -= 10;
                     }

                     if (cx.wood < 10 && g >= 8) {
                        cx.wood += 15;
                        g -= 8;
                     }

                     if (cx.stone < 8 && g >= 8) {
                        cx.stone += 12;
                        g -= 8;
                     }
                  } else if (cx.era >= 1 && g >= 25 && cx.wood >= 12 && market(c, k, cx)) {
                     g -= 25;
                     cx.wood -= 12;
                  }

                  Random r = c.getRandom();
                  if (Diplomacy.atWar(k) && g >= 60 && war < 30) {
                     g -= 40;

                     for (int i = 0; i < 3; i++) {
                        Kingdoms.spawn(k, Kingdoms.soldier(r), Fx.surface(w, k.capital.getX() + r.nextInt(9) - 4, k.capital.getZ() + r.nextInt(9) - 4));
                     }

                     Chronicle.say(s, k, "нанимает отряд наёмников за 40 золота");
                  } else if (g >= 300) {
                     g -= 120;
                     cx.food += 30;
                     Unrest.add(k, -15);
                     BlockPos p = k.capital;
                     Fx.particles(c, ParticleTypes.FIREWORK, p.getX() + 0.5, p.getY() + 6, p.getZ() + 0.5, 120, 4.0, 3.0, 4.0, 0.2);
                     Fx.sound(c, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, 3.0F, 1.0F);
                     Chronicle.say(s, k, "устраивает пир на весь мир (−15 к смуте)");
                  }

                  GOLD.put(k, g);
               }
            }
         }
      }
   }

   static void smoke() {
      for (Kingdoms.K k : Kingdoms.K.values()) {
         if (k.alive) {
            ServerWorld c = k.world;
            if (c instanceof ServerWorld) {
               ServerWorld w = c;
               Civ.City cx = Civ.city(k);
               if (cx.era >= 10) {
                  int n = 0;

                  for (Civ.Building b : cx.buildings) {
                     if (b.done && b.kind == Civ.Kind.HOUSE) {
                        if (n++ > 10) {
                           break;
                        }

                        if (w.isChunkLoaded(b.x1 >> 4, b.z1 >> 4)) {
                           Fx.particles(w, ParticleTypes.LARGE_SMOKE, b.x1 + 0.5, b.y0 + 6.5, b.z1 + 0.5, 3, 0.1, 0.4, 0.1, 0.01);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   static boolean market(ServerWorld w, Kingdoms.K k, Civ.City c) {
      Random r = w.getRandom();
      List<Civ.Building> bs = Highways.allBuildings();

      for (int tr = 0; tr < 10; tr++) {
         double a = r.nextDouble() * Math.PI * 2.0;
         int d = 12 + r.nextInt(12);
         int x = k.capital.getX() + (int)(Math.cos(a) * d);
         int z = k.capital.getZ() + (int)(Math.sin(a) * d);
         if (w.isChunkLoaded(x >> 4, z >> 4) && TownBuilder.dryArea(w, x, z, 4)) {
            boolean clash = false;

            for (int dx = -4; dx <= 4 && !clash; dx += 2) {
               for (int dz = -4; dz <= 4; dz += 2) {
                  if (Roads.inBuilding(bs, x + dx, z + dz)) {
                     clash = true;
                     break;
                  }
               }
            }

            if (!clash) {
               int y = TownBuilder.ground(w, x, z);
               TownBuilder.Job pl = new TownBuilder.Job(w);
               Props.Prop mp = Props.add(Props.Kind.MARKET, k, x, y, z, 0, c.era);
               Props.build(pl, mp, c.era);
               pl.onDone = () -> mp.busy = false;
               Tasks.add(pl);
               MARKET.put(k, new BlockPos(x, y, z));
               Chronicle.say(w.getServer(), k, "открывает рынок — теперь излишки продаются за золото");
               return true;
            }
         }
      }

      return false;
   }

   public static void gift(ServerPlayerEntity god, ServerWorld w, BlockPos pos) {
      Kingdoms.K k = Tech.nearest(w, pos);
      if (k == null) {
         god.sendMessage(Text.literal("Рядом нет королевства"), true);
      } else {
         add(k, 100);
         Fx.particles(w, ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5, 80, 2.0, 2.0, 2.0, 0.3);
         Fx.sound(w, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ENTITY_PLAYER_LEVELUP, 2.0F, 1.2F);
         god.sendMessage(Text.literal("\ud83d\udcb0 " + k.title + ": +100 золота (казна " + gold(k) + ")").formatted(k.color), true);
      }
   }

   static String info(Kingdoms.K k) {
      return gold(k) + " зол., налоги " + TAXN[TAX.getOrDefault(k, 1)] + (MARKET.containsKey(k) ? ", есть рынок" : "");
   }

   public static void clear() {
      GOLD.clear();
      TAX.clear();
      MARKET.clear();
      t = 0;
   }
}
