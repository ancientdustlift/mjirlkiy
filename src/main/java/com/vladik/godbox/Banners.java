package com.vladik.godbox;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.block.AbstractBannerBlock;
import net.minecraft.block.BannerBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.block.entity.BannerPatterns;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.component.type.BannerPatternsComponent.Builder;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.chunk.WorldChunk;

public final class Banners {
   public static final int[] SALT = new int[Kingdoms.K.values().length];
   private static int timer;
   private static final Set<Civ.Kind> FLAGGED = EnumSet.of(
      Civ.Kind.BARRACKS, Civ.Kind.TOWER, Civ.Kind.TEMPLE, Civ.Kind.CASTLE, Civ.Kind.MANOR, Civ.Kind.WAREHOUSE
   );
   private static final List<RegistryKey<BannerPattern>> FIELDS = List.of(
      BannerPatterns.HALF_VERTICAL,
      BannerPatterns.HALF_HORIZONTAL,
      BannerPatterns.STRIPE_CENTER,
      BannerPatterns.STRIPE_MIDDLE,
      BannerPatterns.DIAGONAL_LEFT,
      BannerPatterns.DIAGONAL_RIGHT,
      BannerPatterns.STRAIGHT_CROSS,
      BannerPatterns.CROSS,
      BannerPatterns.SMALL_STRIPES,
      BannerPatterns.TRIANGLE_BOTTOM,
      BannerPatterns.TRIANGLE_TOP,
      BannerPatterns.GRADIENT,
      BannerPatterns.STRIPE_TOP,
      BannerPatterns.STRIPE_BOTTOM
   );
   private static final List<RegistryKey<BannerPattern>> CHARGES = List.of(
      BannerPatterns.CIRCLE,
      BannerPatterns.RHOMBUS,
      BannerPatterns.FLOWER,
      BannerPatterns.SKULL,
      BannerPatterns.CREEPER,
      BannerPatterns.GLOBE,
      BannerPatterns.TRIANGLES_TOP,
      BannerPatterns.TRIANGLES_BOTTOM
   );
   private static final List<RegistryKey<BannerPattern>> FRAMES = List.of(BannerPatterns.BORDER, BannerPatterns.CURLY_BORDER, BannerPatterns.BRICKS);
   private static final DyeColor[] PALETTE = new DyeColor[]{
      DyeColor.WHITE,
      DyeColor.BLACK,
      DyeColor.ORANGE,
      DyeColor.LIGHT_BLUE,
      DyeColor.LIME,
      DyeColor.PURPLE,
      DyeColor.CYAN,
      DyeColor.BROWN,
      DyeColor.MAGENTA,
      DyeColor.PINK,
      DyeColor.LIGHT_GRAY,
      DyeColor.GRAY
   };

   private Banners() {
   }

   public static DyeColor dye(Kingdoms.K k) {
      return k.dye;
   }

   static Item item(Kingdoms.K k) {
      return k.bannerItem();
   }

   static Block block(Kingdoms.K k) {
      return k.banner();
   }

   private static DyeColor pick(Random r, DyeColor... not) {
      for (int i = 0; i < 20; i++) {
         DyeColor c = PALETTE[r.nextInt(PALETTE.length)];
         boolean bad = false;

         for (DyeColor n : not) {
            if (n == c) {
               bad = true;
            }
         }

         if (!bad) {
            return c;
         }
      }

      return DyeColor.WHITE;
   }

   private static void add(Builder b, Registry<BannerPattern> reg, RegistryKey<BannerPattern> key, DyeColor c) {
      reg.getEntry(key).ifPresent(e -> b.add(e, c));
   }

   public static BannerPatternsComponent patterns(ServerWorld w, Kingdoms.K k) {
      Random r = Random.create(w.getSeed() * 31L + k.ordinal() * 7919L + SALT[k.ordinal()] * 104729L + 17L);
      Registry<BannerPattern> reg = w.getRegistryManager().get(RegistryKeys.BANNER_PATTERN);
      DyeColor base = dye(k);
      DyeColor second = pick(r, base);
      DyeColor third = pick(r, base, second);
      Builder b = new Builder();
      add(b, reg, FIELDS.get(r.nextInt(FIELDS.size())), second);
      add(b, reg, CHARGES.get(r.nextInt(CHARGES.size())), r.nextBoolean() ? third : base);
      if (r.nextInt(3) == 0) {
         add(b, reg, FRAMES.get(r.nextInt(FRAMES.size())), second);
      }

      return b.build();
   }

   public static ItemStack stack(ServerWorld w, Kingdoms.K k) {
      ItemStack st = new ItemStack(item(k));
      st.set(DataComponentTypes.BANNER_PATTERNS, patterns(w, k));
      return st;
   }

   private static void paint(ServerWorld w, BlockPos p, ItemStack st) {
      if (w.getBlockEntity(p) instanceof BannerBlockEntity bbe) {
         bbe.readComponents(st);
         bbe.markDirty();
         BlockState s = w.getBlockState(p);
         w.updateListeners(p, s, s, 3);
      }
   }

