package com.shxdowrate.solorust.block.entity;

import com.shxdowrate.solorust.registry.ModBlockEntities;
import com.shxdowrate.solorust.suit.SuitTier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Капсула «Титана» помнит заряд костюма внутри. */
public class TitanPodBlockEntity extends BlockEntity {
	private int energy = SuitTier.TITAN.maxEnergy;

	public TitanPodBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.TITAN_POD, pos, state);
	}

	public int energy() {
		return energy;
	}

	public void setEnergy(int energy) {
		this.energy = energy;
		setChanged();
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		energy = input.getIntOr("Energy", SuitTier.TITAN.maxEnergy);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("Energy", energy);
	}
}
