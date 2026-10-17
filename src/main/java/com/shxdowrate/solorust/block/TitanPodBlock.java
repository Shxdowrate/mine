package com.shxdowrate.solorust.block;

import com.mojang.serialization.MapCodec;
import com.shxdowrate.solorust.block.entity.TitanPodBlockEntity;
import com.shxdowrate.solorust.suit.SuitAssembly;
import com.shxdowrate.solorust.suit.SuitTier;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Упавшая с неба капсула «Титана»: ПКМ — костюм собирается на тебе, капсула исчезает. */
public class TitanPodBlock extends BaseEntityBlock {
	public static final MapCodec<TitanPodBlock> CODEC = simpleCodec(TitanPodBlock::new);

	public TitanPodBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(player instanceof ServerPlayer serverPlayer) || !(level.getBlockEntity(pos) instanceof TitanPodBlockEntity pod)) {
			return InteractionResult.SUCCESS;
		}

		if (SuitTier.worn(serverPlayer) != null || SuitAssembly.busy(serverPlayer)) {
			serverPlayer.sendOverlayMessage(Component.translatable("message.solorust.suit.already_worn").withStyle(ChatFormatting.YELLOW));
			return InteractionResult.SUCCESS;
		}

		int energy = pod.energy();
		// Сначала убираем капсулу (без выпадения маяка), потом собираем костюм.
		level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
		serverPlayer.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		SuitAssembly.start(serverPlayer, SuitAssembly.Style.POD, SuitAssembly.freshSuit(SuitTier.TITAN, energy), null);
		return InteractionResult.SUCCESS;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TitanPodBlockEntity(pos, state);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}
}
