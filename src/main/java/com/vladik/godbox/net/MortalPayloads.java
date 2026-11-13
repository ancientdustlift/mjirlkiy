package com.vladik.godbox.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.CustomPayload.Id;
import net.minecraft.util.Identifier;

public final class MortalPayloads {
   private MortalPayloads() {
   }

   public record MortalHud(boolean on, int pool, int cap, String title, String progress, int state, int scale, int perMin) implements CustomPayload {
      public static final Id<MortalPayloads.MortalHud> ID = new Id<>(Identifier.of("godbox", "mortal_hud"));
      public static final PacketCodec<RegistryByteBuf, MortalPayloads.MortalHud> CODEC = PacketCodec.of(
         MortalPayloads.MortalHud::write, MortalPayloads.MortalHud::new
      );

      private MortalHud(RegistryByteBuf b) {
         this(b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readString(), b.readString(), b.readVarInt(), b.readVarInt(), b.readVarInt());
      }

      private void write(RegistryByteBuf b) {
         b.writeBoolean(this.on);
         b.writeVarInt(this.pool);
         b.writeVarInt(this.cap);
         b.writeString(this.title);
         b.writeString(this.progress);
         b.writeVarInt(this.state);
         b.writeVarInt(this.scale);
         b.writeVarInt(this.perMin);
      }

      @Override
      public Id<? extends CustomPayload> getId() {
         return ID;
      }
   }
}
