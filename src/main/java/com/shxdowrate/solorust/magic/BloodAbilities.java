package com.shxdowrate.solorust.magic;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Кровь Мари Моро («Поколение Ви»): 18 способностей гемокинеза.
 * Многие приёмы тратят твою кровь (здоровье) — но часть из них её же и возвращает.
 */
final class BloodAbilities {
	private static final DustParticleOptions BLOOD = Fx.dust(0x9B0000, 1.3f);
	private static final DustParticleOptions BLOOD_LIGHT = Fx.dust(0xE53935, 0.8f);
	private static final DustParticleOptions BLOOD_DARK = Fx.dust(0x4A0000, 1.6f);

	static final int[] COLORS = {0xE53935, 0xB71C1C, 0x880E4F, 0xFF5252, 0xD50000, 0xC62828, 0xAD1457, 0x8E0000, 0xFF1744};

	static final Ability[] ABILITIES = {
			// 1 — Кровавое оружие
			new Ability("blood_whip", 20, BloodAbilities::whip),
			new Ability("blood_shards", 30, BloodAbilities::shards),
			// 2 — Дальний бой
			new Ability("blood_spear", 50, BloodAbilities::spear),
			new Ability("blood_barrage", 100, BloodAbilities::barrage),
			// 3 — Чужая кровь
			new Ability("blood_stop", 300, BloodAbilities::stopBlood),
			new Ability("blood_burst", 200, BloodAbilities::burst),
			// 4 — Свёртывание
			new Ability("blood_clot", 300, BloodAbilities::clot),
			new Ability("blood_harvest", 300, BloodAbilities::harvest),
			// 5 — Реактивная кровь
			new Ability("blood_jet", 30, BloodAbilities::jet),
			new Ability("blood_flight", 10, BloodAbilities::flight),
			// 6 — Защита
			new Ability("blood_shield", 400, BloodAbilities::shield),
			new Ability("blood_armor", 600, BloodAbilities::armor),
			// 7 — Власть над телом
			new Ability("blood_puppet", 600, BloodAbilities::puppet),
			new Ability("blood_sense", 400, BloodAbilities::sense),
			// 8 — Цепи и дождь
			new Ability("blood_chains", 200, BloodAbilities::chains),
			new Ability("blood_rain", 400, BloodAbilities::rain),
			// 9 — Гемокинез
			new Ability("blood_heart", 3000, BloodAbilities::heartStop),
			new Ability("blood_vortex", 1200, BloodAbilities::vortex),
	};

	private BloodAbilities() {
	}

	/** Заплатить своей кровью. false — крови слишком мало. */
	private static boolean pay(ServerLevel level, ServerPlayer player, float hp) {
		if (player.hasInfiniteMaterials()) {
			return true;
		}

		if (player.getHealth() - hp < 3f) {
			Moves.message(player, "message.solorust.hero.low_blood", ChatFormatting.DARK_RED);
			return false;
		}

		player.setHealth(player.getHealth() - hp);
		level.sendParticles(BLOOD_LIGHT, player.getX(), player.getY(0.6), player.getZ(), 8, 0.3, 0.4, 0.3, 0);
		return true;
	}

	private static boolean whip(ServerLevel level, ServerPlayer player, ItemStack stack) {
		return pay(level, player, 1) && Moves.whip(level, player, 14, 9f, 1.8, BLOOD, BLOOD_LIGHT, SoundEvents.SLIME_ATTACK);
	}

	/** Веер кровяных осколков. */
	private static boolean shards(ServerLevel level, ServerPlayer player, ItemStack stack) {
		if (!pay(level, player, 1)) {
			return false;
		}

		Vec3 look = player.getViewVector(1f);
		Vec3[] basis = Fx.basis(look);
		RandomSource random = player.getRandom();

		for (int i = 0; i < 6; i++) {
			Vec3 dir = look.add(basis[0].scale((random.nextDouble() - 0.5) * 0.35)).add(basis[1].scale((random.nextDouble() - 0.5) * 0.25));
			Moves.bolt(level, player, Fx.hand(player), dir, 2.0, 15, false, BLOOD, null, (at, hit) -> {
				if (hit != null) {
					Fx.hurt(level, player, hit, 5f);
				}

				level.sendParticles(BLOOD_LIGHT, at.x, at.y, at.z, 6, 0.15, 0.15, 0.15, 0);
			});
		}

		Fx.sound(level, player, SoundEvents.SLIME_SQUISH, 1.0f, 1.6f);
		return true;
	}

