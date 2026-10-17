package com.shxdowrate.solorust.suit;

/**
 * Встроенное оружие и системы брони. Каждое — отдельный предмет, который сам появляется в инвентаре,
 * пока надет полный комплект нужного костюма, и исчезает, когда костюм снят.
 *
 * <ul>
 *     <li>{@link Mode#INSTANT} — ПКМ: один выстрел/действие, потом перезарядка (cooldown).</li>
 *     <li>{@link Mode#HOLD} — держи ПКМ: работает каждый тик, тратит энергию, пока зажато.</li>
 *     <li>{@link Mode#CHARGE} — держи ПКМ, чтобы зарядить, отпусти — выстрел. Чем дольше, тем мощнее.</li>
 * </ul>
 */
public enum SuitWeapon {
	//             id                  mode           energy cooldown minCharge maxCharge
	FLAMETHROWER("flamethrower", Mode.HOLD, 3, 0, 0, 0),
	WRIST_ROCKET("wrist_rocket", Mode.INSTANT, 60, 40, 0, 0),
	REPULSOR("repulsor", Mode.INSTANT, 12, 7, 0, 0),
	CHEST_BEAM("chest_beam", Mode.CHARGE, 140, 60, 15, 50),
	MICRO_MISSILES("micro_missiles", Mode.INSTANT, 90, 50, 0, 0),
	SHOULDER_GUNS("shoulder_guns", Mode.HOLD, 3, 0, 0, 0),
	CUTTING_LASER("cutting_laser", Mode.HOLD, 2, 0, 0, 0),
	FLARES("flares", Mode.INSTANT, 40, 100, 0, 0),
	SCANNER("scanner", Mode.INSTANT, 15, 40, 0, 0),
	PILE_DRIVER("pile_driver", Mode.INSTANT, 35, 22, 0, 0),
	HEAVY_ROCKETS("heavy_rockets", Mode.INSTANT, 150, 80, 0, 0),
	NANO_BLADE("nano_blade", Mode.INSTANT, 40, 30, 0, 0),
	NANO_SHIELD("nano_shield", Mode.HOLD, 1, 0, 0, 0),
	NANO_CANNON("nano_cannon", Mode.CHARGE, 220, 100, 25, 70);

	public enum Mode {
		INSTANT, HOLD, CHARGE
	}

	public final String id;
	public final Mode mode;
	/** Энергия за выстрел (для HOLD — за тик, для CHARGE — за полный заряд). */
	public final int energy;
	public final int cooldown;
	public final int minCharge;
	public final int maxCharge;

	SuitWeapon(String id, Mode mode, int energy, int cooldown, int minCharge, int maxCharge) {
		this.id = id;
		this.mode = mode;
		this.energy = energy;
		this.cooldown = cooldown;
		this.minCharge = minCharge;
		this.maxCharge = maxCharge;
	}

	public String itemId() {
		return "suit_" + id;
	}
}
