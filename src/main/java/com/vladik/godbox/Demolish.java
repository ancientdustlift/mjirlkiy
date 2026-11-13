package com.vladik.godbox;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap.Type;

public final class Demolish {
   static final int FLAGS = 35;

   private Demolish() {
   }

   static boolean natural(ServerWorld w, BlockPos p, BlockState s) {
      if (s.isAir() || !s.getFluidState().isEmpty() || s.isOf(Blocks.BEDROCK)) {
         return true;
      } else if (s.isIn(BlockTags.BASE_STONE_OVERWORLD)
         || s.isIn(BlockTags.DIRT)
         || s.isIn(BlockTags.LEAVES)
         || s.isIn(BlockTags.FLOWERS)
         || s.isIn(BlockTags.SAPLINGS)
         || s.isIn(BlockTags.SAND)
         || s.isIn(BlockTags.ICE)
         || s.isIn(BlockTags.SNOW)) {
         return true;
      } else if (s.isOf(Blocks.GRAVEL)
         || s.isOf(Blocks.CLAY)
         || s.isOf(Blocks.SANDSTONE)
         || s.isOf(Blocks.TERRACOTTA)
         || s.isOf(Blocks.CACTUS)
         || s.isOf(Blocks.SUGAR_CANE)
         || s.isOf(Blocks.VINE)
         || s.isOf(Blocks.DEAD_BUSH)
         || s.isOf(Blocks.SEAGRASS)
         || s.isOf(Blocks.KELP)
         || s.isOf(Blocks.KELP_PLANT)
         || s.isOf(Blocks.SNOW_BLOCK)
         || Ores.isOre(s)) {
         return true;
      } else if (s.isReplaceable() && !s.isOf(Blocks.FIRE)) {
         return true;
      } else {
         if (s.isIn(BlockTags.LOGS)) {
            for (BlockPos q : BlockPos.iterate(p.add(-2, -1, -2), p.add(2, 3, 2))) {
               if (w.getBlockState(q).isIn(BlockTags.LEAVES)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   public static void one(ServerPlayerEntity god, ServerWorld w, BlockPos pos, int brush, boolean ruins) {
      for (Kingdoms.K k : Kingdoms.K.values()) {
         if (k.alive && k.world == w) {
            Civ.City c = Civ.city(k);

            for (Civ.Building b : new ArrayList<>(c.buildings)) {
               if (b.inside(pos.getX(), pos.getZ(), 1)) {
                  forget(c, b);
                  collapse(w, b.x0 - 1, b.z0 - 1, b.x1 + 1, b.z1 + 1, b.y0, b.y0 + 18, ruins, false);
                  god.sendMessage(
                     Text.literal("\ud83c\udfda " + b.kind.title + " (" + k.title + ") " + (ruins ? "превращена в руины" : "разрушена")).formatted(k.color),
                     true
                  );
                  return;
               }
            }
         }
      }

      int r = ruins ? 3 + brush : 5;
      int y = w.getTopY(Type.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
      int n = collapse(
         w, pos.getX() - r, pos.getZ() - r, pos.getX() + r, pos.getZ() + r, Math.min(y, pos.getY()) - 2, Math.max(y, pos.getY()) + 20, ruins, true
      );
      god.sendMessage(Text.literal(n > 0 ? "\ud83c\udfda Постройки разрушены (" + n + " блоков)" : "Здесь нечего сносить").formatted(Formatting.GRAY), true);
   }

   public static void area(ServerPlayerEntity god, ServerWorld w, BlockPos pos, int brush) {
      int r = 4 + brush * 3;
      int cnt = 0;

      for (Kingdoms.K k : Kingdoms.K.values()) {
         if (k.alive && k.world == w) {
            Civ.City c = Civ.city(k);

            for (Civ.Building b : new ArrayList<>(c.buildings)) {
               if (b.inside(pos.getX(), pos.getZ(), r)) {
                  forget(c, b);
                  cnt++;
               }
            }
         }
      }

      int y = w.getTopY(Type.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
      int n = collapse(
         w, pos.getX() - r, pos.getZ() - r, pos.getX() + r, pos.getZ() + r, Math.min(y, pos.getY()) - 4, Math.max(y, pos.getY()) + 24, false, true
      );
      god.sendMessage(Text.literal("\ud83c\udfda Снесено: зданий " + cnt + ", блоков " + n).formatted(Formatting.GRAY), true);
   }

   static void forget(Civ.City c, Civ.Building b) {
      c.buildings.remove(b);
      if (c.project != null && c.project.b == b) {
         c.project = null;
      }

      if (c.quarry == b) {
         c.quarry = null;
      }
   }

   static int collapse(ServerWorld w, int x0, int z0, int x1, int z1, int y0, int y1, boolean ruins, boolean onlyManMade) {
      final List<List<BlockPos>> layers = new ArrayList<>();
      int total = 0;
      int bottom = onlyManMade ? y0 : y0 + 1;

      for (int y = y1; y >= bottom; y--) {
         List<BlockPos> layer = new ArrayList<>();

         for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
               BlockPos p = new BlockPos(x, y, z);
               BlockState s = w.getBlockState(p);
               if (!s.isAir()
                  && (s.getFluidState().isEmpty() || !s.isOf(Blocks.WATER))
                  && (onlyManMade ? !natural(w, p, s) : !s.isIn(BlockTags.LEAVES) && !s.isOf(Blocks.BEDROCK))) {
                  layer.add(p);
               }
            }
         }

         if (!layer.isEmpty()) {
            layers.add(layer);
            total += layer.size();
         }
      }

      if (total == 0) {
         return 0;
      } else {
         final double mx = (x0 + x1) / 2.0 + 0.5;
         final double mz = (z0 + z1) / 2.0 + 0.5;
         final int my = y0 + 2;
         Fx.sound(w, mx, my, mz, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 0.7F, 0.6F);
         Tasks.add(new Tasks.Task() {
            int i;
            int t;

            @Override
            public boolean tick() {
               if (++this.t % 2 != 0) {
                  return false;
               } else if (this.i >= layers.size()) {
                  Fx.particles(w, ParticleTypes.CAMPFIRE_COSY_SMOKE, mx, my, mz, 20, (x1 - x0) / 3.0, 1.0, (z1 - z0) / 3.0, 0.02);
                  return true;
               } else {
                  Random r = w.getRandom();
                  List<BlockPos> layer = layers.get(this.i++);
                  double keep = ruins ? 0.15 + 0.7 * ((double)this.i / layers.size()) : 0.0;
                  int n = 0;

                  for (BlockPos p : layer) {
                     BlockState s = w.getBlockState(p);
                     if (!s.isAir()) {
                        if (ruins && r.nextDouble() < keep) {
                           if (s.isOf(Blocks.COBBLESTONE)) {
                              w.setBlockState(p, Blocks.MOSSY_COBBLESTONE.getDefaultState(), 35);
                           } else if (s.isOf(Blocks.STONE_BRICKS)) {
                              w.setBlockState(p, (r.nextBoolean() ? Blocks.MOSSY_STONE_BRICKS : Blocks.CRACKED_STONE_BRICKS).getDefaultState(), 35);
                           } else if (s.isIn(BlockTags.PLANKS) && r.nextInt(3) == 0) {
                              w.setBlockState(p, Blocks.AIR.getDefaultState(), 35);
                           }
                        } else if (s.isOf(Blocks.FARMLAND)) {
                           w.setBlockState(p, Blocks.DIRT.getDefaultState(), 35);
                        } else if (!ruins && r.nextInt(9) == 0 && s.isFullCube(w, p) && n < 12) {
                           FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(w, p, s);
                           fb.dropItem = false;
                           n++;
                        } else {
                           if (r.nextInt(5) == 0) {
                              Fx.particles(w, ParticleTypes.POOF, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 1, 0.2, 0.2, 0.2, 0.01);
                           }

                           w.setBlockState(p, Blocks.AIR.getDefaultState(), 35);
                        }
                     }
                  }

                  BlockPos c0 = layer.get(0);
                  Fx.sound(w, c0.getX(), c0.getY(), c0.getZ(), SoundEvents.BLOCK_STONE_BREAK, 1.0F, 0.6F + r.nextFloat() * 0.4F);
                  if (ruins && this.i >= layers.size()) {
                     for (BlockPos px : layer) {
                        BlockPos up = px.up();
                        if (w.getBlockState(up).isAir() && w.getBlockState(px).isSolidBlock(w, px) && r.nextInt(6) == 0) {
                           w.setBlockState(up, (r.nextBoolean() ? Blocks.MOSS_CARPET : Blocks.COBWEB).getDefaultState(), 35);
                        }
                     }
                  }

                  return false;
               }
            }
         });
         return total;
      }
   }
}
