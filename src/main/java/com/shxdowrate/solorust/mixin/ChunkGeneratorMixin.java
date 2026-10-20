package com.shxdowrate.solorust.mixin;

import java.util.List;

import com.shxdowrate.solorust.war.WarBiomeSource;
import com.shxdowrate.solorust.war.WarWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;

/** Подменяет набор структур обычного генератора: частые деревни — только в мире War Machines. */
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {
	@Inject(method = "createState", at = @At("HEAD"), cancellable = true)
	private void solorust$warVillages(HolderLookup<StructureSet> structureSets, RandomState randomState, long seed,
			CallbackInfoReturnable<ChunkGeneratorStructureState> cir) {
		ChunkGenerator self = (ChunkGenerator) (Object) this;
		boolean war = self.getBiomeSource() instanceof WarBiomeSource;

		List<Holder<StructureSet>> sets = WarWorld.filter(structureSets.listElements().map(e -> (Holder<StructureSet>) e), war).stream()
				.filter(set -> ChunkGeneratorStructureStateAccessor.solorust$hasBiomes(set.value(), self.getBiomeSource()))
				.toList();

		cir.setReturnValue(ChunkGeneratorStructureStateAccessor.solorust$create(randomState, self.getBiomeSource(), seed, seed, sets));
	}
}
