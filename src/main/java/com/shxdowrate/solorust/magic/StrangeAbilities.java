package com.shxdowrate.solorust.magic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** 18 мистических искусств Доктора Стрэнджа. Порядок: [ячейка 1 ЛКМ, ячейка 1 ПКМ, …]. */
final class StrangeAbilities {
	private static final DustParticleOptions GOLD = Fx.dust(0xFFB347, 1.1f);
	private static final DustParticleOptions ORANGE = Fx.dust(0xFF8C1A, 1.3f);
	private static final DustParticleOptions RUNE = Fx.dust(0xFFE08A, 0.7f);
	private static final DustParticleOptions CRIMSON = Fx.dust(0xD81B3C, 1.3f);
	private static final DustParticleOptions CRIMSON_DARK = Fx.dust(0x7A0A1E, 1.0f);
	private static final DustParticleOptions EYE = Fx.dust(0x2BD94A, 1.2f);
	private static final DustParticleOptions EYE_LIGHT = Fx.dust(0xA8FFB8, 0.7f);
	private static final DustParticleOptions ASTRAL = Fx.dust(0x9FE6FF, 1.0f);
	private static final DustParticleOptions MIRROR = Fx.dust(0xE2D9FF, 0.9f);
	private static final DustParticleOptions MIRROR_DARK = Fx.dust(0x6B4FD8, 0.9f);
	private static final DustParticleOptions CLOAK = Fx.dust(0xB71C1C, 1.3f);
	private static final DustParticleOptions DARK = Fx.dust(0x5B1A8E, 2.0f);
	private static final DustParticleOptions DARK_ACCENT = Fx.dust(0xC77DFF, 1.0f);
	private static final DustParticleOptions HEAL = Fx.dust(0xFFF4C2, 1.0f);

	static final Ability[] ABILITIES = {
			// 1 — Кольцо-портал
			new Ability("sling_portal", 60, StrangeAbilities::portal),
			new Ability("sanctum_portal", 600, StrangeAbilities::homePortal),
			// 2 — Защитные чары
			new Ability("seraphim_shield", 200, StrangeAbilities::seraphimShield),
			new Ability("hoggoth_dome", 500, StrangeAbilities::hoggothDome),
			// 3 — Боевая магия
			new Ability("balthakk_bolts", 40, StrangeAbilities::bolts),
			new Ability("watoomb_whip", 30, StrangeAbilities::whip),
			// 4 — Сковывающие чары
			new Ability("cyttorak_bands", 300, StrangeAbilities::bands),
			new Ability("watoomb_winds", 100, StrangeAbilities::winds),
			// 5 — Око Агамотто
			new Ability("agamotto_rewind", 600, StrangeAbilities::rewindWorld),
			new Ability("agamotto_growth", 600, StrangeAbilities::acceleration),
			// 6 — Астрал и зеркала
			new Ability("astral_projection", 400, StrangeAbilities::astral),
			new Ability("mirror_dimension", 600, StrangeAbilities::mirror),
			// 7 — Плащ левитации
			new Ability("cloak_dash", 40, StrangeAbilities::cloakDash),
			new Ability("cloak_flight", 10, StrangeAbilities::cloakFlight),
			// 8 — Пламя и исцеление
			new Ability("faltine_flames", 60, StrangeAbilities::flames),
			new Ability("vishanti_heal", 1200, StrangeAbilities::heal),
			// 9 — Высшая магия
			new Ability("dark_dimension", 2400, StrangeAbilities::darkDimension),
			new Ability("dormammu_loop", 6000, StrangeAbilities::timeLoop),
	};

	/** До какого тика идёт астральная форма (дублирует вложение, чтобы не читать его каждый тик). */
	private static final Map<UUID, Integer> ASTRAL_SECONDS = new HashMap<>();

	private StrangeAbilities() {
	}

	private static boolean noTarget(ServerPlayer player) {
		player.sendOverlayMessage(Component.translatable("message.solorust.magic.no_target").withStyle(ChatFormatting.GRAY));
		return false;
	}

	// ================================================================== 1. Кольцо-портал

	/** Портал туда, куда смотришь (до 160 блоков): ближнее кольцо перед тобой, дальнее — в точке взгляда. */
	private static boolean portal(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 forward = Fx.flatLook(player);
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1f);
		BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(160)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 far = hit.getType() == HitResult.Type.MISS ? eye.add(look.scale(64)) : hit.getLocation().subtract(forward.scale(1.6));
		Vec3 farFeet = ground(level, far);

		if (farFeet.distanceTo(player.position()) < 6) {
			player.sendOverlayMessage(Component.translatable("message.solorust.magic.portal_close").withStyle(ChatFormatting.GRAY));
			return false;
		}

