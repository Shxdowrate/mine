package com.shxdowrate.solorust.registry;

import java.util.List;

import com.mojang.serialization.Codec;
import com.shxdowrate.solorust.SoloRust;
import com.shxdowrate.solorust.gun.GunMods;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

public final class ModComponents {
	/** Патронов в магазине. */
	public static final DataComponentType<Integer> AMMO = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SoloRust.id("ammo"),
			DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).ignoreSwapAnimation().build());

	/** Какой патрон заряжен (id предмета). */
	public static final DataComponentType<String> AMMO_TYPE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SoloRust.id("ammo_type"),
			DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).ignoreSwapAnimation().build());

	/** Установленные модули. */
	public static final DataComponentType<GunMods> MODS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SoloRust.id("mods"),
			DataComponentType.<GunMods>builder().persistent(GunMods.CODEC).networkSynchronized(GunMods.STREAM_CODEC).ignoreSwapAnimation().build());

	/** Выбранная ячейка способностей артефакта (0–8). */
	public static final DataComponentType<Integer> ABILITY_SLOT = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SoloRust.id("ability_slot"),
			DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).ignoreSwapAnimation().build());

	/** До какого игрового тика каждая из 18 способностей на перезарядке. */
	public static final DataComponentType<List<Long>> ABILITY_COOLDOWNS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SoloRust.id("ability_cooldowns"),
			DataComponentType.<List<Long>>builder().persistent(Codec.LONG.listOf()).networkSynchronized(ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list())).ignoreSwapAnimation().build());

	/** Вставленные в перчатку камни (битовая маска). */
	public static final DataComponentType<Integer> STONES = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SoloRust.id("stones"),
			DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).ignoreSwapAnimation().build());

	/** Заряд энергоядра силовой брони (хранится на нагруднике). */
	public static final DataComponentType<Integer> SUIT_ENERGY = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SoloRust.id("suit_energy"),
			DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

	private ModComponents() {
	}

	public static void init() {
	}
}
