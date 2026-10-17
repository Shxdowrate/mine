package com.shxdowrate.solorust.block.entity;

import java.util.ArrayList;
import java.util.List;

import com.shxdowrate.solorust.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Сборочная станция хранит один костюм: шлем, нагрудник, поножи, ботинки. */
public class SuitStationBlockEntity extends BlockEntity {
	private NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);

	public SuitStationBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SUIT_STATION, pos, state);
	}

	public ItemStack get(int slot) {
		return items.get(slot);
	}

	public void set(int slot, ItemStack stack) {
		items.set(slot, stack);
		setChanged();
	}

	public boolean isFull() {
		return items.stream().noneMatch(ItemStack::isEmpty);
	}

	public boolean isEmpty() {
		return items.stream().allMatch(ItemStack::isEmpty);
	}

	/** Забрать весь костюм со станции. */
	public List<ItemStack> takeAll() {
		List<ItemStack> list = new ArrayList<>();

		for (int i = 0; i < items.size(); i++) {
			list.add(items.get(i));
			items.set(i, ItemStack.EMPTY);
		}

		setChanged();
		return list;
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null) {
			Containers.dropContents(level, pos, items);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(4, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
	}
}
