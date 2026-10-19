package com.shxdowrate.solorust.magic;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Мьёльнир: 18 способностей Тора. */
final class ThorAbilities {
	private static final DustParticleOptions BOLT = Fx.dust(0x8FD8FF, 1.2f);
	private static final DustParticleOptions BOLT_WHITE = Fx.dust(0xF0FBFF, 0.8f);
	private static final DustParticleOptions STEEL = Fx.dust(0xB8C2CC, 1.3f);
	private static final int[] RAINBOW = {0xFF3B3B, 0xFF9A1F, 0xFFE11F, 0x3BE04A, 0x2E9BFF, 0x8E44FF};

	static final int[] COLORS = {0xB8C2CC, 0x8FD8FF, 0x4A7BD8, 0x9BB8FF, 0x6EC6FF, 0xFFD54A, 0xC6E8FF, 0x70A0FF, 0xFFFFFF};

	static final Ability[] ABILITIES = {
			// 1 — Молот
			new Ability("thor_throw", 30, ThorAbilities::hammerThrow),
			new Ability("thor_slam", 100, ThorAbilities::hammerSlam),
			// 2 — Молнии
			new Ability("thor_lightning", 25, ThorAbilities::lightning),
			new Ability("thor_chain", 60, ThorAbilities::chainLightning),
			// 3 — Буря
			new Ability("thor_storm", 500, ThorAbilities::storm),
			new Ability("thor_flight", 10, ThorAbilities::flight),
			// 4 — Движение
			new Ability("thor_hammer_dash", 40, ThorAbilities::hammerDash),
			new Ability("thor_spin", 300, ThorAbilities::spinShield),
			// 5 — Гром
			new Ability("thor_thunderclap", 80, ThorAbilities::thunderclap),
			new Ability("thor_charge", 800, ThorAbilities::supercharge),
			// 6 — Биврёст
			new Ability("thor_bifrost", 200, ThorAbilities::bifrost),
			new Ability("thor_bifrost_home", 600, ThorAbilities::bifrostHome),
			// 7 — Бог грома
			new Ability("thor_godslam", 300, ThorAbilities::godSlam),
			new Ability("thor_weather", 200, ThorAbilities::weather),
			// 8 — Разряды и магнетизм
			new Ability("thor_barrage", 80, ThorAbilities::barrage),
			new Ability("thor_magnet", 100, ThorAbilities::magnet),
			// 9 — Асгард
			new Ability("thor_wrath", 3000, ThorAbilities::wrath),
			new Ability("thor_odin", 2400, ThorAbilities::odinBlessing),
	};

	private ThorAbilities() {
	}

	/** Бросок Мьёльнира: летит вперёд, бьёт всех на пути и возвращается в руку. */
	private static boolean hammerThrow(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 look = player.getViewVector(1f);
		Vec3[] pos = {Fx.hand(player)};
		Set<UUID> hitOut = new HashSet<>();
		Set<UUID> hitBack = new HashSet<>();
		Fx.sound(level, player, SoundEvents.TRIDENT_RIPTIDE_3.value(), 1.0f, 0.8f);

		Tasks.start(age -> {
			boolean returning = age >= 16;
			Vec3 target = returning ? Fx.hand(player) : null;
			Vec3 step = returning ? target.subtract(pos[0]).normalize().scale(2.2) : look.scale(2.0);

			if (!returning) {
				BlockHitResult hit = level.clip(new ClipContext(pos[0], pos[0].add(step), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

				if (hit.getType() != HitResult.Type.MISS) {
					step = hit.getLocation().subtract(pos[0]);
				}
			}

			Vec3 next = pos[0].add(step);

			for (LivingEntity e : Fx.pierce(level, player, pos[0], next, 0.7)) {
				if ((returning ? hitBack : hitOut).add(e.getUUID())) {
					Fx.hurt(level, player, e, returning ? 8f : 12f);
					Fx.push(e, step, 1.0, 0.3);
					level.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY(0.6), e.getZ(), 10, 0.3, 0.3, 0.3, 0.3);
				}
			}

			Fx.line(level, STEEL, pos[0], next, 0.5);
			level.sendParticles(BOLT, true, false, next.x, next.y, next.z, 4, 0.2, 0.2, 0.2, 0);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, true, false, next.x, next.y, next.z, 2, 0.1, 0.1, 0.1, 0.1);
			pos[0] = next;

			if (returning && next.distanceTo(Fx.hand(player)) < 2.5) {
				Fx.sound(level, player, SoundEvents.ANVIL_LAND, 0.5f, 1.8f);
				return false;
			}

			return age < 60;
		});
		return true;
	}

