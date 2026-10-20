package com.shxdowrate.solorust.suit;

import java.util.List;

import com.shxdowrate.solorust.registry.ModSounds;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Способности костюмов WARDEN (War Machines), которых нет у силовой брони. */
public final class WardenWeapons {
	private static final DustParticleOptions FROST = new DustParticleOptions(0x9FEFFF, 1.2f);
	private static final DustParticleOptions LOVE = new DustParticleOptions(0xFF5C9D, 1.0f);
	private static final DustParticleOptions SOUL = new DustParticleOptions(0x9C7BFF, 0.9f);
	private static final DustParticleOptions RED = new DustParticleOptions(0xFF2020, 0.6f);

	private WardenWeapons() {
	}

	public static void initDecoys() {
		Decoys.init();
	}

	// ================================================================== наручная винтовка (держи ПКМ)

	static void wristRifle(ServerLevel level, ServerPlayer player, SuitTier tier, int ticks) {
		if (ticks % 3 != 0) {
			return;
		}

		RandomSource random = player.getRandom();
		Vec3 from = SuitWeapons.hand(player);
		SuitWeapons.Ray ray = SuitWeapons.ray(level, player, from, 64, 0.1);
		Vec3 end = ray.end().add(random.nextGaussian() * 0.15, random.nextGaussian() * 0.15, random.nextGaussian() * 0.15);

		tracer(level, from, end);

		if (ray.entity() != null) {
			SuitWeapons.hurt(level, player, ray.entity(), 3.0f * tier.power);
		} else {
			SuitWeapons.impact(level, end, ParticleTypes.SMOKE, 2);
		}

		level.sendParticles(ParticleTypes.FLAME, from.x, from.y, from.z, 1, 0.02, 0.02, 0.02, 0.0);
		SuitWeapons.sound(level, player, ModSounds.SHOT_RIFLE, 1.2f, 1.15f + random.nextFloat() * 0.1f);
	}

	// ================================================================== меткий выстрел (Deadeye)

	static void marksman(ServerLevel level, ServerPlayer player, SuitTier tier) {
		Vec3 from = SuitWeapons.hand(player);
		SuitWeapons.Ray ray = SuitWeapons.ray(level, player, from, 128, 0.05);

		SuitWeapons.line(level, RED, from, ray.end(), 0.5);

		if (ray.entity() != null) {
			LivingEntity target = ray.entity();
			float damage = 14.0f * tier.power;

			// В голову — вдвое больнее.
			if (ray.end().y >= target.getEyeY() - 0.25) {
				damage *= 2.0f;
				level.sendParticles(ParticleTypes.CRIT, ray.end().x, ray.end().y, ray.end().z, 12, 0.1, 0.1, 0.1, 0.3);
			}

			SuitWeapons.hurt(level, player, target, damage);
		}

		SuitWeapons.impact(level, ray.end(), ParticleTypes.ELECTRIC_SPARK, 6);
		SuitWeapons.sound(level, player, ModSounds.SHOT_SNIPER, 3.0f, 1.1f);
	}

	// ================================================================== криоракета

