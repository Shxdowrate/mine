package com.shxdowrate.solorust.registry;

import com.shxdowrate.solorust.SoloRust;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;

public final class ModWorldgen {
	private ModWorldgen() {
	}

	public static void init() {
		addSurface("stone_node");
		addSurface("metal_node");
		addSurface("sulfur_node");
	}

	private static void addSurface(String placedFeature) {
		BiomeModifications.addFeature(
				BiomeSelectors.foundInOverworld(),
				GenerationStep.Decoration.VEGETAL_DECORATION,
				ResourceKey.create(Registries.PLACED_FEATURE, SoloRust.id(placedFeature))
		);
	}
}
