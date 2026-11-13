package com.vladik.godbox;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameRules.BooleanRule;
import net.minecraft.world.GameRules.IntRule;
import net.minecraft.world.GameRules.Key;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.World.ExplosionSourceType;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class Powers3 {
   private static final DustParticleEffect BLACK = new DustParticleEffect(new Vector3f(0.02F, 0.0F, 0.05F), 4.0F);
   private static final DustParticleEffect GREEN = new DustParticleEffect(new Vector3f(0.3F, 1.0F, 0.3F), 2.0F);
   private static final DustParticleEffect GREY = new DustParticleEffect(new Vector3f(0.55F, 0.6F, 0.65F), 3.0F);

   private Powers3() {
   }

   private static void msg(ServerPlayerEntity god, String s, Formatting f) {
      god.sendMessage(Text.literal(s).formatted(f), true);
   }

   private static void cmd(ServerPlayerEntity god, String s) {
      MinecraftServer srv = god.getServer();
      if (srv != null) {
         srv.getCommandManager().executeWithPrefix(god.getCommandSource().withLevel(4).withSilent(), s);
      }
   }

   private static BlockPos top(ServerWorld w, int x, int z) {
      return !w.isChunkLoaded(x >> 4, z >> 4) ? null : w.getTopPosition(Type.MOTION_BLOCKING, new BlockPos(x, 0, z));
   }

   public static boolean handles(Power p) {
      int o = p.ordinal();
      return o >= Power.BLACK_HOLE.ordinal() && o < Power.WORLD_DAY.ordinal() || o > Power.WORLD_BANISH.ordinal();
   }

   public static boolean recordsUndo(Power p) {
      return p.category != null && p.category != Category.WORLD && p.category != Category.LAWS && !p.name().startsWith("GEN_");
   }

   public static boolean early(ServerPlayerEntity god, Power power, BlockPos pos, int brush, boolean first) {
      Inspect.onPower(god, power, first);
      switch (power) {
         case CIV_STATS:
            if (first) {
               Stats.send(god, brush != -1);
            }

            return true;
         case GOD_HAND:
            Hand.use(god, pos, brush, first);
            return true;
         case UNDO:
            if (first) {
               Undo.undo(god);
            }

            return true;
         case CAMERA_TP:
            if (first) {
               god.requestTeleport(pos.getX() + 0.5, god.getY(), pos.getZ() + 0.5);
            }

            return true;
         case TIME_PAUSE:
            if (first) {
               ServerTickManager tm = god.getServer().getTickManager();
               boolean f = !tm.isFrozen();
               tm.setFrozen(f);
               msg(god, f ? "⏸ Время остановлено" : "▶ Время идёт", Formatting.AQUA);
            }

            return true;
         default:
            return false;
      }
   }

   public static void use(ServerPlayerEntity god, Power power, ServerWorld w, BlockPos pos, Vec3d c, int brush, Random r) {
      switch (power) {
         case BLACK_HOLE:
            Tasks.add(new Powers3.BlackHole(w, c.add(0.0, 8.0, 0.0), brush));
            msg(god, "Чёрная дыра открылась!", Formatting.DARK_PURPLE);
            break;
         case TSUNAMI:
            Tasks.add(new Powers3.Tsunami(w, pos, god.getYaw(), brush));
            msg(god, "Цунами идёт вперёд по взгляду камеры", Formatting.BLUE);
            break;
         case ICE_AGE:
            Tasks.add(new Powers3.IceAge(god, w, pos, brush));
            msg(god, "Наступает ледниковый период...", Formatting.AQUA);
            break;
         case ZOMBIE_APOCALYPSE:
            Tasks.add(new Powers3.Zombies(w, pos, brush));
            msg(god, "Зомби-апокалипсис!", Formatting.DARK_GREEN);
            break;
         case ALIEN_INVASION:
            Tasks.add(new Powers3.Aliens(w, pos, brush));
            msg(god, "Вторжение пришельцев!", Formatting.GREEN);
            break;
         case BOSS_CRAB:
         case BOSS_ROBOT:
         case BOSS_DRAGON:
            Bosses.spawn(god, power.arg, w, pos);
            break;
         case BOSS_ORDER:
            Bosses.command(god, w, pos);
            break;
         case BOSS_REMOVE:
            Bosses.remove(god);
            break;
         case KINGDOM_RED:
         case KINGDOM_BLUE:
         case KINGDOM_GREEN:
         case KINGDOM_YELLOW:
            Kingdoms.found(god, power.arg, w, pos, brush);
            break;
         case KINGDOM_ARMY:
            Kingdoms.army(god, w, pos, brush);
            break;
         case KINGDOM_NEW:
            Kingdoms.foundNew(god, w, pos, brush);
            break;
         case RACE_HUMAN:
            Races.power(god, w, pos, Races.Race.HUMAN);
            break;
         case RACE_ELF:
            Races.power(god, w, pos, Races.Race.ELF);
            break;
         case RACE_ORC:
            Races.power(god, w, pos, Races.Race.ORC);
            break;
         case RACE_DWARF:
            Races.power(god, w, pos, Races.Race.DWARF);
            break;
         case SEASON_NEXT:
            Seasons.power(god);
            break;
         case HISTORY:
            History.power(god);
            break;
         case INSPECT:
            Inspect.power(god, w, pos);
            break;
         case EPIDEMIC:
            Plague.power(god, w, pos);
            break;
         case QUARANTINE:
            Plague.quarantine(god, w, pos);
            break;
         case ROYAL_MARRIAGE:
            Treaties.power(god, w, pos);
            break;
         case SCENARIO:
            Mortal.cycle(god, w, pos);
            break;
         case KINGDOM_CREST:
            Banners.reroll(god, w, pos);
            break;
         case KINGDOM_WAR:
            Kingdoms.setWar(god, true);
            break;
         case KINGDOM_PEACE:
            Kingdoms.setWar(god, false);
            break;
         case KINGDOM_INFO:
            Kingdoms.info(god);
            break;
         case KINGDOM_DISBAND:
            Kingdoms.disband(god, w, pos);
            break;
         case KINGDOM_FRIEND:
            Diplomacy.sway(god, w, pos, 35);
            break;
         case KINGDOM_SPITE:
            Diplomacy.sway(god, w, pos, -35);
            break;
         case KINGDOM_CARAVAN:
            Diplomacy.forceCaravan(god, w, pos);
            break;
         case KINGDOM_DIPLO:
            Diplomacy.info(god);
            break;
         case CIV_CHRONICLE:
            Chronicle.show(god);
            break;
         case GIFT_FOOD:
            Faith.gift(god, w, pos, 2, brush);
            break;
         case GIFT_WOOD:
            Faith.gift(god, w, pos, 0, brush);
            break;
         case GIFT_STONE:
            Faith.gift(god, w, pos, 1, brush);
            break;
         case GIFT_ORE:
            Faith.gift(god, w, pos, 3, brush);
            break;
         case GIFT_ALL:
            Faith.gift(god, w, pos, -1, brush);
            break;
         case AGE_UP:
            Ages.force(god, w, pos);
            break;
         case FAITH_NEW:
            Faith.newFaith(god, w, pos);
            break;
         case FAITH_PREACH:
            Faith.preach(god, w, pos);
            break;
         case FAITH_MIRACLE:
            Faith.miracle(god, w, pos);
            break;
         case CULTURE_NEXT:
            Faith.nextCulture(god, w, pos);
            break;
         case CR_NECRO:
         case CR_EVIL_MAGE:
         case CR_WHITE_MAGE:
         case CR_DRUID:
         case CR_PLAGUE_DOCTOR:
         case CR_BANDIT:
         case CR_FIRE_ELEMENTAL:
         case CR_CRYSTAL_GOLEM:
         case CR_FIRE_SKULL:
         case CR_JUMPY_SKULL:
            Creatures.spawn(god, power, w, pos, brush, r);
            break;
         case CIV_INSPIRE:
            Rebels.inspire(god, w, pos);
            break;
         case AGE_HOPE:
         case AGE_SUN:
         case AGE_WONDERS:
         case AGE_CHAOS:
         case AGE_DARK:
         case AGE_MOON:
         case AGE_ICE:
         case AGE_ASH:
         case AGE_DESPAIR:
            WorldAges.set(god.getServer(), Integer.parseInt(power.arg));
            break;
         case TRAIT_STRONG:
         case TRAIT_FAST:
         case TRAIT_TOUGH:
         case TRAIT_REGEN:
         case TRAIT_GIANT:
         case TRAIT_TINY:
         case TRAIT_FIREPROOF:
         case TRAIT_IMMORTAL:
         case TRAIT_CURSED:
         case TRAIT_CLEAR:
            Traits.brush(god, w, pos, brush, power.arg);
            break;
         case CIV_TOWN:
         case CIV_PORT:
         case CIV_SHIP:
            Colonies.power(god, w, pos, power.arg);
            break;
         case GOLD_GIFT:
            Economy.gift(god, w, pos);
            break;
         case VASSALIZE:
            Vassals.power(god, w, pos);
            break;
         case CIVIL_WAR:
            Unrest.power(god, w, pos);
            break;
         case HERO_BLESS:
            Heroes.power(god, w, pos);
            break;
         case HIGHWAY_BUILD:
            Highways.power(god, w, pos);
            break;
         case BEAUTIFY:
            Decor.power(god, w, pos);
            break;
         case PRAYER_ANSWER:
            Prayers.answer(god, w, pos);
            break;
         case POSSESS:
            Possess.use(god, w, pos);
            break;
         case ACHIEVEMENTS:
            Achievements.show(god);
            break;
         case PREDATORS:
            Ecosystem.power(god, w, pos);
            break;
         case HERD_TAME:
            Herds.power(god, w, pos);
            break;
         case LIVE_VOLCANO:
            Tectonics.volcano(god, w, pos);
            break;
         case RIFT:
            Tectonics.rift(god, w, pos);
            break;
         case TECH_GIFT:
            Tech.gift(god, w, pos);
            break;
         case WONDER_PYRAMID:
         case WONDER_LIBRARY:
         case WONDER_LIGHTHOUSE:
         case WONDER_COLOSSUS:
         case WONDER_GARDENS:
            Wonders.power(god, w, pos, power.arg);
            break;
         case MAGE_SPAWN:
         case NECRO_SPAWN:
            Magic.power(god, w, pos, power.arg);
            break;
         case KRAKEN:
            Navy.power(god, w, pos);
            break;
         case ROAD_BUILD:
            Roads.power(god, w, pos);
            break;
         case WALLS_BUILD:
            Siege.power(god, w, pos);
            break;
         case WORLD_EVENT:
            Events.force(god, w, pos);
            break;
         case CIV_ANNEX:
            Conquest.force(god, w, pos);
            break;
         case DEMOLISH:
            Demolish.one(god, w, pos, brush, false);
            break;
         case RUINS:
            Demolish.one(god, w, pos, brush, true);
            break;
         case DEMOLISH_AREA:
            Demolish.area(god, w, pos, brush);
            break;
         case GEN_CONTINENT:
         case GEN_ISLANDS:
         case GEN_ARCHIPELAGO:
         case GEN_PANGAEA:
         case GEN_MOUNTAINS:
         case GEN_RANDOM:
            WorldGen.start(god, w, pos, brush, power.arg);
            break;
         case ORE_MIX:
         case ORE_COAL:
         case ORE_IRON:
         case ORE_COPPER:
         case ORE_GOLD:
         case ORE_REDSTONE:
         case ORE_LAPIS:
         case ORE_DIAMOND:
         case ORE_EMERALD:
         case ORE_DEBRIS:
            Ores.paint(god, w, pos, brush, power.arg, r);
            break;
         case TIME_NORMAL:
         case TIME_FAST:
         case TIME_FASTER:
         case TIME_SLOW:
            ServerTickManager tm = god.getServer().getTickManager();
            tm.setFrozen(false);
            tm.setTickRate(Float.parseFloat(power.arg));
            msg(god, "Скорость времени: " + power.title, Formatting.AQUA);
            break;
         default:
            if (power.category == Category.LAWS) {
               law(god, power);
            }
      }
   }

   static long lawMask(MinecraftServer s) {
      long mask = 0L;
      int i = 0;

      for (Power p : Power.values()) {
         if (p.category == Category.LAWS) {
            if (i < 64 && lawState(s, p)) {
               mask |= 1L << i;
            }

            i++;
         }
      }

      return mask;
   }

   private static void law(ServerPlayerEntity god, Power power) {
      MinecraftServer s = god.getServer();
      GameRules g = s.getGameRules();
      String a = power.arg == null ? "" : power.arg;
      if (a.equals("seasons")) {
         Seasons.on = !Seasons.on;
         msg(god, "Закон «" + power.title + "»: " + (Seasons.on ? "включён" : "выключен"), Seasons.on ? Formatting.GREEN : Formatting.GRAY);
      } else if (a.equals("mortal")) {
         Mortal.toggle(god);
      } else if (!a.equals("storage") && !a.equals("decor")) {
         if (a.equals("prayers") || a.equals("eco") || a.equals("herds") || a.equals("tectonics") || a.equals("perf")) {
            boolean v23 = switch (a) {
               case "prayers" -> Prayers.on = !Prayers.on;
               case "eco" -> Ecosystem.on = !Ecosystem.on;
               case "herds" -> Herds.on = !Herds.on;
               case "tectonics" -> Tectonics.on = !Tectonics.on;
               default -> Perf.on = !Perf.on;
            };
            msg(god, "Закон «" + power.title + "»: " + (v23 ? "включён" : "выключен"), v23 ? Formatting.GREEN : Formatting.GRAY);
         } else if (a.equals("economy") || a.equals("vassals") || a.equals("rebels") || a.equals("heroes") || a.equals("highways")) {
            boolean v = switch (a) {
               case "economy" -> Economy.on = !Economy.on;
               case "vassals" -> Vassals.on = !Vassals.on;
               case "rebels" -> Unrest.on = !Unrest.on;
               case "heroes" -> Heroes.on = !Heroes.on;
               default -> Highways.on = !Highways.on;
            };
            msg(god, "Закон «" + power.title + "»: " + (v ? "включён" : "выключен"), v ? Formatting.GREEN : Formatting.GRAY);
         } else if (a.equals("tech") || a.equals("wonders") || a.equals("magic") || a.equals("roads") || a.equals("walls") || a.equals("navy")) {
            boolean v = switch (a) {
               case "tech" -> Tech.on = !Tech.on;
               case "wonders" -> Wonders.on = !Wonders.on;
               case "magic" -> Magic.on = !Magic.on;
               case "roads" -> Roads.on = !Roads.on;
               case "walls" -> Siege.on = !Siege.on;
               default -> Navy.on = !Navy.on;
            };
            msg(god, "Закон «" + power.title + "»: " + (v ? "включён" : "выключен"), v ? Formatting.GREEN : Formatting.GRAY);
         } else if (a.equals("plague")) {
            Plague.on = !Plague.on;
            msg(god, "Закон «" + power.title + "»: " + (Plague.on ? "включён" : "выключен"), Plague.on ? Formatting.GREEN : Formatting.GRAY);
         } else if (a.equals("expand")) {
            Colonies.on = !Colonies.on;
            msg(god, "Закон «Расселение»: " + (Colonies.on ? "включён" : "выключен"), Colonies.on ? Formatting.GREEN : Formatting.GRAY);
         } else if (a.equals("sea")) {
            Ships.on = !Ships.on;
            msg(god, "Закон «Мореходство»: " + (Ships.on ? "включён" : "выключен"), Ships.on ? Formatting.GREEN : Formatting.GRAY);
         } else if (a.equals("ages")) {
            WorldAges.on = !WorldAges.on;
            msg(
               god,
               "Закон «Смена эпох мира»: "
                  + (WorldAges.on ? "включён (сейчас " + WorldAges.current() + ")" : "выключен — эпоха замерла: " + WorldAges.current()),
               WorldAges.on ? Formatting.GREEN : Formatting.GRAY
            );
         } else if (a.equals("treaties")) {
            Treaties.on = !Treaties.on;
            msg(god, "Закон «" + power.title + "»: " + (Treaties.on ? "включён" : "выключен"), Treaties.on ? Formatting.GREEN : Formatting.GRAY);
         } else if (a.equals("conquest")) {
            Conquest.on = !Conquest.on;
            msg(
               god,
               "Закон «Захват королевств»: " + (Conquest.on ? "включён" : "выключен — павшие королевства просто исчезают"),
               Conquest.on ? Formatting.GREEN : Formatting.GRAY
            );
         } else if (a.equals("events")) {
            Events.on = !Events.on;
            msg(god, "Закон «Случайные события»: " + (Events.on ? "включён" : "выключен"), Events.on ? Formatting.GREEN : Formatting.GRAY);
         } else if (a.equals("sleep")) {
            Sleep.on = !Sleep.on;
            msg(
               god,
               "Закон «Сон в кроватях»: " + (Sleep.on ? "включён — ночью жители спят в кроватях" : "выключен"),
               Sleep.on ? Formatting.GREEN : Formatting.GRAY
            );
         } else if (a.equals("diplomacy")) {
            Diplomacy.auto = !Diplomacy.auto;
            msg(
               god,
               "Закон «Дипломатия»: " + (Diplomacy.auto ? "включён — королевства сами заключают союзы, торгуют и воюют" : "выключен"),
               Diplomacy.auto ? Formatting.GREEN : Formatting.GRAY
            );
         } else {
            boolean on;
            if (a.equals("peace")) {
               on = !Kingdoms.eternalPeace;
               Kingdoms.eternalPeace = on;
               if (on && Kingdoms.war) {
                  Kingdoms.war = false;
                  Kingdoms.calm();
               }
            } else if (a.equals("randomTickSpeed")) {
               IntRule rule = g.get(GameRules.RANDOM_TICK_SPEED);
               on = rule.get() <= 3;
               rule.set(on ? 30 : 3, s);
            } else {
               Key<BooleanRule> key = switch (a) {
                  case "doFireTick" -> GameRules.DO_FIRE_TICK;
                  case "doMobSpawning" -> GameRules.DO_MOB_SPAWNING;
                  case "doDaylightCycle" -> GameRules.DO_DAYLIGHT_CYCLE;
                  case "doWeatherCycle" -> GameRules.DO_WEATHER_CYCLE;
                  case "mobGriefing" -> GameRules.DO_MOB_GRIEFING;
                  case "doMobLoot" -> GameRules.DO_MOB_LOOT;
                  case "doInsomnia" -> GameRules.DO_INSOMNIA;
                  default -> null;
               };
               if (key == null) {
                  return;
               }

               BooleanRule rule = g.get(key);
               on = !rule.get();
               rule.set(on, s);
            }

            god.sendMessage(
               Text.literal("Закон «" + power.title + "»: ")
                  .formatted(Formatting.GOLD)
                  .append(Text.literal(on ? "ВКЛ" : "ВЫКЛ").formatted(on ? Formatting.GREEN : Formatting.RED, Formatting.BOLD)),
               true
            );
         }
      } else {
         boolean v25 = a.equals("storage") ? (Storage.on = !Storage.on) : (Decor.on = !Decor.on);
         msg(god, "Закон «" + power.title + "»: " + (v25 ? "включён" : "выключен"), v25 ? Formatting.GREEN : Formatting.GRAY);
      }
   }

   public static boolean lawState(MinecraftServer s, Power power) {
      String a = power.arg == null ? "" : power.arg;
      GameRules g = s.getGameRules();

      return switch (a) {
         case "mortal" -> Mortal.on;
         case "peace" -> Kingdoms.eternalPeace;
         case "diplomacy" -> Diplomacy.auto;
         case "events" -> Events.on;
         case "ages" -> WorldAges.on;
         case "expand" -> Colonies.on;
         case "sea" -> Ships.on;
         case "tech" -> Tech.on;
         case "wonders" -> Wonders.on;
         case "magic" -> Magic.on;
         case "roads" -> Roads.on;
         case "walls" -> Siege.on;
         case "navy" -> Navy.on;
         case "economy" -> Economy.on;
         case "vassals" -> Vassals.on;
         case "rebels" -> Unrest.on;
         case "heroes" -> Heroes.on;
         case "highways" -> Highways.on;
         case "prayers" -> Prayers.on;
         case "eco" -> Ecosystem.on;
         case "herds" -> Herds.on;
         case "tectonics" -> Tectonics.on;
         case "perf" -> Perf.on;
         case "storage" -> Storage.on;
         case "decor" -> Decor.on;
         case "seasons" -> Seasons.on;
         case "plague" -> Plague.on;
         case "conquest" -> Conquest.on;
         case "treaties" -> Treaties.on;
         case "sleep" -> Sleep.on;
         case "randomTickSpeed" -> g.getInt(GameRules.RANDOM_TICK_SPEED) > 3;
         case "doFireTick" -> g.getBoolean(GameRules.DO_FIRE_TICK);
         case "doMobSpawning" -> g.getBoolean(GameRules.DO_MOB_SPAWNING);
         case "doDaylightCycle" -> g.getBoolean(GameRules.DO_DAYLIGHT_CYCLE);
         case "doWeatherCycle" -> g.getBoolean(GameRules.DO_WEATHER_CYCLE);
         case "mobGriefing" -> g.getBoolean(GameRules.DO_MOB_GRIEFING);
         case "doMobLoot" -> g.getBoolean(GameRules.DO_MOB_LOOT);
         case "doInsomnia" -> g.getBoolean(GameRules.DO_INSOMNIA);
         default -> false;
      };
   }

   private static final class Aliens implements Tasks.Task {
      final ServerWorld w;
      final BlockPos c;
      final List<Powers3.Ufo> ufos = new ArrayList<>();
      int t;

      Aliens(ServerWorld w, BlockPos c, int brush) {
         this.w = w;
         this.c = c;
         Random r = w.getRandom();
         int n = 2 + brush / 3;

         for (int i = 0; i < n; i++) {
            Powers3.Ufo u = new Powers3.Ufo();
            u.pos = new Vec3d(c.getX() + r.nextInt(41) - 20, c.getY() + 26, c.getZ() + r.nextInt(41) - 20);
            u.cooldown = 20 + r.nextInt(40);
            this.ufos.add(u);
         }

         Fx.sound(w, c.getX(), c.getY() + 20, c.getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE, 8.0F, 0.5F);
      }

      double ground(double x, double z, double fallback) {
         BlockPos p = Powers3.top(this.w, MathHelper.floor(x), MathHelper.floor(z));
         return p == null ? fallback : p.getY();
      }

      @Override
      public boolean tick() {
         this.t++;
         Random r = this.w.getRandom();

         for (Powers3.Ufo u : this.ufos) {
            if (u.goal == null || u.goal.squaredDistanceTo(u.pos.x, u.goal.y, u.pos.z) < 4.0) {
               u.goal = new Vec3d(this.c.getX() + r.nextInt(81) - 40, 0.0, this.c.getZ() + r.nextInt(81) - 40);
            }

            double gy = this.ground(u.pos.x, u.pos.z, u.pos.y - 26.0) + 26.0;
            Vec3d flat = new Vec3d(u.goal.x - u.pos.x, 0.0, u.goal.z - u.pos.z);
            double len = flat.length();
            Vec3d mv = u.beam <= 0 && !(len < 0.01) ? flat.multiply(Math.min(0.5, len) / len) : Vec3d.ZERO;
            u.pos = new Vec3d(u.pos.x + mv.x, u.pos.y + (gy - u.pos.y) * 0.1, u.pos.z + mv.z);
            if (this.t % 2 == 0) {
               for (int i = 0; i < 16; i++) {
                  double a = i * Math.PI / 8.0 + this.t * 0.15;
                  Fx.particles(this.w, ParticleTypes.END_ROD, u.pos.x + Math.cos(a) * 3.0, u.pos.y, u.pos.z + Math.sin(a) * 3.0, 1, 0.0, 0.0, 0.0, 0.0);
               }

               Fx.particles(this.w, Powers3.GREY, u.pos.x, u.pos.y, u.pos.z, 12, 1.5, 0.2, 1.5, 0.0);
               Fx.particles(this.w, ParticleTypes.ELECTRIC_SPARK, u.pos.x, u.pos.y - 0.5, u.pos.z, 3, 1.0, 0.1, 1.0, 0.05);
            }

            if (u.beam > 0) {
               u.beam--;
               double gr = this.ground(u.pos.x, u.pos.z, u.pos.y - 26.0);
               if (this.t % 2 == 0) {
                  for (double y = gr; y < u.pos.y; y++) {
                     Fx.particles(this.w, Powers3.GREEN, u.pos.x, y, u.pos.z, 2, 0.6, 0.3, 0.6, 0.0);
                  }
               }

               Box col = new Box(u.pos.x - 2.5, gr - 1.0, u.pos.z - 2.5, u.pos.x + 2.5, u.pos.y + 1.0, u.pos.z + 2.5);

               for (LivingEntity e : this.w.getEntitiesByClass(LivingEntity.class, col, ex -> ex.isAlive() && !GodManager.isGodEntity(ex) && !ex.isSpectator())) {
                  if (!(e instanceof PlayerEntity p && p.isCreative())) {
                     Vec3d toC = new Vec3d(u.pos.x - e.getX(), 0.0, u.pos.z - e.getZ()).multiply(0.1);
                     e.setVelocity(e.getVelocity().multiply(0.5).add(toC).add(0.0, 0.3, 0.0));
                     e.velocityModified = true;
                     e.fallDistance = 0.0F;
                     if (!(e instanceof PlayerEntity) && e.getY() > u.pos.y - 3.0) {
                        Fx.particles(this.w, ParticleTypes.PORTAL, e.getX(), e.getY(), e.getZ(), 30, 0.5, 0.5, 0.5, 0.3);
                        Fx.sound(this.w, e.getX(), e.getY(), e.getZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, 2.0F, 1.5F);
                        e.discard();
                     }
                  }
               }
            } else if (--u.cooldown <= 0) {
               u.cooldown = 40 + r.nextInt(50);
               int action = r.nextInt(3);
               if (action == 0) {
                  u.beam = 70;
                  Fx.sound(this.w, u.pos.x, u.pos.y, u.pos.z, SoundEvents.BLOCK_BEACON_POWER_SELECT, 5.0F, 1.6F);
               } else if (action == 1) {
                  Vec3d to = null;
                  List<LivingEntity> near = Fx.living(this.w, new Vec3d(u.pos.x, u.pos.y - 26.0, u.pos.z), 30.0);
                  near.removeIf(ex -> GodManager.isGodEntity(ex) || ex.isSpectator());
                  if (!near.isEmpty()) {
                     LivingEntity ex = near.get(r.nextInt(near.size()));
                     to = ex.getPos();
                  } else {
                     double x = u.pos.x + r.nextInt(31) - 15.0;
                     double z = u.pos.z + r.nextInt(31) - 15.0;
                     to = new Vec3d(x, this.ground(x, z, u.pos.y - 26.0), z);
                  }

                  Bosses.beam(this.w, u.pos, to, Powers3.GREEN);
                  Fx.sound(this.w, u.pos.x, u.pos.y, u.pos.z, SoundEvents.ENTITY_GUARDIAN_ATTACK, 5.0F, 1.8F);
                  this.w.createExplosion(null, to.x, to.y, to.z, 2.5F, true, ExplosionSourceType.TNT);
               } else {
                  BlockPos px = Powers3.top(this.w, MathHelper.floor(u.pos.x), MathHelper.floor(u.pos.z));
                  if (px != null) {
                     MobEntity alien = EntityType.ENDERMAN.spawn(this.w, px, SpawnReason.TRIGGERED);
                     if (alien != null) {
                        alien.setCustomName(Text.literal("Пришелец").formatted(Formatting.GREEN));
                        // The model hides the Enderman, and a glowing outline would trace its hidden body.
                        if (!Models.attachCustom(alien, "alien")) {
                           alien.setGlowing(true);
                        }

                        Fx.particles(this.w, Powers3.GREEN, px.getX() + 0.5, px.getY() + 1, px.getZ() + 0.5, 40, 0.5, 1.0, 0.5, 0.0);
                     }
                  }
               }
            }
         }

         if (this.t % 40 == 0) {
            Fx.sound(this.w, this.c.getX(), this.c.getY() + 26, this.c.getZ(), SoundEvents.BLOCK_BEACON_AMBIENT, 6.0F, 2.0F);
         }

         if (this.t < 800) {
            return false;
         } else {
            for (Powers3.Ufo u : this.ufos) {
               Fx.particles(this.w, ParticleTypes.FLASH, u.pos.x, u.pos.y, u.pos.z, 1, 0.0, 0.0, 0.0, 0.0);
            }

            Fx.sound(this.w, this.c.getX(), this.c.getY() + 26, this.c.getZ(), SoundEvents.BLOCK_BEACON_DEACTIVATE, 8.0F, 0.6F);
            return true;
         }
      }
   }

   private static final class BlackHole implements Tasks.Task {
      final ServerWorld w;
      final Vec3d c;
      final int maxR;
      int t;
      ItemDisplayEntity core;
      ItemDisplayEntity ring;

      // The core model is about 0.44 blocks in radius and the disc 1.44 blocks, both at scale 1.
      private void model(double rad) {
         if (this.t == 1) {
            this.core = Models.prop(this.w, "blackhole", "core", this.c.x, this.c.y, this.c.z);
            this.ring = Models.prop(this.w, "blackhole", "ring", this.c.x, this.c.y, this.c.z);
         }

         if (this.core != null) {
            Models.pose(this.core, new Quaternionf().rotationY(this.t * 0.05F), (float)(rad * 0.7));
         }

         if (this.ring != null) {
            Models.pose(this.ring, new Quaternionf().rotationX(0.35F).rotateY(this.t * 0.22F), (float)(rad * 0.6));
         }

         if (this.t >= 400) {
            if (this.core != null) {
               this.core.discard();
            }

            if (this.ring != null) {
               this.ring.discard();
            }
         }
      }

      BlackHole(ServerWorld w, Vec3d c, int brush) {
         this.w = w;
         this.c = c;
         this.maxR = 6 + brush * 2;
      }

      @Override
      public boolean tick() {
         this.t++;
         double rad = Math.min((double)this.maxR, 2.0 + this.t / 15.0);
         Random r = this.w.getRandom();
         this.model(rad);
         Fx.particles(this.w, Powers3.BLACK, this.c.x, this.c.y, this.c.z, 25, rad * 0.25, rad * 0.25, rad * 0.25, 0.0);
         Fx.particles(this.w, ParticleTypes.REVERSE_PORTAL, this.c.x, this.c.y, this.c.z, 40, rad, rad, rad, 0.1);
         Fx.particles(this.w, ParticleTypes.SQUID_INK, this.c.x, this.c.y, this.c.z, 10, rad * 0.4, rad * 0.4, rad * 0.4, 0.02);
         if (this.t % 30 == 1) {
            Fx.sound(this.w, this.c.x, this.c.y, this.c.z, SoundEvents.BLOCK_BEACON_AMBIENT, 6.0F, 0.5F);
         }

         double pull = rad * 2.5 + 6.0;

         for (Entity e : this.w.getOtherEntities(null, new Box(this.c, this.c).expand(pull), ex -> !GodManager.isGodEntity(ex) && !ex.isSpectator() && !(ex instanceof DisplayEntity))) {
            if (!(e instanceof PlayerEntity p && p.isCreative())) {
               Vec3d d = this.c.subtract(e.getPos());
               double len = d.length();
               if (len < 1.8) {
                  if (e instanceof LivingEntity le) {
                     le.damage(this.w.getDamageSources().outOfWorld(), 1000.0F);
                  } else {
                     e.discard();
                  }
               } else {
                  Vec3d v = d.multiply(Math.min(0.6, 0.08 + 2.0 / len) / len);
                  e.setVelocity(e.getVelocity().multiply(0.8).add(v));
                  e.velocityModified = true;
                  e.fallDistance = 0.0F;
               }
            }
         }

         for (int i = 0; i < 25; i++) {
            double x = (r.nextDouble() * 2.0 - 1.0) * rad;
            double y = (r.nextDouble() * 2.0 - 1.0) * rad;
            double z = (r.nextDouble() * 2.0 - 1.0) * rad;
            if (!(x * x + y * y + z * z > rad * rad)) {
               BlockPos px = BlockPos.ofFloored(this.c.x + x, this.c.y + y, this.c.z + z);
               BlockState s = this.w.getBlockState(px);
               if (!s.isAir() && !(s.getHardness(this.w, px) < 0.0F)) {
                  if (r.nextInt(3) == 0) {
                     Fx.particles(
                        this.w, new BlockStateParticleEffect(ParticleTypes.BLOCK, s), px.getX() + 0.5, px.getY() + 0.5, px.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.1
                     );
                  }

                  this.w.setBlockState(px, Blocks.AIR.getDefaultState(), 3);
               }
            }
         }

         if (this.t >= 400) {
            this.w.createExplosion(null, this.c.x, this.c.y, this.c.z, 6.0F, false, ExplosionSourceType.TNT);
            Fx.particles(this.w, ParticleTypes.FLASH, this.c.x, this.c.y, this.c.z, 3, 0.0, 0.0, 0.0, 0.0);
            return true;
         } else {
            return false;
         }
      }
   }

   private static final class IceAge implements Tasks.Task {
      final ServerPlayerEntity god;
      final ServerWorld w;
      final BlockPos c;
      final int R;
      int x;
      int tile;
      int t;
      final List<int[]> tiles = new ArrayList<>();

      IceAge(ServerPlayerEntity god, ServerWorld w, BlockPos c, int brush) {
         this.god = god;
         this.w = w;
         this.c = c;
         this.R = 40 + brush * 8;
         this.x = -this.R;

         for (int bx = c.getX() - this.R; bx <= c.getX() + this.R; bx += 32) {
            for (int bz = c.getZ() - this.R; bz <= c.getZ() + this.R; bz += 32) {
               this.tiles.add(new int[]{bx, bz});
            }
         }

         w.setWeather(0, 6000, true, false);
      }

      @Override
      public boolean tick() {
         this.t++;
         Random r = this.w.getRandom();

         for (int k = 0; k < 3 && this.x <= this.R; this.x++) {
            for (int z = -this.R; z <= this.R; z++) {
               if (this.x * this.x + z * z <= this.R * this.R) {
                  BlockPos top = Powers3.top(this.w, this.c.getX() + this.x, this.c.getZ() + z);
                  if (top != null) {
                     BlockPos below = top.down();
                     BlockState s = this.w.getBlockState(below);
                     if (s.getFluidState().isIn(FluidTags.WATER)) {
                        if (s.getFluidState().isStill()) {
                           this.w.setBlockState(below, (r.nextInt(5) == 0 ? Blocks.PACKED_ICE : Blocks.ICE).getDefaultState(), 2);
                        }
                     } else if (s.getFluidState().isIn(FluidTags.LAVA)) {
                        this.w.setBlockState(below, Blocks.OBSIDIAN.getDefaultState(), 2);
                     } else {
                        BlockState at = this.w.getBlockState(top);
                        if (at.isOf(Blocks.FIRE) || at.isOf(Blocks.SOUL_FIRE)) {
                           this.w.setBlockState(top, Blocks.AIR.getDefaultState(), 2);
                           at = Blocks.AIR.getDefaultState();
                        }

                        if (!at.isOf(Blocks.SNOW) && (at.isAir() || at.isReplaceable() && at.getFluidState().isEmpty())) {
                           BlockState snow = Blocks.SNOW.getDefaultState().with(SnowBlock.LAYERS, 1 + r.nextInt(3));
                           if (snow.canPlaceAt(this.w, top)) {
                              this.w.setBlockState(top, snow, 2);
                           }
                        }
                     }
                  }
               }
            }

            k++;
         }

         for (int k = 0; k < 2 && this.tile < this.tiles.size(); this.tile++) {
            int[] a = this.tiles.get(this.tile);
            Powers3.cmd(
               this.god,
               "fillbiome "
                  + a[0]
                  + " "
                  + (this.c.getY() - 10)
                  + " "
                  + a[1]
                  + " "
                  + (a[0] + 31)
                  + " "
                  + (this.c.getY() + 20)
                  + " "
                  + (a[1] + 31)
                  + " minecraft:snowy_plains"
            );
            k++;
         }

         if (this.t % 20 == 0) {
            for (LivingEntity e : Fx.living(this.w, Vec3d.ofCenter(this.c), this.R)) {
               if (!GodManager.isGodEntity(e) && !e.isSpectator()) {
                  e.setFrozenTicks(e.getMinFreezeDamageTicks() + 30);
                  e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1));
               }
            }

            Fx.particles(this.w, ParticleTypes.SNOWFLAKE, this.c.getX(), this.c.getY() + 10, this.c.getZ(), 200, this.R / 2.0, 6.0, this.R / 2.0, 0.02);
         }

         return this.x > this.R && this.tile >= this.tiles.size() && this.t > 400;
      }
   }

   private static final class Tsunami implements Tasks.Task {
      final ServerWorld w;
      final Vec3d start;
      final Vec3d dir;
      final Vec3d side;
      final int width;
      final int height;
      final int length;
      int step;
      int t;
      final ArrayDeque<List<BlockPos>> rows = new ArrayDeque<>();
      final Set<BlockPos> active = new HashSet<>();

      Tsunami(ServerWorld w, BlockPos pos, float yaw, int brush) {
         this.w = w;
         Vec3d d = Vec3d.fromPolar(0.0F, yaw);
         this.dir = new Vec3d(d.x, 0.0, d.z).normalize();
         this.side = new Vec3d(-this.dir.z, 0.0, this.dir.x);
         this.width = 16 + brush * 4;
         this.height = 4 + brush / 2;
         this.length = 40 + brush * 6;
         this.start = Vec3d.ofCenter(pos).subtract(this.dir.multiply(12.0));
      }

      private void drain(List<BlockPos> row) {
         for (BlockPos p : row) {
            this.active.remove(p);
            if (this.w.getBlockState(p).isOf(Blocks.WATER)) {
               this.w.setBlockState(p, Blocks.AIR.getDefaultState(), 3);
            }
         }
      }

      @Override
      public boolean tick() {
         if (++this.t % 2 != 0) {
            return false;
         } else if (this.step >= this.length) {
            if (!this.rows.isEmpty()) {
               this.drain(this.rows.poll());
            }

            return this.rows.isEmpty();
         } else {
            Vec3d f = this.start.add(this.dir.multiply(this.step));
            int h = this.step < 4
               ? this.height / 2 + 1
               : (this.step > this.length - 6 ? Math.max(1, this.height * (this.length - this.step) / 6) : this.height);
            List<BlockPos> row = new ArrayList<>();

            for (int s = -this.width / 2; s <= this.width / 2; s++) {
               Vec3d q = f.add(this.side.multiply(s));
               int x = MathHelper.floor(q.x);
               int z = MathHelper.floor(q.z);
               if (this.w.isChunkLoaded(x >> 4, z >> 4)) {
                  BlockPos g = this.w.getTopPosition(Type.OCEAN_FLOOR, new BlockPos(x, 0, z));

                  while (this.w.getBlockState(g).getFluidState().isIn(FluidTags.WATER) && !this.active.contains(g) && g.getY() < this.w.getTopY()) {
                     g = g.up();
                  }

                  for (int y = 0; y < h; y++) {
                     BlockPos p = g.up(y);
                     if (!this.active.contains(p)) {
                        BlockState st = this.w.getBlockState(p);
                        if (!st.isAir()) {
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
                     Fx.particles(this.w, ParticleTypes.SPLASH, q.x, g.getY() + h, q.z, 6, 0.5, 0.3, 0.5, 0.2);
                  }
               }
            }

            for (LivingEntity e : Fx.living(this.w, f.add(0.0, 2.0, 0.0), this.width / 2.0 + 1.0)) {
               if (!GodManager.isGodEntity(e) && !e.isSpectator()) {
                  Vec3d rel = e.getPos().subtract(f);
                  double along = rel.dotProduct(this.dir);
                  if (!(along < -2.0) && !(along > 4.0)) {
                     e.setVelocity(e.getVelocity().multiply(0.5).add(this.dir.multiply(0.9)).add(0.0, 0.25, 0.0));
                     e.velocityModified = true;
                  }
               }
            }

            if (this.step % 8 == 0) {
               Fx.sound(this.w, f.x, f.y + h, f.z, SoundEvents.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 6.0F, 0.5F);
            }

            this.rows.add(row);
            this.step++;
            if (this.rows.size() > 3) {
               this.drain(this.rows.poll());
            }

            return false;
         }
      }
   }

   private static final class Ufo {
      Vec3d pos;
      Vec3d goal;
      int beam;
      int cooldown;
   }

   private static final class Zombies implements Tasks.Task {
      final ServerWorld w;
      final List<MobEntity> horde = new ArrayList<>();
      int t;

      Zombies(ServerWorld w, BlockPos pos, int brush) {
         this.w = w;
         Random r = w.getRandom();
         int n = 8 + brush * 3;

         for (int i = 0; i < n; i++) {
            EntityType<? extends MobEntity> type = switch (r.nextInt(3)) {
               case 0 -> EntityType.ZOMBIE;
               case 1 -> EntityType.HUSK;
               default -> EntityType.ZOMBIE_VILLAGER;
            };
            BlockPos p = Powers3.top(w, pos.getX() + r.nextInt(13) - 6, pos.getZ() + r.nextInt(13) - 6);
            if (p != null) {
               MobEntity z = type.spawn(w, p, SpawnReason.TRIGGERED);
               if (z != null) {
                  this.mark(z);
               }
            }
         }

         double rad = 12 + brush * 2;

         for (VillagerEntity v : w.getEntitiesByClass(VillagerEntity.class, new Box(pos).expand(rad), LivingEntity::isAlive)) {
            this.infect(v);
         }

         Fx.sound(w, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 6.0F, 0.6F);
      }

      void mark(MobEntity z) {
         z.addCommandTag("godbox_zombie");
         z.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
         z.setEquipmentDropChance(EquipmentSlot.HEAD, 0.0F);
         z.setPersistent();
         this.horde.add(z);
      }

      boolean infectable(LivingEntity e) {
         return e instanceof MerchantEntity || e instanceof AnimalEntity || e instanceof IllagerEntity || e instanceof WitchEntity;
      }

      void infect(LivingEntity e) {
         if (this.horde.size() < 200 && !e.getCommandTags().contains("godbox_zombie") && !e.getCommandTags().contains("godbox_boss")) {
            MobEntity res = null;
            if (e instanceof VillagerEntity v) {
               res = v.convertTo(EntityType.ZOMBIE_VILLAGER, false);
            } else if (e instanceof PigEntity p) {
               res = p.convertTo(EntityType.ZOMBIFIED_PIGLIN, false);
            } else if (e instanceof MobEntity m && this.infectable(e)) {
               res = m.convertTo(EntityType.ZOMBIE, false);
            }

            if (res != null) {
               this.mark(res);
               Fx.particles(
                  this.w,
                  ParticleTypes.ENTITY_EFFECT == null ? ParticleTypes.SMOKE : ParticleTypes.SMOKE,
                  res.getX(),
                  res.getY() + 1.0,
                  res.getZ(),
                  20,
                  0.4,
                  0.6,
                  0.4,
                  0.02
               );
               Fx.sound(this.w, res.getX(), res.getY(), res.getZ(), SoundEvents.ENTITY_ZOMBIE_INFECT, 1.5F, 1.0F);
            }
         }
      }

      @Override
      public boolean tick() {
         this.t++;
         if (this.t % 20 != 0) {
            return this.t >= 2400;
         } else {
            this.horde.removeIf(zx -> zx.isRemoved() || !zx.isAlive());

            for (MobEntity z : new ArrayList<>(this.horde)) {
               for (LivingEntity e : Fx.living(this.w, z.getPos(), 2.5)) {
                  if (e != z && this.infectable(e)) {
                     this.infect(e);
                  }
               }

               LivingEntity tg = z.getTarget();
               if (tg == null || !tg.isAlive()) {
                  LivingEntity best = null;
                  double bd = 576.0;

                  for (LivingEntity ex : Fx.living(this.w, z.getPos(), 24.0)) {
                     boolean ok = this.infectable(ex) && !ex.getCommandTags().contains("godbox_zombie");
                     if (ex instanceof PlayerEntity p) {
                        ok = !GodManager.isGodEntity(p) && !p.isSpectator() && !p.isCreative();
                     }

                     if (ok) {
                        double d = ex.squaredDistanceTo(z);
                        if (d < bd) {
                           bd = d;
                           best = ex;
                        }
                     }
                  }

                  if (best != null) {
                     z.setTarget(best);
                  }
               }
            }

            return this.t >= 2400;
         }
      }
   }
}
