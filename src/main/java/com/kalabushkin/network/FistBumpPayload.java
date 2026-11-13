package com.kalabushkin.network;

import com.kalabushkin.Kalabushkin;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Tells clients that a mob returns a fist bump, so they can play its animation. */
public record FistBumpPayload(int entityId) implements CustomPayload {
	public static final Id<FistBumpPayload> ID = new Id<>(Kalabushkin.id("fist_bump"));
	public static final PacketCodec<RegistryByteBuf, FistBumpPayload> CODEC =
			PacketCodecs.VAR_INT.xmap(FistBumpPayload::new, FistBumpPayload::entityId).cast();

	@Override
	public Id<FistBumpPayload> getId() {
		return ID;
	}
}
