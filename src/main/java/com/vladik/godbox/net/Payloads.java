package com.vladik.godbox.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.CustomPayload.Id;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class Payloads {
   private Payloads() {
   }

   public record Borders(String data) implements CustomPayload {
      public static final Id<Payloads.Borders> ID = new Id<>(Identifier.of("godbox", "borders"));
      public static final PacketCodec<RegistryByteBuf, Payloads.Borders> CODEC = PacketCodec.tuple(
         PacketCodecs.STRING, Payloads.Borders::data, Payloads.Borders::new
      );

      @Override
      public Id<? extends CustomPayload> getId() {
         return ID;
      }
   }

   public record Hud(float rate, boolean frozen, int age, int ageLeft, boolean agesOn, long laws) implements CustomPayload {
      public static final Id<Payloads.Hud> ID = new Id<>(Identifier.of("godbox", "hud"));
      public static final PacketCodec<RegistryByteBuf, Payloads.Hud> CODEC = PacketCodec.tuple(
         PacketCodecs.FLOAT,
         Payloads.Hud::rate,
         PacketCodecs.BOOL,
         Payloads.Hud::frozen,
         PacketCodecs.VAR_INT,
         Payloads.Hud::age,
         PacketCodecs.VAR_INT,
         Payloads.Hud::ageLeft,
         PacketCodecs.BOOL,
         Payloads.Hud::agesOn,
         PacketCodecs.VAR_LONG,
         Payloads.Hud::laws,
         Payloads.Hud::new
      );

      @Override
      public Id<? extends CustomPayload> getId() {
         return ID;
      }
   }

   public record State(boolean on) implements CustomPayload {
      public static final Id<Payloads.State> ID = new Id<>(Identifier.of("godbox", "state"));
      public static final PacketCodec<RegistryByteBuf, Payloads.State> CODEC = PacketCodec.tuple(PacketCodecs.BOOL, Payloads.State::on, Payloads.State::new);

      @Override
      public Id<? extends CustomPayload> getId() {
         return ID;
      }
   }

   public record Stats(boolean open, String text) implements CustomPayload {
      public static final Id<Payloads.Stats> ID = new Id<>(Identifier.of("godbox", "stats"));
      public static final PacketCodec<RegistryByteBuf, Payloads.Stats> CODEC = PacketCodec.tuple(
         PacketCodecs.BOOL, Payloads.Stats::open, PacketCodecs.STRING, Payloads.Stats::text, Payloads.Stats::new
      );

      @Override
      public Id<? extends CustomPayload> getId() {
         return ID;
      }
   }

   public record Titles(String data) implements CustomPayload {
      public static final Id<Payloads.Titles> ID = new Id<>(Identifier.of("godbox", "titles"));
      public static final PacketCodec<RegistryByteBuf, Payloads.Titles> CODEC = PacketCodec.tuple(
         PacketCodecs.STRING, Payloads.Titles::data, Payloads.Titles::new
      );

      @Override
      public Id<? extends CustomPayload> getId() {
         return ID;
      }
   }

   public record Toggle(boolean on) implements CustomPayload {
      public static final Id<Payloads.Toggle> ID = new Id<>(Identifier.of("godbox", "toggle"));
      public static final PacketCodec<RegistryByteBuf, Payloads.Toggle> CODEC = PacketCodec.tuple(PacketCodecs.BOOL, Payloads.Toggle::on, Payloads.Toggle::new);

      @Override
      public Id<? extends CustomPayload> getId() {
         return ID;
      }
   }

   public record Use(String power, BlockPos pos, int brush, boolean first) implements CustomPayload {
      public static final Id<Payloads.Use> ID = new Id<>(Identifier.of("godbox", "use3"));
      public static final PacketCodec<RegistryByteBuf, Payloads.Use> CODEC = PacketCodec.tuple(
         PacketCodecs.STRING,
         Payloads.Use::power,
         BlockPos.PACKET_CODEC,
         Payloads.Use::pos,
         PacketCodecs.VAR_INT,
         Payloads.Use::brush,
         PacketCodecs.BOOL,
         Payloads.Use::first,
         Payloads.Use::new
      );

      @Override
      public Id<? extends CustomPayload> getId() {
         return ID;
      }
   }
}
