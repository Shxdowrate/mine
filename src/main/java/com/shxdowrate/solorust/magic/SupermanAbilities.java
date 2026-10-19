package com.shxdowrate.solorust.magic;

import java.util.LinkedHashMap;
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
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Символ Супермена: 18 способностей Человека из стали. */
final class SupermanAbilities {
	private static final DustParticleOptions HEAT = Fx.dust(0xFF2200, 0.7f);
	private static final DustParticleOptions HEAT_CORE = Fx.dust(0xFFB199, 0.5f);
	private static final DustParticleOptions BLUE = Fx.dust(0x1E4FCB, 1.3f);
	private static final DustParticleOptions RED = Fx.dust(0xD32F2F, 1.3f);
	private static final DustParticleOptions SUN = Fx.dust(0xFFE082, 1.4f);
	private static final DustParticleOptions FROST = Fx.dust(0xB3E5FC, 1.0f);

	static final int[] COLORS = {0xFF2200, 0xB3E5FC, 0x1E4FCB, 0x80DEEA, 0xD32F2F, 0xE0E0E0, 0xFF6F00, 0xFFE082, 0xFFD600};

	private static final List<TagKey<Block>> ORE_TAGS = List.of(BlockTags.DIAMOND_ORES, BlockTags.EMERALD_ORES, BlockTags.GOLD_ORES,
			BlockTags.IRON_ORES, BlockTags.REDSTONE_ORES, BlockTags.LAPIS_ORES, BlockTags.COPPER_ORES, BlockTags.COAL_ORES);

	static final Ability[] ABILITIES = {
			// 1 — Тепловое зрение
			new Ability("superman_heat", 10, SupermanAbilities::heatVision),
			new Ability("superman_heat_burn", 200, SupermanAbilities::heatBurn),
			// 2 — Дыхание
			new Ability("superman_freeze", 120, SupermanAbilities::freezeBreath),
			new Ability("superman_punch", 40, SupermanAbilities::punch),
			// 3 — Полёт
			new Ability("superman_flight", 10, SupermanAbilities::flight),
			new Ability("superman_supersonic", 40, SupermanAbilities::supersonic),
			// 4 — Зрение и кожа
			new Ability("superman_xray", 600, SupermanAbilities::xray),
			new Ability("superman_steel", 900, SupermanAbilities::steel),
			// 5 — Хлопок и ветер
			new Ability("superman_clap", 80, SupermanAbilities::clap),
			new Ability("superman_gale", 100, SupermanAbilities::gale),
			// 6 — Сила
			new Ability("superman_launch", 80, SupermanAbilities::launch),
			new Ability("superman_carry", 100, SupermanAbilities::carry),
			// 7 — Солнце
			new Ability("superman_meteor", 200, SupermanAbilities::meteorDive),
			new Ability("superman_solar", 1200, SupermanAbilities::solarCharge),
			// 8 — Скорость и жар
			new Ability("superman_heat_fan", 160, SupermanAbilities::heatFan),
			new Ability("superman_speed", 600, SupermanAbilities::superSpeed),
			// 9 — Криптон
			new Ability("superman_solar_flare", 3600, SupermanAbilities::solarFlare),
			new Ability("superman_krypton", 3000, SupermanAbilities::lastSon),
	};

	private SupermanAbilities() {
	}

