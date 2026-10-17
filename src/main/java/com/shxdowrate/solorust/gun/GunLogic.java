package com.shxdowrate.solorust.gun;

import java.util.List;

import com.shxdowrate.solorust.item.GunItem;
import com.shxdowrate.solorust.network.RecoilPayload;
import com.shxdowrate.solorust.registry.ModComponents;
import com.shxdowrate.solorust.registry.ModSounds;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Серверная логика стрельбы и перезарядки. */
public final class GunLogic {
	private GunLogic() {
	}

	// ------------------------------------------------------------------ стрельба

	public static void fire(ServerPlayer player) {
		ItemStack stack = player.getMainHandItem();

		if (!(stack.getItem() instanceof GunItem gun) || player.isSpectator()) {
			return;
		}

		if (player.getCooldowns().isOnCooldown(stack)) {
			return;
		}

		GunDef def = gun.def();
		ServerLevel level = player.level();
		int ammo = stack.getOrDefault(ModComponents.AMMO, 0);

		if (ammo <= 0) {
			sound(level, player, ModSounds.DRY_FIRE, 0.7f, 1.0f);
			player.getCooldowns().addCooldown(stack, 6);

			// Пустой магазин — сразу пробуем перезарядиться, как в Расте.
			reload(player);
			return;
		}

		RandomSource random = player.getRandom();

		if (def.misfire() > 0 && random.nextFloat() < def.misfire()) {
			sound(level, player, net.minecraft.sounds.SoundEvents.FLINTANDSTEEL_USE, 0.8f, 1.2f);
			player.getCooldowns().addCooldown(stack, 8);
			return;
		}

		GunStats stats = GunStats.of(def, stack, player);
		Item ammoItem = ammoItem(stack, def);
		AmmoClass.Props props = AmmoClass.props(ammoItem);

		stack.set(ModComponents.AMMO, ammo - 1);
		boolean hit = shoot(level, player, def, stats, props, random);
		player.getCooldowns().addCooldown(stack, stats.interval());

		float pitchKick = stats.recoil() * (0.85f + random.nextFloat() * 0.3f);
		float yawKick = (random.nextFloat() - 0.5f) * stats.recoil() * 0.6f;
		ServerPlayNetworking.send(player, new RecoilPayload(pitchKick, yawKick, hit));
	}

	private static Item ammoItem(ItemStack stack, GunDef def) {
		String id = stack.getOrDefault(ModComponents.AMMO_TYPE, "");

		if (!id.isEmpty()) {
			Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));

