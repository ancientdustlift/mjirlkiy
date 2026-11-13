package com.vladik.godbox;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Type;
import net.minecraft.util.math.random.Random;
import org.joml.Quaternionf;

public final class Ships {
   public static boolean on = true;
   static final Map<Kingdoms.K, BlockPos> PORTS = new EnumMap<>(Kingdoms.K.class);
   private static final Map<Kingdoms.K, BoatEntity> FISH = new EnumMap<>(Kingdoms.K.class);
   private static final Map<BoatEntity, ItemDisplayEntity> RIGS = new IdentityHashMap<>();
   private static int t;

   private Ships() {
   }

   static boolean sea(ServerWorld w, BlockPos p) {
      return w.getFluidState(p).isIn(FluidTags.WATER) && w.getBlockState(p.up()).isAir();
   }

   static BlockPos waterTop(ServerWorld w, int x, int z) {
      if (!w.isChunkLoaded(x >> 4, z >> 4)) {
         return null;
      } else {
         BlockPos s = Fx.surface(w, x, z).down();
         return w.getFluidState(s).isIn(FluidTags.WATER) ? s : null;
      }
   }

   static BlockPos port(ServerWorld w, Kingdoms.K k) {
      BlockPos p = PORTS.get(k);
      if (p != null && w.getFluidState(p).isIn(FluidTags.WATER)) {
         return p;
      } else {
         PORTS.remove(k);
         if (k.capital == null) {
            return null;
         } else {
            Random r = w.getRandom();

            for (int i = 0; i < 120; i++) {
               BlockPos wt = waterTop(w, k.capital.getX() + r.nextInt(97) - 48, k.capital.getZ() + r.nextInt(97) - 48);
               if (wt != null && w.getFluidState(wt.down()).isIn(FluidTags.WATER)) {
                  for (Direction land : Type.HORIZONTAL) {
                     BlockPos l = wt.offset(land, 2);
                     if (waterTop(w, l.getX(), l.getZ()) == null) {
                        BlockPos far = wt.offset(land.getOpposite(), 12);
                        if (waterTop(w, far.getX(), far.getZ()) != null) {
                           return build(w, k, wt, land);
                        }
                     }
                  }
               }
            }

            return null;
         }
      }
   }

   static void pier(TownBuilder.Job j, Kingdoms.K k, int era, BlockPos wt, Direction land) {
      Direction sea = land.getOpposite();
      Direction side = sea.rotateYClockwise();
      int y = wt.getY();

      for (int d = -1; d < 8; d++) {
         for (int s = -1; s <= 1; s++) {
            BlockPos p = wt.offset(sea, d).offset(side, s);
            p = new BlockPos(p.getX(), y, p.getZ());
            j.put(p, Eras.style(era, Civ.Kind.HOUSE, Blocks.OAK_PLANKS.getDefaultState()));
            if ((d == 2 || d == 6) && s != 0) {
               j.put(p.down(), Blocks.OAK_FENCE.getDefaultState());
               j.put(p.down(2), Blocks.OAK_FENCE.getDefaultState());
               j.put(p.up(), Blocks.OAK_FENCE.getDefaultState());
               j.put(p.up(2), era >= 3 ? Blocks.LANTERN.getDefaultState() : Blocks.TORCH.getDefaultState());
            }
         }
      }

      BlockPos end = wt.offset(sea, 7);

      for (int dy = 1; dy <= 4; dy++) {
         j.put(new BlockPos(end.getX(), y + dy, end.getZ()), Blocks.OAK_FENCE.getDefaultState());
      }

      j.put(new BlockPos(end.getX(), y + 5, end.getZ()), k.wool.getDefaultState());
      j.put(wt.offset(sea, 0).offset(side, 1).up(), Blocks.BARREL.getDefaultState());
      j.put(wt.offset(sea, 0).offset(side, -1).up(), Blocks.CHEST.getDefaultState());
   }

   static BlockPos build(ServerWorld w, Kingdoms.K k, BlockPos wt, Direction land) {
      Direction sea = land.getOpposite();
      int era = Civ.city(k).era;
      TownBuilder.Job j = new TownBuilder.Job(w);
      pier(j, k, era, wt, land);
      Props.Prop pr = Props.add(Props.Kind.PIER, k, wt.getX(), wt.getY(), wt.getZ(), land.getHorizontal(), era);
      j.onDone = () -> pr.busy = false;
      Tasks.add(j);
      BlockPos moor = wt.offset(sea, 10);
      PORTS.put(k, moor);
      Chronicle.say(w.getServer(), k, "построило порт — начинается эпоха мореходства");
      return moor;
   }

