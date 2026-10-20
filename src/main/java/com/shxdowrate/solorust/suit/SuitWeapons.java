package com.shxdowrate.solorust.suit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.shxdowrate.solorust.registry.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Серверная логика всех систем костюма. */
public final class SuitWeapons {
	private static final DustParticleOptions LASER_DUST = new DustParticleOptions(0xFF2A2A, 0.7f);
	private static final DustParticleOptions NANO_DUST = new DustParticleOptions(0xB46CFF, 1.0f);

	/** Лазер-резак: какой блок сейчас режет игрок и сколько тиков уже. */
	private record Cut(BlockPos pos, int ticks) {
	}

	private static final Map<UUID, Cut> CUTTING = new HashMap<>();

	private SuitWeapons() {
	}

	// ================================================================== точки входа

	/** Начало удержания ПКМ (HOLD / CHARGE). */
	public static void start(ServerLevel level, ServerPlayer player, SuitTier tier, SuitWeapon weapon) {
		switch (weapon) {
			case FLAMETHROWER -> sound(level, player, SoundEvents.FIRECHARGE_USE, 1.0f, 0.7f);
			case SHOULDER_GUNS -> sound(level, player, SoundEvents.DISPENSER_LAUNCH, 0.8f, 0.6f);
			case CUTTING_LASER -> sound(level, player, SoundEvents.BEACON_ACTIVATE, 0.6f, 2.0f);
			case NANO_SHIELD -> sound(level, player, SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.6f);
			case CHEST_BEAM -> sound(level, player, SoundEvents.WARDEN_SONIC_CHARGE, 1.0f, 1.3f);
			case NANO_CANNON -> sound(level, player, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.0f, 0.8f);
			case COLLAPSE -> sound(level, player, SoundEvents.BEACON_ACTIVATE, 1.5f, 0.5f);
			case CHAOS_BLAST -> sound(level, player, SoundEvents.WARDEN_SONIC_CHARGE, 1.5f, 0.5f);
			case CHAOS_SPHERE -> sound(level, player, SoundEvents.FIRECHARGE_USE, 0.8f, 0.5f);
			case RUNE_SHIELD -> sound(level, player, SoundEvents.EVOKER_CAST_SPELL, 0.8f, 1.2f);
			case HEAT_VISION -> sound(level, player, SoundEvents.FIRECHARGE_USE, 0.6f, 1.6f);
			case DRILL -> sound(level, player, SoundEvents.GRINDSTONE_USE, 0.8f, 0.6f);
			default -> {
			}
		}

		CUTTING.remove(player.getUUID());
		SuitAbilities.releaseHeld(player);
	}

