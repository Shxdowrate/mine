package com.shxdowrate.solorust.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.shxdowrate.solorust.SoloRust;
import com.shxdowrate.solorust.block.LootBarrelBlock;
import com.shxdowrate.solorust.block.LootCrateBlock;
import com.shxdowrate.solorust.block.NodeBlock;
import com.shxdowrate.solorust.block.RecyclerBlock;
import com.shxdowrate.solorust.block.ResearchTableBlock;
import com.shxdowrate.solorust.block.WorkbenchBlock;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import com.shxdowrate.solorust.block.SuitStationBlock;
import com.shxdowrate.solorust.block.TitanPodBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
	/** Все блок-предметы мода, по порядку — для вкладки творческого режима. */
	public static final List<Item> BLOCK_ITEMS = new ArrayList<>();

	public static final Block STONE_NODE = register("stone_node", NodeBlock::new, node(MapColor.STONE));
	public static final Block METAL_NODE = register("metal_node", NodeBlock::new, node(MapColor.RAW_IRON));
	public static final Block SULFUR_NODE = register("sulfur_node", NodeBlock::new, node(MapColor.COLOR_YELLOW));

	public static final Block LOOT_BARREL = register("loot_barrel", LootBarrelBlock::new, barrel(MapColor.COLOR_BLUE));
	public static final Block OIL_BARREL = register("oil_barrel", LootBarrelBlock::new, barrel(MapColor.COLOR_RED));
	public static final Block LOOT_CRATE = register("loot_crate", LootCrateBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD));
	public static final Block MILITARY_CRATE = register("military_crate", LootCrateBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_GREEN).strength(3.0f).sound(SoundType.METAL));

	public static final Block SUPPLY_DROP = register("supply_drop", LootCrateBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_BLUE).strength(3.0f).sound(SoundType.METAL));
	public static final Block RESEARCH_TABLE = register("research_table", ResearchTableBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL).strength(2.5f).sound(SoundType.METAL));

	public static final Block WORKBENCH_1 = register("workbench_1", p -> new WorkbenchBlock(p, 1), BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD));
	public static final Block WORKBENCH_2 = register("workbench_2", p -> new WorkbenchBlock(p, 2), BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL).strength(3.5f).sound(SoundType.METAL));
	public static final Block WORKBENCH_3 = register("workbench_3", p -> new WorkbenchBlock(p, 3), BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL).strength(4.5f).sound(SoundType.METAL));
	public static final Block RECYCLER = register("recycler", RecyclerBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_YELLOW).strength(4.0f).requiresCorrectToolForDrops().sound(SoundType.METAL));

	// --- Силовая броня ---
	public static final Block SUIT_STATION = register("suit_station", SuitStationBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL).strength(5.0f, 12.0f).requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK));
	public static final Block TITAN_POD = register("titan_pod", TitanPodBlock::new, BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_ORANGE).strength(4.0f, 30.0f).sound(SoundType.NETHERITE_BLOCK).lightLevel(state -> 6));

	static {
		// Капсулу нельзя поставить руками — она падает только по маяку.
		BLOCK_ITEMS.remove(BuiltInRegistries.ITEM.getValue(SoloRust.id("titan_pod")));
	}

	private ModBlocks() {
	}

	private static BlockBehaviour.Properties node(MapColor color) {
		return BlockBehaviour.Properties.of()
				.mapColor(color)
				.strength(3.0f, 6.0f)
				.requiresCorrectToolForDrops()
				.sound(SoundType.STONE);
	}

	private static BlockBehaviour.Properties barrel(MapColor color) {
		return BlockBehaviour.Properties.of()
				.mapColor(color)
				.strength(1.0f)
				.sound(SoundType.METAL)
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

	/** Предмет-блок по блоку (через реестр, не зависит от Item.BY_BLOCK). */
	public static Item item(Block block) {
		return BuiltInRegistries.ITEM.getValue(BuiltInRegistries.BLOCK.getKey(block));
	}

	public static void init() {
	}
}