   static BoatEntity boat(ServerWorld w, Kingdoms.K k, BlockPos at, boolean chest) {
      BoatEntity b = chest ? EntityType.CHEST_BOAT.create(w) : EntityType.BOAT.create(w);
      if (b == null) {
         return null;
      } else {
         int era = Civ.city(k).era;
         b.setVariant(
            era >= 6
               ? net.minecraft.entity.vehicle.BoatEntity.Type.DARK_OAK
               : (era >= 3 ? net.minecraft.entity.vehicle.BoatEntity.Type.SPRUCE : net.minecraft.entity.vehicle.BoatEntity.Type.OAK)
         );
         b.refreshPositionAndAngles(at.getX() + 0.5, at.getY() + 0.9, at.getZ() + 0.5, 0.0F, 0.0F);
         b.addCommandTag("gb_ship");
         w.spawnEntity(b);
         rig(w, b);
         return b;
      }
   }

   static boolean colonize(ServerWorld w, Kingdoms.K k, BlockPos target, List<MobEntity> settlers) {
      BlockPos moor = port(w, k);
      if (moor != null && !Seasons.frozen(k)) {
         List<BoatEntity> boats = new ArrayList<>();
         BoatEntity cur = null;
         int i = 0;

         for (MobEntity m : settlers) {
            if (cur == null || cur.getPassengerList().size() >= 2) {
               cur = boat(w, k, moor.add(i % 3 - 1, 0, i / 3), false);
               if (cur == null) {
                  break;
               }

               boats.add(cur);
               i++;
            }

            m.stopRiding();
            m.refreshPositionAndAngles(cur.getX(), cur.getY(), cur.getZ(), 0.0F, 0.0F);
            m.startRiding(cur, true);
         }

         BoatEntity cargo = boat(w, k, moor.add(1, 0, 1), true);
         if (cargo != null) {
            boats.add(cargo);
         }

         if (boats.isEmpty()) {
            return false;
         } else {
            Chronicle.say(w.getServer(), k, "отправляет корабли с колонистами за море");
            Tasks.add(Tech.sail(k, new Ships.Voyage(w, boats, target, moor.getY(), at -> {
               for (BoatEntity b : boats) {
                  for (Entity p : new ArrayList<>(b.getPassengerList())) {
                     p.stopRiding();
                     BlockPos g = Fx.surface(w, target.getX() + w.getRandom().nextInt(7) - 3, target.getZ() + w.getRandom().nextInt(7) - 3);
                     p.refreshPositionAndAngles(g.getX() + 0.5, g.getY(), g.getZ() + 0.5, 0.0F, 0.0F);
                     Fx.particles(w, ParticleTypes.POOF, g.getX() + 0.5, g.getY() + 1, g.getZ() + 0.5, 10, 0.3, 0.5, 0.3, 0.02);
                  }
               }

               Chronicle.say(w.getServer(), k, "колонисты высадились на новом берегу");
               Colonies.found(w, k, target);
               Tasks.delay(200, () -> boats.forEach(bx -> {
                  if (bx.isAlive() && !bx.hasPassengers()) {
                     bx.discard();
                  }
               }));
            })));
            return true;
         }
      } else {
         return false;
      }
   }

   // The ship model is drawn at twice size; its hull floor sits 5 model pixels below its centre.
   private static final float SHIP_SCALE = 2.0F;
   private static final double SHIP_LIFT = 0.6;

   private static void rig(ServerWorld w, BoatEntity b) {
      if (!RIGS.containsKey(b)) {
         ItemDisplayEntity d = Models.prop(w, "ship", "ship", b.getX(), b.getY() + SHIP_LIFT, b.getZ());
         if (d != null) {
            Models.pose(d, new Quaternionf(), SHIP_SCALE);
            d.setYaw(b.getYaw());
            RIGS.put(b, d);
         }
      }
   }

