package com.shxdowrate.solorust.registry;

import com.shxdowrate.solorust.SoloRust;
import com.shxdowrate.solorust.item.GunItem;
import com.shxdowrate.solorust.suit.SuitArmorItem;
import com.shxdowrate.solorust.suit.SuitContainerItem;
import com.shxdowrate.solorust.suit.SuitTier;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

public final class ModCreativeTab {
	public static final ResourceKey<CreativeModeTab> TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, SoloRust.id("main"));
	public static final ResourceKey<CreativeModeTab> SUITS = ResourceKey.create(Registries.CREATIVE_MODE_TAB, SoloRust.id("suits"));

	private ModCreativeTab() {
	}

	public static void init() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB, FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.solorust.main"))
				.icon(() -> new ItemStack(ModBlocks.SULFUR_NODE))
				.displayItems((context, entries) -> {
					ModBlocks.BLOCK_ITEMS.stream().filter(i -> i != ModBlocks.item(ModBlocks.SUIT_STATION)).forEach(entries::accept);
					for (var item : ModItems.ITEMS) {
						if (item instanceof SuitArmorItem || item instanceof SuitContainerItem || item == ModSuits.POWER_CORE) {
							continue;
						}

						ItemStack stack = new ItemStack(item);

						// В творческом оружие сразу заряжено.
						if (item instanceof GunItem gun) {
							stack.set(ModComponents.AMMO, gun.def().magazine());
						}

						entries.accept(stack);
					}
				})
				.build());

		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SUITS, FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.solorust.suits"))
				.icon(() -> new ItemStack(ModSuits.piece(SuitTier.MK3, ArmorType.HELMET)))
				.displayItems((context, entries) -> {
					entries.accept(ModSuits.POWER_CORE);
					entries.accept(ModBlocks.item(ModBlocks.SUIT_STATION));
					ModSuits.CONTAINERS.values().forEach(entries::accept);
					ModSuits.ALL_ARMOR.forEach(entries::accept);
				})
				.build());
	}
}
