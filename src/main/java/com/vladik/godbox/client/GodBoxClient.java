package com.vladik.godbox.client;

import com.vladik.godbox.Kingdoms;
import com.vladik.godbox.net.HistoryPayloads;
import com.vladik.godbox.net.MortalPayloads;
import com.vladik.godbox.net.Payloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil.Type;

public class GodBoxClient implements ClientModInitializer {
   public static KeyBinding TOGGLE;

   public void onInitializeClient() {
      TOGGLE = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.godbox.toggle", Type.KEYSYM, 71, "category.godbox"));
      GodKeys.init();
      Favorites.load();
      GodParticlesClient.init();
      Art.init();
      ClientTickEvents.END_CLIENT_TICK.register(client -> {
         while (TOGGLE.wasPressed()) {
            if (client.player != null && client.currentScreen == null && ClientPlayNetworking.canSend(Payloads.Toggle.ID)) {
               ClientPlayNetworking.send(new Payloads.Toggle(true));
            }
         }
      });
      WorldRenderEvents.AFTER_TRANSLUCENT.register(BrushPreview::render);
      ClientPlayNetworking.registerGlobalReceiver(Payloads.Titles.ID, (payload, ctx) -> ctx.client().execute(() -> {
         String[] t = payload.data().split("\n", -1);
         Kingdoms.K[] ks = Kingdoms.K.values();

         for (int i = 0; i < ks.length && i < t.length; i++) {
            if (!t[i].isEmpty()) {
               ks[i].title = t[i];
            }
         }
      }));
      ClientPlayNetworking.registerGlobalReceiver(Payloads.Borders.ID, (payload, ctx) -> ctx.client().execute(() -> WorldMap.set(payload.data())));
      ClientPlayNetworking.registerGlobalReceiver(Payloads.Hud.ID, (payload, ctx) -> ctx.client().execute(() -> {
         HudState.set(payload.rate(), payload.frozen(), payload.age(), payload.ageLeft(), payload.agesOn());
         LawStates.sync(payload.laws());
      }));
      ClientPlayNetworking.registerGlobalReceiver(Payloads.Stats.ID, (payload, ctx) -> ctx.client().execute(() -> {
         StatsPanel.text = payload.text();
         if (payload.open()) {
            StatsPanel.on = true;
         }
      }));
      ClientPlayNetworking.registerGlobalReceiver(Payloads.State.ID, (payload, ctx) -> ctx.client().execute(() -> {
         MinecraftClient c = ctx.client();
         if (payload.on()) {
            if (!(c.currentScreen instanceof GodScreen)) {
               c.setScreen(new GodScreen());
            }
         } else if (c.currentScreen instanceof GodScreen gs) {
            gs.closeSilently();
         }
      }));
      ClientPlayNetworking.registerGlobalReceiver(
         HistoryPayloads.HistoryData.ID, (payload, ctx) -> ctx.client().execute(() -> WorldMap.history(payload.index(), payload.total(), payload.data()))
      );
      ClientPlayNetworking.registerGlobalReceiver(HistoryPayloads.HistoryOpen.ID, (payload, ctx) -> ctx.client().execute(WorldMap::openReplay));
      InspectScreen.register();
      ChronicleScreen.register();
      ClientPlayNetworking.registerGlobalReceiver(MortalPayloads.MortalHud.ID, (payload, ctx) -> ctx.client().execute(() -> MortalClient.set(payload)));
   }
}