   private static void rigs(MinecraftServer s) {
      Iterator<Map.Entry<BoatEntity, ItemDisplayEntity>> it = RIGS.entrySet().iterator();

      while (it.hasNext()) {
         Map.Entry<BoatEntity, ItemDisplayEntity> e = it.next();
         BoatEntity b = e.getKey();
         ItemDisplayEntity d = e.getValue();
         if (b.isAlive() && d.isAlive() && b.getWorld() == d.getWorld()) {
            d.setPosition(b.getX(), b.getY() + SHIP_LIFT, b.getZ());
            d.setYaw(b.getYaw());
         } else {
            d.discard();
            it.remove();
         }
      }

      // Boats are saved with the world but their models are not, so re-rig the ones that lost theirs.
      if (t % 100 == 0) {
         for (ServerWorld w : s.getWorlds()) {
            for (BoatEntity b : w.getEntitiesByType(TypeFilter.instanceOf(BoatEntity.class), x -> x.isAlive() && x.getCommandTags().contains("gb_ship"))) {
               rig(w, b);
            }
         }
      }
   }

   public static void tick(MinecraftServer s) {
      t++;
      rigs(s);

      for (Kingdoms.K k : Kingdoms.K.values()) {
         BoatEntity b = FISH.get(k);
         BlockPos m = PORTS.get(k);
         if (b != null && m != null && b.isAlive() && b.getWorld() instanceof ServerWorld w) {
            double a = t * 0.012 + k.ordinal() * 1.5;
            double nx = m.getX() + 0.5 + Math.cos(a) * 7.0;
            double nz = m.getZ() + 0.5 + Math.sin(a) * 7.0;
            if (sea(w, new BlockPos((int)Math.floor(nx), m.getY(), (int)Math.floor(nz)))) {
               b.refreshPositionAndAngles(nx, m.getY() + 0.9, nz, (float)Math.toDegrees(a), 0.0F);
               b.setVelocity(Vec3d.ZERO);
            }
         }
      }

      if (on && t % 1200 == 0) {
         List<Kingdoms.K> withPort = new ArrayList<>();

         for (Kingdoms.K kx : Kingdoms.K.values()) {
            if (kx.alive) {
               ServerWorld var20 = kx.world;
               if (var20 instanceof ServerWorld) {
                  Civ.City var21 = Civ.city(kx);
                  BlockPos m = PORTS.get(kx);
                  if (m == null && Tech.has(kx, Tech.T.SAILING) && var21.wood >= 25) {
                     if (port(var20, kx) != null) {
                        var21.wood -= 20;
                     }
                  } else if (m != null) {
                     withPort.add(kx);
                     fisher(var20, kx, m, var21);
                  }
                  continue;
               }
            }

            PORTS.remove(kx);
            FISH.remove(kx);
         }

         if (withPort.size() >= 2 && t % 2400 == 0) {
            trade(withPort);
         }
      }
   }

   private static void fisher(ServerWorld w, Kingdoms.K k, BlockPos m, Civ.City c) {
      BoatEntity b = FISH.get(k);
      if (b == null || !b.isAlive()) {
         String tag = "gb_fish_" + k.name();
         List<BoatEntity> old = w.getEntitiesByClass(BoatEntity.class, new Box(m).expand(48.0), x -> x.getCommandTags().contains(tag));
         if (!old.isEmpty()) {
            b = old.get(0);
         } else {
            b = boat(w, k, m, false);
            if (b == null) {
               return;
            }

            b.addCommandTag(tag);
            MobEntity f = Kingdoms.spawn(k, EntityType.VILLAGER, m.up());
            if (f != null) {
               f.startRiding(b, true);
            }
         }

         FISH.put(k, b);
      }

      if (b.hasPassengers()) {
         c.food += 4;
         Fx.particles(w, ParticleTypes.FISHING, b.getX(), b.getY() + 0.5, b.getZ(), 12, 1.5, 0.1, 1.5, 0.05);
      }
   }

