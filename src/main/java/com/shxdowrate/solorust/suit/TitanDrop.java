package com.shxdowrate.solorust.suit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.shxdowrate.solorust.block.entity.TitanPodBlockEntity;
import com.shxdowrate.solorust.registry.ModBlocks;
import com.shxdowrate.solorust.registry.ModSuits;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/** Капсула «Титана»: падает с неба в точку, куда бросили маяк, с огнём и ударом о землю. */
public final class TitanDrop {
	private static final double START_HEIGHT = 70;
	private static final double SPEED = 1.8;

	private static final class Drop {
		final ServerLevel level;
		final ServerPlayer owner;
		final BlockPos target;
		final int energy;
		double y;
		int age;

		Drop(ServerLevel level, ServerPlayer owner, BlockPos target, int energy) {
			this.level = level;
			this.owner = owner;
			this.target = target;
			this.energy = energy;
			this.y = Math.min(level.getMaxY() - 2, target.getY() + START_HEIGHT);
		}
	}

	private static final List<Drop> DROPS = new ArrayList<>();

	private TitanDrop() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> tick());
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			// Капсула не долетела — возвращаем маяк.
			for (Drop d : DROPS) {
				if (!d.owner.isRemoved()) {
					d.owner.getInventory().add(SuitContainerItem.withEnergy(ModSuits.TITAN_BEACON, d.energy));
				}
			}

			DROPS.clear();
		});
	}

	public static boolean call(ServerLevel level, ServerPlayer player, BlockPos target, int energy) {
		if (!level.getBlockState(target).canBeReplaced()) {
			player.sendOverlayMessage(Component.translatable("message.solorust.suit.bad_drop").withStyle(ChatFormatting.RED));
			return false;
		}

		DROPS.add(new Drop(level, player, target, energy));
		player.sendOverlayMessage(Component.translatable("message.solorust.suit.titan_incoming").withStyle(ChatFormatting.GOLD));
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 0.6f);
		level.sendParticles(ParticleTypes.END_ROD, target.getX() + 0.5, target.getY() + 0.2, target.getZ() + 0.5, 30, 0.1, 6, 0.1, 0.0);
		return true;
	}

	private static void tick() {
		for (Iterator<Drop> it = DROPS.iterator(); it.hasNext(); ) {
			Drop d = it.next();
			d.age++;
			d.y -= SPEED;

			double x = d.target.getX() + 0.5;
			double z = d.target.getZ() + 0.5;

			// Маркер цели на земле.
			if (d.age % 4 == 0) {
				d.level.sendParticles(ParticleTypes.END_ROD, x, d.target.getY() + 0.1, z, 2, 0.4, 0, 0.4, 0.0);
			}

			if (d.y > d.target.getY() + 0.5) {
				d.level.sendParticles(ParticleTypes.FLAME, true, false, x, d.y, z, 6, 0.4, 0.6, 0.4, 0.05);
				d.level.sendParticles(ParticleTypes.LARGE_SMOKE, true, false, x, d.y + 1.5, z, 3, 0.3, 0.6, 0.3, 0.02);

				if (d.age % 8 == 0) {
					d.level.playSound(null, x, d.y, z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 3.0f, 0.4f);
				}

				continue;
			}

			land(d, x, z);
			it.remove();
		}
	}

	private static void land(Drop d, double x, double z) {
		BlockPos pos = d.target;

		for (int i = 0; i < 6 && !d.level.getBlockState(pos).canBeReplaced(); i++) {
			pos = pos.above();
		}

		d.level.explode(d.owner.isRemoved() ? null : d.owner, x, pos.getY() + 0.5, z, 2.5f, Level.ExplosionInteraction.NONE);
		d.level.sendParticles(ParticleTypes.CLOUD, x, pos.getY() + 0.2, z, 40, 1.5, 0.2, 1.5, 0.1);

		BlockState pod = ModBlocks.TITAN_POD.defaultBlockState();
		d.level.setBlockAndUpdate(pos, pod);

		if (d.level.getBlockEntity(pos) instanceof TitanPodBlockEntity be) {
			be.setEnergy(d.energy);
		}

		if (!d.owner.isRemoved()) {
			d.owner.sendOverlayMessage(Component.translatable("message.solorust.suit.titan_landed").withStyle(ChatFormatting.GOLD));
		}
	}
}
