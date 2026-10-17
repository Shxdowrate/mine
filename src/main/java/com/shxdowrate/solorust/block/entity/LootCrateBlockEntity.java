package com.shxdowrate.solorust.block.entity;

import com.shxdowrate.solorust.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Ящик с лутом. Ящики из генерации мира «респавнятся»: если ящик пуст, через 10 минут после того,
 * как его нашли пустым, он снова наполняется — монументы можно фармить, как в Расте.
 */
public class LootCrateBlockEntity extends RandomizableContainerBlockEntity {
	private static final long RESPAWN_TICKS = 12000;

	private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
	private String respawnTable = "";
	private long emptySince = 0;

	public LootCrateBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.LOOT_CRATE, pos, state);
	}

	/** Задать таблицу лута и включить респавн (вызывается генерацией мира). */
	public void setRespawningLoot(ResourceKey<LootTable> table, long seed) {
		this.respawnTable = table.identifier().toString();
		setLootTable(table, seed);
	}

	/** Вызывается при открытии: если пора — снова наполняет ящик. */
	public void tryRespawn(Level level) {
		if (respawnTable.isEmpty() || getLootTable() != null || !isEmpty()) {
			emptySince = 0;
			return;
		}

		long now = level.getGameTime();

		if (emptySince == 0) {
			emptySince = now;
			setChanged();
		} else if (now - emptySince >= RESPAWN_TICKS) {
			setLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(respawnTable)), level.getRandom().nextLong());
			emptySince = 0;
			setChanged();
		}
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

		respawnTable = input.getStringOr("RespawnTable", "");
		emptySince = input.getLongOr("EmptySince", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);

		if (!trySaveLootTable(output)) {
			ContainerHelper.saveAllItems(output, items);
		}

		output.putString("RespawnTable", respawnTable);
		output.putLong("EmptySince", emptySince);
	}
}
