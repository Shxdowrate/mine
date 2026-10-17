package com.shxdowrate.solorust.registry;

import com.shxdowrate.solorust.SoloRust;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
	public static final SoundEvent SHOT_PISTOL = register("gun.pistol");
	public static final SoundEvent SHOT_SMG = register("gun.smg");
	public static final SoundEvent SHOT_RIFLE = register("gun.rifle");
	public static final SoundEvent SHOT_SNIPER = register("gun.sniper");
	public static final SoundEvent SHOT_SHOTGUN = register("gun.shotgun");
	public static final SoundEvent SHOT_NAIL = register("gun.nail");
	public static final SoundEvent SHOT_SILENCED = register("gun.silenced");
	public static final SoundEvent DRY_FIRE = register("gun.dry");
	public static final SoundEvent RELOAD = register("gun.reload");
	public static final SoundEvent RELOAD_END = register("gun.reload_end");
	public static final SoundEvent HIT_MARKER = register("gun.hit");

	private ModSounds() {
	}

	private static SoundEvent register(String name) {
		Identifier id = SoloRust.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void init() {
	}
}
