package com.vladik.godbox;

import net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

/** Bombs that visibly fall from the sky before they go off. */
final class Drops {
   private static final int FALL_TICKS = 12;
   private static final double HEIGHT = 16.0;

   private Drops() {
   }

   /**
    * Drops the bomb model onto {@code c} and then runs {@code impact}. Without custom models the impact runs at once, as it
    * did before bombs had a model.
    */
   static void bomb(ServerWorld w, Vec3d c, String part, float scale, Runnable impact) {
      ItemDisplayEntity e = Models.prop(w, "bombs", part, c.x, c.y + HEIGHT, c.z);
      if (e == null) {
         impact.run();
      } else {
         float spin = w.getRandom().nextFloat() * 6.28F;
         Models.pose(e, new Quaternionf().rotationY(spin), scale);
         Fx.sound(w, c.x, c.y + 6.0, c.z, SoundEvents.ENTITY_ARROW_SHOOT, 1.5F, 0.5F);
         int[] t = new int[]{0};
         Tasks.add(() -> {
            t[0]++;
            // Ease in so the bomb accelerates like a falling object.
            double k = (double)t[0] / FALL_TICKS;
            e.setPosition(c.x, c.y + HEIGHT * (1.0 - k * k) + 0.5 * scale, c.z);
            Models.pose(e, new Quaternionf().rotationY(spin + t[0] * 0.25F), scale);
            if (t[0] < FALL_TICKS) {
               return false;
            } else {
               e.discard();
               impact.run();
               return true;
            }
         });
      }
   }
}
