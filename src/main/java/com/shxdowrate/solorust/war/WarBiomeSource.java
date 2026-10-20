package com.shxdowrate.solorust.war;

import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/**
 * Обёртка над обычным источником биомов для мира «War Machines». Биомы те же, что в обычном мире,
 * а по обёртке мод узнаёт свой тип мира: деревни идут чаще и заняты бандитами.
 */
public class WarBiomeSource extends BiomeSource {
	public static final MapCodec<WarBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			BiomeSource.CODEC.fieldOf("delegate").forGetter(source -> source.delegate)
	).apply(instance, WarBiomeSource::new));

	private final BiomeSource delegate;

	public WarBiomeSource(BiomeSource delegate) {
		this.delegate = delegate;
	}

	@Override
	protected MapCodec<? extends BiomeSource> codec() {
		return CODEC;
	}

	@Override
	protected Stream<Holder<Biome>> collectPossibleBiomes() {
		return delegate.possibleBiomes().stream();
	}

	@Override
	public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
		return delegate.getNoiseBiome(x, y, z, sampler);
	}
}
