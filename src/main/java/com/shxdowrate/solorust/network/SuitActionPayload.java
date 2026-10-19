package com.shxdowrate.solorust.network;

import com.shxdowrate.solorust.SoloRust;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Клиент → сервер: 0 — свернуть/развернуть костюм (G), 1 — вызвать костюм со станции (V). */
public record SuitActionPayload(int action) implements CustomPacketPayload {
	public static final int TOGGLE = 0;
	public static final int RECALL = 1;

	public static final CustomPacketPayload.Type<SuitActionPayload> TYPE = new CustomPacketPayload.Type<>(SoloRust.id("suit_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SuitActionPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SuitActionPayload::action,
			SuitActionPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
