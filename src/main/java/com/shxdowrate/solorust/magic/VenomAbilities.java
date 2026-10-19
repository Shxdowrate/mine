package com.shxdowrate.solorust.magic;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Симбиот: 18 способностей Венома. */
final class VenomAbilities {
	private static final DustParticleOptions BLACK = Fx.dust(0x0A0A0F, 1.5f);
	private static final DustParticleOptions OIL = Fx.dust(0x262636, 1.0f);
	private static final DustParticleOptions WHITE = Fx.dust(0xEEEEEE, 0.9f);
	private static final DustParticleOptions TONGUE = Fx.dust(0xC2185B, 1.0f);

	static final int[] COLORS = {0x9E9EAE, 0xEEEEEE, 0xC2185B, 0x707088, 0xFFFFFF, 0x5E5E7A, 0xD32F2F, 0x8888AA, 0xFF1744};

	static final Ability[] ABILITIES = {
			// 1 — Щупальца
			new Ability("venom_lash", 20, VenomAbilities::lash),
			new Ability("venom_tendrils", 100, VenomAbilities::tendrils),
			// 2 — Движение
			new Ability("venom_grapple", 20, VenomAbilities::grapple),
			new Ability("venom_pounce", 60, VenomAbilities::pounce),
			// 3 — Голод
			new Ability("venom_bite", 80, VenomAbilities::bite),
			new Ability("venom_armor", 600, VenomAbilities::armor),
			// 4 — Охота
			new Ability("venom_spikes", 80, VenomAbilities::spikes),
			new Ability("venom_sense", 400, VenomAbilities::sense),
			// 5 — Хватка
			new Ability("venom_throw", 60, VenomAbilities::grabThrow),
			new Ability("venom_cocoon", 300, VenomAbilities::cocoon),
			// 6 — Скрытность
			new Ability("venom_camouflage", 600, VenomAbilities::camouflage),
			new Ability("venom_regen", 600, VenomAbilities::regen),
			// 7 — Зверь
			new Ability("venom_roar", 200, VenomAbilities::roar),
			new Ability("venom_beast", 900, VenomAbilities::beast),
			// 8 — Западня
			new Ability("venom_trap", 300, VenomAbilities::trap),
			new Ability("venom_pull_all", 200, VenomAbilities::pullAll),
			// 9 — Мы — Веном
			new Ability("venom_frenzy", 2400, VenomAbilities::frenzy),
			new Ability("venom_we_are", 3000, VenomAbilities::weAreVenom),
	};

	private VenomAbilities() {
	}

	private static boolean lash(ServerLevel level, ServerPlayer player, ItemStack stack) {
		return Moves.whip(level, player, 12, 9f, 1.6, BLACK, OIL, SoundEvents.SLIME_ATTACK);
	}

	/** Щупальца во все стороны. */
	private static boolean tendrils(ServerLevel level, ServerPlayer player, ItemStack stack) {
		List<LivingEntity> targets = Moves.nearby(level, player, 6, false, 20);

		for (LivingEntity e : targets) {
			Fx.line(level, BLACK, player.getBoundingBox().getCenter(), e.getBoundingBox().getCenter(), 0.3);
			Fx.hurt(level, player, e, 9f);
			Fx.push(e, e.position().subtract(player.position()), 1.4, 0.4);
		}

		for (int i = 0; i < 10; i++) {
			double a = i * Math.PI / 5;
			Vec3 c = player.getBoundingBox().getCenter();
			Fx.line(level, OIL, c, c.add(Math.cos(a) * 5, Math.sin(i) * 1.5, Math.sin(a) * 5), 0.4);
		}

		Fx.sound(level, player, SoundEvents.SLIME_SQUISH, 1.5f, 0.5f);
		return true;
	}

	/** Гарпун: симбиот цепляется за стену и тянет тебя к ней. */
	private static boolean grapple(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 eye = player.getEyePosition();
		BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getViewVector(1f).scale(40)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

		if (hit.getType() != HitResult.Type.BLOCK) {
			return Moves.noTarget(player);
		}

		Vec3 to = hit.getLocation().subtract(eye);
		player.setDeltaMovement(to.normalize().scale(Math.min(3.2, 0.9 + to.length() * 0.12)).add(0, 0.35, 0));
		player.hurtMarked = true;
		player.fallDistance = 0;
		MagicState.FALL_SAFE.put(player.getUUID(), level.getGameTime() + 80);
		Fx.line(level, BLACK, Fx.hand(player), hit.getLocation(), 0.3);
		Fx.sound(level, player, SoundEvents.SLIME_ATTACK, 1.0f, 0.7f);
		return true;
	}

	/** Прыжок хищника. */
	private static boolean pounce(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.dash(level, player, 2.2, 0.9, 8f, 14, BLACK);
		Fx.sound(level, player, SoundEvents.PHANTOM_FLAP, 1.0f, 0.6f);
		return true;
	}

