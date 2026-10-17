package com.shxdowrate.solorust.world;

import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/**
 * Обёртка над обычным источником биомов. Биомы те же, что в обычном мире,
 * но по ней мод понимает, что это мир типа «Rust», и включает дороги, валуны и лут.
 */
public class RustBiomeSource extends BiomeSource {
	public static final MapCodec<RustBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			BiomeSource.CODEC.fieldOf("delegate").forGetter(source -> source.delegate)
	).apply(instance, RustBiomeSource::new));

	private final BiomeSource delegate;

	public RustBiomeSource(BiomeSource delegate) {
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