	/** Кровавое копьё: пронзает всех на линии. */
	private static boolean spear(ServerLevel level, ServerPlayer player, ItemStack stack) {
		if (!pay(level, player, 2)) {
			return false;
		}

		Moves.beam(level, player, 48, 16f, true, BLOOD, BLOOD_DARK, Fx.hand(player));
		Fx.sound(level, player, SoundEvents.TRIDENT_RIPTIDE_3.value(), 0.8f, 1.4f);
		return true;
	}

	/** Шквал: шесть самонаводящихся капель. */
	private static boolean barrage(ServerLevel level, ServerPlayer player, ItemStack stack) {
		if (!pay(level, player, 2)) {
			return false;
		}

		Tasks.start(age -> {
			if (age % 3 == 0 && !player.isRemoved()) {
				Moves.bolt(player.level(), player, Fx.hand(player), player.getViewVector(1f), 1.4, 40, true, BLOOD, BLOOD_LIGHT, (at, hit) -> {
					if (hit != null) {
						Fx.hurt(player.level(), player, hit, 5f);
					}

					player.level().sendParticles(BLOOD_LIGHT, at.x, at.y, at.z, 8, 0.2, 0.2, 0.2, 0);
				});
			}

			return age < 16;
		});
		return true;
	}

	/** Остановить кровь: цель замирает на 8 секунд и слабеет. */
	private static boolean stopBlood(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Fx.Ray ray = Fx.ray(level, player, 32, 0.4);
		LivingEntity target = ray.entity();

		if (target == null) {
			return Moves.noTarget(player);
		}

		if (target instanceof Player) {
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 160, 9));
		} else {
			Freeze.freeze(target, level.getGameTime() + 160);
		}

		target.addEffect(new MobEffectInstance(MobEffects.WITHER, 160, 0));
		target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 2));
		Fx.line(level, BLOOD, Fx.hand(player), target.getBoundingBox().getCenter(), 0.4);
		level.sendParticles(BLOOD_DARK, target.getX(), target.getY(0.5), target.getZ(), 25, 0.3, 0.5, 0.3, 0);
		Fx.sound(level, player, SoundEvents.WARDEN_HEARTBEAT, 2.0f, 0.5f);
		return true;
	}

	/** Кровяной взрыв: ослабленная цель лопается, задевая соседей. */
	private static boolean burst(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Fx.Ray ray = Fx.ray(level, player, 32, 0.4);
		LivingEntity target = ray.entity();

		if (target == null) {
			return Moves.noTarget(player);
		}

		if (!pay(level, player, 3)) {
			return false;
		}

		Vec3 c = target.getBoundingBox().getCenter();
		boolean weak = !Fx.isBoss(target) && (target.getHealth() < 20 || target.getHealth() < target.getMaxHealth() * 0.3f);

		if (weak) {
			target.kill(level);
		} else {
			Fx.hurt(level, player, target, 15f);
		}

		for (LivingEntity e : Fx.around(level, player, c, 4)) {
			if (e != target) {
				Fx.hurt(level, player, e, 8f);
			}
		}

		level.sendParticles(BLOOD, c.x, c.y, c.z, 80, 0.8, 0.8, 0.8, 0);
		level.sendParticles(BLOOD_DARK, c.x, c.y, c.z, 40, 1.2, 0.6, 1.2, 0);
		Fx.sound(level, c, SoundEvents.SLIME_SQUISH, 2.0f, 0.4f);
		Fx.sound(level, c, SoundEvents.GENERIC_EXPLODE.value(), 0.6f, 1.6f);
		return true;
	}

	/** Свёртывание: раны закрываются, яды и огонь уходят. */
	private static boolean clot(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.heal(level, player, 10f, BLOOD);
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1));
		Fx.sound(level, player, SoundEvents.WARDEN_HEARTBEAT, 1.5f, 1.2f);
		return true;
	}

	/** Кровавая жатва: кровь всех врагов вокруг течёт к тебе. */
	private static boolean harvest(ServerLevel level, ServerPlayer player, ItemStack stack) {
		List<LivingEntity> targets = Moves.nearby(level, player, 12, false, 20);

		if (targets.isEmpty()) {
			return Moves.noTarget(player);
		}

		for (LivingEntity e : targets) {
			Fx.hurt(level, player, e, 5f);
			Fx.line(level, BLOOD, e.getBoundingBox().getCenter(), player.getBoundingBox().getCenter(), 0.4);
		}

		player.heal(targets.size() * 3f);
		Fx.sound(level, player, SoundEvents.SOUL_ESCAPE.value(), 2.0f, 0.5f);
		return true;
	}

	/** Реактивная струя крови: толчок по взгляду. */
	private static boolean jet(ServerLevel level, ServerPlayer player, ItemStack stack) {
		if (!pay(level, player, 1)) {
			return false;
		}

		Moves.dash(level, player, 2.0, 0.5, 0f, 10, BLOOD);
		Fx.sound(level, player, SoundEvents.SLIME_ATTACK, 1.0f, 0.6f);
		return true;
	}

	private static boolean flight(ServerLevel level, ServerPlayer player, ItemStack stack) {
		return StrangeAbilities.toggleFlight(level, player, BLOOD);
	}

	/** Щит из крови: сфера сжигает снаряды, урон снижен. */
	private static boolean shield(ServerLevel level, ServerPlayer player, ItemStack stack) {
		if (!pay(level, player, 2)) {
			return false;
		}

		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 2));

		Tasks.start(age -> {
			if (player.isRemoved()) {
				return false;
			}

			ServerLevel lv = player.level();
			Moves.destroyProjectiles(lv, player, 3);

			if (age % 2 == 0) {
				Fx.circle(lv, BLOOD, player.position().add(0, 1, 0), new Vec3(0, 1, 0), 1.6, 18, age * 0.3);
				Fx.circle(lv, BLOOD_DARK, player.position().add(0, 1, 0), new Vec3(1, 0, 0), 1.6, 18, -age * 0.3);
			}

			return age < 200;
		});
		return true;
	}

	/** Кровавая броня: защита, сила и шипы. */
	private static boolean armor(ServerLevel level, ServerPlayer player, ItemStack stack) {
		if (!pay(level, player, 2)) {
			return false;
		}

		Moves.buff(player, 400, MobEffects.RESISTANCE, 1, MobEffects.STRENGTH, 1);
		MagicState.THORNS.put(player.getUUID(), level.getGameTime() + 400);
		Moves.aura(player, 400, BLOOD, 0, 0);
		return true;
	}

	/** Кукловод: управляешь телом цели — она сражается за тебя 40 секунд. */
	private static boolean puppet(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Fx.Ray ray = Fx.ray(level, player, 32, 0.4);

		if (!(ray.entity() instanceof Mob mob) || Fx.isBoss(mob)) {
			return Moves.noTarget(player);
		}

		MagicState.charm(mob, player, level.getGameTime() + 800, true);
		mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 800, 0, false, false, true));
		Fx.line(level, BLOOD_LIGHT, Fx.hand(player), mob.getEyePosition(), 0.3);
		Moves.message(player, "message.solorust.magic.mind_control", ChatFormatting.DARK_RED, mob.getName());
		Fx.sound(level, player, SoundEvents.WARDEN_HEARTBEAT, 2.0f, 0.8f);
		return true;
	}

	/** Чутьё крови: видишь каждое бьющееся сердце вокруг. */
	private static boolean sense(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.message(player, "message.solorust.hero.sense", ChatFormatting.DARK_RED, Moves.reveal(level, player, 48, 400));
		Fx.sound(level, player, SoundEvents.WARDEN_HEARTBEAT, 2.0f, 1.0f);
		return true;
	}

	/** Кровавые цепи: до 5 врагов в поле зрения стянуты в одну точку и скованы. */
	private static boolean chains(ServerLevel level, ServerPlayer player, ItemStack stack) {
		List<LivingEntity> targets = new ArrayList<>(Fx.cone(level, player, 24, 35));

		if (targets.isEmpty()) {
			return Moves.noTarget(player);
		}

		if (!pay(level, player, 2)) {
			return false;
		}

		Vec3 point = Moves.groundAt(level, player, 24);
		int n = 0;

		for (LivingEntity e : targets) {
			if (n++ >= 5) {
				break;
			}

			Fx.line(level, BLOOD, e.getBoundingBox().getCenter(), point.add(0, 1, 0), 0.3);
			e.teleportTo(point.x + (n - 3) * 0.6, point.y, point.z);
			Fx.hurt(level, player, e, 6f);

			if (!(e instanceof Player)) {
				Freeze.freeze(e, level.getGameTime() + 60);
			}
		}

		Fx.sound(level, point, SoundEvents.CHAIN_PLACE, 2.0f, 0.5f);
		return true;
	}

	/** Кровавый дождь: 4 секунды иглы крови сыплются на точку. */
	private static boolean rain(ServerLevel level, ServerPlayer player, ItemStack stack) {
		if (!pay(level, player, 2)) {
			return false;
		}

		Vec3 c = Moves.groundAt(level, player, 48);

		Tasks.start(age -> {
			RandomSource random = level.getRandom();

			for (int i = 0; i < 6; i++) {
				Vec3 p = c.add((random.nextDouble() - 0.5) * 12, 8, (random.nextDouble() - 0.5) * 12);
				Fx.line(level, BLOOD_LIGHT, p, p.add(0, -8, 0), 0.8);
			}

			if (age % 5 == 0) {
				for (LivingEntity e : Fx.around(level, player, c.add(0, 1, 0), 6.5)) {
					Fx.hurt(level, player, e, 3f);
				}
			}

			return age < 80;
		});
		return true;
	}

	/** Остановка сердца: все враги вокруг получают огромный урон, ослабленные — гибнут. */
	private static boolean heartStop(ServerLevel level, ServerPlayer player, ItemStack stack) {
		List<LivingEntity> targets = Moves.nearby(level, player, 20, true, 40);

		if (targets.isEmpty()) {
			return Moves.noTarget(player);
		}

		if (!pay(level, player, 6)) {
			return false;
		}

		for (LivingEntity e : targets) {
			Vec3 c = e.getBoundingBox().getCenter();

			if (!Fx.isBoss(e) && e.getHealth() < e.getMaxHealth() * 0.4f) {
				e.kill(level);
			} else {
				Fx.hurt(level, player, e, 22f);
			}

			level.sendParticles(BLOOD, c.x, c.y, c.z, 30, 0.4, 0.5, 0.4, 0);
		}

		Fx.sound(level, player, SoundEvents.WARDEN_HEARTBEAT, 3.0f, 0.5f);
		Fx.sound(level, player, SoundEvents.WARDEN_SONIC_BOOM, 1.0f, 0.5f);
		return true;
	}

	/** Кровавый вихрь: 6 секунд затягивает и ранит всех вокруг, а кровь лечит тебя. */
	private static boolean vortex(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Tasks.start(age -> {
			if (player.isRemoved() || !player.isAlive()) {
				return false;
			}

			ServerLevel lv = player.level();
			Vec3 c = player.position();

			for (int i = 0; i < 6; i++) {
				double a = age * 0.4 + i * Math.PI / 3;
				double r = 2 + (age % 15) * 0.35;
				lv.sendParticles(i % 2 == 0 ? BLOOD : BLOOD_DARK, true, false, c.x + Math.cos(a) * r, c.y + 0.4 + i * 0.3, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
			}

			for (LivingEntity e : Fx.around(lv, player, c.add(0, 1, 0), 8)) {
				Vec3 to = c.subtract(e.position());
				e.setDeltaMovement(e.getDeltaMovement().scale(0.5).add(to.normalize().scale(0.25)));
				e.hurtMarked = true;

				if (age % 10 == 0) {
					Fx.hurt(lv, player, e, 4f);
					player.heal(1f);
				}
			}

			return age < 120;
		});
		Fx.sound(level, player, SoundEvents.WARDEN_HEARTBEAT, 2.0f, 0.7f);
		return true;
	}

	static void bleed(ServerLevel level, Vec3 at) {
		level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, at.x, at.y, at.z, 3, 0.2, 0.2, 0.2, 0);
	}
}
