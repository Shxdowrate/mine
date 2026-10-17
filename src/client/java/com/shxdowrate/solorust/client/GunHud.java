package com.shxdowrate.solorust.client;

import com.shxdowrate.solorust.gun.AmmoClass;
import com.shxdowrate.solorust.gun.Attachment;
import com.shxdowrate.solorust.gun.GunStats;
import com.shxdowrate.solorust.item.GunItem;
import com.shxdowrate.solorust.registry.ModComponents;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Счётчик патронов справа внизу и затемнение вокруг оптического прицела. */
public final class GunHud {
	private GunHud() {
	}

	public static void ammo(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;

		if (player == null || !(player.getMainHandItem().getItem() instanceof GunItem gun) || mc.options.hideGui) {
			return;
		}

		ItemStack stack = player.getMainHandItem();
		int mag = GunStats.magazine(gun.def(), GunStats.mods(stack));
		int ammo = stack.getOrDefault(ModComponents.AMMO, 0);
		Item ammoItem = loadedAmmo(stack, gun.def().ammo());
		int reserve = count(player.getInventory(), ammoItem);

		int right = graphics.guiWidth() - 10;
		int bottom = graphics.guiHeight() - 12;

		String big = ammo + " / " + mag;
		int color = ammo == 0 ? 0xFFE05A4F : 0xFFFFFFFF;
		graphics.fill(right - 118, bottom - 30, right + 4, bottom + 6, 0x80000000);
		graphics.text(mc.font, big, right - mc.font.width(big), bottom - 24, color, true);

		Component name = Component.translatable(ammoItem.getDescriptionId());
		String reserveText = "× " + reserve;
		graphics.text(mc.font, reserveText, right - mc.font.width(reserveText), bottom - 12, 0xFFE8C547, true);
		graphics.text(mc.font, name, right - 114, bottom - 12, 0xFFAAAAAA, true);

		if (player.getCooldowns().isOnCooldown(stack) && ammo < mag && player.getCooldowns().getCooldownPercent(stack, 0) > 0.2f) {
			Component reloading = Component.translatable("hud.solorust.reloading");
			graphics.text(mc.font, reloading, right - 114, bottom - 24, 0xFFE8C547, true);
		} else if (ammo == 0) {
			Component hint = Component.translatable("hud.solorust.press_reload", SoloRustClient.RELOAD.getTranslatedKeyMessage());
			graphics.text(mc.font, hint, right - 114, bottom - 24, 0xFFE05A4F, true);
		}
	}

	private static Item loadedAmmo(ItemStack stack, AmmoClass ammoClass) {
		String id = stack.getOrDefault(ModComponents.AMMO_TYPE, "");

		if (!id.isEmpty()) {
			Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));

			if (ammoClass.items().contains(item)) {
				return item;
			}
		}

		return ammoClass.items().get(0);
	}

	private static int count(Inventory inventory, Item item) {
		int total = 0;

		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i).is(item)) {
				total += inventory.getItem(i).getCount();
			}
		}

		return total;
	}

	/** Чёрная маска с круглым окном и перекрестьем, пока игрок смотрит в оптику. */
	public static void scope(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;

		if (player == null || !mc.options.getCameraType().isFirstPerson()) {
			return;
		}

		Attachment sight = SoloRustClient.aimSight(player);

		if (sight == null) {
			return;
		}

		int w = graphics.guiWidth();
		int h = graphics.guiHeight();
		int cx = w / 2;
		int cy = h / 2;

		if (sight == Attachment.HOLOSIGHT || sight == Attachment.SIMPLE_SIGHT) {
			// Красная точка коллиматора.
			int c = sight == Attachment.HOLOSIGHT ? 0xFFFF3030 : 0xFF30FF60;
			graphics.fill(cx - 1, cy - 1, cx + 1, cy + 1, c);
			return;
		}

		int r = (int) (Math.min(w, h) * 0.45);
		int black = 0xFF000000;

		graphics.fill(0, 0, w, cy - r, black);
		graphics.fill(0, cy + r, w, h, black);

		for (int y = cy - r; y < cy + r; y++) {
			double dy = (y + 0.5 - cy) / r;
			int half = (int) Math.round(Math.sqrt(Math.max(0, 1 - dy * dy)) * r);
			graphics.fill(0, y, cx - half, y + 1, black);
			graphics.fill(cx + half, y, w, y + 1, black);
		}

		// Перекрестье.
		graphics.fill(cx - r, cy, cx - 3, cy + 1, black);
		graphics.fill(cx + 3, cy, cx + r, cy + 1, black);
		graphics.fill(cx, cy - r, cx + 1, cy - 3, black);
		graphics.fill(cx, cy + 3, cx + 1, cy + r, black);
		graphics.fill(cx, cy, cx + 1, cy + 1, 0xFFFF2020);
	}
}
