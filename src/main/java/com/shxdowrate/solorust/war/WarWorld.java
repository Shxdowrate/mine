package com.shxdowrate.solorust.war;

import java.util.List;
import java.util.stream.Stream;

import com.shxdowrate.solorust.SoloRust;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.StructureSet;

/**
 * Миры War Machines: «War Machines» (обычный рельеф, обёртка {@link WarBiomeSource}) и «WarFlat»
 * (суперплоскость, в списке структур которой есть наши частые деревни).
 */
public final class WarWorld {
	/** Частые деревни — только для миров War Machines. */
	public static final ResourceKey<StructureSet> WAR_VILLAGES = ResourceKey.create(Registries.STRUCTURE_SET, SoloRust.id("war_villages"));

	private WarWorld() {
	}

	public static boolean is(Level level) {
		return level instanceof ServerLevel server && level.dimension() == Level.OVERWORLD && is(server.getChunkSource().getGenerator());
	}

	public static boolean is(ChunkGenerator generator) {
		if (generator.getBiomeSource() instanceof WarBiomeSource) {
			return true;
		}

		return generator instanceof FlatLevelSource flat && flat.settings().structureOverrides()
				.map(set -> set.stream().anyMatch(h -> h.is(WAR_VILLAGES)))
				.orElse(false);
	}

	/**
	 * Набор структур для обычного (не плоского) генератора: в мире War Machines обычные деревни заменяются
	 * частыми, в остальных мирах частые деревни не появляются.
	 */
	public static List<Holder<StructureSet>> filter(Stream<Holder<StructureSet>> sets, boolean war) {
		return sets.filter(holder -> {
			if (holder.is(WAR_VILLAGES)) {
				return war;
			}

			return !(war && holder.is(BuiltinStructureSets.VILLAGES));
		}).toList();
	}
}
