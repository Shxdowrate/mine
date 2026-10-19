package com.shxdowrate.solorust.magic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Одна способность артефакта.
 *
 * @param id       ключ перевода {@code ability.solorust.<id>}
 * @param cooldown перезарядка в тиках
 * @param caster   действие; вернуть {@code false}, если применить не вышло (нет цели и т. п.) — тогда перезарядка не начнётся
 */
public record Ability(String id, int cooldown, Caster caster) {
	@FunctionalInterface
	public interface Caster {
		boolean cast(ServerLevel level, ServerPlayer player, ItemStack stack);
	}

	public String translationKey() {
		return "ability.solorust." + id;
	}
}
