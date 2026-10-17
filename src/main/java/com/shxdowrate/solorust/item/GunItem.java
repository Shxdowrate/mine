package com.shxdowrate.solorust.item;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Огнестрел: мгновенный выстрел лучом. Патроны берутся из инвентаря.
 * Автоматы стреляют очередью, пока зажата ПКМ (майн сам повторяет «использование» каждые 4 тика).
 */
public class GunItem extends Item {
	/**
	 * @param ammo     какие патроны ест
	 * @param damage   урон одной пули/дробины (в полусердечках)
	 * @param cooldown перезарядка между выстрелами, тики
	 * @param range    дальность, блоки
	 * @param pellets  сколько дробин за выстрел
	 * @param spread   разброс, градусы
	 * @param misfire  шанс осечки (для Эоки)
	 */
	public record Stats(Supplier<Item> ammo, float damage, int cooldown, double range, int pellets, float spread, float misfire) {
	}

	private final Stats stats;

	public GunItem(Properties properties, Stats stats) {
		super(properties);
		this.stats = stats;
	}

	public Stats stats() {
		return stats;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (player.getCooldowns().isOnCooldown(stack)) {
			return InteractionResult.FAIL;
		}

		if (level instanceof ServerLevel serverLevel) {
			if (!hasAmmo(player)) {
				serverLevel.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 1.6f);
				player.getCooldowns().addCooldown(stack, 10);

				if (player instanceof ServerPlayer serverPlayer) {
					serverPlayer.sendSystemMessage(Component.translatable("message.solorust.no_ammo", Component.translatable(stats.ammo().get().getDescriptionId())), true);
				}

				return InteractionResult.FAIL;
			}

			RandomSource random = player.getRandom();

			if (stats.misfire() > 0 && random.nextFloat() < stats.misfire()) {
				serverLevel.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
				player.getCooldowns().addCooldown(stack, 8);
				return InteractionResult.CONSUME;
			}

			consumeAmmo(player);
			fire(serverLevel, player, random);
			player.getCooldowns().addCooldown(stack, stats.cooldown());
		}

		return InteractionResult.CONSUME;
	}

	private boolean hasAmmo(Player player) {
		if (player.hasInfiniteMaterials()) {
			return true;
		}

		Item ammo = stats.ammo().get();
		Inventory inventory = player.getInventory();

		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i).is(ammo)) {
				return true;
			}
		}

		return false;
	}

	private void consumeAmmo(Player player) {
		if (player.hasInfiniteMaterials()) {
			return;
		}

		Item ammo = stats.ammo().get();
		Inventory inventory = player.getInventory();

		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack slot = inventory.getItem(i);

			if (slot.is(ammo)) {
				slot.shrink(1);
				return;
			}
		}
	}

	private void fire(ServerLevel level, Player player, RandomSource random) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0f);

		for (int p = 0; p < stats.pellets(); p++) {
			Vec3 dir = spread(look, stats.spread(), random);
			Vec3 end = eye.add(dir.scale(stats.range()));

			BlockHitResult blockHit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
			Vec3 limit = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

			Entity target = null;
			Vec3 targetPoint = null;
			double best = eye.distanceToSqr(limit);
			List<Entity> candidates = level.getEntities(player, new AABB(eye, limit).inflate(1.0),
					e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator());

			for (Entity entity : candidates) {
				var hit = entity.getBoundingBox().inflate(0.15).clip(eye, limit);

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
			tracer(level, eye.add(dir.scale(1.2)), impact);

			if (target != null) {
				float damage = stats.damage();

				// Хедшот: попадание выше уровня глаз минус чуть-чуть — урон x1.5.
				if (targetPoint.y >= target.getEyeY() - 0.2) {
					damage *= 1.5f;
					level.sendParticles(ParticleTypes.CRIT, targetPoint.x, targetPoint.y, targetPoint.z, 6, 0.1, 0.1, 0.1, 0.2);
				}

				target.invulnerableTime = 0;
				target.hurtServer(level, level.damageSources().playerAttack(player), damage);
				level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, targetPoint.x, targetPoint.y, targetPoint.z, 2, 0.1, 0.1, 0.1, 0.05);
			} else if (blockHit.getType() == HitResult.Type.BLOCK) {
				BlockState state = level.getBlockState(blockHit.getBlockPos());

				if (!state.isAir()) {
					level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), impact.x, impact.y, impact.z, 6, 0.05, 0.05, 0.05, 0.1);
				}
			}
		}

		Vec3 muzzle = eye.add(look.scale(1.0));
		level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y - 0.1, muzzle.z, 3, 0.03, 0.03, 0.03, 0.01);
		level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS,
				1.2f, pitch());
	}

	private float pitch() {
		if (stats.damage() >= 20) {
			return 0.5f;
		}

		if (stats.pellets() > 1) {
			return 0.6f;
		}

		return stats.cooldown() <= 3 ? 1.3f : 0.9f;
	}

	private static Vec3 spread(Vec3 look, float degrees, RandomSource random) {
		if (degrees <= 0) {
			return look;
		}

		double rad = Math.toRadians(degrees);
		double yawOffset = random.nextGaussian() * rad * 0.5;
		double pitchOffset = random.nextGaussian() * rad * 0.5;
		Vec3 rotated = look.yRot((float) yawOffset).xRot((float) pitchOffset);
		return rotated.normalize();
	}

	private static void tracer(ServerLevel level, Vec3 from, Vec3 to) {
		Vec3 delta = to.subtract(from);
		double length = delta.length();
		int steps = Mth.clamp((int) (length / 2.0), 1, 40);

		for (int i = 0; i < steps; i++) {
			Vec3 point = from.add(delta.scale(i / (double) steps));
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 1, 0, 0, 0, 0);
		}
	}
}