	/** ПКМ у мгновенных систем; у заряжаемых — отпускание ПКМ с долей заряда 0..1. */
	public static void fire(ServerLevel level, ServerPlayer player, SuitTier tier, SuitWeapon weapon, float charge) {
		switch (weapon) {
			case REPULSOR -> repulsor(level, player, tier);
			case WRIST_ROCKET -> wristRocket(level, player, tier);
			case MICRO_MISSILES -> microMissiles(level, player, tier);
			case FLARES -> flares(level, player);
			case SCANNER -> scanner(level, player);
			case PILE_DRIVER -> pileDriver(level, player, tier);
			case HEAVY_ROCKETS -> heavyRockets(level, player, tier);
			case NANO_BLADE -> bladeDash(level, player, tier);
			case CHEST_BEAM -> chestBeam(level, player, tier, charge);
			case NANO_CANNON -> nanoCannon(level, player, tier, charge);
			case GRAVITY_GRIP, SUPER_GRAB -> SuitAbilities.throwHeld(level, player, tier, weapon, charge);
			case TIME_DILATION -> SuitAbilities.timeDilation(level, player);
			case SPACE_JUMP -> SuitAbilities.spaceJump(level, player);
			case MATTER_SHIFT -> SuitAbilities.matterShift(level, player);
			case MIND_LINK -> SuitAbilities.mindLink(level, player);
			case LIFE_SURGE -> SuitAbilities.lifeSurge(level, player);
			case COLLAPSE -> SuitAbilities.collapse(level, player, tier);
			case MYSTIC_BOLT -> SuitAbilities.mysticBolt(level, player, tier);
			case PORTAL -> SuitAbilities.portal(level, player);
			case ASTRAL_FORM -> SuitAbilities.astralForm(level, player);
			case TIME_LOOP -> SuitAbilities.timeLoop(level, player);
			case SONIC_CLAP -> SuitAbilities.sonicClap(level, player, tier);
			case SEISMIC -> SuitAbilities.seismic(level, player);
			case LIGHT_FLARE -> SuitAbilities.lightFlare(level, player);
			case BUILD_WALL, BUILD_BRIDGE, BUILD_FLOOR, DEMOLISH -> SuitAbilities.build(level, player, weapon);
			case CLOAK -> SuitAbilities.cloak(level, player);
			case GRAPPLE -> SuitAbilities.grapple(level, player);
			case CHAOS_BOLT -> ChaosAbilities.bolt(level, player, tier);
			case CHAOS_CHAIN -> ChaosAbilities.chain(level, player, tier);
			case CHAOS_WAVE -> ChaosAbilities.wave(level, player, tier);
			case CHAOS_VORTEX -> ChaosAbilities.vortex(level, player);
			case CHAOS_METEORS -> ChaosAbilities.meteors(level, player, tier);
			case CHAOS_RIFT -> ChaosAbilities.rift(level, player, tier);
			case CHAOS_CURSE -> ChaosAbilities.curse(level, player);
			case CHAOS_BLAST -> ChaosAbilities.blast(level, player, tier);
			case MARKSMAN -> WardenWeapons.marksman(level, player, tier);
			case CRYO_MISSILE -> WardenWeapons.cryoMissile(level, player, tier);
			case LOVE_ARROW -> WardenWeapons.loveArrow(level, player, tier);
			case DECOY -> WardenWeapons.decoy(level, player, tier);
			default -> {
			}
		}
	}

	/** Каждый тик, пока зажата ПКМ у HOLD-систем. */
	public static void hold(ServerLevel level, ServerPlayer player, SuitTier tier, SuitWeapon weapon, int ticks) {
		switch (weapon) {
			case FLAMETHROWER -> flamethrower(level, player, tier, ticks);
			case SHOULDER_GUNS -> shoulderGuns(level, player, tier, ticks);
			case CUTTING_LASER -> laser(level, player, tier, ticks);
			case NANO_SHIELD -> shieldVisual(level, player, ticks);
			case RUNE_SHIELD -> SuitAbilities.runeShield(level, player, ticks);
			case HEAT_VISION -> SuitAbilities.heatVision(level, player, tier, ticks);
			case DRILL -> SuitAbilities.drill(level, player, ticks);
			case CHAOS_SPHERE -> ChaosAbilities.sphere(level, player, tier, ticks);
			case WRIST_RIFLE -> WardenWeapons.wristRifle(level, player, tier, ticks);
			case SOUL_DRAIN -> WardenWeapons.soulDrain(level, player, tier, ticks);
			default -> {
			}
		}
	}

