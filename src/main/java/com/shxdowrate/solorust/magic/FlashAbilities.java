package com.shxdowrate.solorust.magic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Кольцо Флэша: 18 способностей Спидфорса. */
final class FlashAbilities {
	private static final DustParticleOptions SPARK = Fx.dust(0xFFD21F, 1.1f);
	private static final DustParticleOptions SPARK_WHITE = Fx.dust(0xFFF6C2, 0.7f);
	private static final DustParticleOptions RED = Fx.dust(0xC62828, 1.0f);

	static final int[] COLORS = {0xFFD21F, 0xFFB300, 0xFFE57F, 0x90CAF9, 0xFFF176, 0xC62828, 0x26C6DA, 0xFFAB00, 0xFFFFFF};

	static final Ability[] ABILITIES = {
			// 1 — Скорость
			new Ability("flash_speedforce", 600, FlashAbilities::speedforce),
			new Ability("flash_dash", 30, FlashAbilities::dash),
			// 2 — Удары
			new Ability("flash_flurry", 80, FlashAbilities::flurry),
			new Ability("flash_lightning_throw", 40, FlashAbilities::lightningThrow),
			// 3 — Вихри
			new Ability("flash_tornado", 300, FlashAbilities::tornado),
			new Ability("flash_mach_punch", 80, FlashAbilities::machPunch),
			// 4 — Восприятие
			new Ability("flash_slowmo", 600, FlashAbilities::slowMotion),
			new Ability("flash_phase", 60, FlashAbilities::phase),
			// 5 — Ловкость
			new Ability("flash_disarm", 300, FlashAbilities::disarm),
			new Ability("flash_collect", 100, FlashAbilities::collect),
			// 6 — Сила массы
			new Ability("flash_superpunch", 100, FlashAbilities::superPunch),
			new Ability("flash_infinite_mass", 600, FlashAbilities::infiniteMass),
			// 7 — Время
			new Ability("flash_time_run", 400, FlashAbilities::timeRun),
			new Ability("flash_barrier", 400, FlashAbilities::barrier),
			// 8 — Молниеносность
			new Ability("flash_blitz", 300, FlashAbilities::blitz),
			new Ability("flash_metabolism", 600, FlashAbilities::metabolism),
			// 9 — Спидфорс
			new Ability("flash_storm", 2400, FlashAbilities::speedStorm),
			new Ability("flash_sonic_boom", 1200, FlashAbilities::sonicBoom),
	};

	private FlashAbilities() {
	}

	/** Спидфорс: 30 секунд огромной скорости с молниями за спиной. */
	private static boolean speedforce(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.buff(player, 600, MobEffects.SPEED, 6, MobEffects.JUMP_BOOST, 1, MobEffects.HASTE, 2);
		trail(player, 600);
		Fx.sound(level, player, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6f, 2.0f);
		return true;
	}

