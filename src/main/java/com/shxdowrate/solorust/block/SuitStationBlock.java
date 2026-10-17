package com.shxdowrate.solorust.block;

import com.mojang.serialization.MapCodec;
import com.shxdowrate.solorust.block.entity.SuitStationBlockEntity;
import com.shxdowrate.solorust.suit.SuitActions;
import com.shxdowrate.solorust.suit.SuitArmorItem;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Сборочная станция. Встань рядом и ПКМ — манипуляторы соберут костюм прямо на тебе.
 * ПКМ в костюме по пустой станции — снять костюм на неё. Деталью брони в руке — положить деталь.
 * Shift + ПКМ — забрать детали. Клавиша V — вызвать костюм со станции к себе.
 */
public class SuitStationBlock extends BaseEntityBlock {
	public static final MapCodec<SuitStationBlock> CODEC = simpleCodec(SuitStationBlock::new);
	public static final BooleanProperty LOADED = BooleanProperty.create("loaded");

	public SuitStationBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LOADED, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LOADED);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(stack.getItem() instanceof SuitArmorItem armor)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}

		if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof SuitStationBlockEntity station) {
			int slot = SuitActions.stationSlot(armor.type());

			if (!station.get(slot).isEmpty()) {
				return InteractionResult.FAIL;
			}

			station.set(slot, stack.split(1));
			refresh(serverLevel, pos, station);
			level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 0.8f, 1.0f);
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer
				&& level.getBlockEntity(pos) instanceof SuitStationBlockEntity station) {
			SuitActions.useStation(serverLevel, pos, serverPlayer, station);
		}

		return InteractionResult.SUCCESS;
	}

	/** Обновить вид станции: с костюмом или пустая. */
	public static void refresh(Level level, BlockPos pos, SuitStationBlockEntity station) {
		BlockState state = level.getBlockState(pos);

		if (state.hasProperty(LOADED) && state.getValue(LOADED) != station.isFull()) {
			level.setBlockAndUpdate(pos, state.setValue(LOADED, station.isFull()));
		}
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SuitStationBlockEntity(pos, state);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}
}
