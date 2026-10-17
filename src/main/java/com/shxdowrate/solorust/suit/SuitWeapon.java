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
	NANO_CANNON("nano_cannon", Mode.CHARGE, 220, 100, 25, 70),

	// --- «Сингулярность»: ядра ---
	GRAVITY_GRIP("gravity_grip", Mode.CHARGE, 120, 40, 6, 40),
	TIME_DILATION("time_dilation", Mode.INSTANT, 260, 300, 0, 0),
	SPACE_JUMP("space_jump", Mode.INSTANT, 80, 16, 0, 0),
	MATTER_SHIFT("matter_shift", Mode.INSTANT, 260, 120, 0, 0),
	MIND_LINK("mind_link", Mode.INSTANT, 240, 300, 0, 0),
	LIFE_SURGE("life_surge", Mode.INSTANT, 300, 600, 0, 0),
	COLLAPSE("collapse", Mode.CHARGE, 2500, 1200, 60, 60),

	// --- Архимаг ---
	MYSTIC_BOLT("mystic_bolt", Mode.INSTANT, 14, 10, 0, 0),
	PORTAL("portal", Mode.INSTANT, 120, 20, 0, 0),
	RUNE_SHIELD("rune_shield", Mode.HOLD, 1, 0, 0, 0),
	ASTRAL_FORM("astral_form", Mode.INSTANT, 200, 400, 0, 0),
	TIME_LOOP("time_loop", Mode.INSTANT, 300, 400, 0, 0),

	// --- «Апекс» ---
	HEAT_VISION("heat_vision", Mode.HOLD, 3, 0, 0, 0),
	SONIC_CLAP("sonic_clap", Mode.INSTANT, 60, 40, 0, 0),
	SUPER_GRAB("super_grab", Mode.CHARGE, 30, 20, 4, 30),

	// --- Рабочие ---
	DRILL("drill", Mode.HOLD, 2, 0, 0, 0),
	SEISMIC("seismic", Mode.INSTANT, 40, 60, 0, 0),
	LIGHT_FLARE("light_flare", Mode.INSTANT, 8, 8, 0, 0),
	BUILD_WALL("build_wall", Mode.INSTANT, 20, 10, 0, 0),
	BUILD_BRIDGE("build_bridge", Mode.INSTANT, 20, 10, 0, 0),
	BUILD_FLOOR("build_floor", Mode.INSTANT, 25, 10, 0, 0),
	DEMOLISH("demolish", Mode.INSTANT, 30, 10, 0, 0),
	CLOAK("cloak", Mode.INSTANT, 150, 600, 0, 0),
	GRAPPLE("grapple", Mode.INSTANT, 25, 14, 0, 0);

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
