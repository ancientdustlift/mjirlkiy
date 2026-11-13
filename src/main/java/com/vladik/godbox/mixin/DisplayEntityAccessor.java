package com.vladik.godbox.mixin;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.decoration.DisplayEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({DisplayEntity.class})
public interface DisplayEntityAccessor {
   @Accessor("START_INTERPOLATION")
   static TrackedData<Integer> godbox$startInterpolation() {
      throw new AssertionError();
   }
}
