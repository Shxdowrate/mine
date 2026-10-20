package com.shxdowrate.solorust.suit;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorMaterial;

/**
 * Редкость костюма WARDEN (режим War Machines). От редкости зависят броня, энергия, скорость полёта,
 * бонусы и сила встроенного оружия — сами костюмы различаются раскраской и способностями.
 */
public enum WardenRarity {
	COMMON("common", 0xFFB0B0B0, Rarity.COMMON),
	UNCOMMON("uncommon", 0xFF5BD35B, Rarity.UNCOMMON),
	RARE("rare", 0xFF4DA6FF, Rarity.RARE),
	EPIC("epic", 0xFFB86BFF, Rarity.EPIC),
	LEGENDARY("legendary", 0xFFFFB13D, Rarity.EPIC),
	MYTHIC("mythic", 0xFFFF4F6D, Rarity.EPIC);

	public final String id;
	/** Цвет названия редкости и рамок в планшете (ARGB). */
	public final int color;
	public final Rarity itemRarity;

	WardenRarity(String id, int color, Rarity itemRarity) {
		this.id = id;
		this.color = color;
		this.itemRarity = itemRarity;
	}

	public String translationKey() {
		return "warden.solorust.rarity." + id;
	}

	// ------------------------------------------------------------------ характеристики по редкости

	ArmorMaterial armor() {
		return switch (this) {
			case COMMON -> SuitTier.armorOf(2, 5, 6, 2, 1.0f, 0.0f, SoundEvents.ARMOR_EQUIP_IRON);
			case UNCOMMON -> SuitTier.armorOf(3, 5, 7, 3, 1.5f, 0.05f, SoundEvents.ARMOR_EQUIP_IRON);
			case RARE -> SuitTier.armorOf(3, 6, 8, 3, 2.0f, 0.08f, SoundEvents.ARMOR_EQUIP_IRON);
			case EPIC -> SuitTier.armorOf(4, 7, 9, 4, 3.0f, 0.1f, SoundEvents.ARMOR_EQUIP_NETHERITE);
			case LEGENDARY -> SuitTier.armorOf(5, 8, 10, 5, 4.0f, 0.15f, SoundEvents.ARMOR_EQUIP_NETHERITE);
			case MYTHIC -> SuitTier.armorOf(5, 9, 11, 5, 5.0f, 0.2f, SoundEvents.ARMOR_EQUIP_NETHERITE);
		};
	}

	int maxEnergy() {
		return new int[] {600, 750, 900, 1100, 1400, 1800}[ordinal()];
	}

	int recharge() {
		return new int[] {6, 7, 8, 9, 11, 13}[ordinal()];
	}

	float flySpeed() {
		return new float[] {0.07f, 0.075f, 0.08f, 0.09f, 0.10f, 0.11f}[ordinal()];
	}

	float boost() {
		return new float[] {0.12f, 0.13f, 0.15f, 0.17f, 0.19f, 0.21f}[ordinal()];
	}

	float maxBoostSpeed() {
		return new float[] {2.4f, 2.6f, 2.8f, 3.0f, 3.2f, 3.4f}[ordinal()];
	}

	SuitTier.Stats stats() {
		return switch (this) {
			case COMMON -> new SuitTier.Stats(0.10, 2, 0.05, 0.0, 0.6, 0.0, 0, 24);
			case UNCOMMON -> new SuitTier.Stats(0.12, 3, 0.05, 0.0, 0.6, 0.0, 2, 24);
			case RARE -> new SuitTier.Stats(0.15, 3, 0.08, 0.0, 0.6, 0.5, 4, 32);
			case EPIC -> new SuitTier.Stats(0.18, 4, 0.10, 0.0, 0.6, 0.5, 6, 48);
			case LEGENDARY -> new SuitTier.Stats(0.22, 5, 0.12, 0.0, 0.6, 1.0, 10, 64);
			case MYTHIC -> new SuitTier.Stats(0.26, 6, 0.15, 0.0, 0.6, 1.0, 14, 96);
		};
	}

	float power() {
		return new float[] {1.0f, 1.1f, 1.25f, 1.45f, 1.7f, 2.0f}[ordinal()];
	}

	/** Дальность подсветки мобов шлемом. */
	public double helmetEntityRange() {
		return new double[] {24, 32, 40, 48, 56, 64}[ordinal()];
	}

	/** Радиус сканирования блоков шлемом. */
	public int helmetBlockRange() {
		return new int[] {8, 10, 12, 14, 18, 22}[ordinal()];
	}
}
