package com.vladik.godbox.net;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.CustomPayload.Id;
import net.minecraft.util.Identifier;

public final class ChroniclePayloads {
   public static final int PAGE = 40;
   public static final int TEXT = 300;
   public static final int MAX_PINS = 40;
   public static final int MAX_LINES = 48;
   public static final int LINE = 180;

   private ChroniclePayloads() {
   }

   public record Dto(long id, int day, int year, int season, String k, int type, boolean major, String text, boolean hasPos, int x, int z) {
      public static final PacketCodec<RegistryByteBuf, ChroniclePayloads.Dto> CODEC = PacketCodec.of((v, buf) -> {
         buf.writeVarLong(v.id);
         buf.writeVarInt(v.day);
         buf.writeVarInt(v.year);
         buf.writeVarInt(v.season + 1);
         PacketCodecs.string(16).encode(buf, v.k);
         buf.writeVarInt(v.type);
         buf.writeBoolean(v.major);
         PacketCodecs.string(300).encode(buf, v.text);
         buf.writeBoolean(v.hasPos);
         if (v.hasPos) {
            buf.writeInt(v.x);
            buf.writeInt(v.z);
         }
      }, buf -> {
         long id = buf.readVarLong();
         int day = buf.readVarInt();
         int year = buf.readVarInt();
         int season = buf.readVarInt() - 1;
         String k = PacketCodecs.string(16).decode(buf);
         int type = buf.readVarInt();
         boolean major = buf.readBoolean();
         String text = PacketCodecs.string(300).decode(buf);
         boolean has = buf.readBoolean();
         int x = 0;
         int z = 0;
         if (has) {
            x = buf.readInt();
            z = buf.readInt();
         }

         return new ChroniclePayloads.Dto(id, day, year, season, k, type, major, text, has, x, z);
      });
      public static final PacketCodec<RegistryByteBuf, List<ChroniclePayloads.Dto>> LIST = PacketCodecs.collection(ArrayList::new, CODEC, 40);
      public static final PacketCodec<RegistryByteBuf, List<ChroniclePayloads.Dto>> PIN_LIST = PacketCodecs.collection(ArrayList::new, CODEC, 40);
   }

   public record Go(long id) implements CustomPayload {
      public static final Id<ChroniclePayloads.Go> ID = new Id<>(Identifier.of("godbox", "chron_go"));
      public static final PacketCodec<RegistryByteBuf, ChroniclePayloads.Go> CODEC = PacketCodec.tuple(
         PacketCodecs.VAR_LONG, ChroniclePayloads.Go::id, ChroniclePayloads.Go::new
      );

      @Override
      public Id<? extends CustomPayload> getId() {
         return ID;
      }
   }

   public record Open() implements CustomPayload {
      public static final Id<ChroniclePayloads.Open> ID = new Id<>(Identifier.of("godbox", "chron_open"));
      public static final PacketCodec<RegistryByteBuf, ChroniclePayloads.Open> CODEC = PacketCodec.unit(new ChroniclePayloads.Open());

      @Override
      public Id<? extends CustomPayload> getId() {
         return ID;
      }
   }

   public record Page(int page, int pages, int total, List<ChroniclePayloads.Dto> entries) implements CustomPayload {
      public static final Id<ChroniclePayloads.Page> ID = new Id<>(Identifier.of("godbox", "chron_page"));
      public static final PacketCodec<RegistryByteBuf, ChroniclePayloads.Page> CODEC = PacketCodec.tuple(
         PacketCodecs.VAR_INT,
         ChroniclePayloads.Page::page,
         PacketCodecs.VAR_INT,
         ChroniclePayloads.Page::pages,
         PacketCodecs.VAR_INT,
         ChroniclePayloads.Page::total,
         ChroniclePayloads.Dto.LIST,
         ChroniclePayloads.Page::entries,
         ChroniclePayloads.Page::new
      );

      @Override
      public Id<? extends CustomPayload> getId() {
         return ID;
      }
   }

   public record PageReq(int page, int kMask, int tMask, boolean majorOnly, String query) implements CustomPayload {
      public static final Id<ChroniclePayloads.PageReq> ID = new Id<>(Identifier.of("godbox", "chron_req"));
      public static final PacketCodec<RegistryByteBuf, ChroniclePayloads.PageReq> CODEC = PacketCodec.tuple(
         PacketCodecs.VAR_INT,
         ChroniclePayloads.PageReq::page,
         PacketCodecs.VAR_INT,
         ChroniclePayloads.PageReq::kMask,
         PacketCodecs.VAR_INT,
         ChroniclePayloads.PageReq::tMask,
         PacketCodecs.BOOL,
         ChroniclePayloads.PageReq::majorOnly,
         PacketCodecs.string(40),
         ChroniclePayloads.PageReq::query,
         ChroniclePayloads.PageReq::new
      );

      @Override
      public Id<? extends CustomPayload> getId() {
         return ID;
      }
   }

   public record PinReq() implements CustomPayload {
      public static final Id<ChroniclePayloads.PinReq> ID = new Id<>(Identifier.of("godbox", "chron_pinreq"));
      public static final PacketCodec<RegistryByteBuf, ChroniclePayloads.PinReq> CODEC = PacketCodec.unit(new ChroniclePayloads.PinReq());

      @Override
      public Id<? extends CustomPayload> getId() {
         return ID;
      }
   }

   public record Pins(List<ChroniclePayloads.Dto> entries) implements CustomPayload {
      public static final Id<ChroniclePayloads.Pins> ID = new Id<>(Identifier.of("godbox", "chron_pins"));
      public static final PacketCodec<RegistryByteBuf, ChroniclePayloads.Pins> CODEC = PacketCodec.tuple(
         ChroniclePayloads.Dto.PIN_LIST, ChroniclePayloads.Pins::entries, ChroniclePayloads.Pins::new
      );

      @Override
      public Id<? extends CustomPayload> getId() {
         return ID;
      }
   }

   public record Sum(String k, List<String> lines) implements CustomPayload {
      public static final Id<ChroniclePayloads.Sum> ID = new Id<>(Identifier.of("godbox", "chron_sum"));
      public static final PacketCodec<RegistryByteBuf, ChroniclePayloads.Sum> CODEC = PacketCodec.tuple(
         PacketCodecs.string(16),
         ChroniclePayloads.Sum::k,
         PacketCodecs.collection(ArrayList::new, PacketCodecs.string(180), 48),
         ChroniclePayloads.Sum::lines,
         ChroniclePayloads.Sum::new
      );

      @Override
      public Id<? extends CustomPayload> getId() {
         return ID;
      }
   }

   public record SumReq(String k) implements CustomPayload {
      public static final Id<ChroniclePayloads.SumReq> ID = new Id<>(Identifier.of("godbox", "chron_sumreq"));
      public static final PacketCodec<RegistryByteBuf, ChroniclePayloads.SumReq> CODEC = PacketCodec.tuple(
         PacketCodecs.string(16), ChroniclePayloads.SumReq::k, ChroniclePayloads.SumReq::new
      );

      @Override
      public Id<? extends CustomPayload> getId() {
         return ID;
      }
   }
}
