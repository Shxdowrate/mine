package com.shxdowrate.solorust.network;

import com.shxdowrate.solorust.SoloRust;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Клиент → сервер: действия в планшете WARDEN. value — номер коробки или костюма. */
public record WardenPayload(int action, int value) implements CustomPacketPayload {
	public static final int OPEN_BOX = 0;
	public static final int EQUIP = 1;
	public static final int UNEQUIP = 2;

	public static final CustomPacketPayload.Type<WardenPayload> TYPE = new CustomPacketPayload.Type<>(SoloRust.id("warden"));
	public static final StreamCodec<RegistryFriendlyByteBuf, WardenPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, WardenPayload::action,
			ByteBufCodecs.VAR_INT, WardenPayload::value,
			WardenPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
