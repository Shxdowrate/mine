package com.shxdowrate.solorust.gun;

/**
 * Характеристики ствола. Урон — в полусердечках за одну пулю/дробину
 * (для дробовиков это множитель к урону патрона).
 *
 * @param tier        уровень верстака для крафта (0 — руками, -1 — только лут)
 * @param automatic   стреляет очередью, пока зажата ЛКМ
 * @param interval    тиков между выстрелами
 * @param hipSpread   разброс от бедра, градусы
 * @param adsSpread   разброс в прицеливании, градусы
 * @param recoil      подброс камеры за выстрел, градусы
 * @param magazine    размер магазина
 * @param reload      время перезарядки, тики
 * @param zoom        множитель поля зрения в прицеливании без прицела (меньше = сильнее зум)
 * @param mods        можно ли ставить модули
 * @param misfire     шанс осечки
 */
public record GunDef(
		String id,
		int tier,
		AmmoClass ammo,
		boolean automatic,
		int interval,
		float damage,
		float hipSpread,
		float adsSpread,
		float recoil,
		double range,
		int magazine,
		int reload,
		float zoom,
		boolean mods,
		float misfire
) {
	public static GunDef semi(String id, int tier, AmmoClass ammo, int interval, float damage, float hip, float ads, float recoil,
			double range, int mag, int reload, float zoom, boolean mods) {
		return new GunDef(id, tier, ammo, false, interval, damage, hip, ads, recoil, range, mag, reload, zoom, mods, 0f);
	}

	public static GunDef auto(String id, int tier, AmmoClass ammo, int interval, float damage, float hip, float ads, float recoil,
			double range, int mag, int reload, float zoom) {
		return new GunDef(id, tier, ammo, true, interval, damage, hip, ads, recoil, range, mag, reload, zoom, true, 0f);
	}

	public GunDef withMisfire(float chance) {
		return new GunDef(id, tier, ammo, automatic, interval, damage, hipSpread, adsSpread, recoil, range, magazine, reload, zoom, mods, chance);
	}
}
