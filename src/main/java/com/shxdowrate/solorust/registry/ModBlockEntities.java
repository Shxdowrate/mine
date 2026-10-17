package com.shxdowrate.solorust.registry;

import com.shxdowrate.solorust.SoloRust;
import com.shxdowrate.solorust.block.entity.LootCrateBlockEntity;
import com.shxdowrate.solorust.block.entity.RecyclerBlockEntity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

public final class ModBlockEntities {
	public static final BlockEntityType<LootCrateBlockEntity> LOOT_CRATE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SoloRust.id("loot_crate"),
			FabricBlockEntityTypeBuilder.create(LootCrateBlockEntity::new, ModBlocks.LOOT_CRATE, ModBlocks.MILITARY_CRATE).build());

	public static final BlockEntityType<RecyclerBlockEntity> RECYCLER = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SoloRust.id("recycler"),
			FabricBlockEntityTypeBuilder.create(RecyclerBlockEntity::new, ModBlocks.RECYCLER).build());

	private ModBlockEntities() {
	}

	public static void init() {
	}
}
