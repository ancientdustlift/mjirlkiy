package com.vladik.godbox.client;

import com.vladik.godbox.Power;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

public final class Recent {
   public static final int MAX = 12;
   private static final List<Power> LIST = new ArrayList<>();

   private Recent() {
   }

   private static Path file() {
      return FabricLoader.getInstance().getConfigDir().resolve("godbox_recent.txt");
   }

   public static void load() {
      LIST.clear();

      try {
         Path f = file();
         if (!Files.exists(f)) {
            return;
         }

         for (String s : Files.readAllLines(f, StandardCharsets.UTF_8)) {
            Power p = Power.byName(s.trim());
            if (p != null && p.category != null && !LIST.contains(p) && LIST.size() < 12) {
               LIST.add(p);
            }
         }
      } catch (Exception var4) {
      }
   }

   private static void save() {
      try {
         List<String> out = new ArrayList<>();

         for (Power p : LIST) {
            out.add(p.name());
         }

         Files.write(file(), out, StandardCharsets.UTF_8);
      } catch (Exception var3) {
      }
   }

   public static List<Power> list() {
      return LIST;
   }

   public static void push(Power p) {
      if (p != null && p.category != null) {
         if (LIST.isEmpty() || LIST.get(0) != p) {
            LIST.remove(p);
            LIST.add(0, p);

            while (LIST.size() > 12) {
               LIST.remove(LIST.size() - 1);
            }

            save();
         }
      }
   }
}
