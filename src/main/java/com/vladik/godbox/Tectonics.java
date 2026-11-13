package com.vladik.godbox;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.random.Random;

public final class Tectonics {
   public static boolean on = true;
   static boolean erupted;
   static final List<Tectonics.V> VOLCANOES = new ArrayList<>();
   private static int t;

   private Tectonics() {
   }

   static void tell(MinecraftServer s, String m) {
      for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
         p.sendMessage(Text.literal(m).formatted(Formatting.GOLD), false);
      }
   }

   public static void volcano(ServerPlayerEntity god, ServerWorld w, BlockPos pos) {
      if (VOLCANOES.size() >= 8) {
         god.sendMessage(Text.literal("Уже 8 живых вулканов — больше нельзя"), true);
      } else {
         int h = 14;
         Tasks.add(new Powers2.Volcano(w, pos.up(), h));
         Tectonics.V v = new Tectonics.V(pos.up(h + 1), h);
         v.next = t + 900;
         VOLCANOES.add(v);
         god.sendMessage(Text.literal("\ud83c\udf0b Живой вулкан пробудился — теперь он сам извергается время от времени").formatted(Formatting.GOLD), true);
      }
   }

   public static void rift(ServerPlayerEntity god, ServerWorld w, BlockPos pos) {
      double a = w.getRandom().nextDouble() * Math.PI;
      Tasks.add(new Tectonics.Rift(w, pos, Math.cos(a), Math.sin(a)));
      Tasks.add(new Disasters.Quake(w, pos));
      god.sendMessage(Text.literal("\ud83c\udf0d Земля раскалывается!").formatted(Formatting.GOLD), true);
   }

   public static void tick(MinecraftServer s) {
      if (++t % 20 == 0) {
         ServerWorld w = s.getOverworld();
         Random r = w.getRandom();

         for (Tectonics.V v : VOLCANOES) {
            if (v.next <= 0) {
               v.next = t + 1200 + r.nextInt(2400);
            }

            if (t >= v.next) {
               v.next = t + 2400 + r.nextInt(4800);
               if (w.isChunkLoaded(v.top.getX() >> 4, v.top.getZ() >> 4)) {
                  BlockPos c = Fx.surface(w, v.top.getX(), v.top.getZ());
                  Tasks.add(new Tectonics.Eruption(w, c, v.h));
                  Tasks.add(new Disasters.Quake(w, c));
                  erupted = true;
                  tell(s, "\ud83c\udf0b Извержение вулкана у " + c.getX() + ", " + c.getZ() + "!");
                  Chronicle.log("Извергся вулкан у " + c.getX() + ", " + c.getZ());

                  for (Kingdoms.K k : Kingdoms.K.values()) {
                     if (k.alive && k.capital != null && k.capital.getSquaredDistance(c) < 19600.0) {
                        Unrest.add(k, 4);
                     }
                  }
               }
            }
         }

         if (on) {
            if (t % 6000 == 0 && r.nextBoolean()) {
               List<Kingdoms.K> ks = new ArrayList<>();

               for (Kingdoms.K kx : Kingdoms.K.values()) {
                  if (kx.alive && kx.capital != null) {
                     ServerWorld var27 = kx.world;
                     if (var27 instanceof ServerWorld && var27.isChunkLoaded(kx.capital.getX() >> 4, kx.capital.getZ() >> 4)) {
                        ks.add(kx);
                     }
                  }
               }

               if (!ks.isEmpty()) {
                  Kingdoms.K kxx = ks.get(r.nextInt(ks.size()));
                  ServerWorld kw = kxx.world;
                  BlockPos p = Fx.surface(kw, kxx.capital.getX() + r.nextInt(81) - 40, kxx.capital.getZ() + r.nextInt(81) - 40);
                  Tasks.add(new Disasters.Quake(kw, p));
                  Unrest.add(kxx, 3);
                  Chronicle.say(s, kxx, "пережило землетрясение");
               }
            }

            if (t % 24000 == 0 && VOLCANOES.size() < 6 && r.nextInt(3) == 0) {
               List<ServerPlayerEntity> ps = w.getPlayers();
               if (ps.isEmpty()) {
                  return;
               }

               ServerPlayerEntity p = ps.get(r.nextInt(ps.size()));
               double a = r.nextDouble() * Math.PI * 2.0;
               int d = 100 + r.nextInt(80);
               int x = p.getBlockX() + (int)(Math.cos(a) * d);
               int z = p.getBlockZ() + (int)(Math.sin(a) * d);
               if (!w.isChunkLoaded(x >> 4, z >> 4)) {
                  return;
               }

               for (Kingdoms.K kxx : Kingdoms.K.values()) {
                  if (kxx.alive && kxx.capital != null && kxx.capital.getSquaredDistance(new BlockPos(x, kxx.capital.getY(), z)) < 8100.0) {
                     return;
                  }
               }

               BlockPos g = Fx.surface(w, x, z);
               if (!w.getFluidState(g.down()).isEmpty()) {
                  return;
               }

               Tasks.add(new Powers2.Volcano(w, g, 12));
               VOLCANOES.add(new Tectonics.V(g.up(13), 12));
               tell(s, "\ud83c\udf0b Из-под земли поднялся новый вулкан у " + x + ", " + z);
            }
         }
      }
   }

   public static void clear() {
      VOLCANOES.clear();
      erupted = false;
      t = 0;
   }

   static final class Eruption implements Tasks.Task {
      final ServerWorld w;
      final BlockPos c;
      final int h;
      int life = 240;

      Eruption(ServerWorld w, BlockPos c, int h) {
         this.w = w;
         this.c = c;
         this.h = h;
      }

      @Override
      public boolean tick() {
         Random r = this.w.getRandom();
         this.life--;
         double x = this.c.getX() + 0.5;
         double y = this.c.getY() + 1;
         double z = this.c.getZ() + 0.5;
         Fx.particles(this.w, ParticleTypes.LAVA, x, y, z, 6, 1.0, 0.5, 1.0, 0.5);
         Fx.particles(this.w, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x, y + 2.0, z, 3, 1.5, 2.0, 1.5, 0.05);
         Fx.particles(this.w, ParticleTypes.LARGE_SMOKE, x, y + 4.0, z, 6, 2.0, 3.0, 2.0, 0.1);
         if (this.life % 4 == 0) {
            BlockPos at = this.c.up(3);
            if (this.w.getBlockState(at).isAir()) {
               BlockState st = (r.nextInt(3) == 0 ? Blocks.MAGMA_BLOCK : Blocks.BLACKSTONE).getDefaultState();
               FallingBlockEntity fb = FallingBlockEntity.spawnFromBlock(this.w, at, st);
               fb.setVelocity((r.nextDouble() - 0.5) * 1.4, 0.8 + r.nextDouble() * 0.7, (r.nextDouble() - 0.5) * 1.4);
               fb.velocityModified = true;
            }
         }

         if (this.life % 20 == 0) {
            Fx.sound(this.w, x, y, z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 4.0F, 0.5F);

            for (LivingEntity e : Fx.living(this.w, Vec3d.ofCenter(this.c), this.h)) {
               e.setFireTicks(100);
            }
         }

         if (this.life == 220 || this.life == 140) {
            BlockPos lp = this.c;
            if (this.w.getBlockState(lp).isAir()) {
               this.w.setBlockState(lp, Blocks.LAVA.getDefaultState());
            }
         }

         if (this.life > 0) {
            return false;
         } else {
            int rad = (int)(this.h * 1.6);
            Mutable m = new Mutable();

            for (int dx = -rad; dx <= rad; dx++) {
               for (int dz = -rad; dz <= rad; dz++) {
                  for (int dy = -this.h - 3; dy <= 3; dy++) {
                     m.set(this.c.getX() + dx, this.c.getY() + dy, this.c.getZ() + dz);
                     if (this.w.getFluidState(m).isIn(FluidTags.LAVA)) {
                        boolean still = this.w.getFluidState(m).isStill();
                        this.w
                           .setBlockState(
                              m, still ? Blocks.MAGMA_BLOCK.getDefaultState() : (r.nextInt(3) == 0 ? Blocks.BLACKSTONE : Blocks.BASALT).getDefaultState()
                           );
                     }
                  }
               }
            }

            return true;
         }
      }
   }

   static final class Rift implements Tasks.Task {
      final ServerWorld w;
      final BlockPos c;
      final double dx;
      final double dz;
      int i = -24;

      Rift(ServerWorld w, BlockPos c, double dx, double dz) {
         this.w = w;
         this.c = c;
         this.dx = dx;
         this.dz = dz;
      }

      @Override
      public boolean tick() {
         int n = 0;

         while (n < 2 && this.i <= 24) {
            int depth = 10 - Math.abs(this.i) / 3;
            int bx = this.c.getX() + (int)Math.round(this.dx * this.i);
            int bz = this.c.getZ() + (int)Math.round(this.dz * this.i);

            for (int s = -1; s <= 1; s++) {
               int x = bx + (int)Math.round(-this.dz * s);
               int z = bz + (int)Math.round(this.dx * s);
               BlockPos top = Fx.surface(this.w, x, z);

               for (int d = 1; d <= depth; d++) {
                  BlockPos p = top.down(d);
                  BlockState st = this.w.getBlockState(p);
                  if (st.isOf(Blocks.BEDROCK) || !Fx.breakable(this.w, p, st, 50.0F)) {
                     break;
                  }

                  this.w.setBlockState(p, d >= depth - 1 && s == 0 ? Blocks.LAVA.getDefaultState() : Blocks.AIR.getDefaultState());
               }

               Fx.particles(this.w, ParticleTypes.LARGE_SMOKE, x + 0.5, top.getY() + 0.5, z + 0.5, 4, 0.4, 0.6, 0.4, 0.05);
            }

            if (this.i % 6 == 0) {
               Fx.sound(this.w, bx, this.c.getY(), bz, SoundEvents.BLOCK_STONE_BREAK, 4.0F, 0.5F);
            }

            n++;
            this.i++;
         }

         return this.i > 24;
      }
   }

   static final class V {
      final BlockPos top;
      final int h;
      int next;

      V(BlockPos top, int h) {
         this.top = top;
         this.h = h;
      }
   }
}
