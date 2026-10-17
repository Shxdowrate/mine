package com.shxdowrate.solorust.suit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.shxdowrate.solorust.registry.ModComponents;
import com.shxdowrate.solorust.registry.ModSuits;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Надевание и снятие костюма «с анимацией»: станция собирает его манипуляторами, кейс раскрывается,
 * нанокостюм растекается по телу, «Титан» собирается из капсулы, вызванный костюм прилетает по воздуху.
 * Части надеваются по очереди — ботинки, поножи, нагрудник и последним шлем.
 */
public final class SuitAssembly {
	public enum Style {
		/** Сборочная станция: манипуляторы, искры, игрока держит на месте. */
		STATION(40, true),
		/** Кейс раскрывается на тебе. */
		CASE(18, false),
		/** Наночастицы растекаются по телу. */
		NANO(24, false),
		/** Капсула «Титана», упавшая с неба. */
		POD(30, true),
		/** Костюм прилетел со станции и собирается на лету. */
		RECALL(16, false);

		final int duration;
		final boolean hold;

		Style(int duration, boolean hold) {
			this.duration = duration;
			this.hold = hold;
		}
	}

	/** Порядок надевания. */
	private static final EquipmentSlot[] ORDER = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

	private static final DustParticleOptions NANO_DUST = new DustParticleOptions(0xB46CFF, 1.0f);

	private static final class Job {
		final ServerPlayer player;
		final Style style;
		final ItemStack[] pieces;
		final Vec3 from;
		final int travel;
		int age;
		int equipped;

		Job(ServerPlayer player, Style style, ItemStack[] pieces, Vec3 from, int travel) {
			this.player = player;
			this.style = style;
			this.pieces = pieces;
			this.from = from;
			this.travel = travel;
		}
	}

	private static final Map<UUID, Job> JOBS = new HashMap<>();

