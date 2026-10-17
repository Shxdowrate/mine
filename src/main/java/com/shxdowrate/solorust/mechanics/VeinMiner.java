package com.shxdowrate.solorust.mechanics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.shxdowrate.solorust.block.NodeBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;

/**
 * «Три тимбер»: ломаешь один блок бревна топором или руды/валуна киркой — ломается всё дерево / жила / валун.
 * Зажми Shift, чтобы сломать только один блок.
 */
public final class VeinMiner {
	private static final int MAX_LOGS = 192;
	private static final int MAX_ORES = 64;

	/** Защита от рекурсии: наши собственные поломки снова вызывают событие. Работает только в серверном потоке. */
	private static boolean breaking = false;

	private VeinMiner() {
	}

	public static void init() {
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (breaking || level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
				return;
			}

			if (player.isShiftKeyDown()) {
				return;
			}

			ItemStack tool = player.getMainHandItem();
			boolean isLog = state.is(BlockTags.LOGS) && tool.is(ItemTags.AXES);
			boolean isOre = (state.is(ConventionalBlockTags.ORES) || state.getBlock() instanceof NodeBlock) && tool.is(ItemTags.PICKAXES);

			if (!isLog && !isOre) {
				return;
			}

			List<BlockPos> targets = collect(level, pos, state.getBlock(), isLog ? MAX_LOGS : MAX_ORES);

			breaking = true;
			try {
				for (BlockPos target : targets) {
					ItemStack held = player.getMainHandItem();

					// Не доламываем инструмент до конца — оставляем ему 1 прочности.
					if (held.isEmpty() || (held.isDamageableItem() && held.getMaxDamage() - held.getDamageValue() <= 1)) {
						break;
					}

					serverPlayer.gameMode.destroyBlock(target);
				}
			} finally {
				breaking = false;
			}
		});
	}

	/** Поиск в ширину по всем 26 соседям (чтобы цеплять и диагональные ветки). */
	private static List<BlockPos> collect(Level level, BlockPos origin, Block block, int limit) {
		List<BlockPos> result = new ArrayList<>();
		Set<BlockPos> visited = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();

		visited.add(origin);
		queue.add(origin);

		while (!queue.isEmpty() && result.size() < limit) {
			BlockPos current = queue.poll();

			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) {
							continue;
						}

						BlockPos next = current.offset(dx, dy, dz);

						if (!visited.add(next)) {
							continue;
						}

						BlockState nextState = level.getBlockState(next);

						if (nextState.is(block)) {
							result.add(next);
							queue.add(next);

							if (result.size() >= limit) {
								return result;
							}
						}
					}
				}
			}
		}

		return result;
	}
}
