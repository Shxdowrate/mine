package com.shxdowrate.solorust;

import com.shxdowrate.solorust.registry.ModBlocks;
import com.shxdowrate.solorust.registry.ModCreativeTab;
import com.shxdowrate.solorust.registry.ModItems;
import com.shxdowrate.solorust.registry.ModWorldgen;
import com.shxdowrate.solorust.mechanics.VeinMiner;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoloRust implements ModInitializer {
	public static final String MOD_ID = "solorust";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.init();
		ModItems.init();
		ModCreativeTab.init();
		ModWorldgen.init();
		VeinMiner.init();

		LOGGER.info("Solo Rust loaded");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
