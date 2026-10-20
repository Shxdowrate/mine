package com.shxdowrate.solorust.war;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * Ракеты гранатомётчиков. Как и ракеты костюма — «виртуальные»: сервер сам двигает их и рисует частицами.
 * Стреляют с упреждением и чуть подруливают на цель, поэтому против летающего костюма работают лучше пуль.
 * Тепловые ловушки (FLARES) уводят их в сторону.
 */
public final class BanditRockets {
	private static final double SPEED = 1.15;
	private static final double TURN = 0.06;
	private static final int LIFE = 90;
	private static final float POWER = 1.3f;

	private static final class Rocket {
		final ServerLevel level;
		final Bandit owner;
		LivingEntity target;
		Vec3 pos;
		Vec3 vel;
		int age;
		boolean diverted;

		Rocket(ServerLevel level, Bandit owner, LivingEntity target, Vec3 pos, Vec3 vel) {
			this.level = level;
			this.owner = owner;
			this.target = target;
			this.pos = pos;
			this.vel = vel;
		}
	}

	private static final List<Rocket> ACTIVE = new ArrayList<>();

	private BanditRockets() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> tick());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVE.clear());
	}

	/** Выстрел с упреждением: летим туда, где цель окажется к моменту подлёта. */
	static void fire(ServerLevel level, Bandit owner, LivingEntity target, Vec3 targetVelocity) {
		Vec3 from = owner.getEyePosition().add(owner.getViewVector(1f).scale(0.8));
		Vec3 aim = target.getBoundingBox().getCenter();

		// Две итерации уточнения времени подлёта.
		for (int i = 0; i < 2; i++) {
			double time = from.distanceTo(aim) / SPEED;
			aim = target.getBoundingBox().getCenter().add(targetVelocity.scale(time * owner.role().lead));
		}

		Vec3 vel = aim.subtract(from).normalize().scale(SPEED);
		ACTIVE.add(new Rocket(level, owner, target, from, vel));
		level.playSound(null, from.x, from.y, from.z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 2.0f, 0.6f);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, from.x, from.y, from.z, 4, 0.1, 0.1, 0.1, 0.02);
	}

	/** Тепловые ловушки игрока: ракеты в радиусе 32 блоков теряют цель и уходят в сторону. */
	public static void divert(ServerLevel level, ServerPlayer player) {
		for (Rocket r : ACTIVE) {
			if (r.level == level && r.pos.distanceToSqr(player.position()) < 32 * 32) {
				r.target = null;
				r.diverted = true;
				Vec3 side = new Vec3(r.vel.z, 0.6, -r.vel.x).normalize().scale(SPEED);
				r.vel = r.vel.add(side).normalize().scale(SPEED);
			}
		}
	}

	private static void tick() {
		if (ACTIVE.isEmpty()) {
			return;
		}

		List<Rocket> list = new ArrayList<>(ACTIVE);
		ACTIVE.clear();

		for (Rocket r : list) {
			if (step(r)) {
				ACTIVE.add(r);
			}
		}
	}

	private static boolean step(Rocket r) {
		ServerLevel level = r.level;

		if (!level.isLoaded(BlockPos.containing(r.pos))) {
			return false;
		}

		r.age++;

		if (r.target != null && r.target.isAlive() && r.target.level() == level && r.age > 4) {
			Vec3 desired = r.target.getBoundingBox().getCenter().subtract(r.pos).normalize().scale(SPEED);
			r.vel = r.vel.add(desired.subtract(r.vel).scale(TURN)).normalize().scale(SPEED);
		} else if (r.diverted) {
			r.vel = r.vel.add(0, 0.02, 0).normalize().scale(SPEED);
		}

		Vec3 next = r.pos.add(r.vel);
		BlockHitResult block = level.clip(new ClipContext(r.pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, r.owner));
		Vec3 limit = block.getType() == HitResult.Type.MISS ? next : block.getLocation();

		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(r.pos, limit).inflate(0.6),
				e -> e.isAlive() && !(e instanceof Bandit) && !e.isSpectator())) {
			var hit = e.getBoundingBox().inflate(0.35).clip(r.pos, limit);

			if (hit.isPresent() || e.getBoundingBox().inflate(0.35).contains(r.pos)) {
				explode(r, hit.orElse(r.pos));
				return false;
			}
		}

		Vec3 mid = r.pos.add(limit).scale(0.5);
		level.sendParticles(ParticleTypes.FLAME, true, false, r.pos.x, r.pos.y, r.pos.z, 1, 0.02, 0.02, 0.02, 0.0);
		level.sendParticles(ParticleTypes.SMOKE, true, false, mid.x, mid.y, mid.z, 2, 0.05, 0.05, 0.05, 0.0);

		if (block.getType() == HitResult.Type.BLOCK) {
			explode(r, limit);
			return false;
		}

		r.pos = next;

		if (r.age >= LIFE) {
			explode(r, r.pos);
			return false;
		}

		return true;
	}

	private static void explode(Rocket r, Vec3 at) {
		r.level.explode(r.owner.isAlive() ? r.owner : null, at.x, at.y, at.z, POWER, Level.ExplosionInteraction.NONE);
	}
}
