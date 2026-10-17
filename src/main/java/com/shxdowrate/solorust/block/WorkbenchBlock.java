package com.shxdowrate.solorust.block;

import com.shxdowrate.solorust.menu.WorkbenchMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Верстак 1/2/3 уровня: открывает каталог крафта своего уровня. */
public class WorkbenchBlock extends Block {
	private final int tier;

	public WorkbenchBlock(Properties properties, int tier) {
		super(properties);
		this.tier = tier;
	}

	@Override
	public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			player.openMenu(new SimpleMenuProvider(
					(containerId, inventory, p) -> new WorkbenchMenu(containerId, inventory, tier, pos),
					Component.translatable(getDescriptionId())));
		}

		return InteractionResult.SUCCESS;
	}
}
