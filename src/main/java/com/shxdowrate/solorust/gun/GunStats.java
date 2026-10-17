package com.shxdowrate.solorust.gun;

import com.shxdowrate.solorust.item.GunItem;
import com.shxdowrate.solorust.registry.ModComponents;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Итоговые характеристики ствола с учётом модулей и того, что делает игрок. Общий код для клиента и сервера. */
public record GunStats(int interval, float damageMult, float spread, float recoil, int magazine, float zoom, boolean silenced, Attachment sight) {
	public static GunMods mods(ItemStack stack) {
		return stack.getOrDefault(ModComponents.MODS, GunMods.EMPTY);
	}

	public static int magazine(GunDef def, GunMods mods) {
		return mods.has(Attachment.EXTENDED_MAG) ? (int) Math.ceil(def.magazine() * 1.25) : def.magazine();
	}

	public static boolean isAiming(Player player) {
		return player.isUsingItem() && player.getUseItem().getItem() instanceof GunItem;
	}

	public static GunStats of(GunDef def, ItemStack stack, Player player) {
		GunMods mods = mods(stack);
		boolean aiming = isAiming(player);
		boolean moving = player.getDeltaMovement().horizontalDistanceSqr() > 0.003;
		boolean crouching = player.isShiftKeyDown();

		float interval = def.interval();
		float damage = 1f;
		float hip = def.hipSpread();
		float ads = def.adsSpread();
		float recoil = def.recoil();
		float zoom = def.zoom();
		boolean silenced = false;
		Attachment sight = null;

		for (Attachment a : mods.list()) {
			switch (a) {
				case HOLOSIGHT -> {
					ads *= 0.7f;
					zoom = Math.min(zoom, 0.7f);
					sight = a;
				}
				case SIMPLE_SIGHT -> {
					ads *= 0.85f;
					zoom = Math.min(zoom, 0.78f);
					sight = a;
				}
				case SCOPE_8X -> {
					ads *= 0.35f;
					zoom = 0.22f;
					sight = a;
				}
				case SCOPE_16X -> {
					ads *= 0.25f;
					zoom = 0.12f;
					sight = a;
				}
				case SILENCER -> {
					damage *= 0.9f;
					recoil *= 0.9f;
					silenced = true;
				}
				case MUZZLE_BOOST -> {
					interval *= 0.75f;
					damage *= 0.9f;
				}
				case MUZZLE_BRAKE -> {
					recoil *= 0.55f;
					damage *= 0.85f;
				}
				case LASER_SIGHT -> hip *= 0.6f;
				case EXTENDED_MAG -> {
				}
			}
		}

		float spread = aiming ? ads : hip;

		if (moving) {
			spread *= aiming ? 1.3f : 1.6f;
		}

		if (crouching) {
			spread *= 0.75f;
			recoil *= 0.8f;
		}

		if (aiming) {
			recoil *= 0.8f;
		}

		return new GunStats(Math.max(1, Math.round(interval)), damage, spread, recoil, magazine(def, mods), zoom, silenced, sight);
	}
}
