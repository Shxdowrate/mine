package com.shxdowrate.solorust.suit;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.shxdowrate.solorust.SoloRust;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Способности «необычных» костюмов: ядра «Сингулярности», магия архимага, сила «Апекса»
 * и инструменты рабочих костюмов. Боевые системы Mk-серии — в {@link SuitWeapons}.
 */
public final class SuitAbilities {
	private static final DustParticleOptions GOLD = new DustParticleOptions(0xFFB347, 0.9f);
	private static final DustParticleOptions PORTAL_DUST = new DustParticleOptions(0xFF8C1A, 1.2f);
	private static final DustParticleOptions HEAT = new DustParticleOptions(0xFF2200, 0.6f);
	private static final DustParticleOptions ASH = new DustParticleOptions(0x4A4A4A, 1.6f);
	private static final DustParticleOptions MIND = new DustParticleOptions(0xFFD23D, 0.8f);

	// ------------------------------------------------------------------ состояние

	/** Что держит гравитационный захват / руки «Апекса». */
	private static final Map<UUID, List<Entity>> HELD = new HashMap<>();
	/** Мобы под контролем «Разума»: до какого тика. */
	private static final Map<Mob, Long> MIND_CONTROLLED = new HashMap<>();
	/** Скрытые «Призраком» игроки: до какого тика. */
	private static final Map<UUID, Long> CLOAKED = new HashMap<>();
	/** Пары порталов архимага. */
	private static final Map<UUID, PortalPair> PORTALS = new HashMap<>();
	private static final Map<UUID, Long> PORTAL_COOLDOWN = new HashMap<>();
	/** Сколько тиков осталось астральной форме. */
	private static final Map<UUID, Integer> ASTRAL_TIMER = new HashMap<>();
	/** История положения для «Петли времени»: раз в секунду, последние 12 секунд. */
	private static final Map<UUID, Deque<Snapshot>> HISTORY = new HashMap<>();

	private record Snapshot(ResourceKey<Level> dimension, Vec3 pos, float yRot, float xRot, float health, int food) {
	}

	private static final class PortalPair {
		final ServerLevel level;
		final Vec3 a;
		final Vec3 aNormal;
		Vec3 b;
		Vec3 bNormal;
		long expires;

		PortalPair(ServerLevel level, Vec3 a, Vec3 aNormal, long expires) {
			this.level = level;
			this.a = a;
			this.aNormal = aNormal;
			this.expires = expires;
		}
	}

	/** Где осталось тело и какой был режим игры — чтобы вернуть игрока даже после перезахода. */
	public record AstralState(GlobalPos body, String gameMode) {
		public static final Codec<AstralState> CODEC = RecordCodecBuilder.create(i -> i.group(
				GlobalPos.CODEC.fieldOf("body").forGetter(AstralState::body),
				Codec.STRING.fieldOf("mode").forGetter(AstralState::gameMode)
		).apply(i, AstralState::new));
	}

	public static final AttachmentType<AstralState> ASTRAL = AttachmentRegistry.<AstralState>create(SoloRust.id("astral"),
			builder -> builder.persistent(AstralState.CODEC));

