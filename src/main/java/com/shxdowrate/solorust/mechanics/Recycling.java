package com.shxdowrate.solorust.mechanics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.shxdowrate.solorust.registry.ModItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Что во что разбирает переработчик. */
public final class Recycling {
	public record Output(Item item, int count) {
		public ItemStack stack() {
			return new ItemStack(item, count);
		}
	}

	private static Map<Item, List<Output>> table;

	private Recycling() {
	}

	public static List<Output> get(ItemStack stack) {
		if (table == null) {
			build();
		}

		return table.get(stack.getItem());
	}

	private static void build() {
		Map<Item, List<Output>> map = new HashMap<>();
		map.put(ModItems.GEARS, List.of(new Output(ModItems.SCRAP, 10), new Output(Items.IRON_NUGGET, 9)));
		map.put(ModItems.METAL_SPRING, List.of(new Output(ModItems.SCRAP, 10), new Output(Items.IRON_INGOT, 1)));
		map.put(ModItems.METAL_PIPE, List.of(new Output(ModItems.SCRAP, 5), new Output(Items.IRON_INGOT, 1)));
		map.put(ModItems.METAL_BLADE, List.of(new Output(ModItems.SCRAP, 2), new Output(Items.IRON_NUGGET, 5)));
		map.put(ModItems.ROAD_SIGNS, List.of(new Output(ModItems.SCRAP, 5), new Output(Items.IRON_NUGGET, 6)));
		map.put(ModItems.ROPE, List.of(new Output(ModItems.SCRAP, 1), new Output(Items.STRING, 3)));
		map.put(ModItems.TARP, List.of(new Output(ModItems.SCRAP, 1), new Output(Items.STRING, 4), new Output(Items.LEATHER, 1)));
		map.put(ModItems.SEWING_KIT, List.of(new Output(ModItems.SCRAP, 1), new Output(Items.STRING, 4)));
		map.put(ModItems.SEMI_BODY, List.of(new Output(ModItems.SCRAP, 15), new Output(Items.IRON_INGOT, 2)));
		map.put(ModItems.SMG_BODY, List.of(new Output(ModItems.SCRAP, 15), new Output(Items.IRON_INGOT, 2)));
		map.put(ModItems.RIFLE_BODY, List.of(new Output(ModItems.SCRAP, 25), new Output(Items.IRON_INGOT, 3)));
		map.put(ModItems.TECH_TRASH, List.of(new Output(ModItems.SCRAP, 20), new Output(Items.GOLD_NUGGET, 4), new Output(Items.REDSTONE, 2)));
		table = map;
	}
}
