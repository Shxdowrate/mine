package com.shxdowrate.solorust.network;

import com.shxdowrate.solorust.gun.GunLogic;
import com.shxdowrate.solorust.magic.ArtifactItem;
import com.shxdowrate.solorust.suit.SuitActions;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ModNetworking {
	private ModNetworking() {
	}

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(GunActionPayload.TYPE, GunActionPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(RecoilPayload.TYPE, RecoilPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ArtifactPayload.TYPE, ArtifactPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ArtifactPayload.TYPE, (payload, context) -> {
			switch (payload.action()) {
				case ArtifactPayload.SELECT -> ArtifactItem.select(context.player(), payload.value());
				case ArtifactPayload.CAST -> ArtifactItem.cast(context.player(), payload.value());
				default -> {
				}
			}
		});

		PayloadTypeRegistry.serverboundPlay().register(SuitActionPayload.TYPE, SuitActionPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(SuitActionPayload.TYPE, (payload, context) -> {
			switch (payload.action()) {
				case SuitActionPayload.TOGGLE -> SuitActions.toggle(context.player());
				case SuitActionPayload.RECALL -> SuitActions.recall(context.player());
				default -> {
				}
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(GunActionPayload.TYPE, (payload, context) -> {
			switch (payload.action()) {
				case GunActionPayload.FIRE -> GunLogic.fire(context.player());
				case GunActionPayload.RELOAD -> GunLogic.reload(context.player());
				default -> {
				}
			}
		});
	}
}
