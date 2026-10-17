package com.shxdowrate.solorust.gun;

/** Модули на оружие и их эффекты. */
public enum Attachment {
	HOLOSIGHT("holosight", Slot.SIGHT),
	SIMPLE_SIGHT("simple_sight", Slot.SIGHT),
	SCOPE_8X("scope_8x", Slot.SIGHT),
	SCOPE_16X("scope_16x", Slot.SIGHT),
	SILENCER("silencer", Slot.MUZZLE),
	MUZZLE_BOOST("muzzle_boost", Slot.MUZZLE),
	MUZZLE_BRAKE("muzzle_brake", Slot.MUZZLE),
	LASER_SIGHT("laser_sight", Slot.UNDER),
	EXTENDED_MAG("extended_magazine", Slot.MAGAZINE);

	public enum Slot { SIGHT, MUZZLE, UNDER, MAGAZINE }

	public final String id;
	public final Slot slot;

	Attachment(String id, Slot slot) {
		this.id = id;
		this.slot = slot;
	}

	public static Attachment byId(String id) {
		for (Attachment a : values()) {
			if (a.id.equals(id)) {
				return a;
			}
		}

		return null;
	}

	public boolean isScope() {
		return this == SCOPE_8X || this == SCOPE_16X;
	}
}
