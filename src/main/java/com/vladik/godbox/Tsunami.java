package com.vladik.godbox;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap.Type;
import org.joml.Quaternionf;

/**
 * A wave that rises out of the nearest body of water and rolls over the target. It floods ground up to a few blocks above
 * the water it came from, so hills and walls stop it, and it dies out once a whole front is blocked.
 */
final class Tsunami implements Tasks.Task {
   private static final int SEARCH = 96;
   private static final int BUILDUP = 30;
   private static final int[][] AROUND = new int[][]{{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};
   private static final Item[] FLOTSAM = new Item[]{Items.KELP, Items.SEAGRASS, Items.COD, Items.SALMON, Items.STICK, Items.NAUTILUS_SHELL};
   private final ServerWorld w;
   private final Vec3d start;
   private final Vec3d dir;
   private final Vec3d side;
   private final int width;
   private final int height;
   private final int length;
   /** Y of the water surface the wave came from; the wave cannot climb far above it. */
   private final int seaY;
   final boolean fromWater;
   private final float scale;
   private final ItemDisplayEntity[] crest;
   private final ArrayDeque<List<BlockPos>> rows = new ArrayDeque<>();
   private final Set<BlockPos> active = new HashSet<>();
   private final Map<Long, Integer> floors = new HashMap<>();
   private final ArrayDeque<List<BlockPos>> wake = new ArrayDeque<>();
   private final Set<Entity> struck = Collections.newSetFromMap(new IdentityHashMap<>());
   private int step;
   private int t;
   private int blocked;

   Tsunami(ServerWorld w, BlockPos pos, float yaw, int brush) {
      this.w = w;
      this.width = 16 + brush * 4;
      this.height = 4 + brush / 2;
      Vec3d target = Vec3d.ofCenter(pos);
      BlockPos water = nearestWater(w, pos);
      Vec3d look = Vec3d.fromPolar(0.0F, yaw);
      Vec3d d = new Vec3d(look.x, 0.0, look.z).normalize();
      double run = 12.0;
      Vec3d from = target.subtract(d.multiply(run));
      if (water != null) {
         Vec3d source = Vec3d.ofCenter(water);
         double dist = Math.sqrt(source.squaredDistanceTo(target.x, source.y, target.z));
         if (dist >= 3.0) {
            d = new Vec3d(target.x - source.x, 0.0, target.z - source.z).normalize();
            run = dist;
         } else {
            // Cast on the water itself: nothing to aim at, so follow the camera.
            run = 0.0;
         }

         from = source;
      }

      this.fromWater = water != null;
      this.dir = d;
      this.side = new Vec3d(-d.z, 0.0, d.x);
      this.start = from;
      this.seaY = water != null ? water.getY() : Fx.surface(w, MathHelper.floor(from.x), MathHelper.floor(from.z)).getY() - 1;
      this.length = (int)Math.min(200.0, run + 28 + brush * 6);
      this.scale = MathHelper.clamp(this.height + 1, 5, 8);
      this.crest = new ItemDisplayEntity[Math.max(1, MathHelper.ceil((this.width + 1) / this.scale))];
   }

   private static boolean water(ServerWorld w, int x, int z) {
      if (!w.isChunkLoaded(x >> 4, z >> 4)) {
         return false;
      } else {
         BlockPos top = Fx.surface(w, x, z).down();
         return w.getFluidState(top).isIn(FluidTags.WATER) && w.getFluidState(top.down()).isIn(FluidTags.WATER);
      }
   }

   /** The closest open water at least two blocks deep, ignoring puddles and wells. */
   static BlockPos nearestWater(ServerWorld w, BlockPos pos) {
      for (int r = 0; r <= SEARCH; r += 3) {
         int n = r == 0 ? 1 : Math.max(8, (int)(Math.PI * 2 * r / 3.0));

         for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            int x = pos.getX() + (int)Math.round(Math.cos(a) * r);
            int z = pos.getZ() + (int)Math.round(Math.sin(a) * r);
            if (water(w, x, z)) {
               int open = 0;

               for (int k = 0; k < 8; k++) {
                  double b = Math.PI * 2 * k / 8;
                  if (water(w, x + (int)Math.round(Math.cos(b) * 4.0), z + (int)Math.round(Math.sin(b) * 4.0))) {
                     open++;
                  }
               }

               if (open >= 5) {
                  return Fx.surface(w, x, z).down();
               }
            }
         }
      }

      return null;
   }