	/** Каждый тик зарядки CHARGE-систем: искры собираются к точке выстрела. */
	public static void charging(ServerLevel level, ServerPlayer player, SuitTier tier, SuitWeapon weapon, int ticks) {
		if (weapon == SuitWeapon.GRAVITY_GRIP || weapon == SuitWeapon.SUPER_GRAB) {
			SuitAbilities.grip(level, player, weapon, ticks);

			if (weapon == SuitWeapon.SUPER_GRAB) {
				return;
			}
		}

		Vec3 origin = weapon == SuitWeapon.CHEST_BEAM ? chest(player) : hand(player);
		RandomSource random = player.getRandom();
		float fraction = Math.min(1f, (float) ticks / weapon.maxCharge);
		ParticleOptions particle = switch (weapon) {
			case NANO_CANNON -> ParticleTypes.SOUL_FIRE_FLAME;
			case GRAVITY_GRIP, COLLAPSE -> ParticleTypes.REVERSE_PORTAL;
			case CHAOS_BLAST -> ChaosAbilities.RED;
			default -> ParticleTypes.END_ROD;
		};

		for (int i = 0; i < 1 + (int) (fraction * 3); i++) {
			Vec3 from = origin.add(random.nextGaussian() * 0.6, random.nextGaussian() * 0.6, random.nextGaussian() * 0.6);
			Vec3 v = origin.subtract(from).scale(0.25);
			level.sendParticles(particle, from.x, from.y, from.z, 0, v.x, v.y, v.z, 1.0);
		}

		if (ticks == weapon.minCharge) {
			sound(level, player, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.4f, 0.8f);
		}

		if (ticks == weapon.maxCharge) {
			sound(level, player, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6f, 1.6f);
		}
	}

	// ================================================================== мгновенные

	private static void repulsor(ServerLevel level, ServerPlayer player, SuitTier tier) {
		Vec3 from = hand(player);
		Ray ray = ray(level, player, from, 48, 0.3);
		float damage = 6.5f * tier.power;

		line(level, ParticleTypes.END_ROD, from, ray.end, 0.8);

		if (ray.entity != null) {
			hurt(level, player, ray.entity, damage);
			push(ray.entity, player.getViewVector(1f), 1.0 * tier.power, 0.25);
		}

		impact(level, ray.end, ParticleTypes.ELECTRIC_SPARK, 8);
		sound(level, player, SoundEvents.FIRECHARGE_USE, 0.8f, 1.9f);
		sound(level, player, SoundEvents.BEACON_POWER_SELECT, 0.4f, 2.0f);
	}

	private static void wristRocket(ServerLevel level, ServerPlayer player, SuitTier tier) {
		Vec3 from = hand(player);
		Vec3 dir = player.getViewVector(1f);
		SuitProjectiles.launch(level, player, from, dir.scale(1.3), null, SuitProjectiles.Kind.ROCKET, 2.2f * tier.power);
		sound(level, player, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.2f, 0.7f);
	}

	private static void microMissiles(ServerLevel level, ServerPlayer player, SuitTier tier) {
		List<LivingEntity> targets = targetsInView(level, player, 64, 35);
		RandomSource random = player.getRandom();
		Vec3 look = player.getViewVector(1f);
		int count = 6;

		for (int i = 0; i < count; i++) {
			LivingEntity target = targets.isEmpty() ? null : targets.get(i % targets.size());
			// Ракеты вылетают из плеч веером вверх и в стороны, потом доворачивают на цель.
			Vec3 from = offset(player, 0.2, (i % 2 == 0 ? 0.45 : -0.45), -0.15);
			Vec3 up = new Vec3(0, 1, 0);
			Vec3 right = look.cross(up).normalize();
			Vec3 vel = look.scale(0.5).add(up.scale(0.45 + random.nextFloat() * 0.3)).add(right.scale((random.nextFloat() - 0.5) * 0.9));
			SuitProjectiles.launch(level, player, from, vel, target, SuitProjectiles.Kind.MISSILE, 1.4f * tier.power, i * 2);
		}

		sound(level, player, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0f, 1.4f);
		sound(level, player, SoundEvents.DISPENSER_LAUNCH, 0.8f, 1.6f);

		player.sendSystemMessage(Component.translatable(targets.isEmpty() ? "message.solorust.suit.no_lock" : "message.solorust.suit.locked", targets.size())
				.withStyle(ChatFormatting.AQUA), true);
	}

