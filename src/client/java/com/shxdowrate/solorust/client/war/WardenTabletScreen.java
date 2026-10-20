package com.shxdowrate.solorust.client.war;

import java.util.ArrayList;
import java.util.List;

import com.shxdowrate.solorust.network.WardenPayload;
import com.shxdowrate.solorust.registry.ModSuits;
import com.shxdowrate.solorust.suit.SuitTier;
import com.shxdowrate.solorust.suit.SuitWeapon;
import com.shxdowrate.solorust.suit.WardenRarity;
import com.shxdowrate.solorust.war.WarData;
import com.shxdowrate.solorust.war.WardenShop;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Планшет WARDEN: слева гардероб (вкладки по редкости, открытые костюмы надеваются кликом),
 * справа коробки с костюмами и баланс.
 */
public class WardenTabletScreen extends Screen {
	private static final int PANEL_W = 360;
	private static final int PANEL_H = 236;

	private static WardenRarity selected = WardenRarity.COMMON;

	private int lastCash = -1;
	private int lastOwned = -1;
	private final List<SuitRow> rows = new ArrayList<>();

	private record SuitRow(SuitTier tier, int x, int y, boolean owned) {
	}

	public WardenTabletScreen() {
		super(Component.translatable("warden.solorust.tablet.title"));
	}

	private int left() {
		return (width - PANEL_W) / 2;
	}

	private int top() {
		return (height - PANEL_H) / 2;
	}

	@Override
	protected void init() {
		rows.clear();
		int x0 = left() + 8;
		int y0 = top() + 8;
		var player = minecraft.player;

		if (player == null) {
			return;
		}

		lastCash = WarData.cash(player);
		lastOwned = WarData.ownedCount(player);

		// Вкладки редкостей.
		WardenRarity[] rarities = WardenRarity.values();
		int tabW = 30;

		for (int i = 0; i < rarities.length; i++) {
			WardenRarity rarity = rarities[i];
			Component label = Component.translatable(rarity.translationKey() + ".short").withColor(rarity.color & 0xFFFFFF);
			Button tab = Button.builder(label, b -> {
				selected = rarity;
				rebuildWidgets();
			}).bounds(x0 + i * (tabW + 2), y0 + 18, tabW, 16)
					.tooltip(Tooltip.create(Component.translatable(rarity.translationKey()).withColor(rarity.color & 0xFFFFFF)))
					.build();
			tab.active = rarity != selected;
			addRenderableWidget(tab);
		}

		// Костюмы выбранной редкости.
		int y = y0 + 40;

		for (SuitTier tier : SuitTier.wardenSuits()) {
			if (tier.warden != selected) {
				continue;
			}

			boolean owned = WarData.owns(player, tier) || player.hasInfiniteMaterials();
			Component name = owned
					? Component.translatable(tier.translationKey()).withColor(tier.hudColor & 0xFFFFFF)
					: Component.translatable("warden.solorust.tablet.locked").withStyle(ChatFormatting.DARK_GRAY);

			Button button = Button.builder(name, b -> {
				ClientPlayNetworking.send(new WardenPayload(WardenPayload.EQUIP, tier.ordinal()));
				onClose();
			}).bounds(x0 + 20, y, 172, 20).tooltip(Tooltip.create(describe(tier, owned))).build();
			button.active = owned;
			addRenderableWidget(button);
			rows.add(new SuitRow(tier, x0, y + 2, owned));
			y += 22;
		}

		// Коробки.
		int bx = left() + 206;
		int by = y0 + 18;

		for (WardenShop.Box box : WardenShop.Box.values()) {
			boolean canPay = lastCash >= box.price || player.hasInfiniteMaterials();
			Component label = Component.translatable("warden.solorust.tablet.open", WardenShop.fmt(box.price));
			Button button = Button.builder(label, b -> ClientPlayNetworking.send(new WardenPayload(WardenPayload.OPEN_BOX, box.ordinal())))
					.bounds(bx, by + 22, 146, 18)
					.tooltip(Tooltip.create(chances(box)))
					.build();
			button.active = canPay;
			addRenderableWidget(button);
			by += 50;
		}

		addRenderableWidget(Button.builder(Component.translatable("warden.solorust.tablet.unequip"),
				b -> ClientPlayNetworking.send(new WardenPayload(WardenPayload.UNEQUIP, 0)))
				.bounds(bx, top() + PANEL_H - 50, 146, 18).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
				.bounds(bx, top() + PANEL_H - 28, 146, 18).build());
	}

