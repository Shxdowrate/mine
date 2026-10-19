package com.shxdowrate.solorust.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.shxdowrate.solorust.magic.ArtifactItem;
import com.shxdowrate.solorust.network.ArtifactPayload;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;

/**
 * Управление артефактами: пока артефакт в руке, клавиши 1–9 и колёсико выбирают ячейку способностей
 * (а не слот хотбара), ЛКМ и ПКМ применяют способности. Зажатый Alt возвращает обычный хотбар.
 */
public final class ArtifactClient {
	public static final KeyMapping FREE_HOTBAR = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.solorust.free_hotbar", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, SoloRustClient.CATEGORY));

	static int selected = 0;
	private static int lastInventorySlot = -1;
	private static Item lastItem = null;
	private static boolean wasAttackDown = false;

	private ArtifactClient() {
	}

	static void init() {
		// ЛКМ с артефактом не бьёт и не копает — она применяет способность.
		ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> holding(player));
		ClientTickEvents.START_CLIENT_TICK.register(ArtifactClient::startTick);
		ClientTickEvents.END_CLIENT_TICK.register(ArtifactClient::endTick);
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, com.shxdowrate.solorust.SoloRust.id("artifact"), ArtifactHud::render);
	}

	public static boolean holding(LocalPlayer player) {
		return player != null && player.getMainHandItem().getItem() instanceof ArtifactItem;
	}

	/** Клавиши выбора ячеек сейчас управляют способностями. */
	static boolean active(Minecraft mc) {
		return holding(mc.player) && mc.screen == null && !FREE_HOTBAR.isDown() && !mc.player.isSpectator();
	}

	private static void sync(LocalPlayer player) {
		ItemStack stack = player.getMainHandItem();
		int inventorySlot = player.getInventory().getSelectedSlot();

		if (inventorySlot != lastInventorySlot || stack.getItem() != lastItem) {
			lastInventorySlot = inventorySlot;
			lastItem = stack.getItem();

			if (stack.getItem() instanceof ArtifactItem) {
				selected = ArtifactItem.slot(stack);
			}
		}
	}

	private static void startTick(Minecraft mc) {
		LocalPlayer player = mc.player;

		if (player == null) {
			return;
		}

		sync(player);

		if (!active(mc)) {
			return;
		}

		// Забираем нажатия 1–9 раньше, чем ваниль переключит слот хотбара.
		for (int i = 0; i < 9; i++) {
			while (mc.options.keyHotbarSlots[i].consumeClick()) {
				select(player, i);
			}
		}
	}

	private static void endTick(Minecraft mc) {
		LocalPlayer player = mc.player;

		if (player == null) {
			wasAttackDown = false;
			return;
		}

		boolean attackDown = mc.options.keyAttack.isDown() && mc.screen == null;

		if (attackDown && !wasAttackDown && holding(player) && !player.isSpectator()) {
			ClientPlayNetworking.send(new ArtifactPayload(ArtifactPayload.CAST, 0));
		}

		wasAttackDown = attackDown;
	}

	private static void select(LocalPlayer player, int slot) {
		if (slot == selected) {
			return;
		}

		selected = slot;
		ClientPlayNetworking.send(new ArtifactPayload(ArtifactPayload.SELECT, slot));
		player.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.5f, 0.8f + slot * 0.08f);
	}

	private static double scrollAccumulator = 0;

	/** Колёсико мыши (из миксина). Возвращает true, если прокрутку забрал артефакт. */
	public static boolean onScroll(long handle, double xoffset, double yoffset) {
		Minecraft mc = Minecraft.getInstance();

		if (handle != mc.getWindow().handle() || mc.getOverlay() != null || !active(mc)) {
			scrollAccumulator = 0;
			return false;
		}

		boolean discrete = mc.options.discreteMouseScroll().get();
		double sensitivity = mc.options.mouseWheelSensitivity().get();
		double amount = (yoffset != 0 ? yoffset : -xoffset);
		amount = (discrete ? Math.signum(amount) : amount) * sensitivity;

		if (scrollAccumulator != 0 && Math.signum(amount) != Math.signum(scrollAccumulator)) {
			scrollAccumulator = 0;
		}

		scrollAccumulator += amount;
		int steps = (int) scrollAccumulator;

		if (steps != 0) {
			scrollAccumulator -= steps;
			// Как в ванили: прокрутка вверх — влево (к меньшему номеру).
			select(mc.player, Math.floorMod(selected - steps, 9));
		}

		return true;
	}
}