	private static void heavyRockets(ServerLevel level, ServerPlayer player, SuitTier tier) {
		Ray ray = ray(level, player, player.getEyePosition(), 96, 0.6);
		LivingEntity target = ray.entity;

		if (target == null) {
			List<LivingEntity> view = targetsInView(level, player, 96, 15);
			target = view.isEmpty() ? null : view.get(0);
		}

		Vec3 look = player.getViewVector(1f);

		for (int i = 0; i < 4; i++) {
			Vec3 from = offset(player, 0.1, i < 2 ? 0.7 : -0.7, 0.2 + (i % 2) * 0.25);
			Vec3 vel = look.scale(0.9).add(0, 0.25, 0);
			SuitProjectiles.launch(level, player, from, vel, target, SuitProjectiles.Kind.HEAVY, 3.2f, i * 4);
		}

		sound(level, player, SoundEvents.FIREWORK_ROCKET_LAUNCH, 2.0f, 0.5f);
	}

	private static void flares(ServerLevel level, ServerPlayer player) {
		Vec3 c = player.position().add(0, player.getBbHeight() * 0.6, 0);
		RandomSource random = player.getRandom();

		for (int i = 0; i < 24; i++) {
			double a = i / 24.0 * Math.PI * 2;
			Vec3 v = new Vec3(Math.cos(a) * 0.5, 0.25 + random.nextFloat() * 0.3, Math.sin(a) * 0.5);
			level.sendParticles(ParticleTypes.FIREWORK, c.x, c.y, c.z, 0, v.x, v.y, v.z, 1.0);
			level.sendParticles(ParticleTypes.FLAME, c.x, c.y, c.z, 0, v.x * 0.6, v.y * 0.4, v.z * 0.6, 1.0);
		}

		// Ракеты бандитов уходят на ложные цели.
		com.shxdowrate.solorust.war.BanditRockets.divert(level, player);

		// Сбиваем снаряды вокруг.
		for (Entity e : level.getEntities(player, player.getBoundingBox().inflate(14), e -> e instanceof Projectile)) {
			Projectile projectile = (Projectile) e;

			if (projectile.getOwner() == player) {
				continue;
			}

			level.sendParticles(ParticleTypes.POOF, e.getX(), e.getY(), e.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
			e.discard();
		}

		// Ослепляем и отбрасываем врагов поблизости.
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(8), e -> isTarget(e, player))) {
			e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
			e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
			e.igniteForSeconds(3);
			Vec3 away = e.position().subtract(player.position()).normalize();
			push(e, away, 1.2, 0.35);
		}

