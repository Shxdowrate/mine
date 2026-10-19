package com.shxdowrate.solorust.magic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.shxdowrate.solorust.SoloRust;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

/** Состояние, общее для обоих артефактов: полёт, история перемещений, союзники, щиты, петли времени, астрал. */
public final class MagicState {
	// ------------------------------------------------------------------ данные

	/** Снимок игрока для «Отката» и «Петли Дормамму». */
	record Snapshot(ResourceKey<Level> dimension, Vec3 pos, float yRot, float xRot, float health, int food) {
		static Snapshot of(ServerPlayer player) {
			return new Snapshot(player.level().dimension(), player.position(), player.getYRot(), player.getXRot(),
					player.getHealth(), player.getFoodData().getFoodLevel());
		}
	}

	/** Сломанный блок для «Перемотки» Ока Агамотто. */
	record BrokenBlock(ResourceKey<Level> dimension, BlockPos pos, BlockState state, long time) {
	}

	/** Моб под чужой волей: союзник игрока (контроль разума) или участник раздора. */
	static final class Charm {
		final UUID owner;
		final long until;
		final boolean ally;

		Charm(UUID owner, long until, boolean ally) {
			this.owner = owner;
			this.until = until;
			this.ally = ally;
		}
	}

	/** Где осталось тело и какой был режим игры — чтобы вернуть игрока даже после перезахода. */
	public record AstralState(GlobalPos body, String gameMode, long until) {
		public static final Codec<AstralState> CODEC = RecordCodecBuilder.create(i -> i.group(
				GlobalPos.CODEC.fieldOf("body").forGetter(AstralState::body),
				Codec.STRING.fieldOf("mode").forGetter(AstralState::gameMode),
				Codec.LONG.fieldOf("until").forGetter(AstralState::until)
		).apply(i, AstralState::new));
	}

	public static final AttachmentType<AstralState> ASTRAL = AttachmentRegistry.<AstralState>create(SoloRust.id("astral_projection"),
			builder -> builder.persistent(AstralState.CODEC));

	private static final int HISTORY_TICKS = 200;
	private static final long BLOCK_HISTORY_TICKS = 2400;

	static final Map<UUID, Deque<Snapshot>> HISTORY = new HashMap<>();
	static final Deque<BrokenBlock> BROKEN = new ArrayDeque<>();
	static final Map<Mob, Charm> CHARMED = new IdentityHashMap<>();
	/** Полёт от артефакта. */
	static final Set<UUID> FLIGHT = new HashSet<>();
	/** До какого тика действует: зеркальное измерение, щит Серафима, защита от падения после рывка. */
	static final Map<UUID, Long> MIRROR = new HashMap<>();
	static final Map<UUID, Long> SHIELD = new HashMap<>();
	static final Map<UUID, Long> FALL_SAFE = new HashMap<>();
	/** Скрытность без неуязвимости (камуфляж симбиота и т. п.). */
	static final Map<UUID, Long> STEALTH = new HashMap<>();
	/** Шипы: обидчик получает урон. */
	static final Map<UUID, Long> THORNS = new HashMap<>();
	/** Щит хаоса: часть ударов исчезает и бьёт обратно. */
	static final Map<UUID, Long> CHAOS_SHIELD = new HashMap<>();
	/** Вращающийся барьер: снаряды вокруг отражаются. */
	static final Map<UUID, Long> DEFLECT = new HashMap<>();
	/** Петля Дормамму: снимок и до какого тика действует. */
	static final Map<UUID, Snapshot> LOOP = new HashMap<>();
	static final Map<UUID, Long> LOOP_UNTIL = new HashMap<>();
	/** Кто сейчас взрывает метеоры (чтобы не ранить самого себя). */
	static Entity explodingCaster = null;

	private MagicState() {
	}