   /** Highest ground the wave can run up: twice its height above the water it came from. */
   private int limit() {
      return this.seaY + 1 + this.height * 2;
   }

   private int heightAt(int step) {
      if (step < 4) {
         return this.height / 2 + 1;
      } else {
         int left = this.length - step;
         return left < 12 ? Math.max(1, this.height * left / 12) : this.height;
      }
   }

   /**
    * First block above the ground and any natural water in this column. The answer is remembered from the first look, before
    * the wave's own water reaches the column, so that the clean-up afterwards knows what was there to begin with.
    */
   private BlockPos floor(int x, int z) {
      long key = BlockPos.asLong(x, 0, z);
      Integer known = this.floors.get(key);
      if (known != null) {
         return new BlockPos(x, known, z);
      } else {
         BlockPos g = this.w.getTopPosition(Type.OCEAN_FLOOR, new BlockPos(x, 0, z));

         // Stand on the ground rather than on tree tops, or water would pool unseen under the canopy.
         while (g.getY() > this.w.getBottomY()) {
            BlockPos under = g.down();
            BlockState b = this.w.getBlockState(under);
            boolean open = b.getFluidState().isEmpty() && b.getCollisionShape(this.w, under).isEmpty();
            if (!open && !b.isIn(BlockTags.LEAVES) && !b.isIn(BlockTags.LOGS)) {
               break;
            }

            g = g.down();
         }

         // Flowing water is spill, not a body of water, and is not climbed over.
         while (this.w.getFluidState(g).isIn(FluidTags.WATER) && this.w.getFluidState(g).isStill() && !this.active.contains(g) && g.getY() < this.w.getTopY()) {
            g = g.up();
         }

         this.floors.put(key, g.getY());
         return g;
      }
   }

   /**
    * Notes the ground level along a front, a little wider than the wave and including the gaps a slanted front leaves
    * between rows, and collects it for the clean-up.
    */
   private void look(Vec3d f, List<BlockPos> out) {
      for (int s = -this.width / 2 - 2; s <= this.width / 2 + 2; s++) {
         Vec3d q = f.add(this.side.multiply(s));
         int x = MathHelper.floor(q.x);
         int z = MathHelper.floor(q.z);

         for (int[] o : AROUND) {
            if (this.w.isChunkLoaded(x + o[0] >> 4, z + o[1] >> 4)) {
               BlockPos g = this.floor(x + o[0], z + o[1]);
               if (out != null) {
                  out.add(g);
               }
            }
         }
      }
   }

   /** Removes whatever water is still standing where the wave passed, such as sources formed between its rows. */
   private void mop(List<BlockPos> columns) {
      for (BlockPos g : columns) {
         for (int y = 0; y <= this.height + 1; y++) {
            BlockPos p = g.up(y);
            if (!this.active.contains(p) && this.w.getFluidState(p).isIn(FluidTags.WATER)) {
               this.w.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
            }
         }
      }
   }

   private void drain(List<BlockPos> row) {
      Random r = this.w.getRandom();

      for (BlockPos p : row) {
         this.active.remove(p);
         if (this.w.getBlockState(p).isOf(Blocks.WATER)) {
            this.w.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
            BlockPos below = p.down();
            BlockState ground = this.w.getBlockState(below);
            if (ground.isOf(Blocks.GRASS_BLOCK) || ground.isOf(Blocks.DIRT) || ground.isOf(Blocks.FARMLAND) || ground.isOf(Blocks.DIRT_PATH)) {
               // The retreating water leaves mud and the odd bit of flotsam behind.
               float f = r.nextFloat();
               if (f < 0.12F) {
                  this.w.setBlockState(below, Blocks.MUD.getDefaultState(), 3);
               } else if (f < 0.13F) {
                  ItemEntity it = new ItemEntity(this.w, p.getX() + 0.5, p.getY() + 0.2, p.getZ() + 0.5, new ItemStack(FLOTSAM[r.nextInt(FLOTSAM.length)]));
                  it.setPickupDelay(20);
                  this.w.spawnEntity(it);
               }
            }
         }
      }
   }

