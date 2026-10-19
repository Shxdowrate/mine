package com.shxdowrate.solorust.suit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Костюм хаоса. Главное — три «больших» способности: управляемый вихрь, волна, после которой
 * выжившие зависают в воздухе, и взрыв хаоса, выжигающий всё вокруг.
 */
public final class ChaosAbilities {
	static final DustParticleOptions RED = new DustParticleOptions(0xD4143C, 1.4f);
	static final DustParticleOptions DARK = new DustParticleOptions(0x2A0010, 1.8f);
	static final DustParticleOptions VIOLET = new DustParticleOptions(0x8A2BE2, 1.2f);

	/** Цели с большим запасом здоровья считаются «боссами»: их хаос ранит, но не стирает. */
	private static final float BOSS_HEALTH = 200f;

	// ------------------------------------------------------------------ вихрь

	private static final class Vortex {
		final ServerLevel level;
		final ServerPlayer owner;
		Vec3 pos;
		int age;
		final Set<LivingEntity> caught = new HashSet<>();

		Vortex(ServerLevel level, ServerPlayer owner, Vec3 pos) {
			this.level = level;
			this.owner = owner;
			this.pos = pos;
		}
	}

	private static final int VORTEX_TICKS = 200;
	private static final List<Vortex> VORTICES = new ArrayList<>();

	// ------------------------------------------------------------------ волна

	private static final class Wave {
		final ServerLevel level;
		final ServerPlayer owner;
		final Vec3 origin;
		final Vec3 dir;
		final float power;
		double dist;
		final Set<LivingEntity> hit = new HashSet<>();

		Wave(ServerLevel level, ServerPlayer owner, Vec3 origin, Vec3 dir, float power) {
			this.level = level;
			this.owner = owner;
			this.origin = origin;
			this.dir = dir;
			this.power = power;
		}
	}

	private static final double WAVE_RANGE = 32;
	private static final List<Wave> WAVES = new ArrayList<>();

	/** Зависшие после волны: цель → (высота, до какого тика). */
	private record Hover(double y, long until) {
	}

	private static final Map<LivingEntity, Hover> SUSPENDED = new HashMap<>();

