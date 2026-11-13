package com.vladik.godbox;

import com.vladik.godbox.net.ChroniclePayloads;
import java.lang.StackWalker.StackFrame;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public final class Chronicle {
   static final ArrayDeque<String> LOG = new ArrayDeque<>();
   static final ArrayList<Chronicle.Entry> ENTRIES = new ArrayList<>();
   private static MinecraftServer server;
   static long seq;
   private static long nextId = 1L;
   private static boolean hasNewFormat;
   private static boolean legacyDone;
   static final int MAX_TEXT = 300;
   private static final Map<String, Chronicle.Type> BY_CLASS = new HashMap<>();
   private static final Pattern LEGACY = Pattern.compile("^(?:§.)*День (\\d+): (?:§.)*(.*)$", 32);
   private static final String[] MAJOR = new String[]{
      "объявля",
      "мирный договор",
      "заключили союз",
      "пало",
      "захватило",
      "осаждает столицу",
      "вступает в новую эпоху",
      "наступила",
      "возводит чудо",
      "завладело чудом",
      "достроило замок",
      "основало новую религию",
      "гражданск",
      "восстало",
      "восстает",
      "империей",
      "вассалом",
      "независимость",
      "лишилось короля",
      "мятеж",
      "коронован",
      "династия",
      "унаследовал",
      "вспыхнул",
      "эпидеми",
      "вступает в войну",
      "защищает союзника"
   };
   private static final String[] WAR = new String[]{
      "объявля", "захватило", "осаждает", "флот", "десант", "гражданск", "мятеж", "вступает в войну", "защищает союзника"
   };
   private static final String[] DIPL = new String[]{"мирный договор", "союз", "дань", "брачн", "вассал", "сюзерен"};

   private Chronicle() {
   }

   private static void reg(Chronicle.Type t, String... classes) {
      for (String c : classes) {
         BY_CLASS.put(c, t);
      }
   }

   public static void init() {
      SaveExt.register("chron", Chronicle::save, Chronicle::load, Chronicle::reset);
   }

   public static void tick(MinecraftServer s) {
      server = s;
      if (!legacyDone) {
         importLegacy();
      }
   }

   static long day() {
      return server != null && server.getOverworld() != null ? server.getOverworld().getTimeOfDay() / 24000L + 1L : 1L;
   }

   static int max() {
      return Math.max(200, Config.i("chronicle.max", 4000));
   }

   public static void log(String s) {
      if (s != null && !s.isBlank()) {
         add(detect(Formatting.strip(s)), null, null, null, s, false);
      }
   }

   static void log(Kingdoms.K k, Chronicle.Type type, boolean major, BlockPos pos, String s) {
      if (s != null && !s.isBlank()) {
         add(k, type, major, pos, s, false);
      }
   }

   static void say(MinecraftServer s, Kingdoms.K k, String msg) {
      if (s != null) {
         server = s;
      }

      add(k, null, null, null, msg, true);
   }

   static void say(MinecraftServer s, Kingdoms.K k, Chronicle.Type type, boolean major, BlockPos pos, String msg) {
      if (s != null) {
         server = s;
      }

      add(k, type, major, pos, msg, true);
   }

   private static void add(Kingdoms.K k, Chronicle.Type forcedType, Boolean forcedMajor, BlockPos pos, String raw, boolean chat) {
      String text = Formatting.strip(raw);
      if (text != null && !text.isBlank()) {
         text = text.replace('\n', ' ').replace('\r', ' ').trim();
         if (text.length() > 300) {
            text = text.substring(0, 299) + "…";
         }

         long d = day();
         String line = "§8День " + d + ": §r" + (k != null && chat ? "§" + k.color.getCode() + k.title + "§7 " + raw : raw);
         if (LOG.isEmpty() || !LOG.peekLast().equals(line)) {
            LOG.addLast(line);
            seq++;

            while (LOG.size() > 200) {
               LOG.removeFirst();
            }

            Chronicle.Type type = forcedType;
            boolean major;
            if (forcedType != null && forcedMajor != null) {
               major = forcedMajor;
            } else {
               Chronicle.Cls cls = classify(caller(), text);
               if (forcedType == null) {
                  type = cls.type;
               }

               major = forcedMajor != null ? forcedMajor : cls.major;
            }

            boolean has = false;
            int x = 0;
            int z = 0;
            String dim = "";
            if (pos != null) {
               has = true;
               x = pos.getX();
               z = pos.getZ();
            } else if (k != null && k.capital != null) {
               has = true;
               x = k.capital.getX();
               z = k.capital.getZ();
            }

            if (k != null && k.world != null) {
               dim = k.world.getRegistryKey().getValue().toString();
            } else if (has && server != null && server.getOverworld() != null) {
               dim = server.getOverworld().getRegistryKey().getValue().toString();
            }

            int year = 1;
            int season = -1;
            if (Seasons.on) {
               year = Seasons.year();
               season = Seasons.index();
            } else if (server != null && server.getOverworld() != null) {
               year = (int)(server.getOverworld().getTimeOfDay() / 600L + 1L);
            }

            append(new Chronicle.Entry(nextId++, (int)d, year, season, k, type, major, text, has, x, z, dim));
            if (chat && k != null && server != null) {
               int mode = Config.i("chronicle.chat", 1);
               if (mode > 0 && (mode != 1 || major)) {
                  Text t = Text.literal("[Летопись] ")
                     .formatted(Formatting.GOLD)
                     .append(Text.literal(k.title).formatted(k.color))
                     .append(Text.literal(" " + raw).formatted(Formatting.GRAY));

                  for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                     p.sendMessage(t, false);
                  }
               }
            }
         }
      }
   }

   private static void append(Chronicle.Entry e) {
      ENTRIES.add(e);
      int cap = max();
      if (ENTRIES.size() > cap + cap / 10) {
         int drop = ENTRIES.size() - cap;
         List<Chronicle.Entry> keep = new ArrayList<>(cap);

         for (Chronicle.Entry x : ENTRIES) {
            if (drop > 0 && !x.major) {
               drop--;
            } else {
               keep.add(x);
            }
         }

         while (drop > 0 && !keep.isEmpty()) {
            keep.remove(0);
            drop--;
         }

         ENTRIES.clear();
         ENTRIES.addAll(keep);
      }
   }

   private static String caller() {
      return StackWalker.getInstance().walk(st -> st.map(StackFrame::getClassName).filter(n -> !n.equals(Chronicle.class.getName())).findFirst().orElse(""));
   }

   private static Chronicle.Cls classify(String callerName, String text) {
      String c = callerName;
      int dot = callerName.lastIndexOf(46);
      if (dot >= 0) {
         c = callerName.substring(dot + 1);
      }

      int dollar = c.indexOf(36);
      if (dollar >= 0) {
         c = c.substring(0, dollar);
      }

      String low = norm(text);
      Chronicle.Type t = BY_CLASS.get(c);
      if (t == null || t == Chronicle.Type.OTHER) {
         t = null;
      }

      for (String w : WAR) {
         if (low.contains(w)) {
            t = Chronicle.Type.WAR;
            break;
         }
      }

      if (t != Chronicle.Type.WAR) {
         for (String wx : DIPL) {
            if (low.contains(wx)) {
               t = Chronicle.Type.DIPLOMACY;
               break;
            }
         }
      }

      if (low.contains("чудо света") || low.contains("чудом света")) {
         t = Chronicle.Type.BUILD;
      }

      if (t == null) {
         t = Chronicle.Type.OTHER;
      }

      boolean major = c.equals("Dynasty") || c.equals("Kings") && !low.contains("наследник");
      if (!major) {
         for (String wxx : MAJOR) {
            if (low.contains(wxx)) {
               major = true;
               break;
            }
         }
      }

      if (c.equals("Plague")) {
         major = low.contains("приш") || low.contains("вспыхнул");
      }

      return new Chronicle.Cls(t, major);
   }

   private static Kingdoms.K detect(String text) {
      Kingdoms.K best = null;
      int at = Integer.MAX_VALUE;

      for (Kingdoms.K k : Kingdoms.K.values()) {
         int i = text.indexOf(k.title);
         if (i >= 0 && i < at) {
            at = i;
            best = k;
         }
      }

      return best;
   }

   private static void importLegacy() {
      legacyDone = true;
      if (!hasNewFormat && ENTRIES.isEmpty() && !LOG.isEmpty()) {
         for (String line : LOG) {
            Matcher m = LEGACY.matcher(line);
            int day = 1;
            String text = line;
            if (m.matches()) {
               try {
                  day = Integer.parseInt(m.group(1));
               } catch (NumberFormatException var7) {
               }

               text = m.group(2);
            }

            text = Formatting.strip(text);
            if (text != null && !text.isBlank()) {
               if (text.length() > 300) {
                  text = text.substring(0, 299) + "…";
               }

               Chronicle.Cls c = classify("", text);
               Kingdoms.K k = detect(text);
               ENTRIES.add(new Chronicle.Entry(nextId++, day, 0, -1, k, c.type, c.major, text, false, 0, 0, ""));
            }
         }
      }
   }

   private static String esc(String s) {
      StringBuilder b = new StringBuilder(s.length() + 8);

      for (int i = 0; i < s.length(); i++) {
         char c = s.charAt(i);
         switch (c) {
            case '\n':
               b.append("%0A");
               break;
            case '\r':
               b.append("%0D");
               break;
            case '%':
               b.append("%25");
               break;
            case '|':
               b.append("%7C");
               break;
            default:
               b.append(c);
         }
      }

      return b.toString();
   }

   private static String unesc(String s) {
      if (s.indexOf(37) < 0) {
         return s;
      } else {
         StringBuilder b = new StringBuilder(s.length());

         for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < s.length()) {
               try {
                  b.append((char)Integer.parseInt(s.substring(i + 1, i + 3), 16));
                  i += 2;
                  continue;
               } catch (NumberFormatException var5) {
               }
            }

            b.append(c);
         }

         return b.toString();
      }
   }

   private static List<String> save() {
      List<String> o = new ArrayList<>(ENTRIES.size() + 1);
      o.add("v|" + nextId);

      for (Chronicle.Entry e : ENTRIES) {
         o.add(
            "e|"
               + e.id
               + "|"
               + e.day
               + "|"
               + e.year
               + "|"
               + e.season
               + "|"
               + (e.k == null ? "" : e.k.id)
               + "|"
               + e.type.ordinal()
               + "|"
               + (e.major ? 1 : 0)
               + "|"
               + (e.hasPos ? e.x : "")
               + "|"
               + (e.hasPos ? e.z : "")
               + "|"
               + esc(e.dim)
               + "|"
               + esc(e.text)
         );
      }

      return o;
   }

   private static void load(String[] a) {
      try {
         if (a.length >= 2 && a[0].equals("v")) {
            hasNewFormat = true;
            nextId = Math.max(nextId, Long.parseLong(a[1]));
         } else if (a.length >= 12 && a[0].equals("e")) {
            boolean has = !a[8].isEmpty() && !a[9].isEmpty();
            String text = unesc(a[11]);
            if (text.isBlank()) {
               return;
            }

            long id = Long.parseLong(a[1]);
            ENTRIES.add(
               new Chronicle.Entry(
                  id,
                  Integer.parseInt(a[2]),
                  Integer.parseInt(a[3]),
                  Integer.parseInt(a[4]),
                  Kingdoms.byId(a[5]),
                  Chronicle.Type.of(Integer.parseInt(a[6])),
                  a[7].equals("1"),
                  text,
                  has,
                  has ? Integer.parseInt(a[8]) : 0,
                  has ? Integer.parseInt(a[9]) : 0,
                  unesc(a[10])
               )
            );
            nextId = Math.max(nextId, id + 1L);
            hasNewFormat = true;
         }
      } catch (RuntimeException var5) {
         GodBox.LOG.warn("GodBox: запись летописи повреждена и пропущена");
      }
   }

   private static void reset() {
      ENTRIES.clear();
      nextId = 1L;
      hasNewFormat = false;
      legacyDone = false;
   }

   static List<String> last(int n) {
      List<String> all = new ArrayList<>(LOG);
      return all.subList(Math.max(0, all.size() - n), all.size());
   }

   static Chronicle.Entry byId(long id) {
      for (int i = ENTRIES.size() - 1; i >= 0; i--) {
         if (ENTRIES.get(i).id == id) {
            return ENTRIES.get(i);
         }
      }

      return null;
   }

   static String norm(String s) {
      return s.toLowerCase(Locale.ROOT).replace('ё', 'е');
   }

   static void show(ServerPlayerEntity god) {
      if (ServerPlayNetworking.canSend(god, ChroniclePayloads.Open.ID)) {
         ServerPlayNetworking.send(god, new ChroniclePayloads.Open());
      } else {
         god.sendMessage(Text.literal("=== Летопись мира ===").formatted(Formatting.GOLD), false);
         List<String> l = last(15);
         if (l.isEmpty()) {
            god.sendMessage(Text.literal("Пока ничего не произошло.").formatted(Formatting.GRAY), false);
         }

         for (String s : l) {
            god.sendMessage(Text.literal(s), false);
         }
      }
   }

   public static void clear() {
      LOG.clear();
      ENTRIES.clear();
      nextId = 1L;
      hasNewFormat = false;
      legacyDone = false;
   }

   static {
      reg(Chronicle.Type.WAR, "Conquest", "Siege", "SiegeEngines", "Navy", "Rebels", "Unrest");
      reg(Chronicle.Type.DIPLOMACY, "Diplomacy", "Treaties", "Vassals");
      reg(Chronicle.Type.DYNASTY, "Dynasty", "Kings", "Heroes");
      reg(Chronicle.Type.DISASTER, "Events", "Plague", "Tectonics", "Creatures");
      reg(Chronicle.Type.BUILD, "Colonies", "Highways", "Roads", "Ships", "Herds", "Repair", "Wonders", "Banners");
      reg(Chronicle.Type.FAITH, "Faith", "Prayers", "Magic", "Races");
      reg(Chronicle.Type.SCIENCE, "Tech", "Eras", "WorldAges", "Seasons", "Ages");
      reg(Chronicle.Type.ECONOMY, "Economy");
   }

   private record Cls(Chronicle.Type type, boolean major) {
   }

   public static final class Entry {
      public final long id;
      public final int day;
      public final int year;
      public final int season;
      public final Kingdoms.K k;
      public final Chronicle.Type type;
      public final boolean major;
      public final String text;
      public final boolean hasPos;
      public final int x;
      public final int z;
      public final String dim;

      Entry(long id, int day, int year, int season, Kingdoms.K k, Chronicle.Type type, boolean major, String text, boolean hasPos, int x, int z, String dim) {
         this.id = id;
         this.day = day;
         this.year = year;
         this.season = season;
         this.k = k;
         this.type = type;
         this.major = major;
         this.text = text;
         this.hasPos = hasPos;
         this.x = x;
         this.z = z;
         this.dim = dim == null ? "" : dim;
      }
   }

   public static enum Type {
      WAR("Война", 14700624),
      DIPLOMACY("Дипломатия", 6340848),
      DYNASTY("Династия", 14729296),
      DISASTER("Бедствие", 14712880),
      BUILD("Строительство", 11575424),
      FAITH("Вера", 12615904),
      SCIENCE("Наука и эпохи", 7397536),
      ECONOMY("Экономика", 13684832),
      OTHER("Прочее", 10526880);

      public final String label;
      public final int rgb;

      private Type(String label, int rgb) {
         this.label = label;
         this.rgb = rgb;
      }

      public static Chronicle.Type of(int i) {
         Chronicle.Type[] v = values();
         return i >= 0 && i < v.length ? v[i] : OTHER;
      }
   }
}
