package com.shxdowrate.solorust.magic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Готовые «кирпичики» способностей героев: лучи, снаряды, рывки, волны, баффы, союзники. */
final class Moves {
	private Moves() {
	}

	static boolean noTarget(ServerPlayer player) {
		player.sendOverlayMessage(Component.translatable("message.solorust.magic.no_target").withStyle(ChatFormatting.GRAY));
		return false;
	}

	static void message(ServerPlayer player, String key, ChatFormatting color, Object... args) {
		player.sendOverlayMessage(Component.translatable(key, args).withStyle(color));
	}

	// ------------------------------------------------------------------ лучи

	/** Луч до первой цели (или насквозь). Возвращает поражённые цели. */
	static List<LivingEntity> beam(ServerLevel level, ServerPlayer player, double range, float damage, boolean pierce,
			ParticleOptions main, ParticleOptions accent, Vec3 from) {
		List<LivingEntity> hit = new ArrayList<>();
		Fx.Ray ray = Fx.ray(level, player, range, 0.3);
		Vec3 end;

		if (pierce) {
			end = ray.block().getType() == HitResult.Type.MISS ? player.getEyePosition().add(player.getViewVector(1f).scale(range)) : ray.block().getLocation();
			hit.addAll(Fx.pierce(level, player, player.getEyePosition(), end, 0.4));
		} else {
			end = ray.end();

			if (ray.entity() != null) {
				hit.add(ray.entity());
			}
		}

		for (LivingEntity e : hit) {
			Fx.hurt(level, player, e, damage);
		}

		Fx.line(level, main, from, end, 0.3);

		if (accent != null) {
			Fx.line(level, accent, from, end, 1.2);
		}

		level.sendParticles(main, end.x, end.y, end.z, 12, 0.3, 0.3, 0.3, 0);
		return hit;
	}

	// ------------------------------------------------------------------ снаряды

