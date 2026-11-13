package com.vladik.godbox;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;

public final class Kings {
   static final String TAG = "gb_king";
   static final Map<Kingdoms.K, UUID> KING = new EnumMap<>(Kingdoms.K.class);
   private static final DustParticleEffect GOLD = new DustParticleEffect(new Vector3f(1.0F, 0.82F, 0.1F), 1.0F);
   private static int t;

   private Kings() {
   }

   static MobEntity king(Kingdoms.K k) {
      for (MobEntity m : k.members) {
         if (m.isAlive() && m.getCommandTags().contains("gb_king")) {
            return m;
         }
      }

      return null;
   }

   public static String kingName(Kingdoms.K k) {
      MobEntity m = king(k);
      if (m == null) {
         return "нет";
      } else {
         return m.hasCustomName() ? m.getCustomName().getString() : "безымянный";
      }
   }

   public static void tick(MinecraftServer s) {
      t++;
      if (t % 10 == 0) {
         for (Kingdoms.K k : Kingdoms.K.values()) {
            if (k.alive) {
               ServerWorld m = k.world;
               if (m instanceof ServerWorld) {
                  MobEntity mx = king(k);
                  if (mx != null) {
                     Fx.particles(m, GOLD, mx.getX(), mx.getY() + mx.getHeight() + 0.35, mx.getZ(), 4, 0.18, 0.04, 0.18, 0.0);
                  }
               }
            }
         }
      }

      if (t % 100 == 0) {
         for (Kingdoms.K kx : Kingdoms.K.values()) {
            if (kx.alive) {
               ServerWorld var17 = kx.world;
               if (var17 instanceof ServerWorld) {
                  MobEntity m = king(kx);
                  if (m != null) {
                     KING.put(kx, m.getUuid());
                     Dynasty.ensure(s, kx, var17, m);
                  } else {
                     if (KING.containsKey(kx)) {
                        KING.remove(kx);
                        MobEntity heir = Dynasty.heir(kx);
                        if (heir != null) {
                           Dynasty.dispute(s, kx, var17, heir);
                           crown(s, kx, var17);
                           continue;
                        }

                        if (Treaties.succession(s, kx, var17)) {
                           continue;
                        }

                        Chronicle.say(s, kx, "лишилось короля, а наследников нет! В королевстве смута");
                        Civ.City c = Civ.city(kx);
                        c.food = c.food * 2 / 3;
                        List<BlockPos> towns = Conquest.TOWNS.get(kx);
                        if (towns != null && !towns.isEmpty() && var17.getRandom().nextInt(3) == 0) {
                           BlockPos p = towns.get(var17.getRandom().nextInt(towns.size()));
                           if (var17.isChunkLoaded(p)) {
                              Rebels.rebel(var17, kx, p);
                           }
                        }
                     }

                     crown(s, kx, var17);
                  }
                  continue;
               }
            }

            KING.remove(kx);
         }
      }
   }

   static void crown(MinecraftServer s, Kingdoms.K k, ServerWorld w) {
      MobEntity m = Dynasty.pick(k);
      if (m != null) {
         m.addCommandTag("gb_king");
         Traits.apply(m, "tough");
         Traits.apply(m, "regen");
         KING.put(k, m.getUuid());
         Fx.particles(w, GOLD, m.getX(), m.getY() + 2.0, m.getZ(), 40, 0.6, 0.6, 0.6, 0.0);
         Fx.sound(w, m.getX(), m.getY(), m.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.5F, 1.0F);
         Dynasty.crowned(s, k, w, m);
      }
   }

   public static void clear() {
      KING.clear();
      t = 0;
   }
}
