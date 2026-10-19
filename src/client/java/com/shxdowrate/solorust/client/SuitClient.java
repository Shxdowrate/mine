package com.shxdowrate.solorust.client;

import com.shxdowrate.solorust.SoloRust;
import com.shxdowrate.solorust.client.helmet.HelmetHud;
import com.shxdowrate.solorust.suit.SuitEnergy;
import com.shxdowrate.solorust.suit.SuitTier;
import com.shxdowrate.solorust.suit.SuitWeapon;
import com.shxdowrate.solorust.suit.SuitWeaponItem;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

/** Интерфейс шлема и форсаж в полёте. */
public final class SuitClient {
	private SuitClient() {
	}

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(SuitClient::tick);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR, SoloRust.id("suit_visor"), SuitClient::visor);
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, SoloRust.id("suit_hud"), SuitClient::hud);
	}

	// ------------------------------------------------------------------ форсаж

	/**
	 * Движение игрока считает клиент, поэтому разгон в полёте делаем здесь: зажат «бег» (Ctrl) — двигатели
	 * толкают по направлению взгляда до предельной скорости костюма. Энергию за форсаж списывает сервер.
	 */
	private static void tick(Minecraft client) {
		LocalPlayer player = client.player;

		if (player == null || client.isPaused()) {
			return;
		}

		SuitTier tier = SuitTier.worn(player);
		doubleJump(client, player, tier);

		if (tier == null || !tier.flight || !player.getAbilities().flying || !client.options.keySprint.isDown()) {
			return;
		}

		if (!player.hasInfiniteMaterials() && SuitEnergy.get(player, tier) <= 0) {
			return;
		}

		Vec3 look = player.getViewVector(1f);
		Vec3 velocity = player.getDeltaMovement().add(look.scale(tier.boost));

		if (velocity.length() > tier.maxBoostSpeed) {
			velocity = velocity.normalize().scale(tier.maxBoostSpeed);
		}

		player.setDeltaMovement(velocity);
	}

	private static boolean jumpWasDown = false;
	private static boolean doubleJumpUsed = false;

	/** «Призрак»: второй прыжок в воздухе. */
	private static void doubleJump(Minecraft client, LocalPlayer player, SuitTier tier) {
		boolean down = client.options.keyJump.isDown();

		if (player.onGround() || player.isInWater() || player.getAbilities().flying) {
			doubleJumpUsed = false;
		} else if (tier == SuitTier.SCOUT && down && !jumpWasDown && !doubleJumpUsed) {
			Vec3 v = player.getDeltaMovement();
			player.setDeltaMovement(v.x * 1.25, 0.62, v.z * 1.25);
			player.playSound(net.minecraft.sounds.SoundEvents.BREEZE_JUMP, 0.6f, 1.4f);
			doubleJumpUsed = true;
		}

		jumpWasDown = down;
	}

	// ------------------------------------------------------------------ HUD

	/** Уголки визора по краям экрана — видно, что ты «внутри шлема». */
	private static void visor(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;

		if (player == null || mc.options.hideGui || !mc.options.getCameraType().isFirstPerson()) {
			return;
		}

		SuitTier tier = SuitTier.worn(player);

		if (tier == null) {
			return;
		}

		int w = graphics.guiWidth();
		int h = graphics.guiHeight();
		int cx = w / 2;
		int cy = h / 2;

		if (HelmetHud.active()) {
			visorFrame(graphics, tier, w, h, cx, cy);
		}

		chargeBar(graphics, player, tier, cx, cy);
	}

	private static void visorFrame(GuiGraphicsExtractor graphics, SuitTier tier, int w, int h, int cx, int cy) {
		int c = (tier.hudColor & 0x00FFFFFF) | 0x70000000;
		int len = 28;
		int m = 6;

		// Четыре уголка.
		graphics.fill(m, m, m + len, m + 1, c);
		graphics.fill(m, m, m + 1, m + len, c);
		graphics.fill(w - m - len, m, w - m, m + 1, c);
		graphics.fill(w - m - 1, m, w - m, m + len, c);
		graphics.fill(m, h - m - 1, m + len, h - m, c);
		graphics.fill(m, h - m - len, m + 1, h - m, c);
		graphics.fill(w - m - len, h - m - 1, w - m, h - m, c);
		graphics.fill(w - m - 1, h - m - len, w - m, h - m, c);

		// Прицельная рамка вокруг перекрестья.
		int r = 9;
		int cc = (tier.hudColor & 0x00FFFFFF) | 0x90000000;
		graphics.fill(cx - r, cy - r, cx - r + 3, cy - r + 1, cc);
		graphics.fill(cx - r, cy - r, cx - r + 1, cy - r + 3, cc);
		graphics.fill(cx + r - 3, cy - r, cx + r, cy - r + 1, cc);
		graphics.fill(cx + r - 1, cy - r, cx + r, cy - r + 3, cc);
		graphics.fill(cx - r, cy + r - 1, cx - r + 3, cy + r, cc);
		graphics.fill(cx - r, cy + r - 3, cx - r + 1, cy + r, cc);
		graphics.fill(cx + r - 3, cy + r - 1, cx + r, cy + r, cc);
		graphics.fill(cx + r - 1, cy + r - 3, cx + r, cy + r, cc);
	}

	/** Шкала заряда для заряжаемого оружия. */
	private static void chargeBar(GuiGraphicsExtractor graphics, LocalPlayer player, SuitTier tier, int cx, int cy) {
		ItemStack using = player.getUseItem();

		if (player.isUsingItem() && using.getItem() instanceof SuitWeaponItem item && item.weapon().mode == SuitWeapon.Mode.CHARGE) {
			SuitWeapon weapon = item.weapon();
			int ticks = player.getTicksUsingItem();
			float fraction = Math.min(1f, (float) ticks / weapon.maxCharge);
			int bw = 40;
			int x = cx - bw / 2;
			int y = cy + 14;
			boolean ready = ticks >= weapon.minCharge;
			graphics.fill(x - 1, y - 1, x + bw + 1, y + 3, 0x90000000);
			graphics.fill(x, y, x + Math.round(bw * fraction), y + 2, ready ? (tier.hudColor | 0xFF000000) : 0xFF777777);
		}
	}

	/** Энергия, режим полёта и выбранная система — слева вверху. */
	private static void hud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;

		if (player == null || mc.options.hideGui) {
			return;
		}

		SuitTier tier = SuitTier.worn(player);

		if (tier == null) {
			return;
		}

		int color = tier.hudColor | 0xFF000000;
		int energy = SuitEnergy.get(player, tier);
		float fraction = (float) energy / tier.maxEnergy;
		int x = 10;
		int y = 10;
		int barWidth = 100;

		graphics.fill(x - 4, y - 4, x + barWidth + 4, y + 40, 0x70000000);

		Component name = Component.translatable(tier.translationKey());
		graphics.text(mc.font, name, x, y, color, true);

		int barY = y + 12;
		int barColor = fraction < 0.2f ? 0xFFE05A4F : color;
		graphics.fill(x, barY, x + barWidth, barY + 5, 0xFF1A1A1A);
		graphics.fill(x, barY, x + Math.round(barWidth * fraction), barY + 5, barColor);

		String percent = player.hasInfiniteMaterials() ? "∞" : Math.round(fraction * 100) + "%";
		graphics.text(mc.font, percent, x + barWidth - mc.font.width(percent), y, 0xFFFFFFFF, true);

		// Состояние двигателей.
		Component mode;

		if (!tier.flight) {
			mode = Component.translatable("hud.solorust.suit.ground");
		} else if (player.getAbilities().flying) {
			mode = Component.translatable(mc.options.keySprint.isDown() ? "hud.solorust.suit.boost" : "hud.solorust.suit.flying");
		} else {
			mode = Component.translatable("hud.solorust.suit.flight_ready");
		}

		graphics.text(mc.font, mode, x, barY + 9, 0xFFAAAAAA, true);

		// Выбранная система.
		ItemStack held = player.getMainHandItem();

		if (held.getItem() instanceof SuitWeaponItem item) {
			Component weapon = Component.translatable(held.getItem().getDescriptionId());
			boolean cooling = player.getCooldowns().isOnCooldown(held);
			int weaponColor = cooling ? 0xFF777777 : 0xFFFFFFFF;
			graphics.text(mc.font, weapon, x, barY + 20, weaponColor, true);

			String cost = "⚡" + item.weapon().energy;
			graphics.text(mc.font, cost, x + barWidth - mc.font.width(cost), barY + 20, energy >= item.weapon().energy ? 0xFF8FD8FF : 0xFFE05A4F, true);
		} else {
			graphics.text(mc.font, Component.translatable("hud.solorust.suit.pick_weapon"), x, barY + 20, 0xFF666666, true);
		}
	}
}
