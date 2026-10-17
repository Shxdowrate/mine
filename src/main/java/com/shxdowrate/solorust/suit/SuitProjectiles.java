package com.shxdowrate.solorust.suit;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Ракеты костюма. Это не сущности, а лёгкие «виртуальные» снаряды: сервер двигает их сам и рисует частицами,
 * поэтому не нужны ни модели, ни рендереры. Блоки взрывы не ломают — только урон и отброс.
 */
public final class SuitProjectiles {
	public enum Kind {
		/** Наручная ракета Mk I: летит прямо, слегка падает. */
		ROCKET(1.3, 0.0, 0.012, 100),
		/** Микроракета: быстрая, самонаводящаяся. */
		MISSILE(1.5, 0.22, 0.0, 120),
		/** Тяжёлая ракета «Титана»: медленнее, мощнее, наводится. */
		HEAVY(1.1, 0.12, 0.0, 140);

		final double speed;
		final double turn;
		final double gravity;
		final int life;

		Kind(double speed, double turn, double gravity, int life) {
			this.speed = speed;
			this.turn = turn;
			this.gravity = gravity;
			this.life = life;
		}
	}

	private static final class Missile {
		final ServerLevel level;
		final ServerPlayer owner;
		final Kind kind;
		final float power;
		final LivingEntity target;
		/** Точка вылета относительно игрока (для отложенного старта). */
		final Vec3 rel;
		Vec3 pos;
		Vec3 vel;
		int age;
		int delay;

		Missile(ServerLevel level, ServerPlayer owner, Kind kind, float power, LivingEntity target, Vec3 pos, Vec3 vel, int delay) {
			this.level = level;
			this.owner = owner;
			this.kind = kind;
			this.power = power;
			this.target = target;
			this.pos = pos;
			this.rel = pos.subtract(owner.position());
			this.vel = vel;
			this.delay = delay;
		}
	}

	private static final List<Missile> ACTIVE = new ArrayList<>();

	private SuitProjectiles() {
	}

	public static void launch(ServerLevel level, ServerPlayer owner, Vec3 pos, Vec3 vel, LivingEntity target, Kind kind, float power) {
		launch(level, owner, pos, vel, target, kind, power, 0);
	}

	/** delay — через сколько тиков ракета вылетит (для красивого залпа «очередью»). */
	public static void launch(ServerLevel level, ServerPlayer owner, Vec3 pos, Vec3 vel, LivingEntity target, Kind kind, float power, int delay) {
		ACTIVE.add(new Missile(level, owner, kind, power, target, pos, vel, delay));
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> tick());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVE.clear());
	}

	private static void tick() {
		if (ACTIVE.isEmpty()) {
			return;
		}

		List<Missile> list = new ArrayList<>(ACTIVE);
		ACTIVE.clear();

		for (Missile m : list) {
			if (m.delay > 0) {
				m.delay--;

				if (m.delay == 0) {
					// Вылетаем из той же точки костюма, где бы игрок сейчас ни был — залп не отстаёт в полёте.
					m.pos = m.owner.position().add(m.rel);
					m.level.playSound(null, m.pos.x, m.pos.y, m.pos.z, net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_LAUNCH,
							net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.5f);
				}

				ACTIVE.add(m);
				continue;
			}

			if (step(m)) {
				ACTIVE.add(m);
			}
		}
	}

	/** true — ракета летит дальше. */
	private static boolean step(Missile m) {
		ServerLevel level = m.level;

		if (m.owner.isRemoved() || m.owner.level() != level || !level.isLoaded(BlockPos.containing(m.pos))) {
			return false;
		}

		m.age++;

		// Наведение: плавно доворачиваем вектор скорости на цель. Первые тики ракета «вылетает из пусковой».
		if (m.target != null && m.target.isAlive() && m.target.level() == level && m.age > 3) {
			Vec3 desired = m.target.getBoundingBox().getCenter().subtract(m.pos).normalize().scale(m.kind.speed);
			m.vel = m.vel.add(desired.subtract(m.vel).scale(m.kind.turn)).normalize().scale(m.kind.speed);
		} else if (m.kind.gravity > 0) {
			m.vel = m.vel.add(0, -m.kind.gravity, 0);
		} else if (m.age > 3) {
			m.vel = m.vel.normalize().scale(Math.min(m.kind.speed, m.vel.length() + 0.15));
		}

		Vec3 next = m.pos.add(m.vel);
		BlockHitResult block = level.clip(new ClipContext(m.pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, m.owner));
		Vec3 limit = block.getType() == HitResult.Type.MISS ? next : block.getLocation();

		// Попадание в сущность.
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(m.pos, limit).inflate(0.6),
				e -> SuitWeapons.isTarget(e, m.owner))) {
			var hit = e.getBoundingBox().inflate(0.35).clip(m.pos, limit);

			if (hit.isPresent() || e.getBoundingBox().inflate(0.35).contains(m.pos)) {
				explode(m, hit.orElse(m.pos));
				return false;
			}
		}

		trail(m, m.pos, limit);

		if (block.getType() == HitResult.Type.BLOCK) {
			explode(m, limit);
			return false;
		}

		m.pos = next;

		if (m.age >= m.kind.life) {
			explode(m, m.pos);
			return false;
		}

		return true;
	}

	private static void trail(Missile m, Vec3 from, Vec3 to) {
		Vec3 mid = from.add(to).scale(0.5);
		m.level.sendParticles(ParticleTypes.FLAME, true, false, from.x, from.y, from.z, 1, 0.02, 0.02, 0.02, 0.0);
		m.level.sendParticles(ParticleTypes.SMOKE, true, false, mid.x, mid.y, mid.z, m.kind == Kind.HEAVY ? 3 : 1, 0.05, 0.05, 0.05, 0.0);

		if (m.kind == Kind.HEAVY) {
			m.level.sendParticles(ParticleTypes.LARGE_SMOKE, true, false, from.x, from.y, from.z, 1, 0.05, 0.05, 0.05, 0.0);
		}
	}

	private static void explode(Missile m, Vec3 at) {
		m.level.explode(m.owner, at.x, at.y, at.z, m.power, Level.ExplosionInteraction.NONE);
	}
}
