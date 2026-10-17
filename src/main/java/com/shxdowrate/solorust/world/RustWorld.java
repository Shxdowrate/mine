package com.shxdowrate.solorust.world;

import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Помощники для мира типа «Rust». Мир считается растовским, если его генератор
 * использует наш {@link RustBiomeSource} (его подставляет пресет мира «Rust»).
 */
public final class RustWorld {
	private RustWorld() {
	}

	public static boolean is(ChunkGenerator generator) {
		return generator.getBiomeSource() instanceof RustBiomeSource;
	}

	/** Даёт высоту земли прямо из шума генератора — работает даже для ещё не сгенерированных чанков. */
	public interface HeightSampler {
		/** Первая свободная Y над землёй (дно водоёмов считается землёй). */
		int ground(int x, int z);

		int seaLevel();

		long seed();
	}

	public static HeightSampler sampler(ChunkGenerator generator, LevelHeightAccessor heightAccessor, RandomState randomState, long seed) {
		return new HeightSampler() {
			@Override
			public int ground(int x, int z) {
				return generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heightAccessor, randomState);
			}

			@Override
			public int seaLevel() {
				return generator.getSeaLevel();
			}

			@Override
			public long seed() {
				return seed;
			}
		};
	}

	public static long mix(long a, long b, long c) {
		long h = a * 0x9E3779B97F4A7C15L ^ b * 0xC2B2AE3D27D4EB4FL ^ c * 0x165667B19E3779F9L;
		h ^= h >>> 33;
		h *= 0xff51afd7ed558ccdL;
		h ^= h >>> 33;
		h *= 0xc4ceb9fe1a85ec53L;
		h ^= h >>> 33;
		return h;
	}
}