	private ChaosAbilities() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			long time = server.overworld().getGameTime();
			vortices();
			waves(time);
			suspended(time);
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			VORTICES.clear();
			WAVES.clear();
			SUSPENDED.clear();
		});
	}

	// ================================================================== выстрелы

	public static void bolt(ServerLevel level, ServerPlayer player, SuitTier tier) {
		Vec3 from = SuitWeapons.hand(player);
		SuitWeapons.Ray ray = SuitWeapons.ray(level, player, from, 48, 0.3);
		SuitWeapons.line(level, RED, from, ray.end(), 0.4);
		SuitWeapons.line(level, ParticleTypes.SMOKE, from, ray.end(), 1.2);

		if (ray.entity() != null) {
			SuitWeapons.hurt(level, player, ray.entity(), 6f * tier.power);
		}

		level.sendParticles(DARK, ray.end().x, ray.end().y, ray.end().z, 8, 0.2, 0.2, 0.2, 0.0);
		SuitWeapons.sound(level, player, SoundEvents.BLAZE_SHOOT, 0.7f, 1.6f);
	}

	public static void chain(ServerLevel level, ServerPlayer player, SuitTier tier) {
		SuitWeapons.Ray ray = SuitWeapons.ray(level, player, player.getEyePosition(), 40, 0.5);
		LivingEntity current = ray.entity();

		if (current == null) {
			List<LivingEntity> view = SuitWeapons.targetsInView(level, player, 40, 20);
			current = view.isEmpty() ? null : view.get(0);
		}

		if (current == null) {
			player.sendOverlayMessage(Component.translatable("message.solorust.suit.no_lock", 0).withStyle(ChatFormatting.GRAY));
			return;
		}

		Vec3 from = SuitWeapons.hand(player);
		Set<LivingEntity> visited = new HashSet<>();
		float damage = 11f * tier.power;

		for (int i = 0; i < 6 && current != null; i++) {
			Vec3 to = current.getBoundingBox().getCenter();
			SuitWeapons.line(level, RED, from, to, 0.35);
			SuitWeapons.line(level, ParticleTypes.ELECTRIC_SPARK, from, to, 0.6);
			SuitWeapons.hurt(level, player, current, damage);
			visited.add(current);
			damage *= 0.85f;
			from = to;

			LivingEntity next = null;
			double best = 8 * 8;

			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, current.getBoundingBox().inflate(8),
					e -> SuitWeapons.isTarget(e, player) && !visited.contains(e) && !(e instanceof Player))) {
				double d = e.distanceToSqr(current);

				if (d < best) {
					best = d;
					next = e;
				}
			}

			current = next;
		}

		SuitWeapons.sound(level, player, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.5f, 1.8f);
	}

	public static void rift(ServerLevel level, ServerPlayer player, SuitTier tier) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1f);
		BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(16)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 endEye = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation().subtract(look.scale(0.8)) : eye.add(look.scale(16));
		Vec3 dest = endEye.subtract(0, player.getEyeHeight(), 0);

		for (LivingEntity e : SuitWeapons.pierce(level, player, eye, endEye, 1.2)) {
			SuitWeapons.hurt(level, player, e, 12f * tier.power);
		}

		SuitWeapons.line(level, DARK, eye, endEye, 0.3);
		SuitWeapons.line(level, RED, eye, endEye, 0.5);
		player.teleportTo(dest.x, dest.y, dest.z);
		player.fallDistance = 0;
		SuitWeapons.sound(level, player, SoundEvents.ENDERMAN_TELEPORT, 1.0f, 0.5f);
	}

	public static void meteors(ServerLevel level, ServerPlayer player, SuitTier tier) {
		SuitWeapons.Ray ray = SuitWeapons.ray(level, player, player.getEyePosition(), 64, 0.3);
		Vec3 target = ray.end();
		RandomSource r = player.getRandom();

		for (int i = 0; i < 10; i++) {
			Vec3 land = target.add((r.nextDouble() - 0.5) * 12, 0, (r.nextDouble() - 0.5) * 12);
			Vec3 start = land.add((r.nextDouble() - 0.5) * 6, 28 + r.nextDouble() * 8, (r.nextDouble() - 0.5) * 6);
			Vec3 vel = land.subtract(start).normalize().scale(1.6);
			SuitProjectiles.launch(level, player, start, vel, null, SuitProjectiles.Kind.METEOR, 2.4f * tier.power / 1.8f, 1 + i * 4);
		}

		level.sendParticles(RED, target.x, target.y + 0.2, target.z, 40, 5, 0.1, 5, 0.0);
		SuitWeapons.sound(level, player, SoundEvents.WITHER_SHOOT, 1.0f, 0.5f);
		player.sendOverlayMessage(Component.translatable("message.solorust.chaos.meteors").withStyle(ChatFormatting.RED));
	}

	public static void curse(ServerLevel level, ServerPlayer player) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1f);
		int count = 0;

		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(24), e -> SuitWeapons.isTarget(e, player))) {
			Vec3 to = e.getBoundingBox().getCenter().subtract(eye);

			if (to.length() > 24 || SuitWeapons.angle(look, to) > 40) {
				continue;
			}

			e.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1));
			e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
			e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 1));
			e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
			level.sendParticles(ParticleTypes.SOUL, e.getX(), e.getY(1), e.getZ(), 10, 0.3, 0.4, 0.3, 0.02);
			count++;
		}

		for (int i = 1; i <= 8; i++) {
			Vec3 p = eye.add(look.scale(i * 3));
			level.sendParticles(VIOLET, p.x, p.y, p.z, 6, i * 0.25, i * 0.25, i * 0.25, 0.0);
		}

		SuitWeapons.sound(level, player, SoundEvents.WITHER_AMBIENT, 1.0f, 0.6f);
		player.sendOverlayMessage(Component.translatable("message.solorust.chaos.curse", count).withStyle(ChatFormatting.DARK_PURPLE));
	}

	/** Сфера: держи ПКМ — урон гасится (см. SuitManager), а всё рядом обжигает и отталкивает. */
	public static void sphere(ServerLevel level, ServerPlayer player, SuitTier tier, int ticks) {
		Vec3 c = player.position().add(0, player.getBbHeight() * 0.5, 0);
		RandomSource r = player.getRandom();

		for (int i = 0; i < 10; i++) {
			double u = r.nextDouble() * Math.PI * 2;
			double v = Math.acos(2 * r.nextDouble() - 1);
			double rad = 2.0;
			level.sendParticles(i % 2 == 0 ? RED : DARK, c.x + rad * Math.sin(v) * Math.cos(u), c.y + rad * Math.cos(v), c.z + rad * Math.sin(v) * Math.sin(u), 1, 0, 0, 0, 0);
		}

		if (ticks % 10 == 0) {
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(3), e -> SuitWeapons.isTarget(e, player))) {
				SuitWeapons.hurt(level, player, e, 4f * tier.power);
				SuitWeapons.push(e, e.position().subtract(player.position()), 1.0, 0.3);
			}

			SuitWeapons.sound(level, player, SoundEvents.FIRE_AMBIENT, 0.6f, 0.5f);
		}
	}

	// ================================================================== вихрь

	public static void vortex(ServerLevel level, ServerPlayer player) {
		VORTICES.removeIf(v -> v.owner == player);
		SuitWeapons.Ray ray = SuitWeapons.ray(level, player, player.getEyePosition(), 32, 0);
		VORTICES.add(new Vortex(level, player, ray.blockEnd()));
		SuitWeapons.sound(level, player, SoundEvents.ELYTRA_FLYING, 1.0f, 0.5f);
		player.sendOverlayMessage(Component.translatable("message.solorust.chaos.vortex").withStyle(ChatFormatting.RED));
	}

	private static void vortices() {
		for (Iterator<Vortex> it = VORTICES.iterator(); it.hasNext(); ) {
			Vortex v = it.next();
			v.age++;

			if (v.owner.isRemoved() || v.owner.level() != v.level) {
				it.remove();
				continue;
			}

			// Вихрь едет туда, куда смотрит хозяин (управляется взглядом).
			SuitWeapons.Ray ray = SuitWeapons.ray(v.level, v.owner, v.owner.getEyePosition(), 32, 0);
			Vec3 toTarget = ray.blockEnd().subtract(v.pos);
			double step = Math.min(0.5, toTarget.length());

			if (step > 0.01) {
				v.pos = v.pos.add(toTarget.normalize().scale(step));
			}

			Vec3 center = v.pos.add(0, 2, 0);

			for (LivingEntity e : v.level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(10, 6, 10),
					e -> SuitWeapons.isTarget(e, v.owner) && !(e instanceof Player))) {
				Vec3 rel = e.position().subtract(center);
				Vec3 flat = new Vec3(rel.x, 0, rel.z);
				double dist = flat.length();

				if (dist > 10) {
					continue;
				}

				v.caught.add(e);
				// Затягиваем к центру и закручиваем по спирали вверх.
				Vec3 inward = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize().scale(-0.25 - 0.05 * (10 - dist));
				Vec3 spin = new Vec3(-rel.z, 0, rel.x).normalize().scale(0.45);
				double lift = Math.max(-0.1, (center.y + 1.5 - e.getY()) * 0.12);
				e.setDeltaMovement(inward.add(spin).add(0, lift, 0));
				e.hurtMarked = true;
				e.fallDistance = 0;

				if (v.age % 10 == 0) {
					SuitWeapons.hurt(v.level, v.owner, e, 2.5f);
				}
			}

			// Спираль частиц.
			for (int i = 0; i < 6; i++) {
				double h = (i / 6.0) * 6;
				double a = v.age * 0.4 + i * 1.1;
				double rad = 0.6 + h * 0.6;
				double x = v.pos.x + Math.cos(a) * rad;
				double z = v.pos.z + Math.sin(a) * rad;
				v.level.sendParticles(i % 2 == 0 ? RED : DARK, true, false, x, v.pos.y + h, z, 1, 0, 0, 0, 0);
			}

			v.level.sendParticles(ParticleTypes.LARGE_SMOKE, true, false, v.pos.x, v.pos.y + 0.5, v.pos.z, 2, 0.8, 0.2, 0.8, 0.02);

			if (v.age % 20 == 0) {
				v.level.playSound(null, v.pos.x, v.pos.y, v.pos.z, SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.8f, 0.4f);
			}

			if (v.age >= VORTEX_TICKS) {
				finishVortex(v, center);
				it.remove();
			}
		}
	}

	/** Конец вихря: всех пойманных добивает и раскидывает в стороны. */
	private static void finishVortex(Vortex v, Vec3 center) {
		RandomSource r = v.owner.getRandom();

		for (LivingEntity e : v.caught) {
			if (e.isRemoved() || !e.isAlive() || e.distanceToSqr(center) > 14 * 14) {
				continue;
			}

			e.invulnerableTime = 0;
			e.hurtServer(v.level, v.level.damageSources().playerAttack(v.owner), e.getMaxHealth() > BOSS_HEALTH ? 40f : 100000f);
			double a = r.nextDouble() * Math.PI * 2;
			e.setDeltaMovement(Math.cos(a) * 2.2, 1.0, Math.sin(a) * 2.2);
			e.hurtMarked = true;
		}

		v.level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0, 0, 0, 0);
		v.level.sendParticles(RED, center.x, center.y, center.z, 120, 4, 3, 4, 0.0);
		v.level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0f, 0.6f);
	}

	// ================================================================== волна

	public static void wave(ServerLevel level, ServerPlayer player, SuitTier tier) {
		Vec3 look = player.getViewVector(1f);
		Vec3 dir = new Vec3(look.x, 0, look.z);
		dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : dir.normalize();
		WAVES.add(new Wave(level, player, player.position().add(0, 0.5, 0), dir, tier.power));
		SuitWeapons.sound(level, player, SoundEvents.WARDEN_SONIC_BOOM, 1.5f, 0.6f);
	}

	private static void waves(long time) {
		for (Iterator<Wave> it = WAVES.iterator(); it.hasNext(); ) {
			Wave w = it.next();
			double from = w.dist;
			w.dist += 1.5;

			if (w.owner.isRemoved() || w.owner.level() != w.level) {
				it.remove();
				continue;
			}

			// Дуга фронта волны: ±40°, ширина растёт с расстоянием.
			for (int i = -8; i <= 8; i++) {
				Vec3 d = w.dir.yRot((float) Math.toRadians(i * 5));
				Vec3 p = w.origin.add(d.scale(w.dist));
				w.level.sendParticles(i % 2 == 0 ? RED : DARK, true, false, p.x, p.y, p.z, 2, 0.1, 0.6, 0.1, 0.0);
			}

			for (LivingEntity e : w.level.getEntitiesOfClass(LivingEntity.class, new AABB(w.origin, w.origin).inflate(w.dist + 2, 6, w.dist + 2),
					e -> SuitWeapons.isTarget(e, w.owner) && !w.hit.contains(e))) {
				Vec3 rel = e.position().subtract(w.origin);
				Vec3 flat = new Vec3(rel.x, 0, rel.z);
				double d = flat.length();

				if (d < from - 1 || d > w.dist + 1 || SuitWeapons.angle(w.dir, flat) > 42 || Math.abs(rel.y) > 5) {
					continue;
				}

				w.hit.add(e);
				SuitWeapons.hurt(w.level, w.owner, e, 22f * w.power);

				// Кто выжил — зависает в воздухе на 5 секунд.
				if (e.isAlive()) {
					SUSPENDED.put(e, new Hover(e.getY() + 2.5, time + 100));
					w.level.sendParticles(VIOLET, e.getX(), e.getY(0.5), e.getZ(), 12, 0.3, 0.5, 0.3, 0.0);
				}
			}

			if (w.dist >= WAVE_RANGE) {
				it.remove();
			}
		}
	}

	private static void suspended(long time) {
		for (Iterator<Map.Entry<LivingEntity, Hover>> it = SUSPENDED.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<LivingEntity, Hover> entry = it.next();
			LivingEntity e = entry.getKey();

			if (e.isRemoved() || !e.isAlive() || time > entry.getValue().until()) {
				it.remove();
				continue;
			}

			e.setDeltaMovement(0, (entry.getValue().y() - e.getY()) * 0.25, 0);
			e.hurtMarked = true;
			e.fallDistance = 0;

			if (time % 5 == 0 && e.level() instanceof ServerLevel level) {
				level.sendParticles(VIOLET, e.getX(), e.getY(), e.getZ(), 2, 0.3, 0.1, 0.3, 0.0);
			}
		}
	}

	// ================================================================== взрыв хаоса

	/**
	 * Взрыв хаоса. В эпицентре (6 блоков) погибает всё живое, включая игроков. Во всём радиусе (28 блоков)
	 * гибнут мобы; игроки дальше эпицентра получают урон по убыванию; боссы только сильно ранены.
	 */
	public static void blast(ServerLevel level, ServerPlayer player, SuitTier tier) {
		double radius = 28;
		double core = 6;
		Vec3 c = player.position().add(0, 1, 0);
		int killed = 0;

		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius), e -> SuitWeapons.isTarget(e, player))) {
			double d = e.position().distanceTo(player.position());

			if (d > radius) {
				continue;
			}

			boolean boss = e.getMaxHealth() > BOSS_HEALTH;
			float damage;

			if (boss) {
				damage = d <= core ? 200f : 80f;
			} else if (d <= core || !(e instanceof Player)) {
				damage = 100000f;
			} else {
				damage = (float) (24.0 * (1.0 - (d - core) / (radius - core)) + 4.0);
			}

			e.invulnerableTime = 0;
			e.hurtServer(level, level.damageSources().playerAttack(player), damage);
			SuitWeapons.push(e, e.position().subtract(player.position()), 2.5, 0.8);

			if (!e.isAlive()) {
				killed++;
			}
		}

		// Кольца взрыва.
		for (int ring = 1; ring <= 4; ring++) {
			double r = radius * ring / 4.0;

			for (int i = 0; i < 48; i++) {
				double a = i / 48.0 * Math.PI * 2;
				double x = c.x + Math.cos(a) * r;
				double z = c.z + Math.sin(a) * r;
				level.sendParticles(ring % 2 == 0 ? RED : DARK, true, false, x, c.y, z, 3, 0.3, 0.6, 0.3, 0.0);
			}

			level.sendParticles(ParticleTypes.EXPLOSION, true, false, c.x, c.y, c.z, 12, r * 0.5, 1.5, r * 0.5, 0.0);
		}

		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, true, false, c.x, c.y, c.z, 3, 1, 1, 1, 0.0);
		level.sendParticles(VIOLET, true, false, c.x, c.y + 2, c.z, 200, 3, 4, 3, 0.0);
		level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 4.0f, 0.5f);
		level.playSound(null, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 3.0f, 0.6f);
		level.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 3.0f, 0.4f);

		player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
		player.sendOverlayMessage(Component.translatable("message.solorust.chaos.blast", killed).withStyle(ChatFormatting.DARK_RED));
	}

	public static void forget(UUID player) {
		VORTICES.removeIf(v -> v.owner.getUUID().equals(player));
	}
}
