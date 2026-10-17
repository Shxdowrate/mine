package com.shxdowrate.solorust.block.entity;

import java.util.List;

import com.shxdowrate.solorust.mechanics.Recycling;
import com.shxdowrate.solorust.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Переработчик: верхний ряд (9 слотов) — вход, два нижних — выход.
 * Каждые 2 секунды разбирает один предмет из входа.
 */
public class RecyclerBlockEntity extends BaseContainerBlockEntity {
	public static final int INPUT_SLOTS = 9;
	public static final int SIZE = 27;
	private static final int INTERVAL = 40;

	private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
	private int timer;

	public RecyclerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.RECYCLER, pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, RecyclerBlockEntity recycler) {
		if (++recycler.timer < INTERVAL) {
			return;
		}

		recycler.timer = 0;

		for (int i = 0; i < INPUT_SLOTS; i++) {
			ItemStack input = recycler.items.get(i);

			if (input.isEmpty()) {
				continue;
			}

			List<Recycling.Output> outputs = Recycling.get(input);

			if (outputs == null || !recycler.canFit(outputs)) {
				continue;
			}

			input.shrink(1);

			for (Recycling.Output output : outputs) {
				recycler.insert(output.stack());
			}

			recycler.setChanged();
			level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.6f, 0.8f + level.getRandom().nextFloat() * 0.3f);
			return;
		}
	}

	private boolean canFit(List<Recycling.Output> outputs) {
		NonNullList<ItemStack> copy = NonNullList.withSize(SIZE, ItemStack.EMPTY);

		for (int i = 0; i < SIZE; i++) {
			copy.set(i, items.get(i).copy());
		}

		for (Recycling.Output output : outputs) {
			if (!insertInto(copy, output.stack())) {
				return false;
			}
		}

		return true;
	}

	private void insert(ItemStack stack) {
		insertInto(items, stack);
	}

	private static boolean insertInto(NonNullList<ItemStack> list, ItemStack stack) {
		ItemStack remaining = stack.copy();

		for (int i = INPUT_SLOTS; i < SIZE && !remaining.isEmpty(); i++) {
			ItemStack slot = list.get(i);

			if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, remaining)) {
				int move = Math.min(remaining.getCount(), slot.getMaxStackSize() - slot.getCount());

				if (move > 0) {
					slot.grow(move);
					remaining.shrink(move);
				}
			}
		}

		for (int i = INPUT_SLOTS; i < SIZE && !remaining.isEmpty(); i++) {
			if (list.get(i).isEmpty()) {
				list.set(i, remaining.copy());
				remaining.setCount(0);
			}
		}

		return remaining.isEmpty();
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
		return SIZE;
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("container.solorust.recycler");
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return ChestMenu.threeRows(containerId, inventory, this);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
	}
}
