package com.vladik.godbox;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;

public final class Save {
   private static int timer;

   private Save() {
   }

   private static Path file(MinecraftServer s) {
      return s.getSavePath(WorldSavePath.ROOT).resolve("godbox_state.txt");
   }

   private static String clean(String s) {
      return s.replace('|', '/').replace('\n', ' ').replace('\r', ' ');
   }

   private static int i(String s) {
      return Integer.parseInt(s.trim());
   }

   private static boolean b(String s) {
      return Boolean.parseBoolean(s.trim());
   }

   public static void tick(MinecraftServer s) {
      if (++timer >= 6000) {
         timer = 0;
         save(s);
      }
   }

   public static void save(MinecraftServer s) {
      try {
         List<String> o = new ArrayList<>();
         o.add("godbox|2");
         SaveExt.saveAll(o);

         for (Entry<Kingdoms.K, BlockPos> pe : Ships.PORTS.entrySet()) {
            o.add("P|" + pe.getKey().name() + "|" + pe.getValue().getX() + "|" + pe.getValue().getY() + "|" + pe.getValue().getZ());
         }

         o.add("S|" + Colonies.on + "|" + Ships.on);

         for (Entry<String, Kingdoms.K> we : Wonders.OWNER.entrySet()) {
            BlockPos wp = Wonders.AT.get(we.getKey());
            if (wp != null && we.getValue() != null) {
               o.add("W|" + we.getKey() + "|" + we.getValue().name() + "|" + wp.getX() + "|" + wp.getY() + "|" + wp.getZ());
            }
         }

         for (Entry<Kingdoms.K, Integer> te : Tech.MASK.entrySet()) {
            o.add("X|" + te.getKey().name() + "|" + te.getValue() + "|" + Tech.PTS.getOrDefault(te.getKey(), 0));
         }

         for (String rd : Roads.DONE) {
            o.add("Y|" + rd);
         }

         for (Entry<Kingdoms.K, Integer> se : Siege.WALL.entrySet()) {
            o.add("G|" + se.getKey().name() + "|" + se.getValue());
         }

         o.add("Z|" + Tech.on + "|" + Wonders.on + "|" + Magic.on + "|" + Navy.on + "|" + Roads.on + "|" + Siege.on);

         for (Kingdoms.K k22 : Kingdoms.K.values()) {
            if (Economy.GOLD.containsKey(k22)) {
               o.add("E|" + k22.name() + "|" + Economy.gold(k22) + "|" + Economy.TAX.getOrDefault(k22, 1));
            }

            BlockPos mp22 = Economy.MARKET.get(k22);
            if (mp22 != null) {
               o.add("M|" + k22.name() + "|" + mp22.getX() + "|" + mp22.getY() + "|" + mp22.getZ());
            }

            if (Unrest.UNREST.containsKey(k22)) {
               o.add("U|" + k22.name() + "|" + Unrest.unrest(k22));
            }

            if (Highways.BUILT.containsKey(k22)) {
               o.add("N|" + k22.name() + "|" + Highways.count(k22));
            }
         }

         for (Entry<Kingdoms.K, Kingdoms.K> e22 : Vassals.LORD.entrySet()) {
            o.add("V|" + e22.getKey().name() + "|" + e22.getValue().name());
         }

         for (String h22 : Highways.DONE) {
            o.add("H|" + h22);
         }

         for (String j22 : Heroes.HALL) {
            o.add("J|" + j22.replace('|', '/'));
         }

         o.add("Q|" + Economy.on + "|" + Vassals.on + "|" + Unrest.on + "|" + Heroes.on + "|" + Highways.on);

         for (Kingdoms.K k23 : Kingdoms.K.values()) {
            if (Prayers.FAITH.containsKey(k23)) {
               o.add("%|" + k23.name() + "|" + Prayers.faith(k23));
            }

            BlockPos pn23 = Herds.PEN.get(k23);
            if (pn23 != null) {
               o.add("&|" + k23.name() + "|" + pn23.getX() + "|" + pn23.getY() + "|" + pn23.getZ());
            }
         }

         for (Achievements.A a23 : Achievements.GOT) {
            o.add("O|" + a23.name());
         }

         for (Tectonics.V v23 : Tectonics.VOLCANOES) {
            o.add("^|" + v23.top.getX() + "|" + v23.top.getY() + "|" + v23.top.getZ() + "|" + v23.h);
         }

         o.add("!|" + Prayers.on + "|" + Ecosystem.on + "|" + Herds.on + "|" + Tectonics.on + "|" + Perf.on);
         o.add("~|" + Storage.on + "|" + Decor.on);

         for (Kingdoms.K kk : Kingdoms.K.values()) {
            o.add("#|" + kk.name() + "|" + Banners.SALT[kk.ordinal()]);
         }

         o.addAll(Dynasty.save());
         o.add("A|" + WorldAges.age + "|" + WorldAges.timer + "|" + WorldAges.on);
         o.add("F|" + Kingdoms.war + "|" + Kingdoms.eternalPeace + "|" + Conquest.on + "|" + Events.on + "|" + Diplomacy.auto + "|" + Sleep.on);

         for (Kingdoms.K k : Kingdoms.K.values()) {
            if (k.alive && k.world != null && k.capital != null) {
               o.add("K|" + k.name() + "|" + k.world.getRegistryKey().getValue() + "|" + k.capital.getX() + "|" + k.capital.getY() + "|" + k.capital.getZ());
               Civ.City c = Civ.city(k);
               o.add(
                  "C|"
                     + k.name()
                     + "|"
                     + c.wood
                     + "|"
                     + c.stone
                     + "|"
                     + c.food
                     + "|"
                     + c.ore
                     + "|"
                     + c.era
                     + "|"
                     + c.faith
                     + "|"
                     + c.culture
                     + "|"
                     + c.religion
                     + "|"
                     + c.castle
                     + "|"
                     + c.water
                     + "|"
                     + c.cycle
               );

               for (Civ.Building bd : c.buildings) {
                  if (bd.done) {
                     o.add("B|" + k.name() + "|" + bd.kind.name() + "|" + bd.x0 + "|" + bd.z0 + "|" + bd.y0 + "|" + (bd == c.quarry ? 1 : 0));
                  }
               }

               List<BlockPos> towns = Conquest.TOWNS.get(k);
               if (towns != null) {
                  for (BlockPos t : towns) {
                     o.add("T|" + k.name() + "|" + t.getX() + "|" + t.getY() + "|" + t.getZ());
                  }
               }

               for (Kingdoms.K j : Kingdoms.K.values()) {
                  if (j != k) {
                     o.add("D|" + k.name() + "|" + j.name() + "|" + Diplomacy.REL[k.ordinal()][j.ordinal()] + "|" + Diplomacy.OP[k.ordinal()][j.ordinal()]);
                  }
               }
            }
         }

         for (String r : Faith.RELIGIONS) {
            o.add("R|" + clean(r));
         }

         for (String l : Chronicle.LOG) {
            o.add("L|" + clean(l));
         }

         write(file(s), o);
      } catch (Exception var12) {
         GodBox.LOG.error("GodBox: сохранение не удалось", (Throwable)var12);
      }
   }

