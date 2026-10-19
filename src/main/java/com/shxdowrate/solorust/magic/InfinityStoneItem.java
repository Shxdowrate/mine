package com.shxdowrate.solorust.magic;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Камень бесконечности: вставляется в перчатку кликом по ней в инвентаре. */
public class InfinityStoneItem extends Item {
	private final InfinityStone stone;

	public InfinityStoneItem(Properties properties, InfinityStone stone) {
		super(properties);
		this.stone = stone;
	}

	public InfinityStone stone() {
		return stone;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Component.translatable("tooltip.solorust.stone." + stone.id).withStyle(stone.format));
		builder.accept(Component.translatable("tooltip.solorust.stone.insert").withStyle(ChatFormatting.DARK_GRAY));
	}
}
