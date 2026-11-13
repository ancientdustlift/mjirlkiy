package com.vladik.godbox;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.vladik.godbox.net.Payloads;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class KingdomNames {
   public static final int MAX_LEN = 32;

   private KingdomNames() {
   }

   static void init() {
      SaveExt.register("kname", KingdomNames::save, KingdomNames::load, KingdomNames::reset);
      ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendTo(handler.player));
      CommandRegistrationCallback.EVENT
         .register(
            (dispatcher, access, env) -> {
               SuggestionProvider<ServerCommandSource> ids = (ctx, b) -> {
                  for (Kingdoms.K k : Kingdoms.K.values()) {
                     b.suggest(k.id);
                  }

                  return b.buildFuture();
               };
               dispatcher.register(
                  CommandManager.literal("godbox")
                     .requires(src -> src.hasPermissionLevel(2))
                     .then(
                        CommandManager.literal("kingdom")
                           .then(
                              CommandManager.argument("id", StringArgumentType.word())
                                 .suggests(ids)
                                 .then(
                                    CommandManager.literal("name")
                                       .then(
                                          CommandManager.argument("text", StringArgumentType.greedyString())
                                             .executes(c -> name(c.getSource(), StringArgumentType.getString(c, "id"), StringArgumentType.getString(c, "text")))
                                       )
                                 )
                                 .then(CommandManager.literal("reset").executes(c -> name(c.getSource(), StringArgumentType.getString(c, "id"), null)))
                           )
                     )
               );
            }
         );
   }

   public static String sanitize(String s) {
      if (s == null) {
         return "";
      } else {
         StringBuilder b = new StringBuilder();
         s.codePoints().forEach(cp -> {
            if (cp != 167) {
               if (cp == 124) {
                  b.append('/');
               } else if (!Character.isISOControl(cp) && !Character.isWhitespace(cp)) {
                  b.appendCodePoint(cp);
               } else {
                  b.append(' ');
               }
            }
         });
         String t = b.toString().replaceAll(" {2,}", " ").trim();
         if (t.codePointCount(0, t.length()) > 32) {
            t = t.substring(0, t.offsetByCodePoints(0, 32)).trim();
         }

         return t;
      }
   }

   private static int name(ServerCommandSource src, String id, String text) {
      Kingdoms.K k = Kingdoms.byId(id);
      if (k == null) {
         src.sendError(Text.literal("Нет такого королевства: " + id));
         return 0;
      } else {
         String nt = text == null ? k.defaultTitle : sanitize(text);
         if (nt.isEmpty()) {
            src.sendError(Text.literal("Название не может быть пустым"));
            return 0;
         } else {
            for (Kingdoms.K o : Kingdoms.K.values()) {
               if (o != k && o.title.equalsIgnoreCase(nt)) {
                  src.sendError(Text.literal("Название уже занято: " + o.id));
                  return 0;
               }
            }

            String old = k.title;
            if (old.equals(nt)) {
               src.sendFeedback(() -> Text.literal("Название не изменилось: " + nt), false);
               return 1;
            } else {
               MinecraftServer s = src.getServer();
               apply(s, k, nt);
               Chronicle.log(k, Chronicle.Type.OTHER, true, null, "«" + old + "» отныне зовётся «" + nt + "»");
               s.getPlayerManager().broadcast(Text.literal("✎ «" + old + "» отныне зовётся «" + nt + "»").formatted(k.color), false);
               return 1;
            }
         }
      }
   }

   private static void apply(MinecraftServer s, Kingdoms.K k, String title) {
      k.title = title;
      Team t = s.getScoreboard().getTeam(k.team());
      if (t != null) {
         t.setDisplayName(Text.literal(title));
      }

      sendAll(s);
   }

   private static String data() {
      StringBuilder b = new StringBuilder();

      for (Kingdoms.K k : Kingdoms.K.values()) {
         b.append(b.length() > 0 ? "\n" : "").append(k.title);
      }

      return b.toString();
   }

   static void sendTo(ServerPlayerEntity p) {
      if (ServerPlayNetworking.canSend(p, Payloads.Titles.ID)) {
         ServerPlayNetworking.send(p, new Payloads.Titles(data()));
      }
   }

   static void sendAll(MinecraftServer s) {
      for (ServerPlayerEntity p : s.getPlayerManager().getPlayerList()) {
         sendTo(p);
      }
   }

   private static List<String> save() {
      List<String> out = new ArrayList<>();

      for (Kingdoms.K k : Kingdoms.K.values()) {
         if (!k.title.equals(k.defaultTitle)) {
            out.add(k.id + "|" + sanitize(k.title));
         }
      }

      return out;
   }

   private static void load(String[] a) {
      if (a.length >= 2) {
         Kingdoms.K k = Kingdoms.byId(a[0]);
         String t = sanitize(a[1]);
         if (k != null && !t.isEmpty()) {
            k.title = t;
         }
      }
   }

   static void reset() {
      for (Kingdoms.K k : Kingdoms.K.values()) {
         k.title = k.defaultTitle;
      }
   }
}
