package com.vladik.godbox;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SaplingBlock;
import net.minecraft.block.SnowyBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap.Type;

public final class WorldGen implements Tasks.Task {
   static final String[] TYPES = new String[]{"continent", "islands", "archipelago", "pangaea", "mountains"};
   static final int FLAGS = 50;
   private static int running;
   final ServerPlayerEntity god;
   final ServerWorld w;
   final int cx;
   final int cz;
   final int R;
   final int side;
   final int total;
   final int sea;
   final String type;
   final long seed;
   final Random rnd;
   final List<long[]> trees = new ArrayList<>();
   int i;
   int ti;
   int ticks;
   int lastPct = -1;

   WorldGen(ServerPlayerEntity god, ServerWorld w, BlockPos c, int brush, String type) {
      this.god = god;
      this.w = w;
      this.cx = c.getX();
      this.cz = c.getZ();
      this.R = 24 + brush * 12;
      this.side = this.R * 2 + 1;
      this.total = this.side * this.side;
      this.sea = w.getSeaLevel();
      this.rnd = Random.create();
      this.seed = this.rnd.nextLong();
      this.type = type != null && !type.equals("random") ? type : TYPES[this.rnd.nextInt(TYPES.length)];
   }

   public static void start(ServerPlayerEntity god, ServerWorld w, BlockPos pos, int brush, String type) {
      if (running >= 2) {
         god.sendMessage(Text.literal("Подожди — мир ещё создаётся").formatted(Formatting.YELLOW), true);
      } else {
         WorldGen g = new WorldGen(god, w, pos, Math.max(1, Math.min(10, brush)), type);
         running++;
         Tasks.add(g);
         Fx.sound(w, pos.getX(), pos.getY() + 5, pos.getZ(), SoundEvents.BLOCK_END_PORTAL_SPAWN, 1.5F, 1.2F);
         god.sendMessage(Text.literal("\ud83c\udf0d Сотворение мира: " + title(g.type) + ", радиус " + g.R).formatted(Formatting.AQUA), false);
      }
   }

   static String title(String t) {
      return switch (t) {
         case "islands" -> "Острова";
         case "archipelago" -> "Архипелаг";
         case "pangaea" -> "Пангея";
         case "mountains" -> "Горный край";
         default -> "Континент";
      };
   }

   static double hash(long seed, int x, int z) {
      long h = seed ^ x * -7046029254386353131L ^ z * -4417276706812531889L;
      h = (h ^ h >>> 30) * -4658895280553007687L;
      h = (h ^ h >>> 27) * -7723592293110705685L;
      h ^= h >>> 31;
      return (h >>> 11) * 1.110223E-16F * 2.0 - 1.0;
   }

   static double noise(long seed, double x, double z) {
      int x0 = MathHelper.floor(x);
      int z0 = MathHelper.floor(z);
      double fx = x - x0;
      double fz = z - z0;
      double u = fx * fx * (3.0 - 2.0 * fx);
      double v = fz * fz * (3.0 - 2.0 * fz);
      double a = hash(seed, x0, z0);
      double b = hash(seed, x0 + 1, z0);
      double c = hash(seed, x0, z0 + 1);
      double d = hash(seed, x0 + 1, z0 + 1);
      return MathHelper.lerp(v, MathHelper.lerp(u, a, b), MathHelper.lerp(u, c, d));
   }

   static double fbm(long seed, double x, double z, int oct) {
      double s = 0.0;
      double amp = 1.0;
      double f = 1.0;
      double norm = 0.0;

      for (int o = 0; o < oct; o++) {
         s += noise(seed + o * 131L, x * f, z * f) * amp;
         norm += amp;
         amp *= 0.5;
         f *= 2.0;
      }

      return s / norm;
   }

   double elevation(int x, int z, double dn) {
      double n = fbm(this.seed, x / 48.0, z / 48.0, 5);
      String var7 = this.type;

      return switch (var7) {
         case "islands" -> fbm(this.seed + 3L, x / 26.0, z / 26.0, 4) * 1.3 + 0.05 - dn * 0.7;
         case "archipelago" -> fbm(this.seed + 5L, x / 13.0, z / 13.0, 3) * 1.4 - 0.25 - dn * 0.3;
         case "pangaea" -> 0.35 + n * 0.55 - Math.pow(dn, 4.0) * 1.3;
         case "mountains" -> {
            double rr = 1.0 - Math.abs(fbm(this.seed + 7L, x / 40.0, z / 40.0, 4));
            yield 0.15 + rr * rr * 1.2 + n * 0.35 - dn * dn * 0.9;
         }
         default -> 0.4 + n * 0.8 - dn * dn * 1.0;
      };
   }

