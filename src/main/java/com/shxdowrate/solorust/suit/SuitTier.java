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
			1.6f),

	// Лёгкий костюм из кейса: слабая броня, зато разворачивается за секунду где угодно.
	LIGHT("light", 0xFF7FFFD4,
			armor(2, 4, 5, 2, 1.0f, 0.0f, SoundEvents.ARMOR_EQUIP_IRON),
			350, 4, false, 0f, 0f, 0f, 0,
			List.of(REPULSOR, SCANNER),
			new Stats(0.10, 2, 0.08, 0.0, 0.5, 0.0, 0, 6),
			0.8f),

	// Космический костюм с ядрами: гравитация, время, пространство, материя, разум, жизнь и «Коллапс».
	SINGULARITY("singularity", 0xFFFFD66B,
			armor(6, 10, 12, 6, 6.0f, 0.2f, SoundEvents.ARMOR_EQUIP_NETHERITE),
			3000, 15, true, 0.12f, 0.22f, 3.2f, 2,
			List.of(GRAVITY_GRIP, TIME_DILATION, SPACE_JUMP, MATTER_SHIFT, MIND_LINK, LIFE_SURGE, COLLAPSE, REPULSOR, SCANNER),
			new Stats(0.35, 8, 0.1, 0.0, 0.6, 2.0, 20, 64),
			2.0f),

	// Маг: брони почти нет, зато левитация, порталы, щиты, астрал и петля времени.
	ARCHMAGE("archmage", 0xFFFFB347,
			armor(2, 4, 5, 2, 1.0f, 0.0f, SoundEvents.ARMOR_EQUIP_LEATHER),
			1500, 12, true, 0.06f, 0.08f, 1.6f, 1,
			List.of(MYSTIC_BOLT, PORTAL, RUNE_SHIELD, ASTRAL_FORM, TIME_LOOP),
			new Stats(0.10, 1, 0.05, 0.0, 0.5, 1.0, 4, 64),
			1.3f),

	// Сверхчеловек: летает быстрее всех, лучи из глаз, сила; почти неуязвим, пока сыт.
	APEX("apex", 0xFFF2F2F2,
			armor(3, 6, 8, 3, 4.0f, 0.4f, SoundEvents.ARMOR_EQUIP_LEATHER),
			1800, 14, true, 0.14f, 0.26f, 3.6f, 1,
			List.of(HEAT_VISION, SONIC_CLAP, SUPER_GRAB),
			new Stats(0.45, 14, 0.25, 0.0, 0.6, 1.5, 20, 256),
			1.8f),

	// Рабочие костюмы.
	MINER("miner", 0xFFFFD23D,
			armor(3, 6, 7, 3, 2.0f, 0.1f, SoundEvents.ARMOR_EQUIP_IRON),
			1000, 8, false, 0f, 0f, 0f, 0,
			List.of(DRILL, SEISMIC, LIGHT_FLARE, SCANNER),
			new Stats(0.05, 3, 0.05, 0.0, 0.5, 1.5, 0, 10),
			1.0f),

	BUILDER("builder", 0xFFFF8A3D,
			armor(3, 5, 6, 3, 1.0f, 0.0f, SoundEvents.ARMOR_EQUIP_IRON),
			1000, 10, true, 0.07f, 0.1f, 1.6f, 1,
			List.of(BUILD_WALL, BUILD_BRIDGE, BUILD_FLOOR, DEMOLISH, LIGHT_FLARE),
			new Stats(0.10, 1, 0.0, 0.0, 0.6, 5.0, 0, 64),
			1.0f),

	DIVER("diver", 0xFF4FE3C1,
			armor(3, 6, 7, 3, 2.0f, 0.1f, SoundEvents.ARMOR_EQUIP_TURTLE),
			1000, 8, false, 0f, 0f, 0f, 0,
			List.of(MICRO_MISSILES, CUTTING_LASER, SCANNER),
			new Stats(0.05, 3, 0.0, 0.0, 0.5, 1.0, 4, 12),
			1.1f),

	SCOUT("scout", 0xFF8FD16B,
			armor(2, 5, 6, 2, 1.0f, 0.0f, SoundEvents.ARMOR_EQUIP_LEATHER),
			800, 8, false, 0f, 0f, 0f, 0,
			List.of(CLOAK, GRAPPLE, SCANNER),
			new Stats(0.35, 2, 0.15, 0.0, 0.6, 0.0, 0, 16),
			1.0f),

	// Костюм хаоса: разрушение по площади — вихрь, волна и взрыв, который выжигает всё вокруг.
	CHAOS("chaos", 0xFFFF2E4D,
			armor(5, 9, 11, 5, 5.0f, 0.2f, SoundEvents.ARMOR_EQUIP_NETHERITE),
			2600, 13, true, 0.10f, 0.2f, 3.0f, 2,
			List.of(CHAOS_BOLT, CHAOS_CHAIN, CHAOS_WAVE, CHAOS_VORTEX, CHAOS_METEORS, CHAOS_RIFT, CHAOS_SPHERE, CHAOS_CURSE, CHAOS_BLAST, SCANNER),
			new Stats(0.25, 7, 0.1, 0.0, 0.6, 1.0, 12, 64),
			1.8f),

	// ================================================================== War Machines: костюмы WARDEN
	// Все летают. Характеристики задаёт редкость (WardenRarity), здесь — только цвет и способности.

	W_HOT_ROD("w_hot_rod", 0xFFFF6A3D, WardenRarity.COMMON, List.of(REPULSOR)),
	W_STEEL("w_steel", 0xFFC9D1DA, WardenRarity.COMMON, List.of(REPULSOR)),
	W_ECO("w_eco", 0xFF7BE07B, WardenRarity.COMMON, List.of(REPULSOR)),
	W_CARBON("w_carbon", 0xFF9AA0A8, WardenRarity.COMMON, List.of(REPULSOR)),
	W_NAVY("w_navy", 0xFF5A8CFF, WardenRarity.COMMON, List.of(REPULSOR)),
	W_DESERT("w_desert", 0xFFE8C77A, WardenRarity.COMMON, List.of(REPULSOR)),
	W_ARCTIC("w_arctic", 0xFFE6F6FF, WardenRarity.COMMON, List.of(REPULSOR)),

	W_DISCO("w_disco", 0xFFFF6BEA, WardenRarity.UNCOMMON, List.of(REPULSOR, FLARES)),
	W_SWAMP_RAIDER("w_swamp_raider", 0xFF8FBF4A, WardenRarity.UNCOMMON, List.of(REPULSOR, WRIST_ROCKET)),
	W_WOODLANDS("w_woodlands", 0xFF6FA35A, WardenRarity.UNCOMMON, List.of(REPULSOR, WRIST_RIFLE)),

	W_SOLDIER("w_soldier", 0xFF9DB36B, WardenRarity.RARE, List.of(REPULSOR, WRIST_RIFLE, FLARES)),
	W_BARRAGE("w_barrage", 0xFFFF8A3D, WardenRarity.RARE, List.of(REPULSOR, MICRO_MISSILES, FLARES)),
	W_BUMBLE_BEE("w_bumble_bee", 0xFFFFD23D, WardenRarity.RARE, List.of(REPULSOR, SHOULDER_GUNS)),
	W_MOUNTAIN("w_mountain", 0xFFB7A79A, WardenRarity.RARE, List.of(REPULSOR, PILE_DRIVER, WRIST_ROCKET)),
	W_CHROMA("w_chroma", 0xFF6BFFF0, WardenRarity.RARE, List.of(REPULSOR, CHEST_BEAM)),

	W_SHEMAGH("w_shemagh", 0xFFD9B98A, WardenRarity.EPIC, List.of(REPULSOR, WRIST_RIFLE, WRIST_ROCKET, FLARES)),
	W_NIGHT_OPS("w_night_ops", 0xFF7DFF8A, WardenRarity.EPIC, List.of(REPULSOR, WRIST_RIFLE, CLOAK, SCANNER)),
	W_DEADEYE("w_deadeye", 0xFFFF3D3D, WardenRarity.EPIC, List.of(REPULSOR, MARKSMAN, SCANNER)),
	W_CRYO("w_cryo", 0xFF7FE8FF, WardenRarity.EPIC, List.of(REPULSOR, CRYO_MISSILE, FLARES)),

	W_ARSENAL("w_arsenal", 0xFFB0B8C0, WardenRarity.LEGENDARY, List.of(WRIST_RIFLE, SHOULDER_GUNS, MICRO_MISSILES, FLARES, SCANNER)),
	W_CUPID("w_cupid", 0xFFFF7FB0, WardenRarity.LEGENDARY, List.of(REPULSOR, LOVE_ARROW, FLARES)),

	W_TRICKSTER("w_trickster", 0xFF4FE36B, WardenRarity.MYTHIC, List.of(REPULSOR, DECOY, CLOAK, SCANNER)),
	W_PORTALMANCER("w_portalmancer", 0xFFFF9A2E, WardenRarity.MYTHIC, List.of(REPULSOR, PORTAL, MICRO_MISSILES)),
	W_INFINITY("w_infinity", 0xFFFFE36B, WardenRarity.MYTHIC, List.of(REPULSOR, CHEST_BEAM, HEAVY_ROCKETS, SCANNER)),
	W_WRAITH("w_wraith", 0xFF9C7BFF, WardenRarity.MYTHIC, List.of(REPULSOR, SOUL_DRAIN, CLOAK));

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
	/** Редкость, если это костюм WARDEN (War Machines); null — силовая броня. */
	public final WardenRarity warden;

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
		this.warden = null;
	}

	/** Костюм WARDEN: всё, кроме цвета и способностей, берётся из редкости. */
	SuitTier(String id, int hudColor, WardenRarity rarity, List<SuitWeapon> weapons) {
		this.id = id;
		this.hudColor = hudColor;
		this.material = withAsset(rarity.armor(), id);
		this.maxEnergy = rarity.maxEnergy();
		this.recharge = rarity.recharge();
		this.flight = true;
		this.flySpeed = rarity.flySpeed();
		this.boost = rarity.boost();
		this.maxBoostSpeed = rarity.maxBoostSpeed();
		this.flightDrain = 2;
		this.weapons = weapons;
		this.stats = rarity.stats();
		this.power = rarity.power();
		this.warden = rarity;
	}

	public boolean isWarden() {
		return warden != null;
	}

	/** Все костюмы WARDEN по порядку редкости. */
	public static List<SuitTier> wardenSuits() {
		return java.util.Arrays.stream(values()).filter(SuitTier::isWarden).toList();
	}

	public static SuitTier byId(String id) {
		for (SuitTier tier : values()) {
			if (tier.id.equals(id)) {
				return tier;
			}
		}

		return null;
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

	static ArmorMaterial armorOf(int boots, int legs, int chest, int helmet, float toughness, float knockback, Holder<SoundEvent> equipSound) {
		return armor(boots, legs, chest, helmet, toughness, knockback, equipSound);
	}

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
