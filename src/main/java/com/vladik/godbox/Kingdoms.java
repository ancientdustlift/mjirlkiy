package com.vladik.godbox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

public final class Kingdoms {
   public static boolean war;
   public static boolean eternalPeace;
   private static int timer;
   private static final Set<MobEntity> PREPARED = Collections.newSetFromMap(new WeakHashMap<>());

   private Kingdoms() {
   }

   public static Kingdoms.K byId(String id) {
      for (Kingdoms.K k : Kingdoms.K.values()) {
         if (k.id.equals(id)) {
            return k;
         }
      }

      return null;
   }

   public static Kingdoms.K of(Entity e) {
      if (e == null) {
         return null;
      } else {
         Team t = e.getScoreboardTeam();
         return t != null && t.getName().startsWith("godbox_") ? byId(t.getName().substring(7)) : null;
      }
   }

   private static Team team(MinecraftServer s, Kingdoms.K k) {
      ServerScoreboard sb = s.getScoreboard();
      Team t = sb.getTeam(k.team());
      if (t == null) {
         t = sb.addTeam(k.team());
      }

      t.setColor(k.color);
      t.setFriendlyFireAllowed(false);
      t.setDisplayName(Text.literal(k.title));
      return t;
   }

   private static ItemStack dyed(Item item, int rgb) {
      ItemStack st = new ItemStack(item);
      st.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(rgb, false));
      return st;
   }

   static void join(Kingdoms.K k, MobEntity m) {
      MinecraftServer s = m.getServer();
      if (s != null) {
         s.getScoreboard().addScoreHolderToTeam(m.getNameForScoreboard(), team(s, k));
         m.setGlowing(true);
         m.setPersistent();
         if (!(m instanceof VillagerEntity)) {
            m.targetSelector.clear(g -> true);
            PREPARED.add(m);
            m.equipStack(EquipmentSlot.HEAD, dyed(Items.LEATHER_HELMET, k.rgb));
            m.equipStack(EquipmentSlot.CHEST, dyed(Items.LEATHER_CHESTPLATE, k.rgb));
            m.setEquipmentDropChance(EquipmentSlot.HEAD, 0.0F);
            m.setEquipmentDropChance(EquipmentSlot.CHEST, 0.0F);
            if (k.world != null && m.getRandom().nextInt(6) == 0) {
               m.equipStack(EquipmentSlot.HEAD, Banners.stack(k.world, k));
            }

            Tech.equip(k, m);
            EntityAttributeInstance a = m.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE);
            if (a != null) {
               a.setBaseValue(96.0);
            }
         }

         Races.apply(k, m);
      }
   }

   static MobEntity spawn(Kingdoms.K k, EntityType<? extends MobEntity> type, BlockPos at) {
      MobEntity m = type.spawn(k.world, at, SpawnReason.TRIGGERED);
      if (m != null) {
         join(k, m);
         k.members.add(m);
         Traits.born(m);
      }

      return m;
   }

   static EntityType<? extends MobEntity> soldier(Random r) {
      return r.nextInt(4) == 0 ? EntityType.PILLAGER : EntityType.VINDICATOR;
   }

   private static BlockPos around(ServerWorld w, BlockPos c, int r) {
      Random rnd = w.getRandom();
      return Fx.surface(w, c.getX() + rnd.nextInt(r * 2 + 1) - r, c.getZ() + rnd.nextInt(r * 2 + 1) - r);
   }

   private static void msg(ServerPlayerEntity god, String s, Formatting f) {
      god.sendMessage(Text.literal(s).formatted(f), true);
   }

   private static void broadcast(MinecraftServer s, Text t) {
      Chronicle.log(t.getString());
      s.getPlayerManager().broadcast(t, false);
   }

   private static boolean loaded(Kingdoms.K k) {
      return k.world != null && k.capital != null && k.world.shouldTickEntity(k.capital);
   }

   private static Kingdoms.K nearest(ServerWorld w, BlockPos pos) {
      Kingdoms.K best = null;
      double bd = Double.MAX_VALUE;

      for (Kingdoms.K k : Kingdoms.K.values()) {
         if (k.alive && k.world == w) {
            double d = k.capital.getSquaredDistance(pos);
            if (d < bd) {
               bd = d;
               best = k;
            }
         }
      }

      return best;
   }

   public static Kingdoms.K free() {
      for (Kingdoms.K k : Kingdoms.K.values()) {
         if (!k.alive) {
            return k;
         }
      }

      return null;
   }

   public static void foundNew(ServerPlayerEntity god, ServerWorld w, BlockPos pos, int brush) {
      Kingdoms.K near = nearest(w, pos);
      if (near != null && near.capital.getSquaredDistance(pos) < 1024.0) {
         msg(god, "Слишком близко к столице: " + near.title, Formatting.YELLOW);
      } else {
         Kingdoms.K k = free();
         if (k == null) {
            msg(god, "Все королевства (" + Kingdoms.K.values().length + ") уже основаны — распусти одно из них", Formatting.RED);
         } else {
            Races.set(k, Races.pick(w, pos));
            found(god, k.id, w, pos, brush);
            msg(god, k.title + " основано, раса: " + Races.of(k).title.toLowerCase(), k.color);
         }
      }
   }

   private static int aliveCount() {
      int n = 0;

      for (Kingdoms.K k : Kingdoms.K.values()) {
         if (k.alive) {
            n++;
         }
      }

      return n;
   }

   public static void found(ServerPlayerEntity god, String id, ServerWorld w, BlockPos pos, int brush) {
      Kingdoms.K k = byId(id);
      if (k != null) {
         Random r = w.getRandom();
         BlockPos base = TownBuilder.landOr(w, Fx.surface(w, pos.getX(), pos.getZ()));
         if (k.alive && k.world == w) {
            for (int i = 0; i < 2 + brush / 2; i++) {
               spawn(k, EntityType.VILLAGER, around(w, base, 3));
            }

            for (int i = 0; i < 2 + brush / 2; i++) {
               spawn(k, soldier(r), around(w, base, 3));
            }

            msg(god, k.title + ": прибыли поселенцы", k.color);
         } else {
            if (k.alive) {
               disbandKingdom(k);
            }

            k.world = w;
            k.capital = base;
            k.alive = true;
            k.grow = 0;
            k.fallTimer = 0;
            k.members.clear();
            team(w.getServer(), k);
            fort(w, base, k);
            Civ.found(k);

            for (int i = 0; i < 5; i++) {
               spawn(k, EntityType.VILLAGER, around(w, base, 2));
            }

            for (int i = 0; i < 6; i++) {
               spawn(k, soldier(r), around(w, base, 3));
            }

            if (Config.b("villagers.golems", false)) {
               spawn(k, EntityType.IRON_GOLEM, around(w, base, 2));
            }

            Fx.sound(w, base.getX(), base.getY(), base.getZ(), SoundEvents.EVENT_RAID_HORN.value(), 4.0F, 1.2F);
            broadcast(
               w.getServer(), Text.literal("\ud83c\udff0 Основано: " + k.title + " · " + Races.of(k).title.toLowerCase()).formatted(k.color, Formatting.BOLD)
            );
         }
      }
   }

   private static void fort(ServerWorld w, BlockPos base, Kingdoms.K k) {
      BlockState brick = Races.style(k, Blocks.STONE_BRICKS.getDefaultState());

      for (int dx = -4; dx <= 4; dx++) {
         for (int dz = -4; dz <= 4; dz++) {
            boolean edge = Math.abs(dx) == 4 || Math.abs(dz) == 4;
            if (edge && (dz != 4 || Math.abs(dx) > 1)) {
               BlockPos g = Fx.surface(w, base.getX() + dx, base.getZ() + dz);
               int h = (dx + dz & 1) == 0 ? 3 : 2;

               for (int y = 0; y < h; y++) {
                  w.setBlockState(g.up(y), brick);
               }
            }
         }
      }

      for (int y = 0; y < 5; y++) {
         w.setBlockState(base.up(y), Blocks.OAK_FENCE.getDefaultState());
      }

      w.setBlockState(base.up(5), k.wool.getDefaultState());
      w.setBlockState(base.up(4).east(), k.wool.getDefaultState());
      w.setBlockState(base.up(3).east(), k.wool.getDefaultState());
      w.setBlockState(base.up(4).east(2), k.wool.getDefaultState());
      w.setBlockState(base.up(3).east(2), k.wool.getDefaultState());
   }

   public static void army(ServerPlayerEntity god, ServerWorld w, BlockPos pos, int brush) {
      Kingdoms.K k = nearest(w, pos);
      if (k == null) {
         msg(god, "Сначала оснуй королевство", Formatting.RED);
      } else {
         BlockPos at = Fx.surface(w, pos.getX(), pos.getZ());
         int n = 2 + brush;

         for (int i = 0; i < n; i++) {
            spawn(k, soldier(w.getRandom()), around(w, at, 3));
         }

         msg(god, k.title + ": +" + n + " воинов", k.color);
      }
   }

   public static void setWar(ServerPlayerEntity god, boolean on) {
      MinecraftServer s = god.getServer();
      if (on) {
         if (eternalPeace) {
            msg(god, "Действует закон «Вечный мир»", Formatting.YELLOW);
            return;
         }

         if (aliveCount() < 2) {
            msg(god, "Для войны нужно хотя бы 2 королевства", Formatting.RED);
            return;
         }

         war = true;
         Diplomacy.setAll(true);
         broadcast(s, Text.literal("⚔ Началась война королевств!").formatted(Formatting.RED, Formatting.BOLD));
      } else {
         war = false;
         boolean[][] treaty = Treaties.godPeaceTerms(s);
         int n = Kingdoms.K.values().length;
         int[][] op = new int[n][n];

         for (int i = 0; i < n; i++) {
            op[i] = (int[])Diplomacy.OP[i].clone();
         }

         Diplomacy.setAll(false);

         for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
               if (treaty[i][j]) {
                  Diplomacy.OP[i][j] = op[i][j];
                  Diplomacy.OP[j][i] = op[j][i];
               }
            }
         }

         calm();
         broadcast(s, Text.literal("\ud83d\udd4a Королевства заключили мир").formatted(Formatting.WHITE));
      }
   }

   public static void calm() {
      for (Kingdoms.K k : Kingdoms.K.values()) {
         for (MobEntity m : k.members) {
            LivingEntity t = m.getTarget();
            if (t != null && of(t) != null) {
               m.setTarget(null);
            }
         }
      }
   }

   public static void info(ServerPlayerEntity god) {
      if (aliveCount() == 0) {
         msg(god, "Королевств пока нет", Formatting.GRAY);
      } else {
         MutableText t = Text.literal(war ? "⚔ Война! " : "\ud83d\udd4a Мир. ").formatted(war ? Formatting.RED : Formatting.WHITE);

         for (Kingdoms.K k : Kingdoms.K.values()) {
            if (k.alive) {
               int v = 0;

               for (MobEntity m : k.members) {
                  if (m instanceof VillagerEntity) {
                     v++;
                  }
               }

               t.append(
                  Text.literal(k.title + " (" + Races.of(k).title.toLowerCase() + "): " + v + " жит., " + (k.members.size() - v) + " воинов  ")
                     .formatted(k.color)
               );
            }
         }

         for (Kingdoms.K kk : Kingdoms.K.values()) {
            if (kk.alive) {
               t.append(Text.literal("\n" + kk.title + ": " + Civ.summary(kk)).formatted(kk.color));
            }
         }

         god.sendMessage(t, false);
      }
   }

   public static void disband(ServerPlayerEntity god, ServerWorld w, BlockPos pos) {
      Kingdoms.K k = nearest(w, pos);
      if (k == null) {
         msg(god, "Рядом нет королевств", Formatting.GRAY);
      } else {
         disbandKingdom(k);
         msg(god, k.title + " распущено", k.color);
      }
   }

   static void disbandKingdom(Kingdoms.K k) {
      Civ.remove(k);

      for (MobEntity m : k.members) {
         m.setGlowing(false);
      }

      if (k.world != null) {
         ServerScoreboard sb = k.world.getServer().getScoreboard();
         Team t = sb.getTeam(k.team());
         if (t != null) {
            sb.removeTeam(t);
         }
      }

      k.members.clear();
      k.alive = false;
      Diplomacy.forget(k);
      Treaties.forget(k);
      Conquest.forget(k);
      Races.forget(k);
      if (aliveCount() < 2) {
         war = false;
      }
   }

   public static void tick(MinecraftServer server) {
      if (++timer % 20 == 0) {
         if (aliveCount() != 0) {
            for (Kingdoms.K k : Kingdoms.K.values()) {
               k.members.clear();
            }

            Set<ServerWorld> worlds = new HashSet<>();

            for (Kingdoms.K k : Kingdoms.K.values()) {
               if (k.alive && k.world != null) {
                  worlds.add(k.world);
               }
            }

            for (ServerWorld w : worlds) {
               for (Entity e : w.iterateEntities()) {
                  if (e instanceof MobEntity m && m.isAlive()) {
                     Kingdoms.K kx = of(m);
                     if (kx != null && kx.alive && kx.world == w) {
                        kx.members.add(m);
                        if (!(m instanceof VillagerEntity) && PREPARED.add(m)) {
                           m.targetSelector.clear(g -> true);
                        }
                     }
                  }
               }
            }

            for (Kingdoms.K kx : Kingdoms.K.values()) {
               if (kx.alive && !Civ.economy && ++kx.grow >= 25) {
                  kx.grow = 0;
                  if (kx.members.size() < 150 && loaded(kx)) {
                     Random r = kx.world.getRandom();
                     if (r.nextBoolean()) {
                        spawn(kx, EntityType.VILLAGER, around(kx.world, kx.capital, 3));
                     } else {
                        spawn(kx, soldier(r), around(kx.world, kx.capital, 3));
                     }
                  }
               }
            }

            for (Kingdoms.K kxx : Kingdoms.K.values()) {
               if (kxx.alive) {
                  for (MobEntity mx : new ArrayList<>(kxx.members)) {
                     if (!(mx instanceof VillagerEntity)) {
                        LivingEntity t = mx.getTarget();
                        if (t != null && (!t.isAlive() || of(t) == kxx || GodManager.isGodEntity(t) || of(t) != null && !Diplomacy.hostile(kxx, of(t)))) {
                           mx.setTarget(null);
                           t = null;
                        }

                        LivingEntity att = mx.getAttacker();
                        if (att != null
                           && att != t
                           && att.isAlive()
                           && of(att) != kxx
                           && !GodManager.isGodEntity(att)
                           && (of(att) == null || !Diplomacy.allied(kxx, of(att)))) {
                           mx.setTarget(att);
                           t = att;
                        }

                        if (t == null) {
                           t = Diplomacy.atWar(kxx) ? nearestEnemy(kxx, mx, 32.0) : nearestMonster(mx, 16.0);
                           if (t == null && Diplomacy.atWar(kxx)) {
                              t = nearestMonster(mx, 16.0);
                           }

                           if (t != null) {
                              mx.setTarget(t);
                           }
                        }

                        if (t == null) {
                           BlockPos goal = null;
                           if (Diplomacy.atWar(kxx)) {
                              goal = Conquest.target(kxx, mx);
                           }

                           if (goal == null && Diplomacy.atWar(kxx)) {
                              goal = nearestEnemyCapital(kxx, mx);
                           } else if (kxx.capital != null && mx.getBlockPos().getSquaredDistance(kxx.capital) > 2304.0) {
                              goal = kxx.capital;
                           }

                           if (goal != null && mx.getNavigation().isIdle()) {
                              mx.getNavigation().startMovingTo(goal.getX() + 0.5, goal.getY(), goal.getZ() + 0.5, 1.0);
                           }
                        }
                     }
                  }
               }
            }

            if (war || Diplomacy.anyWar()) {
               for (Kingdoms.K kxxx : Kingdoms.K.values()) {
                  if (kxxx.alive) {
                     if (kxxx.members.isEmpty() && loaded(kxxx)) {
                        if (++kxxx.fallTimer >= 5) {
                           broadcast(server, Text.literal("☠ " + kxxx.title + " пало!").formatted(kxxx.color, Formatting.BOLD));
                           if (!Conquest.fallTo(server, kxxx)) {
                              disbandKingdom(kxxx);
                           }
                        }
                     } else {
                        kxxx.fallTimer = 0;
                     }
                  }
               }

               if (aliveCount() <= 1) {
                  war = false;

                  for (Kingdoms.K kxxxx : Kingdoms.K.values()) {
                     if (kxxxx.alive) {
                        broadcast(server, Text.literal("\ud83d\udc51 " + kxxxx.title + " победило в войне!").formatted(kxxxx.color, Formatting.BOLD));
                     }
                  }
               }
            }
         }
      }
   }

   private static LivingEntity nearestEnemy(Kingdoms.K k, MobEntity m, double r) {
      LivingEntity best = null;
      double bd = r * r;

      for (Kingdoms.K o : Kingdoms.K.values()) {
         if (o != k && o.alive && o.world == k.world && Diplomacy.hostile(k, o)) {
            for (MobEntity e : o.members) {
               double d = e.squaredDistanceTo(m);
               if (d < bd && e.isAlive()) {
                  bd = d;
                  best = e;
               }
            }
         }
      }

      return best;
   }

   private static LivingEntity nearestMonster(MobEntity m, double r) {
      LivingEntity best = null;
      double bd = r * r;

      for (MobEntity e : m.getWorld()
         .getEntitiesByClass(MobEntity.class, m.getBoundingBox().expand(r), ex -> ex instanceof Monster && ex.isAlive() && of(ex) == null)) {
         double d = e.squaredDistanceTo(m);
         if (d < bd) {
            bd = d;
            best = e;
         }
      }

      return best;
   }

   private static BlockPos nearestEnemyCapital(Kingdoms.K k, MobEntity m) {
      BlockPos best = null;
      double bd = Double.MAX_VALUE;

      for (Kingdoms.K o : Kingdoms.K.values()) {
         if (o != k && o.alive && o.world == k.world && Diplomacy.hostile(k, o)) {
            double d = o.capital.getSquaredDistance(m.getBlockPos());
            if (d < bd) {
               bd = d;
               best = o.capital;
            }
         }
      }

      return best;
   }

   public static void clear() {
      for (Kingdoms.K k : Kingdoms.K.values()) {
         k.alive = false;
         k.world = null;
         k.capital = null;
         k.members.clear();
      }

      war = false;
      eternalPeace = false;
      timer = 0;
      PREPARED.clear();
      Races.clear();
      Civ.clear();
   }

   public static enum K {
      RED("red", "Красное королевство", Formatting.RED, 11546150, Blocks.RED_WOOL, DyeColor.RED),
      BLUE("blue", "Синее королевство", Formatting.BLUE, 3949738, Blocks.BLUE_WOOL, DyeColor.BLUE),
      GREEN("green", "Зелёное королевство", Formatting.GREEN, 6192150, Blocks.GREEN_WOOL, DyeColor.GREEN),
      YELLOW("yellow", "Жёлтое королевство", Formatting.YELLOW, 16701501, Blocks.YELLOW_WOOL, DyeColor.YELLOW),
      ORANGE("orange", "Оранжевое королевство", Formatting.GOLD, 16351261, Blocks.ORANGE_WOOL, DyeColor.ORANGE),
      PURPLE("purple", "Фиолетовое королевство", Formatting.DARK_PURPLE, 8991416, Blocks.PURPLE_WOOL, DyeColor.PURPLE),
      CYAN("cyan", "Бирюзовое королевство", Formatting.DARK_AQUA, 1481884, Blocks.CYAN_WOOL, DyeColor.CYAN),
      PINK("pink", "Розовое королевство", Formatting.LIGHT_PURPLE, 15961002, Blocks.PINK_WOOL, DyeColor.PINK),
      WHITE("white", "Белое королевство", Formatting.WHITE, 16383998, Blocks.WHITE_WOOL, DyeColor.WHITE),
      BLACK("black", "Чёрное королевство", Formatting.DARK_GRAY, 2829107, Blocks.BLACK_WOOL, DyeColor.BLACK),
      LIGHT_BLUE("light_blue", "Голубое королевство", Formatting.AQUA, 3847130, Blocks.LIGHT_BLUE_WOOL, DyeColor.LIGHT_BLUE),
      BROWN("brown", "Коричневое королевство", Formatting.DARK_RED, 8606770, Blocks.BROWN_WOOL, DyeColor.BROWN);

      public final String id;
      public volatile String title;
      public final String defaultTitle;
      public final Formatting color;
      public final int rgb;
      public final Block wool;
      public final DyeColor dye;
      ServerWorld world;
      BlockPos capital;
      boolean alive;
      int grow;
      int fallTimer;
      final List<MobEntity> members = new ArrayList<>();

      private K(String id, String title, Formatting color, int rgb, Block wool, DyeColor dye) {
         this.dye = dye;
         this.id = id;
         this.title = title;
         this.defaultTitle = title;
         this.color = color;
         this.rgb = rgb;
         this.wool = wool;
      }

      public String team() {
         return "godbox_" + this.id;
      }

      public Block banner() {
         return Registries.BLOCK.get(Identifier.ofVanilla(this.dye.getName() + "_banner"));
      }

      public Item bannerItem() {
         return Registries.ITEM.get(Identifier.ofVanilla(this.dye.getName() + "_banner"));
      }

      public Block bed() {
         return Registries.BLOCK.get(Identifier.ofVanilla(this.dye.getName() + "_bed"));
      }
   }
}
