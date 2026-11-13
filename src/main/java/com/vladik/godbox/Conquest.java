package com.vladik.godbox;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public final class Conquest {
   public static boolean on = true;
   static final Map<Kingdoms.K, List<BlockPos>> TOWNS = new EnumMap<>(Kingdoms.K.class);
   private static final Map<Kingdoms.K, Integer> SIEGE = new EnumMap<>(Kingdoms.K.class);
   private static final Map<Kingdoms.K, BlockPos> LAST = new EnumMap<>(Kingdoms.K.class);
   private static int timer;

   private Conquest() {
   }

   static List<BlockPos> towns(Kingdoms.K k) {
      return TOWNS.computeIfAbsent(k, x -> new ArrayList<>());
   }

   static BlockPos center(Kingdoms.K k, Random r) {
      BlockPos p = k.capital;
      List<BlockPos> t = TOWNS.get(k);
      if (t != null && !t.isEmpty() && r.nextInt(t.size() + 1) != 0) {
         p = t.get(r.nextInt(t.size()));
      }

      LAST.put(k, p);
      return p;
   }

   static int cx(Kingdoms.K k, Random r) {
      return center(k, r).getX();
   }

   static int cz(Kingdoms.K k) {
      BlockPos p = LAST.get(k);
      return (p != null ? p : k.capital).getZ();
   }

   private static boolean soldier(MobEntity m) {
      return !(m instanceof VillagerEntity);
   }

   private static double hd(MobEntity m, BlockPos p) {
      double dx = m.getX() - p.getX() - 0.5;
      double dz = m.getZ() - p.getZ() - 0.5;
      return dx * dx + dz * dz;
   }

   static Kingdoms.K besieger(Kingdoms.K d, BlockPos p) {
      for (MobEntity m : d.members) {
         if (m.isAlive() && (soldier(m) ? hd(m, p) < 400.0 : hd(m, p) < 81.0)) {
            return null;
         }
      }

      Kingdoms.K best = null;
      int bn = 1;

      for (Kingdoms.K o : Kingdoms.K.values()) {
         if (o != d && o.alive && o.world == d.world && Diplomacy.hostile(d, o)) {
            int n = 0;

            for (MobEntity mx : o.members) {
               if (mx.isAlive() && soldier(mx) && hd(mx, p) < 196.0) {
                  n++;
               }
            }

            if (n > bn) {
               bn = n;
               best = o;
            }
         }
      }

      return best;
   }

   private static void say(MinecraftServer s, Text t) {
      Chronicle.log(t.getString());
      s.getPlayerManager().broadcast(t, false);
   }

   public static void tick(MinecraftServer s) {
      if (++timer % 20 == 0 && on) {
         for (Kingdoms.K d : Kingdoms.K.values()) {
            if (d.alive && d.world != null && d.capital != null && d.world.shouldTickEntity(d.capital) && Diplomacy.atWar(d)) {
               ServerWorld w = d.world;
               Kingdoms.K a = besieger(d, d.capital);
               if (a == null) {
                  SIEGE.remove(d);
               } else {
                  int n = SIEGE.merge(d, 1, Integer::sum);
                  Fx.particles(w, ParticleTypes.LARGE_SMOKE, d.capital.getX() + 0.5, d.capital.getY() + 5, d.capital.getZ() + 0.5, 12, 2.0, 1.0, 2.0, 0.02);
                  if (n == 1) {
                     say(s, Text.literal("\ud83d\udd25 " + a.title + " осаждает столицу: " + d.title + "!").formatted(a.color));
                  }

                  if (n >= 12) {
                     SIEGE.remove(d);
                     annex(s, a, d);
                     continue;
                  }
               }

               List<BlockPos> t = TOWNS.get(d);
               if (t != null) {
                  for (BlockPos p : new ArrayList<>(t)) {
                     Kingdoms.K b = besieger(d, p);
                     if (b != null && w.getRandom().nextInt(8) == 0) {
                        takeTown(s, b, d, p);
                     }
                  }
               }
            } else {
               SIEGE.remove(d);
            }
         }
      }
   }

   static boolean annex(MinecraftServer s, Kingdoms.K a, Kingdoms.K d) {
      if (a != null && d != null && a != d && a.alive && d.alive && d.world != null && d.capital != null) {
         Tech.trophy(s, a, d);
         ServerWorld w = d.world;
         Civ.City ca = Civ.city(a);
         Civ.City cb = Civ.city(d);
         int nb = 0;

         for (Civ.Building b : cb.buildings) {
            if (b != cb.quarry || ca.quarry == null) {
               ca.buildings.add(b);
               nb++;
            }
         }

         ca.wood = ca.wood + cb.wood;
         ca.stone = ca.stone + cb.stone;
         ca.food = ca.food + cb.food;
         ca.ore = ca.ore + cb.ore;
         ca.era = Math.max(ca.era, cb.era);
         if (cb.castle) {
            ca.castle = true;
         }

         List<BlockPos> ta = towns(a);
         ta.add(d.capital);
         List<BlockPos> td = TOWNS.remove(d);
         if (td != null) {
            ta.addAll(td);
         }

         int conv = 0;

         for (MobEntity m : new ArrayList<>(d.members)) {
            if (m.isAlive()) {
               m.removeCommandTag("gbking");
               m.setTarget(null);
               Kingdoms.join(a, m);
               a.members.add(m);
               conv++;
            }
         }

         d.members.clear();
         recolor(w, d.wool, a.wool, d.capital, 48);
         if (td != null) {
            for (BlockPos p : td) {
               recolor(w, d.wool, a.wool, p, 40);
            }
         }

         BlockPos c = d.capital;
         Plague.conquered(a, d);
         Kingdoms.disbandKingdom(d);
         Fx.sound(w, c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5, SoundEvents.EVENT_RAID_HORN.value(), 6.0F, 0.8F);
         Fx.particles(w, ParticleTypes.TOTEM_OF_UNDYING, c.getX() + 0.5, c.getY() + 4, c.getZ() + 0.5, 80, 2.0, 2.0, 2.0, 0.4);
         if (!Treaties.union) {
            say(
               s,
               Text.literal("⚔ " + a.title + " захватило " + d.title + "! Перешло построек: " + nb + ", жителей и воинов: " + conv)
                  .formatted(a.color, Formatting.BOLD)
            );
         }

         return true;
      } else {
         return false;
      }
   }

   static void takeTown(MinecraftServer s, Kingdoms.K a, Kingdoms.K d, BlockPos p) {
      List<BlockPos> td = TOWNS.get(d);
      if (td != null && td.remove(p)) {
         Civ.City ca = Civ.city(a);
         Civ.City cb = Civ.city(d);
         int nb = 0;

         for (Civ.Building b : new ArrayList<>(cb.buildings)) {
            if (b != cb.quarry) {
               double dx = b.cx() - p.getX();
               double dz = b.cz() - p.getZ();
               if (!(dx * dx + dz * dz > 2025.0)) {
                  if (cb.project != null && cb.project.b == b) {
                     cb.project = null;
                  }

                  cb.buildings.remove(b);
                  ca.buildings.add(b);
                  nb++;
               }
            }
         }

         int conv = 0;

         for (MobEntity m : new ArrayList<>(d.members)) {
            if (m.isAlive() && !(hd(m, p) > 2025.0) && !m.getCommandTags().contains("gbking")) {
               m.setTarget(null);
               Kingdoms.join(a, m);
               d.members.remove(m);
               a.members.add(m);
               conv++;
            }
         }

         towns(a).add(p);
         Props.transfer(d, a, p, 45);
         Plague.carry(d, a, 0.4, "с жителями захваченного города из");
         Treaties.captured(a, d);
         recolor(d.world, d.wool, a.wool, p, 45);
         Fx.sound(d.world, p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5, SoundEvents.EVENT_RAID_HORN.value(), 4.0F, 1.0F);
         say(
            s,
            Text.literal("\ud83c\udff4 " + a.title + " захватило город королевства «" + d.title + "» (" + nb + " построек, " + conv + " жителей)")
               .formatted(a.color)
         );
      }
   }

   static boolean fallTo(MinecraftServer s, Kingdoms.K d) {
      if (on && d.capital != null) {
         Kingdoms.K best = null;
         double bd = Double.MAX_VALUE;

         for (Kingdoms.K o : Kingdoms.K.values()) {
            if (o != d && o.alive && o.world == d.world && o.capital != null && Diplomacy.hostile(d, o)) {
               double dist = o.capital.getSquaredDistance(d.capital);

               for (MobEntity m : o.members) {
                  if (m.isAlive() && soldier(m) && hd(m, d.capital) < 4096.0) {
                     dist -= 1000000.0;
                  }
               }

               if (dist < bd) {
                  bd = dist;
                  best = o;
               }
            }
         }

         return best != null && annex(s, best, d);
      } else {
         return false;
      }
   }

   static BlockPos target(Kingdoms.K k, MobEntity m) {
      BlockPos best = null;
      double bd = Double.MAX_VALUE;

      for (Kingdoms.K o : Kingdoms.K.values()) {
         if (o != k && o.alive && o.world == k.world && o.capital != null && Diplomacy.hostile(k, o)) {
            List<BlockPos> all = new ArrayList<>();
            all.add(o.capital);
            List<BlockPos> t = TOWNS.get(o);
            if (t != null) {
               all.addAll(t);
            }

            for (BlockPos p : all) {
               double d = p.getSquaredDistance(m.getBlockPos());
               if (d < bd) {
                  bd = d;
                  best = p;
               }
            }
         }
      }

      return best;
   }

   public static void force(ServerPlayerEntity god, ServerWorld w, BlockPos pos) {
      Kingdoms.K d = null;
      Kingdoms.K a = null;
      double bd = Double.MAX_VALUE;

      for (Kingdoms.K k : Kingdoms.K.values()) {
         if (k.alive && k.world == w && k.capital != null) {
            double x = k.capital.getSquaredDistance(pos);
            if (x < bd) {
               bd = x;
               d = k;
            }
         }
      }

      if (d == null) {
         god.sendMessage(Text.literal("Рядом нет королевств").formatted(Formatting.GRAY), true);
      } else {
         bd = Double.MAX_VALUE;

         for (Kingdoms.K kx : Kingdoms.K.values()) {
            if (kx != d && kx.alive && kx.world == w && kx.capital != null) {
               double x = kx.capital.getSquaredDistance(d.capital);
               if (x < bd) {
                  bd = x;
                  a = kx;
               }
            }
         }

         if (a == null) {
            god.sendMessage(Text.literal("Нужно второе королевство").formatted(Formatting.RED), true);
         } else {
            annex(w.getServer(), a, d);
         }
      }
   }

   static void recolor(ServerWorld w, Block from, Block to, BlockPos c, int r) {
      if (w != null && from != to) {
         final BlockState ns = to.getDefaultState();
         Tasks.add(new Tasks.Task() {
            int i = 0;
            final int side = r * 2 + 1;

            @Override
            public boolean tick() {
               int n = 0;

               while (n < 300 && this.i < this.side * this.side) {
                  int x = c.getX() - r + this.i % this.side;
                  int z = c.getZ() - r + this.i / this.side;

                  for (int y = c.getY() - 4; y <= c.getY() + 24; y++) {
                     BlockPos p = new BlockPos(x, y, z);
                     if (w.getBlockState(p).isOf(from)) {
                        w.setBlockState(p, ns);
                     }
                  }

                  n++;
                  this.i++;
               }

               return this.i >= this.side * this.side;
            }
         });
      }
   }

   public static void forget(Kingdoms.K k) {
      TOWNS.remove(k);
      SIEGE.remove(k);
      LAST.remove(k);
   }

   public static void clear() {
      TOWNS.clear();
      SIEGE.clear();
      LAST.clear();
      timer = 0;
   }
}
