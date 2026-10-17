package com.shxdowrate.solorust.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Бочка как в Расте: её не открывают, а разбивают — лут вываливается (см. таблицу лута блока).
 */
public class LootBarrelBlock extends Block {
	private static final VoxelShape SHAPE = Shapes.box(2 / 16f, 0, 2 / 16f, 14 / 16f, 1, 14 / 16f);

	public LootBarrelBlock(Properties properties) {
		super(properties);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
}
