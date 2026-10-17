package com.shxdowrate.solorust.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.shxdowrate.solorust.SoloRust;
import com.shxdowrate.solorust.block.NodeBlock;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
	/** Все блок-предметы мода, по порядку — для вкладки творческого режима. */
	public static final List<Item> BLOCK_ITEMS = new ArrayList<>();

	public static final Block STONE_NODE = register("stone_node", NodeBlock::new, node(MapColor.STONE));
	public static final Block METAL_NODE = register("metal_node", NodeBlock::new, node(MapColor.RAW_IRON));
	public static final Block SULFUR_NODE = register("sulfur_node", NodeBlock::new, node(MapColor.COLOR_YELLOW));

	private ModBlocks() {
	}

	private static BlockBehaviour.Properties node(MapColor color) {
		return BlockBehaviour.Properties.of()
				.mapColor(color)
				.strength(4.0f, 6.0f)
				.requiresCorrectToolForDrops()
				.sound(SoundType.STONE)
				.noOcclusion();
	}

	private static <T extends Block> T register(String name, Function<BlockBehaviour.Properties, T> factory, BlockBehaviour.Properties properties) {
		Identifier id = SoloRust.id(name);
		T block = factory.apply(properties.setId(ResourceKey.create(Registries.BLOCK, id)));
		Registry.register(BuiltInRegistries.BLOCK, id, block);

		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
		Item item = Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey)));
		BLOCK_ITEMS.add(item);
		return block;
	}

	public static void init() {
	}
}
