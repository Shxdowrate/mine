package com.shxdowrate.solorust.suit;

import java.util.function.Consumer;

import com.shxdowrate.solorust.registry.ModComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Костюм «в свёрнутом виде»: кейс, нанокапсула или маяк вызова «Титана». ПКМ (или клавиша G) — развернуть,
 * G в надетом костюме — свернуть обратно. Заряд энергоядра сохраняется.
 */
public class SuitContainerItem extends Item {
	public enum Kind {
		SUITCASE(SuitTier.LIGHT, SuitAssembly.Style.CASE),
		NANO_CAPSULE(SuitTier.NANO, SuitAssembly.Style.NANO),
		TITAN_BEACON(SuitTier.TITAN, SuitAssembly.Style.POD);

		public final SuitTier tier;
		public final SuitAssembly.Style style;

		Kind(SuitTier tier, SuitAssembly.Style style) {
			this.tier = tier;
			this.style = style;
		}

		public static Kind forTier(SuitTier tier) {
			for (Kind kind : values()) {
				if (kind.tier == tier) {
					return kind;
				}
			}

			return null;
		}
	}

	private final Kind kind;

	public SuitContainerItem(Properties properties, Kind kind) {
		super(properties);
		this.kind = kind;
	}

	public Kind kind() {
		return kind;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}

		return deploy(serverLevel, serverPlayer, stack) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
	}

	/** Развернуть костюм из этого предмета. Предмет расходуется. */
	public boolean deploy(ServerLevel level, ServerPlayer player, ItemStack stack) {
		if (SuitTier.worn(player) != null || SuitAssembly.busy(player)) {
			player.sendOverlayMessage(Component.translatable("message.solorust.suit.already_worn").withStyle(ChatFormatting.YELLOW));
			return false;
		}

		int energy = stack.getOrDefault(ModComponents.SUIT_ENERGY, kind.tier.maxEnergy);

		if (kind == Kind.TITAN_BEACON) {
			// Маяк бросаем в точку, куда смотрим (или под ноги), — оттуда и упадёт капсула.
			Vec3 eye = player.getEyePosition();
			BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getViewVector(1f).scale(48)),
					ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
			var target = hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos().relative(hit.getDirection()) : player.blockPosition();

			if (!TitanDrop.call(level, player, target, energy)) {
				return false;
			}
		} else if (!SuitAssembly.start(player, kind.style, SuitAssembly.freshSuit(kind.tier, energy), null)) {
			return false;
		}

		stack.shrink(1);
		return true;
	}

	/** Предмет-контейнер с запомненным зарядом. */
	public static ItemStack withEnergy(Item item, int energy) {
		ItemStack stack = new ItemStack(item);
		stack.set(ModComponents.SUIT_ENERGY, energy);
		return stack;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Component.translatable(kind.tier.translationKey()).withColor(kind.tier.hudColor & 0xFFFFFF));
		builder.accept(Component.translatable("tooltip.solorust.suit.energy", stack.getOrDefault(ModComponents.SUIT_ENERGY, kind.tier.maxEnergy), kind.tier.maxEnergy)
				.withStyle(ChatFormatting.AQUA));
		builder.accept(Component.translatable("tooltip.solorust.suit_container." + kind.name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GRAY));
	}
}
