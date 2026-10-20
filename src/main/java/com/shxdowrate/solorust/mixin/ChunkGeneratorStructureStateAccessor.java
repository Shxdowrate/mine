package com.shxdowrate.solorust.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;

@Mixin(ChunkGeneratorStructureState.class)
public interface ChunkGeneratorStructureStateAccessor {
	@Invoker("<init>")
	static ChunkGeneratorStructureState solorust$create(RandomState randomState, BiomeSource biomeSource, long levelSeed, long concentricRingsSeed,
			List<Holder<StructureSet>> possibleStructureSets) {
		throw new AssertionError();
	}

	@Invoker("hasBiomesForStructureSet")
	static boolean solorust$hasBiomes(StructureSet structureSet, BiomeSource biomeSource) {
		throw new AssertionError();
	}
}
