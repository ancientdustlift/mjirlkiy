package com.vladik.godbox;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
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

public final class Navy {
   public static boolean on = true;
   static final List<MobEntity> KRAKENS = new ArrayList<>();
   private static int t;

   private Navy() {
   }

   public static void tick(MinecraftServer s) {
      t++;
      if (t % 20 == 0) {
         krakens();
      }

      if (t % 12000 == 0 && KRAKENS.isEmpty()) {
         wildKraken(s);
      }

      if (on && t % 1200 == 0) {
         for (Kingdoms.K a : Kingdoms.K.values()) {
            if (a.alive) {
               ServerWorld var6 = a.world;
               if (var6 instanceof ServerWorld) {
                  ServerWorld w = var6;
                  if (Tech.has(a, Tech.T.NAVIGATION) && Ships.PORTS.get(a) != null) {
                     for (Kingdoms.K b : Kingdoms.K.values()) {
                        if (a != b && b.alive && b.world == a.world && Diplomacy.rel(a, b) == 1) {
                           BlockPos target = Ships.PORTS.get(b);
                           if (target != null && !Seasons.frozen(a) && w.getRandom().nextInt(2) == 0) {
                              fleet(w, a, b, target);
                              break;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   static void fleet(ServerWorld w, Kingdoms.K a, Kingdoms.K b, BlockPos target) {
      BlockPos moor = Ships.port(w, a);
      if (moor != null) {
         Random r = w.getRandom();
         List<MobEntity> army = new ArrayList<>();

         for (MobEntity m : a.members) {
            if (m.isAlive() && !(m instanceof VillagerEntity) && !m.hasVehicle() && !m.getCommandTags().contains("gb_king") && army.size() < 6) {
               army.add(m);
            }
         }

         Civ.City c = Civ.city(a);

         while (army.size() < 4 && c.wood >= 8) {
            c.wood -= 8;
            MobEntity mx = Kingdoms.spawn(a, Kingdoms.soldier(r), moor.up());
            if (mx == null) {
               break;
            }

            army.add(mx);
         }

         if (army.size() >= 2) {
            List<BoatEntity> boats = new ArrayList<>();
            BoatEntity cur = null;
            int i = 0;

            for (MobEntity mx : army) {
               if (cur == null || cur.getPassengerList().size() >= 2) {
                  cur = Ships.boat(w, a, moor.add(i % 3 - 1, 0, i / 3), false);
                  if (cur == null) {
                     break;
                  }

                  boats.add(cur);
                  i++;
               }

               mx.stopRiding();
               mx.refreshPositionAndAngles(cur.getX(), cur.getY(), cur.getZ(), 0.0F, 0.0F);
               mx.startRiding(cur, true);
            }

            if (!boats.isEmpty()) {
               Chronicle.say(w.getServer(), a, "отправляет военный флот к берегам «" + b.title + "»");
               int gun = Tech.broadside(a);
               Tasks.add(
                  Tech.sail(
                     a,
                     new Ships.Voyage(
                        w,
                        boats,
                        target,
                        moor.getY(),
                        at -> {
                           for (int k = 0; k < 3 + gun * 2; k++) {
                              int d = 10 + k * 15;
                              Tasks.delay(
                                 d,
                                 () -> {
                                    BlockPos p = Fx.surface(
                                       w, target.getX() + r.nextInt(17 + gun * 12) - 8 - gun * 6, target.getZ() + r.nextInt(17 + gun * 12) - 8 - gun * 6
                                    );

                                    for (BoatEntity bt : boats) {
                                       if (bt.isAlive()) {
                                          Fx.particles(w, ParticleTypes.LARGE_SMOKE, bt.getX(), bt.getY() + 1.0, bt.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
                                       }
                                    }

                                    Fx.sound(w, at.getX(), at.getY(), at.getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 2.0F, 0.7F);
                                    w.createExplosion(null, p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 2.0F + gun, ExplosionSourceType.TNT);
                                 }
                              );
                           }

                           Tasks.delay(60, () -> {
                              BlockPos land = TownBuilder.landOr(w, target);

                              for (BoatEntity bt : boats) {
                                 for (Entity p : new ArrayList<>(bt.getPassengerList())) {
                                    p.stopRiding();
                                    BlockPos g = Fx.surface(w, land.getX() + r.nextInt(5) - 2, land.getZ() + r.nextInt(5) - 2);
                                    p.refreshPositionAndAngles(g.getX() + 0.5, g.getY(), g.getZ() + 0.5, 0.0F, 0.0F);
                                 }
                              }

                              Chronicle.say(w.getServer(), a, "высадило десант в порту «" + b.title + "»");
                              Plague.carry(a, b, 0.5, "с военным флотом из");
                              Tasks.delay(200, () -> boats.forEach(btx -> {
                                 if (btx.isAlive() && !btx.hasPassengers()) {
                                    btx.discard();
                                 }
                              }));
                           });
                        }
                     )
                  )
               );
            }
         }
      }
   }

   static MobEntity kraken(ServerWorld w, BlockPos at) {
      MobEntity m = EntityType.ELDER_GUARDIAN.create(w);
      if (m == null) {
         return null;
      } else {
         m.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
         m.addCommandTag("gb_kraken");
         m.setCustomName(Text.literal("Кракен").formatted(Formatting.DARK_AQUA, Formatting.BOLD));
         m.setCustomNameVisible(true);
         EntityAttributeInstance sc = m.getAttributeInstance(EntityAttributes.GENERIC_SCALE);
         if (sc != null) {
            sc.setBaseValue(3.0);
         }

         EntityAttributeInstance hp = m.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
         if (hp != null) {
            hp.setBaseValue(320.0);
            m.setHealth(320.0F);
         }

         m.setPersistent();
         w.spawnEntity(m);
         Models.attachCustom(m, "kraken");
         KRAKENS.add(m);
         Fx.sound(w, at.getX(), at.getY(), at.getZ(), SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 3.0F, 0.5F);
         Fx.particles(w, ParticleTypes.BUBBLE_COLUMN_UP, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 80, 3.0, 2.0, 3.0, 0.1);
         return m;
      }
   }

   static void krakens() {
      KRAKENS.removeIf(mx -> !mx.isAlive());

      for (MobEntity m : new ArrayList<>(KRAKENS)) {
         if (m.getWorld() instanceof ServerWorld w) {
            Random r = w.getRandom();

            for (BoatEntity b : w.getEntitiesByClass(BoatEntity.class, m.getBoundingBox().expand(12.0), Entity::isAlive)) {
               if (r.nextInt(3) == 0) {
                  for (Entity p : new ArrayList<>(b.getPassengerList())) {
                     p.stopRiding();
                  }

                  Fx.particles(w, ParticleTypes.SPLASH, b.getX(), b.getY() + 1.0, b.getZ(), 40, 1.0, 0.5, 1.0, 0.2);
                  Fx.sound(w, b.getX(), b.getY(), b.getZ(), SoundEvents.ENTITY_GENERIC_SPLASH, 2.0F, 0.6F);
                  b.discard();
               }
            }

            for (LivingEntity e : w.getEntitiesByClass(
               LivingEntity.class,
               m.getBoundingBox().expand(16.0),
               ex -> ex != m && ex.isAlive() && ex.isTouchingWater() && !ex.getCommandTags().contains("gb_kraken") && ex.getType() != EntityType.GUARDIAN
            )) {
               Vec3d d = m.getPos().subtract(e.getPos());
               if (d.lengthSquared() > 0.01) {
                  e.addVelocity(d.normalize().multiply(0.35));
               }

               e.velocityModified = true;
               if (d.lengthSquared() < 36.0) {
                  e.damage(w.getDamageSources().mobAttack(m), 6.0F);
               }
            }

            if (t % 200 == 0) {
               int g = w.getEntitiesByClass(MobEntity.class, m.getBoundingBox().expand(20.0), e -> e.getType() == EntityType.GUARDIAN).size();
               if (g < 4) {
                  MobEntity gu = EntityType.GUARDIAN.create(w);
                  if (gu != null) {
                     gu.refreshPositionAndAngles(m.getX() + r.nextInt(7) - 3.0, m.getY(), m.getZ() + r.nextInt(7) - 3.0, 0.0F, 0.0F);
                     w.spawnEntity(gu);
                  }
               }
            }
         }
      }
   }

   static void wildKraken(MinecraftServer s) {
      List<BlockPos> ports = new ArrayList<>(Ships.PORTS.values());
      ServerWorld w = s.getOverworld();
      if (ports.size() >= 2 && w.getRandom().nextInt(3) == 0) {
         BlockPos a = ports.get(0);
         BlockPos b = ports.get(1 + w.getRandom().nextInt(ports.size() - 1));
         int x = (a.getX() + b.getX()) / 2;
         int z = (a.getZ() + b.getZ()) / 2;
         if (w.isChunkLoaded(x >> 4, z >> 4)) {
            BlockPos top = Fx.surface(w, x, z);
            if (!w.getFluidState(top.down()).isEmpty()) {
               if (kraken(w, top.down(2)) != null) {
                  Chronicle.log("Из глубин на торговом пути поднялся Кракен!");
               }
            }
         }
      }
   }

   public static void power(ServerPlayerEntity god, ServerWorld w, BlockPos pos) {
      BlockPos top = Fx.surface(w, pos.getX(), pos.getZ());
      if (w.getFluidState(top.down()).isEmpty()) {
         god.sendMessage(Text.literal("Кракен живёт только в воде").formatted(Formatting.RED), true);
      } else {
         kraken(w, top.down(2));
         god.sendMessage(Text.literal("Из глубин поднимается Кракен!").formatted(Formatting.DARK_AQUA), true);
      }
   }

   public static void clear() {
      KRAKENS.clear();
      t = 0;
   }
}
