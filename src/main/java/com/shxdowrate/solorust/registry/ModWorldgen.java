package com.shxdowrate.solorust.registry;

import com.shxdowrate.solorust.SoloRust;
import com.shxdowrate.solorust.world.BoulderFeature;
import com.shxdowrate.solorust.world.RoadFeature;
import com.shxdowrate.solorust.world.RustBiomeSource;
import com.shxdowrate.solorust.world.RustSpawn;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;

public final class ModWorldgen {
	private ModWorldgen() {
	}

	public static void init() {
		Registry.register(BuiltInRegistries.BIOME_SOURCE, SoloRust.id("rust"), RustBiomeSource.CODEC);

		Registry.register(BuiltInRegistries.FEATURE, SoloRust.id("road"), new RoadFeature(NoneFeatureConfiguration.CODEC));
		Registry.register(BuiltInRegistries.FEATURE, SoloRust.id("stone_boulder"), new BoulderFeature(NoneFeatureConfiguration.CODEC, () -> ModBlocks.STONE_NODE));
		Registry.register(BuiltInRegistries.FEATURE, SoloRust.id("metal_boulder"), new BoulderFeature(NoneFeatureConfiguration.CODEC, () -> ModBlocks.METAL_NODE));
		Registry.register(BuiltInRegistries.FEATURE, SoloRust.id("sulfur_boulder"), new BoulderFeature(NoneFeatureConfiguration.CODEC, () -> ModBlocks.SULFUR_NODE));

		// Всё ниже срабатывает только в мирах типа «Rust» — проверка внутри самих фич.
		add("stone_boulder", GenerationStep.Decoration.VEGETAL_DECORATION);
		add("metal_boulder", GenerationStep.Decoration.VEGETAL_DECORATION);
		add("sulfur_boulder", GenerationStep.Decoration.VEGETAL_DECORATION);
		add("road", GenerationStep.Decoration.TOP_LAYER_MODIFICATION);

		RustSpawn.init();
	}

	private static void add(String placedFeature, GenerationStep.Decoration step) {
		BiomeModifications.addFeature(
				BiomeSelectors.foundInOverworld(),
				step,
				ResourceKey.create(Registries.PLACED_FEATURE, SoloRust.id(placedFeature))
		);
	}
}