   private static Path backup(Path f) {
      return f.resolveSibling("godbox_state.bak");
   }

   private static void write(Path f, List<String> lines) throws IOException {
      Path tmp = f.resolveSibling("godbox_state.tmp");

      try (FileChannel ch = FileChannel.open(tmp, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
         ch.write(ByteBuffer.wrap((String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8)));
         ch.force(true);
      }

      if (validHeader(f) != null) {
         try {
            Files.copy(f, backup(f), StandardCopyOption.REPLACE_EXISTING);
         } catch (Exception var8) {
            GodBox.LOG.warn("GodBox: не удалось обновить резервную копию", (Throwable)var8);
         }
      }

      try {
         Files.move(tmp, f, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException var7) {
         Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING);
      }
   }

   private static List<String> validHeader(Path f) {
      try {
         if (Files.isRegularFile(f) && Files.size(f) != 0L) {
            List<String> l = Files.readAllLines(f, StandardCharsets.UTF_8);
            return !l.isEmpty() && l.get(0).startsWith("godbox|") ? l : null;
         } else {
            return null;
         }
      } catch (Exception var2) {
         return null;
      }
   }

   public static void load(MinecraftServer s) {
      timer = 0;
      SaveExt.loadedFormat = 0;
      SaveExt.resetAll();

      try {
         Path f = file(s);
         List<String> in = validHeader(f);
         if (in == null) {
            Path bak = backup(f);
            if (Files.exists(f) && Files.size(f) > 0L) {
               GodBox.LOG.warn("GodBox: godbox_state.txt повреждён (нет заголовка)");
            }

            in = validHeader(bak);
            if (in == null) {
               if (Files.exists(f) || Files.exists(bak)) {
                  GodBox.LOG.warn("GodBox: нет пригодного файла сохранения, мир начинается с нуля");
               }

               return;
            }

            GodBox.LOG.warn("GodBox: загружена резервная копия godbox_state.bak");
         }

         int lineNo = 0;
         int badCount = 0;
         StringBuilder badAt = new StringBuilder();
         Faith.RELIGIONS.clear();
         Chronicle.LOG.clear();
         Prayers.FAITH.clear();
         Herds.PEN.clear();
         Achievements.GOT.clear();
         Tectonics.VOLCANOES.clear();
         Arrays.fill(Banners.SALT, 0);
         Dynasty.HOUSES.clear();
         int kingdoms = 0;

         for (String line : in) {
            lineNo++;
            String[] a = line.split("\\|", -1);

            try {
               if (a[0].equals("godbox")) {
                  SaveExt.loadedFormat = a.length > 1 ? i(a[1]) : 1;
                  if (SaveExt.loadedFormat > 2) {
                     GodBox.LOG
                        .warn("GodBox: сохранение создано более новой версией мода (формат {}), незнакомые данные будут пропущены", SaveExt.loadedFormat);
                  }
               } else if (a[0].startsWith("@")) {
                  SaveExt.load(a);
               } else {
                  String bad = a[0];
                  switch (bad) {
                     case "P":
                        Ships.PORTS.put(Kingdoms.K.valueOf(a[1]), new BlockPos(Integer.parseInt(a[2]), Integer.parseInt(a[3]), Integer.parseInt(a[4])));
                        break;
                     case "S":
                        Colonies.on = Boolean.parseBoolean(a[1]);
                        Ships.on = Boolean.parseBoolean(a[2]);
                        break;
                     case "W":
                        Wonders.OWNER.put(a[1], Kingdoms.K.valueOf(a[2]));
                        Wonders.AT.put(a[1], new BlockPos(Integer.parseInt(a[3]), Integer.parseInt(a[4]), Integer.parseInt(a[5])));
                        break;
                     case "X":
                        Tech.MASK.put(Kingdoms.K.valueOf(a[1]), Integer.parseInt(a[2]));
                        Tech.PTS.put(Kingdoms.K.valueOf(a[1]), Integer.parseInt(a[3]));
                        break;
                     case "Y":
                        Roads.DONE.add(a[1]);
                        break;
                     case "E":
                        Kingdoms.K e22 = Kingdoms.K.valueOf(a[1]);
                        Economy.GOLD.put(e22, Integer.parseInt(a[2]));
                        Economy.TAX.put(e22, Integer.parseInt(a[3]));
                        break;
                     case "M":
                        Economy.MARKET.put(Kingdoms.K.valueOf(a[1]), new BlockPos(Integer.parseInt(a[2]), Integer.parseInt(a[3]), Integer.parseInt(a[4])));
                        break;
                     case "U":
                        Unrest.UNREST.put(Kingdoms.K.valueOf(a[1]), Integer.parseInt(a[2]));
                        break;
                     case "N":
                        Highways.BUILT.put(Kingdoms.K.valueOf(a[1]), Integer.parseInt(a[2]));
                        break;
                     case "V":
                        Vassals.LORD.put(Kingdoms.K.valueOf(a[1]), Kingdoms.K.valueOf(a[2]));
                        break;
                     case "H":
                        Highways.DONE.add(a[1]);
                        break;
                     case "J":
                        Heroes.HALL.add(a[1]);
                        break;
                     case "~":
                        Storage.on = Boolean.parseBoolean(a[1]);
                        Decor.on = Boolean.parseBoolean(a[2]);
                        break;
                     case "$":
                        Dynasty.load(a);
                        break;
                     case "#":
                        try {
                           Banners.SALT[Kingdoms.K.valueOf(a[1]).ordinal()] = Integer.parseInt(a[2]);
                        } catch (Exception var15) {
                        }
                        break;
                     case "%":
                        Prayers.FAITH.put(Kingdoms.K.valueOf(a[1]), Integer.parseInt(a[2]));
                        break;
                     case "&":
                        Herds.PEN.put(Kingdoms.K.valueOf(a[1]), new BlockPos(Integer.parseInt(a[2]), Integer.parseInt(a[3]), Integer.parseInt(a[4])));
                        break;
                     case "O":
                        Achievements.GOT.add(Achievements.A.valueOf(a[1]));
                        break;
                     case "^":
                        Tectonics.VOLCANOES
                           .add(new Tectonics.V(new BlockPos(Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3])), Integer.parseInt(a[4])));
                        break;
                     case "!":
                        Prayers.on = Boolean.parseBoolean(a[1]);
                        Ecosystem.on = Boolean.parseBoolean(a[2]);
                        Herds.on = Boolean.parseBoolean(a[3]);
                        Tectonics.on = Boolean.parseBoolean(a[4]);
                        Perf.on = Boolean.parseBoolean(a[5]);
                        break;
                     case "Q":
                        Economy.on = Boolean.parseBoolean(a[1]);
                        Vassals.on = Boolean.parseBoolean(a[2]);
                        Unrest.on = Boolean.parseBoolean(a[3]);
                        Heroes.on = Boolean.parseBoolean(a[4]);
                        Highways.on = Boolean.parseBoolean(a[5]);
                        break;
                     case "G":
                        Siege.WALL.put(Kingdoms.K.valueOf(a[1]), Integer.parseInt(a[2]));
                        break;
                     case "Z":
                        Tech.on = Boolean.parseBoolean(a[1]);
                        Wonders.on = Boolean.parseBoolean(a[2]);
                        Magic.on = Boolean.parseBoolean(a[3]);
                        Navy.on = Boolean.parseBoolean(a[4]);
                        Roads.on = Boolean.parseBoolean(a[5]);
                        Siege.on = Boolean.parseBoolean(a[6]);
                        break;
                     case "A":
                        WorldAges.age = Math.max(0, Math.min(WorldAges.NAMES.length - 1, Integer.parseInt(a[1])));
                        WorldAges.timer = Integer.parseInt(a[2]);
                        WorldAges.on = Boolean.parseBoolean(a[3]);
                        break;
                     case "F":
                        Kingdoms.war = b(a[1]);
                        Kingdoms.eternalPeace = b(a[2]);
                        Conquest.on = b(a[3]);
                        Events.on = b(a[4]);
                        Diplomacy.auto = b(a[5]);
                        Sleep.on = b(a[6]);
                        break;
                     case "K": {
                        Kingdoms.K k = Kingdoms.K.valueOf(a[1]);
                        ServerWorld w = s.getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(a[2])));
                        if (w != null) {
                           k.world = w;
                           k.capital = new BlockPos(i(a[3]), i(a[4]), i(a[5]));
                           k.alive = true;
                           k.grow = 0;
                           k.fallTimer = -60;
                           k.members.clear();
                           Civ.city(k);
                           kingdoms++;
                        }
                        break;
                     }
                     case "C": {
                        Kingdoms.K k = Kingdoms.K.valueOf(a[1]);
                        if (k.alive) {
                           Civ.City c = Civ.city(k);
                           c.wood = i(a[2]);
                           c.stone = i(a[3]);
                           c.food = i(a[4]);
                           c.ore = i(a[5]);
                           c.era = Math.max(0, Math.min(Eras.NAMES.length - 1, i(a[6])));
                           c.faith = i(a[7]);
                           c.culture = i(a[8]);
                           c.religion = i(a[9]);
                           c.castle = b(a[10]);
                           c.water = b(a[11]);
                           c.cycle = i(a[12]);
                           c.buildings.clear();
                           c.quarry = null;
                           c.project = null;
                        }
                        break;
                     }
                     case "B": {
                        Kingdoms.K k = Kingdoms.K.valueOf(a[1]);
                        if (k.alive) {
                           Civ.City c = Civ.city(k);
                           Civ.Building bd = new Civ.Building(Civ.Kind.valueOf(a[2]), i(a[3]), i(a[4]), i(a[5]));
                           bd.done = true;
                           c.buildings.add(bd);
                           if ("1".equals(a[6])) {
                              c.quarry = bd;
                           }
                        }
                        break;
                     }
                     case "T": {
                        Kingdoms.K k = Kingdoms.K.valueOf(a[1]);
                        if (k.alive) {
                           Conquest.TOWNS.computeIfAbsent(k, x -> new ArrayList<>()).add(new BlockPos(i(a[2]), i(a[3]), i(a[4])));
                        }
                        break;
                     }
                     case "D": {
                        Kingdoms.K k = Kingdoms.K.valueOf(a[1]);
                        Kingdoms.K j = Kingdoms.K.valueOf(a[2]);
                        Diplomacy.REL[k.ordinal()][j.ordinal()] = i(a[3]);
                        Diplomacy.OP[k.ordinal()][j.ordinal()] = i(a[4]);
                        break;
                     }
                     case "R":
                        Faith.RELIGIONS.add(a[1]);
                        break;
                     case "L":
                        Chronicle.LOG.add(a[1]);
                  }
               }
            } catch (Exception var16) {
               if (++badCount <= 5) {
                  badAt.append(badAt.length() > 0 ? ", " : "").append(lineNo);
               }
            }
         }

         if (badCount > 0) {
            GodBox.LOG.warn("GodBox: не разобрано строк сохранения: {} (первые: {})", badCount, badAt);
         }

         if (kingdoms > 0) {
            GodBox.LOG.info("GodBox: восстановлено королевств: {}", kingdoms);
         }
      } catch (Exception var17) {
         GodBox.LOG.error("GodBox: загрузка не удалась", (Throwable)var17);
      }
   }
}
