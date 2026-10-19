package com.shxdowrate.solorust.client;

import com.shxdowrate.solorust.magic.Ability;
import com.shxdowrate.solorust.magic.ArtifactItem;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Панель способностей справа: 9 строк «№ · ЛКМ · ПКМ», выбранная строка подсвечена,
 * на перезарядке — серым с секундами, закрытые (нет камня) — тёмным.
 */
final class ArtifactHud {
	private static final int ROW = 12;

	private ArtifactHud() {
	}

	static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;

		if (player == null || mc.options.hideGui || mc.level == null || !(player.getMainHandItem().getItem() instanceof ArtifactItem artifact)) {
			return;
		}

		ItemStack stack = player.getMainHandItem();
		Font font = mc.font;
		long now = mc.level.getGameTime();
		boolean free = ArtifactClient.FREE_HOTBAR.isDown();
		int selected = ArtifactClient.selected;

		// Ширина колонок по самым длинным названиям.
		int leftWidth = 0;
		int rightWidth = 0;

		for (int slot = 0; slot < ArtifactItem.SLOTS; slot++) {
			leftWidth = Math.max(leftWidth, font.width(name(artifact.ability(slot, 0))) + 18);
			rightWidth = Math.max(rightWidth, font.width(name(artifact.ability(slot, 1))) + 18);
		}

		int width = 14 + leftWidth + 10 + rightWidth;
		int height = ArtifactItem.SLOTS * ROW;
		int x = graphics.guiWidth() - width - 6;
		int y = graphics.guiHeight() / 2 - height / 2 - 6;

		// Заголовок.
		Component title = stack.getHoverName();
		graphics.fill(x - 3, y - 14, x + width + 3, y + height + 15, free ? 0x50000000 : 0x90000000);
		graphics.text(font, title, x, y - 11, 0xFFFFC94A, true);
		Component mouse = Component.translatable("hud.solorust.artifact.mouse");
		graphics.text(font, mouse, x + width - font.width(mouse), y - 11, 0xFF8A8A8A, true);

		for (int slot = 0; slot < ArtifactItem.SLOTS; slot++) {
			int ry = y + slot * ROW;
			boolean unlocked = artifact.unlocked(stack, slot);
			int color = 0xFF000000 | artifact.slotColor(slot);

			if (slot == selected && !free) {
				graphics.fill(x - 2, ry - 1, x + width + 2, ry + ROW - 2, 0x40FFFFFF);
				graphics.fill(x - 3, ry - 1, x - 1, ry + ROW - 2, color);
			}

			graphics.text(font, String.valueOf(slot + 1), x + 1, ry + 1, unlocked ? color : 0xFF555555, true);
			cell(graphics, font, artifact, stack, slot, 0, x + 12, ry + 1, unlocked, now, slot == selected && !free);
			graphics.text(font, "│", x + 12 + leftWidth, ry + 1, 0xFF444444, false);
			cell(graphics, font, artifact, stack, slot, 1, x + 22 + leftWidth, ry + 1, unlocked, now, slot == selected && !free);
		}

		Component hint = Component.translatable(free ? "hud.solorust.artifact.free" : "hud.solorust.artifact.hint",
				ArtifactClient.FREE_HOTBAR.getTranslatedKeyMessage());
		graphics.text(font, hint, x, y + height + 3, free ? 0xFFFFC94A : 0xFF8A8A8A, true);

		if (!artifact.unlocked(stack, selected) && !free) {
			Component reason = artifact.lockReason(stack, selected);
			int w = font.width(reason);
			graphics.text(font, reason, graphics.guiWidth() / 2 - w / 2, graphics.guiHeight() - 72, 0xFFAAAAAA, true);
		}
	}

	private static void cell(GuiGraphicsExtractor graphics, Font font, ArtifactItem artifact, ItemStack stack, int slot, int button,
			int x, int y, boolean unlocked, long now, boolean selected) {
		Ability ability = artifact.ability(slot, button);
		long left = ArtifactItem.cooldownLeft(stack, slot, button, now);
		int color;

		if (!unlocked) {
			color = 0xFF505050;
		} else if (left > 0) {
			color = 0xFF8A8A8A;
		} else {
			color = selected ? 0xFFFFFFFF : 0xFFD0D0D0;
		}

		graphics.text(font, name(ability), x, y, color, true);

		if (unlocked && left > 0) {
			Component seconds = left >= 1200
					? Component.translatable("hud.solorust.cd_minutes", (left + 1199) / 1200)
					: Component.translatable("hud.solorust.cd_seconds", (left + 19) / 20);
			graphics.text(font, seconds, x + font.width(name(ability)) + 3, y, 0xFFE05A4F, true);
		}
	}

	private static Component name(Ability ability) {
		return Component.translatable(ability.translationKey());
	}
}
