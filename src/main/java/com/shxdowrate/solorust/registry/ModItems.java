package com.shxdowrate.solorust.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.shxdowrate.solorust.SoloRust;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class ModItems {
	public static final List<Item> ITEMS = new ArrayList<>();

	/** Сырая сера с камня — переплавляется в печи. */
	public static final Item SULFUR_ORE = register("sulfur_ore", Item::new, new Item.Properties());
	/** Чистая сера — идёт в порох. */
	public static final Item SULFUR = register("sulfur", Item::new, new Item.Properties());

	private ModItems() {
	}

	private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SoloRust.id(name));
		T item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
		ITEMS.add(item);
		return item;
	}

	public static void init() {
	}
}
