package com.shxdowrate.solorust.block;

import com.shxdowrate.solorust.mechanics.Blueprints;
import com.shxdowrate.solorust.registry.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Исследовательский стол: ПКМ по нему с найденным оружием/модулем в руке — предмет и скрап тратятся,
 * а чертёж изучается навсегда.
 */
public class ResearchTableBlock extends Block {
	public ResearchTableBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (stack.isEmpty()) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}

		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}

		Item item = stack.getItem();
		int cost = Blueprints.researchCost(item);

		if (cost <= 0) {
			serverPlayer.sendSystemMessage(Component.translatable("message.solorust.cant_research").withStyle(ChatFormatting.GRAY), true);
			return InteractionResult.SUCCESS;
		}

		if (Blueprints.knows(player, item) && !player.hasInfiniteMaterials()) {
			serverPlayer.sendSystemMessage(Component.translatable("message.solorust.already_known").withStyle(ChatFormatting.YELLOW), true);
			return InteractionResult.SUCCESS;
		}

		Inventory inventory = player.getInventory();

		if (!player.hasInfiniteMaterials()) {
			if (count(inventory, ModItems.SCRAP) < cost) {
				serverPlayer.sendSystemMessage(Component.translatable("message.solorust.research_cost", cost).withStyle(ChatFormatting.RED), true);
				level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.6f, 1.0f);
				return InteractionResult.SUCCESS;
			}

			take(inventory, ModItems.SCRAP, cost);
			stack.shrink(1);
		}

		Blueprints.learn(player, item);
		serverPlayer.sendSystemMessage(Component.translatable("message.solorust.researched", Component.translatable(item.getDescriptionId()))
				.withStyle(ChatFormatting.GREEN), true);
		level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.7f, 1.4f);
		serverLevel.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 30, 0.4, 0.3, 0.4, 0.5);
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendSystemMessage(Component.translatable("message.solorust.research_hint").withStyle(ChatFormatting.GRAY), true);
		}

		return InteractionResult.SUCCESS;
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

	private static void take(Inventory inventory, Item item, int amount) {
		for (int i = 0; i < inventory.getContainerSize() && amount > 0; i++) {
			ItemStack s = inventory.getItem(i);

			if (s.is(item)) {
				int t = Math.min(amount, s.getCount());
				s.shrink(t);
				amount -= t;
			}
		}
	}
}
