package com.shxdowrate.solorust.magic;

import net.minecraft.world.item.ItemStack;

/** Кольцо-портал Верховного чародея: 18 мистических искусств Доктора Стрэнджа. */
public class SorcererRingItem extends ArtifactItem {
	private static final int[] COLORS = {0xFF9A1F, 0xFFC94A, 0xFF6A1F, 0xD81B3C, 0x2BD94A, 0x7FD7FF, 0xC62828, 0xFF5722, 0x8E44FF};

	public SorcererRingItem(Properties properties) {
		super(properties);
	}

	@Override
	public Ability ability(int slot, int button) {
		return StrangeAbilities.ABILITIES[slot * 2 + button];
	}

	@Override
	public int slotColor(int slot) {
		return COLORS[slot];
	}

	@Override
	public String key() {
		return "ring";
	}

	@Override
	public boolean grantsFlight(ItemStack stack) {
		return true;
	}
}
