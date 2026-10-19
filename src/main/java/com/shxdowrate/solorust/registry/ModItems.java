package com.shxdowrate.solorust.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.shxdowrate.solorust.SoloRust;
import com.shxdowrate.solorust.gun.Attachment;
import com.shxdowrate.solorust.gun.GunDef;
import com.shxdowrate.solorust.item.AttachmentItem;
import com.shxdowrate.solorust.item.GunItem;
import com.shxdowrate.solorust.item.SupplySignalItem;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import net.fabricmc.fabric.api.registry.FuelValueEvents;

import static com.shxdowrate.solorust.gun.AmmoClass.EOKA;
import static com.shxdowrate.solorust.gun.AmmoClass.NAIL;
import static com.shxdowrate.solorust.gun.AmmoClass.PISTOL;
import static com.shxdowrate.solorust.gun.AmmoClass.RIFLE;
import static com.shxdowrate.solorust.gun.AmmoClass.SHOTGUN;

public final class ModItems {
	public static final List<Item> ITEMS = new ArrayList<>();
	public static final List<GunItem> GUNS = new ArrayList<>();
	public static final List<AttachmentItem> ATTACHMENTS = new ArrayList<>();

	// --- Сырьё ---
	public static final Item SULFUR_ORE = simple("sulfur_ore");
	public static final Item SULFUR = simple("sulfur");
	public static final Item SCRAP = simple("scrap");
	public static final Item LOW_GRADE_FUEL = simple("low_grade_fuel");

	// --- Компоненты (только из лута) ---
	public static final Item ROPE = simple("rope");
	public static final Item TARP = simple("tarp");
	public static final Item SEWING_KIT = simple("sewing_kit");
	public static final Item METAL_BLADE = simple("metal_blade");
	public static final Item METAL_PIPE = simple("metal_pipe");
	public static final Item METAL_SPRING = simple("metal_spring");
	public static final Item GEARS = simple("gears");
	public static final Item ROAD_SIGNS = simple("road_signs");
	public static final Item SEMI_BODY = simple("semi_automatic_body");
	public static final Item SMG_BODY = simple("smg_body");
	public static final Item RIFLE_BODY = simple("rifle_body");
	public static final Item TECH_TRASH = simple("tech_trash");

	// --- Патроны ---
	public static final Item HANDMADE_SHELL = simple("handmade_shell");
	public static final Item BUCKSHOT = simple("buckshot");
	public static final Item SLUG = simple("slug");
	public static final Item PISTOL_AMMO = simple("pistol_ammo");
	public static final Item RIFLE_AMMO = simple("rifle_ammo");
	public static final Item NAILS = simple("nails");

	// --- Пистолеты ---
	//                                         id                tier ammo  int  dmg   hip   ads   rec  range mag reload zoom mods
	public static final GunItem EOKA_PISTOL = gun(GunDef.semi("eoka_pistol", 0, EOKA, 20, 1.0f, 4.0f, 3.0f, 5.0f, 30, 1, 30, 0.9f, false).withMisfire(0.35f));
	public static final GunItem NAILGUN = gun(GunDef.semi("nailgun", 1, NAIL, 3, 3.0f, 1.6f, 1.0f, 1.5f, 40, 16, 40, 0.9f, false));
	public static final GunItem REVOLVER = gun(GunDef.semi("revolver", 1, PISTOL, 4, 6.5f, 1.6f, 0.8f, 2.5f, 60, 8, 50, 0.85f, true));
	public static final GunItem P250 = gun(GunDef.semi("p250", 2, PISTOL, 3, 7.0f, 1.4f, 0.6f, 2.6f, 64, 10, 45, 0.85f, true));
	public static final GunItem PYTHON = gun(GunDef.semi("python", 2, PISTOL, 6, 10.0f, 1.6f, 0.6f, 4.5f, 70, 6, 60, 0.85f, true));
	public static final GunItem M92 = gun(GunDef.semi("m92", 3, PISTOL, 3, 8.0f, 1.2f, 0.5f, 2.6f, 70, 15, 45, 0.85f, true));
	public static final GunItem PROTOTYPE_17 = gun(GunDef.semi("prototype_17", 3, PISTOL, 2, 6.0f, 1.4f, 0.7f, 2.0f, 60, 21, 40, 0.85f, true));

	// --- Пистолеты-пулемёты ---
	public static final GunItem CUSTOM_SMG = gun(GunDef.auto("custom_smg", 1, PISTOL, 2, 5.0f, 2.2f, 1.2f, 1.6f, 64, 24, 60, 0.8f));
	public static final GunItem THOMPSON = gun(GunDef.auto("thompson", 2, PISTOL, 3, 6.5f, 1.8f, 0.9f, 1.8f, 72, 20, 60, 0.8f));
	public static final GunItem MP5 = gun(GunDef.auto("mp5", 3, PISTOL, 2, 6.0f, 1.6f, 0.7f, 1.4f, 80, 30, 60, 0.8f));

