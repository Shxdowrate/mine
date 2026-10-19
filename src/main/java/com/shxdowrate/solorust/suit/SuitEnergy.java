package com.shxdowrate.solorust.suit;

import com.shxdowrate.solorust.registry.ModComponents;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Энергия костюма живёт на нагруднике: новый костюм заряжен полностью. */
public final class SuitEnergy {
	private SuitEnergy() {
	}

	public static int get(ItemStack chest, SuitTier tier) {
		return Mth.clamp(chest.getOrDefault(ModComponents.SUIT_ENERGY, tier.maxEnergy), 0, tier.maxEnergy);
	}

	public static int get(Player player, SuitTier tier) {
		return get(player.getItemBySlot(EquipmentSlot.CHEST), tier);
	}

	public static void set(Player player, SuitTier tier, int value) {
		ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
		int clamped = Mth.clamp(value, 0, tier.maxEnergy);

		if (get(chest, tier) != clamped) {
			chest.set(ModComponents.SUIT_ENERGY, clamped);
		}
	}

	/** Списывает энергию. В творческом режиме энергия бесконечная. Возвращает false, если не хватило. */
	public static boolean use(Player player, SuitTier tier, int amount) {
		if (player.hasInfiniteMaterials()) {
			return true;
		}

		int current = get(player, tier);

		if (current < amount) {
			return false;
		}

		set(player, tier, current - amount);
		return true;
	}
}
