package com.vladik.godbox;

import com.vladik.godbox.net.MortalPayloads;
import com.vladik.godbox.net.Payloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.network.ServerPlayerEntity;

public final class Hud {
   private static int t;

   private Hud() {
   }

   public static void tick(MinecraftServer s) {
      if (++t % 10 == 0) {
         Payloads.Hud h = null;

         for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
            if (GodManager.isGod(p) && ServerPlayNetworking.canSend(p, Payloads.Hud.ID)) {
               if (h == null) {
                  ServerTickManager tm = s.getTickManager();
                  int left = Math.max(0, (24000 - WorldAges.timer) / 20);
                  h = new Payloads.Hud(tm.getTickRate(), tm.isFrozen(), WorldAges.age, left, WorldAges.on, Powers3.lawMask(s));
               }

               ServerPlayNetworking.send(p, h);
            }
         }

         MortalPayloads.MortalHud m = Mortal.hud();
         if (m != null) {
            for (ServerPlayerEntity px : s.getPlayerManager().getPlayerList()) {
               if (GodManager.isGod(px) && ServerPlayNetworking.canSend(px, MortalPayloads.MortalHud.ID)) {
                  ServerPlayNetworking.send(px, m);
               }
            }
         }
      }
   }
}
