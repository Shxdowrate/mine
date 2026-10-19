package com.shxdowrate.solorust.magic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** 18 способностей Перчатки бесконечности. Порядок: [ячейка 1 ЛКМ, ячейка 1 ПКМ, ячейка 2 ЛКМ, …]. */
final class GauntletAbilities {
	private static final DustParticleOptions POWER = Fx.dust(InfinityStone.POWER.color, 1.6f);
	private static final DustParticleOptions POWER_SMALL = Fx.dust(0xD08CFF, 0.8f);
	private static final DustParticleOptions SPACE = Fx.dust(InfinityStone.SPACE.color, 1.3f);
	private static final DustParticleOptions REALITY = Fx.dust(InfinityStone.REALITY.color, 1.4f);
	private static final DustParticleOptions REALITY_DARK = Fx.dust(0x5A0010, 1.0f);
	private static final DustParticleOptions SOUL = Fx.dust(InfinityStone.SOUL.color, 1.2f);
	private static final DustParticleOptions TIME = Fx.dust(InfinityStone.TIME.color, 1.2f);
	private static final DustParticleOptions MIND = Fx.dust(InfinityStone.MIND.color, 1.3f);
	private static final DustParticleOptions VOID = Fx.dust(0x14001F, 2.2f);
	private static final DustParticleOptions ASH = Fx.dust(0x4A3F3A, 1.6f);
	private static final DustParticleOptions GOLD = Fx.dust(0xFFC94A, 1.2f);

	private static final int[] STONE_COLORS = {
			InfinityStone.POWER.color, InfinityStone.SPACE.color, InfinityStone.REALITY.color,
			InfinityStone.SOUL.color, InfinityStone.TIME.color, InfinityStone.MIND.color
	};

	static final Ability[] ABILITIES = {
			// 1 — Камень Силы
			new Ability("power_beam", 30, GauntletAbilities::powerBeam),
			new Ability("power_slam", 80, GauntletAbilities::powerSlam),
			// 2 — Камень Пространства
			new Ability("space_jump", 20, GauntletAbilities::spaceJump),
			new Ability("space_banish", 200, GauntletAbilities::banish),
			// 3 — Камень Реальности
			new Ability("reality_transform", 160, GauntletAbilities::transform),
			new Ability("reality_disintegrate", 60, GauntletAbilities::disintegrate),
			// 4 — Камень Души
			new Ability("soul_harvest", 80, GauntletAbilities::soulHarvest),
			new Ability("soul_discord", 400, GauntletAbilities::discord),
			// 5 — Камень Времени
			new Ability("time_stop", 600, GauntletAbilities::timeStop),
			new Ability("time_rewind", 300, GauntletAbilities::rewind),
			// 6 — Камень Разума
			new Ability("mind_beam", 40, GauntletAbilities::mindBeam),
			new Ability("mind_control", 400, GauntletAbilities::mindControl),
			// 7 — Сила + Пространство
			new Ability("titan_moon", 600, GauntletAbilities::titanMoon),
			new Ability("singularity", 500, GauntletAbilities::singularity),
			// 8 — Пространство + Время
			new Ability("world_cycle", 200, GauntletAbilities::worldCycle),
			new Ability("gauntlet_flight", 10, GauntletAbilities::flight),
			// 9 — все шесть камней
			new Ability("snap", 12000, GauntletAbilities::snap),
			new Ability("infinity_avatar", 1800, GauntletAbilities::avatar),
	};

	private GauntletAbilities() {
	}

	private static boolean noTarget(ServerPlayer player) {
		player.sendOverlayMessage(Component.translatable("message.solorust.magic.no_target").withStyle(ChatFormatting.GRAY));
		return false;
	}

	// ================================================================== 1. Сила

