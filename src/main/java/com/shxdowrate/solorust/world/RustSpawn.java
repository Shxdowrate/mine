package com.shxdowrate.solorust.world;

import com.mojang.serialization.Codec;
import com.shxdowrate.solorust.SoloRust;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.storage.LevelData;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * При первом запуске мира «Rust» переносит точку спавна на ближайшую к центру дорогу.
 */
public final class RustSpawn {
	private static final AttachmentType<Boolean> SPAWN_PLACED = AttachmentRegistry.createPersistent(SoloRust.id("spawn_placed"), Codec.BOOL);

	private RustSpawn() {
	}

	public static void init() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			ServerLevel level = server.overworld();
			ChunkGenerator generator = level.getChunkSource().getGenerator();

			if (!RustWorld.is(generator) || Boolean.TRUE.equals(level.getAttached(SPAWN_PLACED))) {
				return;
			}

			RustWorld.HeightSampler sampler = RustWorld.sampler(generator, level, level.getChunkSource().randomState(), level.getSeed());
			BlockPos spawn = RoadNetwork.findSpawn(sampler);

			if (spawn != null) {
				level.setRespawnData(LevelData.RespawnData.of(level.dimension(), spawn, 0.0f, 0.0f));
				SoloRust.LOGGER.info("Rust world: spawn moved to road at {}", spawn);
			}

			level.setAttached(SPAWN_PLACED, true);
		});
	}
}
