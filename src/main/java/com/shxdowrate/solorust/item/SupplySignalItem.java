package com.shxdowrate.solorust.item;

import com.shxdowrate.solorust.mechanics.Airdrops;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Сигнальная шашка: зажигаешь — через 30 секунд рядом падает ящик снабжения. */
public class SupplySignalItem extends Item {
	public SupplySignalItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
			Airdrops.call(serverLevel, player.blockPosition());
			serverLevel.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 0.8f);
			serverPlayer.sendSystemMessage(Component.translatable("message.solorust.airdrop_called").withStyle(ChatFormatting.GOLD), true);

			if (!player.hasInfiniteMaterials()) {
				stack.shrink(1);
			}
		}

		return InteractionResult.SUCCESS;
	}
}
