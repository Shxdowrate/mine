package com.shxdowrate.solorust.mechanics;

import java.util.ArrayList;
import java.util.List;

import com.shxdowrate.solorust.registry.ModBlocks;
import com.shxdowrate.solorust.registry.ModItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/** Рецепты верстаков. Верстак уровня N показывает всё, что требует уровень N и ниже. */
public final class WorkbenchRecipes {
	public record Cost(Item item, int count) {
	}

	public record Recipe(int tier, ItemLike result, int count, List<Cost> costs) {
		public ItemStack resultStack() {
			return new ItemStack(result, count);
		}
	}

	private static List<Recipe> all;

	private WorkbenchRecipes() {
	}

	public static List<Recipe> forTier(int tier) {
		if (all == null) {
			build();
		}

		List<Recipe> list = new ArrayList<>();

		for (Recipe recipe : all) {
			if (recipe.tier() <= tier) {
				list.add(recipe);
			}
		}

		return list;
	}

	private static Recipe r(int tier, ItemLike result, int count, Object... costs) {
		List<Cost> list = new ArrayList<>();

		for (int i = 0; i < costs.length; i += 2) {
			list.add(new Cost(((ItemLike) costs[i]).asItem(), (Integer) costs[i + 1]));
		}

		return new Recipe(tier, result, count, list);
	}

