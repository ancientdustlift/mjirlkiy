package com.vladik.godbox.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil.Type;

public final class GodKeys {
   public static KeyBinding MAP;
   public static KeyBinding WORLD_MAP;
   public static KeyBinding CODEX;
   public static KeyBinding STATS;
   public static KeyBinding HELP;
   public static KeyBinding ERAS;
   public static KeyBinding CHRONICLE;

   private GodKeys() {
   }

   private static KeyBinding reg(String id, int key) {
      return KeyBindingHelper.registerKeyBinding(new KeyBinding("key.godbox." + id, Type.KEYSYM, key, "category.godbox"));
   }

   public static void init() {
      MAP = reg("minimap", 77);
      WORLD_MAP = reg("worldmap", 78);
      CODEX = reg("codex", 72);
      STATS = reg("stats", 75);
      HELP = reg("help", 73);
      ERAS = reg("eras", 74);
      CHRONICLE = reg("chronicle", 76);
   }

   public static boolean is(KeyBinding k, int key, int scan) {
      return k != null && k.matchesKey(key, scan);
   }
}
