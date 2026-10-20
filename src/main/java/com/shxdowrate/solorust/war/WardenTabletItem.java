package com.shxdowrate.solorust.war;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/** Планшет WARDEN: ПКМ открывает гардероб костюмов и магазин коробок. */
public class WardenTabletItem extends Item {
	/** Открыть экран планшета (подставляет клиент). */
	public static Runnable openScreen = () -> {
	};

	public WardenTabletItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			openScreen.run();
		} else {
			level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT,
					net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.8f);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Component.translatable("tooltip.solorust.warden_tablet").withStyle(ChatFormatting.GRAY));
	}
}
