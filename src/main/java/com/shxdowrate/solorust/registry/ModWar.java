package com.shxdowrate.solorust.registry;

import com.shxdowrate.solorust.SoloRust;
import com.shxdowrate.solorust.suit.SuitTier;
import com.shxdowrate.solorust.war.Bandit;
import com.shxdowrate.solorust.war.BanditCamps;
import com.shxdowrate.solorust.war.BanditRockets;
import com.shxdowrate.solorust.war.WarBiomeSource;
import com.shxdowrate.solorust.war.WarData;
import com.shxdowrate.solorust.war.WarRewards;
import com.shxdowrate.solorust.war.WardenTabletItem;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.equipment.ArmorType;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

/** War Machines: бандиты, планшет WARDEN и своя вкладка творческого режима с костюмами. */
public final class ModWar {
	public static final ResourceKey<EntityType<?>> BANDIT_KEY = ResourceKey.create(Registries.ENTITY_TYPE, SoloRust.id("bandit"));
	public static final EntityType<Bandit> BANDIT = Registry.register(BuiltInRegistries.ENTITY_TYPE, BANDIT_KEY,
			EntityType.Builder.<Bandit>of(Bandit::new, MobCategory.MONSTER)
					.sized(0.6f, 1.95f)
					.eyeHeight(1.62f)
					.clientTrackingRange(10)
					.build(BANDIT_KEY));

	public static final WardenTabletItem WARDEN_TABLET = ModItems.register("warden_tablet", WardenTabletItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
	public static final SpawnEggItem BANDIT_SPAWN_EGG = ModItems.register("bandit_spawn_egg", SpawnEggItem::new,
			new Item.Properties().spawnEgg(BANDIT));

	public static final ResourceKey<CreativeModeTab> TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, SoloRust.id("war_machines"));

	static {
		// Предметы War Machines — только в своей вкладке.
		ModItems.ITEMS.remove(WARDEN_TABLET);
		ModItems.ITEMS.remove(BANDIT_SPAWN_EGG);
	}

	private ModWar() {
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(BANDIT, Bandit.createAttributes());
		Registry.register(BuiltInRegistries.BIOME_SOURCE, SoloRust.id("war"), WarBiomeSource.CODEC);

		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB, FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.solorust.war_machines"))
				.icon(() -> new ItemStack(ModSuits.piece(SuitTier.W_ARSENAL, ArmorType.HELMET)))
				.displayItems((context, entries) -> {
					entries.accept(WARDEN_TABLET);
					entries.accept(BANDIT_SPAWN_EGG);

					for (SuitTier tier : SuitTier.wardenSuits()) {
						for (ArmorType type : new ArmorType[] {ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS}) {
							entries.accept(ModSuits.piece(tier, type));
						}
					}
				})
				.build());

		WarData.init();
		WarRewards.init();
		BanditCamps.init();
		BanditRockets.init();
		com.shxdowrate.solorust.suit.WardenWeapons.initDecoys();
	}
}
