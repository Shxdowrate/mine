package com.shxdowrate.solorust.war;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.shxdowrate.solorust.registry.ModWar;
import com.shxdowrate.solorust.suit.SuitTier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Откуда берутся деньги: полёт в костюме WARDEN, убийства бандитов и монстров, зачистка лагерей.
 * И стартовый набор при первом входе в мир War Machines.
 */
public final class WarRewards {
	/** Блоков полёта за 1 Cash и предел дохода за секунду. */
	private static final double BLOCKS_PER_CASH = 8.0;
	private static final int MAX_FLIGHT_CASH_PER_SECOND = 10;
	/** Награда за обычного монстра, убитого в костюме WARDEN. */
	private static final int MONSTER_REWARD = 15;

	private static final Map<UUID, Vec3> LAST_POS = new HashMap<>();
	private static final Map<UUID, Double> FLOWN = new HashMap<>();

	private WarRewards() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(WarRewards::tick);

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof Bandit || !(entity instanceof Enemy)) {
				return;
			}

			if (source.getEntity() instanceof ServerPlayer player && wearsWarden(player)) {
				reward(player, MONSTER_REWARD, entity.getName());
			}
		});

		ServerPlayerEvents.JOIN.register(WarRewards::starterKit);
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> LAST_POS.remove(newPlayer.getUUID()));
		ServerPlayerEvents.LEAVE.register(player -> {
			LAST_POS.remove(player.getUUID());
			FLOWN.remove(player.getUUID());
		});
	}

	public static void reward(ServerPlayer player, int amount, Component what) {
		WarData.addCash(player, amount);
		player.sendSystemMessage(Component.literal("+" + WardenShop.fmt(amount) + " $  ").withStyle(ChatFormatting.GREEN)
				.append(what.copy().withStyle(ChatFormatting.GRAY)), true);
	}

	static boolean wearsWarden(ServerPlayer player) {
		SuitTier tier = SuitTier.worn(player);
		return tier != null && tier.isWarden();
	}

	// ------------------------------------------------------------------ доход за полёт

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			UUID id = player.getUUID();
			Vec3 pos = player.position();
			Vec3 last = LAST_POS.put(id, pos);

			if (last == null || !wearsWarden(player) || !player.getAbilities().flying || player.isCreative() || player.isSpectator()) {
				continue;
			}

			double distance = Math.min(pos.distanceTo(last), MAX_FLIGHT_CASH_PER_SECOND * BLOCKS_PER_CASH);

			// Телепорт или смена измерения — не полёт.
			if (distance > 120) {
				continue;
			}

			double flown = FLOWN.getOrDefault(id, 0.0) + distance;
			int cash = (int) (flown / BLOCKS_PER_CASH);
			FLOWN.put(id, flown - cash * BLOCKS_PER_CASH);

			if (cash > 0) {
				WarData.addCash(player, cash);
			}
		}
	}

	// ------------------------------------------------------------------ стартовый набор

	private static void starterKit(ServerPlayer player) {
		if (!WarWorld.is(player.level()) || Boolean.TRUE.equals(player.getAttached(WarData.STARTED))) {
			return;
		}

		player.setAttached(WarData.STARTED, true);
		ItemStack tablet = new ItemStack(ModWar.WARDEN_TABLET);

		if (!player.getInventory().add(tablet)) {
			player.drop(tablet, false);
		}

		// Костюм прилетает с неба, как только игрок появился.
		WardenShop.equip(player, SuitTier.W_HOT_ROD.ordinal());
		player.sendSystemMessage(Component.translatable("message.solorust.war.welcome").withStyle(ChatFormatting.GOLD));
		player.sendSystemMessage(Component.translatable("message.solorust.war.welcome_hint").withStyle(ChatFormatting.GRAY));
	}
}