	private SuitAbilities() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(SuitAbilities::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			HELD.clear();
			MIND_CONTROLLED.clear();
			CLOAKED.clear();
			PORTALS.clear();
			PORTAL_COOLDOWN.clear();
			ASTRAL_TIMER.clear();
			HISTORY.clear();
		});

		// Вышел из игры в астрале — при входе возвращаем в тело.
		ServerPlayerEvents.JOIN.register(player -> {
			if (player.getAttached(ASTRAL) != null) {
				endAstral(player);
			}
		});

		// «Ядро жизни» спасает от смерти, если хватает энергии.
		ServerPlayerEvents.ALLOW_DEATH.register((player, source, amount) -> {
			SuitTier tier = SuitTier.worn(player);

			if (tier != SuitTier.SINGULARITY || !SuitEnergy.use(player, tier, 1000)) {
				return true;
			}

			player.setHealth(player.getMaxHealth() * 0.5f);
			clearHarmful(player);
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
			ServerLevel level = player.level();
			level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY(1), player.getZ(), 80, 0.6, 1, 0.6, 0.4);
			SuitWeapons.sound(level, player, SoundEvents.TOTEM_USE, 1.0f, 1.0f);
			player.sendOverlayMessage(Component.translatable("message.solorust.ability.revived").withStyle(ChatFormatting.GREEN));
			return false;
		});
	}

	// ================================================================== общий тик

	private static void tick(MinecraftServer server) {
		long time = server.overworld().getGameTime();

		portals(time);

		if (time % 5 == 0) {
			mindControl(time);
			cloak(server, time);
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			UUID id = player.getUUID();

			if (ASTRAL_TIMER.containsKey(id)) {
				astralTick(player);
			}

			if (time % 20 == 0 && SuitTier.worn(player) == SuitTier.ARCHMAGE && !ASTRAL_TIMER.containsKey(id)) {
				Deque<Snapshot> history = HISTORY.computeIfAbsent(id, k -> new ArrayDeque<>());
				history.addLast(new Snapshot(player.level().dimension(), player.position(), player.getYRot(), player.getXRot(),
						player.getHealth(), player.getFoodData().getFoodLevel()));

				while (history.size() > 12) {
					history.removeFirst();
				}
			}
		}
	}

	// ================================================================== «Сингулярность»

	/** Захват: гравитационное ядро держит до 5 целей, руки «Апекса» — одну цель рядом. */
	public static void grip(ServerLevel level, ServerPlayer player, SuitWeapon weapon, int ticks) {
		boolean single = weapon == SuitWeapon.SUPER_GRAB;
		List<Entity> held = HELD.get(player.getUUID());

		if (held == null) {
			held = new ArrayList<>();

			if (single) {
				SuitWeapons.Ray ray = SuitWeapons.ray(level, player, player.getEyePosition(), 7, 0.6);

				if (ray.entity() != null) {
					held.add(ray.entity());
				}
			} else {
				List<LivingEntity> view = SuitWeapons.targetsInView(level, player, 32, 22);
				held.addAll(view.subList(0, Math.min(5, view.size())));
			}

			HELD.put(player.getUUID(), held);

			if (!held.isEmpty()) {
				SuitWeapons.sound(level, player, single ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.BEACON_POWER_SELECT, 0.8f, single ? 0.8f : 0.6f);
			}
		}

		Vec3 look = player.getViewVector(1f);
		Vec3 point = player.getEyePosition().add(look.scale(single ? 2.5 + player.getBbWidth() : 6));

		for (int i = 0; i < held.size(); i++) {
			Entity e = held.get(i);

			if (e.isRemoved() || e.level() != level) {
				continue;
			}

			double a = held.size() > 1 ? i * Math.PI * 2 / held.size() + ticks * 0.08 : 0;
			Vec3 spot = point.add(Math.cos(a) * (held.size() > 1 ? 2 : 0), Math.sin(a) * (held.size() > 1 ? 1 : 0), 0);
			Vec3 pull = spot.subtract(e.position().add(0, e.getBbHeight() / 2, 0)).scale(0.35);
			e.setDeltaMovement(pull);
			e.hurtMarked = true;
			e.fallDistance = 0;

			if (!single && ticks % 2 == 0) {
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, e.getX(), e.getY(0.5), e.getZ(), 3, 0.3, 0.4, 0.3, 0.02);
			}
		}
	}

	/** Отпустили ПКМ — бросаем то, что держали. */
	public static void throwHeld(ServerLevel level, ServerPlayer player, SuitTier tier, SuitWeapon weapon, float charge) {
		List<Entity> held = HELD.remove(player.getUUID());

		if (held == null || held.isEmpty()) {
			return;
		}

		Vec3 look = player.getViewVector(1f);
		double speed = weapon == SuitWeapon.SUPER_GRAB ? 2.0 + 2.5 * charge : 1.4 + 2.6 * charge;

		for (Entity e : held) {
			if (e.isRemoved()) {
				continue;
			}

			e.setDeltaMovement(look.scale(speed).add(0, 0.35, 0));
			e.hurtMarked = true;

			if (e instanceof LivingEntity living) {
				SuitWeapons.hurt(level, player, living, (3f + 8f * charge) * tier.power);
				e.setDeltaMovement(look.scale(speed).add(0, 0.35, 0));
			}
		}

		SuitWeapons.sound(level, player, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.0f, 0.7f);
	}

	public static void releaseHeld(ServerPlayer player) {
		HELD.remove(player.getUUID());
	}

	public static void timeDilation(ServerLevel level, ServerPlayer player) {
		AABB area = player.getBoundingBox().inflate(24);

		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, e -> SuitWeapons.isTarget(e, player) && !(e instanceof Player))) {
			e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 4));
			e.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 200, 2));
			e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
		}

		for (Entity e : level.getEntities(player, area, e -> e instanceof Projectile)) {
			e.setDeltaMovement(e.getDeltaMovement().scale(0.05));
			e.hurtMarked = true;
		}

		player.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 1));

		for (int i = 0; i < 60; i++) {
			double a = i / 60.0 * Math.PI * 2;
			level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY(0.5), player.getZ(), 0, Math.cos(a), 0, Math.sin(a), 0.8);
		}

		SuitWeapons.sound(level, player, SoundEvents.BEACON_DEACTIVATE, 1.5f, 0.4f);
		SuitWeapons.sound(level, player, SoundEvents.BELL_RESONATE, 1.0f, 0.6f);
		player.sendOverlayMessage(Component.translatable("message.solorust.ability.time").withStyle(ChatFormatting.GREEN));
	}

	public static void spaceJump(ServerLevel level, ServerPlayer player) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getViewVector(1f).scale(96));
		BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 dest;

		if (hit.getType() == HitResult.Type.BLOCK) {
			BlockPos pos = hit.getBlockPos().relative(hit.getDirection());

			if (hit.getDirection() == Direction.DOWN) {
				pos = pos.below();
			}

			dest = Vec3.atBottomCenterOf(pos);
		} else {
			dest = end.subtract(0, 1.6, 0);
		}

		Vec3 from = player.position();
		level.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1, from.z, 60, 0.4, 0.9, 0.4, 0.4);
		player.teleportTo(dest.x, dest.y, dest.z);
		player.fallDistance = 0;
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, dest.x, dest.y + 1, dest.z, 60, 0.4, 0.9, 0.4, 0.1);
		level.playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.8f);
		SuitWeapons.sound(level, player, SoundEvents.CHORUS_FRUIT_TELEPORT, 1.0f, 1.2f);
	}

	public static void matterShift(ServerLevel level, ServerPlayer player) {
		SuitWeapons.Ray ray = SuitWeapons.ray(level, player, player.getEyePosition(), 48, 0.3);
		Vec3 c = ray.end();
		List<EntityType<? extends Mob>> forms = List.of(EntityType.CHICKEN, EntityType.RABBIT, EntityType.SHEEP, EntityType.PIG);
		int changed = 0;

		for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(c, c).inflate(6), m -> m instanceof Enemy && m.isAlive() && m.getMaxHealth() <= 100)) {
			EntityType<? extends Mob> form = forms.get(player.getRandom().nextInt(forms.size()));
			form.spawn(level, mob.blockPosition(), EntitySpawnReason.MOB_SUMMONED);
			level.sendParticles(ParticleTypes.WAX_ON, mob.getX(), mob.getY(0.5), mob.getZ(), 25, 0.4, 0.5, 0.4, 0.5);
			mob.discard();
			changed++;
		}

		// Лава застывает, огонь гаснет.
		BlockPos center = BlockPos.containing(c);

		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-4, -4, -4), center.offset(4, 4, 4))) {
			BlockState state = level.getBlockState(pos);
			FluidState fluid = state.getFluidState();

			if (state.is(Blocks.LAVA)) {
				level.setBlockAndUpdate(pos, fluid.isSource() ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
			} else if (state.is(BlockTags.FIRE)) {
				level.removeBlock(pos, false);
			}
		}

		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, c.x, c.y, c.z, 60, 2, 1, 2, 0.3);
		SuitWeapons.sound(level, player, SoundEvents.ZOMBIE_VILLAGER_CURE, 0.8f, 1.6f);
		player.sendOverlayMessage(Component.translatable("message.solorust.ability.matter", changed).withStyle(ChatFormatting.LIGHT_PURPLE));
	}

	public static void mindLink(ServerLevel level, ServerPlayer player) {
		long until = level.getGameTime() + 600;
		int count = 0;

		for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(20), m -> m instanceof Enemy && m.isAlive() && m.getMaxHealth() <= 100)) {
			MIND_CONTROLLED.put(mob, until);
			mob.setTarget(null);
			level.sendParticles(MIND, mob.getX(), mob.getY(1.1), mob.getZ(), 12, 0.3, 0.2, 0.3, 0.0);
			count++;
		}

		SuitWeapons.sound(level, player, SoundEvents.EVOKER_PREPARE_ATTACK, 1.0f, 1.4f);
		player.sendOverlayMessage(Component.translatable("message.solorust.ability.mind", count).withStyle(ChatFormatting.YELLOW));
	}

	private static void mindControl(long time) {
		for (Iterator<Map.Entry<Mob, Long>> it = MIND_CONTROLLED.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Mob, Long> entry = it.next();
			Mob mob = entry.getKey();

			if (mob.isRemoved() || !mob.isAlive() || time > entry.getValue()) {
				if (!mob.isRemoved()) {
					mob.setTarget(null);
				}

				it.remove();
				continue;
			}

			LivingEntity current = mob.getTarget();

			if (current == null || current instanceof Player || !current.isAlive()) {
				// Ищем ближайшего «свободного» врага; если таких нет — любого другого врага.
				List<Mob> others = mob.level().getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(16),
						m -> m != mob && m instanceof Enemy && m.isAlive());
				others.sort(Comparator.<Mob>comparingInt(m -> MIND_CONTROLLED.containsKey(m) ? 1 : 0).thenComparingDouble(m -> m.distanceToSqr(mob)));
				mob.setTarget(others.isEmpty() ? null : others.get(0));
			}

			if (mob.level() instanceof ServerLevel level && time % 20 == 0) {
				level.sendParticles(MIND, mob.getX(), mob.getY(1.1), mob.getZ(), 3, 0.2, 0.1, 0.2, 0.0);
			}
		}
	}

	public static void lifeSurge(ServerLevel level, ServerPlayer player) {
		player.setHealth(player.getMaxHealth());
		clearHarmful(player);
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
		player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 1));

		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(8),
				e -> e != player && (e instanceof Player || (e instanceof TamableAnimal t && t.isOwnedBy(player))))) {
			e.heal(10);
			level.sendParticles(ParticleTypes.HEART, e.getX(), e.getY(1), e.getZ(), 5, 0.3, 0.3, 0.3, 0.0);
		}

		level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY(1), player.getZ(), 12, 0.6, 0.6, 0.6, 0.0);
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY(1), player.getZ(), 40, 0.5, 0.8, 0.5, 0.3);
		SuitWeapons.sound(level, player, SoundEvents.PLAYER_LEVELUP, 1.0f, 1.4f);
	}

	private static void clearHarmful(ServerPlayer player) {
		for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
			if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
				player.removeEffect(effect.getEffect());
			}
		}
	}

	/** «Коллапс»: все враги вокруг рассыпаются пеплом. Забирает всю энергию и силы. */
	public static void collapse(ServerLevel level, ServerPlayer player, SuitTier tier) {
		int count = 0;

		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(64),
				e -> e instanceof Enemy && SuitWeapons.isTarget(e, player))) {
			level.sendParticles(ASH, e.getX(), e.getY(0.5), e.getZ(), 30, e.getBbWidth() * 0.6, e.getBbHeight() * 0.5, e.getBbWidth() * 0.6, 0.02);
			level.sendParticles(ParticleTypes.ASH, e.getX(), e.getY(0.5), e.getZ(), 20, 0.4, 0.6, 0.4, 0.05);
			e.invulnerableTime = 0;
			// Боссов не стираем целиком — только сильно раним.
			e.hurtServer(level, level.damageSources().playerAttack(player), e.getMaxHealth() > 200 ? 150f : 100000f);
			count++;
		}

		for (int i = 0; i < 120; i++) {
			double a = i / 120.0 * Math.PI * 2;
			level.sendParticles(ParticleTypes.END_ROD, true, false, player.getX(), player.getY(0.5), player.getZ(), 0, Math.cos(a), 0.02, Math.sin(a), 1.6);
		}

		if (!player.hasInfiniteMaterials()) {
			SuitEnergy.set(player, tier, 0);
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 600, 1));
			player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 1));
			player.hurtServer(level, level.damageSources().magic(), 8f);
		}

		SuitWeapons.sound(level, player, SoundEvents.LIGHTNING_BOLT_THUNDER, 2.0f, 0.5f);
		SuitWeapons.sound(level, player, SoundEvents.WARDEN_SONIC_BOOM, 1.5f, 0.5f);
		player.sendOverlayMessage(Component.translatable("message.solorust.ability.collapse", count).withStyle(ChatFormatting.GOLD));
	}

	// ================================================================== архимаг

	public static void mysticBolt(ServerLevel level, ServerPlayer player, SuitTier tier) {
		Vec3 from = SuitWeapons.hand(player);
		SuitWeapons.Ray ray = SuitWeapons.ray(level, player, from, 48, 0.3);
		SuitWeapons.line(level, GOLD, from, ray.end(), 0.35);
		SuitWeapons.line(level, ParticleTypes.WITCH, from, ray.end(), 1.5);

		if (ray.entity() != null) {
			SuitWeapons.hurt(level, player, ray.entity(), 7f * tier.power);
			ray.entity().addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
		}

		level.sendParticles(ParticleTypes.ENCHANTED_HIT, ray.end().x, ray.end().y, ray.end().z, 10, 0.2, 0.2, 0.2, 0.2);
		SuitWeapons.sound(level, player, SoundEvents.EVOKER_CAST_SPELL, 0.8f, 1.6f);
	}

	public static void portal(ServerLevel level, ServerPlayer player) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1f);
		BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(40)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 center;

		if (hit.getType() == HitResult.Type.BLOCK) {
			BlockPos pos = hit.getBlockPos().relative(hit.getDirection());
			center = Vec3.atBottomCenterOf(pos).add(0, 1, 0);
		} else {
			center = eye.add(look.scale(4)).subtract(0, 0.6, 0);
		}

		// Плоскость портала «смотрит» на того, кто его открыл.
		Vec3 normal = new Vec3(-look.x, 0, -look.z);
		normal = normal.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : normal.normalize();
		long time = level.getGameTime();
		PortalPair pair = PORTALS.get(player.getUUID());

		if (pair == null || pair.b != null || pair.level != level) {
			PORTALS.put(player.getUUID(), new PortalPair(level, center, normal, time + 600));
			player.sendOverlayMessage(Component.translatable("message.solorust.ability.portal_first").withStyle(ChatFormatting.GOLD));
		} else {
			pair.b = center;
			pair.bNormal = normal;
			pair.expires = time + 1200;
			player.sendOverlayMessage(Component.translatable("message.solorust.ability.portal_open").withStyle(ChatFormatting.GOLD));
		}

		level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 30, 0.3, 0.6, 0.3, 0.05);
		SuitWeapons.sound(level, player, SoundEvents.FIRECHARGE_USE, 0.8f, 0.6f);
	}

	private static void portals(long time) {
		if (PORTALS.isEmpty()) {
			return;
		}

		for (Iterator<PortalPair> it = PORTALS.values().iterator(); it.hasNext(); ) {
			PortalPair p = it.next();

			if (time > p.expires) {
				it.remove();
				continue;
			}

			if (time % 2 == 0) {
				ring(p.level, p.a, p.aNormal, time);

				if (p.b != null) {
					ring(p.level, p.b, p.bNormal, time);
				}
			}

			if (p.b != null) {
				transport(p, p.a, p.b, p.bNormal, time);
				transport(p, p.b, p.a, p.aNormal, time);
			}
		}

		PORTAL_COOLDOWN.values().removeIf(t -> t < time);
	}

	private static void ring(ServerLevel level, Vec3 c, Vec3 normal, long time) {
		Vec3 side = new Vec3(-normal.z, 0, normal.x);

		for (int i = 0; i < 24; i++) {
			double a = i / 24.0 * Math.PI * 2 + time * 0.05;
			Vec3 p = c.add(side.scale(Math.cos(a) * 0.75)).add(0, Math.sin(a) * 1.1, 0);
			level.sendParticles(PORTAL_DUST, p.x, p.y, p.z, 1, 0, 0, 0, 0);
		}

		if (time % 6 == 0) {
			level.sendParticles(ParticleTypes.FLAME, c.x, c.y, c.z, 2, 0.4, 0.7, 0.4, 0.01);
		}
	}

	private static void transport(PortalPair p, Vec3 from, Vec3 to, Vec3 toNormal, long time) {
		AABB box = new AABB(from, from).inflate(0.6, 1.1, 0.6);

		for (Entity e : p.level.getEntities((Entity) null, box, e -> !e.isSpectator() && !(e instanceof Projectile && e.tickCount < 2))) {
			if (PORTAL_COOLDOWN.containsKey(e.getUUID())) {
				continue;
			}

			Vec3 dest = to.add(toNormal.scale(1.1)).subtract(0, 1, 0);
			e.teleportTo(dest.x, dest.y, dest.z);
			e.fallDistance = 0;
			PORTAL_COOLDOWN.put(e.getUUID(), time + 30);
			p.level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 1.3f);
		}
	}

	/** Рунический щит отражает летящие спереди снаряды (урон гасит {@link SuitManager}). */
	public static void runeShield(ServerLevel level, ServerPlayer player, int ticks) {
		Vec3 look = player.getViewVector(1f);
		Vec3 c = player.getEyePosition().add(look.scale(1.2)).add(0, -0.3, 0);
		Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
		Vec3 up = right.cross(look).normalize();

		if (ticks % 2 == 0) {
			for (int i = 0; i < 20; i++) {
				double a = i / 20.0 * Math.PI * 2 + ticks * 0.12;
				Vec3 p = c.add(right.scale(Math.cos(a) * 1.0)).add(up.scale(Math.sin(a) * 1.0));
				level.sendParticles(GOLD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
			}

			level.sendParticles(ParticleTypes.ENCHANT, c.x, c.y, c.z, 4, 0.4, 0.4, 0.4, 0.5);
		}

		for (Entity e : level.getEntities(player, player.getBoundingBox().inflate(4), e -> e instanceof Projectile)) {
			Vec3 to = e.position().subtract(player.getEyePosition());

			if (SuitWeapons.angle(look, to) < 70 && e.getDeltaMovement().dot(to) < 0) {
				e.setDeltaMovement(e.getDeltaMovement().scale(-1.1));
				((Projectile) e).setOwner(player);
				e.hurtMarked = true;
				level.sendParticles(ParticleTypes.ENCHANTED_HIT, e.getX(), e.getY(), e.getZ(), 6, 0.1, 0.1, 0.1, 0.2);
			}
		}
	}

	public static void astralForm(ServerLevel level, ServerPlayer player) {
		if (ASTRAL_TIMER.containsKey(player.getUUID())) {
			return;
		}

		player.setAttached(ASTRAL, new AstralState(GlobalPos.of(level.dimension(), player.blockPosition()), player.gameMode().getName()));
		ASTRAL_TIMER.put(player.getUUID(), 300);
		level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY(1), player.getZ(), 30, 0.3, 0.6, 0.3, 0.05);
		SuitWeapons.sound(level, player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0f, 0.8f);
		player.setGameMode(GameType.SPECTATOR);
		player.sendOverlayMessage(Component.translatable("message.solorust.ability.astral").withStyle(ChatFormatting.AQUA));
	}

	private static void astralTick(ServerPlayer player) {
		int left = ASTRAL_TIMER.merge(player.getUUID(), -1, Integer::sum);
		AstralState state = player.getAttached(ASTRAL);

		if (state != null && left % 10 == 0 && player.level().dimension().equals(state.body().dimension())) {
			BlockPos b = state.body().pos();
			player.level().sendParticles(ParticleTypes.SOUL, b.getX() + 0.5, b.getY() + 1, b.getZ() + 0.5, 4, 0.2, 0.5, 0.2, 0.01);
		}

		if (left % 20 == 0 && left > 0) {
			player.sendOverlayMessage(Component.translatable("message.solorust.ability.astral_left", left / 20).withStyle(ChatFormatting.AQUA));
		}

		if (left <= 0 || state == null) {
			endAstral(player);
		}
	}

	private static void endAstral(ServerPlayer player) {
		ASTRAL_TIMER.remove(player.getUUID());
		AstralState state = player.removeAttached(ASTRAL);

		if (state == null) {
			return;
		}

		ServerLevel body = player.level().getServer().getLevel(state.body().dimension());

		if (body != null) {
			BlockPos b = state.body().pos();
			player.teleportTo(body, b.getX() + 0.5, b.getY(), b.getZ() + 0.5, Set.of(), player.getYRot(), player.getXRot(), false);
		}

		GameType mode = GameType.byName(state.gameMode());
		player.setGameMode(mode == GameType.SPECTATOR ? GameType.SURVIVAL : mode);
		player.level().sendParticles(ParticleTypes.SOUL, player.getX(), player.getY(1), player.getZ(), 30, 0.3, 0.6, 0.3, 0.05);
		SuitWeapons.sound(player.level(), player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0f, 1.4f);
	}

	public static void timeLoop(ServerLevel level, ServerPlayer player) {
		Deque<Snapshot> history = HISTORY.get(player.getUUID());

		if (history == null || history.size() < 3) {
			player.sendOverlayMessage(Component.translatable("message.solorust.ability.no_history").withStyle(ChatFormatting.GRAY));
			return;
		}

		// Ближайший снимок не новее 10 секунд назад (или самый старый).
		Snapshot snap = history.peekFirst();
		List<Snapshot> list = new ArrayList<>(history);

		if (list.size() > 10) {
			snap = list.get(list.size() - 11);
		}

		if (!snap.dimension().equals(level.dimension())) {
			player.sendOverlayMessage(Component.translatable("message.solorust.station.other_dimension").withStyle(ChatFormatting.RED));
			return;
		}

		SuitWeapons.line(level, ParticleTypes.REVERSE_PORTAL, player.position().add(0, 1, 0), snap.pos().add(0, 1, 0), 0.6);
		player.teleportTo(level, snap.pos().x, snap.pos().y, snap.pos().z, Set.of(), snap.yRot(), snap.xRot(), false);
		player.setHealth(Math.max(player.getHealth(), snap.health()));
		player.getFoodData().setFoodLevel(Math.max(player.getFoodData().getFoodLevel(), snap.food()));
		player.fallDistance = 0;
		clearHarmful(player);
		history.clear();

		level.sendParticles(GOLD, player.getX(), player.getY(1), player.getZ(), 40, 0.5, 0.8, 0.5, 0.0);
		SuitWeapons.sound(level, player, SoundEvents.BELL_RESONATE, 1.0f, 1.6f);
		player.sendOverlayMessage(Component.translatable("message.solorust.ability.loop").withStyle(ChatFormatting.GOLD));
	}

	// ================================================================== «Апекс»

	public static void heatVision(ServerLevel level, ServerPlayer player, SuitTier tier, int ticks) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1f);
		Vec3 flat = new Vec3(-look.z, 0, look.x);
		flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
		SuitWeapons.Ray ray = SuitWeapons.ray(level, player, eye, 48, 0.1);

		for (int s = -1; s <= 1; s += 2) {
			Vec3 from = eye.add(look.scale(0.25)).add(flat.scale(0.11 * s)).add(0, -0.04, 0);
			SuitWeapons.line(level, HEAT, from, ray.end(), 0.3);
		}

		if (ray.entity() != null && ticks % 2 == 0) {
			SuitWeapons.hurt(level, player, ray.entity(), 3f * tier.power);
			ray.entity().igniteForSeconds(4);
		} else if (ray.block().getType() == HitResult.Type.BLOCK && ticks % 10 == 0) {
			BlockPos fire = ray.block().getBlockPos().relative(ray.block().getDirection());

			if (level.getBlockState(fire).isAir() && player.mayBuild() && level.mayInteract(player, fire)) {
				level.setBlockAndUpdate(fire, BaseFireBlock.getState(level, fire));
			}
		}

		if (ticks % 3 == 0) {
			level.sendParticles(ParticleTypes.SMALL_FLAME, ray.end().x, ray.end().y, ray.end().z, 3, 0.05, 0.05, 0.05, 0.01);
		}

		if (ticks % 8 == 0) {
			SuitWeapons.sound(level, player, SoundEvents.BEACON_AMBIENT, 0.8f, 1.6f);
		}
	}

	public static void sonicClap(ServerLevel level, ServerPlayer player, SuitTier tier) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1f);

		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(12), e -> SuitWeapons.isTarget(e, player))) {
			Vec3 to = e.getBoundingBox().getCenter().subtract(eye);

			if (to.length() < 12 && SuitWeapons.angle(look, to) < 45) {
				SuitWeapons.hurt(level, player, e, 6f * tier.power);
				SuitWeapons.push(e, look, 3.5, 0.6);
			}
		}

		for (int i = 1; i <= 5; i++) {
			Vec3 p = eye.add(look.scale(i * 2));
			level.sendParticles(ParticleTypes.GUST, p.x, p.y, p.z, 1, 0.2 * i, 0.2 * i, 0.2 * i, 0);
		}

		Vec3 boom = eye.add(look.scale(3));
		level.sendParticles(ParticleTypes.SONIC_BOOM, boom.x, boom.y, boom.z, 1, 0, 0, 0, 0);
		SuitWeapons.sound(level, player, SoundEvents.WARDEN_SONIC_BOOM, 1.2f, 1.6f);
		SuitWeapons.sound(level, player, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.0f, 0.6f);
		player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
	}

	// ================================================================== «Бурильщик»

	public static void drill(ServerLevel level, ServerPlayer player, int ticks) {
		if (ticks % 4 != 0) {
			return;
		}

		Vec3 eye = player.getEyePosition();
		BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getViewVector(1f).scale(5.5)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

		if (hit.getType() != HitResult.Type.BLOCK) {
			return;
		}

		Direction face = hit.getDirection();
		Direction a = face.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
		Direction b = face.getAxis() == Direction.Axis.X ? Direction.SOUTH : (face.getAxis() == Direction.Axis.Y ? Direction.EAST : Direction.EAST);
		BlockPos center = hit.getBlockPos();

		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				mine(level, player, center.relative(a, i).relative(b, j));
			}
		}

		Vec3 p = hit.getLocation();
		level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 6, 0.3, 0.3, 0.3, 0.2);

		if (ticks % 8 == 0) {
			SuitWeapons.sound(level, player, SoundEvents.GRINDSTONE_USE, 0.8f, 0.7f);
		}
	}

	/** Сломать блок «как алмазной киркой», добыча — сразу в инвентарь. */
	private static boolean mine(ServerLevel level, ServerPlayer player, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		float hardness = state.getDestroySpeed(level, pos);

		if (state.isAir() || hardness < 0 || hardness >= 50 || !state.getFluidState().isEmpty() && state.getFluidState().isSource()
				|| !player.mayBuild() || !level.mayInteract(player, pos)) {
			return false;
		}

		List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, new ItemStack(Items.DIAMOND_PICKAXE));
		level.destroyBlock(pos, false, player);

		for (ItemStack drop : drops) {
			if (!player.getInventory().add(drop)) {
				player.drop(drop, false);
			}
		}

		return true;
	}

	private static final List<TagKey<Block>> ORE_TAGS = List.of(BlockTags.DIAMOND_ORES, BlockTags.EMERALD_ORES, BlockTags.GOLD_ORES,
			BlockTags.IRON_ORES, BlockTags.REDSTONE_ORES, BlockTags.LAPIS_ORES, BlockTags.COPPER_ORES, BlockTags.COAL_ORES);

	public static void seismic(ServerLevel level, ServerPlayer player) {
		BlockPos center = player.blockPosition();
		Map<Block, int[]> found = new LinkedHashMap<>();
		int r = 16;

		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
			BlockState state = level.getBlockState(pos);

			if (state.isAir()) {
				continue;
			}

			boolean ore = state.is(Blocks.ANCIENT_DEBRIS);

			for (int i = 0; !ore && i < ORE_TAGS.size(); i++) {
				ore = state.is(ORE_TAGS.get(i));
			}

			if (ore) {
				int dist = (int) Math.round(Math.sqrt(pos.distSqr(center)));
				int[] info = found.computeIfAbsent(state.getBlock(), k -> new int[] {0, Integer.MAX_VALUE});
				info[0]++;
				info[1] = Math.min(info[1], dist);
			}
		}

		level.sendParticles(ParticleTypes.SCRAPE, player.getX(), player.getY(), player.getZ(), 40, 3, 0.1, 3, 0.1);
		SuitWeapons.sound(level, player, SoundEvents.CONDUIT_ACTIVATE, 0.8f, 0.6f);

		if (found.isEmpty()) {
			player.sendSystemMessage(Component.translatable("message.solorust.ability.seismic_none").withStyle(ChatFormatting.GRAY));
			return;
		}

		player.sendSystemMessage(Component.translatable("message.solorust.ability.seismic").withStyle(ChatFormatting.GOLD));
		found.entrySet().stream()
				.sorted(Comparator.comparingInt(e -> e.getValue()[1]))
				.limit(8)
				.forEach(e -> player.sendSystemMessage(Component.literal(" • ").append(e.getKey().getName())
						.append(Component.translatable("message.solorust.ability.seismic_line", e.getValue()[0], e.getValue()[1]))
						.withStyle(ChatFormatting.YELLOW)));
	}

	public static void lightFlare(ServerLevel level, ServerPlayer player) {
		Vec3 eye = player.getEyePosition();
		BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getViewVector(1f).scale(32)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		BlockPos pos = hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos().relative(hit.getDirection()) : BlockPos.containing(hit.getLocation());

		if (!level.getBlockState(pos).isAir() || !player.mayBuild() || !level.mayInteract(player, pos)) {
			return;
		}

		level.setBlockAndUpdate(pos, Blocks.LIGHT.defaultBlockState());
		level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.2, 0.2, 0.2, 0.05);
		SuitWeapons.line(level, ParticleTypes.END_ROD, SuitWeapons.hand(player), Vec3.atCenterOf(pos), 1.5);
		SuitWeapons.sound(level, player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.8f);
	}

	// ================================================================== «Прораб»

	/** Блоки для стройки берутся из второй руки. */
	private static BlockState buildBlock(ServerPlayer player) {
		ItemStack off = player.getOffhandItem();
		return off.getItem() instanceof BlockItem item ? item.getBlock().defaultBlockState() : null;
	}

	private static boolean place(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
		ItemStack off = player.getOffhandItem();

		if (off.isEmpty() || !level.getBlockState(pos).canBeReplaced() || !player.mayBuild() || !level.mayInteract(player, pos)
				|| !state.canSurvive(level, pos)) {
			return false;
		}

		level.setBlockAndUpdate(pos, state);

		if (!player.hasInfiniteMaterials()) {
			off.shrink(1);
		}

		return true;
	}

	public static void build(ServerLevel level, ServerPlayer player, SuitWeapon weapon) {
		BlockState state = buildBlock(player);

		if (state == null && weapon != SuitWeapon.DEMOLISH) {
			player.sendOverlayMessage(Component.translatable("message.solorust.ability.need_blocks").withStyle(ChatFormatting.YELLOW));
			return;
		}

		Vec3 eye = player.getEyePosition();
		BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getViewVector(1f).scale(24)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		Direction facing = Direction.fromYRot(player.getYRot());
		Direction right = facing.getClockWise();
		int placed = 0;

		switch (weapon) {
			case BUILD_WALL -> {
				if (hit.getType() != HitResult.Type.BLOCK) {
					return;
				}

				BlockPos base = hit.getBlockPos().relative(hit.getDirection());

				for (int dy = 0; dy < 3; dy++) {
					for (int dx = -2; dx <= 2; dx++) {
						placed += place(level, player, base.relative(right, dx).above(dy), state) ? 1 : 0;
					}
				}
			}
			case BUILD_BRIDGE -> {
				BlockPos start = player.getOnPos();

				for (int i = 1; i <= 16; i++) {
					placed += place(level, player, start.relative(facing, i), state) ? 1 : 0;
				}
			}
			case BUILD_FLOOR -> {
				if (hit.getType() != HitResult.Type.BLOCK) {
					return;
				}

				BlockPos center = hit.getDirection() == Direction.UP ? hit.getBlockPos().above() : hit.getBlockPos().relative(hit.getDirection());

				for (int dx = -2; dx <= 2; dx++) {
					for (int dz = -2; dz <= 2; dz++) {
						placed += place(level, player, center.offset(dx, 0, dz), state) ? 1 : 0;
					}
				}
			}
			case DEMOLISH -> {
				if (hit.getType() != HitResult.Type.BLOCK) {
					return;
				}

				for (BlockPos pos : BlockPos.betweenClosed(hit.getBlockPos().offset(-1, -1, -1), hit.getBlockPos().offset(1, 1, 1))) {
					placed += mine(level, player, pos.immutable()) ? 1 : 0;
				}
			}
			default -> {
			}
		}

		if (placed > 0) {
			Vec3 at = hit.getLocation();
			level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 10, 1, 0.5, 1, 0.02);
			SuitWeapons.sound(level, player, weapon == SuitWeapon.DEMOLISH ? SoundEvents.STONE_BREAK : SoundEvents.SCAFFOLDING_PLACE, 1.0f, 0.9f);
		}
	}

	// ================================================================== «Призрак»

	public static void cloak(ServerLevel level, ServerPlayer player) {
		CLOAKED.put(player.getUUID(), level.getGameTime() + 600);
		player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 600, 0, false, false, true));
		dropAggro(level, player);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY(1), player.getZ(), 30, 0.4, 0.6, 0.4, 0.02);
		SuitWeapons.sound(level, player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0f, 1.2f);
		player.sendOverlayMessage(Component.translatable("message.solorust.ability.cloak").withStyle(ChatFormatting.DARK_GREEN));
	}

	private static void cloak(MinecraftServer server, long time) {
		if (CLOAKED.isEmpty()) {
			return;
		}

		CLOAKED.values().removeIf(t -> t < time);

		for (UUID id : CLOAKED.keySet()) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);

			if (player != null) {
				dropAggro(player.level(), player);
			}
		}
	}

	private static void dropAggro(ServerLevel level, ServerPlayer player) {
		for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48), m -> m.getTarget() == player)) {
			mob.setTarget(null);
		}
	}

	public static void grapple(ServerLevel level, ServerPlayer player) {
		Vec3 eye = player.getEyePosition();
		BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getViewVector(1f).scale(40)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

		if (hit.getType() != HitResult.Type.BLOCK) {
			player.sendOverlayMessage(Component.translatable("message.solorust.ability.grapple_miss").withStyle(ChatFormatting.GRAY));
			return;
		}

		Vec3 to = hit.getLocation().subtract(eye);
		double dist = to.length();
		Vec3 vel = to.normalize().scale(Math.min(3.2, 0.9 + dist * 0.12)).add(0, 0.35, 0);
		player.setDeltaMovement(vel);
		player.hurtMarked = true;
		player.fallDistance = 0;
		SuitWeapons.line(level, ParticleTypes.CRIT, SuitWeapons.hand(player), hit.getLocation(), 0.7);
		SuitWeapons.sound(level, player, SoundEvents.FISHING_BOBBER_THROW, 1.0f, 0.7f);
		SuitWeapons.sound(level, player, SoundEvents.CHAIN_PLACE, 1.0f, 1.2f);
	}
}