	/**
	 * Летящий заряд (частицы), при желании самонаводящийся.
	 *
	 * @param onHit что сделать при попадании: точка и сущность (может быть null)
	 */
	static void bolt(ServerLevel level, ServerPlayer player, Vec3 from, Vec3 dir, double speed, int life, boolean homing,
			ParticleOptions main, ParticleOptions trail, BiConsumer<Vec3, LivingEntity> onHit) {
		Vec3[] state = {from, dir.normalize().scale(speed)};

		Tasks.start(age -> {
			Vec3 pos = state[0];
			Vec3 heading = state[1];
			Vec3 vel = heading;

			if (homing && age > 1) {
				LivingEntity target = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(20),
								e -> Fx.isTarget(e, player) && Fx.angle(heading, e.getBoundingBox().getCenter().subtract(pos)) < 60)
						.stream()
						.min(Comparator.<LivingEntity>comparingInt(e -> Fx.hostile(e) ? 0 : 1).thenComparingDouble(e -> e.distanceToSqr(pos)))
						.orElse(null);

				if (target != null) {
					Vec3 to = target.getBoundingBox().getCenter().subtract(pos).normalize();
					vel = heading.normalize().scale(0.7).add(to.scale(0.3)).normalize().scale(speed);
					state[1] = vel;
				}
			}

			Vec3 next = pos.add(vel);
			BlockHitResult hit = level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
			LivingEntity struck = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, next).inflate(0.5),
							e -> Fx.isTarget(e, player) && e.getBoundingBox().inflate(0.4).clip(pos, next).isPresent())
					.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(pos))).orElse(null);

			level.sendParticles(main, true, false, pos.x, pos.y, pos.z, 3, 0.08, 0.08, 0.08, 0);

			if (trail != null) {
				level.sendParticles(trail, true, false, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.02);
			}

			if (struck != null || hit.getType() != HitResult.Type.MISS || age > life) {
				Vec3 at = struck != null ? struck.getBoundingBox().getCenter() : (hit.getType() != HitResult.Type.MISS ? hit.getLocation() : next);
				onHit.accept(at, struck);
				return false;
			}

			state[0] = next;
			return true;
		});
	}

	// ------------------------------------------------------------------ ближний бой и контроль

	/** Плеть: волнистая линия до цели, урон и подтягивание. */
	static boolean whip(ServerLevel level, ServerPlayer player, double range, float damage, double pull,
			ParticleOptions main, ParticleOptions accent, SoundEvent sound) {
		Fx.Ray ray = Fx.ray(level, player, range, 0.6);
		LivingEntity target = ray.entity();
		Vec3 hand = Fx.hand(player);
		Vec3 end = target != null ? target.getBoundingBox().getCenter() : ray.end();
		Vec3 delta = end.subtract(hand);
		Vec3[] basis = Fx.basis(delta);
		int steps = Math.max(4, (int) (delta.length() * 4));

		for (int i = 0; i <= steps; i++) {
			double t = i / (double) steps;
			double wave = Math.sin(t * Math.PI * 4) * 0.35 * (1 - t);
			Vec3 p = hand.add(delta.scale(t)).add(basis[0].scale(wave));
			level.sendParticles(i % 3 == 0 && accent != null ? accent : main, true, false, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}

		Fx.sound(level, player, sound, 1.0f, 0.8f);

		if (target == null) {
			return true;
		}

		Fx.hurt(level, player, target, damage);

		if (pull > 0) {
			Vec3 to = player.position().subtract(target.position());
			Fx.push(target, to, Math.min(pull, 0.5 + to.length() * 0.12), 0.45);
		}

		return true;
	}

	/** Удар в упор по цели перед собой. */
	static boolean punch(ServerLevel level, ServerPlayer player, double range, float damage, double knock, double lift, ParticleOptions particle) {
		Fx.Ray ray = Fx.ray(level, player, range, 0.5);

		if (ray.entity() == null) {
			return noTarget(player);
		}

		LivingEntity e = ray.entity();
		Fx.hurt(level, player, e, damage);
		Fx.push(e, player.getViewVector(1f), knock, lift);
		Vec3 c = e.getBoundingBox().getCenter();
		level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 1, 0, 0, 0, 0);
		level.sendParticles(particle, c.x, c.y, c.z, 20, 0.4, 0.4, 0.4, 0);
		Fx.sound(level, c, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.6f);
		Fx.sound(level, c, SoundEvents.GENERIC_EXPLODE.value(), 0.6f, 1.4f);
		return true;
	}

	/** Волна в конусе перед собой. */
	static int cone(ServerLevel level, ServerPlayer player, double range, double angle, float damage, double push, double lift,
			ParticleOptions particle) {
		List<LivingEntity> targets = Fx.cone(level, player, range, angle);

		for (LivingEntity e : targets) {
			if (damage > 0) {
				Fx.hurt(level, player, e, damage);
			}

			if (push > 0) {
				Fx.push(e, player.getViewVector(1f), push, lift);
			}
		}

		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1f);
		RandomSource random = player.getRandom();

		for (int i = 0; i < 40; i++) {
			Vec3 v = look.add((random.nextDouble() - 0.5) * angle / 60, (random.nextDouble() - 0.5) * angle / 60, (random.nextDouble() - 0.5) * angle / 60).normalize();
			level.sendParticles(particle, true, false, eye.x + v.x, eye.y + v.y - 0.2, eye.z + v.z, 0, v.x, v.y, v.z, range / 12.0);
		}

		return targets.size();
	}

	/** Волна во все стороны вокруг точки. */
	static int nova(ServerLevel level, ServerPlayer player, Vec3 center, double radius, float damage, double push, double lift,
			ParticleOptions particle) {
		List<LivingEntity> targets = Fx.around(level, player, center, radius);

		for (LivingEntity e : targets) {
			double d = e.position().distanceTo(center);
			double k = Math.max(0.3, 1.0 - d / (radius + 1));

			if (damage > 0) {
				Fx.hurt(level, player, e, (float) (damage * k));
			}

			if (push > 0) {
				Vec3 out = e.position().subtract(center);
				Fx.push(e, out.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : out, push * k, lift);
			}
		}

		Tasks.start(age -> {
			double r = 0.5 + age * radius / 8.0;
			Fx.circle(level, particle, center.add(0, 0.2, 0), new Vec3(0, 1, 0), r, (int) Math.max(16, r * 10), age * 0.2);
			return age < 8;
		});
		return targets.size();
	}

	/** Рывок вперёд: бьёт всех на пути (по одному разу). */
	static void dash(ServerLevel level, ServerPlayer player, double speed, double lift, float damage, int ticks, ParticleOptions trail) {
		Vec3 look = player.getViewVector(1f);
		player.setDeltaMovement(look.scale(speed).add(0, lift, 0));
		player.hurtMarked = true;
		player.fallDistance = 0;
		MagicState.FALL_SAFE.put(player.getUUID(), level.getGameTime() + ticks + 60);
		Set<UUID> hit = new HashSet<>();

		Tasks.start(age -> {
			ServerLevel lv = player.level();
			lv.sendParticles(trail, player.getX(), player.getY(0.5), player.getZ(), 5, 0.3, 0.4, 0.3, 0);

			if (damage > 0) {
				for (LivingEntity e : Fx.around(lv, player, player.getBoundingBox().getCenter(), 2.2)) {
					if (hit.add(e.getUUID())) {
						Fx.hurt(lv, player, e, damage);
						Fx.push(e, look, 1.5, 0.5);
					}
				}
			}

			return age < ticks;
		});
	}

	/** Прыжок вверх и удар при приземлении. */
	static void leapSlam(ServerLevel level, ServerPlayer player, double up, double forward, double radius, float damage,
			ParticleOptions particle, Runnable onLand) {
		Vec3 look = Fx.flatLook(player);
		player.setDeltaMovement(look.scale(forward).add(0, up, 0));
		player.hurtMarked = true;
		MagicState.FALL_SAFE.put(player.getUUID(), level.getGameTime() + 200);

		Tasks.start(age -> {
			if (player.isRemoved() || age > 160) {
				return false;
			}

			ServerLevel lv = player.level();
			lv.sendParticles(particle, player.getX(), player.getY(0.3), player.getZ(), 2, 0.2, 0.2, 0.2, 0);

			// Ускоряем падение, чтобы удар был тяжёлым.
			if (age > 6 && player.getDeltaMovement().y < 0) {
				player.setDeltaMovement(player.getDeltaMovement().add(0, -0.35, 0));
				player.hurtMarked = true;
			}

			if (age > 4 && player.onGround()) {
				Vec3 c = player.position();
				nova(lv, player, c, radius, damage, 2.0, 0.7, particle);
				lv.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 0.5, c.z, 1, 0, 0, 0, 0);
				Fx.sound(lv, player, SoundEvents.MACE_SMASH_GROUND_HEAVY, 2.0f, 0.6f);
				Fx.sound(lv, player, SoundEvents.GENERIC_EXPLODE.value(), 1.2f, 0.8f);

				if (onLand != null) {
					onLand.run();
				}

				return false;
			}

			return true;
		});
	}

	/** Цепная волна: от цели к соседним (до count целей). */
	static int chain(ServerLevel level, ServerPlayer player, double range, int count, double hop, float damage,
			ParticleOptions main, ParticleOptions accent) {
		Fx.Ray ray = Fx.ray(level, player, range, 0.4);

		if (ray.entity() == null) {
			return noTarget(player) ? 1 : 0;
		}

		List<LivingEntity> chain = new ArrayList<>();
		LivingEntity current = ray.entity();
		Vec3 from = Fx.hand(player);

		while (current != null && chain.size() < count) {
			chain.add(current);
			Vec3 to = current.getBoundingBox().getCenter();
			jagged(level, main, from, to);

			if (accent != null) {
				level.sendParticles(accent, to.x, to.y, to.z, 8, 0.3, 0.3, 0.3, 0.1);
			}

			Fx.hurt(level, player, current, damage);
			from = to;
			LivingEntity last = current;
			current = level.getEntitiesOfClass(LivingEntity.class, last.getBoundingBox().inflate(hop),
							e -> Fx.isTarget(e, player) && !chain.contains(e))
					.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(last))).orElse(null);
		}

		return chain.size();
	}

	/** Ломаная линия (молния). */
	static void jagged(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to) {
		RandomSource random = level.getRandom();
		Vec3 prev = from;
		int segments = Math.max(3, (int) (from.distanceTo(to) / 2));

		for (int i = 1; i <= segments; i++) {
			Vec3 p = from.add(to.subtract(from).scale(i / (double) segments));

			if (i < segments) {
				p = p.add((random.nextDouble() - 0.5) * 0.9, (random.nextDouble() - 0.5) * 0.9, (random.nextDouble() - 0.5) * 0.9);
			}

			Fx.line(level, particle, prev, p, 0.25);
			prev = p;
		}
	}

	/** Молния без пожаров: видимая вспышка + урон вокруг точки. */
	static void lightning(ServerLevel level, ServerPlayer player, Vec3 at, float damage, double radius) {
		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);

		if (bolt != null) {
			bolt.snapTo(at.x, at.y, at.z);
			bolt.setVisualOnly(true);
			level.addFreshEntity(bolt);
		}

		for (LivingEntity e : Fx.around(level, player, at.add(0, 1, 0), radius)) {
			Fx.hurt(level, player, e, damage);
			e.igniteForSeconds(3);
		}

		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 0.5, at.z, 30, 0.6, 0.6, 0.6, 0.4);
	}

	/** Точка на земле под тем местом, куда смотрит игрок. */
	static Vec3 groundAt(ServerLevel level, ServerPlayer player, double range) {
		Fx.Ray ray = Fx.ray(level, player, range, 0.3);
		return ray.entity() != null ? ray.entity().position() : ray.end();
	}

	// ------------------------------------------------------------------ баффы и союзники

	static void buff(LivingEntity e, int ticks, Object... effectsAndLevels) {
		for (int i = 0; i + 1 < effectsAndLevels.length; i += 2) {
			@SuppressWarnings("unchecked")
			Holder<MobEffect> effect = (Holder<MobEffect>) effectsAndLevels[i];
			e.addEffect(new MobEffectInstance(effect, ticks, (Integer) effectsAndLevels[i + 1]));
		}
	}

	static void aura(ServerPlayer player, int ticks, ParticleOptions particle, double radius, float damagePerSecond) {
		Tasks.start(age -> {
			if (player.isRemoved() || !player.isAlive()) {
				return false;
			}

			ServerLevel lv = player.level();

			if (age % 3 == 0) {
				double a = age * 0.25;

				for (int i = 0; i < 3; i++) {
					double b = a + i * Math.PI * 2 / 3;
					lv.sendParticles(particle, player.getX() + Math.cos(b) * 0.8, player.getY() + 0.2 + (age % 30) / 30.0 * 1.8,
							player.getZ() + Math.sin(b) * 0.8, 1, 0, 0, 0, 0);
				}
			}

			if (damagePerSecond > 0 && age % 20 == 0) {
				for (LivingEntity e : Fx.around(lv, player, player.getBoundingBox().getCenter(), radius)) {
					if (Fx.hostile(e) || e instanceof Player) {
						Fx.hurt(lv, player, e, damagePerSecond);
					}
				}
			}

			return age < ticks;
		});
	}

	static void heal(ServerLevel level, ServerPlayer player, float amount, ParticleOptions particle) {
		player.heal(amount);
		player.clearFire();
		Fx.clearHarmful(player);
		level.sendParticles(particle, player.getX(), player.getY(1), player.getZ(), 25, 0.4, 0.7, 0.4, 0);
		level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY(1.1), player.getZ(), 5, 0.4, 0.3, 0.4, 0);
	}

	/** Союзники на время: подчиняются через систему контроля разума и исчезают по истечении. */
	static <T extends Mob> int summon(ServerLevel level, ServerPlayer player, EntityType<T> type, int count, int ticks,
			Component name, ParticleOptions particle) {
		List<Mob> spawned = new ArrayList<>();
		Vec3 look = Fx.flatLook(player);

		for (int i = 0; i < count; i++) {
			double a = (i - (count - 1) / 2.0) * 0.9;
			Vec3 at = player.position().add(look.yRot((float) a).scale(2.5));
			BlockPos pos = BlockPos.containing(at.x, at.y + 0.5, at.z);
			T mob = type.spawn(level, pos, EntitySpawnReason.MOB_SUMMONED);

			if (mob == null) {
				continue;
			}

			if (name != null) {
				mob.setCustomName(name);
			}

			mob.setPersistenceRequired();
			MagicState.charm(mob, player, level.getGameTime() + ticks, true);
			mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false, true));
			level.sendParticles(particle, mob.getX(), mob.getY(0.5), mob.getZ(), 30, 0.4, 0.6, 0.4, 0);
			spawned.add(mob);
		}

		Tasks.start(age -> {
			if (age < ticks) {
				return true;
			}

			for (Mob mob : spawned) {
				if (!mob.isRemoved()) {
					level.sendParticles(particle, mob.getX(), mob.getY(0.5), mob.getZ(), 20, 0.4, 0.6, 0.4, 0);
					mob.discard();
				}
			}

			return false;
		});
		return spawned.size();
	}

	/** Подсветить всех вокруг. */
	static int reveal(ServerLevel level, ServerPlayer player, double radius, int ticks) {
		int n = 0;

		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius), e -> e != player && e.isAlive())) {
			e.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false, true));
			n++;
		}

		player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, ticks + 200, 0, false, false, true));
		return n;
	}

	/** Притянуть предметы и опыт. */
	static int magnet(ServerLevel level, ServerPlayer player, double radius, ParticleOptions particle) {
		int n = 0;

		for (Entity e : level.getEntities(player, player.getBoundingBox().inflate(radius), e -> e instanceof ItemEntity || e instanceof ExperienceOrb)) {
			level.sendParticles(particle, e.getX(), e.getY() + 0.2, e.getZ(), 3, 0.1, 0.1, 0.1, 0);
			e.teleportTo(player.getX(), player.getY() + 0.3, player.getZ());
			e.setDeltaMovement(Vec3.ZERO);

			if (e instanceof ItemEntity item) {
				item.setNoPickUpDelay();
			}

			n++;
		}

		Fx.sound(level, player, SoundEvents.ITEM_PICKUP, 1.0f, 0.6f);
		return n;
	}

	/** Схватить цель и держать перед собой, потом бросить. */
	static boolean grabAndThrow(ServerLevel level, ServerPlayer player, double range, int holdTicks, double throwSpeed, float damage,
			ParticleOptions particle) {
		Fx.Ray ray = Fx.ray(level, player, range, 0.5);
		LivingEntity target = ray.entity();

		if (target == null || Fx.isBoss(target)) {
			return noTarget(player);
		}

		Tasks.start(age -> {
			if (target.isRemoved() || !target.isAlive() || player.isRemoved()) {
				return false;
			}

			Vec3 look = player.getViewVector(1f);

			if (age < holdTicks) {
				Vec3 spot = player.getEyePosition().add(look.scale(2.5 + target.getBbWidth()));
				Vec3 pull = spot.subtract(target.position().add(0, target.getBbHeight() / 2, 0)).scale(0.4);
				target.setDeltaMovement(pull);
				target.hurtMarked = true;
				target.fallDistance = 0;

				if (age % 2 == 0) {
					level.sendParticles(particle, target.getX(), target.getY(0.5), target.getZ(), 3, 0.3, 0.4, 0.3, 0);
				}

				return true;
			}

			target.setDeltaMovement(look.scale(throwSpeed).add(0, 0.4, 0));
			target.hurtMarked = true;
			Fx.hurt(level, player, target, damage);
			target.setDeltaMovement(look.scale(throwSpeed).add(0, 0.4, 0));
			Fx.sound(level, player, SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 0.7f);
			return false;
		});
		return true;
	}

	/** Найти безопасное место на поверхности по x/z. */
	static Vec3 surface(ServerLevel level, double x, double z) {
		BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(x, 0, z));
		return Vec3.atBottomCenterOf(top);
	}

	/** Цели вокруг, отсортированные по расстоянию. */
	static List<LivingEntity> nearby(ServerLevel level, ServerPlayer player, double radius, boolean hostileOnly, int max) {
		List<LivingEntity> list = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
				e -> Fx.isTarget(e, player) && (!hostileOnly || Fx.hostile(e) || e instanceof Player)));
		list.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
		return list.size() > max ? new ArrayList<>(list.subList(0, max)) : list;
	}

	/** Отключить снаряды вокруг. */
	static int destroyProjectiles(ServerLevel level, ServerPlayer player, double radius) {
		int n = 0;

		for (Entity e : level.getEntities(player, player.getBoundingBox().inflate(radius), e -> e instanceof Projectile p && p.getOwner() != player)) {
			level.sendParticles(ParticleTypes.ENCHANTED_HIT, e.getX(), e.getY(), e.getZ(), 4, 0.1, 0.1, 0.1, 0.1);
			e.discard();
			n++;
		}

		return n;
	}
}