			if (def.ammo().items().contains(item)) {
				return item;
			}
		}

		return def.ammo().items().get(0);
	}

	private static boolean shoot(ServerLevel level, ServerPlayer player, GunDef def, GunStats stats, AmmoClass.Props props, RandomSource random) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);
		float spread = Math.max(0.05f, stats.spread() + props.extraSpread());
		boolean hitSomething = false;

		for (int p = 0; p < props.pellets(); p++) {
			Vec3 dir = spread(look, spread, random);
			Vec3 end = eye.add(dir.scale(def.range()));

			BlockHitResult blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
			Vec3 limit = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

			Entity target = null;
			Vec3 targetPoint = null;
			double best = eye.distanceToSqr(limit);
			List<Entity> candidates = level.getEntities(player, new AABB(eye, limit).inflate(1.0),
					e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator());

			for (Entity entity : candidates) {
				var hit = entity.getBoundingBox().inflate(0.12).clip(eye, limit);

				if (hit.isPresent()) {
					double d = eye.distanceToSqr(hit.get());

					if (d < best) {
						best = d;
						target = entity;
						targetPoint = hit.get();
					}
				}
			}

			Vec3 impact = targetPoint != null ? targetPoint : limit;

			if (p == 0 || p % 3 == 0) {
				tracer(level, eye.add(dir.scale(1.5)).add(0, -0.15, 0), impact);
			}

			if (target != null) {
				float damage = def.damage() * props.damage() * stats.damageMult();

				// Падение урона на дистанции: после 60% дальности урон плавно снижается до 50%.
				double dist = Math.sqrt(best);
				double falloffStart = def.range() * 0.6;

				if (dist > falloffStart) {
					damage *= (float) Mth.lerp((dist - falloffStart) / (def.range() - falloffStart), 1.0, 0.5);
				}

				if (targetPoint.y >= target.getEyeY() - 0.22) {
					damage *= 1.5f;
					level.sendParticles(ParticleTypes.CRIT, targetPoint.x, targetPoint.y, targetPoint.z, 5, 0.1, 0.1, 0.1, 0.2);
				}

				target.invulnerableTime = 0;
				target.hurtServer(level, level.damageSources().playerAttack(player), damage);
				level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, targetPoint.x, targetPoint.y, targetPoint.z, 1, 0.1, 0.1, 0.1, 0.05);
				hitSomething = true;
			} else if (blockHit.getType() == HitResult.Type.BLOCK) {
				BlockState state = level.getBlockState(blockHit.getBlockPos());

				if (!state.isAir()) {
					level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), impact.x, impact.y, impact.z, 4, 0.05, 0.05, 0.05, 0.1);
					level.sendParticles(ParticleTypes.SMOKE, impact.x, impact.y, impact.z, 1, 0.02, 0.02, 0.02, 0.01);
				}
			}
		}

		Vec3 muzzle = eye.add(look.scale(1.2)).add(0, -0.2, 0);

		if (!stats.silenced()) {
			level.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 1, 0.02, 0.02, 0.02, 0.0);
		}

		level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 2, 0.03, 0.03, 0.03, 0.01);

		SoundEvent shot = stats.silenced() ? ModSounds.SHOT_SILENCED : shotSound(def);
		float volume = stats.silenced() ? 0.6f : (shot == ModSounds.SHOT_SNIPER ? 4.0f : 2.5f);
		sound(level, player, shot, volume, 0.95f + random.nextFloat() * 0.1f);
		return hitSomething;
	}

	private static SoundEvent shotSound(GunDef def) {
		return switch (def.ammo()) {
			case SHOTGUN, EOKA -> ModSounds.SHOT_SHOTGUN;
			case NAIL -> ModSounds.SHOT_NAIL;
			case PISTOL -> def.automatic() ? ModSounds.SHOT_SMG : ModSounds.SHOT_PISTOL;
			case RIFLE -> def.interval() >= 25 ? ModSounds.SHOT_SNIPER : ModSounds.SHOT_RIFLE;
		};
	}

	private static Vec3 spread(Vec3 look, float degrees, RandomSource random) {
		double rad = Math.toRadians(degrees);
		double yawOffset = random.nextGaussian() * rad * 0.5;
		double pitchOffset = random.nextGaussian() * rad * 0.5;
		return look.yRot((float) yawOffset).xRot((float) pitchOffset).normalize();
	}

	private static void tracer(ServerLevel level, Vec3 from, Vec3 to) {
		Vec3 delta = to.subtract(from);
		int steps = Mth.clamp((int) (delta.length() / 2.5), 1, 48);

		for (int i = 0; i < steps; i++) {
			Vec3 point = from.add(delta.scale(i / (double) steps));
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 1, 0, 0, 0, 0);
		}
	}

	private static void sound(ServerLevel level, ServerPlayer player, SoundEvent sound, float volume, float pitch) {
		level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
	}

	// ------------------------------------------------------------------ перезарядка

	public static void reload(ServerPlayer player) {
		ItemStack stack = player.getMainHandItem();

		if (!(stack.getItem() instanceof GunItem gun) || player.getCooldowns().isOnCooldown(stack)) {
			return;
		}

		GunDef def = gun.def();
		int mag = GunStats.magazine(def, GunStats.mods(stack));
		int current = stack.getOrDefault(ModComponents.AMMO, 0);
		boolean creative = player.hasInfiniteMaterials();
		Inventory inventory = player.getInventory();

		Item loaded = current > 0 ? ammoItem(stack, def) : null;
		Item chosen = null;

		// Сначала тот же патрон, что уже заряжен; иначе первый подходящий из инвентаря.
		if (loaded != null && (creative || count(inventory, loaded) > 0)) {
			chosen = loaded;
		} else {
			for (Item candidate : def.ammo().items()) {
				if (creative || count(inventory, candidate) > 0) {
					chosen = candidate;
					break;
				}
			}
		}

		if (chosen == null) {
			player.sendSystemMessage(Component.translatable("message.solorust.no_ammo",
					Component.translatable(def.ammo().items().get(0).getDescriptionId())), true);
			return;
		}

		if (loaded != null && loaded != chosen) {
			// Разряжаем другой тип патронов обратно в инвентарь.
			giveBack(player, new ItemStack(loaded, current));
			current = 0;
		}

		int need = mag - current;

		if (need <= 0) {
			return;
		}

		int taken = creative ? need : take(inventory, chosen, need);
		stack.set(ModComponents.AMMO, current + taken);
		stack.set(ModComponents.AMMO_TYPE, BuiltInRegistries.ITEM.getKey(chosen).toString());
		player.getCooldowns().addCooldown(stack, def.reload());

		ServerLevel level = player.level();
		sound(level, player, ModSounds.RELOAD, 0.8f, 1.0f);
		ReloadSounds.schedule(player, def.reload());
	}

	private static int count(Inventory inventory, Item item) {
		int total = 0;

		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack s = inventory.getItem(i);

			if (s.is(item)) {
				total += s.getCount();
			}
		}

		return total;
	}

	private static int take(Inventory inventory, Item item, int amount) {
		int left = amount;

		for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
			ItemStack s = inventory.getItem(i);

			if (s.is(item)) {
				int t = Math.min(left, s.getCount());
				s.shrink(t);
				left -= t;
			}
		}

		return amount - left;
	}

	private static void giveBack(ServerPlayer player, ItemStack stack) {
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}
	}
}
