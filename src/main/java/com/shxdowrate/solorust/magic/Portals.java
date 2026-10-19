package com.shxdowrate.solorust.magic;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Порталы кольца-портала: два искрящихся кольца, через которые можно ходить в обе стороны (и из других измерений).
 * У игрока одновременно открыта одна пара; новая закрывает старую.
 */
final class Portals {
	private static final DustParticleOptions SPARK = Fx.dust(0xFF9A1F, 1.1f);
	private static final DustParticleOptions SPARK_HOT = Fx.dust(0xFFD54A, 0.8f);
	static final double RADIUS = 1.25;

	/** Конец портала: центр кольца, плоскость (нормаль) и куда выходишь, если вышел из этого конца. */
	record End(ServerLevel level, Vec3 center, Vec3 normal, Vec3 exitDir) {
	}

	private static final class Pair {
		final End a;
		final End b;
		final long opened;
		final long expires;

		Pair(End a, End b, long opened, long expires) {
			this.a = a;
			this.b = b;
			this.opened = opened;
			this.expires = expires;
		}
	}

	private static final Map<UUID, Pair> PORTALS = new HashMap<>();
	private static final Map<UUID, Long> COOLDOWN = new HashMap<>();

	private Portals() {
	}

	static void open(ServerPlayer owner, End a, End b, int ticks) {
		PORTALS.put(owner.getUUID(), new Pair(a, b, owner.level().getGameTime(), owner.level().getGameTime() + ticks));
		COOLDOWN.remove(owner.getUUID());
		Fx.sound(a.level(), a.center(), SoundEvents.FIRECHARGE_USE, 1.0f, 0.5f);
		Fx.sound(b.level(), b.center(), SoundEvents.FIRECHARGE_USE, 1.0f, 0.5f);
		Fx.sound(a.level(), a.center(), SoundEvents.BEACON_POWER_SELECT, 0.6f, 1.8f);
	}

	static void tick(long now) {
		if (PORTALS.isEmpty()) {
			return;
		}

		for (Iterator<Pair> it = PORTALS.values().iterator(); it.hasNext(); ) {
			Pair p = it.next();
			long left = p.expires - now;

			if (left < 0) {
				it.remove();
				continue;
			}

			// Открытие: кольцо «разгорается» первые 10 тиков, закрытие — сжимается последние 20.
			double scale = Math.max(0.05, Math.min(1.0, Math.min((now - p.opened) / 10.0, left / 20.0)));
			ring(p.a, now, scale);
			ring(p.b, now, scale);

			if (scale > 0.6) {
				transport(p.a, p.b, now);
				transport(p.b, p.a, now);
			}
		}

		COOLDOWN.values().removeIf(t -> t < now);
	}

	private static void ring(End end, long time, double scale) {
		ServerLevel level = end.level();
		double r = RADIUS * scale;
		Vec3[] basis = Fx.basis(end.normal());

		for (int i = 0; i < 30; i++) {
			double a = i * Math.PI * 2 / 30 + time * 0.25;
			double jitter = 1.0 + (level.getRandom().nextDouble() - 0.5) * 0.08;
			Vec3 p = end.center().add(basis[0].scale(Math.cos(a) * r * jitter)).add(basis[1].scale(Math.sin(a) * r * 1.25 * jitter));
			level.sendParticles(i % 3 == 0 ? SPARK_HOT : SPARK, true, false, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}

		// Искры отлетают по касательной — как у кольца в фильме.
		if (time % 2 == 0) {
			double a = level.getRandom().nextDouble() * Math.PI * 2;
			Vec3 rim = end.center().add(basis[0].scale(Math.cos(a) * r)).add(basis[1].scale(Math.sin(a) * r * 1.25));
			Vec3 tangent = basis[0].scale(-Math.sin(a)).add(basis[1].scale(Math.cos(a)));
			level.sendParticles(ParticleTypes.FLAME, true, false, rim.x, rim.y, rim.z, 0, tangent.x, tangent.y - 0.3, tangent.z, 0.15);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, true, false, rim.x, rim.y, rim.z, 0, tangent.x, tangent.y, tangent.z, 0.3);
		}
	}

	private static void transport(End from, End to, long now) {
		Vec3 c = from.center();
		AABB box = new AABB(c, c).inflate(RADIUS + 0.5);

		for (Entity e : from.level().getEntities((Entity) null, box, e -> !e.isSpectator() && e.isAlive() && !e.isPassenger())) {
			if (COOLDOWN.containsKey(e.getUUID()) || e instanceof Projectile && e.tickCount < 2) {
				continue;
			}

			Vec3 mid = e.getBoundingBox().getCenter();
			Vec3 off = mid.subtract(c);
			double along = off.dot(from.normal());
			Vec3[] basis = Fx.basis(from.normal());
			double u = off.dot(basis[0]) / RADIUS;
			double v = off.dot(basis[1]) / (RADIUS * 1.25);

			if (Math.abs(along) > 0.55 || u * u + v * v > 1.0) {
				continue;
			}

			// Выходим из второго кольца, сохраняя скорость и направление взгляда.
			Vec3 velocity = e.getDeltaMovement();
			Vec3 dest = to.center().add(to.exitDir().scale(0.9 + e.getBbWidth() * 0.5)).subtract(0, e.getBbHeight() * 0.5, 0);
			COOLDOWN.put(e.getUUID(), now + 20);

			if (to.level() == from.level()) {
				e.teleportTo(dest.x, dest.y, dest.z);
			} else {
				e.teleportTo(to.level(), dest.x, dest.y, dest.z, Set.of(), e.getYRot(), e.getXRot(), false);
			}

			e.setDeltaMovement(velocity);
			e.hurtMarked = true;
			e.fallDistance = 0;
			Fx.sound(to.level(), dest, SoundEvents.ENDERMAN_TELEPORT, 0.6f, 1.4f);
		}
	}

	static void close(UUID owner) {
		PORTALS.remove(owner);
	}

	static void clear() {
		PORTALS.clear();
		COOLDOWN.clear();
	}
}
