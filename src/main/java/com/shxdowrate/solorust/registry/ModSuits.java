package com.shxdowrate.solorust.registry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.shxdowrate.solorust.suit.SuitArmorItem;
import com.shxdowrate.solorust.suit.SuitContainerItem;
import com.shxdowrate.solorust.suit.SuitTier;
import com.shxdowrate.solorust.suit.SuitWeapon;
import com.shxdowrate.solorust.suit.SuitWeaponItem;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorType;

/** Предметы силовой брони: части костюмов всех поколений, встроенное оружие и энергоядро. */
public final class ModSuits {
	public static final Item POWER_CORE = ModItems.register("suit_power_core", Item::new, new Item.Properties().rarity(Rarity.RARE));

	/** Части брони: [поколение][тип]. */
	public static final Map<SuitTier, Map<ArmorType, SuitArmorItem>> ARMOR = new EnumMap<>(SuitTier.class);
	public static final Map<SuitWeapon, SuitWeaponItem> WEAPONS = new EnumMap<>(SuitWeapon.class);
	public static final List<SuitArmorItem> ALL_ARMOR = new ArrayList<>();
	public static final Map<SuitContainerItem.Kind, SuitContainerItem> CONTAINERS = new EnumMap<>(SuitContainerItem.Kind.class);

	private static final ArmorType[] PIECES = {ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS};

	static {
		for (SuitTier tier : SuitTier.values()) {
			Map<ArmorType, SuitArmorItem> pieces = new EnumMap<>(ArmorType.class);

			for (ArmorType type : PIECES) {
				Item.Properties properties = new Item.Properties()
						.humanoidArmor(tier.material, type)
						.component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
						.rarity(rarity(tier));

				if (tier.power >= 1.6f) {
					properties.fireResistant();
				}

				SuitArmorItem item = ModItems.register("suit_" + tier.id + "_" + type.getName(), p -> new SuitArmorItem(p, tier, type), properties);
				pieces.put(type, item);
				ALL_ARMOR.add(item);
			}

			ARMOR.put(tier, pieces);
		}

		for (SuitWeapon weapon : SuitWeapon.values()) {
			Item.Properties properties = new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant();

			if (weapon == SuitWeapon.NANO_BLADE) {
				// Нанолезвие — ещё и полноценный меч (ЛКМ), ПКМ — рывок с разрезом.
				properties.sword(ToolMaterial.NETHERITE, 8.0f, -2.0f).component(DataComponents.UNBREAKABLE, Unit.INSTANCE);
			}

			SuitWeaponItem item = ModItems.register(weapon.itemId(), p -> new SuitWeaponItem(p, weapon), properties);
			WEAPONS.put(weapon, item);
			// Встроенное оружие выдаёт сам костюм — в творческой вкладке его не показываем.
			ModItems.ITEMS.remove(item);
		}
	}

	public static final SuitContainerItem SUITCASE = container(SuitContainerItem.Kind.SUITCASE, "suitcase", Rarity.UNCOMMON);
	public static final SuitContainerItem NANO_CAPSULE = container(SuitContainerItem.Kind.NANO_CAPSULE, "nano_capsule", Rarity.EPIC);
	public static final SuitContainerItem TITAN_BEACON = container(SuitContainerItem.Kind.TITAN_BEACON, "titan_beacon", Rarity.EPIC);

	private static SuitContainerItem container(SuitContainerItem.Kind kind, String id, Rarity rarity) {
		SuitContainerItem item = ModItems.register(id, p -> new SuitContainerItem(p, kind), new Item.Properties().stacksTo(1).rarity(rarity).fireResistant());
		CONTAINERS.put(kind, item);
		return item;
	}

	private ModSuits() {
	}

	private static Rarity rarity(SuitTier tier) {
		if (tier.isWarden()) {
			return tier.warden.itemRarity;
		}

		return switch (tier) {
			case MK1, LIGHT -> Rarity.UNCOMMON;
			case MK2, MK3, MINER, BUILDER, DIVER, SCOUT -> Rarity.RARE;
			default -> Rarity.EPIC;
		};
	}

	public static SuitArmorItem piece(SuitTier tier, ArmorType type) {
		return ARMOR.get(tier).get(type);
	}

	public static void init() {
	}
}
