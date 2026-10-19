package com.shxdowrate.solorust.magic;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Тиара Алой Ведьмы: 18 способностей Ванды. */
final class WandaAbilities {
	private static final DustParticleOptions CHAOS = Fx.dust(0xE0204F, 1.4f);
	private static final DustParticleOptions CHAOS_LIGHT = Fx.dust(0xFF7A9C, 0.8f);
	private static final DustParticleOptions HEX = Fx.dust(0xB0103A, 2.0f);

	static final int[] COLORS = {0xE0204F, 0xFF4F7B, 0xB0103A, 0xFF7A9C, 0xC2185B, 0xFF8FAB, 0x9C27B0, 0x6A1B9A, 0xFF1744};

	static final Ability[] ABILITIES = {
			// 1 — Хаос
			new Ability("wanda_orb", 20, WandaAbilities::chaosOrb),
			new Ability("wanda_wave", 100, WandaAbilities::chaosWave),
			// 2 — Телекинез
			new Ability("wanda_lift_slam", 80, WandaAbilities::liftSlam),
			new Ability("wanda_mass_throw", 200, WandaAbilities::massThrow),
			// 3 — Разум
			new Ability("wanda_nightmare", 300, WandaAbilities::nightmare),
			new Ability("wanda_puppets", 600, WandaAbilities::puppets),
			// 4 — Хватка
			new Ability("wanda_choke", 120, WandaAbilities::choke),
			new Ability("wanda_barrier", 400, WandaAbilities::barrier),
			// 5 — Реальность
			new Ability("wanda_rewrite", 500, WandaAbilities::rewrite),
			new Ability("wanda_hex", 1200, WandaAbilities::hex),
			// 6 — Левитация
			new Ability("wanda_flight", 10, WandaAbilities::flight),
			new Ability("wanda_gather", 100, WandaAbilities::gather),
			// 7 — Семья
			new Ability("wanda_vision", 1200, WandaAbilities::vision),
			new Ability("wanda_tommy", 600, WandaAbilities::tommy),
			// 8 — Даркхолд
			new Ability("wanda_drain", 300, WandaAbilities::drain),
			new Ability("wanda_runes", 400, WandaAbilities::runeTrap),
			// 9 — Алая Ведьма
			new Ability("wanda_blast", 2400, WandaAbilities::chaosBlast),
			new Ability("wanda_crown", 3000, WandaAbilities::crown),
	};

	private WandaAbilities() {
	}

	/** Сфера хаоса: самонаводящийся сгусток. */
	private static boolean chaosOrb(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.bolt(level, player, Fx.hand(player), player.getViewVector(1f), 1.2, 50, true, CHAOS, CHAOS_LIGHT, (at, hit) -> {
			if (hit != null) {
				Fx.hurt(level, player, hit, 10f);
			}

			level.sendParticles(CHAOS, at.x, at.y, at.z, 20, 0.4, 0.4, 0.4, 0);
			Fx.sound(level, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0f, 0.6f);
		});
		Fx.sound(level, player, SoundEvents.EVOKER_CAST_SPELL, 0.8f, 1.6f);
		return true;
	}

	/** Волна хаоса во все стороны. */
	private static boolean chaosWave(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.nova(level, player, player.position().add(0, 1, 0), 9, 12f, 2.2, 0.6, CHAOS);
		Fx.sound(level, player, SoundEvents.WARDEN_SONIC_BOOM, 1.0f, 1.4f);
		return true;
	}

