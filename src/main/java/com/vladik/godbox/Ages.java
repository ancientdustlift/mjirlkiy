package com.vladik.godbox;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.VindicatorEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class Ages {
   public static final String[] NAMES = Eras.NAMES;
   private static final Identifier HP = Identifier.of("godbox", "age_hp");
   private static final Identifier DMG = Identifier.of("godbox", "age_dmg");
   private static int timer;

   private Ages() {
   }

   static String name(Civ.City c) {
      return NAMES[Math.max(0, Math.min(NAMES.length - 1, c.era))];
   }

   public static void tick(MinecraftServer s) {
      Eras.tick(s);
      if (++timer % 100 == 0) {
         for (Kingdoms.K k : Kingdoms.K.values()) {
            if (k.alive && k.world != null && k.capital != null) {
               Civ.City c = Civ.city(k);
               int vill = 0;

               for (MobEntity m : k.members) {
                  if (m instanceof VillagerEntity && m.isAlive()) {
                     vill++;
                  }
               }

               int built = 0;

               for (Civ.Building b : c.buildings) {
                  if (b.done && b.kind != Civ.Kind.QUARRY) {
                     built++;
                  }
               }

               int sage = c.culture == 4 ? 1 : 0;
               if (Eras.ready(c, vill, built, sage)) {
                  Eras.pay(c);
                  advance(s, k, c);
               }

               if (c.era >= 6 && !c.castle && timer % 1200 == 0) {
                  castle(k, c, k.world);
               }

               buff(k, c);
            }
         }
      }
   }

   static void advance(MinecraftServer s, Kingdoms.K k, Civ.City c) {
      if (c.era < NAMES.length - 1) {
         c.era++;
         Chronicle.say(s, k, "вступает в новую эпоху: " + NAMES[c.era] + "!");
         ServerWorld w = k.world;
         BlockPos p = k.capital;
         Fx.particles(w, ParticleTypes.TOTEM_OF_UNDYING, p.getX() + 0.5, p.getY() + 3, p.getZ() + 0.5, 120, 3.0, 3.0, 3.0, 0.4);
         w.playSound(null, p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 3.0F, 1.0F);
         if (c.era >= 6 && !c.castle) {
            castle(k, c, w);
         }

         Eras.rebuild(k, c, w);
      }
   }

   static void buff(Kingdoms.K k, Civ.City c) {
      String tag = "gbage" + c.era;

      for (MobEntity m : k.members) {
         if (!(m instanceof VillagerEntity) && m.isAlive() && !m.getCommandTags().contains(tag)) {
            for (int i = 0; i < NAMES.length; i++) {
               m.removeCommandTag("gbage" + i);
            }

            m.addCommandTag(tag);
            EntityAttributeInstance hp = m.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            if (hp != null) {
               hp.removeModifier(HP);
               if (c.era > 0) {
                  hp.addPersistentModifier(new EntityAttributeModifier(HP, 3.0 * c.era, Operation.ADD_VALUE));
               }

               m.setHealth(m.getMaxHealth());
            }

            EntityAttributeInstance dm = m.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
            if (dm != null) {
               dm.removeModifier(DMG);
               if (c.era > 0) {
                  dm.addPersistentModifier(new EntityAttributeModifier(DMG, 1.0 * c.era, Operation.ADD_VALUE));
               }
            }

            if (m instanceof VindicatorEntity) {
               ItemStack axe = new ItemStack(Eras.axe(c.era));
               m.equipStack(EquipmentSlot.MAINHAND, axe);
               m.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
            }
         }
      }
   }

   public static void force(ServerPlayerEntity god, ServerWorld w, BlockPos pos) {
      Kingdoms.K k = Diplomacy.nearest(w, pos);
      if (k == null) {
         god.sendMessage(Text.literal("Рядом нет королевства"), true);
      } else {
         Civ.City c = Civ.city(k);
         if (c.era >= NAMES.length - 1) {
            god.sendMessage(Text.literal(k.title + " уже в последней эпохе"), true);
            if (!c.castle) {
               castle(k, c, w);
            }
         } else {
            advance(god.getServer(), k, c);
         }
      }
   }

   static void castle(Kingdoms.K k, Civ.City c, ServerWorld w) {
      final Civ.Building b = Civ.site(k, c, Civ.Kind.CASTLE, w);
      if (b != null) {
         b.done = true;
         b.era = c.era;
         b.rev = 2;
         b.busy = true;
         c.buildings.add(b);
         c.castle = true;
         final List<BlockPos> ps = new ArrayList<>();
         final List<BlockState> ss = new ArrayList<>();
         plan(k, b, w, ps, ss);
         Eras.apply(k, b.kind, ss);
         Chronicle.say(w.getServer(), k, "начало строить великий замок!");
         Tasks.add(new Tasks.Task() {
            int i;

            @Override
            public boolean tick() {
               int n = 0;

               while (n < 12 && this.i < ps.size()) {
                  BlockPos p = ps.get(this.i);
                  w.setBlockState(p, ss.get(this.i), 3);
                  if (this.i % 12 == 0 && !ss.get(this.i).isAir()) {
                     w.playSound(null, p, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.BLOCKS, 0.8F, 0.9F);
                     Fx.particles(w, ParticleTypes.CLOUD, p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5, 2, 0.3, 0.2, 0.3, 0.01);
                  }

                  n++;
                  this.i++;
               }

               if (this.i >= ps.size()) {
                  b.busy = false;
                  Chronicle.say(w.getServer(), k, "достроило замок!");
                  Fx.particles(w, ParticleTypes.FIREWORK, b.cx() + 0.5, b.y0 + 14, b.cz() + 0.5, 150, 4.0, 3.0, 4.0, 0.2);
                  w.playSound(null, b.center(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 3.0F, 0.8F);
                  return true;
               } else {
                  return false;
               }
            }
         });
      }
   }

   private static void put(List<BlockPos> ps, List<BlockState> ss, int x, int y, int z, BlockState s) {
      ps.add(new BlockPos(x, y, z));
      ss.add(s);
   }

   static void plan(Kingdoms.K k, Civ.Building b, ServerWorld w, List<BlockPos> ps, List<BlockState> ss) {
      BlockState air = Blocks.AIR.getDefaultState();
      BlockState brick = Blocks.STONE_BRICKS.getDefaultState();
      BlockState mossy = Blocks.MOSSY_STONE_BRICKS.getDefaultState();
      BlockState cobble = Blocks.COBBLESTONE.getDefaultState();
      BlockState floor = Blocks.POLISHED_ANDESITE.getDefaultState();
      BlockState roof = Blocks.DARK_OAK_PLANKS.getDefaultState();
      BlockState wool = k.wool.getDefaultState();
      BlockState glass = Blocks.GLASS_PANE.getDefaultState();
      BlockState torch = Blocks.TORCH.getDefaultState();
      BlockState fence = Blocks.DARK_OAK_FENCE.getDefaultState();
      int x0 = b.x0;
      int x1 = b.x1;
      int z0 = b.z0;
      int z1 = b.z1;
      int y0 = b.y0;
      int cx = b.cx();
      int cz = b.cz();

      for (int x = x0; x <= x1; x++) {
         for (int z = z0; z <= z1; z++) {
            for (int y = y0 - 1; y >= y0 - 6; y--) {
               BlockPos p = new BlockPos(x, y, z);
               if (w.getBlockState(p).isSolidBlock(w, p)) {
                  break;
               }

               put(ps, ss, x, y, z, cobble);
            }

            put(ps, ss, x, y0, z, floor);
         }
      }

      for (int y = y0 + 1; y <= y0 + 15; y++) {
         for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
               if (!w.getBlockState(new BlockPos(x, y, z)).isAir()) {
                  put(ps, ss, x, y, z, air);
               }
            }
         }
      }

      for (int y = y0 + 1; y <= y0 + 14; y++) {
         for (int x = x0; x <= x1; x++) {
            for (int zx = z0; zx <= z1; zx++) {
               boolean edge = x == x0 || x == x1 || zx == z0 || zx == z1;
               boolean tower = (x <= x0 + 2 || x >= x1 - 2) && (zx <= z0 + 2 || zx >= z1 - 2);
               boolean keep = Math.abs(x - cx) <= 2 && Math.abs(zx - cz) <= 2;
               boolean keepEdge = keep && (Math.abs(x - cx) == 2 || Math.abs(zx - cz) == 2);
               BlockState s = null;
               if (tower) {
                  if (y <= y0 + 8) {
                     s = (x + y + zx) % 5 == 0 ? mossy : brick;
                  } else if (y != y0 + 9 || x != x0 && x != x1 && x != x0 + 2 && x != x1 - 2 || zx != z0 && zx != z1 && zx != z0 + 2 && zx != z1 - 2) {
                     if (y != y0 + 9 || x != x0 + 1 && x != x1 - 1 || zx != z0 + 1 && zx != z1 - 1) {
                        if (y == y0 + 10 && (x == x0 + 1 || x == x1 - 1) && (zx == z0 + 1 || zx == z1 - 1)) {
                           s = torch;
                        }
                     } else {
                        s = wool;
                     }
                  } else {
                     s = brick;
                  }
               } else if (edge) {
                  boolean gate = zx == z0 && Math.abs(x - cx) <= 1 && y <= y0 + 3;
                  if (gate) {
                     s = null;
                  } else if (y <= y0 + 5) {
                     s = (x + y + zx) % 6 == 0 ? mossy : brick;
                  } else if (y == y0 + 6 && (x + zx) % 2 == 0) {
                     s = brick;
                  }
               } else if (keep) {
                  boolean door = zx == cz - 2 && x == cx && y <= y0 + 2;
                  if (keepEdge && y <= y0 + 9 && !door) {
                     s = y != y0 + 5 || x != cx && zx != cz ? brick : glass;
                  } else if (y == y0 + 10) {
                     s = roof;
                  } else if (y == y0 + 11 && keepEdge && (x + zx) % 2 == 0) {
                     s = brick;
                  } else if (x == cx && zx == cz && y >= y0 + 11 && y <= y0 + 13) {
                     s = fence;
                  } else if (x == cx + 1 && zx == cz && (y == y0 + 13 || y == y0 + 12)) {
                     s = wool;
                  }
               }

               if (s != null) {
                  put(ps, ss, x, y, zx, s);
               }
            }
         }
      }

      put(ps, ss, cx - 2, y0 + 4, z0, wool);
      put(ps, ss, cx + 2, y0 + 4, z0, wool);
      put(ps, ss, cx - 2, y0 + 6, z0, torch);
      put(ps, ss, cx + 2, y0 + 6, z0, torch);
      put(ps, ss, x0 + 4, y0 + 1, z0 + 4, Blocks.LANTERN.getDefaultState());
      put(ps, ss, x1 - 4, y0 + 1, z1 - 4, Blocks.LANTERN.getDefaultState());
      put(ps, ss, x0 + 4, y0 + 1, z1 - 4, Blocks.LANTERN.getDefaultState());
      put(ps, ss, x1 - 4, y0 + 1, z0 + 4, Blocks.LANTERN.getDefaultState());
   }

   public static void clear() {
      timer = 0;
      Eras.clear();
   }
}
