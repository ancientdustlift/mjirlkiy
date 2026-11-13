package com.vladik.godbox;

import com.vladik.godbox.net.HistoryPayloads;
import com.vladik.godbox.net.MortalPayloads;
import com.vladik.godbox.net.Payloads;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GodBox implements ModInitializer {
   public static final String MOD_ID = "godbox";
   public static final String VERSION = "3.2.0";
   public static final Logger LOG = LoggerFactory.getLogger("godbox");

   public void onInitialize() {
      registerNetwork();
      Mortal.init();
      Systems.registerDefaults();
      Systems.freeze();
      ServerTickEvents.END_SERVER_TICK.register(Systems::tickAll);
      ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> GodManager.onDisconnect(handler.player));
      Config.load();
      Models.init();
      Guns.init();
      GodParticles.init();
      ServerLifecycleEvents.SERVER_STARTED.register(Save::load);
      ServerLifecycleEvents.SERVER_STARTED.register(server -> Config.flush());
      ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
         Config.flush();
         Save.save(server);
         Systems.clearAll();
      });
      Heroes.init();
      Ecosystem.init();
      Races.init();
      Eras.init();
      Highways.init();
      Seasons.init();
      Events.init();
      History.init();
      Chronicle.init();
      Inspect.init();
      Plague.init();
      LOG.info("GodBox {} loaded: {} systems, {} powers. Press G in game to become a god", "3.2.0", Systems.all().size(), Power.values().length);
   }

   private static void registerNetwork() {
      PayloadTypeRegistry.playC2S().register(Payloads.Toggle.ID, Payloads.Toggle.CODEC);
      PayloadTypeRegistry.playC2S().register(Payloads.Use.ID, Payloads.Use.CODEC);
      PayloadTypeRegistry.playS2C().register(Payloads.State.ID, Payloads.State.CODEC);
      PayloadTypeRegistry.playS2C().register(Payloads.Stats.ID, Payloads.Stats.CODEC);
      PayloadTypeRegistry.playS2C().register(Payloads.Borders.ID, Payloads.Borders.CODEC);
      PayloadTypeRegistry.playS2C().register(Payloads.Titles.ID, Payloads.Titles.CODEC);
      PayloadTypeRegistry.playS2C().register(Payloads.Hud.ID, Payloads.Hud.CODEC);
      ServerPlayNetworking.registerGlobalReceiver(Payloads.Toggle.ID, (payload, ctx) -> {
         ServerPlayerEntity player = ctx.player();
         player.getServer().execute(() -> GodManager.setGod(player, payload.on()));
      });
      ServerPlayNetworking.registerGlobalReceiver(Payloads.Use.ID, (payload, ctx) -> {
         ServerPlayerEntity player = ctx.player();
         player.getServer().execute(() -> {
            Power power = Power.byName(payload.power());
            if (power == null) {
               LOG.warn("GodBox: unknown power '{}' from {}", payload.power(), player.getName().getString());
            } else {
               try {
                  if (!Mortal.pay(player, power, payload.first())) {
                     return;
                  }

                  Powers.use(player, power, payload.pos(), payload.brush(), payload.first());
                  if (payload.first()) {
                     Achievements.used(power);
                  }
               } catch (Exception var4) {
                  LOG.error("GodBox power {} failed", power, var4);
               }
            }
         });
      });
      PayloadTypeRegistry.playC2S().register(HistoryPayloads.HistoryRequest.ID, HistoryPayloads.HistoryRequest.CODEC);
      PayloadTypeRegistry.playS2C().register(HistoryPayloads.HistoryData.ID, HistoryPayloads.HistoryData.CODEC);
      PayloadTypeRegistry.playS2C().register(HistoryPayloads.HistoryOpen.ID, HistoryPayloads.HistoryOpen.CODEC);
      ServerPlayNetworking.registerGlobalReceiver(HistoryPayloads.HistoryRequest.ID, (payload, ctx) -> {
         ServerPlayerEntity player = ctx.player();
         player.getServer().execute(() -> History.request(player));
      });
      Inspect.registerNetwork();
      ChronicleNet.register();
      PayloadTypeRegistry.playS2C().register(MortalPayloads.MortalHud.ID, MortalPayloads.MortalHud.CODEC);
   }
}
