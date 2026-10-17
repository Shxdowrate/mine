package com.shxdowrate.solorust.gun;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Установленные на ствол модули (id модулей, пустая строка — слот пуст). */
public record GunMods(String sight, String muzzle, String under, String magazine) {
	public static final GunMods EMPTY = new GunMods("", "", "", "");

	public static final Codec<GunMods> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.optionalFieldOf("sight", "").forGetter(GunMods::sight),
			Codec.STRING.optionalFieldOf("muzzle", "").forGetter(GunMods::muzzle),
			Codec.STRING.optionalFieldOf("under", "").forGetter(GunMods::under),
			Codec.STRING.optionalFieldOf("magazine", "").forGetter(GunMods::magazine)
	).apply(i, GunMods::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, GunMods> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, GunMods::sight,
			ByteBufCodecs.STRING_UTF8, GunMods::muzzle,
			ByteBufCodecs.STRING_UTF8, GunMods::under,
			ByteBufCodecs.STRING_UTF8, GunMods::magazine,
			GunMods::new
	);

	public String get(Attachment.Slot slot) {
		return switch (slot) {
			case SIGHT -> sight;
			case MUZZLE -> muzzle;
			case UNDER -> under;
			case MAGAZINE -> magazine;
		};
	}

	public GunMods with(Attachment.Slot slot, String id) {
		return switch (slot) {
			case SIGHT -> new GunMods(id, muzzle, under, magazine);
			case MUZZLE -> new GunMods(sight, id, under, magazine);
			case UNDER -> new GunMods(sight, muzzle, id, magazine);
			case MAGAZINE -> new GunMods(sight, muzzle, under, id);
		};
	}

	public boolean has(Attachment attachment) {
		return get(attachment.slot).equals(attachment.id);
	}

	public boolean isEmpty() {
		return sight.isEmpty() && muzzle.isEmpty() && under.isEmpty() && magazine.isEmpty();
	}

	public List<Attachment> list() {
		List<Attachment> list = new ArrayList<>();

		for (Attachment.Slot slot : Attachment.Slot.values()) {
			Attachment a = Attachment.byId(get(slot));

			if (a != null) {
				list.add(a);
			}
		}

		return list;
	}

	/** Строки для custom_model_data: модель оружия по ним показывает прицел, дульное и подствольное. */
	public List<String> modelStrings() {
		return List.of(sight, muzzle, under);
	}
}
