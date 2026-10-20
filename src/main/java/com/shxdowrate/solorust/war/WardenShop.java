package com.shxdowrate.solorust.war;

import java.util.ArrayList;
import java.util.List;

import com.shxdowrate.solorust.suit.SuitArmorItem;
import com.shxdowrate.solorust.suit.SuitAssembly;
import com.shxdowrate.solorust.suit.SuitTier;
import com.shxdowrate.solorust.suit.WardenRarity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Магазин планшета WARDEN: коробки с костюмами (как спиннер в War Machines) и гардероб —
 * открытый костюм надевается в любой момент, прилетая с неба.
 */
public final class WardenShop {
	/**
	 * Коробка: цена и шансы редкостей (в процентах, по порядку {@link WardenRarity}).
	 * За повтор возвращается часть цены.
	 */
	public enum Box {
		BASIC("basic", 2_500, new int[] {70, 30, 0, 0, 0, 0}),
		ADVANCED("advanced", 15_000, new int[] {0, 25, 50, 25, 0, 0}),
		ELITE("elite", 75_000, new int[] {0, 0, 0, 40, 45, 15});

		public final String id;
		public final int price;
		public final int[] chances;

		Box(String id, int price, int[] chances) {
			this.id = id;
			this.price = price;
			this.chances = chances;
		}

		public int refund() {
			return price * 3 / 10;
		}

		public String translationKey() {
			return "warden.solorust.box." + id;
		}
	}

	private WardenShop() {
	}

	// ================================================================== коробки

	public static void openBox(ServerPlayer player, int boxIndex) {
		if (boxIndex < 0 || boxIndex >= Box.values().length) {
			return;
		}

		Box box = Box.values()[boxIndex];
		boolean free = player.hasInfiniteMaterials();

		if (!free && !WarData.spend(player, box.price)) {
			player.sendSystemMessage(Component.translatable("message.solorust.war.no_cash", fmt(box.price - WarData.cash(player)))
					.withStyle(ChatFormatting.RED), true);
			return;
		}

		RandomSource random = player.getRandom();
		WardenRarity rarity = roll(box, random);
		List<SuitTier> pool = new ArrayList<>();

		for (SuitTier tier : SuitTier.wardenSuits()) {
			if (tier.warden == rarity) {
				pool.add(tier);
			}
		}

		SuitTier won = pool.get(random.nextInt(pool.size()));
		ServerLevel level = player.level();
		Component name = Component.translatable(won.translationKey()).withColor(won.warden.color & 0xFFFFFF);
		Component rarityName = Component.translatable(won.warden.translationKey()).withColor(won.warden.color & 0xFFFFFF);

		if (WarData.owns(player, won)) {
			WarData.addCash(player, box.refund());
			player.sendSystemMessage(Component.translatable("message.solorust.war.box_duplicate", name, rarityName, fmt(box.refund()))
					.withStyle(ChatFormatting.GRAY));
			level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 0.8f);
		} else {
			WarData.unlock(player, won);
			player.sendSystemMessage(Component.translatable("message.solorust.war.box_new", name, rarityName).withStyle(ChatFormatting.GOLD));
			level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS,
					rarity.ordinal() >= WardenRarity.LEGENDARY.ordinal() ? 1.0f : 0.5f, 1.0f);
			level.sendParticles(rarity.ordinal() >= WardenRarity.EPIC.ordinal() ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.HAPPY_VILLAGER,
					player.getX(), player.getY() + 1, player.getZ(), 40, 0.5, 0.8, 0.5, 0.3);
		}
	}

	private static WardenRarity roll(Box box, RandomSource random) {
		int r = random.nextInt(100);
		int sum = 0;

		for (WardenRarity rarity : WardenRarity.values()) {
			sum += box.chances[rarity.ordinal()];

			if (r < sum) {
				return rarity;
			}
		}

		return WardenRarity.COMMON;
	}

	// ================================================================== гардероб

	public static void equip(ServerPlayer player, int tierOrdinal) {
		if (tierOrdinal < 0 || tierOrdinal >= SuitTier.values().length) {
			return;
		}

		SuitTier tier = SuitTier.values()[tierOrdinal];

		if (!tier.isWarden() || !(WarData.owns(player, tier) || player.hasInfiniteMaterials())) {
			return;
		}

		if (SuitAssembly.busy(player)) {
			return;
		}

		// Старый костюм WARDEN просто исчезает — он остаётся в гардеробе.
		removeWardenItems(player);

		Vec3 sky = player.position().add(0, 24, 0);
		SuitAssembly.start(player, SuitAssembly.Style.RECALL, SuitAssembly.freshSuit(tier, -1), sky);
		player.sendSystemMessage(Component.translatable("message.solorust.war.suit_incoming",
				Component.translatable(tier.translationKey()).withColor(tier.warden.color & 0xFFFFFF)), true);
	}

	public static void unequip(ServerPlayer player) {
		if (SuitAssembly.busy(player)) {
			return;
		}

		if (removeWardenItems(player) > 0) {
			ServerLevel level = player.level();
			level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1, player.getZ(), 20, 0.4, 0.8, 0.4, 0.05);
			level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8f, 1.4f);
		}
	}

	/** Убирает все части костюмов WARDEN из слотов брони и инвентаря. */
	static int removeWardenItems(ServerPlayer player) {
		int removed = 0;

		for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			if (isWarden(player.getItemBySlot(slot))) {
				player.setItemSlot(slot, ItemStack.EMPTY);
				removed++;
			}
		}

		Inventory inventory = player.getInventory();

		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (isWarden(inventory.getItem(i))) {
				inventory.setItem(i, ItemStack.EMPTY);
				removed++;
			}
		}

		return removed;
	}

	public static boolean isWarden(ItemStack stack) {
		return stack.getItem() instanceof SuitArmorItem armor && armor.tier().isWarden();
	}

	public static String fmt(int value) {
		return String.format(java.util.Locale.ROOT, "%,d", value).replace(',', ' ');
	}
}