	// ------------------------------------------------------------------ регистрация

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(MagicState::tick);

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			Freeze.releaseAll();

			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.getAttached(ASTRAL) != null) {
					StrangeAbilities.endAstral(player);
				}
			}
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			Tasks.clear();
			HISTORY.clear();
			BROKEN.clear();
			CHARMED.clear();
			FLIGHT.clear();
			MIRROR.clear();
			SHIELD.clear();
			FALL_SAFE.clear();
			LOOP.clear();
			LOOP_UNTIL.clear();
			Portals.clear();
		});

		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> Freeze.onLoad(entity));

		ServerPlayerEvents.JOIN.register(player -> {
			if (player.getAttached(ASTRAL) != null) {
				StrangeAbilities.endAstral(player);
			}

			// Полёт сохраняется в мире; если артефакт с полётом при себе — это наш полёт.
			if (player.getAbilities().mayfly && !player.isCreative() && !player.isSpectator() && hasFlightArtifact(player)) {
				FLIGHT.add(player.getUUID());
			}
		});

		ServerPlayerEvents.LEAVE.register(player -> {
			UUID id = player.getUUID();
			HISTORY.remove(id);
			MIRROR.remove(id);
			SHIELD.remove(id);
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			HISTORY.remove(newPlayer.getUUID());
			FLIGHT.remove(newPlayer.getUUID());
		});

		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (blockEntity == null) {
				recordBroken(level, pos, state);
			}
		});

		ServerLivingEntityEvents.ALLOW_DAMAGE.register(MagicState::allowDamage);
		ServerPlayerEvents.ALLOW_DEATH.register((player, source, amount) -> !StrangeAbilities.dormammu(player));
	}

	static void recordBroken(Level level, BlockPos pos, BlockState state) {
		BROKEN.addLast(new BrokenBlock(level.dimension(), pos.immutable(), state, level.getGameTime()));

		while (BROKEN.size() > 8192) {
			BROKEN.removeFirst();
		}
	}

	// ------------------------------------------------------------------ тик

	private static void tick(MinecraftServer server) {
		long now = server.overworld().getGameTime();

		Tasks.tick();
		Freeze.tick(now);
		Portals.tick(now);

		if (now % 5 == 0) {
			charms(now);
		}

		MIRROR.values().removeIf(t -> t < now);
		SHIELD.values().removeIf(t -> t < now);
		FALL_SAFE.values().removeIf(t -> t < now);
		STEALTH.values().removeIf(t -> t < now);
		THORNS.values().removeIf(t -> t < now);
		CHAOS_SHIELD.values().removeIf(t -> t < now);
		DEFLECT.values().removeIf(t -> t < now);

		for (Iterator<Map.Entry<UUID, Long>> it = LOOP_UNTIL.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Long> e = it.next();

			UUID id = e.getKey();

			if (e.getValue() < now) {
				it.remove();
				LOOP.remove(id);
				ServerPlayer player = server.getPlayerList().getPlayer(id);

				if (player != null) {
					player.sendOverlayMessage(Component.translatable("message.solorust.magic.loop_end").withStyle(ChatFormatting.GRAY));
				}
			}
		}

		while (!BROKEN.isEmpty() && BROKEN.peekFirst().time() < now - BLOCK_HISTORY_TICKS) {
			BROKEN.removeFirst();
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			UUID id = player.getUUID();

			if (player.getAttached(ASTRAL) != null) {
				StrangeAbilities.astralTick(player, now);
				continue;
			}

			if (!player.isSpectator()) {
				Deque<Snapshot> history = HISTORY.computeIfAbsent(id, k -> new ArrayDeque<>());
				history.addLast(Snapshot.of(player));

				while (history.size() > HISTORY_TICKS) {
					history.removeFirst();
				}
			}

			if (DEFLECT.containsKey(id)) {
				deflect(player);
			}

			if ((MIRROR.containsKey(id) || STEALTH.containsKey(id)) && now % 5 == 0) {
				dropAggro(player.level(), player);
			}

			if (FLIGHT.contains(id) && now % 10 == 0) {
				keepFlight(player);
			}
		}
	}

	// ------------------------------------------------------------------ полёт

	static boolean toggleFlight(ServerPlayer player) {
		Abilities abilities = player.getAbilities();

		if (player.isCreative() || player.isSpectator()) {
			return false;
		}

		if (FLIGHT.remove(player.getUUID())) {
			abilities.mayfly = false;
			abilities.flying = false;
			player.onUpdateAbilities();
			FALL_SAFE.put(player.getUUID(), player.level().getGameTime() + 200);
			return false;
		}

		FLIGHT.add(player.getUUID());
		abilities.mayfly = true;
		abilities.flying = true;
		player.onUpdateAbilities();
		return true;
	}

	private static void keepFlight(ServerPlayer player) {
		if (player.isCreative() || player.isSpectator()) {
			FLIGHT.remove(player.getUUID());
			return;
		}

		if (!hasFlightArtifact(player)) {
			FLIGHT.remove(player.getUUID());
			Abilities abilities = player.getAbilities();
			abilities.mayfly = false;
			abilities.flying = false;
			player.onUpdateAbilities();
			FALL_SAFE.put(player.getUUID(), player.level().getGameTime() + 200);
			return;
		}

		if (!player.getAbilities().mayfly) {
			player.getAbilities().mayfly = true;
			player.onUpdateAbilities();
		}

		if (player.getAbilities().flying && player.level().getGameTime() % 4 == 0) {
			ServerLevel level = player.level();
			level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.2, player.getZ(), 1, 0.2, 0.05, 0.2, 0.01);
		}
	}

	private static boolean hasFlightArtifact(Player player) {
		Inventory inventory = player.getInventory();

		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);

			if (stack.getItem() instanceof ArtifactItem artifact && artifact.grantsFlight(stack)) {
				return true;
			}
		}

		return false;
	}

	// ------------------------------------------------------------------ союзники и раздор

	static void charm(Mob mob, ServerPlayer owner, long until, boolean ally) {
		CHARMED.put(mob, new Charm(owner.getUUID(), until, ally));

		if (mob.getTarget() == owner) {
			mob.setTarget(null);
		}
	}

	static boolean isMinionOf(Entity e, Player player) {
		if (player == null) {
			return false;
		}

		Charm charm = e instanceof Mob mob ? CHARMED.get(mob) : null;
		return charm != null && charm.ally && charm.owner.equals(player.getUUID());
	}

	private static void charms(long now) {
		for (Iterator<Map.Entry<Mob, Charm>> it = CHARMED.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Mob, Charm> entry = it.next();
			Mob mob = entry.getKey();
			Charm charm = entry.getValue();

			if (mob.isRemoved() || !mob.isAlive() || now > charm.until) {
				if (!mob.isRemoved()) {
					mob.setTarget(null);
					mob.removeEffect(MobEffects.GLOWING);
				}

				it.remove();
				continue;
			}

			if (!(mob.level() instanceof ServerLevel level) || Freeze.isFrozen(mob)) {
				continue;
			}

			Player owner = level.getPlayerByUUID(charm.owner);
			LivingEntity target = mob.getTarget();

			if (charm.ally) {
				if (target == owner || target != null && (!target.isAlive() || isMinionOf(target, owner))) {
					mob.setTarget(null);
					target = null;
				}

				if (target == null && owner != null) {
					target = allyTarget(level, mob, owner);
					mob.setTarget(target);

					if (target == null && mob.distanceToSqr(owner) > 64) {
						mob.getNavigation().moveTo(owner, 1.2);
					}
				}

				if (now % 20 == 0) {
					level.sendParticles(Fx.dust(0xFFD21F, 0.9f), mob.getX(), mob.getY(1.15), mob.getZ(), 3, 0.2, 0.1, 0.2, 0.0);
				}
			} else {
				if (target == null || target instanceof Player || !target.isAlive()) {
					List<Mob> others = level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(16), m -> m != mob && m.isAlive());
					mob.setTarget(others.isEmpty() ? null : others.get(mob.getRandom().nextInt(others.size())));
				}

				if (now % 20 == 0) {
					level.sendParticles(ParticleTypes.ANGRY_VILLAGER, mob.getX(), mob.getY(1.1), mob.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
				}
			}
		}
	}

	/** Кого атаковать союзнику: того, кого бил хозяин, того, кто бил хозяина, или ближайшего врага. */
	private static LivingEntity allyTarget(ServerLevel level, Mob mob, Player owner) {
		LivingEntity hurtByOwner = owner.getLastHurtMob();

		if (valid(hurtByOwner, mob, owner) && owner.tickCount - owner.getLastHurtMobTimestamp() < 200) {
			return hurtByOwner;
		}

		LivingEntity attacker = owner.getLastHurtByMob();

		if (valid(attacker, mob, owner)) {
			return attacker;
		}

		List<Mob> enemies = level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(16),
				m -> m != mob && m.isAlive() && Fx.hostile(m) && !isMinionOf(m, owner));
		return enemies.stream().min(Comparator.comparingDouble(m -> m.distanceToSqr(mob))).orElse(null);
	}

	private static boolean valid(LivingEntity e, Mob mob, Player owner) {
		return e != null && e != mob && e != owner && e.isAlive() && e.distanceToSqr(mob) < 32 * 32 && !isMinionOf(e, owner);
	}

	static void dropAggro(ServerLevel level, ServerPlayer player) {
		for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48), m -> m.getTarget() == player)) {
			mob.setTarget(null);
		}
	}

	// ------------------------------------------------------------------ урон

	private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		Entity attacker = source.getEntity();

		// Союзник под контролем разума не бьёт хозяина.
		if (attacker instanceof Mob mob && isMinionOf(mob, entity instanceof Player p ? p : null)) {
			return false;
		}

		if (!(entity instanceof ServerPlayer player)) {
			return true;
		}

		UUID id = player.getUUID();

		if (explodingCaster == player && source.is(DamageTypeTags.IS_EXPLOSION)) {
			return false;
		}

		if (source.is(DamageTypeTags.IS_FALL) && (FLIGHT.contains(id) || FALL_SAFE.containsKey(id))) {
			return false;
		}

		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return true;
		}

		if (MIRROR.containsKey(id) && attacker != null && !(attacker instanceof Player)) {
			return false;
		}

		if (THORNS.containsKey(id) && attacker instanceof LivingEntity living && attacker != player && !source.is(net.minecraft.world.damagesource.DamageTypes.THORNS)) {
			living.hurtServer(player.level(), player.level().damageSources().thorns(player), Math.max(3f, amount * 0.5f));
		}

		if (CHAOS_SHIELD.containsKey(id) && player.getRandom().nextFloat() < 0.6f) {
			ServerLevel level = player.level();
			level.sendParticles(Fx.dust(0xF72585, 1.2f), player.getX(), player.getY(0.6), player.getZ(), 15, 0.4, 0.6, 0.4, 0);

			if (attacker instanceof LivingEntity living && attacker != player) {
				living.hurtServer(level, level.damageSources().magic(), amount);
			}

			return false;
		}

		if (SHIELD.containsKey(id)) {
			Vec3 from = source.getSourcePosition();

			if (from != null && Fx.angle(player.getViewVector(1f), from.subtract(player.getEyePosition())) < 80) {
				ServerLevel level = player.level();
				Vec3 at = player.getEyePosition().add(player.getViewVector(1f).scale(1.6));
				level.sendParticles(ParticleTypes.ENCHANTED_HIT, at.x, at.y, at.z, 10, 0.4, 0.4, 0.4, 0.2);
				Fx.sound(level, player, SoundEvents.SHIELD_BLOCK.value(), 0.8f, 1.5f);
				return false;
			}
		}

		return true;
	}

	/** Снаряды вокруг игрока разворачиваются обратно. */
	private static void deflect(ServerPlayer player) {
		ServerLevel level = player.level();

		for (Entity e : level.getEntities(player, player.getBoundingBox().inflate(4),
				e -> e instanceof net.minecraft.world.entity.projectile.Projectile p && p.getOwner() != player)) {
			Vec3 away = e.position().subtract(player.position());

			if (e.getDeltaMovement().dot(away) < 0) {
				e.setDeltaMovement(away.normalize().scale(Math.max(1.0, e.getDeltaMovement().length())));
				((net.minecraft.world.entity.projectile.Projectile) e).setOwner(player);
				e.hurtMarked = true;
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY(), e.getZ(), 6, 0.1, 0.1, 0.1, 0.2);
			}
		}
	}

	static void giveOrDrop(ServerPlayer player, ItemStack stack) {
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}
	}

	static List<Snapshot> history(ServerPlayer player) {
		Deque<Snapshot> history = HISTORY.get(player.getUUID());
		return history == null ? List.of() : new ArrayList<>(history);
	}

	static GameType mode(String name) {
		GameType mode = GameType.byName(name);
		return mode == GameType.SPECTATOR ? GameType.SURVIVAL : mode;
	}

	static void bonusEffect(LivingEntity e, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int ticks, int amp) {
		e.addEffect(new MobEffectInstance(effect, ticks, amp, false, true, true));
	}
}