	private static void build() {
		List<Recipe> l = new ArrayList<>();

		// ===== Уровень 1 =====
		l.add(r(1, ModItems.HANDMADE_SHELL, 4, Items.GUNPOWDER, 1, Items.COBBLESTONE, 2));
		l.add(r(1, ModItems.PISTOL_AMMO, 4, Items.GUNPOWDER, 2, Items.IRON_NUGGET, 3));
		l.add(r(1, ModItems.BUCKSHOT, 4, Items.GUNPOWDER, 3, Items.IRON_NUGGET, 4));
		l.add(r(1, ModItems.NAILS, 8, Items.IRON_NUGGET, 4));
		l.add(r(1, ModItems.WATERPIPE, 1, ModItems.METAL_PIPE, 1, Items.IRON_INGOT, 2, ModItems.SCRAP, 5));
		l.add(r(1, ModItems.DOUBLE_BARREL, 1, ModItems.METAL_PIPE, 2, Items.IRON_INGOT, 4, ModItems.SCRAP, 10));
		l.add(r(1, ModItems.NAILGUN, 1, Items.IRON_INGOT, 3, ModItems.SCRAP, 15));
		l.add(r(1, ModItems.REVOLVER, 1, ModItems.METAL_PIPE, 1, ModItems.METAL_SPRING, 1, Items.IRON_INGOT, 4));
		l.add(r(1, ModItems.CUSTOM_SMG, 1, ModItems.SMG_BODY, 1, ModItems.METAL_SPRING, 1, Items.IRON_INGOT, 6));
		l.add(r(1, ModItems.SIMPLE_SIGHT, 1, Items.IRON_INGOT, 2, ModItems.SCRAP, 10));
		l.add(r(1, ModBlocks.WORKBENCH_2, 1, ModBlocks.WORKBENCH_1, 1, ModItems.SCRAP, 50, Items.IRON_INGOT, 16));

		// ===== Уровень 2 =====
		l.add(r(2, ModItems.RIFLE_AMMO, 3, Items.GUNPOWDER, 3, Items.IRON_NUGGET, 4));
		l.add(r(2, ModItems.SLUG, 2, Items.GUNPOWDER, 2, Items.IRON_NUGGET, 3));
		l.add(r(2, ModItems.P250, 1, ModItems.SEMI_BODY, 1, Items.IRON_INGOT, 5));
		l.add(r(2, ModItems.PYTHON, 1, ModItems.METAL_PIPE, 1, ModItems.METAL_SPRING, 1, Items.IRON_INGOT, 8, Items.GOLD_INGOT, 1));
		l.add(r(2, ModItems.THOMPSON, 1, ModItems.SMG_BODY, 1, ModItems.METAL_SPRING, 1, ModItems.METAL_PIPE, 1, Items.IRON_INGOT, 4));
		l.add(r(2, ModItems.PUMP_SHOTGUN, 1, ModItems.METAL_PIPE, 2, ModItems.METAL_SPRING, 1, Items.IRON_INGOT, 8));
		l.add(r(2, ModItems.SEMI_AUTO_RIFLE, 1, ModItems.SEMI_BODY, 1, ModItems.METAL_SPRING, 1, Items.IRON_INGOT, 6));
		l.add(r(2, ModItems.HOLOSIGHT, 1, ModItems.TECH_TRASH, 1, Items.GLASS, 2, Items.IRON_INGOT, 4));
		l.add(r(2, ModItems.SILENCER, 1, ModItems.METAL_PIPE, 1, Items.IRON_INGOT, 4));
		l.add(r(2, ModItems.MUZZLE_BOOST, 1, ModItems.METAL_PIPE, 1, ModItems.GEARS, 1, Items.IRON_INGOT, 4));
		l.add(r(2, ModItems.MUZZLE_BRAKE, 1, ModItems.METAL_PIPE, 1, Items.IRON_INGOT, 5));
		l.add(r(2, ModItems.LASER_SIGHT, 1, ModItems.TECH_TRASH, 1, Items.REDSTONE, 4, Items.IRON_INGOT, 2));
		l.add(r(2, ModItems.EXTENDED_MAG, 1, ModItems.METAL_SPRING, 1, Items.IRON_INGOT, 6));
		l.add(r(2, ModBlocks.RECYCLER, 1, ModItems.SCRAP, 40, Items.IRON_INGOT, 12, ModItems.GEARS, 3));
		l.add(r(2, ModBlocks.WORKBENCH_3, 1, ModBlocks.WORKBENCH_2, 1, ModItems.SCRAP, 150, Items.IRON_INGOT, 32, Items.GOLD_INGOT, 4));

		// ===== Уровень 3 =====
		l.add(r(3, ModItems.M92, 1, ModItems.SEMI_BODY, 1, ModItems.METAL_SPRING, 1, Items.GOLD_INGOT, 2, Items.IRON_INGOT, 6));
		l.add(r(3, ModItems.PROTOTYPE_17, 1, ModItems.SEMI_BODY, 1, ModItems.TECH_TRASH, 1, Items.GOLD_INGOT, 2));
		l.add(r(3, ModItems.MP5, 1, ModItems.SMG_BODY, 1, ModItems.METAL_SPRING, 1, Items.GOLD_INGOT, 3, Items.IRON_INGOT, 6));
		l.add(r(3, ModItems.ASSAULT_RIFLE, 1, ModItems.RIFLE_BODY, 1, ModItems.METAL_SPRING, 1, Items.GOLD_INGOT, 4, Items.IRON_INGOT, 8));
		l.add(r(3, ModItems.LR300, 1, ModItems.RIFLE_BODY, 1, ModItems.METAL_SPRING, 1, ModItems.TECH_TRASH, 1, Items.GOLD_INGOT, 6));
		l.add(r(3, ModItems.M39, 1, ModItems.RIFLE_BODY, 1, ModItems.SEMI_BODY, 1, Items.GOLD_INGOT, 4));
		l.add(r(3, ModItems.BOLT_RIFLE, 1, ModItems.RIFLE_BODY, 1, ModItems.METAL_PIPE, 3, ModItems.METAL_SPRING, 1, Items.GOLD_INGOT, 3));
		l.add(r(3, ModItems.SPAS12, 1, ModItems.METAL_PIPE, 2, ModItems.METAL_SPRING, 2, Items.GOLD_INGOT, 4, Items.IRON_INGOT, 6));
		l.add(r(3, ModItems.SCOPE_8X, 1, ModItems.TECH_TRASH, 2, Items.GLASS, 4, Items.IRON_INGOT, 6));
		l.add(r(3, ModItems.SCOPE_16X, 1, ModItems.TECH_TRASH, 3, Items.GLASS, 6, Items.GOLD_INGOT, 4));
		l.add(r(3, ModItems.SUPPLY_SIGNAL, 1, ModItems.TECH_TRASH, 2, ModItems.SCRAP, 60, Items.GUNPOWDER, 10, Items.RED_DYE, 2));

		all = l;
	}
}
