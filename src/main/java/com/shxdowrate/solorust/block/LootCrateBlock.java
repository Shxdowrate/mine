package com.shxdowrate.solorust.block;

import com.mojang.serialization.MapCodec;
import com.shxdowrate.solorust.block.entity.LootCrateBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Ящик с лутом: открывается как сундук, содержимое берётся из таблицы лута при первом открытии. */
public class LootCrateBlock extends BaseEntityBlock {
	public static final MapCodec<LootCrateBlock> CODEC = simpleCodec(LootCrateBlock::new);

	public LootCrateBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			if (level.getBlockEntity(pos) instanceof LootCrateBlockEntity crate) {
				crate.tryRespawn(level);
			}

			MenuProvider menu = state.getMenuProvider(level, pos);

			if (menu != null) {
				player.openMenu(menu);
			}
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new LootCrateBlockEntity(pos, state);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}
}
