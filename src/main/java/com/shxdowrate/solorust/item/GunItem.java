package com.shxdowrate.solorust.item;

import java.util.List;
import java.util.function.Consumer;

import com.shxdowrate.solorust.gun.Attachment;
import com.shxdowrate.solorust.gun.GunDef;
import com.shxdowrate.solorust.gun.GunMods;
import com.shxdowrate.solorust.gun.GunStats;
import com.shxdowrate.solorust.registry.ModComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Огнестрел. ЛКМ — выстрел (пакет с клиента, см. GunLogic), ПКМ — прицеливание (удержание «использования»),
 * R — перезарядка. Модули ставятся как в Расте: возьми модуль курсором и кликни им по стволу в инвентаре;
 * ПКМ по стволу пустым курсором снимает модуль.
 */
public class GunItem extends Item {
	private final GunDef def;

	public GunItem(Properties properties, GunDef def) {
		super(properties);
		this.def = def;
	}

	public GunDef def() {
		return def;
	}

	// --- Прицеливание ---

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}

		player.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public int getUseDuration(ItemStack itemStack, LivingEntity user) {
		return 72000;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack itemStack) {
		return ItemUseAnimation.NONE;
	}

	// --- Полоска патронов вместо полоски прочности ---

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		int mag = GunStats.magazine(def, GunStats.mods(stack));
		int ammo = stack.getOrDefault(ModComponents.AMMO, 0);
		return Math.round(13.0f * ammo / Math.max(1, mag));
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return 0xE8C547;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		int mag = GunStats.magazine(def, GunStats.mods(stack));
		int ammo = stack.getOrDefault(ModComponents.AMMO, 0);
		builder.accept(Component.translatable("tooltip.solorust.magazine", ammo, mag).withStyle(ChatFormatting.YELLOW));
		builder.accept(Component.translatable(def.automatic() ? "tooltip.solorust.auto" : "tooltip.solorust.semi").withStyle(ChatFormatting.GRAY));

		GunMods mods = GunStats.mods(stack);

		for (Attachment a : mods.list()) {
			builder.accept(Component.literal(" + ").withStyle(ChatFormatting.DARK_GREEN)
					.append(Component.translatable("item.solorust." + a.id).withStyle(ChatFormatting.GREEN)));
		}

		if (def.mods()) {
			builder.accept(Component.translatable("tooltip.solorust.mods_hint").withStyle(ChatFormatting.DARK_GRAY));
		}
	}

	// --- Модули ---

	@Override
	public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction clickAction, Player player, SlotAccess carriedItem) {
		if (!def.mods()) {
			return false;
		}

		GunMods mods = GunStats.mods(self);

		if (clickAction == ClickAction.PRIMARY && other.getItem() instanceof AttachmentItem attachmentItem) {
			Attachment attachment = attachmentItem.attachment();
			String old = mods.get(attachment.slot);

			if (old.equals(attachment.id)) {
				return false;
			}

			setMods(self, mods.with(attachment.slot, attachment.id));
			other.shrink(1);

			if (!old.isEmpty()) {
				ItemStack returned = attachmentStack(old);

				if (other.isEmpty()) {
					carriedItem.set(returned);
				} else if (!player.getInventory().add(returned)) {
					player.drop(returned, false);
				}
			}

			player.playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.8f, 1.4f);
			return true;
		}

		if (clickAction == ClickAction.SECONDARY && other.isEmpty() && !mods.isEmpty()) {
			for (Attachment.Slot attachmentSlot : Attachment.Slot.values()) {
				String id = mods.get(attachmentSlot);

				if (!id.isEmpty()) {
					setMods(self, mods.with(attachmentSlot, ""));
					carriedItem.set(attachmentStack(id));
					player.playSound(SoundEvents.ARMOR_EQUIP_CHAIN.value(), 0.8f, 1.2f);
					return true;
				}
			}
		}

		return false;
	}

	public static void setMods(ItemStack stack, GunMods mods) {
		stack.set(ModComponents.MODS, mods);
		stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), mods.modelStrings(), List.of()));

		// Если магазин стал меньше (сняли расширенный) — лишние патроны просто срезаются до нового размера.
		if (stack.getItem() instanceof GunItem gun) {
			int mag = GunStats.magazine(gun.def, mods);

			if (stack.getOrDefault(ModComponents.AMMO, 0) > mag) {
				stack.set(ModComponents.AMMO, mag);
			}
		}
	}

	private static ItemStack attachmentStack(String id) {
		Item item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("solorust", id));
		return new ItemStack(item);
	}
}
