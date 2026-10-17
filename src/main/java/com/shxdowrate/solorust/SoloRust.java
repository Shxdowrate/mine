package com.shxdowrate.solorust;

import com.shxdowrate.solorust.gun.ReloadSounds;
import com.shxdowrate.solorust.mechanics.Airdrops;
import com.shxdowrate.solorust.mechanics.Blueprints;
import com.shxdowrate.solorust.network.ModNetworking;
import com.shxdowrate.solorust.registry.ModBlockEntities;
import com.shxdowrate.solorust.registry.ModComponents;
import com.shxdowrate.solorust.registry.ModSounds;
import com.shxdowrate.solorust.registry.ModBlocks;
import com.shxdowrate.solorust.registry.ModCreativeTab;
import com.shxdowrate.solorust.registry.ModItems;
import com.shxdowrate.solorust.registry.ModSuits;
import com.shxdowrate.solorust.suit.SuitManager;
import com.shxdowrate.solorust.suit.SuitProjectiles;
import com.shxdowrate.solorust.suit.SuitAssembly;
import com.shxdowrate.solorust.suit.SuitActions;
import com.shxdowrate.solorust.suit.TitanDrop;
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
		ModComponents.init();
		ModSounds.init();
		ModBlocks.init();
		ModBlockEntities.init();
		ModItems.init();
		ModSuits.init();
		Blueprints.init();
		ModNetworking.init();
		ReloadSounds.init();
		Airdrops.init();
		ModCreativeTab.init();
		ModWorldgen.init();
		VeinMiner.init();
		SuitManager.init();
		SuitProjectiles.init();
		SuitAssembly.init();
		TitanDrop.init();
		SuitActions.init();

		LOGGER.info("Solo Rust loaded");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
