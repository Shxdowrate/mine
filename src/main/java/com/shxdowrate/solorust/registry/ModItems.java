package com.shxdowrate.solorust.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.shxdowrate.solorust.SoloRust;
import com.shxdowrate.solorust.item.GunItem;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import net.fabricmc.fabric.api.registry.FuelValueEvents;

public final class ModItems {
	public static final List<Item> ITEMS = new ArrayList<>();

	// --- Сырьё ---
	public static final Item SULFUR_ORE = simple("sulfur_ore");
	public static final Item SULFUR = simple("sulfur");
	public static final Item SCRAP = simple("scrap");
	public static final Item LOW_GRADE_FUEL = simple("low_grade_fuel");

	// --- Компоненты (только из лута) ---
	public static final Item ROPE = simple("rope");
	public static final Item TARP = simple("tarp");
	public static final Item SEWING_KIT = simple("sewing_kit");
	public static final Item METAL_BLADE = simple("metal_blade");
	public static final Item METAL_PIPE = simple("metal_pipe");
	public static final Item METAL_SPRING = simple("metal_spring");
	public static final Item GEARS = simple("gears");
	public static final Item ROAD_SIGNS = simple("road_signs");
	public static final Item SEMI_BODY = simple("semi_automatic_body");
	public static final Item SMG_BODY = simple("smg_body");
	public static final Item RIFLE_BODY = simple("rifle_body");
	public static final Item TECH_TRASH = simple("tech_trash");

	// --- Патроны ---
	public static final Item HANDMADE_SHELL = simple("handmade_shell");
	public static final Item PISTOL_AMMO = simple("pistol_ammo");
	public static final Item RIFLE_AMMO = simple("rifle_ammo");

	// --- Оружие ---
	public static final Item EOKA = gun("eoka_pistol", new GunItem.Stats(() -> ModItems.HANDMADE_SHELL, 6f, 30, 40, 4, 6.0f, 0.35f));
	public static final Item REVOLVER = gun("revolver", new GunItem.Stats(() -> ModItems.PISTOL_AMMO, 7f, 8, 64, 1, 1.5f, 0f));
	public static final Item SEMI_AUTO_RIFLE = gun("semi_automatic_rifle", new GunItem.Stats(() -> ModItems.RIFLE_AMMO, 9f, 6, 96, 1, 1.0f, 0f));
	public static final Item THOMPSON = gun("thompson", new GunItem.Stats(() -> ModItems.PISTOL_AMMO, 6f, 2, 64, 1, 2.0f, 0f));
	public static final Item ASSAULT_RIFLE = gun("assault_rifle", new GunItem.Stats(() -> ModItems.RIFLE_AMMO, 8f, 2, 112, 1, 1.8f, 0f));
	public static final Item BOLT_RIFLE = gun("bolt_action_rifle", new GunItem.Stats(() -> ModItems.RIFLE_AMMO, 22f, 32, 160, 1, 0.2f, 0f));

	private ModItems() {
	}

	private static Item simple(String name) {
		return register(name, Item::new, new Item.Properties());
	}

	private static Item gun(String name, GunItem.Stats stats) {
		return register(name, properties -> new GunItem(properties, stats), new Item.Properties().stacksTo(1));
	}

	private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SoloRust.id(name));
		T item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
		ITEMS.add(item);
		return item;
	}

	public static void init() {
		// Низкосортное топливо горит в печи как 2 угля.
		FuelValueEvents.BUILD.register((builder, context) -> builder.add(LOW_GRADE_FUEL, 3200));
	}
}
