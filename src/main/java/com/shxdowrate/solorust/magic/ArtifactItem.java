package com.shxdowrate.solorust.magic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.shxdowrate.solorust.registry.ModComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Артефакт с 18 способностями: 9 ячеек (выбираются клавишами 1–9 или колёсиком, пока артефакт в руке),
 * в каждой — способность на ЛКМ и на ПКМ.
 */
public abstract class ArtifactItem extends Item {
	public static final int SLOTS = 9;

	protected ArtifactItem(Properties properties) {
		super(properties);
	}

	/** button: 0 — ЛКМ, 1 — ПКМ. */
	public abstract Ability ability(int slot, int button);

	/** Цвет ячейки на панели (RGB). */
	public abstract int slotColor(int slot);

	/** Ключ названия ячейки: {@code artifact.solorust.<key>.slot<N>}. */
	public abstract String key();

	/** Доступна ли ячейка (у перчатки — есть ли нужные камни). */
	public boolean unlocked(ItemStack stack, int slot) {
		return true;
	}

	/** Сообщение, почему ячейка закрыта. */
	public Component lockReason(ItemStack stack, int slot) {
		return Component.empty();
	}

	/** Даёт ли артефакт полёт (пока лежит в инвентаре). */
	public abstract boolean grantsFlight(ItemStack stack);

	public static int slot(ItemStack stack) {
		return Mth.clamp(stack.getOrDefault(ModComponents.ABILITY_SLOT, 0), 0, SLOTS - 1);
	}

	/** Сколько тиков осталось до готовности способности (0 — готова). */
	public static long cooldownLeft(ItemStack stack, int slot, int button, long now) {
		List<Long> cds = stack.getOrDefault(ModComponents.ABILITY_COOLDOWNS, List.of());
		int index = slot * 2 + button;
		return index < cds.size() ? Math.max(0, cds.get(index) - now) : 0;
	}

	// ------------------------------------------------------------------ сервер

	public static void select(ServerPlayer player, int slot) {
		ItemStack stack = player.getMainHandItem();

		if (stack.getItem() instanceof ArtifactItem) {
			stack.set(ModComponents.ABILITY_SLOT, Mth.clamp(slot, 0, SLOTS - 1));
		}
	}

	public static void cast(ServerPlayer player, int button) {
		ItemStack stack = player.getMainHandItem();

		if (stack.getItem() instanceof ArtifactItem artifact && (button == 0 || button == 1)) {
			artifact.tryCast(player, stack, button);
		}
	}

	private void tryCast(ServerPlayer player, ItemStack stack, int button) {
		if (player.isSpectator() || !player.isAlive()) {
			return;
		}

		int slot = slot(stack);

		if (!unlocked(stack, slot)) {
			player.sendOverlayMessage(lockReason(stack, slot));
			return;
		}

		ServerLevel level = player.level();
		long now = level.getGameTime();

		if (cooldownLeft(stack, slot, button, now) > 0) {
			return;
		}

		Ability ability = ability(slot, button);

		if (!ability.caster().cast(level, player, stack)) {
			return;
		}

		int cd = player.hasInfiniteMaterials() ? Math.min(ability.cooldown(), 10) : ability.cooldown();
		List<Long> cds = new ArrayList<>(stack.getOrDefault(ModComponents.ABILITY_COOLDOWNS, List.of()));

		while (cds.size() < SLOTS * 2) {
			cds.add(0L);
		}

		cds.set(slot * 2 + button, now + cd);
		stack.set(ModComponents.ABILITY_COOLDOWNS, List.copyOf(cds));
		player.swing(InteractionHand.MAIN_HAND, true);
	}

	// ------------------------------------------------------------------ ПКМ

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}

		if (player instanceof ServerPlayer serverPlayer) {
			cast(serverPlayer, 1);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Component.translatable("tooltip.solorust.artifact.select").withStyle(ChatFormatting.GRAY));
		builder.accept(Component.translatable("tooltip.solorust.artifact.cast").withStyle(ChatFormatting.GRAY));
		builder.accept(Component.translatable("tooltip.solorust.artifact.free").withStyle(ChatFormatting.DARK_GRAY));
	}
}
