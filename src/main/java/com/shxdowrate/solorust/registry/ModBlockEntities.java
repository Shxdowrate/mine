package com.shxdowrate.solorust.registry;

import com.shxdowrate.solorust.SoloRust;
import com.shxdowrate.solorust.block.entity.LootCrateBlockEntity;
import com.shxdowrate.solorust.block.entity.RecyclerBlockEntity;
import com.shxdowrate.solorust.block.entity.SuitStationBlockEntity;
import com.shxdowrate.solorust.block.entity.TitanPodBlockEntity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

public final class ModBlockEntities {
	public static final BlockEntityType<LootCrateBlockEntity> LOOT_CRATE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SoloRust.id("loot_crate"),
			FabricBlockEntityTypeBuilder.create(LootCrateBlockEntity::new, ModBlocks.LOOT_CRATE, ModBlocks.MILITARY_CRATE, ModBlocks.SUPPLY_DROP).build());

	public static final BlockEntityType<RecyclerBlockEntity> RECYCLER = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SoloRust.id("recycler"),
			FabricBlockEntityTypeBuilder.create(RecyclerBlockEntity::new, ModBlocks.RECYCLER).build());

	public static final BlockEntityType<SuitStationBlockEntity> SUIT_STATION = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SoloRust.id("suit_station"),
			FabricBlockEntityTypeBuilder.create(SuitStationBlockEntity::new, ModBlocks.SUIT_STATION).build());

	public static final BlockEntityType<TitanPodBlockEntity> TITAN_POD = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SoloRust.id("titan_pod"),
			FabricBlockEntityTypeBuilder.create(TitanPodBlockEntity::new, ModBlocks.TITAN_POD).build());

	private ModBlockEntities() {
	}

	public static void init() {
	}
}
