package com.shxdowrate.solorust.world;

import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Прокладывает дороги через чанк: асфальт с разметкой, насыпь в низинах, срез холмов, мосты над водой.
 */
public class RoadFeature extends Feature<NoneFeatureConfiguration> {
	private static final int CLEAR_UP = 5;
	private static final int MAX_FILL = 10;
	private static final int MAX_PILLAR = 40;

	public RoadFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		ChunkGenerator generator = context.chunkGenerator();

		if (!RustWorld.is(generator)) {
			return false;
		}

		BlockPos origin = context.origin();
		int minX = Math.floorDiv(origin.getX(), 16) * 16;
		int minZ = Math.floorDiv(origin.getZ(), 16) * 16;

		RustWorld.HeightSampler sampler = RustWorld.sampler(generator, level, level.getLevel().getChunkSource().randomState(), level.getSeed());
		List<RoadNetwork.Road> roads = RoadNetwork.roadsNear(minX, minZ, minX + 15, minZ + 15, sampler);

		if (roads.isEmpty()) {
			return false;
		}

		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		boolean placed = false;

		for (int x = minX; x < minX + 16; x++) {
			for (int z = minZ; z < minZ + 16; z++) {
				RoadNetwork.Road best = null;
				double bestD = Double.MAX_VALUE;
				double bestS = 0;

				for (RoadNetwork.Road road : roads) {
					double[] proj = road.project(x + 0.5, z + 0.5);

					if (proj[0] < bestD) {
						bestD = proj[0];
						bestS = proj[1];
						best = road;
					}
				}

				if (best == null || bestD > RoadNetwork.CLEAR_HALF) {
					continue;
				}

				int roadTop = Math.round(best.heightAt(bestS, sampler)) - 1;
				placeColumn(level, pos, x, z, roadTop, bestD, bestS);
				placed = true;
			}
		}

		return placed;
	}

	private void placeColumn(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z, int roadTop, double d, double s) {
		boolean onRoad = d <= RoadNetwork.HALF_WIDTH;

		// Расчищаем место над дорогой (деревья, холмы).
		int clearFrom = onRoad ? roadTop + 1 : roadTop + 2;

		for (int y = clearFrom; y <= roadTop + CLEAR_UP; y++) {
			pos.set(x, y, z);

			if (!level.getBlockState(pos).isAir()) {
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}

		if (!onRoad) {
			return;
		}

		pos.set(x, roadTop, z);
		boolean water = level.getFluidState(pos).is(FluidTags.WATER)
				|| level.getFluidState(pos.below()).is(FluidTags.WATER);

		if (water) {
			placeBridge(level, pos, x, z, roadTop, d, s);
			return;
		}

		level.setBlock(pos, asphalt(x, z, d, s), Block.UPDATE_CLIENTS);

		// Насыпь в низинах.
		for (int y = roadTop - 1, i = 0; i < MAX_FILL; y--, i++) {
			pos.set(x, y, z);
			BlockState state = level.getBlockState(pos);

			if (!state.isAir() && !state.canBeReplaced()) {
				break;
			}

			level.setBlock(pos, i == 0 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	private void placeBridge(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z, int roadTop, double d, double s) {
		boolean edge = d > RoadNetwork.HALF_WIDTH - 0.9;

		pos.set(x, roadTop, z);
		level.setBlock(pos, Blocks.SPRUCE_PLANKS.defaultBlockState(), Block.UPDATE_CLIENTS);

		if (edge) {
			pos.set(x, roadTop + 1, z);
			level.setBlock(pos, Blocks.SPRUCE_FENCE.defaultBlockState(), Block.UPDATE_CLIENTS);

			// Опоры каждые 8 блоков.
			if (((int) Math.floor(s)) % 8 == 0) {
				for (int y = roadTop - 1, i = 0; i < MAX_PILLAR; y--, i++) {
					pos.set(x, y, z);
					BlockState state = level.getBlockState(pos);

					if (!state.isAir() && !state.canBeReplaced() && level.getFluidState(pos).isEmpty()) {
						break;
					}

					level.setBlock(pos, Blocks.SPRUCE_LOG.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	/** Потёртый асфальт с жёлтой прерывистой разметкой по центру и грунтовыми обочинами. */
	private static BlockState asphalt(int x, int z, double d, double s) {
		if (d <= 0.55 && ((int) Math.floor(s / 4)) % 2 == 0) {
			return Blocks.YELLOW_TERRACOTTA.defaultBlockState();
		}

		long h = RustWorld.mix(x, z, 0x1234L);
		int roll = (int) Math.floorMod(h, 100);

		if (d > RoadNetwork.HALF_WIDTH - 0.7) {
			return roll < 70 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
		}

		if (roll < 55) {
			return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
		}

		if (roll < 80) {
			return Blocks.BLACKSTONE.defaultBlockState();
		}

		if (roll < 93) {
			return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
		}

		return Blocks.GRAVEL.defaultBlockState();
	}
}
