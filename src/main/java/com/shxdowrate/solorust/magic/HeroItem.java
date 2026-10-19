package com.shxdowrate.solorust.magic;

import net.minecraft.world.item.ItemStack;

/** Артефакт героя: 18 способностей из таблицы, цвета ячеек и (необязательно) полёт. */
public class HeroItem extends ArtifactItem {
	private final String key;
	private final Ability[] abilities;
	private final int[] colors;
	private final boolean flight;

	public HeroItem(Properties properties, String key, Ability[] abilities, int[] colors, boolean flight) {
		super(properties);

		if (abilities.length != SLOTS * 2 || colors.length != SLOTS) {
			throw new IllegalArgumentException(key + ": нужно ровно 18 способностей и 9 цветов");
		}

		this.key = key;
		this.abilities = abilities;
		this.colors = colors;
		this.flight = flight;
	}

	@Override
	public Ability ability(int slot, int button) {
		return abilities[slot * 2 + button];
	}

	@Override
	public int slotColor(int slot) {
		return colors[slot];
	}

	@Override
	public String key() {
		return key;
	}

	@Override
	public boolean grantsFlight(ItemStack stack) {
		return flight;
	}
}
