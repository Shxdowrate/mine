package com.shxdowrate.solorust.client.helmet;

import com.shxdowrate.solorust.client.helmet.HelmetSettings.Option;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Меню шлема (клавиша J): что показывать и что подсвечивать. */
public class HelmetScreen extends Screen {
	public HelmetScreen() {
		super(Component.translatable("helmet.solorust.menu"));
	}

	@Override
	protected void init() {
		int columnWidth = 150;
		int gap = 8;
		int left = width / 2 - columnWidth - gap / 2;
		int right = width / 2 + gap / 2;
		int top = height / 2 - 100;

		addRenderableWidget(Button.builder(master(), b -> {
			HelmetHud.toggle();
			b.setMessage(master());
		}).bounds(width / 2 - columnWidth, top, columnWidth * 2, 20).build());

		Option[] show = {Option.STATS, Option.COMPASS, Option.TARGET};
		Option[] glow = {Option.HOSTILE, Option.PASSIVE, Option.PLAYERS, Option.ITEMS, Option.ORES, Option.CONTAINERS, Option.SPAWNERS};

		for (int i = 0; i < show.length; i++) {
			addToggle(show[i], left, top + 40 + i * 22, columnWidth);
		}

		for (int i = 0; i < glow.length; i++) {
			addToggle(glow[i], right, top + 40 + i * 22, columnWidth);
		}

		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
				.bounds(width / 2 - 100, top + 40 + 7 * 22 + 8, 200, 20).build());
	}

	private void addToggle(Option option, int x, int y, int w) {
		addRenderableWidget(Button.builder(label(option), b -> {
			option.toggle();
			b.setMessage(label(option));
		}).bounds(x, y, w, 20).build());
	}

	private static Component master() {
		return Component.translatable("helmet.solorust.option.enabled")
				.append(": ")
				.append(state(HelmetSettings.enabled()));
	}

	private static Component label(Option option) {
		return Component.translatable(option.translationKey()).append(": ").append(state(option.on()));
	}

	private static Component state(boolean on) {
		return on ? Component.translatable("helmet.solorust.on_short").withStyle(ChatFormatting.GREEN)
				: Component.translatable("helmet.solorust.off_short").withStyle(ChatFormatting.RED);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		int top = height / 2 - 100;
		graphics.centeredText(font, title, width / 2, top - 16, 0xFF4FD8FF);
		graphics.centeredText(font, Component.translatable("helmet.solorust.menu.show"), width / 2 - 79, top + 28, 0xFFAAAAAA);
		graphics.centeredText(font, Component.translatable("helmet.solorust.menu.glow"), width / 2 + 79, top + 28, 0xFFAAAAAA);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
