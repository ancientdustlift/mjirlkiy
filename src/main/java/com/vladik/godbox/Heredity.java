package com.vladik.godbox;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.random.Random;

public final class Heredity {
   private Heredity() {
   }

   public static void inherit(MobEntity parent, MobEntity child) {
      Random r = child.getRandom();
      if (r.nextInt(100) < 70) {
         Life.setTag(child, "gbt:", Integer.toString(Life.trait(parent)));
      }

      for (String t : Traits.IDS) {
         if (Traits.has(parent, t) && !Traits.has(child, t)) {
            int chance = switch (t) {
               case "immortal" -> 10;
               case "cursed" -> 35;
               case "giant", "tiny" -> 40;
               default -> 50;
            };
            if (r.nextInt(100) < chance) {
               Traits.apply(child, t);
            }
         }
      }

      if (r.nextInt(100) < 4) {
         String[] pool = new String[]{"strong", "fast", "tough", "regen", "fireproof"};
         String tx = pool[r.nextInt(pool.length)];
         if (!Traits.has(child, tx)) {
            Traits.apply(child, tx);
         }
      }
   }
}
