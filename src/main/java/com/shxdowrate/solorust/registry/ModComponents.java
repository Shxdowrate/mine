package com.shxdowrate.solorust.registry;

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

	private ModComponents() {
	}

	public static void init() {
	}
}
