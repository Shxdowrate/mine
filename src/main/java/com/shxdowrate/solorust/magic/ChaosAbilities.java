package com.shxdowrate.solorust.magic;

import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Сердце Обители Хаоса — артефакт из мира, где законы природы бросают кости.
 * Разломы, энтропия, искажённая гравитация, бесы и случайность как оружие.
 */
final class ChaosAbilities {
	private static final DustParticleOptions VOID = Fx.dust(0x1A0633, 1.8f);
	private static final DustParticleOptions MAGENTA = Fx.dust(0xF72585, 1.2f);
	private static final DustParticleOptions VIOLET = Fx.dust(0x7209B7, 1.2f);
	private static final DustParticleOptions TOXIC = Fx.dust(0x7CFC00, 1.0f);
	private static final int[] CHAOS_COLORS = {0xF72585, 0x7209B7, 0x3A0CA3, 0x4CC9F0, 0x7CFC00, 0xFFBE0B};

	static final int[] COLORS = {0xF72585, 0x7CFC00, 0x7209B7, 0x4CC9F0, 0xB5179E, 0x3A0CA3, 0xFFBE0B, 0x560BAD, 0xFF006E};

	/** Ступени распада блоков для «Энтропии материи». */
	private static final Map<Block, Block> DECAY = Map.ofEntries(
			Map.entry(Blocks.STONE, Blocks.COBBLESTONE), Map.entry(Blocks.COBBLESTONE, Blocks.GRAVEL), Map.entry(Blocks.GRAVEL, Blocks.SAND),
			Map.entry(Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE), Map.entry(Blocks.COBBLED_DEEPSLATE, Blocks.GRAVEL),
			Map.entry(Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS), Map.entry(Blocks.GRASS_BLOCK, Blocks.DIRT),
			Map.entry(Blocks.DIRT, Blocks.COARSE_DIRT), Map.entry(Blocks.SANDSTONE, Blocks.SAND), Map.entry(Blocks.NETHERRACK, Blocks.SOUL_SAND),
			Map.entry(Blocks.ANDESITE, Blocks.GRAVEL), Map.entry(Blocks.DIORITE, Blocks.GRAVEL), Map.entry(Blocks.GRANITE, Blocks.GRAVEL));

	static final Ability[] ABILITIES = {
			// 1 — Разломы
			new Ability("chaos_rift", 25, ChaosAbilities::rift),
			new Ability("chaos_dice", 200, ChaosAbilities::dice),
			// 2 — Энтропия
			new Ability("chaos_entropy", 100, ChaosAbilities::entropy),
			new Ability("chaos_decay", 60, ChaosAbilities::decay),
			// 3 — Бездна
			new Ability("chaos_tentacles", 200, ChaosAbilities::tentacles),
			new Ability("chaos_gate", 600, ChaosAbilities::gate),
			// 4 — Гравитация
			new Ability("chaos_upfall", 200, ChaosAbilities::upfall),
			new Ability("chaos_crush", 200, ChaosAbilities::crush),
			// 5 — Бесы
			new Ability("chaos_imps", 900, ChaosAbilities::imps),
			new Ability("chaos_swap", 60, ChaosAbilities::swap),
			// 6 — Тьма и безумие
			new Ability("chaos_eclipse", 600, ChaosAbilities::eclipse),
			new Ability("chaos_madness", 400, ChaosAbilities::madness),
			// 7 — Шторм
			new Ability("chaos_storm", 600, ChaosAbilities::storm),
			new Ability("chaos_shield", 600, ChaosAbilities::shield),
			// 8 — Сбой реальности
			new Ability("chaos_glitch", 120, ChaosAbilities::glitch),
			new Ability("chaos_flight", 10, ChaosAbilities::flight),
			// 9 — Обитель
			new Ability("chaos_call", 3000, ChaosAbilities::callOfTheAbode),
			new Ability("chaos_avatar", 3000, ChaosAbilities::avatar),
	};

	private ChaosAbilities() {
	}

	private static DustParticleOptions randomColor(RandomSource random) {
		return Fx.dust(CHAOS_COLORS[random.nextInt(CHAOS_COLORS.length)], 1.2f);
	}

	/** Разлом: луч непредсказуемой силы (8–20 урона), насквозь. */
	private static boolean rift(ServerLevel level, ServerPlayer player, ItemStack stack) {
		RandomSource random = player.getRandom();
		float damage = 8 + random.nextInt(13);
		Moves.beam(level, player, 48, damage, true, randomColor(random), VOID, Fx.hand(player));
		Fx.sound(level, player, SoundEvents.ENDERMAN_SCREAM, 0.5f, 0.5f + random.nextFloat());
		return true;
	}

