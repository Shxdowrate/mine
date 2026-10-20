package com.shxdowrate.solorust.client.war;

import com.shxdowrate.solorust.SoloRust;
import com.shxdowrate.solorust.registry.ModWar;
import com.shxdowrate.solorust.suit.SuitTier;
import com.shxdowrate.solorust.war.WarData;
import com.shxdowrate.solorust.war.WardenShop;
import com.shxdowrate.solorust.war.WardenTabletItem;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

/** Клиент War Machines: рендер бандитов, планшет и счётчик денег. */
public final class WarClient {
	private static int shownCash = -1;
	private static int flashTicks;
	private static long lastFrameTick;

	private WarClient() {
	}

	public static void init() {
		EntityRendererRegistry.register(ModWar.BANDIT, BanditRenderer::new);
		WardenTabletItem.openScreen = () -> Minecraft.getInstance().setScreen(new WardenTabletScreen());
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, SoloRust.id("war_cash"), WarClient::cash);
	}

	/** Деньги в правом верхнем углу — пока надет костюм WARDEN или в руке планшет. */
	private static void cash(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;

		if (player == null || mc.options.hideGui) {
			return;
		}

		SuitTier tier = SuitTier.worn(player);
		boolean show = (tier != null && tier.isWarden())
				|| player.getMainHandItem().is(ModWar.WARDEN_TABLET) || player.getOffhandItem().is(ModWar.WARDEN_TABLET);

		if (!show) {
			return;
		}

		int cash = WarData.cash(player);
		long tick = player.level().getGameTime();

		if (shownCash >= 0 && cash > shownCash) {
			flashTicks = 20;
		}

		if (tick != lastFrameTick) {
			lastFrameTick = tick;

			if (flashTicks > 0) {
				flashTicks--;
			}
		}

		shownCash = cash;
		String text = WardenShop.fmt(cash) + " $";
		int x = graphics.guiWidth() - 8 - mc.font.width(text);
		int y = 8;
		graphics.fill(x - 4, y - 3, graphics.guiWidth() - 4, y + 11, 0x90000000);
		graphics.text(mc.font, text, x, y, flashTicks > 0 ? 0xFFB6FF6B : 0xFF7CFC7C, true);
	}
}
