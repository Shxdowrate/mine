package com.shxdowrate.solorust.gun;

import java.util.List;

import com.shxdowrate.solorust.registry.ModItems;

import net.minecraft.world.item.Item;

/** Какие патроны подходят стволу. */
public enum AmmoClass {
	EOKA,
	SHOTGUN,
	PISTOL,
	RIFLE,
	NAIL;

	/** Свойства конкретного патрона: сколько дробин, множитель урона, добавка к разбросу. */
	public record Props(int pellets, float damage, float extraSpread) {
	}

	public List<Item> items() {
		return switch (this) {
			case EOKA -> List.of(ModItems.HANDMADE_SHELL);
			case SHOTGUN -> List.of(ModItems.BUCKSHOT, ModItems.SLUG, ModItems.HANDMADE_SHELL);
			case PISTOL -> List.of(ModItems.PISTOL_AMMO);
			case RIFLE -> List.of(ModItems.RIFLE_AMMO);
			case NAIL -> List.of(ModItems.NAILS);
		};
	}

	public static Props props(Item ammo) {
		if (ammo == ModItems.HANDMADE_SHELL) {
			return new Props(6, 1.6f, 2.0f);
		}

		if (ammo == ModItems.BUCKSHOT) {
			return new Props(8, 2.4f, 0f);
		}

		if (ammo == ModItems.SLUG) {
			return new Props(1, 11f, -2.0f);
		}

		return new Props(1, 1.0f, 0f);
	}
}