   private void removeCrest() {
      for (ItemDisplayEntity e : this.crest) {
         if (e != null) {
            e.discard();
         }
      }
   }

   private void moveCrest(Vec3d front, float grow, int h) {
      float yaw = (float)Math.atan2(this.dir.x, this.dir.z);

      for (int i = 0; i < this.crest.length; i++) {
         double s = -this.width / 2.0 + this.scale * (i + 0.5);
         Vec3d q = front.add(this.side.multiply(s)).add(this.dir);
         int x = MathHelper.floor(q.x);
         int z = MathHelper.floor(q.z);
         if (this.w.isChunkLoaded(x >> 4, z >> 4)) {
            int gy = this.floor(x, z).getY();
            boolean stopped = gy > this.limit();
            float size = stopped ? 0.05F : this.scale * grow * Math.max(0.35F, (float)h / this.height);
            if (this.crest[i] == null) {
               this.crest[i] = Models.prop(this.w, "wave", "crest", q.x, gy, q.z);
               if (this.crest[i] == null) {
                  continue;
               }

               this.crest[i].setTeleportDuration(2);
            }

            // The model spans one block above its base at scale 1 and is centred half a block up.
            this.crest[i].setPosition(q.x, gy + size * 0.5 - 0.3, q.z);
            Models.pose(this.crest[i], new Quaternionf().rotationY(yaw), size);
         }
      }
   }

   private void sweep(Vec3d f, int h) {
      double half = this.width / 2.0 + 1.0;
      Box box = new Box(f.x - half, this.seaY - 6, f.z - half, f.x + half, this.seaY + this.height + 14, f.z + half);

      for (Entity e : this.w.getOtherEntities(null, box, en -> !GodManager.isGodEntity(en) && !en.isSpectator() && !(en instanceof DisplayEntity))) {
         if (!(e instanceof PlayerEntity p && p.isCreative())) {
            Vec3d rel = e.getPos().subtract(f);
            double along = rel.dotProduct(this.dir);
            if (!(along < -2.0) && !(along > 4.0) && !(Math.abs(rel.dotProduct(this.side)) > half)) {
               e.setVelocity(e.getVelocity().multiply(0.5).add(this.dir.multiply(0.9)).add(0.0, 0.25, 0.0));
               e.velocityModified = true;
               e.extinguish();
               if (e instanceof LivingEntity le && this.struck.add(e)) {
                  le.damage(this.w.getDamageSources().drown(), 2.0F + h / 2);
               }
            }
         }
      }
   }

