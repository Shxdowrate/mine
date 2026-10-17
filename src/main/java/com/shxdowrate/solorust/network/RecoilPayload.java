package com.shxdowrate.solorust.network;

import com.shxdowrate.solorust.SoloRust;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Сервер → клиент: отдача после выстрела (подброс камеры) и было ли попадание (хитмаркер). */
public record RecoilPayload(float pitch, float yaw, boolean hit) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RecoilPayload> TYPE = new CustomPacketPayload.Type<>(SoloRust.id("recoil"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RecoilPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, RecoilPayload::pitch,
			ByteBufCodecs.FLOAT, RecoilPayload::yaw,
			ByteBufCodecs.BOOL, RecoilPayload::hit,
			RecoilPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