	/** Кость хаоса: на всех врагов вокруг падает одна из шести судеб. */
	private static boolean dice(ServerLevel level, ServerPlayer player, ItemStack stack) {
		List<LivingEntity> targets = Moves.nearby(level, player, 12, false, 20);

		if (targets.isEmpty()) {
			return Moves.noTarget(player);
		}

		int face = player.getRandom().nextInt(6);

		for (LivingEntity e : targets) {
			switch (face) {
				case 0 -> e.igniteForSeconds(8);
				case 1 -> e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 80, 3));
				case 2 -> {
					if (e instanceof Player) {
						e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 9));
					} else {
						Freeze.freeze(e, level.getGameTime() + 120);
					}
				}
				case 3 -> e.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 2));
				case 4 -> Moves.lightning(level, player, e.position(), 12f, 1.5);
				default -> {
					if (e instanceof Mob mob && !Fx.isBoss(mob) && Fx.hostile(mob)) {
						EntityType.CHICKEN.spawn(level, mob.blockPosition(), EntitySpawnReason.MOB_SUMMONED);
						mob.discard();
					} else {
						Fx.hurt(level, player, e, 10f);
					}
				}
			}

			level.sendParticles(Fx.dust(CHAOS_COLORS[face], 1.4f), e.getX(), e.getY(1), e.getZ(), 15, 0.3, 0.4, 0.3, 0);
		}

		Moves.message(player, "message.solorust.hero.dice_" + (face + 1), ChatFormatting.LIGHT_PURPLE);
		Fx.sound(level, player, SoundEvents.AMETHYST_BLOCK_HIT, 1.5f, 0.5f + face * 0.2f);
		return true;
	}

	/** Энтропия: цель гниёт заживо. */
	private static boolean entropy(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Fx.Ray ray = Fx.ray(level, player, 40, 0.4);

		if (ray.entity() == null) {
			return Moves.noTarget(player);
		}

		LivingEntity e = ray.entity();
		Fx.hurt(level, player, e, 6f);
		e.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 2));
		e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
		e.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 2));
		Fx.line(level, TOXIC, Fx.hand(player), e.getBoundingBox().getCenter(), 0.35);
		level.sendParticles(ParticleTypes.ASH, e.getX(), e.getY(0.6), e.getZ(), 30, 0.4, 0.5, 0.4, 0.02);
		Fx.sound(level, player, SoundEvents.SCULK_CATALYST_BLOOM, 1.5f, 0.6f);
		return true;
	}

	/** Энтропия материи: блоки вокруг точки рассыпаются на ступень (камень → булыжник → гравий → песок). */
	private static boolean decay(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Fx.Ray ray = Fx.ray(level, player, 32, 0.1);

		if (ray.block().getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
			return Moves.noTarget(player);
		}

		BlockPos center = ray.block().getBlockPos();
		int n = 0;

		for (BlockPos p : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) {
			BlockState state = level.getBlockState(p);
			Block next = DECAY.get(state.getBlock());

			if (next != null && p.distSqr(center) <= 6.5 && player.mayBuild() && level.mayInteract(player, p)) {
				level.setBlock(p, next.defaultBlockState(), Block.UPDATE_ALL);

				if (n++ % 3 == 0) {
					level.sendParticles(TOXIC, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0);
				}
			}
		}

		Fx.sound(level, Vec3.atCenterOf(center), SoundEvents.SCULK_CATALYST_BLOOM, 1.5f, 0.8f);
		return n > 0 || Moves.noTarget(player);
	}

	/** Щупальца Бездны: из разлома в земле тянутся щупальца и затягивают всех в центр. */
	private static boolean tentacles(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 c = Moves.groundAt(level, player, 32);

		Tasks.start(age -> {
			if (age % 2 == 0) {
				Fx.circle(level, VOID, c.add(0, 0.1, 0), new Vec3(0, 1, 0), 1.5, 16, age * 0.2);

				for (int i = 0; i < 5; i++) {
					double a = i * Math.PI * 2 / 5 + age * 0.1;
					double h = 1.5 + Math.sin(age * 0.3 + i) * 1.0;
					Vec3 tip = c.add(Math.cos(a) * 2.5, h, Math.sin(a) * 2.5);
					Fx.line(level, i % 2 == 0 ? VIOLET : VOID, c, tip, 0.3);
				}
			}

			for (LivingEntity e : Fx.around(level, player, c.add(0, 1, 0), 7)) {
				Vec3 to = c.subtract(e.position());
				e.setDeltaMovement(e.getDeltaMovement().scale(0.5).add(to.normalize().scale(0.22)));
				e.hurtMarked = true;

				if (age % 10 == 0) {
					Fx.hurt(level, player, e, 4f);
				}
			}

			return age < 100;
		});
		Fx.sound(level, c, SoundEvents.SCULK_SHRIEKER_SHRIEK, 1.0f, 0.5f);
		return true;
	}

	/** Врата Обители: шагнуть в разлом и выйти в случайном месте в 100–400 блоках. */
	private static boolean gate(ServerLevel level, ServerPlayer player, ItemStack stack) {
		RandomSource random = player.getRandom();
		double a = random.nextDouble() * Math.PI * 2;
		double d = 100 + random.nextDouble() * 300;
		double x = player.getX() + Math.cos(a) * d;
		double z = player.getZ() + Math.sin(a) * d;

		if (level.dimension() != Level.OVERWORLD) {
			Moves.message(player, "message.solorust.hero.gate_overworld", ChatFormatting.GRAY);
			return false;
		}

		level.getChunk(BlockPos.containing(x, 0, z));
		Vec3 dest = Moves.surface(level, x, z);
		level.sendParticles(VOID, player.getX(), player.getY(1), player.getZ(), 60, 0.5, 1, 0.5, 0);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY(1), player.getZ(), 60, 0.5, 1, 0.5, 0.2);
		player.teleportTo(dest.x, dest.y, dest.z);
		player.fallDistance = 0;
		level.sendParticles(MAGENTA, dest.x, dest.y + 1, dest.z, 60, 0.5, 1, 0.5, 0);
		Fx.sound(level, player, SoundEvents.END_PORTAL_SPAWN, 0.6f, 1.6f);
		Moves.message(player, "message.solorust.hero.gate", ChatFormatting.LIGHT_PURPLE, (int) d);
		return true;
	}

	/** Обратная гравитация: всех вокруг уносит вверх, а потом они падают. */
	private static boolean upfall(ServerLevel level, ServerPlayer player, ItemStack stack) {
		List<LivingEntity> targets = Moves.nearby(level, player, 14, false, 30);

		if (targets.isEmpty()) {
			return Moves.noTarget(player);
		}

		for (LivingEntity e : targets) {
			e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 50, 6));
			level.sendParticles(VIOLET, e.getX(), e.getY(), e.getZ(), 10, 0.3, 0.1, 0.3, 0);
		}

		Fx.sound(level, player, SoundEvents.BEACON_DEACTIVATE, 1.5f, 1.8f);
		return true;
	}

	/** Гравитационный пресс: всех вокруг вжимает в землю. */
	private static boolean crush(ServerLevel level, ServerPlayer player, ItemStack stack) {
		List<LivingEntity> targets = Moves.nearby(level, player, 14, false, 30);

		if (targets.isEmpty()) {
			return Moves.noTarget(player);
		}

		for (LivingEntity e : targets) {
			e.setDeltaMovement(0, -3, 0);
			e.hurtMarked = true;
			e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 160, 4));
			Fx.hurt(level, player, e, 8f);
			level.sendParticles(VOID, e.getX(), e.getY(1.5), e.getZ(), 12, 0.3, 0.2, 0.3, 0);
		}

		Fx.sound(level, player, SoundEvents.ANVIL_LAND, 1.5f, 0.4f);
		return true;
	}

	/** Бесы хаоса: три летучих беса сражаются за тебя 30 секунд. */
	private static boolean imps(ServerLevel level, ServerPlayer player, ItemStack stack) {
		int n = Moves.summon(level, player, EntityType.VEX, 3, 600, Component.translatable("entity.solorust.chaos_imp"), MAGENTA);
		Fx.sound(level, player, SoundEvents.VEX_CHARGE, 1.5f, 0.6f);
		return n > 0;
	}

	/** Обмен: меняешься местами с целью. */
	private static boolean swap(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Fx.Ray ray = Fx.ray(level, player, 48, 0.4);
		LivingEntity target = ray.entity();

		if (target == null) {
			return Moves.noTarget(player);
		}

		Vec3 mine = player.position();
		Vec3 theirs = target.position();
		Fx.line(level, MAGENTA, mine.add(0, 1, 0), theirs.add(0, 1, 0), 0.4);
		player.teleportTo(theirs.x, theirs.y, theirs.z);
		target.teleportTo(mine.x, mine.y, mine.z);
		player.fallDistance = 0;
		Fx.sound(level, player, SoundEvents.ENDERMAN_TELEPORT, 1.0f, 0.5f);
		return true;
	}

	/** Затмение: 15 секунд вокруг тьма — враги слепнут, ты невидим и видишь всё. */
	private static boolean eclipse(ServerLevel level, ServerPlayer player, ItemStack stack) {
		player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 300, 0, false, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false, true));
		MagicState.STEALTH.put(player.getUUID(), level.getGameTime() + 300);

		Tasks.start(age -> {
			if (player.isRemoved()) {
				return false;
			}

			ServerLevel lv = player.level();

			if (age % 20 == 0) {
				for (LivingEntity e : Moves.nearby(lv, player, 16, false, 40)) {
					e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
					e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0));
				}
			}

			if (age % 4 == 0) {
				lv.sendParticles(VOID, player.getX(), player.getY(1), player.getZ(), 12, 6, 2, 6, 0);
			}

			return age < 300;
		});
		Fx.sound(level, player, SoundEvents.WARDEN_HEARTBEAT, 1.5f, 0.5f);
		return true;
	}

	/** Безумие: все мобы вокруг 20 секунд дерутся друг с другом. */
	private static boolean madness(ServerLevel level, ServerPlayer player, ItemStack stack) {
		List<Mob> mobs = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(18), m -> Fx.isTarget(m, player) && !Fx.isBoss(m));

		if (mobs.size() < 2) {
			return Moves.noTarget(player);
		}

		for (Mob mob : mobs) {
			MagicState.charm(mob, player, level.getGameTime() + 400, false);
			level.sendParticles(MAGENTA, mob.getX(), mob.getY(1.1), mob.getZ(), 12, 0.3, 0.3, 0.3, 0);
		}

		Fx.sound(level, player, SoundEvents.EVOKER_PREPARE_WOLOLO, 1.5f, 0.5f);
		Moves.message(player, "message.solorust.magic.discord", ChatFormatting.LIGHT_PURPLE, mobs.size());
		return true;
	}

	/** Хаос-шторм: 8 секунд над точкой бушует хаос — молнии, огонь, взрывы (блоки целы). */
	private static boolean storm(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 c = Moves.groundAt(level, player, 64);

		Tasks.start(age -> {
			RandomSource random = level.getRandom();

			if (age % 2 == 0) {
				Vec3 p = c.add((random.nextDouble() - 0.5) * 14, 6 + random.nextDouble() * 3, (random.nextDouble() - 0.5) * 14);
				level.sendParticles(randomColor(random), true, false, p.x, p.y, p.z, 8, 1, 0.3, 1, 0);
			}

			if (age % 8 == 0) {
				Vec3 hit = c.add((random.nextDouble() - 0.5) * 12, 0, (random.nextDouble() - 0.5) * 12);

				switch (random.nextInt(3)) {
					case 0 -> Moves.lightning(level, player, hit, 9f, 2.5);
					case 1 -> {
						MagicState.explodingCaster = player;

						try {
							level.explode(player, hit.x, hit.y + 0.5, hit.z, 2.0f, Level.ExplosionInteraction.NONE);
						} finally {
							MagicState.explodingCaster = null;
						}
					}
					default -> {
						level.sendParticles(ParticleTypes.FLAME, hit.x, hit.y + 0.5, hit.z, 30, 1, 0.5, 1, 0.05);

						for (LivingEntity e : Fx.around(level, player, hit.add(0, 1, 0), 2.5)) {
							Fx.hurt(level, player, e, 5f);
							e.igniteForSeconds(5);
						}
					}
				}
			}

			return age < 160;
		});
		Fx.sound(level, c, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.5f, 0.6f);
		return true;
	}

	/** Щит хаоса: 30 секунд большая часть ударов исчезает и бьёт обидчика. */
	private static boolean shield(ServerLevel level, ServerPlayer player, ItemStack stack) {
		MagicState.CHAOS_SHIELD.put(player.getUUID(), level.getGameTime() + 600);
		Moves.aura(player, 600, MAGENTA, 0, 0);
		Fx.sound(level, player, SoundEvents.BEACON_ACTIVATE, 1.0f, 0.6f);
		return true;
	}

	/** Сбой реальности: цель трижды выбрасывает в случайные точки рядом, каждый раз раня. */
	private static boolean glitch(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Fx.Ray ray = Fx.ray(level, player, 40, 0.4);
		LivingEntity target = ray.entity();

		if (target == null) {
			return Moves.noTarget(player);
		}

		Tasks.start(age -> {
			if (target.isRemoved() || !target.isAlive()) {
				return false;
			}

			if (age % 6 == 0) {
				RandomSource random = level.getRandom();
				Vec3 from = target.position();
				Vec3 to = Moves.surface(level, from.x + (random.nextDouble() - 0.5) * 14, from.z + (random.nextDouble() - 0.5) * 14);

				if (Math.abs(to.y - from.y) < 10) {
					target.teleportTo(to.x, to.y, to.z);
				}

				Fx.hurt(level, player, target, 5f);
				level.sendParticles(randomColor(random), from.x, from.y + 1, from.z, 20, 0.3, 0.6, 0.3, 0);
				Fx.sound(level, from, SoundEvents.ENDERMAN_TELEPORT, 0.6f, 2.0f);
			}

			return age < 17;
		});
		return true;
	}

	private static boolean flight(ServerLevel level, ServerPlayer player, ItemStack stack) {
		return StrangeAbilities.toggleFlight(level, player, MAGENTA);
	}

	/** Зов Обители: над тобой раскрывается разлом и бьёт лучами в каждого врага. */
	private static boolean callOfTheAbode(ServerLevel level, ServerPlayer player, ItemStack stack) {
		List<LivingEntity> targets = Moves.nearby(level, player, 28, true, 30);

		if (targets.isEmpty()) {
			return Moves.noTarget(player);
		}

		Vec3 sky = player.position().add(0, 12, 0);
		Fx.sound(level, player, SoundEvents.WITHER_SPAWN, 1.0f, 1.2f);

		Tasks.start(age -> {
			RandomSource random = level.getRandom();

			if (age % 2 == 0) {
				Fx.circle(level, VOID, sky, new Vec3(0, 1, 0), Math.min(6, 1 + age * 0.3), 40, age * 0.1);
				Fx.circle(level, MAGENTA, sky, new Vec3(0, 1, 0), Math.min(4, 0.5 + age * 0.2), 24, -age * 0.15);
			}

			int index = (age - 20) / 3;

			if (age >= 20 && (age - 20) % 3 == 0 && index < targets.size()) {
				LivingEntity e = targets.get(index);

				if (e.isAlive()) {
					Vec3 at = e.getBoundingBox().getCenter();
					Fx.line(level, randomColor(random), sky, at, 0.4);
					Fx.line(level, VOID, sky, at, 1.0);
					Fx.hurt(level, player, e, 20f);

					switch (random.nextInt(3)) {
						case 0 -> e.igniteForSeconds(5);
						case 1 -> e.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
						default -> e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 2));
					}
				}
			}

			return age < 20 + targets.size() * 3 + 20;
		});
		return true;
	}

	/** Аватар Хаоса: 30 секунд силы, которая каждые 5 секунд меняется случайным образом. */
	private static boolean avatar(ServerLevel level, ServerPlayer player, ItemStack stack) {
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 600, 1));
		Moves.aura(player, 600, MAGENTA, 4, 4f);

		Tasks.start(age -> {
			if (player.isRemoved() || !player.isAlive()) {
				return false;
			}

			if (age % 100 == 0) {
				RandomSource random = player.getRandom();
				var pool = List.of(MobEffects.STRENGTH, MobEffects.SPEED, MobEffects.REGENERATION, MobEffects.JUMP_BOOST, MobEffects.HASTE, MobEffects.ABSORPTION);

				for (int i = 0; i < 3; i++) {
					player.addEffect(new MobEffectInstance(pool.get(random.nextInt(pool.size())), 110, 1 + random.nextInt(3)));
				}

				player.level().sendParticles(randomColor(random), player.getX(), player.getY(1), player.getZ(), 40, 0.5, 0.9, 0.5, 0);
				Fx.sound(player.level(), player, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 0.5f + random.nextFloat());
			}

			return age < 600;
		});
		Moves.message(player, "message.solorust.hero.chaos_avatar", ChatFormatting.LIGHT_PURPLE);
		return true;
	}
}