	/** Удар молотом о землю: кольцо молний и ударная волна. */
	private static boolean hammerSlam(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 c = player.position();
		Moves.nova(level, player, c.add(0, 1, 0), 8, 14f, 2.0, 0.6, BOLT);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 0.3, c.z, 1, 0, 0, 0, 0);

		for (int i = 0; i < 6; i++) {
			double a = i * Math.PI / 3;
			Moves.jagged(level, BOLT_WHITE, c.add(0, 0.2, 0), c.add(Math.cos(a) * 7, 0.2, Math.sin(a) * 7));
		}

		Fx.sound(level, player, SoundEvents.MACE_SMASH_GROUND_HEAVY, 2.0f, 0.5f);
		Fx.sound(level, player, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.5f, 1.0f);
		return true;
	}

	/** Молния в точку, куда смотришь. */
	private static boolean lightning(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 at = Moves.groundAt(level, player, 96);
		Moves.lightning(level, player, at, 16f, 3.5);
		return true;
	}

	/** Цепная молния: перескакивает до 7 целей. */
	private static boolean chainLightning(ServerLevel level, ServerPlayer player, ItemStack stack) {
		int n = Moves.chain(level, player, 48, 7, 9, 11f, BOLT, BOLT_WHITE);

		if (n > 0) {
			Fx.sound(level, player, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.2f, 1.4f);
		}

		return n > 0;
	}

	/** Буря: 15 секунд молнии сами бьют врагов вокруг. */
	private static boolean storm(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Fx.sound(level, player, SoundEvents.LIGHTNING_BOLT_THUNDER, 2.0f, 0.6f);

		Tasks.start(age -> {
			if (player.isRemoved() || !player.isAlive()) {
				return false;
			}

			ServerLevel lv = player.level();

			if (age % 4 == 0) {
				Vec3 sky = player.position().add((lv.getRandom().nextDouble() - 0.5) * 16, 10, (lv.getRandom().nextDouble() - 0.5) * 16);
				lv.sendParticles(ParticleTypes.CLOUD, sky.x, sky.y, sky.z, 6, 2, 0.3, 2, 0.01);
			}

			if (age % 10 == 0) {
				List<LivingEntity> targets = Moves.nearby(lv, player, 24, true, 30);

				if (!targets.isEmpty()) {
					LivingEntity t = targets.get(lv.getRandom().nextInt(targets.size()));
					Moves.lightning(lv, player, t.position(), 10f, 2.5);
				}
			}

			return age < 300;
		});
		return true;
	}

	private static boolean flight(ServerLevel level, ServerPlayer player, ItemStack stack) {
		return StrangeAbilities.toggleFlight(level, player, BOLT);
	}

	/** Рывок за брошенным молотом. */
	private static boolean hammerDash(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.dash(level, player, 3.0, 0.3, 9f, 12, BOLT);
		Fx.sound(level, player, SoundEvents.TRIDENT_RIPTIDE_3.value(), 1.0f, 1.0f);
		return true;
	}

	/** Раскрученный молот: 8 секунд отражает все снаряды и бьёт тех, кто рядом. */
	private static boolean spinShield(ServerLevel level, ServerPlayer player, ItemStack stack) {
		MagicState.DEFLECT.put(player.getUUID(), level.getGameTime() + 160);
		Fx.sound(level, player, SoundEvents.TRIDENT_RIPTIDE_3.value(), 1.0f, 1.4f);

		Tasks.start(age -> {
			if (player.isRemoved()) {
				return false;
			}

			ServerLevel lv = player.level();
			Vec3 c = player.getBoundingBox().getCenter();
			Vec3 look = player.getViewVector(1f);
			Fx.circle(lv, STEEL, c.add(look.scale(1.2)), look, 1.0, 12, age * 0.9);

			if (age % 10 == 0) {
				for (LivingEntity e : Fx.around(lv, player, c, 2.5)) {
					Fx.hurt(lv, player, e, 4f);
					Fx.push(e, e.position().subtract(player.position()), 1.0, 0.3);
				}
			}

			return age < 160;
		});
		return true;
	}

	/** Громовой хлопок: ударная волна в конусе. */
	private static boolean thunderclap(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.cone(level, player, 14, 40, 10f, 3.2, 0.7, ParticleTypes.ELECTRIC_SPARK);
		Fx.sound(level, player, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.5f, 1.4f);
		Fx.sound(level, player, SoundEvents.WARDEN_SONIC_BOOM, 1.0f, 1.2f);
		return true;
	}

	/** Заряд молнией: 20 секунд силы, скорости и электрической ауры. */
	private static boolean supercharge(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.buff(player, 400, MobEffects.STRENGTH, 1, MobEffects.SPEED, 1, MobEffects.RESISTANCE, 0, MobEffects.HASTE, 1);
		Moves.aura(player, 400, BOLT, 3, 4f);
		Moves.lightning(level, player, player.position(), 0f, 0);
		player.clearFire();
		return true;
	}

	/** Биврёст: радужный столб переносит туда, куда смотришь (до 256 блоков). */
	private static boolean bifrost(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getViewVector(1f).scale(256));
		BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 dest = hit.getType() == HitResult.Type.MISS ? Moves.surface(level, end.x, end.z) : Fx.standingSpot(level, hit, end);
		rainbow(level, player.position());
		player.teleportTo(dest.x, dest.y, dest.z);
		player.fallDistance = 0;
		player.setDeltaMovement(Vec3.ZERO);
		player.hurtMarked = true;
		rainbow(level, dest);
		Moves.nova(level, player, dest.add(0, 1, 0), 4, 8f, 1.5, 0.5, BOLT);
		return true;
	}

	/** Биврёст домой: к точке возрождения. */
	private static boolean bifrostHome(ServerLevel level, ServerPlayer player, ItemStack stack) {
		ServerPlayer.RespawnConfig config = player.getRespawnConfig();
		LevelData.RespawnData data = config != null ? config.respawnData() : level.getServer().overworld().getRespawnData();
		GlobalPos home = data.globalPos();
		ServerLevel homeLevel = level.getServer().getLevel(home.dimension());

		if (homeLevel == null) {
			homeLevel = level.getServer().overworld();
		}

		Vec3 dest = config != null
				? Vec3.atBottomCenterOf(home.pos()).add(0, 0.6, 0)
				: Moves.surface(homeLevel, home.pos().getX() + 0.5, home.pos().getZ() + 0.5);
		rainbow(level, player.position());
		player.teleportTo(homeLevel, dest.x, dest.y, dest.z, Set.of(), player.getYRot(), player.getXRot(), false);
		player.fallDistance = 0;
		rainbow(homeLevel, dest);
		return true;
	}

	private static void rainbow(ServerLevel level, Vec3 at) {
		Tasks.start(age -> {
			for (int i = 0; i < RAINBOW.length; i++) {
				double a = i * Math.PI / 3 + age * 0.3;
				Vec3 base = at.add(Math.cos(a) * 0.7, 0, Math.sin(a) * 0.7);

				for (double y = 0; y < 30; y += 1.5) {
					level.sendParticles(Fx.dust(RAINBOW[i], 1.6f), true, false, base.x, base.y + y + (age % 3) * 0.5, base.z, 1, 0, 0, 0, 0);
				}
			}

			return age < 14;
		});
		Fx.sound(level, at, SoundEvents.BEACON_ACTIVATE, 2.0f, 1.6f);
		Fx.sound(level, at, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0f, 1.8f);
	}

	/** Удар Громовержца: прыжок ввысь и падение с огромным взрывом молний. */
	private static boolean godSlam(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.leapSlam(level, player, 1.6, 1.2, 10, 22f, BOLT, () -> {
			ServerLevel lv = player.level();
			Vec3 c = player.position();

			for (int i = 0; i < 5; i++) {
				double a = i * Math.PI * 2 / 5;
				Moves.lightning(lv, player, c.add(Math.cos(a) * 5, 0, Math.sin(a) * 5), 8f, 2.5);
			}
		});
		Fx.sound(level, player, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.5f, 0.8f);
		return true;
	}

	/** Призвать грозу или разогнать тучи. */
	private static boolean weather(ServerLevel level, ServerPlayer player, ItemStack stack) {
		boolean storm = !level.isThundering();

		if (storm) {
			level.getServer().setWeatherParameters(0, 6000, true, true);
		} else {
			level.getServer().setWeatherParameters(12000, 0, false, false);
		}

		Moves.lightning(level, player, player.position().add(Fx.flatLook(player).scale(6)), 0f, 0);
		Moves.message(player, storm ? "message.solorust.hero.storm_on" : "message.solorust.hero.storm_off", ChatFormatting.AQUA);
		return true;
	}

	/** Шквал разрядов: 6 молний подряд в цель под прицелом. */
	private static boolean barrage(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Tasks.start(age -> {
			if (age % 3 == 0) {
				Fx.Ray ray = Fx.ray(player.level(), player, 40, 0.4);
				Moves.jagged(player.level(), BOLT, Fx.hand(player), ray.end());

				if (ray.entity() != null) {
					Fx.hurt(player.level(), player, ray.entity(), 5f);
				}

				Fx.sound(player.level(), player, SoundEvents.FIRECHARGE_USE, 0.5f, 1.8f);
			}

			return age < 17;
		});
		return true;
	}

	/** Электромагнит: стягивает к тебе все предметы и опыт вокруг. */
	private static boolean magnet(ServerLevel level, ServerPlayer player, ItemStack stack) {
		int n = Moves.magnet(level, player, 24, BOLT);
		Moves.message(player, "message.solorust.hero.magnet", ChatFormatting.AQUA, n);
		return true;
	}

	/** Гнев Асгарда: молнии в каждого врага в радиусе 32. */
	private static boolean wrath(ServerLevel level, ServerPlayer player, ItemStack stack) {
		List<LivingEntity> targets = Moves.nearby(level, player, 32, true, 30);

		if (targets.isEmpty()) {
			return Moves.noTarget(player);
		}

		level.getServer().setWeatherParameters(0, 1200, true, true);

		Tasks.start(age -> {
			int i = age / 2;

			if (age % 2 == 0 && i < targets.size()) {
				LivingEntity t = targets.get(i);

				if (t.isAlive()) {
					Moves.lightning(level, player, t.position(), 26f, 2.5);
				}
			}

			return i < targets.size();
		});
		Fx.sound(level, player, SoundEvents.LIGHTNING_BOLT_THUNDER, 3.0f, 0.5f);
		return true;
	}

	/** Благословение Одина: полное исцеление и защита Асгарда. */
	private static boolean odinBlessing(ServerLevel level, ServerPlayer player, ItemStack stack) {
		player.setHealth(player.getMaxHealth());
		Moves.heal(level, player, 0, Fx.dust(0xFFD54A, 1.3f));
		Moves.buff(player, 400, MobEffects.RESISTANCE, 2, MobEffects.FIRE_RESISTANCE, 0, MobEffects.REGENERATION, 1, MobEffects.ABSORPTION, 2);
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY(1), player.getZ(), 40, 0.5, 0.8, 0.5, 0.4);
		Fx.sound(level, player, SoundEvents.TOTEM_USE, 0.8f, 1.2f);
		return true;
	}
}
