package com.shxdowrate.solorust.registry;

import com.shxdowrate.solorust.SoloRust;
import com.shxdowrate.solorust.item.GunItem;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

public final class ModCreativeTab {
	public static final ResourceKey<CreativeModeTab> TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, SoloRust.id("main"));

	private ModCreativeTab() {
	}

	public static void init() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB, FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.solorust.main"))
				.icon(() -> new ItemStack(ModBlocks.SULFUR_NODE))
				.displayItems((context, entries) -> {
					ModBlocks.BLOCK_ITEMS.forEach(entries::accept);
					for (var item : ModItems.ITEMS) {
						ItemStack stack = new ItemStack(item);

						// В творческом оружие сразу заряжено.
						if (item instanceof GunItem gun) {
							stack.set(ModComponents.AMMO, gun.def().magazine());
						}

						entries.accept(stack);
					}
				})
				.build());
	}
}