	private SuitAssembly() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> tick());
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			// Недособранные костюмы не теряем — отдаём владельцу.
			for (Job job : JOBS.values()) {
				dropRemaining(job);
			}

			JOBS.clear();
		});
	}

	public static boolean busy(ServerPlayer player) {
		return JOBS.containsKey(player.getUUID());
	}

	// ================================================================== надевание

	/** Новый комплект указанного поколения (для кейса, капсулы, капсулы Титана). */
	public static List<ItemStack> freshSuit(SuitTier tier, int energy) {
		List<ItemStack> list = new ArrayList<>();

		for (ArmorType type : new ArmorType[] {ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS}) {
			ItemStack stack = new ItemStack(ModSuits.piece(tier, type));

			if (type == ArmorType.CHESTPLATE && energy >= 0) {
				stack.set(ModComponents.SUIT_ENERGY, energy);
			}

			list.add(stack);
		}

		return list;
	}

	/**
	 * Начать надевание. Надетая сейчас броня уходит в инвентарь. from — откуда летит костюм (для вызова), иначе null.
	 */
	public static boolean start(ServerPlayer player, Style style, List<ItemStack> suit, Vec3 from) {
		if (busy(player)) {
			return false;
		}

		ItemStack[] pieces = new ItemStack[4];

		for (ItemStack stack : suit) {
			if (stack.getItem() instanceof SuitArmorItem armor) {
				int index = switch (armor.type()) {
					case BOOTS -> 0;
					case LEGGINGS -> 1;
					case CHESTPLATE -> 2;
					default -> 3;
				};
				pieces[index] = stack;
			}
		}

		for (int i = 0; i < 4; i++) {
			if (pieces[i] == null) {
				pieces[i] = ItemStack.EMPTY;
			}

			// Освобождаем слот брони.
			ItemStack worn = player.getItemBySlot(ORDER[i]);

			if (!worn.isEmpty()) {
				player.setItemSlot(ORDER[i], ItemStack.EMPTY);

				if (!player.getInventory().add(worn)) {
					player.drop(worn, false);
				}
			}
		}

		int travel = 0;

		if (from != null) {
			travel = (int) Math.max(10, Math.min(160, from.distanceTo(player.position()) / 1.8));
		}

		JOBS.put(player.getUUID(), new Job(player, style, pieces, from, travel));
		ServerLevel level = player.level();

		switch (style) {
			case STATION -> sound(level, player, SoundEvents.BEACON_ACTIVATE, 0.8f, 1.2f);
			case CASE -> sound(level, player, SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0f, 0.7f);
			case NANO -> sound(level, player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 0.5f);
			case POD -> sound(level, player, SoundEvents.IRON_DOOR_OPEN, 1.2f, 0.6f);
			case RECALL -> {
				level.playSound(null, from.x, from.y, from.z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.5f, 0.6f);
				player.sendOverlayMessage(Component.translatable("message.solorust.suit.incoming"));
			}
		}

		return true;
	}

	private static void tick() {
		if (JOBS.isEmpty()) {
			return;
		}

		for (Iterator<Job> it = JOBS.values().iterator(); it.hasNext(); ) {
			Job job = it.next();

			if (job.player.isRemoved() || !job.player.isAlive()) {
				dropRemaining(job);
				it.remove();
				continue;
			}

			job.age++;

			if (job.age <= job.travel) {
				fly(job);
				continue;
			}

			int t = job.age - job.travel;
			int step = Math.max(1, job.style.duration / 4);
			ServerPlayer player = job.player;
			ServerLevel level = player.level();

			if (job.style.hold) {
				player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 4, 6, true, false, false));
			}

			effects(level, player, job.style, t);

			while (job.equipped < 4 && t >= step * (job.equipped + 1)) {
				ItemStack piece = job.pieces[job.equipped];

				if (!piece.isEmpty()) {
					player.setItemSlot(ORDER[job.equipped], piece);
				}

				job.pieces[job.equipped] = ItemStack.EMPTY;
				clank(level, player, job.style, job.equipped);
				job.equipped++;
			}

			if (job.equipped >= 4) {
				it.remove();
			}
		}
	}

	/** Костюм летит со станции: светящийся сгусток с реактивным следом. */
	private static void fly(Job job) {
		ServerPlayer player = job.player;
		ServerLevel level = player.level();
		float p = job.age / (float) job.travel;
		Vec3 target = player.position().add(0, player.getBbHeight() * 0.5, 0);
		// Дуга: летит выше прямой линии.
		Vec3 pos = job.from.lerp(target, p).add(0, Math.sin(p * Math.PI) * Math.min(12, job.from.distanceTo(target) * 0.25), 0);

		level.sendParticles(ParticleTypes.FLAME, true, false, pos.x, pos.y, pos.z, 3, 0.1, 0.1, 0.1, 0.02);
		level.sendParticles(ParticleTypes.SMOKE, true, false, pos.x, pos.y, pos.z, 2, 0.15, 0.15, 0.15, 0.01);
		level.sendParticles(ParticleTypes.END_ROD, true, false, pos.x, pos.y, pos.z, 1, 0.2, 0.3, 0.2, 0.0);

		if (job.age % 10 == 0) {
			level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.6f, 0.5f);
		}

		if (job.age == job.travel) {
			sound(level, player, SoundEvents.ANVIL_LAND, 0.6f, 1.6f);
		}
	}

	private static void effects(ServerLevel level, ServerPlayer player, Style style, int t) {
		RandomSource r = player.getRandom();
		double h = player.getBbHeight();
		// Уровень «сборки» поднимается снизу вверх вместе с надеваемыми частями.
		double rise = Math.min(1.0, t / (double) style.duration) * h;
		Vec3 base = player.position();

		switch (style) {
			case STATION -> {
				// Четыре «манипулятора» вокруг игрока сыплют искрами.
				for (int i = 0; i < 4; i++) {
					double a = i * Math.PI / 2 + t * 0.15;
					double x = base.x + Math.cos(a) * 0.8;
					double z = base.z + Math.sin(a) * 0.8;
					level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, base.y + rise, z, 2, 0.05, 0.05, 0.05, 0.05);
				}

				if (t % 6 == 0) {
					sound(level, player, SoundEvents.PISTON_EXTEND, 0.5f, 1.4f + r.nextFloat() * 0.3f);
				}
			}
			case CASE -> level.sendParticles(ParticleTypes.CRIT, base.x, base.y + rise, base.z, 4, 0.35, 0.1, 0.35, 0.05);
			case NANO -> {
				// Частицы растекаются от груди по всему телу.
				Vec3 chest = base.add(0, h * 0.65, 0);

				for (int i = 0; i < 8; i++) {
					Vec3 to = base.add((r.nextDouble() - 0.5) * 0.7, r.nextDouble() * h, (r.nextDouble() - 0.5) * 0.7);
					Vec3 v = to.subtract(chest).scale(0.3);
					level.sendParticles(NANO_DUST, chest.x + v.x, chest.y + v.y, chest.z + v.z, 1, 0.02, 0.02, 0.02, 0.0);
				}
			}
			case POD -> {
				level.sendParticles(ParticleTypes.CLOUD, base.x, base.y + 0.2, base.z, 3, 0.6, 0.1, 0.6, 0.02);
				level.sendParticles(ParticleTypes.ELECTRIC_SPARK, base.x, base.y + rise, base.z, 3, 0.4, 0.1, 0.4, 0.05);
			}
			case RECALL -> level.sendParticles(ParticleTypes.FLAME, base.x, base.y + rise, base.z, 3, 0.35, 0.1, 0.35, 0.02);
		}
	}

	private static void clank(ServerLevel level, ServerPlayer player, Style style, int piece) {
		SoundEvent sound = switch (style) {
			case NANO -> SoundEvents.AMETHYST_BLOCK_CHIME;
			case CASE -> SoundEvents.CHAIN_PLACE;
			default -> SoundEvents.ANVIL_USE;
		};
		sound(level, player, sound, 0.5f, 1.2f + piece * 0.15f);

		if (piece == 3) {
			// Шлем закрылся — костюм в строю.
			sound(level, player, SoundEvents.IRON_DOOR_CLOSE, 0.8f, 1.3f);
		}
	}

	private static void dropRemaining(Job job) {
		for (ItemStack piece : job.pieces) {
			if (!piece.isEmpty()) {
				if (!job.player.isRemoved() && job.player.getInventory().add(piece)) {
					continue;
				}

				job.player.drop(piece, false);
			}
		}
	}

	// ================================================================== снятие

	/** Снять полный комплект: части исчезают с тела с эффектом; возвращает снятые части (шлем, грудь, ноги, ботинки). */
	public static List<ItemStack> strip(ServerPlayer player, Style style) {
		List<ItemStack> removed = new ArrayList<>();
		ServerLevel level = player.level();

		for (int i = ORDER.length - 1; i >= 0; i--) {
			removed.add(player.getItemBySlot(ORDER[i]));
			player.setItemSlot(ORDER[i], ItemStack.EMPTY);
		}

		Vec3 base = player.position();
		ParticleOptions particle = switch (style) {
			case NANO -> NANO_DUST;
			case POD -> ParticleTypes.FLAME;
			default -> ParticleTypes.CRIT;
		};
		level.sendParticles(particle, base.x, base.y + player.getBbHeight() * 0.5, base.z, 40, 0.4, player.getBbHeight() * 0.4, 0.4, 0.05);

		SoundEvent sound = switch (style) {
			case NANO -> SoundEvents.AMETHYST_BLOCK_RESONATE;
			case POD -> SoundEvents.FIREWORK_ROCKET_LAUNCH;
			default -> SoundEvents.ARMOR_EQUIP_IRON.value();
		};
		sound(level, player, sound, 1.0f, 0.8f);
		return removed;
	}

	/** Энергия с нагрудника среди снятых частей. */
	public static int energyOf(List<ItemStack> pieces, SuitTier tier) {
		for (ItemStack stack : pieces) {
			if (stack.getItem() instanceof SuitArmorItem armor && armor.type() == ArmorType.CHESTPLATE) {
				return SuitEnergy.get(stack, tier);
			}
		}

		return tier.maxEnergy;
	}

	static void sound(ServerLevel level, ServerPlayer player, SoundEvent sound, float volume, float pitch) {
		level.playSound(null, player.getX(), player.getY() + 1, player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
	}
}