	static void cryoMissile(ServerLevel level, ServerPlayer player, SuitTier tier) {
		Vec3 from = SuitWeapons.hand(player);
		SuitWeapons.Ray ray = SuitWeapons.ray(level, player, from, 72, 0.4);
		Vec3 at = ray.end();

		SuitWeapons.line(level, FROST, from, at, 0.6);
		SuitWeapons.line(level, ParticleTypes.SNOWFLAKE, from, at, 1.5);

		float radius = 5.0f;

		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(radius),
				e -> SuitWeapons.isTarget(e, player) && e.position().distanceTo(at) <= radius + 1)) {
			SuitWeapons.hurt(level, player, e, 5.0f * tier.power);
			e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 3));
			e.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 120, 2));
			e.setTicksFrozen(Math.max(e.getTicksFrozen(), 240));
		}

		for (int i = 0; i < 60; i++) {
			double a = i / 60.0 * Math.PI * 2;
			level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 0, Math.cos(a) * 0.5, 0.1, Math.sin(a) * 0.5, 1.0);
		}

		level.sendParticles(FROST, at.x, at.y, at.z, 40, radius * 0.4, 0.6, radius * 0.4, 0.0);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.GLASS_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.5f, 0.6f);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.POWDER_SNOW_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 2.0f, 0.8f);
		SuitWeapons.sound(level, player, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0f, 1.4f);
	}

	// ================================================================== нано-стрелы (Cupid): 3 самонаводящиеся стрелы

	static void loveArrow(ServerLevel level, ServerPlayer player, SuitTier tier) {
		List<LivingEntity> targets = SuitWeapons.targetsInView(level, player, 56, 25);
		Vec3 from = SuitWeapons.hand(player);

		if (targets.isEmpty()) {
			SuitWeapons.Ray ray = SuitWeapons.ray(level, player, from, 56, 0.3);
			arc(level, from, ray.end(), 0);

			if (ray.entity() != null) {
				SuitWeapons.hurt(level, player, ray.entity(), 7.0f * tier.power);
			}
		} else {
			for (int i = 0; i < 3; i++) {
				LivingEntity target = targets.get(i % targets.size());
				Vec3 to = target.getBoundingBox().getCenter();
				arc(level, from, to, i - 1);
				SuitWeapons.hurt(level, player, target, 4.5f * tier.power);
				level.sendParticles(ParticleTypes.HEART, to.x, to.y + 0.5, to.z, 3, 0.3, 0.3, 0.3, 0.0);
			}
		}

		SuitWeapons.sound(level, player, SoundEvents.ARROW_SHOOT, 1.0f, 1.5f);
		SuitWeapons.sound(level, player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.8f);
	}

	/** Дуга из розовых частиц: стрела летит по изогнутой траектории. */
	private static void arc(ServerLevel level, Vec3 from, Vec3 to, int side) {
		Vec3 delta = to.subtract(from);
		Vec3 sideways = delta.cross(new Vec3(0, 1, 0));
		sideways = sideways.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : sideways.normalize();
		Vec3 bend = sideways.scale(side * Math.min(4.0, delta.length() * 0.15)).add(0, Math.min(3.0, delta.length() * 0.1), 0);
		int steps = Mth.clamp((int) (delta.length() / 0.6), 2, 160);

		for (int i = 0; i <= steps; i++) {
			double t = i / (double) steps;
			Vec3 p = from.add(delta.scale(t)).add(bend.scale(4 * t * (1 - t)));
			level.sendParticles(LOVE, true, false, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}
	}

	// ================================================================== двойник (Trickster)

	/**
	 * Оставляет на месте двойника в таком же костюме, а сам рывком уходит вперёд и становится невидимым.
	 * Враги поблизости переключаются на двойника; через 8 секунд он взрывается.
	 */
	static void decoy(ServerLevel level, ServerPlayer player, SuitTier tier) {
		ArmorStand stand = EntityType.ARMOR_STAND.create(level, EntitySpawnReason.TRIGGERED);

		if (stand != null) {
			stand.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
			stand.setShowArms(true);
			stand.setNoBasePlate(true);
			stand.setInvulnerable(true);

			for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
				stand.setItemSlot(slot, player.getItemBySlot(slot).copy());
			}

			stand.addTag("solorust_decoy");
			level.addFreshEntity(stand);
			Decoys.add(level, stand, player, tier.power);

			for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(40), m -> m.getTarget() == player)) {
				mob.setTarget(stand);
			}
		}

		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1, player.getZ(), 30, 0.4, 0.8, 0.4, 0.1);

		Vec3 look = player.getViewVector(1f);
		Vec3 dash = new Vec3(look.x, Math.max(0.1, look.y), look.z).normalize().scale(2.2);
		player.setDeltaMovement(dash);
		player.hurtMarked = true;
		player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 120, 0, false, false));
		SuitWeapons.sound(level, player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.2f, 1.0f);
	}

	// ================================================================== вытягивание душ (Wraith, держи ПКМ)

	static void soulDrain(ServerLevel level, ServerPlayer player, SuitTier tier, int ticks) {
		Vec3 from = SuitWeapons.hand(player);
		SuitWeapons.Ray ray = SuitWeapons.ray(level, player, from, 32, 0.4);

		if (ticks % 2 == 0) {
			SuitWeapons.line(level, SOUL, from, ray.end(), 0.5);
		}

		if (ray.entity() != null && ticks % 5 == 0) {
			LivingEntity target = ray.entity();
			SuitWeapons.hurt(level, player, target, 2.5f * tier.power);
			player.heal(1.0f);
			Vec3 c = target.getBoundingBox().getCenter();
			level.sendParticles(ParticleTypes.SOUL, c.x, c.y, c.z, 4, 0.3, 0.4, 0.3, 0.02);
		}

		if (ticks % 10 == 0) {
			SuitWeapons.sound(level, player, SoundEvents.SOUL_ESCAPE.value(), 0.8f, 0.7f);
		}
	}

	// ================================================================== общее

	private static void tracer(ServerLevel level, Vec3 from, Vec3 to) {
		Vec3 delta = to.subtract(from);
		int steps = Mth.clamp((int) (delta.length() / 2.0), 1, 40);

		for (int i = 1; i <= steps; i++) {
			Vec3 p = from.add(delta.scale(i / (double) steps));
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, true, false, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}
	}

	/** Двойники Трикстера: живут 8 секунд, потом взрываются. */
	static final class Decoys {
		private record Entry(ServerLevel level, ArmorStand stand, ServerPlayer owner, float power, long until) {
		}

		private static final java.util.List<Entry> ACTIVE = new java.util.ArrayList<>();

		static void add(ServerLevel level, ArmorStand stand, ServerPlayer owner, float power) {
			ACTIVE.add(new Entry(level, stand, owner, power, level.getGameTime() + 160));
		}

		static void init() {
			net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> tick());
			net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVE.clear());
		}

		private static void tick() {
			ACTIVE.removeIf(e -> {
				if (e.stand.isRemoved()) {
					return true;
				}

				if (e.level.getGameTime() % 10 == 0) {
					e.level.sendParticles(ParticleTypes.HAPPY_VILLAGER, e.stand.getX(), e.stand.getY() + 1.2, e.stand.getZ(), 2, 0.3, 0.6, 0.3, 0.0);
				}

				if (e.level.getGameTime() < e.until) {
					return false;
				}

				Vec3 at = e.stand.position().add(0, 1, 0);
				e.stand.discard();
				e.level.explode(e.owner, at.x, at.y, at.z, 2.5f * Math.min(1.6f, e.power), Level.ExplosionInteraction.NONE);
				return true;
			});
		}
	}
}
