package com.shxdowrate.solorust.suit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.shxdowrate.solorust.SoloRust;
import com.shxdowrate.solorust.registry.ModSuits;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * «Операционная система» костюма: следит за надетым комплектом, включает полёт, тратит и копит энергию,
 * выдаёт/забирает встроенное оружие, накладывает бонусы, гасит урон от падения и делает «топот» Титана.
 */
public final class SuitManager {
	private static final float DEFAULT_FLY_SPEED = 0.05f;

	private static final Identifier MOD_SPEED = SoloRust.id("suit_speed");
	private static final Identifier MOD_ATTACK = SoloRust.id("suit_attack");
	private static final Identifier MOD_JUMP = SoloRust.id("suit_jump");
	private static final Identifier MOD_SCALE = SoloRust.id("suit_scale");
	private static final Identifier MOD_STEP = SoloRust.id("suit_step");
	private static final Identifier MOD_REACH_ENTITY = SoloRust.id("suit_reach_entity");
	private static final Identifier MOD_REACH_BLOCK = SoloRust.id("suit_reach_block");
	private static final Identifier MOD_HEALTH = SoloRust.id("suit_health");
	private static final Identifier MOD_SAFE_FALL = SoloRust.id("suit_safe_fall");

	/** Какое поколение было надето на прошлом тике. */
	private static final Map<UUID, SuitTier> WORN = new HashMap<>();
	/** Кому полёт включил именно костюм (чтобы не трогать творческий режим и чужие моды). */
	private static final Set<UUID> FLIGHT = new HashSet<>();
	private static final Set<UUID> WAS_FLYING = new HashSet<>();
	/** Высота, с которой падает Титан (для ударной волны при приземлении). */
	private static final Map<UUID, Double> FALLING = new HashMap<>();
	private static final Map<UUID, Double> LAST_Y = new HashMap<>();
	private static final Set<UUID> WARNED_FULL = new HashSet<>();

	private SuitManager() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(SuitManager::tick);

