package com.shxdowrate.solorust.war;

import java.util.function.Supplier;

import com.shxdowrate.solorust.registry.ModItems;
import com.shxdowrate.solorust.registry.ModSounds;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Роли бандитов. Каждая со своим оружием, дистанцией боя, точностью и манерой поведения.
 *
 * @param health       здоровье
 * @param armor        броня
 * @param speed        скорость передвижения
 * @param minRange     ближе этого бандит отходит
 * @param maxRange     дальше этого — подходит
 * @param damage       урон одной пули/дробины (до множителя сложности)
 * @param pellets      дробин за выстрел
 * @param interval     тиков между выстрелами в очереди
 * @param burst        выстрелов в очереди (мин..макс)
 * @param pause        пауза между очередями, тики
 * @param magazine     патронов в магазине (после — перезарядка в укрытии)
 * @param reload       время перезарядки, тики
 * @param spread       базовый разброс, градусы
 * @param lead         насколько хорошо берёт упреждение по движущейся цели (0..1)
 * @param reward       награда за убийство (Cash)
 * @param weight       шанс появления в лагере
 */
public enum BanditRole {
	RIFLEMAN("rifleman", 26, 4, 0.30, 8, 30, 3.0f, 1, 3, 3, 5, 24, 30, 60, 4.0f, 0.6f, 150, 40, () -> ModItems.ASSAULT_RIFLE),
	SHOTGUNNER("shotgunner", 30, 6, 0.33, 2, 10, 1.7f, 8, 18, 1, 1, 14, 6, 80, 7.0f, 0.4f, 175, 18, () -> ModItems.PUMP_SHOTGUN),
	SNIPER("sniper", 20, 2, 0.28, 18, 56, 10.0f, 1, 50, 1, 1, 30, 5, 90, 0.6f, 0.9f, 220, 12, () -> ModItems.BOLT_RIFLE),
	GRENADIER("grenadier", 26, 4, 0.29, 10, 36, 0f, 1, 70, 1, 1, 10, 3, 90, 2.0f, 0.75f, 250, 12, () -> Items.CROSSBOW),
	MACHINE_GUNNER("machine_gunner", 46, 10, 0.24, 8, 32, 2.4f, 1, 2, 12, 22, 30, 60, 120, 5.5f, 0.5f, 300, 10, () -> ModItems.M249),
	OFFICER("officer", 40, 8, 0.31, 6, 24, 2.8f, 1, 2, 4, 7, 20, 30, 60, 3.5f, 0.8f, 500, 0, () -> ModItems.MP5);

	public final String id;
	public final double health;
	public final double armor;
	public final double speed;
	public final double minRange;
	public final double maxRange;
	public final float damage;
	public final int pellets;
	public final int interval;
	public final int burstMin;
	public final int burstMax;
	public final int pause;
	public final int magazine;
	public final int reload;
	public final float spread;
	public final float lead;
	public final int reward;
	public final int weight;
	private final Supplier<Item> weapon;

	BanditRole(String id, double health, double armor, double speed, double minRange, double maxRange, float damage, int pellets, int interval,
			int burstMin, int burstMax, int pause, int magazine, int reload, float spread, float lead, int reward, int weight, Supplier<Item> weapon) {
		this.id = id;
		this.health = health;
		this.armor = armor;
		this.speed = speed;
		this.minRange = minRange;
		this.maxRange = maxRange;
		this.damage = damage;
		this.pellets = pellets;
		this.interval = interval;
		this.burstMin = burstMin;
		this.burstMax = burstMax;
		this.pause = pause;
		this.magazine = magazine;
		this.reload = reload;
		this.spread = spread;
		this.lead = lead;
		this.reward = reward;
		this.weight = weight;
		this.weapon = weapon;
	}

	public Item weapon() {
		return weapon.get();
	}

	public SoundEvent shotSound() {
		return switch (this) {
			case SHOTGUNNER -> ModSounds.SHOT_SHOTGUN;
			case SNIPER -> ModSounds.SHOT_SNIPER;
			case OFFICER -> ModSounds.SHOT_SMG;
			default -> ModSounds.SHOT_RIFLE;
		};
	}

	public String translationKey() {
		return "entity.solorust.bandit." + id;
	}

	public static BanditRole byIndex(int index) {
		BanditRole[] values = values();
		return values[Math.floorMod(index, values.length)];
	}

	/** Случайная рядовая роль (офицеров назначают отдельно — по одному на лагерь). */
	public static BanditRole random(RandomSource random) {
		int total = 0;

		for (BanditRole role : values()) {
			total += role.weight;
		}

		int r = random.nextInt(total);

		for (BanditRole role : values()) {
			r -= role.weight;

			if (r < 0) {
				return role;
			}
		}

		return RIFLEMAN;
	}
}