   @Override
   public boolean tick() {
      this.t++;
      if (this.t <= BUILDUP) {
         // The sea swells where the wave will rise.
         float grow = (float)this.t / BUILDUP;
         this.moveCrest(this.start, 0.15F + 0.85F * grow * grow, this.height);
         if (this.t == 1) {
            Fx.sound(this.w, this.start.x, this.seaY + 2, this.start.z, SoundEvents.AMBIENT_UNDERWATER_ENTER, 8.0F, 0.4F);
         }

         if (this.t % 3 == 0) {
            for (int s = -this.width / 2; s <= this.width / 2; s += 3) {
               Vec3d q = this.start.add(this.side.multiply(s));
               Fx.particles(this.w, ParticleTypes.BUBBLE_POP, q.x, this.seaY + 1.2, q.z, 4, 1.0, 0.2, 1.0, 0.02);
               Fx.particles(this.w, ParticleTypes.SPLASH, q.x, this.seaY + 1.2, q.z, 3, 1.0, 0.2, 1.0, 0.1);
            }
         }

         return false;
      } else if (this.t % 2 != 0) {
         return false;
      } else if (this.step >= this.length) {
         this.removeCrest();
         if (!this.rows.isEmpty()) {
            this.drain(this.rows.poll());
         } else if (!this.wake.isEmpty()) {
            this.mop(this.wake.poll());
         }

         return this.rows.isEmpty() && this.wake.isEmpty();
      } else {
         Vec3d f = this.start.add(this.dir.multiply(this.step));
         int h = this.heightAt(this.step);
         int limit = this.limit();
         List<BlockPos> row = new ArrayList<>();
         List<BlockPos> columns = new ArrayList<>();
         int flooded = 0;
         // Look ahead first, so ground levels are read before any of the wave's water gets there.
         this.look(f.add(this.dir.multiply(2.0)), null);
         this.look(f.add(this.dir), null);
         this.look(f, columns);

         for (int s = -this.width / 2; s <= this.width / 2; s++) {
            Vec3d q = f.add(this.side.multiply(s));
            int x = MathHelper.floor(q.x);
            int z = MathHelper.floor(q.z);
            if (this.w.isChunkLoaded(x >> 4, z >> 4)) {
               BlockPos g = this.floor(x, z);
               if (g.getY() > limit) {
                  continue;
               }

               // Higher ground gets shallower water.
               int depth = MathHelper.clamp(h - Math.max(0, g.getY() - (this.seaY + 1)) / 2, 1, h);
               flooded++;

               for (int y = 0; y < depth; y++) {
                  BlockPos p = g.up(y);
                  if (!this.active.contains(p)) {
                     BlockState st = this.w.getBlockState(p);
                     if (!st.isAir() && !(st.isOf(Blocks.WATER) && !st.getFluidState().isStill())) {
                        if (!st.getFluidState().isEmpty()) {
                           continue;
                        }

                        float hard = st.getHardness(this.w, p);
                        if (!st.isReplaceable() && (hard < 0.0F || hard > 0.8F)) {
                           continue;
                        }

                        this.w.breakBlock(p, false);
                     }

                     this.w.setBlockState(p, Blocks.WATER.getDefaultState(), 2);
                     row.add(p);
                     this.active.add(p);
                  }
               }

               if ((s & 1) == 0) {
                  Fx.particles(this.w, ParticleTypes.SPLASH, q.x, g.getY() + depth, q.z, 6, 0.5, 0.3, 0.5, 0.2);
               }

               if ((s & 3) == 0) {
                  Fx.particles(this.w, ParticleTypes.CLOUD, q.x + this.dir.x, g.getY() + depth + 0.5, q.z + this.dir.z, 2, 0.6, 0.3, 0.6, 0.02);
                  Fx.particles(this.w, ParticleTypes.FALLING_WATER, q.x - this.dir.x * 2.0, g.getY() + depth + 1.0, q.z - this.dir.z * 2.0, 3, 0.8, 0.5, 0.8, 0.0);
               }
            }
         }

         this.moveCrest(f, 1.0F, h);
         this.sweep(f, h);
         if (this.step % 8 == 0) {
            Fx.sound(this.w, f.x, this.seaY + h, f.z, SoundEvents.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 6.0F, 0.5F);
         }

         this.rows.add(row);
         this.wake.add(columns);
         this.step++;
         if (this.rows.size() > 3) {
            this.drain(this.rows.poll());
         }

         // Mop up well behind the front, once the drained rows have stopped flowing.
         if (this.wake.size() > 12) {
            this.mop(this.wake.poll());
         }

         // A front that floods nothing has hit a hillside or wall; three in a row and the wave is spent.
         this.blocked = flooded == 0 ? this.blocked + 1 : 0;
         if (this.blocked >= 3) {
            this.step = this.length;
            Fx.sound(this.w, f.x, this.seaY + h, f.z, SoundEvents.ENTITY_GENERIC_SPLASH, 6.0F, 0.4F);
         }

         return false;
      }
   }
}
