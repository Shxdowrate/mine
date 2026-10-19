package com.shxdowrate.solorust.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.shxdowrate.solorust.SoloRust;
import com.shxdowrate.solorust.client.helmet.HelmetHud;
import com.shxdowrate.solorust.client.helmet.HelmetScreen;
import com.shxdowrate.solorust.client.helmet.HelmetSettings;
import com.shxdowrate.solorust.gun.Attachment;
import com.shxdowrate.solorust.gun.GunStats;
import com.shxdowrate.solorust.item.GunItem;
import com.shxdowrate.solorust.network.GunActionPayload;
import com.shxdowrate.solorust.network.RecoilPayload;
import com.shxdowrate.solorust.network.SuitActionPayload;
import com.shxdowrate.solorust.registry.ModComponents;
import com.shxdowrate.solorust.registry.ModSounds;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;

public class SoloRustClient implements ClientModInitializer {
	static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(SoloRust.id("controls"));
	public static final KeyMapping RELOAD = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.solorust.reload", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY));
	public static final KeyMapping HELMET_TOGGLE = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.solorust.helmet_toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY));
	public static final KeyMapping SUIT_TOGGLE = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.solorust.suit_toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY));
	public static final KeyMapping SUIT_RECALL = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.solorust.suit_recall", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY));
	public static final KeyMapping HELMET_MENU = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.solorust.helmet_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY));

	private static boolean wasAttackDown = false;
	private static float pendingPitch = 0;
	private static float pendingYaw = 0;

	@Override
	public void onInitializeClient() {
		// ЛКМ с оружием в руке — не бьём и не копаем, а стреляем.
		ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> holdsGun(player));

		ClientTickEvents.END_CLIENT_TICK.register(SoloRustClient::tick);

		ClientPlayNetworking.registerGlobalReceiver(RecoilPayload.TYPE, (payload, context) -> {
			pendingPitch += payload.pitch();
			pendingYaw += payload.yaw();

			if (payload.hit()) {
				context.player().playSound(ModSounds.HIT_MARKER, 0.6f, 1.0f);
			}
		});

		HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR, SoloRust.id("scope"), GunHud::scope);
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, SoloRust.id("ammo"), GunHud::ammo);

		ArtifactClient.init();

		SuitClient.init();
		HelmetSettings.load();
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (HELMET_TOGGLE.consumeClick()) {
				if (client.player != null && HelmetHud.helmet(client.player) != null) {
					HelmetHud.toggle();
				}
			}

			while (SUIT_TOGGLE.consumeClick()) {
				if (client.player != null) {
					ClientPlayNetworking.send(new SuitActionPayload(SuitActionPayload.TOGGLE));
				}
			}

			while (SUIT_RECALL.consumeClick()) {
				if (client.player != null) {
					ClientPlayNetworking.send(new SuitActionPayload(SuitActionPayload.RECALL));
				}
			}

			while (HELMET_MENU.consumeClick()) {
				if (client.player != null && client.screen == null) {
					client.setScreen(new HelmetScreen());
				}
			}

			HelmetHud.tick(client);
		});
		HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR, SoloRust.id("helmet"), HelmetHud::render);
	}

	public static boolean holdsGun(LocalPlayer player) {
		return player != null && player.getMainHandItem().getItem() instanceof GunItem;
	}

	private static void tick(Minecraft client) {
		LocalPlayer player = client.player;

		if (player == null) {
			return;
		}

		// Плавная отдача: камера уходит вверх за 2–3 тика.
		if (Math.abs(pendingPitch) > 0.01f || Math.abs(pendingYaw) > 0.01f) {
			float dp = pendingPitch * 0.55f;
			float dy = pendingYaw * 0.55f;
			player.setXRot(Math.max(-90f, player.getXRot() - dp));
			player.setYRot(player.getYRot() + dy);
			pendingPitch -= dp;
			pendingYaw -= dy;
		}

		while (RELOAD.consumeClick()) {
			if (holdsGun(player)) {
				ClientPlayNetworking.send(new GunActionPayload(GunActionPayload.RELOAD));
			}
		}

		boolean attackDown = client.options.keyAttack.isDown() && client.screen == null;
		ItemStack stack = player.getMainHandItem();

		if (attackDown && stack.getItem() instanceof GunItem gun) {
			boolean pressedNow = !wasAttackDown;

			// Автомат шлёт выстрел каждый тик, пока зажата ЛКМ (сервер сам держит темп), полуавтомат — только на нажатие.
			if (gun.def().automatic() || pressedNow) {
				ClientPlayNetworking.send(new GunActionPayload(GunActionPayload.FIRE));
			}
		}

		wasAttackDown = attackDown;
	}

	/** Множитель поля зрения при прицеливании (используется миксином). */
	public static float aimZoom(LocalPlayer player) {
		ItemStack stack = player.getUseItem();

		if (!(stack.getItem() instanceof GunItem gun)) {
			return 1f;
		}

		return GunStats.of(gun.def(), stack, player).zoom();
	}

	public static Attachment aimSight(LocalPlayer player) {
		if (!GunStats.isAiming(player)) {
			return null;
		}

		ItemStack stack = player.getUseItem();
		return Attachment.byId(stack.getOrDefault(ModComponents.MODS, com.shxdowrate.solorust.gun.GunMods.EMPTY).sight());
	}
}
