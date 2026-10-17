package com.shxdowrate.solorust.item;

import java.util.function.Consumer;

import com.shxdowrate.solorust.gun.Attachment;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class AttachmentItem extends Item {
	private final Attachment attachment;

	public AttachmentItem(Properties properties, Attachment attachment) {
		super(properties);
		this.attachment = attachment;
	}

	public Attachment attachment() {
		return attachment;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Component.translatable("tooltip.solorust.attachment." + attachment.id).withStyle(ChatFormatting.GRAY));
		builder.accept(Component.translatable("tooltip.solorust.attach_hint").withStyle(ChatFormatting.DARK_GRAY));
	}
}
