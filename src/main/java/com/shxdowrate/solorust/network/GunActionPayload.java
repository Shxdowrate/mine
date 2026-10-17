package com.shxdowrate.solorust.network;

import com.shxdowrate.solorust.SoloRust;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Клиент → сервер: 0 — выстрел, 1 — перезарядка. */
public record GunActionPayload(int action) implements CustomPacketPayload {
	public static final int FIRE = 0;
	public static final int RELOAD = 1;

	public static final CustomPacketPayload.Type<GunActionPayload> TYPE = new CustomPacketPayload.Type<>(SoloRust.id("gun_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GunActionPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, GunActionPayload::action,
			GunActionPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
