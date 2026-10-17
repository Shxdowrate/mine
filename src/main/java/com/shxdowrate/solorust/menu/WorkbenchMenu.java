package com.shxdowrate.solorust.menu;

import java.util.ArrayList;
import java.util.List;

import com.shxdowrate.solorust.mechanics.Blueprints;
import com.shxdowrate.solorust.mechanics.WorkbenchRecipes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

/**
 * Каталог крафта верстака. Использует обычное окно сундука (не нужен клиентский код):
 * в верхних слотах лежат «витринные» предметы с ценой в описании, клик по ним — скрафтить.
 */
public class WorkbenchMenu extends ChestMenu {
	private static final int ROWS = 6;

	private final Container catalog;
	private final List<WorkbenchRecipes.Recipe> recipes;
	private final BlockPos pos;

	public WorkbenchMenu(int containerId, Inventory inventory, int tier, BlockPos pos) {
		this(containerId, inventory, new SimpleContainer(9 * ROWS), WorkbenchRecipes.forTier(tier), pos);
	}

	private WorkbenchMenu(int containerId, Inventory inventory, SimpleContainer catalog, List<WorkbenchRecipes.Recipe> recipes, BlockPos pos) {
		super(MenuType.GENERIC_9x6, containerId, inventory, catalog, ROWS);
		this.catalog = catalog;
		this.recipes = recipes;
		this.pos = pos;

		for (int i = 0; i < recipes.size() && i < catalog.getContainerSize(); i++) {
			catalog.setItem(i, display(recipes.get(i), inventory.player));
		}
	}

	private static ItemStack display(WorkbenchRecipes.Recipe recipe, Player player) {
		ItemStack stack = recipe.resultStack();
		List<Component> lines = new ArrayList<>();
		Item result = recipe.result().asItem();

		if (!Blueprints.knows(player, result)) {
			lines.add(Component.translatable("tooltip.solorust.locked").withStyle(ChatFormatting.RED));
			lines.add(Component.translatable("tooltip.solorust.research_at_table", Blueprints.researchCost(result)).withStyle(ChatFormatting.GRAY));
			stack.set(DataComponents.LORE, new ItemLore(lines));
			return stack;
		}

		lines.add(Component.translatable("tooltip.solorust.cost").withStyle(ChatFormatting.GOLD));

		for (WorkbenchRecipes.Cost cost : recipe.costs()) {
			lines.add(Component.literal(cost.count() + " × ").withStyle(ChatFormatting.GRAY)
					.append(Component.translatable(cost.item().getDescriptionId()).withStyle(ChatFormatting.WHITE)));
		}

		lines.add(Component.translatable("tooltip.solorust.click_to_craft").withStyle(ChatFormatting.DARK_GREEN));
		stack.set(DataComponents.LORE, new ItemLore(lines));
		return stack;
	}

	@Override
	public void clicked(int slotId, int clickData, ContainerInput containerInput, Player player) {
		if (slotId >= 0 && slotId < catalog.getContainerSize()) {
			if (slotId < recipes.size() && player instanceof ServerPlayer serverPlayer) {
				craft(serverPlayer, recipes.get(slotId));
			}

			// Клиент мог «предсказать», что взял витринный предмет, — откатываем это полной синхронизацией.
			sendAllDataToRemote();
			return;
		}

		super.clicked(slotId, clickData, containerInput, player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
		return slot.container != catalog && super.canTakeItemForPickAll(stack, slot);
	}

	@Override
	public boolean stillValid(Player player) {
		return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
	}

	private void craft(ServerPlayer player, WorkbenchRecipes.Recipe recipe) {
		Inventory inventory = player.getInventory();
		boolean free = player.hasInfiniteMaterials();

		if (!Blueprints.knows(player, recipe.result().asItem())) {
			player.sendSystemMessage(Component.translatable("message.solorust.need_blueprint").withStyle(ChatFormatting.RED), true);
			player.level().playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.5f, 1.0f);
			return;
		}

		if (!free) {
			for (WorkbenchRecipes.Cost cost : recipe.costs()) {
				if (count(inventory, cost) < cost.count()) {
					player.sendSystemMessage(Component.translatable("message.solorust.not_enough",
							Component.translatable(cost.item().getDescriptionId())).withStyle(ChatFormatting.RED), true);
					player.level().playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.5f, 1.0f);
					return;
				}
			}

			for (WorkbenchRecipes.Cost cost : recipe.costs()) {
				remove(inventory, cost);
			}
		}

		ItemStack result = recipe.resultStack();

		if (!inventory.add(result)) {
			player.drop(result, false);
		}

		player.level().playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.7f, 1.0f);
	}

	private static int count(Inventory inventory, WorkbenchRecipes.Cost cost) {
		int total = 0;

		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);

			if (stack.is(cost.item())) {
				total += stack.getCount();
			}
		}

		return total;
	}

	private static void remove(Inventory inventory, WorkbenchRecipes.Cost cost) {
		int left = cost.count();

		for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
			ItemStack stack = inventory.getItem(i);

			if (stack.is(cost.item())) {
				int take = Math.min(left, stack.getCount());
				stack.shrink(take);
				left -= take;
			}
		}
	}
}
