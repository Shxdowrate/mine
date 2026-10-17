package com.shxdowrate.solorust.mechanics;

import java.util.ArrayList;
import java.util.List;

import com.shxdowrate.solorust.registry.ModBlocks;
import com.shxdowrate.solorust.world.RoadsideProps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/** Аирдропы: дым над точкой вызова, через 30 секунд — ящик снабжения с хорошим лутом. */
public final class Airdrops {
	private static final int DELAY = 600;

	private static final class Pending {
		final ServerLevel level;
		final BlockPos pos;
		int ticks;

		Pending(ServerLevel level, BlockPos pos) {
			this.level = level;
			this.pos = pos;
			this.ticks = DELAY;
		}
	}

	private static final List<Pending> PENDING = new ArrayList<>();

	private Airdrops() {
	}

	public static void call(ServerLevel level, BlockPos pos) {
		// Ящик падает не прямо на голову, а в нескольких блоках.
		int dx = level.getRandom().nextInt(13) - 6;
		int dz = level.getRandom().nextInt(13) - 6;
		PENDING.add(new Pending(level, pos.offset(dx, 0, dz)));
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (PENDING.isEmpty()) {
				return;
			}

			List<Pending> done = new ArrayList<>();

			for (Pending p : PENDING) {
				p.ticks--;

				if (p.ticks % 5 == 0) {
					BlockPos top = surface(p);
					p.level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, top.getX() + 0.5, top.getY() + 0.5, top.getZ() + 0.5,
							2, 0.2, 0.1, 0.2, 0.02);
					p.level.sendParticles(ParticleTypes.FLAME, top.getX() + 0.5, top.getY() + 0.2, top.getZ() + 0.5, 1, 0.1, 0, 0.1, 0.01);
				}

				if (p.ticks == 200) {
					p.level.playSound(null, p.pos.above(40), SoundEvents.ELYTRA_FLYING, SoundSource.AMBIENT, 6.0f, 0.5f);
				}

				if (p.ticks <= 0) {
					drop(p);
					done.add(p);
				}
			}

			PENDING.removeAll(done);
		});
	}

	private static BlockPos surface(Pending p) {
		int y = p.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.pos.getX(), p.pos.getZ());
		return new BlockPos(p.pos.getX(), y, p.pos.getZ());
	}

	private static void drop(Pending p) {
		BlockPos pos = surface(p);
		p.level.setBlock(pos, ModBlocks.SUPPLY_DROP.defaultBlockState(), Block.UPDATE_ALL);
		RandomizableContainer.setBlockEntityLootTable(p.level, p.level.getRandom(), pos, RoadsideProps.SUPPLY_LOOT);
		p.level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.5f, 0.6f);
		p.level.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.5, 0.3, 0.5, 0.0);

		for (var player : p.level.players()) {
			if (player.blockPosition().closerThan(pos, 128)) {
				player.sendSystemMessage(Component.translatable("message.solorust.airdrop_landed", pos.getX(), pos.getY(), pos.getZ()), true);
			}
		}
	}
}