   public static boolean place(ServerWorld w, Kingdoms.K k, BlockPos p, int rotation) {
      if (!w.getBlockState(p).isAir()) {
         return false;
      } else {
         BlockState st = block(k).getDefaultState().with(BannerBlock.ROTATION, Math.floorMod(rotation, 16));
         if (!st.canPlaceAt(w, p)) {
            return false;
         } else {
            w.setBlockState(p, st, 3);
            paint(w, p, stack(w, k));
            return true;
         }
      }
   }

   public static void onBuilt(Kingdoms.K k, ServerWorld w, Civ.Building b) {
      if (FLAGGED.contains(b.kind)) {
         BlockPos top = w.getTopPosition(Type.MOTION_BLOCKING, new BlockPos(b.cx(), 0, b.cz()));
         if (top.getY() - b.y0 <= 30) {
            int rot = Math.floorMod((int)Math.round(Math.atan2(k.capital.getX() - b.cx(), b.cz() - k.capital.getZ()) / (Math.PI * 2) * 16.0), 16);
            place(w, k, top, rot);
         }
      }
   }

   static int repaint(ServerWorld w, Kingdoms.K k, BlockPos center, int chunks, boolean force) {
      DyeColor color = dye(k);
      ItemStack st = null;
      int n = 0;
      int cx = center.getX() >> 4;
      int cz = center.getZ() >> 4;

      for (int x = cx - chunks; x <= cx + chunks; x++) {
         for (int z = cz - chunks; z <= cz + chunks; z++) {
            WorldChunk ch = w.getChunkManager().getWorldChunk(x, z);
            if (ch != null) {
               List<BlockPos> found = new ArrayList<>();

               for (Entry<BlockPos, BlockEntity> e : ch.getBlockEntities().entrySet()) {
                  if (e.getValue() instanceof BannerBlockEntity bbe) {
                     BlockState s = w.getBlockState(e.getKey());
                     if (s.getBlock() instanceof AbstractBannerBlock ab && ab.getColor() == color && (force || bbe.getPatterns().layers().isEmpty())) {
                        found.add(e.getKey().toImmutable());
                     }
                  }
               }

               for (BlockPos p : found) {
                  if (st == null) {
                     st = stack(w, k);
                  }

                  paint(w, p, st);
                  n++;
               }
            }
         }
      }

      return n;
   }

   private static void capitalFlag(ServerWorld w, Kingdoms.K k) {
      BlockPos c = k.capital;
      BlockPos top = w.getTopPosition(Type.MOTION_BLOCKING, c);
      if (top.getY() - c.getY() <= 40) {
         for (int y = c.getY() - 2; y <= top.getY() + 1; y++) {
            if (w.getBlockState(new BlockPos(c.getX(), y, c.getZ())).getBlock() instanceof AbstractBannerBlock) {
               return;
            }
         }

         place(w, k, top, 0);
      }
   }

   public static void tick(MinecraftServer server) {
      if (++timer % 600 == 0) {
         for (Kingdoms.K k : Kingdoms.K.values()) {
            if (k.alive && k.world != null && k.capital != null && k.world.isChunkLoaded(k.capital)) {
               capitalFlag(k.world, k);
               repaint(k.world, k, k.capital, 6, false);
               List<BlockPos> towns = Conquest.TOWNS.get(k);
               if (towns != null) {
                  for (BlockPos t : new ArrayList<>(towns)) {
                     if (k.world.isChunkLoaded(t)) {
                        repaint(k.world, k, t, 4, false);
                     }
                  }
               }
            }
         }
      }
   }

   public static void reroll(ServerPlayerEntity god, ServerWorld w, BlockPos pos) {
      Kingdoms.K k = Diplomacy.nearest(w, pos);
      if (k != null && k.alive && k.capital != null) {
         SALT[k.ordinal()]++;
         int n = repaint(k.world, k, k.capital, 6, true);
         List<BlockPos> towns = Conquest.TOWNS.get(k);
         if (towns != null) {
            for (BlockPos t : new ArrayList<>(towns)) {
               n += repaint(k.world, k, t, 4, true);
            }
         }

         for (MobEntity m : new ArrayList<>(k.members)) {
            if (m.isAlive() && m.getEquippedStack(EquipmentSlot.HEAD).getItem() == item(k)) {
               m.equipStack(EquipmentSlot.HEAD, stack(k.world, k));
            }
         }

         BlockPos c = k.capital;
         Fx.particles(k.world, ParticleTypes.HAPPY_VILLAGER, c.getX() + 0.5, c.getY() + 3, c.getZ() + 0.5, 40, 3.0, 2.0, 3.0, 0.05);
         Fx.sound(k.world, c.getX() + 0.5, c.getY() + 2, c.getZ() + 0.5, SoundEvents.UI_LOOM_TAKE_RESULT, 2.0F, 1.0F);
         god.sendMessage(Text.literal(k.title + " приняло новый герб (перекрашено знамён: " + n + ")").formatted(k.color), false);
         Chronicle.log(k.title + " приняло новый герб");
      } else {
         god.sendMessage(Text.literal("Рядом нет королевства"), true);
      }
   }

   public static void clear() {
      timer = 0;
   }
}
