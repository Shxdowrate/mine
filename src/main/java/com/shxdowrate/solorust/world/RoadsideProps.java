package com.shxdowrate.solorust.world;

import com.shxdowrate.solorust.SoloRust;
import com.shxdowrate.solorust.block.entity.LootCrateBlockEntity;
import com.shxdowrate.solorust.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Всё, что стоит у дорог: бочки, ящики, ржавые машины. Места детерминированы от сида,
 * поэтому каждый чанк рисует свою часть объекта, даже если тот лежит на границе чанков.
 */
public final class RoadsideProps {
	private static final int SPACING = 18;

	public static final ResourceKey<LootTable> CRATE_LOOT = ResourceKey.create(Registries.LOOT_TABLE, SoloRust.id("chests/loot_crate"));
	public static final ResourceKey<LootTable> MILITARY_LOOT = ResourceKey.create(Registries.LOOT_TABLE, SoloRust.id("chests/military_crate"));
	public static final ResourceKey<LootTable> SUPPLY_LOOT = ResourceKey.create(Registries.LOOT_TABLE, SoloRust.id("chests/supply_drop"));

	private enum Kind { BARRELS, CAR, CRATE, MILITARY }

	private RoadsideProps() {
	}

	public static void place(WorldGenLevel level, RoadNetwork.Road road, int minX, int minZ, RustWorld.HeightSampler sampler, RandomSource random) {
		int spots = (int) (road.length() / SPACING);

		for (int k = 1; k < spots; k++) {
			double s = k * (double) SPACING;

			if (s < 14 || s > road.length() - 14) {
				continue;
			}

			long h = RustWorld.mix(road.id(), k, 0x51DEL);
			int roll = (int) Math.floorMod(h, 100);
			Kind kind;

			if (roll < 38) {
				kind = Kind.BARRELS;
			} else if (roll < 48) {
				kind = Kind.CAR;
			} else if (roll < 55) {
				kind = Kind.CRATE;
			} else if (roll < 58) {
				kind = Kind.MILITARY;
			} else {
				continue;
			}

			int side = ((h >>> 8) & 1) == 0 ? -1 : 1;
			double[] p = road.pointAt(s);
			double[] t = road.directionAt(s);
			double offset = RoadNetwork.HALF_WIDTH + (kind == Kind.CAR ? 3.0 : 2.0);
			int cx = (int) Math.floor(p[0] - t[1] * side * offset);
			int cz = (int) Math.floor(p[1] + t[0] * side * offset);

			if (cx < minX - 4 || cx > minX + 19 || cz < minZ - 4 || cz > minZ + 19) {
				continue;
			}

			// Не ставим на мостах и в воде.
			if (sampler.ground(cx, cz) <= sampler.seaLevel()) {
				continue;
			}

			int y = Math.round(road.heightAt(s, sampler));
			Ctx ctx = new Ctx(level, minX, minZ, h);

			switch (kind) {
				case BARRELS -> barrels(ctx, cx, y, cz, h);
				case CAR -> car(ctx, cx, y, cz, Math.abs(t[0]) >= Math.abs(t[1]), ((h >>> 12) & 1) == 0 ? 1 : -1, random);
				case CRATE -> crate(ctx, cx, y, cz, ModBlocks.LOOT_CRATE.defaultBlockState(), CRATE_LOOT, random);
				case MILITARY -> crate(ctx, cx, y, cz, ModBlocks.MILITARY_CRATE.defaultBlockState(), MILITARY_LOOT, random);
			}
		}
	}

	record Ctx(WorldGenLevel level, int minX, int minZ, long hash) {
		boolean inside(int x, int z) {
			return x >= minX && x < minX + 16 && z >= minZ && z < minZ + 16;
		}

		void set(int x, int y, int z, BlockState state) {
			if (inside(x, z)) {
				level.setBlock(new BlockPos(x, y, z), state, Block.UPDATE_CLIENTS);
			}
		}

		/** Ставит блок на землю: подсыпает землю снизу, если там яма. */
		void ground(int x, int y, int z, BlockState state) {
			if (!inside(x, z)) {
				return;
			}

			for (int dy = 1; dy <= 4; dy++) {
				BlockPos below = new BlockPos(x, y - dy, z);
				BlockState current = level.getBlockState(below);

				if (!current.isAir() && !current.canBeReplaced()) {
					break;
				}

				level.setBlock(below, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
			}

			for (int dy = 1; dy <= 3; dy++) {
				set(x, y + dy, z, Blocks.AIR.defaultBlockState());
			}

			set(x, y, z, state);
		}
	}

	static void barrels(Ctx ctx, int x, int y, int z, long h) {
		int count = 1 + (int) Math.floorMod(h >>> 16, 3);
		int[][] offsets = {{0, 0}, {1, 0}, {0, 1}};

		for (int i = 0; i < count; i++) {
			boolean oil = Math.floorMod(h >>> (20 + i * 3), 4) == 0;
			BlockState state = (oil ? ModBlocks.OIL_BARREL : ModBlocks.LOOT_BARREL).defaultBlockState();
			ctx.ground(x + offsets[i][0], y, z + offsets[i][1], state);
		}
	}

	static void crate(Ctx ctx, int x, int y, int z, BlockState state, ResourceKey<LootTable> loot, RandomSource random) {
		if (!ctx.inside(x, z)) {
			return;
		}

		ctx.ground(x, y, z, state);
		fillCrate(ctx.level(), new BlockPos(x, y, z), loot, random);
	}

	/** Назначает ящику таблицу лута с респавном. */
	public static void fillCrate(WorldGenLevel level, BlockPos pos, ResourceKey<LootTable> loot, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof LootCrateBlockEntity crate) {
			crate.setRespawningLoot(loot, random.nextLong());
		}
	}

	/** Ржавая легковушка 5×3: колёса, кузов, кабина со стеклом и крыша. */
	static void car(Ctx ctx, int cx, int y, int cz, boolean alongX, int front, RandomSource random) {
		BlockState wheel = Blocks.COAL_BLOCK.defaultBlockState();
		BlockState body = Blocks.WAXED_WEATHERED_CUT_COPPER.defaultBlockState();
		BlockState rust = Blocks.WAXED_OXIDIZED_CUT_COPPER.defaultBlockState();
		BlockState roof = Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB.defaultBlockState();
		BlockState glass = Blocks.BLACK_STAINED_GLASS.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();

		for (int a = -2; a <= 2; a++) {
			for (int b = -1; b <= 1; b++) {
				int x = alongX ? cx + a * front : cx + b;
				int z = alongX ? cz + b : cz + a * front;

				if (!ctx.inside(x, z)) {
					continue;
				}

				// Опора под машиной и расчистка над ней.
				ctx.ground(x, y, z, Math.abs(a) == 1 && b != 0 ? wheel : air);

				long cell = RustWorld.mix(ctx.hash(), a, b);
				ctx.set(x, y + 1, z, Math.floorMod(cell, 4) == 0 ? rust : body);

				if (a == -1) {
					ctx.set(x, y + 2, z, glass);
				} else if (a >= 0 && a <= 1) {
					ctx.set(x, y + 2, z, b == 0 ? air : body);
					ctx.set(x, y + 3, z, roof);
				}
			}
		}

		// Иногда за багажником валяется ящик.
		if (Math.floorMod(ctx.hash() >>> 24, 2) == 0) {
			int x = alongX ? cx + 3 * front : cx;
			int z = alongX ? cz : cz + 3 * front;
			crate(ctx, x, y, z, ModBlocks.LOOT_CRATE.defaultBlockState(), CRATE_LOOT, random);
		}
	}
}
