package com.shxdowrate.solorust.magic;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.shxdowrate.solorust.SoloRust;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/** Перчатка бесконечности, камни и кольцо Стрэнджа. */
public final class MagicItems {
	public static final GauntletItem INFINITY_GAUNTLET = register("infinity_gauntlet", GauntletItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
	public static final SorcererRingItem SORCERER_RING = register("sorcerer_ring", SorcererRingItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
	public static final Map<InfinityStone, InfinityStoneItem> STONES = new EnumMap<>(InfinityStone.class);

	static {
		for (InfinityStone stone : InfinityStone.values()) {
			STONES.put(stone, register(stone.id, properties -> new InfinityStoneItem(properties, stone),
					new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
		}
	}

	private MagicItems() {
	}

	private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SoloRust.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	/** Что показать во вкладке творческого режима. */
	public static List<ItemStack> creativeStacks() {
		ItemStack empty = new ItemStack(INFINITY_GAUNTLET);
		GauntletItem.setStones(empty, 0);
		List<ItemStack> list = new java.util.ArrayList<>(List.of(GauntletItem.full(INFINITY_GAUNTLET), empty));

		for (InfinityStone stone : InfinityStone.values()) {
			list.add(new ItemStack(STONES.get(stone)));
		}

		list.add(new ItemStack(SORCERER_RING));
		return list;
	}

	public static void init() {
		MagicState.init();
	}
}
