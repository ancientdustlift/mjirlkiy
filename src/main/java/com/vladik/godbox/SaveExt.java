package com.vladik.godbox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SaveExt {
   public static final int FORMAT = 2;
   private static final Map<String, SaveExt.Ext> EXTS = new LinkedHashMap<>();
   public static int loadedFormat;

   private SaveExt() {
   }

   public static void register(String tag, Supplier<List<String>> save, Consumer<String[]> load, Runnable reset) {
      EXTS.put(tag, new SaveExt.Ext(save, load, reset));
   }

   static void saveAll(List<String> out) {
      for (Entry<String, SaveExt.Ext> e : EXTS.entrySet()) {
         try {
            for (String line : e.getValue().save().get()) {
               out.add("@" + e.getKey() + "|" + line);
            }
         } catch (Exception var5) {
            GodBox.LOG.error("GodBox: сохранение '{}' не удалось", e.getKey(), var5);
         }
      }
   }

   static void resetAll() {
      for (Entry<String, SaveExt.Ext> e : EXTS.entrySet()) {
         try {
            e.getValue().reset().run();
         } catch (Exception var3) {
            GodBox.LOG.error("GodBox: сброс '{}' не удался", e.getKey(), var3);
         }
      }
   }

   static void load(String[] a) {
      SaveExt.Ext x = EXTS.get(a[0].substring(1));
      if (x != null) {
         String[] rest = new String[a.length - 1];
         System.arraycopy(a, 1, rest, 0, rest.length);
         x.load().accept(rest);
      }
   }

   private record Ext(Supplier<List<String>> save, Consumer<String[]> load, Runnable reset) {
   }
}