		Vec3 nearCenter = player.position().add(forward.scale(2.4)).add(0, 1.2, 0);
		Vec3 farCenter = farFeet.add(0, 1.2, 0);
		Portals.open(player,
				new Portals.End(level, nearCenter, forward, forward.scale(-1)),
				new Portals.End(level, farCenter, forward, forward),
				400);
		openFlash(level, nearCenter, forward);
		openFlash(level, farCenter, forward);
		return true;
	}

	/** Портал в Святилище: домой, к точке возрождения (кровать / якорь / спавн мира), в любое измерение. */
	private static boolean homePortal(ServerLevel level, ServerPlayer player, ItemStack stack) {
		ServerPlayer.RespawnConfig config = player.getRespawnConfig();
		LevelData.RespawnData data = config != null ? config.respawnData() : level.getServer().overworld().getRespawnData();
		GlobalPos home = data.globalPos();
		ServerLevel homeLevel = level.getServer().getLevel(home.dimension());

		if (homeLevel == null) {
			homeLevel = level.getServer().overworld();
			home = GlobalPos.of(homeLevel.dimension(), homeLevel.getRespawnData().globalPos().pos());
		}

		BlockPos pos = home.pos();
		BlockState state = homeLevel.getBlockState(pos);
		Vec3 feet;

		if (state.getBlock() instanceof BedBlock || state.is(BlockTags.BEDS)) {
			feet = new Vec3(pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5);
		} else if (config != null && !state.getCollisionShape(homeLevel, pos).isEmpty()) {
			feet = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
		} else if (config != null) {
			feet = Vec3.atBottomCenterOf(pos);
		} else {
			BlockPos top = homeLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
			feet = Vec3.atBottomCenterOf(top);
		}

		Vec3 forward = Fx.flatLook(player);
		Vec3 nearCenter = player.position().add(forward.scale(2.4)).add(0, 1.2, 0);
		Vec3 farCenter = feet.add(forward.scale(1.5)).add(0, 1.2, 0);
		Portals.open(player,
				new Portals.End(level, nearCenter, forward, forward.scale(-1)),
				new Portals.End(homeLevel, farCenter, forward, forward.scale(-1)),
				300);
		openFlash(level, nearCenter, forward);
		openFlash(homeLevel, farCenter, forward);
		player.sendOverlayMessage(Component.translatable("message.solorust.magic.sanctum").withStyle(ChatFormatting.GOLD));
		return true;
	}

	/** Найти землю под точкой (до 24 блоков вниз). */
	private static Vec3 ground(ServerLevel level, Vec3 at) {
		BlockHitResult down = level.clip(new ClipContext(at, at.subtract(0, 24, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
		return down.getType() == HitResult.Type.MISS ? at : down.getLocation();
	}

	private static void openFlash(ServerLevel level, Vec3 c, Vec3 normal) {
		Tasks.start(age -> {
			Fx.circle(level, ORANGE, c, normal, 0.2 + age * 0.12, 20, age * 0.5);
			return age < 9;
		});
		level.sendParticles(ParticleTypes.FLAME, true, false, c.x, c.y, c.z, 30, 0.4, 0.6, 0.4, 0.08);
	}

	// ================================================================== 2. Защитные чары

	/** Щит Серафима: мандала перед тобой 10 секунд — гасит урон спереди, отражает снаряды, отталкивает мобов. */
	private static boolean seraphimShield(ServerLevel level, ServerPlayer player, ItemStack stack) {
		long until = level.getGameTime() + 200;
		MagicState.SHIELD.put(player.getUUID(), until);
		Fx.sound(level, player, SoundEvents.BEACON_ACTIVATE, 1.0f, 1.8f);
		Fx.sound(level, player, SoundEvents.ENCHANTMENT_TABLE_USE, 1.0f, 0.8f);

		Tasks.start(age -> {
			if (player.isRemoved() || !MagicState.SHIELD.containsKey(player.getUUID())) {
				return false;
			}

			ServerLevel lv = player.level();
			Vec3 look = player.getViewVector(1f);
			Vec3 c = player.getEyePosition().add(look.scale(1.6)).add(0, -0.2, 0);

			if (age % 2 == 0) {
				Fx.mandala(lv, GOLD, RUNE, c, look, 1.5, age * 0.12);
			}

			for (Entity e : lv.getEntities(player, player.getBoundingBox().inflate(5), e -> e instanceof Projectile)) {
				Vec3 to = e.position().subtract(player.getEyePosition());

				if (Fx.angle(look, to) < 80 && e.getDeltaMovement().dot(to) < 0 && ((Projectile) e).getOwner() != player) {
					e.setDeltaMovement(e.getDeltaMovement().scale(-1.2));
					((Projectile) e).setOwner(player);
					e.hurtMarked = true;
					lv.sendParticles(ParticleTypes.ENCHANTED_HIT, e.getX(), e.getY(), e.getZ(), 6, 0.1, 0.1, 0.1, 0.2);
					Fx.sound(lv, e.position(), SoundEvents.SHIELD_BLOCK.value(), 0.8f, 1.6f);
				}
			}

			if (age % 5 == 0) {
				for (LivingEntity e : Fx.around(lv, player, c, 2.4)) {
					Fx.push(e, look, 1.2, 0.3);

					if (age % 20 == 0) {
						Fx.hurt(lv, player, e, 3f);
					}
				}
			}

			return age < 200;
		});
		return true;
	}

	/** Купол Хоггота: неподвижная сфера радиусом 6 на 12 секунд — враги и снаряды не проходят внутрь. */
	private static boolean hoggothDome(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 c = player.position();
		double r = 6;
		Fx.sound(level, player, SoundEvents.BEACON_ACTIVATE, 1.5f, 1.3f);
		Fx.sound(level, player, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5f, 0.7f);

		Tasks.start(age -> {
			RandomSource random = level.getRandom();

			// Сфера из рун + две мандалы: на земле и над головой.
			for (int i = 0; i < 36; i++) {
				double y = random.nextDouble() * 2 - 1;
				double a = random.nextDouble() * Math.PI * 2;
				double k = Math.sqrt(1 - y * y);
				Vec3 p = c.add(Math.cos(a) * k * r, Math.abs(y) * r, Math.sin(a) * k * r);
				level.sendParticles(i % 4 == 0 ? RUNE : GOLD, true, false, p.x, p.y, p.z, 1, 0, 0, 0, 0);
			}

			if (age % 4 == 0) {
				Fx.mandala(level, ORANGE, RUNE, c.add(0, 0.15, 0), new Vec3(0, 1, 0), r, age * 0.05);
			}

			AABB box = new AABB(c, c).inflate(r + 1);

			for (Entity e : level.getEntities((Entity) null, box, e -> e.getBoundingBox().getCenter().distanceTo(c) < r + 0.5)) {
				if (e instanceof Projectile projectile) {
					if (projectile.getOwner() != player) {
						level.sendParticles(ParticleTypes.ENCHANTED_HIT, e.getX(), e.getY(), e.getZ(), 6, 0.1, 0.1, 0.1, 0.2);
						e.discard();
					}
				} else if (e instanceof Mob mob && Fx.isTarget(mob, player) && (Fx.hostile(mob) || mob.getTarget() != null)) {
					Vec3 out = mob.position().subtract(c);
					Fx.push(mob, out.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : out, 0.9, 0.25);

					if (age % 20 == 0) {
						Fx.hurt(level, player, mob, 2f);
						level.sendParticles(ParticleTypes.ENCHANTED_HIT, mob.getX(), mob.getY(0.6), mob.getZ(), 8, 0.2, 0.2, 0.2, 0.2);
					}
				}
			}

			return age < 240;
		});
		return true;
	}

	// ================================================================== 3. Боевая магия

	/** Болты Балтакка: три самонаводящиеся сферы энергии. */
	private static boolean bolts(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 look = player.getViewVector(1f);
		Vec3[] basis = Fx.basis(look);
		Vec3 hand = Fx.hand(player);
		Set<UUID> struck = new HashSet<>();

		for (int i = -1; i <= 1; i++) {
			Vec3[] bolt = {hand, look.add(basis[0].scale(i * 0.18)).normalize().scale(1.3)};

			Tasks.start(age -> {
				Vec3 pos = bolt[0];
				Vec3 heading = bolt[1];
				Vec3 vel = heading;

				// Самонаведение на ближайшую цель впереди.
				LivingEntity target = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(20),
								e -> Fx.isTarget(e, player) && Fx.angle(heading, e.getBoundingBox().getCenter().subtract(pos)) < 60)
						.stream()
						.min(Comparator.<LivingEntity>comparingInt(e -> Fx.hostile(e) ? 0 : 1).thenComparingDouble(e -> e.distanceToSqr(pos)))
						.orElse(null);

				if (target != null && age > 2) {
					Vec3 to = target.getBoundingBox().getCenter().subtract(pos).normalize();
					vel = vel.normalize().scale(0.7).add(to.scale(0.3)).normalize().scale(1.3);
					bolt[1] = vel;
				}

				Vec3 next = pos.add(vel);
				BlockHitResult hit = level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
				LivingEntity struckEntity = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, next).inflate(0.4), e -> Fx.isTarget(e, player)
								&& e.getBoundingBox().inflate(0.4).clip(pos, next).isPresent())
						.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(pos))).orElse(null);

				level.sendParticles(ORANGE, true, false, pos.x, pos.y, pos.z, 3, 0.08, 0.08, 0.08, 0);
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, true, false, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0.05);

				if (age % 2 == 0) {
					level.sendParticles(ParticleTypes.END_ROD, true, false, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
				}

				if (struckEntity != null || hit.getType() != HitResult.Type.MISS || age > 50) {
					Vec3 at = struckEntity != null ? struckEntity.getBoundingBox().getCenter() : hit.getLocation();

					if (struckEntity != null) {
						Fx.hurt(level, player, struckEntity, struck.add(struckEntity.getUUID()) ? 9f : 5f);
					}

					for (LivingEntity e : Fx.around(level, player, at, 2.5)) {
						if (e != struckEntity) {
							Fx.hurt(level, player, e, 3f);
						}
					}

					level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0, 0, 0, 0);
					level.sendParticles(ORANGE, at.x, at.y, at.z, 20, 0.4, 0.4, 0.4, 0);
					Fx.sound(level, at, SoundEvents.FIRECHARGE_USE, 0.7f, 1.6f);
					return false;
				}

				bolt[0] = next;
				return true;
			});
		}

		Fx.sound(level, player, SoundEvents.EVOKER_CAST_SPELL, 1.0f, 1.4f);
		return true;
	}

	/** Огненный кнут Ватумба: хлещет цель, поджигает и подтягивает к тебе. */
	private static boolean whip(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Fx.Ray ray = Fx.ray(level, player, 16, 0.6);
		LivingEntity target = ray.entity();
		Vec3 hand = Fx.hand(player);
		Vec3 end = target != null ? target.getBoundingBox().getCenter() : ray.end();

		// Волнистая плеть из огня и золота.
		Vec3 delta = end.subtract(hand);
		Vec3[] basis = Fx.basis(delta);
		int steps = (int) (delta.length() * 4);

		for (int i = 0; i <= steps; i++) {
			double t = i / (double) steps;
			double wave = Math.sin(t * Math.PI * 4) * 0.35 * (1 - t);
			Vec3 p = hand.add(delta.scale(t)).add(basis[0].scale(wave));
			level.sendParticles(i % 3 == 0 ? ParticleTypes.FLAME : ORANGE, true, false, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}

		Fx.sound(level, player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.6f);

		if (target == null) {
			return true;
		}

		Fx.hurt(level, player, target, 7f);
		target.igniteForSeconds(5);
		Vec3 pull = player.position().subtract(target.position());
		Fx.push(target, pull, Math.min(2.2, 0.6 + pull.length() * 0.12), 0.45);
		level.sendParticles(ParticleTypes.FLAME, end.x, end.y, end.z, 15, 0.3, 0.3, 0.3, 0.05);
		Fx.sound(level, end, SoundEvents.FIRECHARGE_USE, 1.0f, 0.8f);
		return true;
	}

	// ================================================================== 4. Сковывающие чары

	/** Багровые узы Циттората: цель скована на 10 секунд (боссы — на 3). */
	private static boolean bands(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Fx.Ray ray = Fx.ray(level, player, 40, 0.4);
		LivingEntity target = ray.entity();

		if (target == null) {
			return noTarget(player);
		}

		int ticks = Fx.isBoss(target) ? 60 : 200;

		if (target instanceof Player victim) {
			victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 9));
			victim.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, ticks, 4));
			victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 4));
		} else {
			Freeze.freeze(target, level.getGameTime() + ticks);
		}

		Fx.line(level, CRIMSON, Fx.hand(player), target.getBoundingBox().getCenter(), 0.3);
		Fx.sound(level, target.position(), SoundEvents.CHAIN_PLACE, 1.5f, 0.6f);
		Fx.sound(level, player, SoundEvents.EVOKER_CAST_SPELL, 0.8f, 0.8f);

		Tasks.start(age -> {
			if (target.isRemoved() || !target.isAlive()) {
				return false;
			}

			if (age % 2 == 0) {
				ServerLevel lv = (ServerLevel) target.level();
				double h = target.getBbHeight();
				double w = Math.max(0.6, target.getBbWidth() * 0.8);

				for (int band = 0; band < 3; band++) {
					double y = h * (0.25 + band * 0.3);
					double tilt = Math.sin(age * 0.1 + band) * 0.25;
					Vec3 c = target.position().add(0, y, 0);
					Fx.circle(lv, band == 1 ? CRIMSON_DARK : CRIMSON, c, new Vec3(tilt, 1, -tilt), w, 16, age * 0.2 * (band % 2 == 0 ? 1 : -1));
				}
			}

			return age < ticks;
		});
		return true;
	}

	/** Ветра Ватумба: ураган в конусе отбрасывает всех и отражает снаряды. */
	private static boolean winds(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1f);

		for (LivingEntity e : Fx.cone(level, player, 14, 40)) {
			double d = e.distanceTo(player);
			Fx.push(e, look, 3.2 * (1.1 - d / 16), 0.8);
			Fx.hurt(level, player, e, 2f);
			e.clearFire();
		}

		for (Entity e : level.getEntities(player, player.getBoundingBox().inflate(14),
				e -> e instanceof Projectile && Fx.angle(look, e.position().subtract(eye)) < 45)) {
			e.setDeltaMovement(look.scale(1.8));
			e.hurtMarked = true;
		}

		player.clearFire();

		for (int i = 1; i <= 7; i++) {
			Vec3 p = eye.add(look.scale(i * 1.8));
			level.sendParticles(ParticleTypes.GUST, true, false, p.x, p.y, p.z, 1, 0.15 * i, 0.15 * i, 0.15 * i, 0);
			level.sendParticles(ParticleTypes.CLOUD, true, false, p.x, p.y, p.z, 6, 0.2 * i, 0.2 * i, 0.2 * i, 0.05);
		}

		Fx.sound(level, player, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.5f, 0.5f);
		Fx.sound(level, player, SoundEvents.ELYTRA_FLYING, 0.6f, 1.5f);
		return true;
	}

	// ================================================================== 5. Око Агамотто

	/** Перемотка: возвращает блоки, сломанные рядом за последние 2 минуты, и чинит всё твоё снаряжение. */
	private static boolean rewindWorld(ServerLevel level, ServerPlayer player, ItemStack stack) {
		eyeGlyph(level, player);
		Vec3 c = player.position();
		Map<BlockPos, BlockState> restore = new HashMap<>();

		// Идём от новых к старым: для каждой клетки остаётся самое старое состояние.
		List<MagicState.BrokenBlock> list = new ArrayList<>(MagicState.BROKEN);

		for (int i = list.size() - 1; i >= 0; i--) {
			MagicState.BrokenBlock b = list.get(i);

			if (b.dimension().equals(level.dimension()) && Vec3.atCenterOf(b.pos()).distanceTo(c) <= 32) {
				restore.put(b.pos(), b.state());
			}
		}

		int restored = 0;

		for (Map.Entry<BlockPos, BlockState> e : restore.entrySet()) {
			BlockPos pos = e.getKey();
			BlockState now = level.getBlockState(pos);

			if ((now.isAir() || now.canBeReplaced()) && e.getValue().canSurvive(level, pos)) {
				level.setBlock(pos, e.getValue(), Block.UPDATE_ALL);

				if (restored++ % 2 == 0) {
					level.sendParticles(EYE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0);
				}
			}
		}

		MagicState.BROKEN.removeIf(b -> b.dimension().equals(level.dimension()) && restore.containsKey(b.pos()));

		// Снаряжение — как новое.
		int repaired = 0;
		Inventory inventory = player.getInventory();

		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack item = inventory.getItem(i);

			if (item.isDamageableItem() && item.isDamaged()) {
				item.setDamageValue(0);
				repaired++;
			}
		}

		Fx.sound(level, player, SoundEvents.BELL_RESONATE, 1.2f, 1.8f);
		Fx.sound(level, player, SoundEvents.BEACON_POWER_SELECT, 1.0f, 0.6f);
		player.sendOverlayMessage(Component.translatable("message.solorust.magic.rewind", restored, repaired).withStyle(ChatFormatting.GREEN));
		return true;
	}

	/** Ускорение времени: 15 секунд вокруг тебя всё растёт и взрослеет в десятки раз быстрее. */
	private static boolean acceleration(ServerLevel level, ServerPlayer player, ItemStack stack) {
		eyeGlyph(level, player);
		BlockPos center = player.blockPosition();
		Fx.sound(level, player, SoundEvents.BELL_RESONATE, 1.2f, 1.2f);

		Tasks.start(age -> {
			RandomSource random = level.getRandom();

			for (int i = 0; i < 160; i++) {
				BlockPos pos = center.offset(random.nextInt(19) - 9, random.nextInt(9) - 4, random.nextInt(19) - 9);
				BlockState state = level.getBlockState(pos);

				if (state.isRandomlyTicking()) {
					state.randomTick(level, pos, random);

					if (random.nextInt(12) == 0) {
						level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 1, 0.2, 0.2, 0.2, 0);
					}
				}
			}

			if (age % 20 == 0) {
				for (AgeableMob mob : level.getEntitiesOfClass(AgeableMob.class, new AABB(center).inflate(10))) {
					int a = mob.getAge();

					if (a < 0) {
						mob.setAge(Math.min(0, a + 1200));
					} else if (a > 0) {
						mob.setAge(Math.max(0, a - 1200));
					}
				}
			}

			if (age % 3 == 0) {
				double r = 9;
				Fx.circle(level, EYE, Vec3.atBottomCenterOf(center).add(0, 0.2, 0), new Vec3(0, 1, 0), r, 50, age * 0.08);
				Fx.circle(level, EYE_LIGHT, Vec3.atBottomCenterOf(center).add(0, 0.25, 0), new Vec3(0, 1, 0), r * 0.6, 30, -age * 0.12);
			}

			if (age % 40 == 0) {
				Fx.sound(level, Vec3.atCenterOf(center), SoundEvents.STONE_BUTTON_CLICK_ON, 0.5f, 2.0f);
			}

			return age < 300;
		});
		return true;
	}

	/** Зелёная мандала Ока Агамотто вокруг запястья. */
	private static void eyeGlyph(ServerLevel level, ServerPlayer player) {
		Tasks.start(age -> {
			Vec3 look = player.getViewVector(1f);
			Vec3 c = Fx.hand(player).add(look.scale(0.4));
			Fx.mandala(player.level(), EYE, EYE_LIGHT, c, look, 0.55 + Math.min(age, 6) * 0.05, age * 0.3);
			return age < 24;
		});
	}

	// ================================================================== 6. Астрал и зеркала

	/** Астральная проекция: душа покидает тело на 20 секунд (режим наблюдателя), затем возвращается. */
	private static boolean astral(ServerLevel level, ServerPlayer player, ItemStack stack) {
		if (player.getAttached(MagicState.ASTRAL) != null || player.gameMode() == GameType.SPECTATOR) {
			return false;
		}

		long until = level.getGameTime() + 400;
		player.setAttached(MagicState.ASTRAL, new MagicState.AstralState(GlobalPos.of(level.dimension(), player.blockPosition()), player.gameMode().getName(), until));
		level.sendParticles(ASTRAL, player.getX(), player.getY(1), player.getZ(), 40, 0.3, 0.8, 0.3, 0);
		level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY(1), player.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
		Fx.sound(level, player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0f, 0.8f);
		player.setGameMode(GameType.SPECTATOR);
		player.sendOverlayMessage(Component.translatable("message.solorust.magic.astral").withStyle(ChatFormatting.AQUA));
		return true;
	}

	static void astralTick(ServerPlayer player, long now) {
		MagicState.AstralState state = player.getAttached(MagicState.ASTRAL);

		if (state == null) {
			return;
		}

		long left = state.until() - now;

		if (left <= 0 || player.gameMode() != GameType.SPECTATOR) {
			endAstral(player);
			return;
		}

		// Тело остаётся на месте — светится контуром души.
		if (now % 5 == 0 && player.level().dimension().equals(state.body().dimension())) {
			BlockPos b = state.body().pos();
			player.level().sendParticles(ASTRAL, b.getX() + 0.5, b.getY() + 0.9, b.getZ() + 0.5, 6, 0.2, 0.6, 0.2, 0);
		}

		if (left % 20 == 0) {
			int seconds = (int) (left / 20);
			Integer shown = ASTRAL_SECONDS.put(player.getUUID(), seconds);

			if (shown == null || shown != seconds) {
				player.sendOverlayMessage(Component.translatable("message.solorust.magic.astral_left", seconds).withStyle(ChatFormatting.AQUA));
			}
		}
	}

	static void endAstral(ServerPlayer player) {
		ASTRAL_SECONDS.remove(player.getUUID());
		MagicState.AstralState state = player.removeAttached(MagicState.ASTRAL);

		if (state == null) {
			return;
		}

		ServerLevel body = player.level().getServer().getLevel(state.body().dimension());

		if (body != null) {
			BlockPos b = state.body().pos();
			player.teleportTo(body, b.getX() + 0.5, b.getY(), b.getZ() + 0.5, Set.of(), player.getYRot(), player.getXRot(), false);
		}

		player.setGameMode(MagicState.mode(state.gameMode()));
		player.fallDistance = 0;
		player.level().sendParticles(ASTRAL, player.getX(), player.getY(1), player.getZ(), 40, 0.3, 0.8, 0.3, 0);
		Fx.sound(player.level(), player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0f, 1.4f);
	}

	/** Зеркальное измерение: 15 секунд ты вне реальности — невидим, мобы теряют тебя и не могут ранить. */
	private static boolean mirror(ServerLevel level, ServerPlayer player, ItemStack stack) {
		long until = level.getGameTime() + 300;
		MagicState.MIRROR.put(player.getUUID(), until);
		player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 300, 0, false, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.SPEED, 300, 0, false, false, true));
		MagicState.dropAggro(level, player);

		Vec3 start = player.position();
		Tasks.start(age -> {
			ServerLevel lv = player.level();

			// Трещины реальности в момент входа: кольца-осколки разлетаются в стороны.
			if (age < 10) {
				Fx.circle(lv, MIRROR, start.add(0, 1, 0), new Vec3(0, 1, 0), 0.5 + age * 0.6, 24 + age * 4, age * 0.4);
				Fx.circle(lv, MIRROR_DARK, start.add(0, 1, 0), new Vec3(1, 0, 0), 0.5 + age * 0.5, 20 + age * 3, -age * 0.4);
			}

			// Калейдоскоп вокруг игрока — видно только по частицам.
			if (age % 4 == 0 && !player.isRemoved()) {
				double a = age * 0.15;

				for (int i = 0; i < 6; i++) {
					double b = a + i * Math.PI / 3;
					Vec3 p = player.position().add(Math.cos(b) * 1.4, 1 + Math.sin(b * 2) * 0.5, Math.sin(b) * 1.4);
					lv.sendParticles(i % 2 == 0 ? MIRROR : MIRROR_DARK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
				}
			}

			if (age == 299) {
				lv.sendParticles(MIRROR, player.getX(), player.getY(1), player.getZ(), 40, 0.4, 0.8, 0.4, 0);
				Fx.sound(lv, player, SoundEvents.GLASS_BREAK, 0.6f, 1.4f);
			}

			return age < 300 && MagicState.MIRROR.containsKey(player.getUUID());
		});

		Fx.sound(level, player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.2f, 0.6f);
		Fx.sound(level, player, SoundEvents.GLASS_BREAK, 0.8f, 0.5f);
		player.sendOverlayMessage(Component.translatable("message.solorust.magic.mirror").withStyle(ChatFormatting.LIGHT_PURPLE));
		return true;
	}

	// ================================================================== 7. Плащ левитации

	/** Рывок плаща: бросок вперёд, сбивающий всех на пути. */
	private static boolean cloakDash(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 look = player.getViewVector(1f);
		player.setDeltaMovement(look.scale(2.6).add(0, 0.25, 0));
		player.hurtMarked = true;
		player.fallDistance = 0;
		MagicState.FALL_SAFE.put(player.getUUID(), level.getGameTime() + 80);
		Set<UUID> hit = new HashSet<>();
		Fx.sound(level, player, SoundEvents.ELYTRA_FLYING, 0.8f, 1.6f);
		Fx.sound(level, player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);

		Tasks.start(age -> {
			ServerLevel lv = player.level();
			lv.sendParticles(CLOAK, player.getX(), player.getY(0.6), player.getZ(), 6, 0.3, 0.4, 0.3, 0);

			for (LivingEntity e : Fx.around(lv, player, player.getBoundingBox().getCenter(), 2.0)) {
				if (hit.add(e.getUUID())) {
					Fx.hurt(lv, player, e, 8f);
					Fx.push(e, look, 1.6, 0.5);
					lv.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY(0.5), e.getZ(), 1, 0, 0, 0, 0);
				}
			}

			return age < 10;
		});
		return true;
	}

	private static boolean cloakFlight(ServerLevel level, ServerPlayer player, ItemStack stack) {
		return toggleFlight(level, player, CLOAK);
	}

	static boolean toggleFlight(ServerLevel level, ServerPlayer player, DustParticleOptions color) {
		if (player.isCreative() || player.isSpectator()) {
			player.sendOverlayMessage(Component.translatable("message.solorust.magic.flight_creative").withStyle(ChatFormatting.GRAY));
			return false;
		}

		boolean on = MagicState.toggleFlight(player);
		level.sendParticles(color, player.getX(), player.getY(0.5), player.getZ(), 30, 0.4, 0.6, 0.4, 0);
		Fx.sound(level, player, on ? SoundEvents.ELYTRA_FLYING : SoundEvents.BEACON_DEACTIVATE, 0.6f, on ? 1.4f : 1.6f);
		player.sendOverlayMessage(Component.translatable(on ? "message.solorust.magic.flight_on" : "message.solorust.magic.flight_off")
				.withStyle(on ? ChatFormatting.AQUA : ChatFormatting.GRAY));
		return true;
	}

	// ================================================================== 8. Пламя и исцеление

	/** Пламя Фалтина: огненный поток в конусе, поджигает и обжигает. */
	private static boolean flames(ServerLevel level, ServerPlayer player, ItemStack stack) {
		Vec3 hand = Fx.hand(player);
		Vec3 look = player.getViewVector(1f);
		RandomSource random = player.getRandom();

		for (LivingEntity e : Fx.cone(level, player, 12, 30)) {
			Fx.hurt(level, player, e, 7f);
			e.igniteForSeconds(8);
		}

		Tasks.start(age -> {
			Vec3 from = Fx.hand(player);
			Vec3 dir = player.getViewVector(1f);

			for (int i = 0; i < 25; i++) {
				Vec3 v = dir.add((random.nextDouble() - 0.5) * 0.35, (random.nextDouble() - 0.5) * 0.35, (random.nextDouble() - 0.5) * 0.35).normalize();
				level.sendParticles(i % 5 == 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, true, false, from.x, from.y, from.z, 0, v.x, v.y, v.z, 0.9);
			}

			return age < 6;
		});

		level.sendParticles(ParticleTypes.LAVA, hand.x + look.x * 3, hand.y + look.y * 3, hand.z + look.z * 3, 6, 0.5, 0.5, 0.5, 0);
		Fx.sound(level, player, SoundEvents.BLAZE_SHOOT, 1.2f, 0.6f);
		Fx.sound(level, player, SoundEvents.FIRECHARGE_USE, 1.0f, 0.5f);
		return true;
	}

	/** Исцеление Вишанти: полное здоровье, сытость, снятие всех недугов; лечит и союзников рядом. */
	private static boolean heal(ServerLevel level, ServerPlayer player, ItemStack stack) {
		player.setHealth(player.getMaxHealth());
		player.getFoodData().setFoodLevel(20);
		player.getFoodData().setSaturation(20f);
		player.clearFire();
		Fx.clearHarmful(player);
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
		player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 1));

		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(10),
				e -> e != player && e.isAlive() && (e instanceof Player || e instanceof TamableAnimal t && t.isOwnedBy(player) || MagicState.isMinionOf(e, player)))) {
			e.heal(e.getMaxHealth());
			e.clearFire();
			Fx.clearHarmful(e);
			level.sendParticles(ParticleTypes.HEART, e.getX(), e.getY(1), e.getZ(), 5, 0.3, 0.3, 0.3, 0);
		}

		Vec3 c = player.position();
		Tasks.start(age -> {
			Fx.circle(level, HEAL, c.add(0, age * 0.12, 0), new Vec3(0, 1, 0), 1.2, 20, age * 0.4);
			return age < 18;
		});
		level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY(1), player.getZ(), 10, 0.5, 0.5, 0.5, 0);
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY(1), player.getZ(), 30, 0.5, 0.8, 0.5, 0.3);
		Fx.sound(level, player, SoundEvents.PLAYER_LEVELUP, 1.0f, 1.4f);
		Fx.sound(level, player, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0f, 1.6f);
		return true;
	}

	// ================================================================== 9. Высшая магия

	/** Кара Тёмного измерения: мандала в небе и лучи тьмы в каждого врага вокруг (до 24). */
	private static boolean darkDimension(ServerLevel level, ServerPlayer player, ItemStack stack) {
		List<LivingEntity> targets = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(28),
				e -> Fx.isTarget(e, player) && Fx.hostile(e)));

		if (targets.isEmpty()) {
			return noTarget(player);
		}

		targets.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
		List<LivingEntity> queue = targets.subList(0, Math.min(24, targets.size()));
		Vec3 sky = player.position().add(0, 14, 0);
		Fx.sound(level, player, SoundEvents.EVOKER_PREPARE_ATTACK, 2.0f, 0.5f);
		Fx.sound(level, player, SoundEvents.BEACON_ACTIVATE, 2.0f, 0.4f);

		Tasks.start(age -> {
			if (age % 2 == 0) {
				Fx.mandala(level, DARK, DARK_ACCENT, sky, new Vec3(0, 1, 0), Math.min(7, 1 + age * 0.4), age * 0.06);
			}

			int index = (age - 24) / 3;

			if (age >= 24 && (age - 24) % 3 == 0 && index < queue.size()) {
				LivingEntity e = queue.get(index);

				if (e.isAlive() && !e.isRemoved()) {
					Vec3 at = e.getBoundingBox().getCenter();
					Fx.line(level, DARK, sky, at, 0.4);
					Fx.line(level, ParticleTypes.WITCH, sky, at, 1.2);
					Fx.hurt(level, player, e, 18f);
					e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1));
					level.sendParticles(DARK_ACCENT, at.x, at.y, at.z, 20, 0.4, 0.5, 0.4, 0);
					level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 20, 0.3, 0.5, 0.3, 0.1);
					Fx.sound(level, at, SoundEvents.EVOKER_CAST_SPELL, 1.0f, 0.5f);
				}
			}

			if (age == 24) {
				Fx.sound(level, player, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.5f, 0.5f);
			}

			return age < 24 + queue.size() * 3 + 20;
		});
		return true;
	}

	/** Петля Дормамму: минуту любая смерть возвращает тебя в момент заклинания. */
	private static boolean timeLoop(ServerLevel level, ServerPlayer player, ItemStack stack) {
		MagicState.LOOP.put(player.getUUID(), MagicState.Snapshot.of(player));
		MagicState.LOOP_UNTIL.put(player.getUUID(), level.getGameTime() + 1200);
		eyeGlyph(level, player);
		Fx.circle(level, EYE, player.position().add(0, 0.2, 0), new Vec3(0, 1, 0), 2.5, 40, 0);
		Fx.sound(level, player, SoundEvents.BELL_RESONATE, 1.5f, 0.7f);
		Fx.sound(level, player, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.0f, 0.8f);
		player.sendOverlayMessage(Component.translatable("message.solorust.magic.loop").withStyle(ChatFormatting.GREEN));
		return true;
	}

	/** Вызывается при смерти игрока. true — смерть отменена петлёй. */
	static boolean dormammu(ServerPlayer player) {
		MagicState.Snapshot snap = MagicState.LOOP.get(player.getUUID());

		if (snap == null) {
			return false;
		}

		ServerLevel dest = player.level().getServer().getLevel(snap.dimension());

		if (dest == null) {
			return false;
		}

		player.setHealth(Math.max(snap.health(), player.getMaxHealth() * 0.5f));
		player.getFoodData().setFoodLevel(Math.max(player.getFoodData().getFoodLevel(), snap.food()));
		player.clearFire();
		Fx.clearHarmful(player);
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
		player.teleportTo(dest, snap.pos().x, snap.pos().y, snap.pos().z, Set.of(), snap.yRot(), snap.xRot(), false);
		player.fallDistance = 0;
		player.setDeltaMovement(Vec3.ZERO);
		player.hurtMarked = true;

		dest.sendParticles(EYE, player.getX(), player.getY(1), player.getZ(), 60, 0.5, 0.9, 0.5, 0);
		dest.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY(1), player.getZ(), 30, 0.5, 0.8, 0.5, 0.3);
		Fx.sound(dest, player, SoundEvents.BELL_RESONATE, 1.5f, 1.6f);
		Fx.sound(dest, player, SoundEvents.TOTEM_USE, 0.6f, 1.6f);
		player.sendSystemMessage(Component.translatable("message.solorust.magic.dormammu").withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC));
		return true;
	}
}
