package com.shxdowrate.solorust.magic;

import net.minecraft.ChatFormatting;

/** Шесть камней бесконечности: порядок совпадает с ячейками 1–6 перчатки. */
public enum InfinityStone {
	POWER("power_stone", 0x9B30FF, ChatFormatting.DARK_PURPLE),
	SPACE("space_stone", 0x2E7BFF, ChatFormatting.BLUE),
	REALITY("reality_stone", 0xE0182D, ChatFormatting.RED),
	SOUL("soul_stone", 0xFF8A1F, ChatFormatting.GOLD),
	TIME("time_stone", 0x2BD94A, ChatFormatting.GREEN),
	MIND("mind_stone", 0xFFD21F, ChatFormatting.YELLOW);

	public static final int ALL = (1 << values().length) - 1;

	public final String id;
	public final int color;
	public final ChatFormatting format;

	InfinityStone(String id, int color, ChatFormatting format) {
		this.id = id;
		this.color = color;
		this.format = format;
	}

	public int bit() {
		return 1 << ordinal();
	}
}
