package com.vladik.godbox.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.CustomPayload.Id;
import net.minecraft.util.Identifier;

public final class HistoryPayloads {
   private HistoryPayloads() {
   }

   public record HistoryData(int index, int total, String data) implements CustomPayload {
      public static final Id<HistoryPayloads.HistoryData> ID = new Id<>(Identifier.of("godbox", "history_data"));
      public static final PacketCodec<RegistryByteBuf, HistoryPayloads.HistoryData> CODEC = PacketCodec.tuple(
         PacketCodecs.VAR_INT,
         HistoryPayloads.HistoryData::index,
         PacketCodecs.VAR_INT,
         HistoryPayloads.HistoryData::total,
         PacketCodecs.string(262144),
         HistoryPayloads.HistoryData::data,
         HistoryPayloads.HistoryData::new
      );

      @Override
      public Id<? extends CustomPayload> getId() {
         return ID;
      }
   }

   public record HistoryOpen() implements CustomPayload {
      public static final Id<HistoryPayloads.HistoryOpen> ID = new Id<>(Identifier.of("godbox", "history_open"));
      public static final PacketCodec<RegistryByteBuf, HistoryPayloads.HistoryOpen> CODEC = PacketCodec.unit(new HistoryPayloads.HistoryOpen());

      @Override
      public Id<? extends CustomPayload> getId() {
         return ID;
      }
   }

   public record HistoryRequest() implements CustomPayload {
      public static final Id<HistoryPayloads.HistoryRequest> ID = new Id<>(Identifier.of("godbox", "history_req"));
      public static final PacketCodec<RegistryByteBuf, HistoryPayloads.HistoryRequest> CODEC = PacketCodec.unit(new HistoryPayloads.HistoryRequest());

      @Override
      public Id<? extends CustomPayload> getId() {
         return ID;
      }
   }
}
