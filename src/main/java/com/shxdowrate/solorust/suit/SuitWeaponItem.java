package com.shxdowrate.solorust.suit;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Встроенная система костюма. Работает только пока надет полный комплект, в котором она есть.
 * Сам предмет выдаётся и забирается автоматически (см. {@link SuitManager}).
 */
public class SuitWeaponItem extends Item {
	private final SuitWeapon weapon;

	public SuitWeaponItem(Properties properties, SuitWeapon weapon) {
		super(properties);
		this.weapon = weapon;
	}

	public SuitWeapon weapon() {
		return weapon;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}

		ItemStack stack = player.getItemInHand(hand);
		SuitTier tier = SuitTier.worn(player);

		if (tier == null || !tier.has(weapon)) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(Component.translatable("message.solorust.suit.offline").withStyle(ChatFormatting.RED));
			}

			return InteractionResult.FAIL;
		}

		if (weapon.mode != SuitWeapon.Mode.INSTANT) {
			// Держим ПКМ: HOLD работает каждый тик, CHARGE копит заряд до отпускания.
			if (!player.hasInfiniteMaterials() && SuitEnergy.get(player, tier) < Math.max(1, weapon.mode == SuitWeapon.Mode.CHARGE ? weapon.energy / 3 : weapon.energy)) {
				noEnergy(level, player);
				return InteractionResult.FAIL;
			}

			player.startUsingItem(hand);

			if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
				SuitWeapons.start(serverLevel, serverPlayer, tier, weapon);
			}

			return InteractionResult.CONSUME;
		}

		if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
			if (!SuitEnergy.use(player, tier, weapon.energy)) {
				noEnergy(level, player);
				return InteractionResult.FAIL;
			}

			SuitWeapons.fire(serverLevel, serverPlayer, tier, weapon, 1.0f);
			player.getCooldowns().addCooldown(stack, weapon.cooldown);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {
		if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player)) {
			return;
		}

		SuitTier tier = SuitTier.worn(player);

		if (tier == null || !tier.has(weapon)) {
			player.stopUsingItem();
			return;
		}

		int used = getUseDuration(stack, entity) - ticksRemaining;

		if (weapon.mode == SuitWeapon.Mode.HOLD) {
			if (!SuitEnergy.use(player, tier, weapon.energy)) {
				noEnergy(level, player);
				player.stopUsingItem();
				return;
			}

			SuitWeapons.hold(serverLevel, player, tier, weapon, used);
		} else if (weapon.mode == SuitWeapon.Mode.CHARGE) {
			SuitWeapons.charging(serverLevel, player, tier, weapon, used);
		}
	}

	@Override
	public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
		if (weapon.mode != SuitWeapon.Mode.CHARGE || !(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player)) {
			return false;
		}

		SuitTier tier = SuitTier.worn(player);
		int charge = getUseDuration(stack, entity) - remainingTime;

		if (tier == null || !tier.has(weapon) || charge < weapon.minCharge) {
			// Не дозарядили — захваченные цели просто падают.
			SuitAbilities.releaseHeld(player);
			return false;
		}

		float fraction = Math.min(1.0f, (float) charge / weapon.maxCharge);
		int cost = Math.max(1, Math.round(weapon.energy * fraction));

		if (!SuitEnergy.use(player, tier, cost)) {
			noEnergy(level, player);
			return false;
		}

		SuitWeapons.fire(serverLevel, player, tier, weapon, fraction);
		player.getCooldowns().addCooldown(stack, Math.round(weapon.cooldown * (0.4f + 0.6f * fraction)));
		return true;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user) {
		return weapon.mode == SuitWeapon.Mode.INSTANT ? 0 : 72000;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return switch (weapon) {
			case NANO_SHIELD, RUNE_SHIELD, CHAOS_SPHERE -> ItemUseAnimation.BLOCK;
			case CHEST_BEAM, NANO_CANNON, COLLAPSE, CHAOS_BLAST -> ItemUseAnimation.BOW;
			default -> ItemUseAnimation.NONE;
		};
	}

	private static void noEnergy(Level level, Player player) {
		if (!level.isClientSide()) {
			player.sendOverlayMessage(Component.translatable("message.solorust.suit.no_energy").withStyle(ChatFormatting.RED));
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Component.translatable("tooltip.solorust.suit_weapon." + weapon.id).withStyle(ChatFormatting.GRAY));

		String control = switch (weapon.mode) {
			case INSTANT -> "tooltip.solorust.suit.control.instant";
			case HOLD -> "tooltip.solorust.suit.control.hold";
			case CHARGE -> "tooltip.solorust.suit.control.charge";
		};

		builder.accept(Component.translatable(control).withStyle(ChatFormatting.DARK_AQUA));
		builder.accept(Component.translatable(weapon.mode == SuitWeapon.Mode.HOLD ? "tooltip.solorust.suit.cost_tick" : "tooltip.solorust.suit.cost", weapon.energy)
				.withStyle(ChatFormatting.AQUA));
		builder.accept(Component.translatable("tooltip.solorust.suit.bound").withStyle(ChatFormatting.DARK_GRAY));
	}
}
