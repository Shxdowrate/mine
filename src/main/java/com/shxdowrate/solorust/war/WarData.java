package com.shxdowrate.solorust.war;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.shxdowrate.solorust.SoloRust;
import com.shxdowrate.solorust.suit.SuitTier;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Player;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/**
 * Данные игрока в War Machines: деньги (Cash) и открытые костюмы WARDEN. Хранятся в сохранении игрока,
 * переживают смерть и синхронизируются с клиентом (для планшета и счётчика денег).
 */
public final class WarData {
	public static final AttachmentType<Integer> CASH = AttachmentRegistry.<Integer>builder()
			.persistent(Codec.INT)
			.copyOnDeath()
			.initializer(() -> 0)
			.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly())
			.buildAndRegister(SoloRust.id("war_cash"));

	/** id открытых костюмов WARDEN. Hot Rod и Steel открыты у всех с самого начала. */
	public static final AttachmentType<List<String>> OWNED = AttachmentRegistry.<List<String>>builder()
			.persistent(Codec.STRING.listOf())
			.copyOnDeath()
			.initializer(List::of)
			.syncWith(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), AttachmentSyncPredicate.targetOnly())
			.buildAndRegister(SoloRust.id("war_owned"));

	/** Игрок уже получил стартовый набор в мире War Machines. */
	public static final AttachmentType<Boolean> STARTED = AttachmentRegistry.<Boolean>builder()
			.persistent(Codec.BOOL)
			.copyOnDeath()
			.buildAndRegister(SoloRust.id("war_started"));

	public static final List<SuitTier> STARTER = List.of(SuitTier.W_HOT_ROD, SuitTier.W_STEEL);

	private WarData() {
	}

	public static void init() {
	}

	// ------------------------------------------------------------------ деньги

	public static int cash(Player player) {
		Integer value = player.getAttached(CASH);
		return value == null ? 0 : value;
	}

	public static void addCash(Player player, int amount) {
		if (amount == 0) {
			return;
		}

		long next = (long) cash(player) + amount;
		player.setAttached(CASH, (int) Math.max(0, Math.min(Integer.MAX_VALUE, next)));
	}

	/** Списывает деньги, если хватает. */
	public static boolean spend(Player player, int amount) {
		int have = cash(player);

		if (have < amount) {
			return false;
		}

		player.setAttached(CASH, have - amount);
		return true;
	}

	// ------------------------------------------------------------------ костюмы

	public static boolean owns(Player player, SuitTier tier) {
		if (STARTER.contains(tier)) {
			return true;
		}

		List<String> owned = player.getAttached(OWNED);
		return owned != null && owned.contains(tier.id);
	}

	public static void unlock(Player player, SuitTier tier) {
		if (owns(player, tier)) {
			return;
		}

		List<String> owned = player.getAttached(OWNED);
		List<String> next = new ArrayList<>(owned == null ? List.of() : owned);
		next.add(tier.id);
		player.setAttached(OWNED, List.copyOf(next));
	}

	public static int ownedCount(Player player) {
		int count = 0;

		for (SuitTier tier : SuitTier.wardenSuits()) {
			if (owns(player, tier)) {
				count++;
			}
		}

		return count;
	}
}