	/** Укус: большой урон, половина возвращается здоровьем и сытостью. */
	private static boolean bite(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Fx.Ray ray = Fx.ray(level, player, 5, 0.6);
		LivingEntity target = ray.entity();

		if (target == null) {
			return Moves.noTarget(player);
		}

		float before = target.getHealth();
		Fx.hurt(level, player, target, 16f);
		float dealt = Math.max(2f, before - Math.max(0, target.getHealth()));
		player.heal(dealt * 0.5f);
		player.getFoodData().eat(4, 0.6f);
		level.sendParticles(TONGUE, target.getX(), target.getY(0.8), target.getZ(), 15, 0.3, 0.3, 0.3, 0);
		Fx.sound(level, player, SoundEvents.PHANTOM_BITE, 1.2f, 0.6f);
		Fx.sound(level, player, SoundEvents.GENERIC_EAT.value(), 1.0f, 0.6f);
		return true;
	}

	/** Броня симбиота: защита и шипы — каждый ударивший получает урон. */
	private static boolean armor(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.buff(player, 400, MobEffects.RESISTANCE, 2, MobEffects.ABSORPTION, 3);
		MagicState.THORNS.put(player.getUUID(), level.getGameTime() + 400);
		Moves.aura(player, 400, BLACK, 0, 0);
		Fx.sound(level, player, SoundEvents.SLIME_SQUISH, 1.5f, 0.4f);
		return true;
	}