	/** Телекинез: поднять цель в воздух и швырнуть об землю. */
	private static boolean liftSlam(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Fx.Ray ray = Fx.ray(level, player, 32, 0.4);
		LivingEntity target = ray.entity();

		if (target == null) {
			return Moves.noTarget(player);
		}

		Tasks.start(age -> {
			if (target.isRemoved() || !target.isAlive()) {
				return false;
			}

			if (age < 20) {
				target.setDeltaMovement(0, 0.35, 0);
				target.hurtMarked = true;
				level.sendParticles(CHAOS, target.getX(), target.getY(0.5), target.getZ(), 4, 0.4, 0.5, 0.4, 0);
				return true;
			}

			target.setDeltaMovement(0, -3.5, 0);
			target.hurtMarked = true;
			Fx.hurt(level, player, target, 14f);
			level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY(), target.getZ(), 1, 0, 0, 0, 0);
			Fx.sound(level, target.position(), SoundEvents.MACE_SMASH_GROUND_HEAVY, 1.0f, 1.0f);
			return false;
		});
		Fx.sound(level, player, SoundEvents.EVOKER_PREPARE_ATTACK, 1.0f, 1.4f);
		return true;
	}

	/** Поднять всех вокруг и разбросать. */
	private static boolean massThrow(ServerLevel level, ServerPlayer player, ItemStack stack) {
		List<LivingEntity> targets = Moves.nearby(level, player, 12, false, 20);

		if (targets.isEmpty()) {
			return Moves.noTarget(player);
		}

		Tasks.start(age -> {
			for (LivingEntity e : targets) {
				if (e.isRemoved()) {
					continue;
				}

				if (age < 15) {
					e.setDeltaMovement(0, 0.3, 0);
					e.hurtMarked = true;
					level.sendParticles(CHAOS_LIGHT, e.getX(), e.getY(0.5), e.getZ(), 2, 0.3, 0.4, 0.3, 0);
				} else {
					Vec3 out = e.position().subtract(player.position());
					Fx.push(e, out, 3.0, 0.4);
					Fx.hurt(level, player, e, 8f);
				}
			}

			return age < 15;
		});
		Fx.sound(level, player, SoundEvents.EVOKER_CAST_SPELL, 1.2f, 0.8f);
		return true;
	}

	/** Кошмары: враги в конусе видят свои страхи — темнота, слабость, бегство. */
	private static boolean nightmare(ServerLevel level, ServerPlayer player, ItemStack stack) {
		List<LivingEntity> targets = Fx.cone(level, player, 20, 50);

		if (targets.isEmpty()) {
			return Moves.noTarget(player);
		}

		for (LivingEntity e : targets) {
			e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 3));
			e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2));
			e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0));
			e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));

			if (e instanceof Mob mob) {
				mob.setTarget(null);
				Vec3 away = mob.position().add(mob.position().subtract(player.position()).normalize().scale(10));
				mob.getNavigation().moveTo(away.x, away.y, away.z, 1.4);
			}

			level.sendParticles(CHAOS_LIGHT, e.getX(), e.getEyeY() + 0.3, e.getZ(), 10, 0.3, 0.2, 0.3, 0);
		}

		Fx.sound(level, player, SoundEvents.ELDER_GUARDIAN_CURSE, 0.6f, 1.4f);
		return true;
	}

	/** Марионетки: до 4 мобов в поле зрения на минуту становятся союзниками. */
	private static boolean puppets(ServerLevel level, ServerPlayer player, ItemStack stack) {
		List<LivingEntity> view = Fx.cone(level, player, 24, 30);
		int n = 0;

		for (LivingEntity e : view) {
			if (n >= 4) {
				break;
			}

			if (e instanceof Mob mob && !Fx.isBoss(mob)) {
				MagicState.charm(mob, player, level.getGameTime() + 1200, true);
				mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 1200, 0, false, false, true));
				Fx.line(level, CHAOS_LIGHT, Fx.hand(player), mob.getEyePosition(), 0.4);
				n++;
			}
		}

		if (n == 0) {
			return Moves.noTarget(player);
		}

		Moves.message(player, "message.solorust.hero.puppets", ChatFormatting.RED, n);
		Fx.sound(level, player, SoundEvents.EVOKER_PREPARE_WOLOLO, 1.0f, 1.2f);
		return true;
	}

	/** Удушение хаосом: цель висит в воздухе и теряет здоровье. */
	private static boolean choke(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Fx.Ray ray = Fx.ray(level, player, 24, 0.4);
		LivingEntity target = ray.entity();

		if (target == null) {
			return Moves.noTarget(player);
		}

		Tasks.start(age -> {
			if (target.isRemoved() || !target.isAlive()) {
				return false;
			}

			target.setDeltaMovement(0, age < 10 ? 0.15 : 0.0, 0);
			target.hurtMarked = true;
			target.fallDistance = 0;

			if (age % 2 == 0) {
				Fx.circle(level, CHAOS, target.getEyePosition().subtract(0, 0.2, 0), new Vec3(0, 1, 0), 0.45, 10, age * 0.5);
			}

			if (age % 10 == 0) {
				Fx.hurt(level, player, target, 3f);
			}

			return age < 80;
		});
		Fx.sound(level, player, SoundEvents.EVOKER_CAST_SPELL, 0.8f, 0.6f);
		return true;
	}

	/** Барьер хаоса: красная сфера отражает снаряды, урон снижен. */
	private static boolean barrier(ServerLevel level, ServerPlayer player, ItemStack stack) {
		MagicState.DEFLECT.put(player.getUUID(), level.getGameTime() + 200);
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 2));

		Tasks.start(age -> {
			if (player.isRemoved()) {
				return false;
			}

			ServerLevel lv = player.level();

			for (int i = 0; i < 14; i++) {
				double y = lv.getRandom().nextDouble() * 2 - 1;
				double a = lv.getRandom().nextDouble() * Math.PI * 2;
				double k = Math.sqrt(1 - y * y);
				Vec3 p = player.getBoundingBox().getCenter().add(Math.cos(a) * k * 1.8, y * 1.8, Math.sin(a) * k * 1.8);
				lv.sendParticles(i % 3 == 0 ? CHAOS_LIGHT : CHAOS, p.x, p.y, p.z, 1, 0, 0, 0, 0);
			}

			return age < 200;
		});
		Fx.sound(level, player, SoundEvents.BEACON_ACTIVATE, 1.0f, 1.6f);
		return true;
	}

	/** Переписать реальность: все враги вокруг точки становятся безобидными животными. */
	private static boolean rewrite(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 c = Moves.groundAt(level, player, 48);
		List<EntityType<? extends Mob>> forms = List.of(EntityType.CHICKEN, EntityType.RABBIT, EntityType.SHEEP, EntityType.PIG, EntityType.COW);
		int n = 0;

		for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(c, c).inflate(8), m -> Fx.isTarget(m, player) && Fx.hostile(m) && !Fx.isBoss(m))) {
			forms.get(player.getRandom().nextInt(forms.size())).spawn(level, mob.blockPosition(), EntitySpawnReason.MOB_SUMMONED);
			level.sendParticles(HEX, mob.getX(), mob.getY(0.5), mob.getZ(), 25, 0.4, 0.5, 0.4, 0);
			mob.discard();
			n++;
		}

		Fx.circle(level, HEX, c.add(0, 0.2, 0), new Vec3(0, 1, 0), 8, 60, 0);
		Fx.sound(level, c, SoundEvents.ZOMBIE_VILLAGER_CURE, 1.0f, 1.6f);
		Moves.message(player, "message.solorust.hero.rewrite", ChatFormatting.RED, n);
		return true;
	}

	/** Гекс: 30 секунд в зоне R=16 враги слабеют, а ты становишься сильнее. */
	private static boolean hex(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 c = player.position();
		double r = 16;

		Tasks.start(age -> {
			if (age % 4 == 0) {
				// Стены гекса — красная рамка.
				for (int i = 0; i < 40; i++) {
					double t = level.getRandom().nextDouble() * 2 - 1;
					int side = level.getRandom().nextInt(4);
					double x = side < 2 ? (side == 0 ? -r : r) : t * r;
					double z = side < 2 ? t * r : (side == 2 ? -r : r);
					level.sendParticles(HEX, true, false, c.x + x, c.y + level.getRandom().nextDouble() * 8, c.z + z, 1, 0, 0, 0, 0);
				}
			}

			if (age % 20 == 0) {
				AABB box = new AABB(c, c).inflate(r, 8, r);

				for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> Fx.isTarget(e, player) && Fx.hostile(e))) {
					e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 2));
					e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 2));
				}

				if (!player.isRemoved() && box.contains(player.position())) {
					player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 1));
					player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 40, 1));
				}
			}

			return age < 600;
		});
		Fx.sound(level, player, SoundEvents.BEACON_ACTIVATE, 2.0f, 0.6f);
		return true;
	}

	private static boolean flight(ServerLevel level, ServerPlayer player, ItemStack stack) {
		return StrangeAbilities.toggleFlight(level, player, CHAOS);
	}

	/** Телекинез предметов: всё вокруг летит к тебе. */
	private static boolean gather(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.message(player, "message.solorust.hero.magnet", ChatFormatting.RED, Moves.magnet(level, player, 24, CHAOS_LIGHT));
		return true;
	}

	/** Вижн: призвать железного стража-союзника на 60 секунд. */
	private static boolean vision(ServerLevel level, ServerPlayer player, ItemStack stack) {
		int n = Moves.summon(level, player, EntityType.IRON_GOLEM, 1, 1200, Component.translatable("entity.solorust.vision"), CHAOS);

		for (IronGolem golem : level.getEntitiesOfClass(IronGolem.class, player.getBoundingBox().inflate(5), g -> MagicState.isMinionOf(g, player))) {
			golem.setPlayerCreated(true);
		}

		Fx.sound(level, player, SoundEvents.IRON_GOLEM_ATTACK, 1.0f, 0.8f);
		return n > 0;
	}

	/** Томми: сверхскорость на 15 секунд. */
	private static boolean tommy(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.buff(player, 300, MobEffects.SPEED, 3, MobEffects.JUMP_BOOST, 1, MobEffects.HASTE, 2);
		Moves.aura(player, 300, Fx.dust(0xC0C0C0, 0.8f), 0, 0);
		Fx.sound(level, player, SoundEvents.ELYTRA_FLYING, 0.6f, 2.0f);
		return true;
	}

	/** Даркхолд: вытянуть жизнь из всех вокруг. */
	private static boolean drain(ServerLevel level, ServerPlayer player, ItemStack stack) {
		List<LivingEntity> targets = Moves.nearby(level, player, 12, false, 20);

		if (targets.isEmpty()) {
			return Moves.noTarget(player);
		}

		for (LivingEntity e : targets) {
			Fx.hurt(level, player, e, 5f);
			Fx.line(level, HEX, e.getBoundingBox().getCenter(), player.getBoundingBox().getCenter(), 0.5);
		}

		player.heal(targets.size() * 3f);
		Fx.sound(level, player, SoundEvents.SOUL_ESCAPE.value(), 2.0f, 0.6f);
		return true;
	}

	/** Рунная ловушка: круг на земле сковывает и жжёт врагов 20 секунд. */
	private static boolean runeTrap(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 c = Moves.groundAt(level, player, 32);

		Tasks.start(age -> {
			if (age % 3 == 0) {
				Fx.mandala(level, CHAOS, CHAOS_LIGHT, c.add(0, 0.15, 0), new Vec3(0, 1, 0), 4, age * 0.05);
			}

			if (age % 10 == 0) {
				for (LivingEntity e : Fx.around(level, player, c.add(0, 1, 0), 4.5)) {
					if (e instanceof Player) {
						e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 9));
					} else {
						Freeze.freeze(e, level.getGameTime() + 30);
					}

					Fx.hurt(level, player, e, 2f);
					e.igniteForSeconds(2);
				}
			}

			return age < 400;
		});
		Fx.sound(level, c, SoundEvents.ENCHANTMENT_TABLE_USE, 1.5f, 0.6f);
		return true;
	}

	/** Взрыв хаоса: огромная волна алой энергии (блоки не ломает). */
	private static boolean chaosBlast(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 c = player.position().add(0, 1, 0);
		Moves.nova(level, player, c, 14, 32f, 3.0, 0.9, HEX);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 3, 2, 1, 2, 0);
		level.sendParticles(CHAOS, c.x, c.y, c.z, 200, 6, 3, 6, 0);
		Fx.sound(level, player, SoundEvents.GENERIC_EXPLODE.value(), 2.5f, 0.6f);
		Fx.sound(level, player, SoundEvents.WARDEN_SONIC_BOOM, 2.0f, 0.5f);
		return true;
	}

	/** Корона Алой Ведьмы: 30 секунд полной силы — полёт, защита, жгучая аура. */
	private static boolean crown(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.buff(player, 600, MobEffects.STRENGTH, 2, MobEffects.RESISTANCE, 1, MobEffects.REGENERATION, 1, MobEffects.FIRE_RESISTANCE, 0);
		Moves.aura(player, 600, HEX, 4, 5f);

		if (!MagicState.FLIGHT.contains(player.getUUID()) && !(player.isCreative() || player.isSpectator())) {
			MagicState.toggleFlight(player);
		}

		Fx.sound(level, player, SoundEvents.BEACON_ACTIVATE, 1.5f, 0.8f);
		Moves.message(player, "message.solorust.hero.crown", ChatFormatting.RED);
		return true;
	}

	static boolean isPlayer(LivingEntity e) {
		return e instanceof Player;
	}
}