	/** Два луча из глаз. */
	private static void eyes(ServerLevel level, ServerPlayer player, Vec3 end) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1f);
		Vec3 side = new Vec3(-look.z, 0, look.x);
		side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();

		for (int s = -1; s <= 1; s += 2) {
			Vec3 from = eye.add(look.scale(0.3)).add(side.scale(0.1 * s)).add(0, -0.05, 0);
			Fx.line(level, HEAT, from, end, 0.25);
			Fx.line(level, HEAT_CORE, from, end, 0.6);
		}
	}

	private static boolean heatVision(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Fx.Ray ray = Fx.ray(level, player, 48, 0.2);
		eyes(level, player, ray.end());

		if (ray.entity() != null) {
			Fx.hurt(level, player, ray.entity(), 7f);
			ray.entity().igniteForSeconds(4);
		}

		level.sendParticles(ParticleTypes.SMALL_FLAME, ray.end().x, ray.end().y, ray.end().z, 4, 0.05, 0.05, 0.05, 0.01);
		Fx.sound(level, player, SoundEvents.BEACON_AMBIENT, 0.6f, 2.0f);
		return true;
	}

	/** Прожиг: 3 секунды непрерывного теплового луча туда, куда смотришь. */
	private static boolean heatBurn(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Tasks.start(age -> {
			if (player.isRemoved() || !player.isAlive()) {
				return false;
			}

			ServerLevel lv = player.level();
			Fx.Ray ray = Fx.ray(lv, player, 48, 0.2);
			eyes(lv, player, ray.end());

			if (ray.entity() != null && age % 2 == 0) {
				Fx.hurt(lv, player, ray.entity(), 3f);
				ray.entity().igniteForSeconds(5);
			}

			if (age % 3 == 0) {
				lv.sendParticles(ParticleTypes.SMALL_FLAME, ray.end().x, ray.end().y, ray.end().z, 3, 0.05, 0.05, 0.05, 0.01);
			}

			if (age % 8 == 0) {
				Fx.sound(lv, player, SoundEvents.BEACON_AMBIENT, 0.8f, 1.8f);
			}

			return age < 60;
		});
		return true;
	}

	/** Ледяное дыхание: замораживает врагов, вода — в лёд, лава — в обсидиан. */
	private static boolean freezeBreath(ServerLevel level, ServerPlayer player, ItemStack stack) {
		for (LivingEntity e : Fx.cone(level, player, 12, 30)) {
			e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 160, 4));
			e.setTicksFrozen(400);
			e.clearFire();
			Fx.hurt(level, player, e, 4f);
		}

		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1f);

		for (int d = 2; d <= 12; d++) {
			BlockPos center = BlockPos.containing(eye.add(look.scale(d)));
			int r = d / 5;

			for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r - 1, -r), center.offset(r, r, r))) {
				BlockState state = level.getBlockState(pos);

				if (!player.mayBuild() || !level.mayInteract(player, pos)) {
					continue;
				}

				if (state.is(Blocks.WATER) && state.getFluidState().isSource()) {
					level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
				} else if (state.is(Blocks.LAVA)) {
					level.setBlockAndUpdate(pos, state.getFluidState().isSource() ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
				} else if (state.is(BlockTags.FIRE)) {
					level.removeBlock(pos, false);
				}
			}
		}

		Moves.cone(level, player, 12, 30, 0, 0, 0, ParticleTypes.SNOWFLAKE);
		Fx.sound(level, player, SoundEvents.PLAYER_HURT_FREEZE, 1.0f, 0.6f);
		Fx.sound(level, player, SoundEvents.POWDER_SNOW_STEP, 1.5f, 0.6f);
		return true;
	}

	private static boolean punch(ServerLevel level, ServerPlayer player, ItemStack stack) {
		return Moves.punch(level, player, 6, 24f, 4.0, 0.7, RED);
	}

	private static boolean flight(ServerLevel level, ServerPlayer player, ItemStack stack) {
		return StrangeAbilities.toggleFlight(level, player, RED);
	}

	/** Сверхзвуковой рывок. */
	private static boolean supersonic(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.dash(level, player, 3.6, 0.2, 10f, 12, BLUE);
		Vec3 p = player.getEyePosition();
		level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		Fx.sound(level, player, SoundEvents.WARDEN_SONIC_BOOM, 1.0f, 1.6f);
		return true;
	}

	/** Рентгеновское зрение: видно всех сквозь стены и руду вокруг. */
	private static boolean xray(ServerLevel level, ServerPlayer player, ItemStack stack) {
		int mobs = Moves.reveal(level, player, 64, 600);
		BlockPos center = player.blockPosition();
		Map<Block, int[]> found = new LinkedHashMap<>();
		java.util.List<BlockPos> marks = new java.util.ArrayList<>();
		int r = 14;

		for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
			BlockState state = level.getBlockState(p);
			boolean ore = state.is(Blocks.ANCIENT_DEBRIS);

			for (int i = 0; !ore && i < ORE_TAGS.size(); i++) {
				ore = state.is(ORE_TAGS.get(i));
			}

			if (ore) {
				int[] info = found.computeIfAbsent(state.getBlock(), k -> new int[] {0, Integer.MAX_VALUE});
				info[0]++;
				info[1] = Math.min(info[1], (int) Math.round(Math.sqrt(p.distSqr(center))));

				if (marks.size() < 200 && !state.is(BlockTags.COAL_ORES)) {
					marks.add(p.immutable());
				}
			}
		}

		Tasks.start(age -> {
			if (age % 10 == 0) {
				for (BlockPos p : marks) {
					player.level().sendParticles(player, ParticleTypes.END_ROD, true, false, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 1, 0.2, 0.2, 0.2, 0);
				}
			}

			return age < 300;
		});

		player.sendSystemMessage(Component.translatable("message.solorust.hero.xray", mobs).withStyle(ChatFormatting.AQUA));
		found.entrySet().stream().sorted(java.util.Comparator.comparingInt(e -> e.getValue()[1])).limit(8)
				.forEach(e -> player.sendSystemMessage(Component.literal(" • ").append(e.getKey().getName())
						.append(Component.translatable("message.solorust.hero.ore_line", e.getValue()[0], e.getValue()[1])).withStyle(ChatFormatting.YELLOW)));
		Fx.sound(level, player, SoundEvents.BEACON_POWER_SELECT, 1.0f, 2.0f);
		return true;
	}

	/** Стальная кожа: почти неуязвим 15 секунд. */
	private static boolean steel(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.buff(player, 300, MobEffects.RESISTANCE, 3, MobEffects.FIRE_RESISTANCE, 0);
		MagicState.DEFLECT.put(player.getUUID(), level.getGameTime() + 300);
		Moves.aura(player, 300, BLUE, 0, 0);
		Fx.sound(level, player, SoundEvents.ANVIL_LAND, 0.8f, 1.6f);
		return true;
	}

	/** Хлопок: ударная волна в конусе. */
	private static boolean clap(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.cone(level, player, 14, 40, 9f, 4.0, 0.6, ParticleTypes.CLOUD);
		Vec3 p = player.getEyePosition().add(player.getViewVector(1f).scale(2));
		level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		Fx.sound(level, player, SoundEvents.WARDEN_SONIC_BOOM, 1.2f, 1.4f);
		return true;
	}

	/** Ураганное дыхание: сдувает всех и тушит огонь. */
	private static boolean gale(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.cone(level, player, 18, 35, 2f, 5.0, 0.8, ParticleTypes.CLOUD);
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1f);

		for (int d = 2; d <= 16; d++) {
			BlockPos c = BlockPos.containing(eye.add(look.scale(d)));

			for (BlockPos pos : BlockPos.betweenClosed(c.offset(-2, -2, -2), c.offset(2, 2, 2))) {
				if (level.getBlockState(pos).is(BlockTags.FIRE)) {
					level.removeBlock(pos, false);
				}
			}
		}

		player.clearFire();
		Fx.sound(level, player, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.5f, 0.5f);
		return true;
	}

	/** Подбросить цель высоко в небо. */
	private static boolean launch(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Fx.Ray ray = Fx.ray(level, player, 6, 0.6);

		if (ray.entity() == null) {
			return Moves.noTarget(player);
		}

		LivingEntity e = ray.entity();
		Fx.hurt(level, player, e, 10f);
		e.setDeltaMovement(player.getViewVector(1f).scale(0.5).add(0, 3.2, 0));
		e.hurtMarked = true;
		level.sendParticles(ParticleTypes.EXPLOSION, e.getX(), e.getY(), e.getZ(), 1, 0, 0, 0, 0);
		Fx.sound(level, player, SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.5f);
		return true;
	}

	private static boolean carry(ServerLevel level, ServerPlayer player, ItemStack stack) {
		return Moves.grabAndThrow(level, player, 8, 100, 3.5, 10f, BLUE);
	}

	/** Метеор: прыжок и пике вниз с огромным ударом. */
	private static boolean meteorDive(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.leapSlam(level, player, 1.4, 1.5, 9, 22f, RED, null);
		Fx.sound(level, player, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0f, 0.6f);
		return true;
	}

	/** Солнечная подзарядка: днём под открытым небом — полное восстановление и сила. */
	private static boolean solarCharge(ServerLevel level, ServerPlayer player, ItemStack stack) {
		boolean sun = level.isBrightOutside() && level.canSeeSky(player.blockPosition().above());
		Moves.heal(level, player, sun ? player.getMaxHealth() : player.getMaxHealth() * 0.4f, SUN);

		if (sun) {
			Moves.buff(player, 600, MobEffects.STRENGTH, 1, MobEffects.ABSORPTION, 3, MobEffects.REGENERATION, 0);
		}

		Fx.line(level, SUN, player.position().add(0, 20, 0), player.position().add(0, 1, 0), 0.4);
		Moves.message(player, sun ? "message.solorust.hero.solar_full" : "message.solorust.hero.solar_weak", sun ? ChatFormatting.GOLD : ChatFormatting.GRAY);
		Fx.sound(level, player, SoundEvents.BEACON_ACTIVATE, 1.0f, 1.4f);
		return true;
	}

	/** Тепловой веер: лучи веером по всем врагам впереди. */
	private static boolean heatFan(ServerLevel level, ServerPlayer player, ItemStack stack) {
		List<LivingEntity> targets = Fx.cone(level, player, 32, 50);

		if (targets.isEmpty()) {
			return Moves.noTarget(player);
		}

		for (LivingEntity e : targets) {
			eyes(level, player, e.getBoundingBox().getCenter());
			Fx.hurt(level, player, e, 10f);
			e.igniteForSeconds(6);
		}

		Fx.sound(level, player, SoundEvents.BEACON_AMBIENT, 1.0f, 1.6f);
		return true;
	}

	private static boolean superSpeed(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.buff(player, 400, MobEffects.SPEED, 4, MobEffects.HASTE, 3, MobEffects.JUMP_BOOST, 1);
		Moves.aura(player, 400, BLUE, 0, 0);
		Fx.sound(level, player, SoundEvents.ELYTRA_FLYING, 0.6f, 1.8f);
		return true;
	}

	/** Солнечная вспышка: вся накопленная энергия Солнца разом. После — слабость. */
	private static boolean solarFlare(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 c = player.position().add(0, 1, 0);
		Moves.nova(level, player, c, 16, 36f, 3.5, 1.0, SUN);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 4, 3, 1, 3, 0);
		level.sendParticles(SUN, c.x, c.y, c.z, 250, 7, 3, 7, 0);
		level.sendParticles(ParticleTypes.FLAME, c.x, c.y, c.z, 120, 6, 3, 6, 0.2);
		Fx.sound(level, player, SoundEvents.GENERIC_EXPLODE.value(), 3.0f, 0.5f);
		Fx.sound(level, player, SoundEvents.BEACON_DEACTIVATE, 2.0f, 0.5f);

		if (!player.hasInfiniteMaterials()) {
			Moves.buff(player, 600, MobEffects.WEAKNESS, 1, MobEffects.SLOWNESS, 0);
		}

		return true;
	}

	/** Последний сын Криптона: 30 секунд полной мощи. */
	private static boolean lastSon(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Moves.buff(player, 600, MobEffects.STRENGTH, 4, MobEffects.RESISTANCE, 3, MobEffects.REGENERATION, 1,
				MobEffects.FIRE_RESISTANCE, 0, MobEffects.SPEED, 2, MobEffects.NIGHT_VISION, 0);
		MagicState.DEFLECT.put(player.getUUID(), level.getGameTime() + 600);

		if (!MagicState.FLIGHT.contains(player.getUUID()) && !(player.isCreative() || player.isSpectator())) {
			MagicState.toggleFlight(player);
		}

		Moves.aura(player, 600, RED, 0, 0);
		level.sendParticles(SUN, player.getX(), player.getY(1), player.getZ(), 60, 0.6, 1, 0.6, 0);
		Fx.sound(level, player, SoundEvents.BEACON_ACTIVATE, 1.5f, 1.0f);
		Moves.message(player, "message.solorust.hero.krypton", ChatFormatting.BLUE);
		return true;
	}

	static DustParticleOptions frost() {
		return FROST;
	}
}
