package com.shxdowrate.solorust.suit;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorType;

/** Часть силовой брони. Нагрудник хранит энергоядро — его заряд показан полоской вместо прочности. */
public class SuitArmorItem extends Item {
	private final SuitTier tier;
	private final ArmorType type;

	public SuitArmorItem(Properties properties, SuitTier tier, ArmorType type) {
		super(properties);
		this.tier = tier;
		this.type = type;
	}

	public SuitTier tier() {
		return tier;
	}

	public ArmorType type() {
		return type;
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return type == ArmorType.CHESTPLATE;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13.0f * SuitEnergy.get(stack, tier) / tier.maxEnergy);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return tier.hudColor & 0xFFFFFF;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Component.translatable(tier.translationKey()).withColor(tier.hudColor & 0xFFFFFF));

		if (type == ArmorType.CHESTPLATE) {
			builder.accept(Component.translatable("tooltip.solorust.suit.energy", SuitEnergy.get(stack, tier), tier.maxEnergy)
					.withStyle(ChatFormatting.AQUA));
		}

		builder.accept(Component.translatable(tier.flight ? "tooltip.solorust.suit.flight" : "tooltip.solorust.suit.no_flight")
				.withStyle(ChatFormatting.GRAY));
		builder.accept(Component.translatable("tooltip.solorust.suit.weapons", tier.weapons.size()).withStyle(ChatFormatting.GRAY));
		builder.accept(Component.translatable("tooltip.solorust.suit.full_set").withStyle(ChatFormatting.DARK_GRAY));
	}
}