	private static void trail(ServerPlayer player, int ticks) {
		Tasks.start(age -> {
			if (player.isRemoved()) {
				return false;
			}

			if (player.getDeltaMovement().horizontalDistanceSqr() > 0.02 || age % 4 == 0) {
				ServerLevel lv = player.level();
				lv.sendParticles(SPARK, player.getX(), player.getY(0.5), player.getZ(), 3, 0.25, 0.4, 0.25, 0);
				lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY(0.5), player.getZ(), 1, 0.25, 0.4, 0.25, 0.05);
			}

			return age < ticks;
		});
	}

	/** Рывок на 24 блока по земле: сбивает всех на пути. */
	private static boolean dash(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 dir = Fx.flatLook(player);
		Vec3 from = player.position();
		Vec3 to = from;

		for (int i = 1; i <= 24; i++) {
			Vec3 next = from.add(dir.scale(i));
			BlockPos feet = BlockPos.containing(next);

			if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()) {
				// Ступенька — забегаем на неё.
				if (level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
						&& level.getBlockState(feet.above(2)).getCollisionShape(level, feet.above(2)).isEmpty()) {
					next = next.add(0, 1, 0);
					from = from.add(0, 1, 0);
				} else {
					break;
				}
			}

			to = next;
		}

		for (LivingEntity e : Fx.pierce(level, player, from.add(0, 1, 0), to.add(0, 1, 0), 0.8)) {
			Fx.hurt(level, player, e, 9f);
			Fx.push(e, dir, 1.5, 0.4);
		}

		Moves.jagged(level, SPARK, player.position().add(0, 1, 0), to.add(0, 1, 0));
		player.teleportTo(to.x, to.y, to.z);
		player.fallDistance = 0;
		Fx.sound(level, player, SoundEvents.ELYTRA_FLYING, 0.5f, 2.0f);
		return true;
	}

	/** Град ударов: 12 молниеносных ударов по цели. */
	private static boolean flurry(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Fx.Ray ray = Fx.ray(level, player, 8, 0.6);
		LivingEntity target = ray.entity();

		if (target == null) {
			return Moves.noTarget(player);
		}

		Tasks.start(age -> {
			if (target.isRemoved() || !target.isAlive()) {
				return false;
			}

			Fx.hurt(level, player, target, 2.5f);
			Vec3 c = target.getBoundingBox().getCenter();
			level.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 4, 0.3, 0.3, 0.3, 0.3);
			level.sendParticles(SPARK, c.x, c.y, c.z, 2, 0.3, 0.3, 0.3, 0);

			if (age % 3 == 0) {
				Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_STRONG, 0.5f, 1.6f);
			}

			return age < 11;
		});
		return true;
	}

	/** Бросок молнии, разогнанной бегом. */
	private static boolean lightningThrow(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.bolt(level, player, Fx.hand(player), player.getViewVector(1f), 2.5, 30, false, SPARK, ParticleTypes.ELECTRIC_SPARK, (at, hit) -> {
			if (hit != null) {
				Fx.hurt(level, player, hit, 15f);
			}

			Moves.jagged(level, SPARK_WHITE, at.add(0, 4, 0), at);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 20, 0.4, 0.4, 0.4, 0.3);
			Fx.sound(level, at, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.0f, 1.4f);
		});
		Fx.sound(level, player, SoundEvents.TRIDENT_RIPTIDE_3.value(), 0.8f, 1.6f);
		return true;
	}

	/** Смерч: бег по кругу поднимает врагов в воздух и швыряет. */
	private static boolean tornado(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 c = player.position();

		Tasks.start(age -> {
			for (int i = 0; i < 4; i++) {
				double a = age * 0.5 + i * Math.PI / 2;
				double y = (age % 20) / 20.0 * 6;
				double r = 2 + y * 0.6;
				level.sendParticles(i % 2 == 0 ? SPARK : ParticleTypes.CLOUD, true, false, c.x + Math.cos(a) * r, c.y + y, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
			}

			for (LivingEntity e : Fx.around(level, player, c.add(0, 1, 0), 8)) {
				Vec3 to = c.subtract(e.position());
				Vec3 swirl = new Vec3(-to.z, 0, to.x).normalize().scale(0.4);
				e.setDeltaMovement(swirl.add(to.normalize().scale(0.15)).add(0, 0.12, 0));
				e.hurtMarked = true;

				if (age % 10 == 0) {
					Fx.hurt(level, player, e, 2f);
				}
			}

			if (age == 79) {
				for (LivingEntity e : Fx.around(level, player, c.add(0, 3, 0), 9)) {
					Fx.push(e, e.position().subtract(c), 2.5, 0.8);
					Fx.hurt(level, player, e, 6f);
				}
			}

			return age < 80;
		});
		Fx.sound(level, player, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.5f, 0.8f);
		return true;
	}

	/** Удар со сверхзвуковой скоростью: волна в конусе. */
	private static boolean machPunch(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.cone(level, player, 12, 30, 12f, 3.5, 0.5, ParticleTypes.CLOUD);
		Vec3 p = player.getEyePosition().add(player.getViewVector(1f).scale(2));
		level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		Fx.sound(level, player, SoundEvents.WARDEN_SONIC_BOOM, 1.0f, 1.8f);
		return true;
	}

	/** Мир замедлился: враги и снаряды вокруг почти стоят 10 секунд, ты — быстрее. */
	private static boolean slowMotion(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.buff(player, 200, MobEffects.SPEED, 3);

		Tasks.start(age -> {
			if (player.isRemoved()) {
				return false;
			}

			ServerLevel lv = player.level();

			for (Entity e : lv.getEntities(player, player.getBoundingBox().inflate(32), e -> e instanceof Projectile || e instanceof Mob)) {
				if (e instanceof Projectile) {
					e.setDeltaMovement(e.getDeltaMovement().scale(0.85));
					e.hurtMarked = true;
				} else if (age % 20 == 0 && e instanceof Mob mob) {
					mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 25, 5));
					mob.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 25, 3));
				}
			}

			if (age % 10 == 0) {
				Fx.circle(lv, SPARK_WHITE, player.position().add(0, 1, 0), new Vec3(0, 1, 0), 2, 16, age * 0.1);
			}

			return age < 200;
		});
		Fx.sound(level, player, SoundEvents.BEACON_DEACTIVATE, 1.0f, 0.5f);
		return true;
	}

	/** Вибрация: пройти сквозь стену (до 12 блоков). */
	private static boolean phase(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 dir = Fx.flatLook(player);
		Vec3 start = player.position();
		boolean inWall = false;

		for (int i = 1; i <= 12; i++) {
			Vec3 p = start.add(dir.scale(i));
			BlockPos feet = BlockPos.containing(p);
			boolean free = level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
					&& level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();

			if (!free) {
				inWall = true;
			} else if (inWall) {
				Vec3 dest = Vec3.atBottomCenterOf(feet);
				Fx.line(level, SPARK, start.add(0, 1, 0), dest.add(0, 1, 0), 0.4);
				player.teleportTo(dest.x, dest.y, dest.z);
				Fx.sound(level, player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0f, 2.0f);
				return true;
			}
		}

		Moves.message(player, "message.solorust.hero.no_wall", ChatFormatting.GRAY);
		return false;
	}

	/** Разоружение: у всех мобов вокруг вылетает оружие из рук. */
	private static boolean disarm(ServerLevel level, ServerPlayer player, ItemStack stack) {
		int n = 0;

		for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16), m -> Fx.isTarget(m, player))) {
			ItemStack held = mob.getItemBySlot(EquipmentSlot.MAINHAND);

			if (!held.isEmpty()) {
				mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
				MagicState.giveOrDrop(player, held.copy());
				Fx.line(level, SPARK, mob.getEyePosition(), player.getEyePosition(), 0.6);
				n++;
			}
		}

		Moves.message(player, "message.solorust.hero.disarm", ChatFormatting.YELLOW, n);
		Fx.sound(level, player, SoundEvents.ITEM_PICKUP, 1.0f, 1.6f);
		return true;
	}

	/** Сбор: за миг обежать округу и собрать всё, что лежит на земле. */
	private static boolean collect(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.message(player, "message.solorust.hero.magnet", ChatFormatting.YELLOW, Moves.magnet(level, player, 40, SPARK));
		return true;
	}

	/** Суперудар: один удар с разбега. */
	private static boolean superPunch(ServerLevel level, ServerPlayer player, ItemStack stack) {
		return Moves.punch(level, player, 8, 22f, 4.5, 0.8, SPARK);
	}

	/** Удар бесконечной массы: разгон на 40 блоков и сокрушительный удар в конце. */
	private static boolean infiniteMass(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 eye = player.getEyePosition();
		Vec3 dir = Fx.flatLook(player);
		BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(dir.scale(40)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 end = (hit.getType() == HitResult.Type.MISS ? eye.add(dir.scale(40)) : hit.getLocation().subtract(dir.scale(1.0))).subtract(0, player.getEyeHeight(), 0);

		for (LivingEntity e : Fx.pierce(level, player, eye, end.add(0, 1, 0), 0.8)) {
			Fx.hurt(level, player, e, 12f);
		}

		Moves.jagged(level, SPARK, player.position().add(0, 1, 0), end.add(0, 1, 0));
		player.teleportTo(end.x, end.y, end.z);
		player.fallDistance = 0;
		Moves.nova(level, player, end.add(0, 1, 0), 6, 26f, 3.0, 0.8, SPARK);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, end.x, end.y + 1, end.z, 1, 0, 0, 0, 0);
		Fx.sound(level, player, SoundEvents.GENERIC_EXPLODE.value(), 1.5f, 0.8f);
		return true;
	}

	/** Бег во времени: назад на 10 секунд. */
	private static boolean timeRun(ServerLevel level, ServerPlayer player, ItemStack stack) {
		List<MagicState.Snapshot> history = MagicState.history(player);

		if (history.size() < 20) {
			Moves.message(player, "message.solorust.magic.no_history", ChatFormatting.GRAY);
			return false;
		}

		MagicState.Snapshot snap = history.get(0);
		ServerLevel dest = level.getServer().getLevel(snap.dimension());

		if (dest == null) {
			return false;
		}

		player.teleportTo(dest, snap.pos().x, snap.pos().y, snap.pos().z, Set.of(), snap.yRot(), snap.xRot(), false);
		player.setHealth(Math.max(player.getHealth(), snap.health()));
		player.fallDistance = 0;
		player.clearFire();
		MagicState.HISTORY.remove(player.getUUID());
		dest.sendParticles(SPARK, player.getX(), player.getY(1), player.getZ(), 60, 0.4, 0.8, 0.4, 0);
		Fx.sound(dest, player, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.8f, 2.0f);
		return true;
	}

	/** Барьер Спидфорса: молнии вокруг отражают снаряды 10 секунд. */
	private static boolean barrier(ServerLevel level, ServerPlayer player, ItemStack stack) {
		MagicState.DEFLECT.put(player.getUUID(), level.getGameTime() + 200);
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 1));

		Tasks.start(age -> {
			if (player.isRemoved()) {
				return false;
			}

			double a = age * 0.7;
			ServerLevel lv = player.level();
			lv.sendParticles(SPARK, player.getX() + Math.cos(a) * 1.3, player.getY(0.5) + Math.sin(age * 0.3) * 0.8, player.getZ() + Math.sin(a) * 1.3, 2, 0, 0, 0, 0);
			lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX() + Math.cos(a + 3) * 1.3, player.getY(0.5), player.getZ() + Math.sin(a + 3) * 1.3, 1, 0, 0, 0, 0);
			return age < 200;
		});
		return true;
	}

	/** Молниеносный обход: удар по каждому врагу вокруг и возврат на место. */
	private static boolean blitz(ServerLevel level, ServerPlayer player, ItemStack stack) {
		List<LivingEntity> targets = Moves.nearby(level, player, 20, true, 12);

		if (targets.isEmpty()) {
			return Moves.noTarget(player);
		}

		Vec3 prev = player.position().add(0, 1, 0);

		for (LivingEntity e : targets) {
			Vec3 at = e.getBoundingBox().getCenter();
			Moves.jagged(level, SPARK, prev, at);
			Fx.hurt(level, player, e, 8f);
			level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 8, 0.3, 0.3, 0.3, 0.3);
			prev = at;
		}

		Moves.jagged(level, SPARK, prev, player.position().add(0, 1, 0));
		Fx.sound(level, player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 1.8f);
		return true;
	}

	/** Ускоренный метаболизм: быстрое заживление ценой еды. */
	private static boolean metabolism(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.heal(level, player, player.getMaxHealth() * 0.6f, SPARK);
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 2));
		player.getFoodData().setFoodLevel(Math.max(0, player.getFoodData().getFoodLevel() - 4));
		return true;
	}

	/** Шторм Спидфорса: 8 секунд вокруг тебя бушует молниеносный вихрь. */
	private static boolean speedStorm(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Set<UUID> struck = new HashSet<>();
		Fx.sound(level, player, SoundEvents.LIGHTNING_BOLT_THUNDER, 2.0f, 1.2f);

		Tasks.start(age -> {
			if (player.isRemoved() || !player.isAlive()) {
				return false;
			}

			ServerLevel lv = player.level();
			Vec3 c = player.position();

			for (int i = 0; i < 6; i++) {
				double a = age * 0.6 + i * Math.PI / 3;
				double r = 3 + (i % 3) * 3;
				lv.sendParticles(i % 2 == 0 ? SPARK : RED, true, false, c.x + Math.cos(a) * r, c.y + 0.5 + (i % 3), c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
			}

			if (age % 5 == 0) {
				List<LivingEntity> targets = new ArrayList<>(Fx.around(lv, player, c.add(0, 1, 0), 10));

				for (LivingEntity e : targets) {
					Fx.hurt(lv, player, e, 5f);
					Vec3 to = c.subtract(e.position());
					e.setDeltaMovement(new Vec3(-to.z, 0, to.x).normalize().scale(0.6).add(0, 0.2, 0));
					e.hurtMarked = true;
					struck.add(e.getUUID());
				}

				if (!targets.isEmpty()) {
					LivingEntity t = targets.get(lv.getRandom().nextInt(targets.size()));
					Moves.jagged(lv, SPARK_WHITE, c.add(0, 1, 0), t.getBoundingBox().getCenter());
				}
			}

			return age < 160;
		});
		return true;
	}

	/** Звуковой удар: волна сверхзвука на 20 блоков вокруг. */
	private static boolean sonicBoom(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.nova(level, player, player.position().add(0, 1, 0), 20, 18f, 3.0, 0.8, ParticleTypes.CLOUD);
		level.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY(1), player.getZ(), 6, 2, 0.5, 2, 0);
		Fx.sound(level, player, SoundEvents.WARDEN_SONIC_BOOM, 2.5f, 0.7f);
		return true;
	}
}
