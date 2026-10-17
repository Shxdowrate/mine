package com.shxdowrate.solorust.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Рудный «камень» как в Расте: лежит на поверхности, при добыче выдаёт пачку ресурсов.
 */
public class NodeBlock extends Block {
	private static final VoxelShape SHAPE = Shapes.or(
			Shapes.box(1 / 16f, 0, 1 / 16f, 15 / 16f, 9 / 16f, 15 / 16f),
			Shapes.box(3 / 16f, 9 / 16f, 3 / 16f, 13 / 16f, 13 / 16f, 13 / 16f)
	);

	public NodeBlock(Properties properties) {
		super(properties);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
}