	/** Шипы из тела в конусе. */
	private static boolean spikes(ServerLevel level, ServerPlayer player, ItemStack stack) {
		for (LivingEntity e : Fx.cone(level, player, 10, 35)) {
			Fx.hurt(level, player, e, 11f);
			e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 2));
			Fx.line(level, BLACK, Fx.hand(player), e.getBoundingBox().getCenter(), 0.3);
		}

		Moves.cone(level, player, 10, 35, 0, 0, 0, OIL);
		Fx.sound(level, player, SoundEvents.SLIME_ATTACK, 1.5f, 1.4f);
		return true;
	}

	/** Чутьё хищника: все живые вокруг подсвечены 30 секунд. */
	private static boolean sense(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.message(player, "message.solorust.hero.sense", ChatFormatting.WHITE, Moves.reveal(level, player, 64, 600));
		Fx.sound(level, player, SoundEvents.WARDEN_HEARTBEAT, 2.0f, 1.0f);
		return true;
	}

	private static boolean grabThrow(ServerLevel level, ServerPlayer player, ItemStack stack) {
		return Moves.grabAndThrow(level, player, 10, 15, 3.2, 8f, BLACK);
	}

	/** Кокон: цель обмотана симбиотом на 8 секунд. */
	private static boolean cocoon(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Fx.Ray ray = Fx.ray(level, player, 24, 0.4);
		LivingEntity target = ray.entity();

		if (target == null) {
			return Moves.noTarget(player);
		}

		if (target instanceof Player) {
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 160, 9));
			target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 160, 0));
		} else {
			Freeze.freeze(target, level.getGameTime() + 160);
		}

		Fx.line(level, BLACK, Fx.hand(player), target.getBoundingBox().getCenter(), 0.3);

		Tasks.start(age -> {
			if (target.isRemoved()) {
				return false;
			}

			if (age % 3 == 0) {
				for (double y = 0; y < target.getBbHeight(); y += 0.3) {
					Fx.circle(level, age % 6 == 0 ? BLACK : WHITE, target.position().add(0, y, 0), new Vec3(0, 1, 0), target.getBbWidth() * 0.7, 8, age * 0.3 + y);
				}
			}

			return age < 160;
		});
		Fx.sound(level, player, SoundEvents.SLIME_SQUISH, 1.2f, 0.7f);
		return true;
	}

	/** Камуфляж: невидимость, мобы теряют тебя. */
	private static boolean camouflage(ServerLevel level, ServerPlayer player, ItemStack stack) {
		player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 600, 0, false, false, true));
		MagicState.STEALTH.put(player.getUUID(), level.getGameTime() + 600);
		MagicState.dropAggro(level, player);
		level.sendParticles(BLACK, player.getX(), player.getY(1), player.getZ(), 40, 0.4, 0.8, 0.4, 0);
		Fx.sound(level, player, SoundEvents.SLIME_SQUISH, 1.0f, 1.2f);
		return true;
	}

	/** Регенерация симбиота: лечит, но сжигает еду. */
	private static boolean regen(ServerLevel level, ServerPlayer player, ItemStack stack) {
		if (player.getFoodData().getFoodLevel() < 6 && !player.hasInfiniteMaterials()) {
			Moves.message(player, "message.solorust.hero.hungry", ChatFormatting.GRAY);
			return false;
		}

		Moves.heal(level, player, player.getMaxHealth() * 0.5f, BLACK);
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 2));
		player.getFoodData().setFoodLevel(Math.max(0, player.getFoodData().getFoodLevel() - 6));
		Fx.sound(level, player, SoundEvents.SLIME_SQUISH, 1.0f, 0.8f);
		return true;
	}

	/** Рык: враги в страхе бегут и слабеют. */
	private static boolean roar(ServerLevel level, ServerPlayer player, ItemStack stack) {
		for (LivingEntity e : Fx.cone(level, player, 16, 60)) {
			e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
			Fx.push(e, player.getViewVector(1f), 1.8, 0.3);

			if (e instanceof Mob mob) {
				mob.setTarget(null);
			} else {
				e.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 160, 0));
			}
		}

		Moves.cone(level, player, 16, 60, 0, 0, 0, TONGUE);
		Fx.sound(level, player, SoundEvents.WARDEN_ROAR, 1.5f, 1.2f);
		return true;
	}

	/** Зверь: 20 секунд силы, скорости и прыжков. */
	private static boolean beast(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.buff(player, 400, MobEffects.STRENGTH, 2, MobEffects.SPEED, 1, MobEffects.JUMP_BOOST, 2);
		Moves.aura(player, 400, OIL, 0, 0);
		Fx.sound(level, player, SoundEvents.RAVAGER_ROAR, 1.0f, 1.2f);
		return true;
	}

	/** Западня: щупальца из земли сковывают всех в точке. */
	private static boolean trap(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 c = Moves.groundAt(level, player, 32);
		List<LivingEntity> targets = Fx.around(level, player, c.add(0, 1, 0), 5);

		for (LivingEntity e : targets) {
			Fx.hurt(level, player, e, 6f);

			if (e instanceof Player) {
				e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 9));
			} else {
				Freeze.freeze(e, level.getGameTime() + 100);
			}

			Fx.line(level, BLACK, e.position(), e.position().add(0, e.getBbHeight(), 0), 0.2);
		}

		for (int i = 0; i < 12; i++) {
			double a = i * Math.PI / 6;
			Vec3 base = c.add(Math.cos(a) * 3, 0, Math.sin(a) * 3);
			Fx.line(level, OIL, base, base.add(0, 2.5, 0), 0.3);
		}

		Fx.sound(level, c, SoundEvents.SLIME_ATTACK, 1.5f, 0.5f);
		return true;
	}

	/** Стянуть всех врагов вокруг к себе. */
	private static boolean pullAll(ServerLevel level, ServerPlayer player, ItemStack stack) {
		List<LivingEntity> targets = Moves.nearby(level, player, 14, true, 15);

		if (targets.isEmpty()) {
			return Moves.noTarget(player);
		}

		for (LivingEntity e : targets) {
			Fx.line(level, BLACK, player.getBoundingBox().getCenter(), e.getBoundingBox().getCenter(), 0.4);
			Vec3 to = player.position().subtract(e.position());
			Fx.push(e, to, Math.min(2.5, 0.5 + to.length() * 0.15), 0.4);
			Fx.hurt(level, player, e, 4f);
		}

		Fx.sound(level, player, SoundEvents.SLIME_ATTACK, 1.5f, 0.6f);
		return true;
	}

	/** Ярость: 6 секунд щупальца хлещут всех вокруг. */
	private static boolean frenzy(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Fx.sound(level, player, SoundEvents.WARDEN_ROAR, 2.0f, 0.8f);

		Tasks.start(age -> {
			if (player.isRemoved() || !player.isAlive()) {
				return false;
			}

			ServerLevel lv = player.level();

			if (age % 4 == 0) {
				List<LivingEntity> targets = Moves.nearby(lv, player, 12, false, 20);

				if (!targets.isEmpty()) {
					LivingEntity t = targets.get(lv.getRandom().nextInt(targets.size()));
					Fx.line(lv, BLACK, player.getBoundingBox().getCenter(), t.getBoundingBox().getCenter(), 0.3);
					Fx.hurt(lv, player, t, 8f);
					player.heal(1f);
				}
			}

			return age < 120;
		});
		return true;
	}

	/** «Мы — Веном»: 30 секунд полной формы симбиота. */
	private static boolean weAreVenom(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.buff(player, 600, MobEffects.STRENGTH, 3, MobEffects.RESISTANCE, 2, MobEffects.SPEED, 1,
				MobEffects.REGENERATION, 1, MobEffects.JUMP_BOOST, 2, MobEffects.NIGHT_VISION, 0);
		MagicState.THORNS.put(player.getUUID(), level.getGameTime() + 600);
		Moves.aura(player, 600, BLACK, 3, 3f);
		level.sendParticles(BLACK, player.getX(), player.getY(1), player.getZ(), 80, 0.5, 1, 0.5, 0);
		Fx.sound(level, player, SoundEvents.WARDEN_ROAR, 2.0f, 0.6f);
		Moves.message(player, "message.solorust.hero.we_are_venom", ChatFormatting.WHITE);
		return true;
	}
}