	private static Component describe(SuitTier tier, boolean owned) {
		MutableComponent text = Component.translatable(tier.translationKey()).withColor(tier.hudColor & 0xFFFFFF)
				.append(Component.literal("\n"))
				.append(Component.translatable(tier.warden.translationKey()).withColor(tier.warden.color & 0xFFFFFF))
				.append(Component.literal("\n"))
				.append(Component.translatable("warden.solorust.tablet.stats", tier.maxEnergy, Math.round(tier.power * 100))
						.withStyle(ChatFormatting.GRAY));

		for (SuitWeapon weapon : tier.weapons) {
			text.append(Component.literal("\n• ").withStyle(ChatFormatting.DARK_AQUA))
					.append(Component.translatable("item.solorust." + weapon.itemId()).withStyle(ChatFormatting.AQUA));
		}

		if (!owned) {
			text.append(Component.literal("\n")).append(Component.translatable("warden.solorust.tablet.locked_hint").withStyle(ChatFormatting.RED));
		}

		return text;
	}

	private static Component chances(WardenShop.Box box) {
		MutableComponent text = Component.translatable(box.translationKey()).withStyle(ChatFormatting.GOLD);

		for (WardenRarity rarity : WardenRarity.values()) {
			int chance = box.chances[rarity.ordinal()];

			if (chance > 0) {
				text.append(Component.literal("\n" + chance + "% ").withStyle(ChatFormatting.GRAY))
						.append(Component.translatable(rarity.translationKey()).withColor(rarity.color & 0xFFFFFF));
			}
		}

		text.append(Component.literal("\n")).append(Component.translatable("warden.solorust.tablet.refund", WardenShop.fmt(box.refund()))
				.withStyle(ChatFormatting.DARK_GRAY));
		return text;
	}

	@Override
	public void tick() {
		super.tick();
		var player = minecraft.player;

		// Деньги и коллекция приходят с сервера — обновляем кнопки, когда они поменялись.
		if (player != null && (WarData.cash(player) != lastCash || WarData.ownedCount(player) != lastOwned)) {
			rebuildWidgets();
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		int x = left();
		int y = top();

		graphics.fill(x, y, x + PANEL_W, y + PANEL_H, 0xE0101418);
		graphics.outline(x, y, PANEL_W, PANEL_H, 0xFF3FA9F5);
		graphics.fill(x + 200, y + 14, x + 201, y + PANEL_H - 6, 0xFF2A3A48);

		super.extractRenderState(graphics, mouseX, mouseY, a);

		graphics.text(font, title, x + 8, y + 6, 0xFF3FA9F5, true);
		Component cash = Component.literal(WardenShop.fmt(lastCash) + " $");
		graphics.text(font, cash, x + PANEL_W - 8 - font.width(cash), y + 6, 0xFF7CFC7C, true);

		var player = minecraft.player;
		Component collection = Component.translatable("warden.solorust.tablet.collection",
				player == null ? 0 : WarData.ownedCount(player), SuitTier.wardenSuits().size());
		graphics.text(font, collection, x + 8, y + PANEL_H - 14, 0xFF8A97A3, false);

		for (SuitRow row : rows) {
			ItemStack icon = new ItemStack(ModSuits.piece(row.tier(), ArmorType.HELMET));
			graphics.item(icon, row.x(), row.y());

			if (!row.owned()) {
				graphics.fill(row.x(), row.y(), row.x() + 16, row.y() + 16, 0xB0101418);
			}
		}

		int by = y + 26;

		for (WardenShop.Box box : WardenShop.Box.values()) {
			graphics.text(font, Component.translatable(box.translationKey()), x + 206, by, 0xFFFFC94D, true);
			graphics.text(font, Component.translatable(box.translationKey() + ".desc"), x + 206, by + 11, 0xFF9AA5B0, false);
			by += 50;
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
