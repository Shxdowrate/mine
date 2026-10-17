package com.shxdowrate.solorust.block.entity;

import com.shxdowrate.solorust.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class LootCrateBlockEntity extends RandomizableContainerBlockEntity {
	private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);

	public LootCrateBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.LOOT_CRATE, pos, state);
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> list) {
		this.items = list;
	}

	@Override
	public int getContainerSize() {
		return 27;
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable(getBlockState().getBlock().getDescriptionId());
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return ChestMenu.threeRows(containerId, inventory, this);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);

		if (!tryLoadLootTable(input)) {
			ContainerHelper.loadAllItems(input, items);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);

		if (!trySaveLootTable(output)) {
			ContainerHelper.saveAllItems(output, items);
		}
	}
}