	/** Пронзающий фиолетовый луч: ранит всех на линии и взрывается в конце (без разрушения блоков). */
	private static boolean powerBeam(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1f);
		BlockHitResult block = level.clip(new ClipContext(eye, eye.add(look.scale(64)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 end = block.getLocation();
		Vec3 hand = Fx.hand(player);

		for (LivingEntity e : Fx.pierce(level, player, eye, end, 0.5)) {
			Fx.hurt(level, player, e, 14f);
			Fx.push(e, look, 1.2, 0.25);
			level.sendParticles(ParticleTypes.ENCHANTED_HIT, e.getX(), e.getY(0.6), e.getZ(), 10, 0.3, 0.3, 0.3, 0.3);
		}

		Fx.line(level, POWER, hand, end, 0.3);
		Fx.line(level, POWER_SMALL, hand, end, 0.7);
		Fx.line(level, ParticleTypes.END_ROD, hand, end, 3.0);
		level.sendParticles(ParticleTypes.EXPLOSION, end.x, end.y, end.z, 2, 0.3, 0.3, 0.3, 0);
		level.sendParticles(POWER, end.x, end.y, end.z, 30, 0.6, 0.6, 0.6, 0);
		Fx.sound(level, player, SoundEvents.WARDEN_SONIC_BOOM, 0.7f, 1.6f);
		Fx.sound(level, end, SoundEvents.GENERIC_EXPLODE.value(), 0.6f, 1.4f);
		return true;
	}

	/** Удар о землю: волна силы раскидывает и ранит всех вокруг. */
	private static boolean powerSlam(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 center = player.position();

		for (LivingEntity e : Fx.around(level, player, center.add(0, 1, 0), 10)) {
			double d = e.position().distanceTo(center);
			double k = 1.0 - d / 11.0;
			Fx.hurt(level, player, e, (float) Math.max(4, 18 * k));
			Fx.push(e, e.position().subtract(center), 0.6 + 2.4 * k, 0.5 + 0.5 * k);
		}

		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 0.5, center.z, 1, 0, 0, 0, 0);
		Fx.sound(level, player, SoundEvents.MACE_SMASH_GROUND_HEAVY, 2.0f, 0.5f);
		Fx.sound(level, player, SoundEvents.GENERIC_EXPLODE.value(), 1.2f, 0.7f);

		// Расходящееся кольцо по земле.
		Tasks.start(age -> {
			double r = 1 + age * 0.9;
			Fx.circle(level, POWER, center.add(0, 0.2, 0), new Vec3(0, 1, 0), r, (int) (r * 10), age * 0.2);

			if (age % 2 == 0) {
				Fx.circle(level, ParticleTypes.END_ROD, center.add(0, 0.4, 0), new Vec3(0, 1, 0), r * 0.9, (int) (r * 3), 0);
			}

			return age < 11;
		});
		return true;
	}

	// ================================================================== 2. Пространство

	/** Мгновенный скачок туда, куда смотришь (до 96 блоков). */
	private static boolean spaceJump(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getViewVector(1f).scale(96));
		BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 dest = Fx.standingSpot(level, hit, end.subtract(0, 1.6, 0));
		Vec3 from = player.position();

		level.sendParticles(SPACE, from.x, from.y + 1, from.z, 50, 0.4, 0.9, 0.4, 0);
		level.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1, from.z, 40, 0.4, 0.9, 0.4, 0.4);
		player.teleportTo(dest.x, dest.y, dest.z);
		player.fallDistance = 0;
		player.setDeltaMovement(Vec3.ZERO);
		player.hurtMarked = true;
		level.sendParticles(SPACE, dest.x, dest.y + 1, dest.z, 50, 0.4, 0.9, 0.4, 0);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, dest.x, dest.y + 1, dest.z, 60, 0.4, 0.9, 0.4, 0.1);
		Fx.sound(level, from, SoundEvents.ENDERMAN_TELEPORT, 1.0f, 0.7f);
		Fx.sound(level, player, SoundEvents.CHORUS_FRUIT_TELEPORT, 1.0f, 1.2f);
		return true;
	}

	/** Изгнание: цель вышвыривает через пространство на 120 блоков вверх. */
	private static boolean banish(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Fx.Ray ray = Fx.ray(level, player, 48, 0.4);
		LivingEntity target = ray.entity();

		if (target == null) {
			return noTarget(player);
		}

		Vec3 from = target.position();
		double y = Math.min(level.getMaxY() + 40, from.y + 120);
		Fx.line(level, SPACE, Fx.hand(player), target.getBoundingBox().getCenter(), 0.4);
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 1, from.z, 80, 0.5, 1, 0.5, 0.3);
		Fx.circle(level, SPACE, from.add(0, 0.1, 0), new Vec3(0, 1, 0), 1.2, 24, 0);
		target.teleportTo(from.x, y, from.z);
		target.setDeltaMovement(Vec3.ZERO);
		target.hurtMarked = true;
		target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
		Fx.sound(level, from, SoundEvents.ENDERMAN_TELEPORT, 1.2f, 0.5f);
		Fx.sound(level, player, SoundEvents.PORTAL_TRAVEL, 0.3f, 1.8f);
		return true;
	}

	// ================================================================== 3. Реальность

	/** Превращение: враг становится безобидным животным, игрок — ослеплён и сбит с толку. */
	private static boolean transform(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Fx.Ray ray = Fx.ray(level, player, 40, 0.4);
		LivingEntity target = ray.entity();

		if (target == null) {
			return noTarget(player);
		}

		Vec3 c = target.getBoundingBox().getCenter();
		Fx.line(level, REALITY, Fx.hand(player), c, 0.35);
		ribbons(level, target.position(), target.getBbHeight());

		if (target instanceof Player victim) {
			victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 160, 0));
			victim.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
			victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 160, 2));
		} else if (target instanceof Mob mob && !Fx.isBoss(mob)) {
			List<EntityType<? extends Mob>> forms = List.of(EntityType.CHICKEN, EntityType.RABBIT, EntityType.SHEEP, EntityType.PIG,
					EntityType.COW, EntityType.FROG, EntityType.AXOLOTL);
			EntityType<? extends Mob> form = forms.get(player.getRandom().nextInt(forms.size()));
			Mob created = form.spawn(level, mob.blockPosition(), EntitySpawnReason.MOB_SUMMONED);

			if (created != null) {
				created.setYRot(mob.getYRot());
			}

			mob.discard();
		} else {
			// Боссам реальность поддаётся только частично.
			Fx.hurt(level, player, target, 20f);
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2));
		}

		Fx.sound(level, c, SoundEvents.ZOMBIE_VILLAGER_CURE, 0.8f, 1.8f);
		Fx.sound(level, c, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0f, 0.6f);
		return true;
	}

	/** Красные «ленты» Эфира, закручивающиеся вокруг цели. */
	private static void ribbons(ServerLevel level, Vec3 base, double height) {
		Tasks.start(age -> {
			for (int s = 0; s < 3; s++) {
				double a = age * 0.6 + s * Math.PI * 2 / 3;
				double y = (age % 12) / 12.0 * (height + 0.5);
				level.sendParticles(REALITY, true, false, base.x + Math.cos(a) * 0.8, base.y + y, base.z + Math.sin(a) * 0.8, 1, 0, 0, 0, 0);
				level.sendParticles(REALITY_DARK, true, false, base.x + Math.cos(a + 0.4) * 0.7, base.y + y + 0.15, base.z + Math.sin(a + 0.4) * 0.7, 1, 0, 0, 0, 0);
			}

			return age < 24;
		});
	}

	/** Распад материи: шар блоков радиусом 3 рассыпается, добыча летит в инвентарь. */
	private static boolean disintegrate(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 eye = player.getEyePosition();
		BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getViewVector(1f).scale(48)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

		if (hit.getType() != HitResult.Type.BLOCK) {
			return noTarget(player);
		}

		BlockPos center = hit.getBlockPos();
		ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
		Map<Item, Integer> loot = new HashMap<>();
		int broken = 0;

		for (BlockPos p : BlockPos.betweenClosed(center.offset(-3, -3, -3), center.offset(3, 3, 3))) {
			if (p.distSqr(center) > 3.4 * 3.4) {
				continue;
			}

			BlockPos pos = p.immutable();
			BlockState state = level.getBlockState(pos);
			float hardness = state.getDestroySpeed(level, pos);

			if (state.isAir() || hardness < 0 || hardness >= 50 || level.getBlockEntity(pos) != null
					|| !state.getFluidState().isEmpty() || !player.mayBuild() || !level.mayInteract(player, pos)) {
				continue;
			}

			for (ItemStack drop : Block.getDrops(state, level, pos, null, player, tool)) {
				loot.merge(drop.getItem(), drop.getCount(), Integer::sum);
			}

			MagicState.recordBroken(level, pos, state);
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

			if (broken++ % 3 == 0) {
				level.sendParticles(REALITY, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0);
				level.sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.3, 0.3, 0.3, 0.2);
			}
		}

		if (broken == 0) {
			return noTarget(player);
		}

		for (Map.Entry<Item, Integer> e : loot.entrySet()) {
			int count = e.getValue();

			while (count > 0) {
				int n = Math.min(count, e.getKey().getDefaultMaxStackSize());
				MagicState.giveOrDrop(player, new ItemStack(e.getKey(), n));
				count -= n;
			}
		}

		Fx.line(level, REALITY, Fx.hand(player), Vec3.atCenterOf(center), 0.4);
		Fx.sound(level, Vec3.atCenterOf(center), SoundEvents.AMETHYST_CLUSTER_BREAK, 1.5f, 0.5f);
		Fx.sound(level, Vec3.atCenterOf(center), SoundEvents.SOUL_ESCAPE.value(), 1.5f, 0.8f);
		return true;
	}

	// ================================================================== 4. Душа

	/** Жатва душ: вытягивает жизнь из цели и лечит тебя на столько же. */
	private static boolean soulHarvest(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Fx.Ray ray = Fx.ray(level, player, 40, 0.4);
		LivingEntity target = ray.entity();

		if (target == null) {
			return noTarget(player);
		}

		float before = target.getHealth();
		Fx.hurt(level, player, target, 12f);
		float drained = Math.max(2f, before - Math.max(0, target.getHealth()));
		player.heal(drained);

		Vec3 from = target.getBoundingBox().getCenter();
		Vec3 to = player.getBoundingBox().getCenter();
		Fx.line(level, SOUL, from, to, 0.4);
		Fx.line(level, ParticleTypes.SOUL, from, to, 1.0);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, from.x, from.y, from.z, 20, 0.3, 0.4, 0.3, 0.05);
		level.sendParticles(ParticleTypes.HEART, to.x, to.y + 0.6, to.z, 3, 0.3, 0.3, 0.3, 0);
		Fx.sound(level, player, SoundEvents.SOUL_ESCAPE.value(), 2.0f, 0.7f);
		return true;
	}

	/** Раздор: мобы вокруг на 15 секунд нападают друг на друга. */
	private static boolean discord(ServerLevel level, ServerPlayer player, ItemStack stack) {
		List<Mob> mobs = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(20),
				m -> Fx.isTarget(m, player) && !Fx.isBoss(m));

		if (mobs.size() < 2) {
			return noTarget(player);
		}

		long until = level.getGameTime() + 300;

		for (Mob mob : mobs) {
			MagicState.charm(mob, player, until, false);
			mob.setTarget(null);
			level.sendParticles(SOUL, mob.getX(), mob.getY(1.1), mob.getZ(), 15, 0.3, 0.3, 0.3, 0);
			level.sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY(0.5), mob.getZ(), 5, 0.3, 0.4, 0.3, 0.02);
		}

		Fx.circle(level, SOUL, player.position().add(0, 0.2, 0), new Vec3(0, 1, 0), 6, 60, 0);
		Fx.sound(level, player, SoundEvents.EVOKER_PREPARE_WOLOLO, 1.5f, 0.6f);
		player.sendOverlayMessage(Component.translatable("message.solorust.magic.discord", mobs.size()).withStyle(ChatFormatting.GOLD));
		return true;
	}

	// ================================================================== 5. Время

	/** Остановка времени: всё вокруг (мобы, стрелы, снаряды) замирает на 10 секунд. */
	private static boolean timeStop(ServerLevel level, ServerPlayer player, ItemStack stack) {
		long until = level.getGameTime() + 200;
		AABB area = player.getBoundingBox().inflate(24);
		int count = 0;

		for (Entity e : level.getEntities(player, area, e -> e instanceof Mob || e instanceof Projectile || e instanceof ItemEntity)) {
			Freeze.freeze(e, until);
			count++;
		}

		for (Player other : level.getEntitiesOfClass(Player.class, area, p -> p != player && Fx.isTarget(p, player))) {
			other.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 9));
			other.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 200, 4));
		}

		Vec3 c = player.position().add(0, 1, 0);
		Tasks.start(age -> {
			if (age < 8) {
				Fx.circle(level, TIME, c, new Vec3(0, 1, 0), 1 + age * 3, 40 + age * 12, age * 0.1);
			}

			if (age % 10 == 0) {
				for (Entity e : level.getEntities(player, area, Freeze::isFrozen)) {
					level.sendParticles(TIME, e.getX(), e.getY(1.05), e.getZ(), 2, 0.2, 0.1, 0.2, 0);
				}

				Fx.sound(level, c, SoundEvents.STONE_BUTTON_CLICK_ON, 0.4f, age % 20 == 0 ? 0.6f : 0.5f);
			}

			return age < 200;
		});

		Fx.sound(level, player, SoundEvents.BELL_RESONATE, 1.5f, 0.5f);
		Fx.sound(level, player, SoundEvents.BEACON_DEACTIVATE, 1.5f, 0.5f);
		player.sendOverlayMessage(Component.translatable("message.solorust.magic.time_stop", count).withStyle(ChatFormatting.GREEN));
		return true;
	}

	/** Откат: возвращает тебя туда, где ты был 5 секунд назад, с тем здоровьем и сытостью. */
	private static boolean rewind(ServerLevel level, ServerPlayer player, ItemStack stack) {
		List<MagicState.Snapshot> history = MagicState.history(player);

		if (history.size() < 20) {
			player.sendOverlayMessage(Component.translatable("message.solorust.magic.no_history").withStyle(ChatFormatting.GRAY));
			return false;
		}

		int index = Math.max(0, history.size() - 100);
		MagicState.Snapshot snap = history.get(index);
		ServerLevel dest = level.getServer().getLevel(snap.dimension());

		if (dest == null) {
			return false;
		}

		// След пути назад.
		for (int i = history.size() - 1; i > index; i -= 3) {
			MagicState.Snapshot s = history.get(i);

			if (s.dimension().equals(level.dimension())) {
				level.sendParticles(TIME, true, false, s.pos().x, s.pos().y + 1, s.pos().z, 2, 0.15, 0.3, 0.15, 0);
			}
		}

		player.teleportTo(dest, snap.pos().x, snap.pos().y, snap.pos().z, Set.of(), snap.yRot(), snap.xRot(), false);
		player.setHealth(Math.max(player.getHealth(), snap.health()));
		player.getFoodData().setFoodLevel(Math.max(player.getFoodData().getFoodLevel(), snap.food()));
		player.clearFire();
		player.fallDistance = 0;
		player.setDeltaMovement(Vec3.ZERO);
		player.hurtMarked = true;
		Fx.clearHarmful(player);
		MagicState.HISTORY.remove(player.getUUID());

		Vec3 c = player.position().add(0, 1, 0);
		dest.sendParticles(TIME, c.x, c.y, c.z, 60, 0.5, 0.9, 0.5, 0);
		dest.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 20, 0.4, 0.8, 0.4, 0.05);
		Fx.sound(dest, player, SoundEvents.BELL_RESONATE, 1.2f, 1.6f);
		Fx.sound(dest, player, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.0f, 1.4f);
		return true;
	}

	// ================================================================== 6. Разум

	/** Пси-луч: урон и оглушение одной цели. */
	private static boolean mindBeam(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Fx.Ray ray = Fx.ray(level, player, 48, 0.3);
		Vec3 hand = Fx.hand(player);
		Fx.line(level, MIND, hand, ray.end(), 0.3);
		Fx.line(level, ParticleTypes.ELECTRIC_SPARK, hand, ray.end(), 1.2);

		if (ray.entity() != null) {
			LivingEntity e = ray.entity();
			Fx.hurt(level, player, e, 10f);
			e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2));
			e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));

			if (e instanceof Player) {
				e.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 120, 0));
			} else if (e instanceof Mob mob) {
				mob.setTarget(null);
				mob.getNavigation().stop();
			}

			level.sendParticles(MIND, e.getX(), e.getEyeY() + 0.3, e.getZ(), 15, 0.3, 0.2, 0.3, 0);
		}

		level.sendParticles(ParticleTypes.ENCHANTED_HIT, ray.end().x, ray.end().y, ray.end().z, 8, 0.2, 0.2, 0.2, 0.2);
		Fx.sound(level, player, SoundEvents.BEACON_POWER_SELECT, 0.8f, 2.0f);
		Fx.sound(level, player, SoundEvents.EVOKER_CAST_SPELL, 0.6f, 1.8f);
		return true;
	}

	/** Контроль разума: моб на 60 секунд становится твоим союзником и идёт за тобой. */
	private static boolean mindControl(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Fx.Ray ray = Fx.ray(level, player, 32, 0.4);

		if (!(ray.entity() instanceof Mob mob) || Fx.isBoss(mob)) {
			return noTarget(player);
		}

		MagicState.charm(mob, player, level.getGameTime() + 1200, true);
		mob.setTarget(null);
		mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 1200, 0, false, false, true));
		mob.heal(mob.getMaxHealth());
		Fx.line(level, MIND, Fx.hand(player), mob.getBoundingBox().getCenter(), 0.3);
		level.sendParticles(MIND, mob.getX(), mob.getEyeY() + 0.4, mob.getZ(), 30, 0.4, 0.3, 0.4, 0);
		Fx.sound(level, player, SoundEvents.EVOKER_PREPARE_ATTACK, 1.0f, 1.4f);
		player.sendOverlayMessage(Component.translatable("message.solorust.magic.mind_control", mob.getName()).withStyle(ChatFormatting.YELLOW));
		return true;
	}

	// ================================================================== 7. Сила + Пространство

	/** «Луна Титана»: дождь метеоров на точку, куда смотришь. Блоки не ломает. */
	private static boolean titanMoon(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Fx.Ray ray = Fx.ray(level, player, 128, 0.3);
		Vec3 target = ray.end();
		RandomSource random = player.getRandom();
		List<Vec3[]> meteors = new ArrayList<>();

		for (int i = 0; i < 12; i++) {
			Vec3 land = target.add((random.nextDouble() - 0.5) * 12, 0, (random.nextDouble() - 0.5) * 12);
			Vec3 start = land.add((random.nextDouble() - 0.5) * 20, 38 + random.nextDouble() * 10, (random.nextDouble() - 0.5) * 20);
			meteors.add(new Vec3[] {start, land.subtract(start).normalize().scale(1.7), new Vec3(i * 4, 0, 0)});
		}

		Fx.sound(level, player, SoundEvents.ENDER_DRAGON_GROWL, 1.0f, 0.5f);
		Fx.circle(level, POWER, target.add(0, 0.3, 0), new Vec3(0, 1, 0), 6, 50, 0);

		Tasks.start(age -> {
			boolean alive = false;

			for (Vec3[] m : meteors) {
				if (m[2] == null) {
					continue;
				}

				alive = true;

				if (age < m[2].x) {
					continue;
				}

				Vec3 pos = m[0];
				Vec3 next = pos.add(m[1]);
				BlockHitResult hit = level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty()));
				boolean hitEntity = !level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, next).inflate(0.8), e -> Fx.isTarget(e, player)).isEmpty();

				level.sendParticles(ParticleTypes.FLAME, true, false, pos.x, pos.y, pos.z, 6, 0.4, 0.4, 0.4, 0.02);
				level.sendParticles(ParticleTypes.LARGE_SMOKE, true, false, pos.x, pos.y, pos.z, 3, 0.3, 0.3, 0.3, 0.01);
				level.sendParticles(POWER, true, false, pos.x, pos.y, pos.z, 3, 0.4, 0.4, 0.4, 0);

				if (hit.getType() != HitResult.Type.MISS || hitEntity || age - m[2].x > 60) {
					Vec3 at = hit.getType() != HitResult.Type.MISS ? hit.getLocation() : next;
					MagicState.explodingCaster = player;

					try {
						level.explode(player, at.x, at.y, at.z, 3.2f, Level.ExplosionInteraction.NONE);
					} finally {
						MagicState.explodingCaster = null;
					}

					level.sendParticles(ParticleTypes.LAVA, at.x, at.y, at.z, 12, 1, 0.5, 1, 0);
					m[2] = null;
				} else {
					m[0] = next;
				}
			}

			return alive;
		});
		return true;
	}

	/** Сингулярность: чёрная дыра 7 секунд затягивает мобов и предметы, а затем схлопывается. */
	private static boolean singularity(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Fx.Ray ray = Fx.ray(level, player, 48, 0.3);
		Vec3 c = ray.entity() != null ? ray.entity().getBoundingBox().getCenter() : ray.end().subtract(player.getViewVector(1f).scale(1.5));

		Fx.sound(level, c, SoundEvents.BEACON_ACTIVATE, 1.5f, 0.5f);
		Fx.sound(level, c, SoundEvents.END_PORTAL_SPAWN, 0.6f, 1.6f);

		Tasks.start(age -> {
			if (age < 140) {
				AABB area = new AABB(c, c).inflate(14);

				for (Entity e : level.getEntities((Entity) null, area, e -> e != player && (e instanceof ItemEntity || e instanceof ExperienceOrb
						|| e instanceof Projectile || Fx.isTarget(e, player)))) {
					Vec3 to = c.subtract(e.getBoundingBox().getCenter());
					double d = to.length();

					if (d > 14) {
						continue;
					}

					double strength = 0.12 + 0.25 * (1 - d / 14);
					e.setDeltaMovement(e.getDeltaMovement().scale(0.6).add(to.normalize().scale(strength * Math.min(d, 3))));
					e.hurtMarked = true;
					e.fallDistance = 0;

					if (d < 2.2 && age % 10 == 0 && e instanceof LivingEntity living) {
						Fx.hurt(level, player, living, 3f);
					}
				}

				// Аккреционный диск и тёмное ядро.
				for (int i = 0; i < 3; i++) {
					double a = age * 0.35 + i * Math.PI * 2 / 3;
					double r = 3.5 - (age % 20) * 0.15;
					level.sendParticles(POWER_SMALL, true, false, c.x + Math.cos(a) * r, c.y + Math.sin(age * 0.1 + i) * 0.3, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
					level.sendParticles(SPACE, true, false, c.x + Math.cos(a + 1) * r * 1.4, c.y, c.z + Math.sin(a + 1) * r * 1.4, 1, 0, 0, 0, 0);
				}

				level.sendParticles(VOID, true, false, c.x, c.y, c.z, 4, 0.25, 0.25, 0.25, 0);
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, true, false, c.x, c.y, c.z, 6, 2.5, 2.5, 2.5, 0.05);

				if (age % 30 == 0) {
					Fx.sound(level, c, SoundEvents.BEACON_AMBIENT, 1.5f, 0.5f);
				}

				return true;
			}

			// Схлопывание.
			for (LivingEntity e : Fx.around(level, player, c, 6)) {
				Fx.hurt(level, player, e, 12f);
				Fx.push(e, e.position().subtract(c), 2.0, 0.6);
			}

			level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 1, 0, 0, 0, 0);
			level.sendParticles(POWER, true, false, c.x, c.y, c.z, 80, 2, 2, 2, 0);
			Fx.sound(level, c, SoundEvents.GENERIC_EXPLODE.value(), 2.0f, 0.6f);
			Fx.sound(level, c, SoundEvents.WARDEN_SONIC_BOOM, 1.5f, 0.6f);
			return false;
		});
		return true;
	}

	// ================================================================== 8. Пространство + Время

	/** Власть над миром: день ↔ ночь и ясная погода. */
	private static boolean worldCycle(ServerLevel level, ServerPlayer player, ItemStack stack) {
		var clock = level.dimensionTypeRegistration().value().defaultClock();

		if (clock.isEmpty()) {
			player.sendOverlayMessage(Component.translatable("message.solorust.magic.no_sky").withStyle(ChatFormatting.GRAY));
			return false;
		}

		Holder<WorldClock> holder = clock.get();
		boolean toNight = level.isBrightOutside();
		level.getServer().clockManager().moveToTimeMarker(holder, toNight ? ClockTimeMarkers.NIGHT : ClockTimeMarkers.DAY);
		level.getServer().setWeatherParameters(12000, 0, false, false);

		Vec3 c = player.position();
		Tasks.start(age -> {
			Fx.circle(level, age % 2 == 0 ? TIME : SPACE, c.add(0, 0.2 + age * 0.4, 0), new Vec3(0, 1, 0), 2 + age * 0.1, 30, age * 0.3);
			return age < 20;
		});
		Fx.sound(level, player, SoundEvents.BELL_RESONATE, 1.5f, toNight ? 0.5f : 1.5f);
		player.sendOverlayMessage(Component.translatable(toNight ? "message.solorust.magic.night" : "message.solorust.magic.day").withStyle(ChatFormatting.AQUA));
		return true;
	}

	private static boolean flight(ServerLevel level, ServerPlayer player, ItemStack stack) {
		return StrangeAbilities.toggleFlight(level, player, SPACE);
	}

	// ================================================================== 9. Все камни

	/** Щелчок: половина всего живого вокруг (кроме твоих питомцев и именованных) обращается в прах. */
	private static boolean snap(ServerLevel level, ServerPlayer player, ItemStack stack) {
		List<LivingEntity> all = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(64),
				e -> Fx.isTarget(e, player) && !(e instanceof Player) && !e.hasCustomName()));

		if (all.isEmpty()) {
			return noTarget(player);
		}

		Collections.shuffle(all, new java.util.Random(player.getRandom().nextLong()));
		List<LivingEntity> victims = all.subList(0, (all.size() + 1) / 2);
		Map<LivingEntity, Integer> when = new HashMap<>();
		RandomSource random = player.getRandom();

		for (LivingEntity e : victims) {
			when.put(e, 20 + random.nextInt(60));
		}

		Fx.sound(level, player, SoundEvents.WOODEN_BUTTON_CLICK_ON, 2.0f, 1.6f);
		Fx.sound(level, player, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.5f, 0.6f);
		Vec3 c = player.getEyePosition();

		for (int i = 0; i < 6; i++) {
			level.sendParticles(Fx.dust(STONE_COLORS[i], 1.5f), c.x, c.y, c.z, 20, 0.6, 0.6, 0.6, 0);
		}

		for (int i = 0; i < 160; i++) {
			double a = i / 160.0 * Math.PI * 2;
			level.sendParticles(ParticleTypes.END_ROD, true, false, player.getX(), player.getY(0.5), player.getZ(), 0, Math.cos(a), 0.02, Math.sin(a), 1.8);
		}

		Tasks.start(age -> {
			for (Map.Entry<LivingEntity, Integer> e : when.entrySet()) {
				LivingEntity victim = e.getKey();
				int t = e.getValue();

				if (victim.isRemoved() || !victim.isAlive()) {
					continue;
				}

				if (age >= t - 20 && age < t) {
					// Начинает рассыпаться.
					level.sendParticles(ASH, victim.getX(), victim.getY(0.5), victim.getZ(), 4, victim.getBbWidth() * 0.4, victim.getBbHeight() * 0.4, victim.getBbWidth() * 0.4, 0);
					level.sendParticles(ParticleTypes.ASH, victim.getX(), victim.getY(0.8), victim.getZ(), 4, 0.3, 0.3, 0.3, 0.02);
				} else if (age == t) {
					level.sendParticles(ASH, victim.getX(), victim.getY(0.5), victim.getZ(), 40, victim.getBbWidth() * 0.6, victim.getBbHeight() * 0.5, victim.getBbWidth() * 0.6, 0.03);
					level.sendParticles(ParticleTypes.WHITE_ASH, victim.getX(), victim.getY(0.5), victim.getZ(), 30, 0.5, 0.7, 0.5, 0.05);

					if (Fx.isBoss(victim)) {
						Fx.hurt(level, player, victim, 200f);
					} else {
						victim.kill(level);
					}
				}
			}

			return age < 82;
		});

		if (!player.hasInfiniteMaterials()) {
			// Цена щелчка: перчатка обжигает руку.
			player.setHealth(Math.max(1f, player.getHealth() * 0.3f));
			player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 1));
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 400, 1));
			player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 1200, 1));
		}

		player.sendOverlayMessage(Component.translatable("message.solorust.magic.snap", victims.size()).withStyle(ChatFormatting.GOLD));
		return true;
	}

	/** Аватар бесконечности: 20 секунд силы всех камней. */
	private static boolean avatar(ServerLevel level, ServerPlayer player, ItemStack stack) {
		int t = 400;
		player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, t, 2));
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, t, 2));
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, t, 1));
		player.addEffect(new MobEffectInstance(MobEffects.SPEED, t, 1));
		player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, t, 1));
		player.addEffect(new MobEffectInstance(MobEffects.HASTE, t, 2));
		player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, t, 0));
		player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, t, 3));
		player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, t + 200, 0));
		player.clearFire();
		Fx.clearHarmful(player);

		Tasks.start(age -> {
			if (player.isRemoved() || !player.isAlive()) {
				return false;
			}

			if (age % 2 == 0) {
				ServerLevel lv = player.level();
				double a = age * 0.3;

				for (int i = 0; i < 6; i++) {
					double b = a + i * Math.PI / 3;
					lv.sendParticles(Fx.dust(STONE_COLORS[i], 0.9f), player.getX() + Math.cos(b) * 0.9, player.getY() + 0.2 + (age % 40) / 40.0 * 2,
							player.getZ() + Math.sin(b) * 0.9, 1, 0, 0, 0, 0);
				}
			}

			return age < t;
		});

		level.sendParticles(GOLD, player.getX(), player.getY(1), player.getZ(), 60, 0.6, 1, 0.6, 0);
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY(1), player.getZ(), 40, 0.5, 0.8, 0.5, 0.4);
		Fx.sound(level, player, SoundEvents.BEACON_ACTIVATE, 1.5f, 1.2f);
		Fx.sound(level, player, SoundEvents.TOTEM_USE, 0.8f, 1.3f);
		return true;
	}
}
