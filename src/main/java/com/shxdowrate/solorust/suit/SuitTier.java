package com.shxdowrate.solorust.suit;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.shxdowrate.solorust.SoloRust;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import static com.shxdowrate.solorust.suit.SuitWeapon.*;

/**
 * Поколения силовой брони. Полный комплект из четырёх частей одного поколения включает системы костюма:
 * энергию, полёт (или прыжковые двигатели), встроенное оружие и бонусы к характеристикам.
 */
public enum SuitTier {
	// Собран из хлама в пещере: тяжёлый, не летает, но прыгает на ракетных ускорителях.
	MK1("mk1", 0xFFE08A3C,
			armor(3, 6, 7, 3, 2.0f, 0.15f, SoundEvents.ARMOR_EQUIP_IRON),
			400, 3, false, 0f, 0f, 0f, 0,
			List.of(FLAMETHROWER, WRIST_ROCKET),
			new Stats(-0.15, 3, 0.12, 0.0, 0.0, 0.0, 0, 8),
			1.0f),

	// Первый летающий прототип: быстрый, но лёгкий и почти без вооружения.
	MK2("mk2", 0xFF9FE6FF,
			armor(3, 6, 7, 3, 2.0f, 0.05f, SoundEvents.ARMOR_EQUIP_IRON),
			700, 5, true, 0.06f, 0.10f, 1.6f, 2,
			List.of(REPULSOR, SCANNER),
			new Stats(0.10, 2, 0.0, 0.0, 0.5, 0.0, 0, 16),
			1.0f),

	// Боевая модель: полный арсенал.
	MK3("mk3", 0xFF4FD8FF,
			armor(4, 7, 9, 4, 3.0f, 0.10f, SoundEvents.ARMOR_EQUIP_NETHERITE),
			1200, 7, true, 0.08f, 0.14f, 2.2f, 2,
			List.of(REPULSOR, CHEST_BEAM, MICRO_MISSILES, SHOULDER_GUNS, CUTTING_LASER, FLARES, SCANNER),
			new Stats(0.20, 4, 0.0, 0.0, 0.6, 0.0, 6, 24),
			1.25f),

	// Тяжёлый экзоскелет против гигантов: огромный, летает тяжело и медленно, бьёт как пресс,
	// а при приземлении (в том числе из пике) бьёт ударной волной.
	TITAN("titan", 0xFFFFC233,
			armor(6, 10, 12, 6, 6.0f, 0.25f, SoundEvents.ARMOR_EQUIP_NETHERITE),
			1600, 8, true, 0.045f, 0.06f, 1.3f, 5,
			List.of(PILE_DRIVER, HEAVY_ROCKETS, CHEST_BEAM, SHOULDER_GUNS, SCANNER),
			new Stats(-0.05, 10, 0.25, 0.6, 1.1, 2.5, 20, 64),
			1.6f),

	// Наноброня: самая быстрая, самая мощная, умеет всё.
	NANO("nano", 0xFFB46CFF,
			armor(5, 8, 10, 5, 4.0f, 0.15f, SoundEvents.ARMOR_EQUIP_NETHERITE),
			2200, 12, true, 0.11f, 0.20f, 3.0f, 2,
			List.of(REPULSOR, CHEST_BEAM, MICRO_MISSILES, CUTTING_LASER, FLARES, NANO_BLADE, NANO_SHIELD, NANO_CANNON, SCANNER),
			new Stats(0.30, 6, 0.05, 0.0, 0.6, 1.0, 10, 64),
			1.6f);

	/**
	 * Бонусы полного комплекта.
	 *
	 * @param speed      множитель скорости ходьбы (+0.2 = +20%)
	 * @param attack     прибавка к урону кулаком/оружием
	 * @param jump       прибавка к силе прыжка
	 * @param scale      прибавка к размеру (0.6 = в 1.6 раза больше)
	 * @param step       прибавка к высоте шага (залезать на блоки без прыжка)
	 * @param reach      прибавка к дальности взаимодействия
	 * @param health     дополнительные единицы здоровья
	 * @param safeFall   безопасная высота падения
	 */
	public record Stats(double speed, double attack, double jump, double scale, double step, double reach, double health, double safeFall) {
	}

	public final String id;
	/** Цвет интерфейса шлема (ARGB). */
	public final int hudColor;
	public final ArmorMaterial material;
	public final int maxEnergy;
	/** Восстановление энергии за секунду. */
	public final int recharge;
	public final boolean flight;
	public final float flySpeed;
	/** Ускорение форсажа (Ctrl в полёте) за тик и предельная скорость в блоках/тик. */
	public final float boost;
	public final float maxBoostSpeed;
	/** Расход энергии на полёт в секунду (вдвое больше на форсаже). */
	public final int flightDrain;
	public final List<SuitWeapon> weapons;
	public final Stats stats;
	/** Множитель урона встроенного оружия. */
	public final float power;

	SuitTier(String id, int hudColor, ArmorMaterial material, int maxEnergy, int recharge, boolean flight, float flySpeed,
			float boost, float maxBoostSpeed, int flightDrain, List<SuitWeapon> weapons, Stats stats, float power) {
		this.id = id;
		this.hudColor = hudColor;
		this.material = withAsset(material, id);
		this.maxEnergy = maxEnergy;
		this.recharge = recharge;
		this.flight = flight;
		this.flySpeed = flySpeed;
		this.boost = boost;
		this.maxBoostSpeed = maxBoostSpeed;
		this.flightDrain = flightDrain;
		this.weapons = weapons;
		this.stats = stats;
		this.power = power;
	}

	public String translationKey() {
		return "suit.solorust." + id;
	}

	public boolean has(SuitWeapon weapon) {
		return weapons.contains(weapon);
	}

	/** Поколение полного комплекта на сущности или null, если комплект неполный / разный. */
	public static SuitTier worn(LivingEntity entity) {
		SuitTier tier = null;

		for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			ItemStack stack = entity.getItemBySlot(slot);

			if (!(stack.getItem() instanceof SuitArmorItem armor)) {
				return null;
			}

			if (tier == null) {
				tier = armor.tier();
			} else if (tier != armor.tier()) {
				return null;
			}
		}

		return tier;
	}

	// ------------------------------------------------------------------ материалы

	private static ArmorMaterial armor(int boots, int legs, int chest, int helmet, float toughness, float knockback, Holder<SoundEvent> equipSound) {
		Map<ArmorType, Integer> defense = new EnumMap<>(ArmorType.class);
		defense.put(ArmorType.BOOTS, boots);
		defense.put(ArmorType.LEGGINGS, legs);
		defense.put(ArmorType.CHESTPLATE, chest);
		defense.put(ArmorType.HELMET, helmet);
		defense.put(ArmorType.BODY, chest);
		// assetId подставляется в конструкторе enum (нужен id поколения).
		return new ArmorMaterial(1000, defense, 12, equipSound, toughness, knockback, ItemTags.REPAIRS_IRON_ARMOR, null);
	}

	private static ArmorMaterial withAsset(ArmorMaterial m, String id) {
		ResourceKey<EquipmentAsset> asset = ResourceKey.create(EquipmentAssets.ROOT_ID, SoloRust.id("suit_" + id));
		return new ArmorMaterial(m.durability(), m.defense(), m.enchantmentValue(), m.equipSound(), m.toughness(), m.knockbackResistance(), m.repairIngredient(), asset);
	}
}
