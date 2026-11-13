package com.vladik.godbox.net;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.CustomPayload.Id;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

public final class InspectPayloads {
   public static final int GRAND = 0;
   public static final int PARENT = 1;
   public static final int SELF = 2;
   public static final int SIBLING = 3;
   public static final int CHILD = 4;

   private InspectPayloads() {
   }

   public record Ask(UUID id) implements CustomPayload {
      public static final Id<InspectPayloads.Ask> ID = new Id<>(Identifier.of("godbox", "inspect_ask"));
      public static final PacketCodec<RegistryByteBuf, InspectPayloads.Ask> CODEC = PacketCodec.tuple(
         Uuids.PACKET_CODEC, InspectPayloads.Ask::id, InspectPayloads.Ask::new
      );

      @Override
      public Id<? extends CustomPayload> getId() {
         return ID;
      }
   }

   public record Follow(UUID id, boolean on) implements CustomPayload {
      public static final Id<InspectPayloads.Follow> ID = new Id<>(Identifier.of("godbox", "inspect_follow"));
      public static final PacketCodec<RegistryByteBuf, InspectPayloads.Follow> CODEC = PacketCodec.tuple(
         Uuids.PACKET_CODEC, InspectPayloads.Follow::id, PacketCodecs.BOOL, InspectPayloads.Follow::on, InspectPayloads.Follow::new
      );

      @Override
      public Id<? extends CustomPayload> getId() {
         return ID;
      }
   }

   public record FollowState(boolean on, String name) implements CustomPayload {
      public static final Id<InspectPayloads.FollowState> ID = new Id<>(Identifier.of("godbox", "inspect_follow_state"));
      public static final PacketCodec<RegistryByteBuf, InspectPayloads.FollowState> CODEC = PacketCodec.tuple(
         PacketCodecs.BOOL, InspectPayloads.FollowState::on, PacketCodecs.STRING, InspectPayloads.FollowState::name, InspectPayloads.FollowState::new
      );

      @Override
      public Id<? extends CustomPayload> getId() {
         return ID;
      }
   }

   public record Node(UUID id, String name, int rel, UUID link, boolean alive, int born, int died, int rgb, boolean royal) {
      static InspectPayloads.Node read(RegistryByteBuf b) {
         return new InspectPayloads.Node(
            b.readUuid(), b.readString(), b.readVarInt(), b.readUuid(), b.readBoolean(), b.readInt(), b.readInt(), b.readInt(), b.readBoolean()
         );
      }

      void write(RegistryByteBuf b) {
         b.writeUuid(this.id);
         b.writeString(this.name);
         b.writeVarInt(this.rel);
         b.writeUuid(this.link);
         b.writeBoolean(this.alive);
         b.writeInt(this.born);
         b.writeInt(this.died);
         b.writeInt(this.rgb);
         b.writeBoolean(this.royal);
      }
   }

   public record Profile(
      int entityId,
      UUID id,
      String name,
      String kingdom,
      int rgb,
      boolean alive,
      boolean following,
      int year,
      List<String> lines,
      List<InspectPayloads.Node> tree
   ) implements CustomPayload {
      public static final Id<InspectPayloads.Profile> ID = new Id<>(Identifier.of("godbox", "inspect_profile"));
      public static final PacketCodec<RegistryByteBuf, InspectPayloads.Profile> CODEC = CustomPayload.codecOf(
         InspectPayloads.Profile::write, InspectPayloads.Profile::read
      );

      static InspectPayloads.Profile read(RegistryByteBuf b) {
         int entityId = b.readInt();
         UUID id = b.readUuid();
         String name = b.readString();
         String kingdom = b.readString();
         int rgb = b.readInt();
         boolean alive = b.readBoolean();
         boolean following = b.readBoolean();
         int year = b.readInt();
         int n = Math.min(b.readVarInt(), 256);
         List<String> lines = new ArrayList<>(n);

         for (int i = 0; i < n; i++) {
            lines.add(b.readString());
         }

         n = Math.min(b.readVarInt(), 256);
         List<InspectPayloads.Node> tree = new ArrayList<>(n);

         for (int i = 0; i < n; i++) {
            tree.add(InspectPayloads.Node.read(b));
         }

         return new InspectPayloads.Profile(entityId, id, name, kingdom, rgb, alive, following, year, lines, tree);
      }

      void write(RegistryByteBuf b) {
         b.writeInt(this.entityId);
         b.writeUuid(this.id);
         b.writeString(this.name);
         b.writeString(this.kingdom);
         b.writeInt(this.rgb);
         b.writeBoolean(this.alive);
         b.writeBoolean(this.following);
         b.writeInt(this.year);
         b.writeVarInt(this.lines.size());

         for (String s : this.lines) {
            b.writeString(s);
         }

         b.writeVarInt(this.tree.size());

         for (InspectPayloads.Node n : this.tree) {
            n.write(b);
         }
      }

      @Override
      public Id<? extends CustomPayload> getId() {
         return ID;
      }
   }
}