		sound(level, player, SoundEvents.FIREWORK_ROCKET_BLAST, 1.5f, 0.8f);
		sound(level, player, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.0f, 1.2f);
	}

	private static void scanner(ServerLevel level, ServerPlayer player) {
		int hostile = 0;
		int other = 0;

		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(48), e -> e != player && e.isAlive())) {
			e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));

			if (e instanceof Enemy) {
				hostile++;
			} else {
				other++;
			}
		}

		player.sendSystemMessage(Component.translatable("message.solorust.suit.scan", hostile, other).withStyle(ChatFormatting.AQUA), true);
		sound(level, player, SoundEvents.SPYGLASS_USE, 1.0f, 0.6f);
		sound(level, player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.8f);

		// Кольцо сканирующей волны.
		for (int i = 0; i < 40; i++) {
			double a = i / 40.0 * Math.PI * 2;
			level.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 0.2, player.getZ(), 0, Math.cos(a), 0, Math.sin(a), 0.6);
		}
	}

	private static void pileDriver(ServerLevel level, ServerPlayer player, SuitTier tier) {
		Vec3 look = player.getViewVector(1f);
		Vec3 eye = player.getEyePosition();
		double range = 4.5 + player.getBbWidth();
		Vec3 fist = eye.add(look.scale(range * 0.6));
		boolean hit = false;

		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range), e -> isTarget(e, player))) {
			Vec3 to = e.getBoundingBox().getCenter().subtract(eye);

			if (to.length() > range + e.getBbWidth() || angle(look, to) > 50) {
				continue;
			}

			hurt(level, player, e, 16f * tier.power);
			push(e, look, 2.6, 0.6);
			hit = true;
		}

		level.sendParticles(ParticleTypes.EXPLOSION, fist.x, fist.y, fist.z, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.CLOUD, fist.x, fist.y, fist.z, 12, 0.4, 0.4, 0.4, 0.15);

		// Удар в блок перед собой — пыль.
		BlockHitResult block = level.clip(new ClipContext(eye, eye.add(look.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

		if (block.getType() == HitResult.Type.BLOCK) {
			BlockState state = level.getBlockState(block.getBlockPos());
			Vec3 p = block.getLocation();
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), p.x, p.y, p.z, 25, 0.4, 0.4, 0.4, 0.2);
		}

		sound(level, player, SoundEvents.MACE_SMASH_GROUND_HEAVY, 1.4f, 0.7f);
		sound(level, player, hit ? SoundEvents.ANVIL_LAND : SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f, 0.6f);
		player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
	}

	private static void bladeDash(ServerLevel level, ServerPlayer player, SuitTier tier) {
		Vec3 look = player.getViewVector(1f);
		Vec3 from = player.getEyePosition();
		Ray ray = ray(level, player, from, 9, 0);
		Vec3 to = ray.blockEnd;

		for (LivingEntity e : pierce(level, player, from, to, 1.4)) {
			hurt(level, player, e, 11f * tier.power);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY(0.5), e.getZ(), 1, 0, 0, 0, 0);
		}

		line(level, NANO_DUST, from.add(0, -0.4, 0), to.add(0, -0.4, 0), 0.5);
		Vec3 dash = look.scale(2.2);
		player.setDeltaMovement(dash.x, Math.max(0.15, dash.y), dash.z);
		player.hurtMarked = true;
		player.fallDistance = 0;

		sound(level, player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.4f);
		sound(level, player, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 0.8f, 1.5f);
		player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
	}

	// ================================================================== заряжаемые

	private static void chestBeam(ServerLevel level, ServerPlayer player, SuitTier tier, float charge) {
		Vec3 from = chest(player);
		Ray ray = ray(level, player, from, 72, 0);
		float damage = (10f + 26f * charge) * tier.power;
		double radius = 0.8 + charge;

		for (LivingEntity e : pierce(level, player, from, ray.blockEnd, radius)) {
			hurt(level, player, e, damage);
			push(e, player.getViewVector(1f), 1.5 * charge + 0.5, 0.3);
			e.igniteForSeconds(2 + 4 * charge);
		}

		line(level, ParticleTypes.END_ROD, from, ray.blockEnd, 0.35);
		line(level, ParticleTypes.ELECTRIC_SPARK, from, ray.blockEnd, 0.5);

		if (charge > 0.5f) {
			line(level, ParticleTypes.SONIC_BOOM, from.add(player.getViewVector(1f).scale(2)), ray.blockEnd, 4.0);
		}

		impact(level, ray.blockEnd, ParticleTypes.FLAME, 20);
		level.sendParticles(ParticleTypes.EXPLOSION, ray.blockEnd.x, ray.blockEnd.y, ray.blockEnd.z, 1, 0, 0, 0, 0);
		sound(level, player, SoundEvents.WARDEN_SONIC_BOOM, 1.5f, 1.2f + charge * 0.3f);
	}

	private static void nanoCannon(ServerLevel level, ServerPlayer player, SuitTier tier, float charge) {
		Vec3 from = hand(player);
		Ray ray = ray(level, player, from, 96, 0.5);
		Vec3 hit = ray.end;

		line(level, ParticleTypes.SOUL_FIRE_FLAME, from, hit, 0.4);
		line(level, NANO_DUST, from, hit, 0.25);
		level.explode(player, hit.x, hit.y, hit.z, 2.0f + 4.0f * charge, Level.ExplosionInteraction.NONE);
		sound(level, player, SoundEvents.WARDEN_SONIC_BOOM, 1.2f, 0.6f);
		sound(level, player, SoundEvents.TRIDENT_THUNDER.value(), 0.8f, 1.6f);
	}

	// ================================================================== удержание

	private static void flamethrower(ServerLevel level, ServerPlayer player, SuitTier tier, int ticks) {
		Vec3 from = hand(player);
		Vec3 look = player.getViewVector(1f);
		RandomSource random = player.getRandom();

		for (int i = 0; i < 6; i++) {
			Vec3 v = look.scale(0.7 + random.nextFloat() * 0.3).add(random.nextGaussian() * 0.08, random.nextGaussian() * 0.08, random.nextGaussian() * 0.08);
			level.sendParticles(ParticleTypes.FLAME, from.x, from.y, from.z, 0, v.x, v.y, v.z, 1.0);
		}

		if (ticks % 2 == 0) {
			level.sendParticles(ParticleTypes.LARGE_SMOKE, from.x + look.x * 4, from.y + look.y * 4, from.z + look.z * 4, 1, 0.3, 0.3, 0.3, 0.02);
		}

		if (ticks % 4 == 0) {
			Vec3 eye = player.getEyePosition();

			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(8), e -> isTarget(e, player))) {
				Vec3 to = e.getBoundingBox().getCenter().subtract(eye);

				if (to.length() < 8 && angle(look, to) < 25 && e.hasLineOfSight(player)) {
					hurt(level, player, e, 2.5f * tier.power);
					e.igniteForSeconds(5);
				}
			}
		}

		if (ticks % 6 == 0) {
			sound(level, player, SoundEvents.BLAZE_BURN, 0.8f, 0.6f);
		}
	}

	private static void shoulderGuns(ServerLevel level, ServerPlayer player, SuitTier tier, int ticks) {
		if (ticks % 3 != 0) {
			return;
		}

		Vec3 eye = player.getEyePosition();
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(32),
						e -> e instanceof Enemy && isTarget(e, player) && player.hasLineOfSight(e));
		targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));

		Vec3 left = offset(player, 0.1, 0.45, 0.15);
		Vec3 right = offset(player, 0.1, -0.45, 0.15);

		if (targets.isEmpty()) {
			// Нет целей — стреляем туда, куда смотрим.
			Ray ray = ray(level, player, eye, 48, 0.2);
			line(level, ParticleTypes.ELECTRIC_SPARK, (ticks / 3) % 2 == 0 ? left : right, ray.end, 2.0);

			if (ray.entity != null) {
				hurt(level, player, ray.entity, 3f * tier.power);
			}

			impact(level, ray.end, ParticleTypes.SMOKE, 2);
		} else {
			for (int i = 0; i < Math.min(2, targets.size()); i++) {
				LivingEntity target = targets.get(i);
				Vec3 at = target.getBoundingBox().getCenter();
				line(level, ParticleTypes.ELECTRIC_SPARK, i == 0 ? left : right, at, 2.0);
				hurt(level, player, target, 3f * tier.power);
			}
		}

		sound(level, player, ModSounds.SHOT_SMG, 1.2f, 1.25f);
	}

	private static void laser(ServerLevel level, ServerPlayer player, SuitTier tier, int ticks) {
		Vec3 from = hand(player);
		Ray ray = ray(level, player, from, 40, 0.1);

		line(level, LASER_DUST, from, ray.end, 0.3);

		if (ray.entity != null) {
			// Каждые 2 тика, игнорируя неуязвимость после удара — лазер «прожигает».
			if (ticks % 2 == 0) {
				hurt(level, player, ray.entity, 2.2f * tier.power);
				ray.entity.igniteForSeconds(2);
			}

			CUTTING.remove(player.getUUID());
		} else if (ray.block.getType() == HitResult.Type.BLOCK) {
			cutBlock(level, player, ray.block.getBlockPos(), ray.end);
		}

		if (ticks % 2 == 0) {
			level.sendParticles(ParticleTypes.SMALL_FLAME, ray.end.x, ray.end.y, ray.end.z, 2, 0.05, 0.05, 0.05, 0.01);
		}

		if (ticks % 10 == 0) {
			sound(level, player, SoundEvents.BEACON_AMBIENT, 0.8f, 2.0f);
		}
	}

	/** Лазер режет блок, если держать луч на нём (твёрдость определяет время). Обсидиан и прочнее не режет. */
	private static void cutBlock(ServerLevel level, ServerPlayer player, BlockPos pos, Vec3 point) {
		BlockState state = level.getBlockState(pos);
		float hardness = state.getDestroySpeed(level, pos);

		if (hardness < 0 || hardness >= 50 || !player.mayBuild() || !level.mayInteract(player, pos)) {
			return;
		}

		Cut cut = CUTTING.get(player.getUUID());
		int ticks = cut != null && cut.pos().equals(pos) ? cut.ticks() + 1 : 1;
		int needed = Math.max(4, (int) (hardness * 8));

		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), point.x, point.y, point.z, 2, 0.05, 0.05, 0.05, 0.05);

		if (ticks >= needed) {
			level.destroyBlock(pos, true, player);
			CUTTING.remove(player.getUUID());
		} else {
			CUTTING.put(player.getUUID(), new Cut(pos, ticks));
		}
	}

	private static void shieldVisual(ServerLevel level, ServerPlayer player, int ticks) {
		if (ticks % 3 != 0) {
			return;
		}

		Vec3 look = player.getViewVector(1f);
		Vec3 c = player.getEyePosition().add(look.scale(1.1)).add(0, -0.3, 0);
		Vec3 up = new Vec3(0, 1, 0);
		Vec3 right = look.cross(up).normalize();
		Vec3 realUp = right.cross(look).normalize();

		for (int i = 0; i < 16; i++) {
			double a = i / 16.0 * Math.PI * 2 + ticks * 0.1;
			Vec3 p = c.add(right.scale(Math.cos(a) * 0.9)).add(realUp.scale(Math.sin(a) * 0.9));
			level.sendParticles(NANO_DUST, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}
	}

	// ================================================================== общее

	/** Результат луча: точка конца (с учётом сущности), блок, сущность и конец луча без учёта сущностей. */
	record Ray(Vec3 end, Vec3 blockEnd, BlockHitResult block, LivingEntity entity) {
	}

	static Ray ray(ServerLevel level, Player player, Vec3 from, double range, double inflate) {
		Vec3 aimFrom = player.getEyePosition();
		Vec3 look = player.getViewVector(1f);
		Vec3 aimEnd = aimFrom.add(look.scale(range));

		// Целимся по перекрестью (от глаз), а луч рисуем от руки/груди к найденной точке.
		BlockHitResult block = level.clip(new ClipContext(aimFrom, aimEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 limit = block.getType() == HitResult.Type.MISS ? aimEnd : block.getLocation();

		LivingEntity best = null;
		Vec3 bestPoint = null;
		double bestDist = aimFrom.distanceToSqr(limit);

		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(aimFrom, limit).inflate(1.0 + inflate), e -> isTarget(e, player))) {
			var hit = e.getBoundingBox().inflate(0.15 + inflate).clip(aimFrom, limit);

			if (hit.isPresent()) {
				double d = aimFrom.distanceToSqr(hit.get());

				if (d < bestDist) {
					bestDist = d;
					best = e;
					bestPoint = hit.get();
				}
			}
		}

		return new Ray(bestPoint != null ? bestPoint : limit, limit, block, best);
	}

	/** Все цели вдоль отрезка (луч насквозь). */
	static List<LivingEntity> pierce(ServerLevel level, Player player, Vec3 from, Vec3 to, double radius) {
		List<LivingEntity> list = new ArrayList<>();

		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(radius + 1), e -> isTarget(e, player))) {
			if (e.getBoundingBox().inflate(radius).clip(from, to).isPresent() || e.getBoundingBox().inflate(radius).contains(from)) {
				list.add(e);
			}
		}

		return list;
	}

	/** Цели в конусе взгляда: сначала враждебные, потом остальные мобы; ближние — первыми. */
	static List<LivingEntity> targetsInView(ServerLevel level, Player player, double range, double maxAngle) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1f);
		List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range), e -> {
			if (!(e instanceof Mob) || !isTarget(e, player)) {
				return false;
			}

			Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
			return to.length() <= range && angle(look, to) <= maxAngle && player.hasLineOfSight(e);
		});

		list.sort(Comparator.<LivingEntity>comparingInt(e -> e instanceof Enemy ? 0 : 1).thenComparingDouble(e -> e.distanceToSqr(player)));
		return list;
	}

	static boolean isTarget(Entity e, Player player) {
		if (!(e instanceof LivingEntity living) || e == player || !living.isAlive() || e.isSpectator()) {
			return false;
		}

		if (e instanceof TamableAnimal tame && tame.isOwnedBy(player)) {
			return false;
		}

		return !(e instanceof Player other && other.isCreative());
	}

	static void hurt(ServerLevel level, ServerPlayer player, LivingEntity target, float damage) {
		target.invulnerableTime = 0;
		target.hurtServer(level, level.damageSources().playerAttack(player), damage);
	}

	static void push(LivingEntity target, Vec3 dir, double strength, double lift) {
		Vec3 flat = new Vec3(dir.x, 0, dir.z);

		if (flat.lengthSqr() < 1.0E-4) {
			flat = new Vec3(0, 0, 0);
		} else {
			flat = flat.normalize();
		}

		double resist = 1.0 - Mth.clamp(target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE), 0, 1);
		target.push(flat.x * strength * resist, lift * resist, flat.z * strength * resist);
		target.hurtMarked = true;
	}

	static double angle(Vec3 a, Vec3 b) {
		double dot = a.normalize().dot(b.normalize());
		return Math.toDegrees(Math.acos(Mth.clamp(dot, -1, 1)));
	}

	static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, double step) {
		Vec3 delta = to.subtract(from);
		int steps = Mth.clamp((int) (delta.length() / step), 1, 220);

		for (int i = 0; i <= steps; i++) {
			Vec3 p = from.add(delta.scale(i / (double) steps));
			level.sendParticles(particle, true, false, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}
	}

	static void impact(ServerLevel level, Vec3 at, ParticleOptions particle, int count) {
		level.sendParticles(particle, at.x, at.y, at.z, count, 0.15, 0.15, 0.15, 0.08);
	}

	static void sound(ServerLevel level, Player player, SoundEvent sound, float volume, float pitch) {
		level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
	}

	/** Точка относительно игрока: вперёд по взгляду, вправо (+) / влево (−) и вверх от уровня глаз. */
	static Vec3 offset(Player player, double forward, double right, double up) {
		Vec3 look = player.getViewVector(1f);
		Vec3 flat = new Vec3(look.x, 0, look.z);
		flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
		Vec3 rightVec = new Vec3(-flat.z, 0, flat.x);
		double s = player.getBbHeight() / 1.8;
		return player.getEyePosition().add(look.scale(forward * s)).add(rightVec.scale(right * s)).add(0, up * s, 0);
	}

	/** Правая ладонь. */
	static Vec3 hand(Player player) {
		return offset(player, 0.8, 0.35, -0.35);
	}

	/** Центр груди (энергоядро). */
	static Vec3 chest(Player player) {
		return offset(player, 0.45, 0, -0.55);
	}

	public static void forget(UUID player) {
		CUTTING.remove(player);
	}
}