	// --- Винтовки ---
	public static final GunItem SEMI_AUTO_RIFLE = gun(GunDef.semi("semi_automatic_rifle", 2, RIFLE, 4, 7.5f, 1.5f, 0.4f, 2.4f, 120, 16, 60, 0.75f, true));
	public static final GunItem M39 = gun(GunDef.semi("m39_rifle", 3, RIFLE, 4, 9.0f, 1.4f, 0.3f, 2.6f, 140, 20, 60, 0.75f, true));
	public static final GunItem ASSAULT_RIFLE = gun(GunDef.auto("assault_rifle", 3, RIFLE, 3, 9.0f, 2.0f, 0.6f, 2.8f, 120, 30, 70, 0.75f));
	public static final GunItem LR300 = gun(GunDef.auto("lr300", 3, RIFLE, 2, 7.5f, 1.7f, 0.5f, 1.8f, 120, 30, 70, 0.75f));
	public static final GunItem BOLT_RIFLE = gun(GunDef.semi("bolt_action_rifle", 3, RIFLE, 34, 18.0f, 3.0f, 0.05f, 7.0f, 200, 4, 90, 0.6f, true));
	public static final GunItem L96 = gun(GunDef.semi("l96_rifle", -1, RIFLE, 46, 22.0f, 3.0f, 0.03f, 7.5f, 240, 5, 100, 0.6f, true));
	public static final GunItem M249 = gun(GunDef.auto("m249", -1, RIFLE, 2, 8.0f, 2.4f, 1.0f, 2.2f, 120, 100, 140, 0.8f));

	// --- Дробовики ---
	public static final GunItem WATERPIPE = gun(GunDef.semi("waterpipe_shotgun", 1, SHOTGUN, 20, 1.0f, 3.5f, 3.0f, 7.0f, 35, 1, 50, 0.9f, false));
	public static final GunItem DOUBLE_BARREL = gun(GunDef.semi("double_barrel_shotgun", 1, SHOTGUN, 6, 1.0f, 3.2f, 2.8f, 7.0f, 35, 2, 60, 0.9f, true));
	public static final GunItem PUMP_SHOTGUN = gun(GunDef.semi("pump_shotgun", 2, SHOTGUN, 18, 1.1f, 3.0f, 2.5f, 6.0f, 40, 6, 90, 0.85f, true));
	public static final GunItem SPAS12 = gun(GunDef.semi("spas12_shotgun", 3, SHOTGUN, 7, 0.9f, 3.0f, 2.5f, 5.0f, 40, 6, 90, 0.85f, true));
	public static final GunItem M4_SHOTGUN = gun(GunDef.semi("m4_shotgun", -1, SHOTGUN, 6, 0.95f, 2.8f, 2.3f, 5.0f, 42, 8, 100, 0.85f, true));

	// --- Модули ---
	public static final AttachmentItem HOLOSIGHT = attachment(Attachment.HOLOSIGHT);
	public static final AttachmentItem SIMPLE_SIGHT = attachment(Attachment.SIMPLE_SIGHT);
	public static final AttachmentItem SCOPE_8X = attachment(Attachment.SCOPE_8X);
	public static final AttachmentItem SCOPE_16X = attachment(Attachment.SCOPE_16X);
	public static final AttachmentItem SILENCER = attachment(Attachment.SILENCER);
	public static final AttachmentItem MUZZLE_BOOST = attachment(Attachment.MUZZLE_BOOST);
	public static final AttachmentItem MUZZLE_BRAKE = attachment(Attachment.MUZZLE_BRAKE);
	public static final AttachmentItem LASER_SIGHT = attachment(Attachment.LASER_SIGHT);
	public static final AttachmentItem EXTENDED_MAG = attachment(Attachment.EXTENDED_MAG);

	// --- Разное ---
	public static final Item SUPPLY_SIGNAL = register("supply_signal", SupplySignalItem::new, new Item.Properties().stacksTo(4));

	private ModItems() {
	}

	private static Item simple(String name) {
		return register(name, Item::new, new Item.Properties());
	}

	private static GunItem gun(GunDef def) {
		GunItem item = register(def.id(), properties -> new GunItem(properties, def), new Item.Properties().stacksTo(1));
		GUNS.add(item);
		return item;
	}

	private static AttachmentItem attachment(Attachment attachment) {
		AttachmentItem item = register(attachment.id, properties -> new AttachmentItem(properties, attachment), new Item.Properties().stacksTo(1));
		ATTACHMENTS.add(item);
		return item;
	}

	public static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SoloRust.id(name));
		T item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
		ITEMS.add(item);
		return item;
	}

	public static void init() {
		// Низкосортное топливо горит в печи как 2 угля.
		FuelValueEvents.BUILD.register((builder, context) -> builder.add(LOW_GRADE_FUEL, 3200));
	}
}
