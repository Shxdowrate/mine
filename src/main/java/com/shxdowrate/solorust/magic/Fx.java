package com.shxdowrate.solorust.magic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Общие помощники для способностей: лучи, цели, частицы, звук. */
final class Fx {
	private Fx() {
	}

	static DustParticleOptions dust(int color, float size) {
		return new DustParticleOptions(color, size);
	}

	/** Результат луча: конечная точка (с учётом сущности), попадание в блок и сущность (может быть null). */
	record Ray(Vec3 end, BlockHitResult block, LivingEntity entity) {
	}

	static Ray ray(ServerLevel level, Player player, double range, double inflate) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1f);
		Vec3 aimEnd = eye.add(look.scale(range));
		BlockHitResult block = level.clip(new ClipContext(eye, aimEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 limit = block.getType() == HitResult.Type.MISS ? aimEnd : block.getLocation();

		LivingEntity best = null;
		Vec3 bestPoint = null;
		double bestDist = eye.distanceToSqr(limit);

		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, limit).inflate(1.0 + inflate), e -> isTarget(e, player))) {
			var hit = e.getBoundingBox().inflate(0.15 + inflate).clip(eye, limit);

			if (hit.isPresent()) {
				double d = eye.distanceToSqr(hit.get());

				if (d < bestDist) {
					bestDist = d;
					best = e;
					bestPoint = hit.get();
				}
			}
		}

		return new Ray(bestPoint != null ? bestPoint : limit, block, best);
	}

	/** Все цели вдоль отрезка (луч насквозь). */
	static List<LivingEntity> pierce(ServerLevel level, Player player, Vec3 from, Vec3 to, double radius) {
		List<LivingEntity> list = new ArrayList<>();

		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(radius + 1), e -> isTarget(e, player))) {
			AABB box = e.getBoundingBox().inflate(radius);

			if (box.clip(from, to).isPresent() || box.contains(from)) {
				list.add(e);
			}
		}

		list.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
		return list;
	}

	/** Цели вокруг точки. */
	static List<LivingEntity> around(ServerLevel level, Player player, Vec3 center, double radius) {
		return level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
				e -> isTarget(e, player) && e.getBoundingBox().getCenter().distanceToSqr(center) <= radius * radius);
	}

	/** Цели в конусе взгляда. */
	static List<LivingEntity> cone(ServerLevel level, Player player, double range, double maxAngle) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1f);
		return level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range), e -> {
			if (!isTarget(e, player)) {
				return false;
			}

			Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
			return to.length() <= range && angle(look, to) <= maxAngle && player.hasLineOfSight(e);
		});
	}

	/** Можно ли бить эту сущность: не сам игрок, не его питомцы, не творческие игроки. */
	static boolean isTarget(Entity e, Player player) {
		if (!(e instanceof LivingEntity living) || e == player || !living.isAlive() || e.isSpectator()) {
			return false;
		}

		if (e instanceof TamableAnimal tame && tame.isOwnedBy(player)) {
			return false;
		}

		if (MagicState.isMinionOf(e, player)) {
			return false;
		}

		return !(e instanceof Player other && (other.isCreative() || other.isSpectator()));
	}

	static boolean isBoss(Entity e) {
		return e instanceof EnderDragon || e instanceof WitherBoss || e instanceof LivingEntity l && l.getMaxHealth() > 150;
	}

	static boolean hostile(Entity e) {
		return e instanceof Enemy || e instanceof Mob mob && mob.getTarget() instanceof Player;
	}

	static void hurt(ServerLevel level, ServerPlayer player, LivingEntity target, float damage) {
		target.invulnerableTime = 0;
		target.hurtServer(level, level.damageSources().playerAttack(player), damage);
	}

	static void push(Entity target, Vec3 dir, double strength, double lift) {
		Vec3 flat = new Vec3(dir.x, 0, dir.z);
		flat = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize();
		double resist = target instanceof LivingEntity living
				? 1.0 - Mth.clamp(living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0, 1)
				: 1.0;
		target.push(flat.x * strength * resist, lift * resist, flat.z * strength * resist);
		target.hurtMarked = true;
	}

	static double angle(Vec3 a, Vec3 b) {
		double dot = a.normalize().dot(b.normalize());
		return Math.toDegrees(Math.acos(Mth.clamp(dot, -1, 1)));
	}

	static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, double step) {
		Vec3 delta = to.subtract(from);
		int steps = Mth.clamp((int) (delta.length() / step), 1, 300);

		for (int i = 0; i <= steps; i++) {
			Vec3 p = from.add(delta.scale(i / (double) steps));
			level.sendParticles(particle, true, false, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}
	}

	/** Окружность радиуса r в плоскости, перпендикулярной normal. */
	static void circle(ServerLevel level, ParticleOptions particle, Vec3 center, Vec3 normal, double r, int points, double phase) {
		Vec3[] basis = basis(normal);

		for (int i = 0; i < points; i++) {
			double a = i * Math.PI * 2 / points + phase;
			Vec3 p = center.add(basis[0].scale(Math.cos(a) * r)).add(basis[1].scale(Math.sin(a) * r));
			level.sendParticles(particle, true, false, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}
	}

	/** Мандала: два кольца, вращающиеся спицы и руны между ними. */
	static void mandala(ServerLevel level, ParticleOptions main, ParticleOptions accent, Vec3 center, Vec3 normal, double r, double phase) {
		circle(level, main, center, normal, r, Math.max(16, (int) (r * 14)), phase);
		circle(level, main, center, normal, r * 0.62, Math.max(12, (int) (r * 9)), -phase * 1.3);
		Vec3[] basis = basis(normal);

		for (int s = 0; s < 8; s++) {
			double a = s * Math.PI / 4 + phase * 0.6;
			Vec3 dir = basis[0].scale(Math.cos(a)).add(basis[1].scale(Math.sin(a)));

			for (double t = 0.62; t <= 1.0; t += 0.12) {
				Vec3 p = center.add(dir.scale(r * t));
				level.sendParticles(accent, true, false, p.x, p.y, p.z, 1, 0, 0, 0, 0);
			}
		}

		// Квадрат, вписанный между кольцами, вращается в другую сторону.
		for (int side = 0; side < 4; side++) {
			double a0 = side * Math.PI / 2 - phase;
			double a1 = a0 + Math.PI / 2;
			Vec3 p0 = center.add(basis[0].scale(Math.cos(a0) * r * 0.62)).add(basis[1].scale(Math.sin(a0) * r * 0.62));
			Vec3 p1 = center.add(basis[0].scale(Math.cos(a1) * r * 0.62)).add(basis[1].scale(Math.sin(a1) * r * 0.62));

			for (double t = 0; t < 1; t += 0.2) {
				Vec3 p = p0.add(p1.subtract(p0).scale(t));
				level.sendParticles(accent, true, false, p.x, p.y, p.z, 1, 0, 0, 0, 0);
			}
		}
	}

	/** Два единичных вектора, перпендикулярных normal и друг другу. */
	static Vec3[] basis(Vec3 normal) {
		Vec3 n = normal.lengthSqr() < 1.0E-6 ? new Vec3(0, 1, 0) : normal.normalize();
		Vec3 ref = Math.abs(n.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
		Vec3 a = n.cross(ref).normalize();
		Vec3 b = n.cross(a).normalize();
		return new Vec3[] {a, b};
	}

	static void sound(ServerLevel level, Entity at, SoundEvent sound, float volume, float pitch) {
		level.playSound(null, at.getX(), at.getEyeY(), at.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
	}

	static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
		level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
	}

	/** Правая ладонь игрока. */
	static Vec3 hand(Player player) {
		Vec3 look = player.getViewVector(1f);
		Vec3 flat = new Vec3(look.x, 0, look.z);
		flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
		Vec3 right = new Vec3(-flat.z, 0, flat.x);
		return player.getEyePosition().add(look.scale(0.8)).add(right.scale(0.35)).add(0, -0.35, 0);
	}

	/** Горизонтальное направление взгляда. */
	static Vec3 flatLook(Player player) {
		Vec3 look = player.getViewVector(1f);
		Vec3 flat = new Vec3(look.x, 0, look.z);
		return flat.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0, player.getYRot()) : flat.normalize();
	}

	/** Точка, куда можно встать: над поверхностью, в которую упёрся луч (или в конце луча). */
	static Vec3 standingSpot(ServerLevel level, BlockHitResult hit, Vec3 fallback) {
		if (hit.getType() != HitResult.Type.BLOCK) {
			return fallback;
		}

		BlockPos pos = hit.getBlockPos().relative(hit.getDirection());

		if (hit.getDirection() == Direction.DOWN) {
			pos = pos.below();
		}

		for (int i = 0; i < 4 && !(level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
				&& level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()); i++) {
			pos = pos.above();
		}

		return Vec3.atBottomCenterOf(pos);
	}

	static void clearHarmful(LivingEntity entity) {
		for (MobEffectInstance effect : new ArrayList<>(entity.getActiveEffects())) {
			if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
				entity.removeEffect(effect.getEffect());
			}
		}
	}
}