   private static void trade(List<Kingdoms.K> ks) {
      Kingdoms.K a = ks.get(t / 2400 % ks.size());
      Kingdoms.K b = ks.get((t / 2400 + 1) % ks.size());
      if (a != b && Diplomacy.rel(a, b) != 1 && !Seasons.frozen(a) && !Seasons.frozen(b)) {
         ServerWorld from = a.world;
         if (from instanceof ServerWorld) {
            BlockPos var8 = PORTS.get(a);
            BlockPos to = PORTS.get(b);
            if (var8 != null && to != null) {
               if (Plague.blocked(a, b)) {
                  return;
               }

               BoatEntity ship = boat(from, a, var8, true);
               if (ship == null) {
                  return;
               }

               Plague.depart(a, b);
               List<BoatEntity> l = new ArrayList<>();
               l.add(ship);
               Tasks.add(Tech.sail(a, new Ships.Voyage(from, l, to, var8.getY(), at -> {
                  Civ.City ca = Civ.city(a);
                  Civ.City cb = Civ.city(b);
                  ca.food += 10;
                  ca.wood += 8;
                  cb.food += 10;
                  cb.ore += 4;
                  Chronicle.log("Торговое судно " + a.title + " прибыло в порт " + b.title);
                  Plague.arrive(a, b, 0.5, "с торговым судном из");
                  Tasks.delay(100, ship::discard);
               })));
               return;
            }

            return;
         }
      }
   }

   public static void clear() {
      RIGS.values().forEach(Entity::discard);
      RIGS.clear();
      PORTS.clear();
      FISH.clear();
      t = 0;
   }

   static final class Voyage implements Tasks.Task {
      final ServerWorld w;
      final List<BoatEntity> boats;
      final BlockPos target;
      final Consumer<BlockPos> arrive;
      final int y;
      int t;
      double speed = 0.3;

      Voyage(ServerWorld w, List<BoatEntity> boats, BlockPos target, int y, Consumer<BlockPos> arrive) {
         this.w = w;
         this.boats = boats;
         this.target = target;
         this.y = y;
         this.arrive = arrive;
      }

      boolean ok(double x, double z) {
         return Ships.sea(this.w, new BlockPos((int)Math.floor(x), this.y, (int)Math.floor(z)));
      }

      void move(BoatEntity b, double nx, double nz, double a) {
         b.refreshPositionAndAngles(nx, this.y + 0.9, nz, (float)Math.toDegrees(a) - 90.0F, 0.0F);
         b.setVelocity(Vec3d.ZERO);
      }

      @Override
      public boolean tick() {
         this.t++;
         this.boats.removeIf(bx -> !bx.isAlive());
         if (this.boats.isEmpty()) {
            return true;
         } else {
            BoatEntity lead = this.boats.get(0);
            double dx = this.target.getX() + 0.5 - lead.getX();
            double dz = this.target.getZ() + 0.5 - lead.getZ();
            if (!(dx * dx + dz * dz < 9.0) && this.t <= 4800) {
               double base = Math.atan2(dz, dx);
               double sp = this.speed;
               double[] off = new double[]{0.0, 0.5, -0.5, 1.0, -1.0, 1.5, -1.5};
               Double dir = null;

               for (double o : off) {
                  double a = base + o;
                  if (this.ok(lead.getX() + Math.cos(a) * 2.5, lead.getZ() + Math.sin(a) * 2.5)) {
                     dir = a;
                     break;
                  }
               }

               if (dir == null) {
                  this.arrive.accept(lead.getBlockPos());
                  return true;
               } else {
                  double cx = Math.cos(dir);
                  double cz = Math.sin(dir);
                  this.move(lead, lead.getX() + cx * sp, lead.getZ() + cz * sp, dir);

                  for (int i = 1; i < this.boats.size(); i++) {
                     BoatEntity b = this.boats.get(i);
                     double tx = lead.getX() - cx * 3.0 * i + (i % 2 == 0 ? 1.5 : -1.5) * cz;
                     double tz = lead.getZ() - cz * 3.0 * i - (i % 2 == 0 ? 1.5 : -1.5) * cx;
                     double ex = tx - b.getX();
                     double ez = tz - b.getZ();
                     double d = Math.sqrt(ex * ex + ez * ez);
                     if (d > 0.05) {
                        double st = Math.min(d, sp * 1.3);
                        double nx = b.getX() + ex / d * st;
                        double nz = b.getZ() + ez / d * st;
                        if (this.ok(nx, nz) || d > 12.0) {
                           this.move(b, nx, nz, Math.atan2(ez, ex));
                        }
                     }
                  }

                  if (this.t % 6 == 0) {
                     Fx.particles(this.w, ParticleTypes.SPLASH, lead.getX() - cx, this.y + 1, lead.getZ() - cz, 6, 0.3, 0.1, 0.3, 0.05);
                  }

                  return false;
               }
            } else {
               this.arrive.accept(lead.getBlockPos());
               return true;
            }
         }
      }
   }
}
