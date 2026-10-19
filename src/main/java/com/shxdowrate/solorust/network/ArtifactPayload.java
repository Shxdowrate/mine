package com.shxdowrate.solorust.network;

import com.shxdowrate.solorust.SoloRust;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Клиент → сервер: выбор ячейки способностей артефакта (value = 0–8) или применение (value = 0 — ЛКМ, 1 — ПКМ). */
public record ArtifactPayload(int action, int value) implements CustomPacketPayload {
	public static final int SELECT = 0;
	public static final int CAST = 1;

	public static final CustomPacketPayload.Type<ArtifactPayload> TYPE = new CustomPacketPayload.Type<>(SoloRust.id("artifact"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ArtifactPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ArtifactPayload::action,
			ByteBufCodecs.VAR_INT, ArtifactPayload::value,
			ArtifactPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
