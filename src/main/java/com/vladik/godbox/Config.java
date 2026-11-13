package com.vladik.godbox;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import net.fabricmc.loader.api.FabricLoader;

public final class Config {
   private static final Map<String, String> VALUES = new TreeMap<>();
   private static final Map<String, String> DEFAULTS = new TreeMap<>();
   private static boolean loaded;
   private static boolean dirty;

   private Config() {
   }

   private static Path file() {
      return FabricLoader.getInstance().getConfigDir().resolve("godbox.properties");
   }

   public static synchronized void load() {
      loaded = true;
      VALUES.clear();

      try {
         Path f = file();
         if (!Files.exists(f)) {
            return;
         }

         for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) {
            String t = line.trim();
            if (!t.isEmpty() && !t.startsWith("#")) {
               int eq = t.indexOf(61);
               if (eq > 0) {
                  VALUES.put(t.substring(0, eq).trim(), t.substring(eq + 1).trim());
               }
            }
         }
      } catch (Exception var5) {
         GodBox.LOG.warn("GodBox: не удалось прочитать настройки", (Throwable)var5);
      }
   }

   public static synchronized void flush() {
      if (dirty) {
         dirty = false;

         try {
            List<String> o = new ArrayList<>();
            o.add("# Настройки GodBox. Удалите строку, чтобы вернуть значение по умолчанию.");
            Map<String, String> all = new TreeMap<>(DEFAULTS);
            all.putAll(VALUES);

            for (Entry<String, String> e : all.entrySet()) {
               o.add(e.getKey() + "=" + e.getValue());
            }

            Files.createDirectories(file().getParent());
            Files.write(file(), o, StandardCharsets.UTF_8);
         } catch (Exception var4) {
            GodBox.LOG.warn("GodBox: не удалось записать настройки", (Throwable)var4);
         }
      }
   }

   private static synchronized String raw(String key, String def) {
      if (!loaded) {
         load();
      }

      if (!def.equals(DEFAULTS.put(key, def)) && !VALUES.containsKey(key)) {
         dirty = true;
      }

      return VALUES.getOrDefault(key, def);
   }

   public static int i(String key, int def) {
      try {
         return Integer.parseInt(raw(key, Integer.toString(def)));
      } catch (NumberFormatException var3) {
         return def;
      }
   }

   public static double d(String key, double def) {
      try {
         return Double.parseDouble(raw(key, Double.toString(def)));
      } catch (NumberFormatException var4) {
         return def;
      }
   }

   public static boolean b(String key, boolean def) {
      return Boolean.parseBoolean(raw(key, Boolean.toString(def)));
   }
}
