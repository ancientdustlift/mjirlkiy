package com.vladik.godbox;

import com.vladik.godbox.net.Payloads;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap.Type;

public final class GodManager {
   private static final Map<UUID, GameMode> PREVIOUS = new HashMap<>();
   private static final String TAG = "godbox_prev_";
   private static int timer;

   private GodManager() {
   }

   static void init() {
      ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> restoreLeftover(handler.player));
   }

   public static boolean isGod(ServerPlayerEntity p) {
      return PREVIOUS.containsKey(p.getUuid());
   }

   public static boolean isGodEntity(Entity e) {
      return e instanceof ServerPlayerEntity sp && isGod(sp);
   }

   private static boolean permitted(ServerPlayerEntity p) {
      return p.hasPermissionLevel(2) || p.getServer().isHost(p.getGameProfile());
   }

   private static void clearTags(ServerPlayerEntity p) {
      for (String t : new ArrayList<>(p.getCommandTags())) {
         if (t.startsWith("godbox_prev_")) {
            p.removeCommandTag(t);
         }
      }
   }

   public static void setGod(ServerPlayerEntity p, boolean on) {
      ServerWorld w = p.getServerWorld();
      if (on && !isGod(p)) {
         if (!permitted(p)) {
            p.sendMessage(Text.literal("Нужны права оператора, чтобы стать богом").formatted(Formatting.RED), false);
            return;
         }

         GameMode prev = p.interactionManager.getGameMode();
         PREVIOUS.put(p.getUuid(), prev);
         clearTags(p);
         p.addCommandTag("godbox_prev_" + prev.getName());
         p.changeGameMode(GameMode.SPECTATOR);
         int top = w.getTopY(Type.MOTION_BLOCKING, p.getBlockX(), p.getBlockZ());
         p.networkHandler.requestTeleport(p.getX(), top + 45, p.getZ(), p.getYaw(), 90.0F);
         ServerPlayNetworking.send(p, new Payloads.State(true));
         p.sendMessage(Text.literal("Ты бог. Esc или G — вернуться.").formatted(Formatting.GOLD), true);
      } else if (!on && isGod(p)) {
         GameMode prev = PREVIOUS.remove(p.getUuid());
         clearTags(p);
         p.changeGameMode(prev == null ? GameMode.SURVIVAL : prev);
         int top = w.getTopY(Type.MOTION_BLOCKING, p.getBlockX(), p.getBlockZ());
         p.networkHandler.requestTeleport(p.getX(), top + 1, p.getZ(), p.getYaw(), 0.0F);
         p.fallDistance = 0.0F;
         ServerPlayNetworking.send(p, new Payloads.State(false));
      }
   }

   public static void onDisconnect(ServerPlayerEntity p) {
      if (isGod(p)) {
         GameMode prev = PREVIOUS.remove(p.getUuid());
         clearTags(p);
         p.changeGameMode(prev == null ? GameMode.SURVIVAL : prev);
         int top = p.getServerWorld().getTopY(Type.MOTION_BLOCKING, p.getBlockX(), p.getBlockZ());
         p.refreshPositionAfterTeleport(p.getX(), top + 1, p.getZ());
         p.fallDistance = 0.0F;
      }
   }

   private static void restoreLeftover(ServerPlayerEntity p) {
      if (!isGod(p)) {
         GameMode prev = null;
         boolean found = false;

         for (String t : p.getCommandTags()) {
            if (t.startsWith("godbox_prev_")) {
               found = true;
               GameMode m = GameMode.byName(t.substring("godbox_prev_".length()), null);
               if (m != null && m != GameMode.SPECTATOR) {
                  prev = m;
               }
            }
         }

         if (found) {
            clearTags(p);
            p.changeGameMode(prev == null ? GameMode.SURVIVAL : prev);
            int top = p.getServerWorld().getTopY(Type.MOTION_BLOCKING, p.getBlockX(), p.getBlockZ());
            p.networkHandler.requestTeleport(p.getX(), Math.max(p.getY(), (double)(top + 1)), p.getZ(), p.getYaw(), 0.0F);
            p.fallDistance = 0.0F;
            GodBox.LOG.info("GodBox: {} вошёл с незавершённым режимом бога — режим игры восстановлен", p.getName().getString());
         }
      }
   }

   public static void tick(MinecraftServer s) {
      if (!PREVIOUS.isEmpty() && ++timer >= 20) {
         timer = 0;

         for (UUID id : new ArrayList<>(PREVIOUS.keySet())) {
            ServerPlayerEntity p = s.getPlayerManager().getPlayer(id);
            if (p != null && !permitted(p)) {
               setGod(p, false);
               p.sendMessage(Text.literal("Права оператора сняты — режим бога отключён").formatted(Formatting.RED), false);
            }
         }
      }
   }

   public static void clear() {
      PREVIOUS.clear();
      timer = 0;
   }
}