   int height(double e) {
      return e >= 0.0
         ? this.sea + 1 + (int)(e * (this.type.equals("mountains") ? 62 : 30))
         : Math.max(this.w.getBottomY() + 6, this.sea + (int)Math.floor(e * 26.0) - 1);
   }

   @Override
   public boolean tick() {
      if (this.god.isRemoved() && this.i == 0) {
         running--;
         return true;
      } else {
         this.ticks++;
         long until = System.nanoTime() + 22000000L;
         if (this.i < this.total) {
            while (this.i < this.total && System.nanoTime() < until) {
               for (int k = 0; k < 16 && this.i < this.total; this.i++) {
                  this.column(this.cx - this.R + this.i % this.side, this.cz - this.R + this.i / this.side);
                  k++;
               }
            }
         } else {
            while (this.ti < this.trees.size() && System.nanoTime() < until) {
               long[] t = this.trees.get(this.ti++);
               this.tree(new BlockPos((int)t[0], (int)t[1], (int)t[2]), (int)t[3]);
            }

            if (this.ti >= this.trees.size()) {
               running--;
               Fx.sound(this.w, this.cx, this.sea + 10, this.cz, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
               this.god
                  .sendMessage(
                     Text.literal("\ud83c\udf0d Мир создан: " + title(this.type) + "! Засели его людьми и королевствами").formatted(Formatting.GREEN), true
                  );
               return true;
            }
         }

         int pct = (int)(100L * (this.i + this.ti) / Math.max(1, this.total + this.trees.size()));
         if (this.ticks % 10 == 0 && pct != this.lastPct) {
            this.lastPct = pct;
            this.god.sendMessage(Text.literal("\ud83c\udf0d Генерация мира: " + pct + "%").formatted(Formatting.AQUA), true);
         }

         return false;
      }
   }

   void put(Mutable m, BlockState s) {
      if (this.w.getBlockState(m) != s) {
         this.w.setBlockState(m, s, 50);
      }
   }

   void column(int x, int z) {
      double dx = x - this.cx;
      double dz = z - this.cz;
      double dn = Math.sqrt(dx * dx + dz * dz) / this.R;
      if (!(dn > 1.0)) {
         if (this.w.isChunkLoaded(x >> 4, z >> 4)) {
            int oldTop = this.w.getTopY(Type.MOTION_BLOCKING, x, z);
            int oldGround = this.w.getTopY(Type.OCEAN_FLOOR, x, z) - 1;
            int h = this.height(this.elevation(x, z, dn));
            if (dn > 0.82) {
               double t = (dn - 0.82) / 0.18;
               t = t * t * (3.0 - 2.0 * t);
               h = (int)Math.round(MathHelper.lerp(t, (double)h, (double)oldGround));
            }

            int maxY = this.w.getTopY() - 2;
            h = Math.min(h, maxY - 12);
            double temp = fbm(this.seed + 99L, x / 90.0, z / 90.0, 3);
            boolean cold = temp < -0.3;
            boolean hot = temp > 0.3;
            int top = Math.min(maxY, Math.max(oldTop + 1, Math.max(h, this.sea) + 1));
            Mutable m = new Mutable();

            for (int y = top; y > h; y--) {
               m.set(x, y, z);
               this.put(
                  m,
                  y > this.sea ? Blocks.AIR.getDefaultState() : (y == this.sea && temp < -0.45 ? Blocks.ICE.getDefaultState() : Blocks.WATER.getDefaultState())
               );
            }

            double hv = hash(this.seed + 11L, x, z);
            boolean under = h < this.sea;
            BlockState surf;
            BlockState sub;
            if (under) {
               surf = h < this.sea - 6
                  ? (hv > 0.4 ? Blocks.GRAVEL.getDefaultState() : (hv < -0.75 ? Blocks.CLAY.getDefaultState() : Blocks.SAND.getDefaultState()))
                  : Blocks.SAND.getDefaultState();
               sub = Blocks.SAND.getDefaultState();
            } else if (h > this.sea + 2 || h > this.sea && cold) {
               if (h > this.sea + 50) {
                  surf = Blocks.SNOW_BLOCK.getDefaultState();
                  sub = Blocks.STONE.getDefaultState();
               } else if (h > this.sea + 34) {
                  surf = !cold && h <= this.sea + 44
                     ? (hv > 0.3 ? Blocks.GRAVEL.getDefaultState() : Blocks.STONE.getDefaultState())
                     : Blocks.SNOW_BLOCK.getDefaultState();
                  sub = Blocks.STONE.getDefaultState();
               } else if (hot) {
                  surf = Blocks.SAND.getDefaultState();
                  sub = Blocks.SANDSTONE.getDefaultState();
               } else if (cold) {
                  surf = Blocks.GRASS_BLOCK.getDefaultState().with(SnowyBlock.SNOWY, true);
                  sub = Blocks.DIRT.getDefaultState();
               } else {
                  surf = Blocks.GRASS_BLOCK.getDefaultState();
                  sub = Blocks.DIRT.getDefaultState();
               }
            } else {
               surf = sub = Blocks.SAND.getDefaultState();
            }

            int bottom = Math.max(this.w.getBottomY() + 1, Math.min(h - 4, oldGround));

            for (int y = h; y >= bottom; y--) {
               m.set(x, y, z);
               BlockState want;
               if (y == h) {
                  want = surf;
               } else if (y > h - 4) {
                  want = sub;
               } else {
                  BlockState s = this.w.getBlockState(m);
                  if (!s.isAir() && s.getFluidState().isEmpty() && !s.isIn(BlockTags.LEAVES) && !s.isIn(BlockTags.LOGS)) {
                     continue;
                  }

                  want = Blocks.STONE.getDefaultState();
               }

               this.put(m, want);
            }

            double r2 = (hash(this.seed + 23L, x, z) + 1.0) / 2.0;
            m.set(x, h + 1, z);
            if (under) {
               if (h < this.sea - 1 && r2 < 0.12 && !cold) {
                  this.put(m, Blocks.SEAGRASS.getDefaultState());
               }
            } else {
               if (surf.isOf(Blocks.GRASS_BLOCK)) {
                  double forest = fbm(this.seed + 33L, x / 30.0, z / 30.0, 2);
                  double tc = forest > 0.15 ? 0.07142857142857142 : 0.00909090909090909;
                  if (r2 < tc) {
                     this.trees.add(new long[]{x, h + 1, z, cold ? 1L : (forest > 0.4 ? 2L : 0L)});
                  } else if (cold) {
                     this.put(m, Blocks.SNOW.getDefaultState());
                  } else if (r2 < 0.3) {
                     this.put(m, Blocks.SHORT_GRASS.getDefaultState());
                  } else if (r2 < 0.325) {
                     Block[] fl = new Block[]{Blocks.DANDELION, Blocks.POPPY, Blocks.CORNFLOWER, Blocks.OXEYE_DAISY, Blocks.AZURE_BLUET, Blocks.ALLIUM};
                     this.put(m, fl[(int)((r2 - 0.3) * 1000.0) % fl.length].getDefaultState());
                  }
               } else if (surf.isOf(Blocks.SAND) && hot && h > this.sea + 2) {
                  if (r2 < 0.012) {
                     int hh = 1 + (int)(r2 * 250.0) % 3;

                     for (int k = 0; k < hh; k++) {
                        m.set(x, h + 1 + k, z);
                        this.put(m, Blocks.CACTUS.getDefaultState());
                     }
                  } else if (r2 < 0.03) {
                     this.put(m, Blocks.DEAD_BUSH.getDefaultState());
                  }
               } else if ((surf.isOf(Blocks.STONE) || surf.isOf(Blocks.GRAVEL)) && r2 < 0.025) {
                  m.set(x, h, z);
                  this.put(m, Ores.ore("mix", false, this.rnd).getDefaultState());
               }
            }
         }
      }
   }

   void tree(BlockPos p, int kind) {
      if (this.w.isChunkLoaded(p)) {
         if (this.w.getBlockState(p.down()).isIn(BlockTags.DIRT)) {
            BlockState here = this.w.getBlockState(p);
            if (here.isAir() || here.isReplaceable()) {
               Block b = kind == 1 ? Blocks.SPRUCE_SAPLING : (kind == 2 ? Blocks.BIRCH_SAPLING : Blocks.OAK_SAPLING);
               BlockState st = b.getDefaultState().with(SaplingBlock.STAGE, 1);
               this.w.setBlockState(p, st, 2);
               ((SaplingBlock)b).generate(this.w, p, st, this.w.getRandom());
            }
         }
      }
   }
}
