package com.shxdowrate.solorust.world;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Рудный валун из нескольких блоков, наполовину утопленный в землю.
 */
public class BoulderFeature extends Feature<NoneFeatureConfiguration> {
	private final Supplier<Block> node;

	public BoulderFeature(Codec<NoneFeatureConfiguration> codec, Supplier<Block> node) {
		super(codec);
		this.node = node;
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		if (!RustWorld.is(context.chunkGenerator())) {
			return false;
		}

		WorldGenLevel level = context.level();
		RandomSource random = context.random();
		BlockPos origin = context.origin();
		BlockState nodeState = node.get().defaultBlockState();

		float rx = 1.2f + random.nextFloat() * 0.8f;
		float ry = 1.0f + random.nextFloat() * 0.5f;
		float rz = 1.2f + random.nextFloat() * 0.8f;
		double cx = origin.getX() + 0.5;
		double cy = origin.getY() - 0.2;
		double cz = origin.getZ() + 0.5;

		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int placed = 0;

		for (int dx = -2; dx <= 2; dx++) {
			for (int dy = -2; dy <= 2; dy++) {
				for (int dz = -2; dz <= 2; dz++) {
					double nx = (origin.getX() + dx + 0.5 - cx) / rx;
					double ny = (origin.getY() + dy + 0.5 - cy) / ry;
					double nz = (origin.getZ() + dz + 0.5 - cz) / rz;
					double dist = nx * nx + ny * ny + nz * nz;

					if (dist > 1.0 + (random.nextFloat() - 0.5f) * 0.3f) {
						continue;
					}

					pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
					BlockState current = level.getBlockState(pos);

					if (current.isAir() || current.canBeReplaced()
							|| current.is(BlockTags.DIRT) || current.is(BlockTags.SAND)
							|| current.is(BlockTags.BASE_STONE_OVERWORLD)) {
						level.setBlock(pos, nodeState, Block.UPDATE_CLIENTS);
						placed++;
					}
				}
			}
		}

		return placed > 0;
	}
}
