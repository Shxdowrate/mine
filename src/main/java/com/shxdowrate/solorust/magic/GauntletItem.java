package com.shxdowrate.solorust.magic;

import java.util.List;
import java.util.function.Consumer;

import com.shxdowrate.solorust.registry.ModComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Перчатка бесконечности. Ячейки 1–6 — по камню на ячейку, 7 — Сила + Пространство,
 * 8 — Пространство + Время, 9 — все шесть камней. Камни вставляются как модули оружия:
 * возьми камень курсором и кликни по перчатке; ПКМ пустым курсором — вынуть последний камень.
 */
public class GauntletItem extends ArtifactItem {
	private static final int[] REQUIRED = {
			InfinityStone.POWER.bit(), InfinityStone.SPACE.bit(), InfinityStone.REALITY.bit(),
			InfinityStone.SOUL.bit(), InfinityStone.TIME.bit(), InfinityStone.MIND.bit(),
			InfinityStone.POWER.bit() | InfinityStone.SPACE.bit(),
			InfinityStone.SPACE.bit() | InfinityStone.TIME.bit(),
			InfinityStone.ALL
	};

	private static final int[] COLORS = {
			InfinityStone.POWER.color, InfinityStone.SPACE.color, InfinityStone.REALITY.color,
			InfinityStone.SOUL.color, InfinityStone.TIME.color, InfinityStone.MIND.color,
			0x6A5BFF, 0x2BB7C9, 0xFFC94A
	};

	public GauntletItem(Properties properties) {
		super(properties);
	}

	public static int stones(ItemStack stack) {
		return stack.getOrDefault(ModComponents.STONES, 0) & InfinityStone.ALL;
	}

	public static void setStones(ItemStack stack, int mask) {
		stack.set(ModComponents.STONES, mask & InfinityStone.ALL);
		stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(String.valueOf(mask & InfinityStone.ALL)), List.of()));
	}

	public static ItemStack full(Item item) {
		ItemStack stack = new ItemStack(item);
		setStones(stack, InfinityStone.ALL);
		return stack;
	}

	@Override
	public Ability ability(int slot, int button) {
		return GauntletAbilities.ABILITIES[slot * 2 + button];
	}

	@Override
	public int slotColor(int slot) {
		return COLORS[slot];
	}

	@Override
	public String key() {
		return "gauntlet";
	}

	@Override
	public boolean unlocked(ItemStack stack, int slot) {
		return (stones(stack) & REQUIRED[slot]) == REQUIRED[slot];
	}

	@Override
	public Component lockReason(ItemStack stack, int slot) {
		MutableComponent missing = Component.empty();
		boolean first = true;

		for (InfinityStone stone : InfinityStone.values()) {
			if ((REQUIRED[slot] & stone.bit()) != 0 && (stones(stack) & stone.bit()) == 0) {
				if (!first) {
					missing.append(", ");
				}

				missing.append(Component.translatable("item.solorust." + stone.id).withStyle(stone.format));
				first = false;
			}
		}

		return Component.translatable("message.solorust.magic.need_stones", missing).withStyle(ChatFormatting.GRAY);
	}

	@Override
	public boolean grantsFlight(ItemStack stack) {
		return unlocked(stack, 7);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return stones(stack) == InfinityStone.ALL;
	}

	// --- Камни ---

	@Override
	public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction clickAction, Player player, SlotAccess carriedItem) {
		int mask = stones(self);

		if (clickAction == ClickAction.PRIMARY && other.getItem() instanceof InfinityStoneItem stoneItem) {
			int bit = stoneItem.stone().bit();

			if ((mask & bit) != 0) {
				return false;
			}

			setStones(self, mask | bit);
			other.shrink(1);
			player.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 1.2f);
			player.playSound(SoundEvents.BEACON_POWER_SELECT, 0.6f, 1.6f);
			return true;
		}

		if (clickAction == ClickAction.SECONDARY && other.isEmpty() && mask != 0) {
			for (int i = InfinityStone.values().length - 1; i >= 0; i--) {
				InfinityStone stone = InfinityStone.values()[i];

				if ((mask & stone.bit()) != 0) {
					setStones(self, mask & ~stone.bit());
					carriedItem.set(new ItemStack(MagicItems.STONES.get(stone)));
					player.playSound(SoundEvents.AMETHYST_BLOCK_BREAK, 1.0f, 1.0f);
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		int mask = stones(stack);

		for (InfinityStone stone : InfinityStone.values()) {
			boolean has = (mask & stone.bit()) != 0;
			builder.accept(Component.literal(has ? " ◆ " : " ◇ ").withStyle(has ? stone.format : ChatFormatting.DARK_GRAY)
					.append(Component.translatable("item.solorust." + stone.id).withStyle(has ? stone.format : ChatFormatting.DARK_GRAY)));
		}

		builder.accept(Component.translatable("tooltip.solorust.gauntlet.insert").withStyle(ChatFormatting.DARK_GRAY));
		super.appendHoverText(stack, context, display, builder, flag);
	}
}
