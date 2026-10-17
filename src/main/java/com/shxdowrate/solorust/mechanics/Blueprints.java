package com.shxdowrate.solorust.mechanics;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.shxdowrate.solorust.SoloRust;
import com.shxdowrate.solorust.item.AttachmentItem;
import com.shxdowrate.solorust.item.GunItem;
import com.shxdowrate.solorust.registry.ModItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/**
 * Чертежи как в Расте: оружие и модули сначала надо найти и изучить на исследовательском столе,
 * только потом они появляются в крафте верстака. Изученное сохраняется за игроком и не теряется при смерти.
 */
public final class Blueprints {
	public static final AttachmentType<List<String>> KNOWN = AttachmentRegistry.<List<String>>create(SoloRust.id("blueprints"),
			builder -> builder
					.persistent(Codec.STRING.listOf())
					.copyOnDeath()
					.initializer(ArrayList::new));

	private Blueprints() {
	}

	public static void init() {
	}

	/** Нужен ли чертёж для крафта этого предмета. */
	public static boolean required(Item item) {
		if (item instanceof GunItem gun) {
			return gun.def().tier() >= 1;
		}

		return item instanceof AttachmentItem || item == ModItems.SUPPLY_SIGNAL;
	}

	/** Сколько скрапа стоит изучение. 0 — изучить нельзя. */
	public static int researchCost(Item item) {
		if (item instanceof GunItem gun) {
			return switch (gun.def().tier()) {
				case 1 -> 25;
				case 2 -> 60;
				case 3 -> 125;
				default -> 0;
			};
		}

		if (item == ModItems.SIMPLE_SIGHT) {
			return 15;
		}

		if (item == ModItems.SCOPE_8X || item == ModItems.SCOPE_16X) {
			return 125;
		}

		if (item instanceof AttachmentItem) {
			return 50;
		}

		if (item == ModItems.SUPPLY_SIGNAL) {
			return 125;
		}

		return 0;
	}

	public static boolean knows(Player player, Item item) {
		if (!required(item) || player.hasInfiniteMaterials()) {
			return true;
		}

		List<String> known = player.getAttached(KNOWN);
		return known != null && known.contains(BuiltInRegistries.ITEM.getKey(item).toString());
	}

	public static void learn(Player player, Item item) {
		List<String> known = new ArrayList<>(player.getAttachedOrCreate(KNOWN));
		String id = BuiltInRegistries.ITEM.getKey(item).toString();

		if (!known.contains(id)) {
			known.add(id);
			player.setAttached(KNOWN, known);
		}
	}
}