		// Встроенное оружие нельзя выбросить: выпавший предмет просто исчезает (костюм выдаст его снова).
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof ItemEntity item && item.getItem().getItem() instanceof SuitWeaponItem) {
				item.discard();
			}
		});

		ServerLivingEntityEvents.ALLOW_DAMAGE.register(SuitManager::allowDamage);

		ServerPlayerEvents.JOIN.register(player -> {
			SuitTier tier = SuitTier.worn(player);

			// Способность летать сохраняется в мире; если костюм надет — это наш полёт.
			if (tier != null && tier.flight && !player.isCreative() && !player.isSpectator()) {
				FLIGHT.add(player.getUUID());
			}
		});

		ServerPlayerEvents.LEAVE.register(player -> forget(player.getUUID()));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> forget(newPlayer.getUUID()));
	}

	private static void forget(UUID id) {
		WORN.remove(id);
		FLIGHT.remove(id);
		WAS_FLYING.remove(id);
		FALLING.remove(id);
		LAST_Y.remove(id);
		WARNED_FULL.remove(id);
		SuitWeapons.forget(id);
	}

	// ================================================================== тик

	private static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			tickPlayer(player);
		}
	}

	private static void tickPlayer(ServerPlayer player) {
		UUID id = player.getUUID();
		SuitTier tier = player.isAlive() ? SuitTier.worn(player) : null;
		SuitTier previous = WORN.get(id);

		if (tier != previous) {
			changed(player, previous, tier);

			if (tier == null) {
				WORN.remove(id);
			} else {
				WORN.put(id, tier);
			}
		}

		long time = player.level().getGameTime();

		if (tier == null) {
			if (time % 10 == 0 || previous != null) {
				syncWeapons(player, null);
			}

			return;
		}

		flight(player, tier);
		titanStomp(player, tier);

		if (time % 10 == 0) {
			energy(player, tier);
			effects(player, tier);
			syncWeapons(player, tier);
		}
	}

	private static void changed(ServerPlayer player, SuitTier from, SuitTier to) {
		ServerLevel level = player.level();
		applyStats(player, to);

		if (to != null) {
			player.sendSystemMessage(Component.translatable("message.solorust.suit.online", Component.translatable(to.translationKey()))
					.withColor(to.hudColor & 0xFFFFFF), true);
			level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.6f);
			WARNED_FULL.remove(player.getUUID());
		} else if (from != null) {
			player.sendSystemMessage(Component.translatable("message.solorust.suit.offline_removed").withStyle(ChatFormatting.GRAY), true);
			level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8f, 1.4f);
			removeSuitEffects(player);
		}

		if (to == null || !to.flight) {
			revokeFlight(player);
		}
	}

	// ================================================================== полёт

	private static void flight(ServerPlayer player, SuitTier tier) {
		UUID id = player.getUUID();
		Abilities abilities = player.getAbilities();

		if (!tier.flight) {
			WAS_FLYING.remove(id);
			return;
		}

		boolean ownFlight = !player.isCreative() && !player.isSpectator();
		boolean hasEnergy = player.hasInfiniteMaterials() || SuitEnergy.get(player, tier) > 0;

		if (ownFlight) {
			boolean dirty = false;

			if (hasEnergy && !abilities.mayfly) {
				abilities.mayfly = true;
				FLIGHT.add(id);
				dirty = true;
			} else if (!hasEnergy && abilities.mayfly && FLIGHT.contains(id)) {
				abilities.mayfly = false;
				abilities.flying = false;
				dirty = true;
				player.sendSystemMessage(Component.translatable("message.solorust.suit.no_energy").withStyle(ChatFormatting.RED), true);
			}

			if (FLIGHT.contains(id) && abilities.getFlyingSpeed() != tier.flySpeed) {
				abilities.setFlyingSpeed(tier.flySpeed);
				dirty = true;
			}

			if (dirty) {
				player.onUpdateAbilities();
			}
		}

		boolean flying = abilities.flying;

		if (flying) {
			player.fallDistance = 0;
			thrusterParticles(player, tier);

			if (!WAS_FLYING.contains(id)) {
				player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.9f, 0.6f);
			}

			WAS_FLYING.add(id);
		} else {
			WAS_FLYING.remove(id);
		}
	}

	private static void thrusterParticles(ServerPlayer player, SuitTier tier) {
		ServerLevel level = player.level();
		boolean boost = player.isSprinting();
		net.minecraft.core.particles.ParticleOptions flame = switch (tier) {
			case NANO -> ParticleTypes.SOUL_FIRE_FLAME;
			case SINGULARITY -> ParticleTypes.END_ROD;
			case ARCHMAGE -> ParticleTypes.ENCHANT;
			case APEX -> ParticleTypes.CLOUD;
			default -> ParticleTypes.FLAME;
		};

		// «Апекс» на форсаже преодолевает звуковой барьер.
		if (tier == SuitTier.APEX && boost && player.level().getGameTime() % 20 == 0) {
			level.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY(0.5), player.getZ(), 1, 0, 0, 0, 0);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.7f, 0.5f);
		}
		Vec3 look = player.getViewVector(1f);
		Vec3 flat = new Vec3(look.x, 0, look.z);
		flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
		Vec3 side = new Vec3(-flat.z, 0, flat.x).scale(0.15 * player.getBbWidth() / 0.6);

		// Две струи из ботинок, при форсаже — длиннее и с дымом.
		for (int s = -1; s <= 1; s += 2) {
			Vec3 p = player.position().add(side.scale(s)).add(0, -0.05, 0);
			Vec3 v = player.getDeltaMovement().scale(-0.3).add(0, -0.25, 0);
			level.sendParticles(flame, p.x, p.y, p.z, 0, v.x, v.y, v.z, boost ? 1.4 : 0.8);

			if (boost) {
				level.sendParticles(ParticleTypes.SMOKE, p.x, p.y - 0.2, p.z, 1, 0.05, 0.05, 0.05, 0.01);
			}
		}

		if (boost && tier != SuitTier.ARCHMAGE && tier != SuitTier.APEX && player.level().getGameTime() % 8 == 0) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.25f, 0.5f);
		}
	}

	private static void revokeFlight(ServerPlayer player) {
		if (!FLIGHT.remove(player.getUUID())) {
			return;
		}

		Abilities abilities = player.getAbilities();

		if (!player.isCreative() && !player.isSpectator()) {
			abilities.mayfly = false;
			abilities.flying = false;
		}

		abilities.setFlyingSpeed(DEFAULT_FLY_SPEED);
		player.onUpdateAbilities();
		WAS_FLYING.remove(player.getUUID());
	}

	// ================================================================== энергия (раз в 0.5 с)

	private static void energy(ServerPlayer player, SuitTier tier) {
		if (player.hasInfiniteMaterials()) {
			SuitEnergy.set(player, tier, tier.maxEnergy);
			return;
		}

		int energy = SuitEnergy.get(player, tier);
		boolean flying = tier.flight && player.getAbilities().flying;
		boolean busy = player.isUsingItem() && player.getUseItem().getItem() instanceof SuitWeaponItem;

		if (flying) {
			int drain = (tier.flightDrain * (player.isSprinting() ? 2 : 1) + 1) / 2;
			energy -= drain;
		} else if (!busy) {
			// Заряд идёт, пока не летишь и не стреляешь; на земле — в полтора раза быстрее.
			int gain = player.onGround() ? tier.recharge * 3 / 4 + 1 : tier.recharge / 2;
			energy += gain;
		}

		SuitEnergy.set(player, tier, energy);
	}

	// ================================================================== бонусы

	private static void applyStats(ServerPlayer player, SuitTier tier) {
		SuitTier.Stats s = tier == null ? null : tier.stats;
		modifier(player, Attributes.MOVEMENT_SPEED, MOD_SPEED, s == null ? 0 : s.speed(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		modifier(player, Attributes.ATTACK_DAMAGE, MOD_ATTACK, s == null ? 0 : s.attack(), AttributeModifier.Operation.ADD_VALUE);
		modifier(player, Attributes.JUMP_STRENGTH, MOD_JUMP, s == null ? 0 : s.jump(), AttributeModifier.Operation.ADD_VALUE);
		modifier(player, Attributes.SCALE, MOD_SCALE, s == null ? 0 : s.scale(), AttributeModifier.Operation.ADD_VALUE);
		modifier(player, Attributes.STEP_HEIGHT, MOD_STEP, s == null ? 0 : s.step(), AttributeModifier.Operation.ADD_VALUE);
		modifier(player, Attributes.ENTITY_INTERACTION_RANGE, MOD_REACH_ENTITY, s == null ? 0 : s.reach(), AttributeModifier.Operation.ADD_VALUE);
		modifier(player, Attributes.BLOCK_INTERACTION_RANGE, MOD_REACH_BLOCK, s == null ? 0 : s.reach(), AttributeModifier.Operation.ADD_VALUE);
		modifier(player, Attributes.MAX_HEALTH, MOD_HEALTH, s == null ? 0 : s.health(), AttributeModifier.Operation.ADD_VALUE);
		modifier(player, Attributes.SAFE_FALL_DISTANCE, MOD_SAFE_FALL, s == null ? 0 : s.safeFall(), AttributeModifier.Operation.ADD_VALUE);

		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
	}

	private static void modifier(ServerPlayer player, Holder<Attribute> attribute, Identifier id, double value, AttributeModifier.Operation operation) {
		AttributeInstance instance = player.getAttribute(attribute);

		if (instance == null) {
			return;
		}

		if (value == 0) {
			instance.removeModifier(id);
		} else {
			instance.addOrUpdateTransientModifier(new AttributeModifier(id, value, operation));
		}
	}

	private static void effects(ServerPlayer player, SuitTier tier) {
		if (tier != SuitTier.MK1) {
			// Шлем с ночным видением (иконку не показываем — это просто «режим шлема»).
			refresh(player, MobEffects.NIGHT_VISION, 0);
		}

		switch (tier) {
			case NANO -> {
				refresh(player, MobEffects.WATER_BREATHING, 0);
				refresh(player, MobEffects.FIRE_RESISTANCE, 0);
			}
			case TITAN, SINGULARITY -> refresh(player, MobEffects.FIRE_RESISTANCE, 0);
			// «Апекс» почти неуязвим, пока сыт; голодный — обычный человек в костюме.
			case APEX -> refresh(player, MobEffects.RESISTANCE, player.getFoodData().getFoodLevel() >= 14 ? 2 : 0);
			case MINER -> refresh(player, MobEffects.HASTE, 1);
			case BUILDER -> refresh(player, MobEffects.HASTE, 0);
			case DIVER -> {
				refresh(player, MobEffects.WATER_BREATHING, 0);
				refresh(player, MobEffects.CONDUIT_POWER, 0);

				if (player.isInWater()) {
					refresh(player, MobEffects.DOLPHINS_GRACE, 0);
				}
			}
			default -> {
			}
		}
	}

	private static void refresh(ServerPlayer player, Holder<MobEffect> effect, int amplifier) {
		MobEffectInstance current = player.getEffect(effect);

		// Чужие (не костюмные) эффекты — зелья, маяк — не трогаем.
		if (current != null && !current.isAmbient()) {
			return;
		}

		if (current == null || current.getDuration() < 260 || current.getAmplifier() != amplifier) {
			player.addEffect(new MobEffectInstance(effect, 400, amplifier, true, false, false));
		}
	}

	private static void removeSuitEffects(ServerPlayer player) {
		for (Holder<MobEffect> effect : java.util.List.of(MobEffects.NIGHT_VISION, MobEffects.WATER_BREATHING, MobEffects.FIRE_RESISTANCE,
				MobEffects.RESISTANCE, MobEffects.HASTE, MobEffects.CONDUIT_POWER, MobEffects.DOLPHINS_GRACE)) {
			MobEffectInstance current = player.getEffect(effect);

			// Снимаем только «костюмные» эффекты (невидимые, ambient), зелья игрока не трогаем.
			if (current != null && current.isAmbient() && !current.isVisible()) {
				player.removeEffect(effect);
			}
		}
	}

	// ================================================================== оружие в инвентаре

	private static void syncWeapons(ServerPlayer player, SuitTier tier) {
		Inventory inventory = player.getInventory();
		Set<SuitWeapon> present = new HashSet<>();

		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);

			if (stack.getItem() instanceof SuitWeaponItem item) {
				// Лишнее (чужое поколение, снятый костюм, дубликаты) — убираем.
				if (tier == null || !tier.has(item.weapon()) || !present.add(item.weapon())) {
					inventory.setItem(i, ItemStack.EMPTY);
				}
			}
		}

		ItemStack carried = player.containerMenu.getCarried();

		if (carried.getItem() instanceof SuitWeaponItem item) {
			if (tier == null || !tier.has(item.weapon()) || !present.add(item.weapon())) {
				player.containerMenu.setCarried(ItemStack.EMPTY);
			}
		}

		if (tier == null) {
			return;
		}

		boolean full = false;

		for (SuitWeapon weapon : tier.weapons) {
			if (present.contains(weapon)) {
				continue;
			}

			if (!inventory.add(new ItemStack(ModSuits.WEAPONS.get(weapon)))) {
				full = true;
			}
		}

		if (full && WARNED_FULL.add(player.getUUID())) {
			player.sendSystemMessage(Component.translatable("message.solorust.suit.inventory_full").withStyle(ChatFormatting.YELLOW), true);
		}

		if (!full) {
			WARNED_FULL.remove(player.getUUID());
		}
	}

	// ================================================================== урон

	private static boolean allowDamage(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source, float amount) {
		if (!(entity instanceof ServerPlayer player)) {
			return true;
		}

		SuitTier tier = SuitTier.worn(player);

		if (tier == null) {
			return true;
		}

		// Летающие костюмы и Титан гасят падение двигателями / амортизаторами.
		if (source.is(DamageTypeTags.IS_FALL) && (tier.flight || tier == SuitTier.TITAN || tier == SuitTier.SCOUT)) {
			return false;
		}

		// Нанощит поглощает любой урон, кроме «неблокируемого» (пустота, /kill), пока есть энергия.
		if (player.isUsingItem() && player.getUseItem().getItem() instanceof SuitWeaponItem item && (item.weapon() == SuitWeapon.NANO_SHIELD || item.weapon() == SuitWeapon.RUNE_SHIELD)
				&& !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			int cost = Math.max(4, Math.round(amount * 8));

			if (SuitEnergy.use(player, tier, cost)) {
				ServerLevel level = player.level();
				level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 1.0f, 1.4f);
				level.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY(0.6), player.getZ(), 12, 0.5, 0.5, 0.5, 0.2);
				return false;
			}
		}

		return true;
	}

	// ================================================================== топот Титана

	private static void titanStomp(ServerPlayer player, SuitTier tier) {
		if (tier != SuitTier.TITAN) {
			return;
		}

		UUID id = player.getUUID();

		double y = player.getY();
		Double lastY = LAST_Y.put(id, y);

		if (!player.onGround()) {
			// Высоту копим по фактическому снижению: так ударная волна работает и после пике в полёте,
			// где fallDistance всегда обнулён. Набор высоты сбрасывает накопленное.
			double dy = lastY == null ? 0 : y - lastY;

			// Медленный спуск в полёте не считается — только падение или пике.
			if (dy < -0.25) {
				FALLING.merge(id, -dy, Double::sum);
			} else if (dy > 0.05) {
				FALLING.remove(id);
			}

			return;
		}

		Double fall = FALLING.remove(id);

		if (fall == null || fall < 3.5) {
			return;
		}

		ServerLevel level = player.level();
		double radius = Math.min(3.0 + fall / 3.0, 9.0);
		float damage = (float) Math.min(5.0 + fall * 1.2, 34.0);

		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius, 2, radius),
				e -> SuitWeapons.isTarget(e, player))) {
			double d = e.distanceTo(player);

			if (d > radius) {
				continue;
			}

			float scaled = (float) (damage * (1.0 - d / (radius * 1.5)));
			SuitWeapons.hurt(level, player, e, scaled);
			SuitWeapons.push(e, e.position().subtract(player.position()), 1.3, 0.55);
		}

		BlockState ground = level.getBlockState(player.getOnPos());

		for (int i = 0; i < 32; i++) {
			double a = i / 32.0 * Math.PI * 2;
			double x = player.getX() + Math.cos(a) * radius * 0.5;
			double z = player.getZ() + Math.sin(a) * radius * 0.5;

			if (!ground.isAir()) {
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), x, player.getY() + 0.1, z, 3, 0.2, 0.05, 0.2, 0.15);
			}

			level.sendParticles(ParticleTypes.CLOUD, x, player.getY() + 0.1, z, 0, Math.cos(a), 0.05, Math.sin(a), 0.4);
		}

		level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.2, player.getZ(), 2, 0.5, 0.1, 0.5, 0);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.6f, 0.6f);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6f, 1.3f);
	}
}
