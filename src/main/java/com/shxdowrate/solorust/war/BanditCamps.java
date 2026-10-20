package com.shxdowrate.solorust.war;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.shxdowrate.solorust.registry.ModWar;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Лагеря бандитов. В мирах War Machines деревни заняты бандитами: каждый житель при загрузке заменяется
 * одним-двумя бандитами, железный голем — пулемётчиком, а у центра деревни встаёт офицер с охраной.
 * Тревога, награды за убийства, паника без офицера и бонус за зачистку лагеря — тоже здесь.
 */
public final class BanditCamps {
	public static final int CAMP_CLEAR_BONUS = 2_500;

	private record Pending(ServerLevel level, Entity entity) {
	}

	private static final Deque<Pending> QUEUE = new ArrayDeque<>();
	/** Лагеря, в которых в этой игровой сессии уже поставили офицера с охраной. */
	private static final Set<Long> GARRISONED = new HashSet<>();

	private BanditCamps() {
	}

	public static void init() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if ((entity instanceof AbstractVillager || entity instanceof IronGolem) && WarWorld.is(level)) {
				// Заменяем на следующем тике: менять сущности прямо во время загрузки чанка нельзя.
				QUEUE.add(new Pending(level, entity));
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int budget = 64;

			while (!QUEUE.isEmpty() && budget-- > 0) {
				Pending p = QUEUE.poll();
				replace(p.level, p.entity);
			}
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			QUEUE.clear();
			GARRISONED.clear();
		});
	}

	// ================================================================== заселение

	private static void replace(ServerLevel level, Entity entity) {
		if (entity.isRemoved()) {
			return;
		}

		BlockPos pos = entity.blockPosition();
		RandomSource random = level.getRandom();
		StructureStart village = level.structureManager().getStructureWithPieceAt(pos, StructureTags.VILLAGE);
		long campId;
		BlockPos center;

		if (village != null && village.isValid()) {
			campId = village.getChunkPos().pack();
			center = village.getBoundingBox().getCenter();
			center = new BlockPos(center.getX(), pos.getY(), center.getZ());
		} else {
			campId = ChunkPos.pack(pos.getX() >> 6, pos.getZ() >> 6);
			center = pos;
		}

		boolean child = entity instanceof AbstractVillager villager && villager.isBaby();
		entity.discard();

		List<BanditRole> roles = new ArrayList<>();

		if (entity instanceof IronGolem) {
			roles.add(BanditRole.MACHINE_GUNNER);
		} else {
			roles.add(BanditRole.random(random));

			if (!child && random.nextFloat() < 0.6f) {
				roles.add(BanditRole.random(random));
			}
		}

		for (BanditRole role : roles) {
			spawn(level, pos, role, campId, center, random);
		}

		// Первый раз в этом лагере — офицер и охрана в центре.
		if (GARRISONED.add(campId) && !hasOfficer(level, center, campId)) {
			BlockPos ground = ground(level, center);
			spawn(level, ground, BanditRole.OFFICER, campId, center, random);

			int guards = 3 + random.nextInt(4);

			for (int i = 0; i < guards; i++) {
				spawn(level, ground, BanditRole.random(random), campId, center, random);
			}
		}
	}

	private static boolean hasOfficer(ServerLevel level, BlockPos center, long campId) {
		return !level.getEntitiesOfClass(Bandit.class, new net.minecraft.world.phys.AABB(center).inflate(96),
				b -> b.campId() == campId && b.role() == BanditRole.OFFICER).isEmpty();
	}

	private static BlockPos ground(ServerLevel level, BlockPos at) {
		return level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at);
	}

	public static Bandit spawn(ServerLevel level, BlockPos pos, BanditRole role, long campId, BlockPos center, RandomSource random) {
		Bandit bandit = ModWar.BANDIT.create(level, EntitySpawnReason.STRUCTURE);

		if (bandit == null) {
			return null;
		}

		double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2;
		double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2;
		bandit.snapTo(x, pos.getY(), z, random.nextFloat() * 360f, 0);
		bandit.setRole(role);
		bandit.setCamp(campId, center, 40);
		bandit.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
		level.addFreshEntity(bandit);
		return bandit;
	}

	// ================================================================== тревога

	/** Бандит заметил врага: все свои в лагере (и все в 24 блоках) тоже берут его в прицел. */
	static void alarm(ServerLevel level, Bandit caller, LivingEntity target) {
		// Поднятые по тревоге не поднимают её заново — иначе вой цепочкой по всему лагерю.
		if (alarming) {
			return;
		}

		alarming = true;

		try {
			raise(level, caller, target);
		} finally {
			alarming = false;
		}
	}

	private static boolean alarming;

	private static void raise(ServerLevel level, Bandit caller, LivingEntity target) {
		List<Bandit> others = level.getEntitiesOfClass(Bandit.class, caller.getBoundingBox().inflate(48),
				b -> b != caller && b.isAlive() && b.getTarget() == null
						&& ((caller.campId() != 0 && b.campId() == caller.campId()) || b.distanceToSqr(caller) < 24 * 24));

		for (Bandit b : others) {
			b.setTarget(target);
		}

		if (!others.isEmpty()) {
			level.playSound(null, caller.getX(), caller.getEyeY(), caller.getZ(), SoundEvents.PILLAGER_CELEBRATE, SoundSource.HOSTILE, 2.0f, 0.8f);
			level.sendParticles(ParticleTypes.ANGRY_VILLAGER, caller.getX(), caller.getEyeY() + 0.6, caller.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
		}
	}

	// ================================================================== гибель

	static void onDeath(ServerLevel level, Bandit bandit, DamageSource source) {
		BanditRole role = bandit.role();

		// Без офицера отряд паникует (если рядом нет другого офицера).
		if (role == BanditRole.OFFICER) {
			List<Bandit> squad = bandit.allies(level, 32);
			boolean otherOfficer = squad.stream().anyMatch(b -> b.role() == BanditRole.OFFICER);

			if (!otherOfficer) {
				for (Bandit b : squad) {
					b.panicTicks = 100 + level.getRandom().nextInt(60);
				}
			}
		}

		ServerPlayer killer = source.getEntity() instanceof ServerPlayer p ? p : null;

		if (killer == null) {
			return;
		}

		WarRewards.reward(killer, role.reward, Component.translatable(role.translationKey()));

		// Последний бандит лагеря — лагерь зачищен.
		if (bandit.campId() != 0) {
			boolean anyLeft = !level.getEntitiesOfClass(Bandit.class, bandit.getBoundingBox().inflate(128),
					b -> b != bandit && b.isAlive() && b.campId() == bandit.campId()).isEmpty();

			if (!anyLeft) {
				WarData.addCash(killer, CAMP_CLEAR_BONUS);
				killer.sendSystemMessage(Component.translatable("message.solorust.war.camp_cleared", WardenShop.fmt(CAMP_CLEAR_BONUS))
						.withStyle(ChatFormatting.GOLD));
				level.playSound(null, killer.getX(), killer.getEyeY(), killer.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.0f);
			}
		}
	}
}
