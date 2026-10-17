package com.shxdowrate.solorust.world;

import com.shxdowrate.solorust.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Монументы 1 тира у перекрёстков: заправка, супермаркет, склад. В каждом есть переработчик и лут.
 * Строятся блоками прямо в коде; каждый чанк рисует только свою часть.
 */
public final class Monuments {
	private Monuments() {
	}

	/** Пишет блоки в «локальных» координатах монумента: u — вдоль фасада, v — вглубь от фасада, y — над полом. */
	private static final class Writer {
		final WorldGenLevel level;
		final RoadNetwork.MonumentSite site;
		final int minX;
		final int minZ;
		final RandomSource random;

		Writer(WorldGenLevel level, RoadNetwork.MonumentSite site, int minX, int minZ, RandomSource random) {
			this.level = level;
			this.site = site;
			this.minX = minX;
			this.minZ = minZ;
			this.random = random;
		}

		BlockPos pos(int u, int y, int v) {
			int w = site.width();
			int d = site.depth();
			int x;
			int z;

			switch (site.front()) {
				case 0 -> { x = site.x0() + u; z = site.z0() + v; }
				case 1 -> { x = site.x0() + (w - 1 - u); z = site.z0() + (d - 1 - v); }
				case 2 -> { x = site.x0() + v; z = site.z0() + (w - 1 - u); }
				default -> { x = site.x0() + (d - 1 - v); z = site.z0() + u; }
			}

			return new BlockPos(x, site.floorY() + y, z);
		}

		boolean inside(BlockPos pos) {
			return pos.getX() >= minX && pos.getX() < minX + 16 && pos.getZ() >= minZ && pos.getZ() < minZ + 16;
		}

		void set(int u, int y, int v, BlockState state) {
			BlockPos pos = pos(u, y, v);

			if (inside(pos)) {
				level.setBlock(pos, state, Block.UPDATE_CLIENTS);
			}
		}

		void set(int u, int y, int v, Block block) {
			set(u, y, v, block.defaultBlockState());
		}

		void box(int u0, int y0, int v0, int u1, int y1, int v1, Block block) {
			for (int u = u0; u <= u1; u++) {
				for (int y = y0; y <= y1; y++) {
					for (int v = v0; v <= v1; v++) {
						set(u, y, v, block);
					}
				}
			}
		}

		/** Стены по периметру прямоугольника. */
		void walls(int u0, int y0, int v0, int u1, int y1, int v1, Block block) {
			for (int u = u0; u <= u1; u++) {
				for (int y = y0; y <= y1; y++) {
					set(u, y, v0, block);
					set(u, y, v1, block);
				}
			}

			for (int v = v0; v <= v1; v++) {
				for (int y = y0; y <= y1; y++) {
					set(u0, y, v, block);
					set(u1, y, v, block);
				}
			}
		}

		void crate(int u, int y, int v, Block block) {
			BlockPos pos = pos(u, y, v);

			if (inside(pos)) {
				level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_CLIENTS);
				RoadsideProps.fillCrate(level, pos, block == ModBlocks.MILITARY_CRATE ? RoadsideProps.MILITARY_LOOT : RoadsideProps.CRATE_LOOT, random);
			}
		}

		void barrel(int u, int v, boolean oil) {
			set(u, 1, v, oil ? ModBlocks.OIL_BARREL : ModBlocks.LOOT_BARREL);
		}

		/** Ровная площадка: пол, расчистка сверху, фундамент снизу. */
		void platform(Block floor) {
			for (int u = -2; u < site.width() + 2; u++) {
				for (int v = -2; v < site.depth() + 2; v++) {
					BlockPos base = pos(u, 0, v);

					if (!inside(base)) {
						continue;
					}

					for (int y = 1; y <= 12; y++) {
						BlockPos p = base.above(y);

						if (!level.getBlockState(p).isAir()) {
							level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
						}
					}

					boolean edge = u < 0 || v < 0 || u >= site.width() || v >= site.depth();
					level.setBlock(base, (edge ? Blocks.COARSE_DIRT : floor).defaultBlockState(), Block.UPDATE_CLIENTS);

					for (int y = 1; y <= 10; y++) {
						BlockPos p = base.below(y);
						BlockState s = level.getBlockState(p);

						if (!s.isAir() && !s.canBeReplaced() && s.getFluidState().isEmpty()) {
							break;
						}

						level.setBlock(p, (y <= 2 ? Blocks.DIRT : Blocks.STONE).defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
		}
	}

	public static void place(WorldGenLevel level, RoadNetwork.MonumentSite site, int minX, int minZ, RandomSource random) {
		Writer w = new Writer(level, site, minX, minZ, random);

		switch (site.type()) {
			case GAS_STATION -> gasStation(w);
			case SUPERMARKET -> supermarket(w);
			case WAREHOUSE -> warehouse(w);
		}
	}

	// ------------------------------------------------------------------ заправка 20×16

	private static void gasStation(Writer w) {
		w.platform(Blocks.SMOOTH_STONE);

		// Асфальт перед магазином.
		w.box(0, 0, 0, 19, 0, 9, Blocks.POLISHED_BLACKSTONE);

		// Навес над колонками.
		for (int[] p : new int[][]{{3, 3}, {12, 3}, {3, 7}, {12, 7}}) {
			w.box(p[0], 1, p[1], p[0], 4, p[1], Blocks.WHITE_CONCRETE);
		}

		w.box(1, 5, 1, 14, 5, 9, Blocks.WHITE_CONCRETE);
		w.walls(1, 5, 1, 14, 5, 9, Blocks.RED_CONCRETE);
		w.set(7, 4, 5, Blocks.SEA_LANTERN);

		// Колонки.
		for (int u : new int[]{6, 9}) {
			w.set(u, 1, 5, Blocks.RED_CONCRETE);
			w.set(u, 2, 5, Blocks.LIGHT_GRAY_CONCRETE);
			w.set(u, 3, 5, Blocks.RED_TERRACOTTA);
		}

		w.barrel(16, 2, true);
		w.barrel(17, 2, true);
		w.barrel(16, 3, false);

		// Магазинчик.
		w.walls(4, 1, 10, 15, 4, 15, Blocks.WHITE_TERRACOTTA);
		w.box(4, 5, 10, 15, 5, 15, Blocks.LIGHT_GRAY_CONCRETE);
		w.box(5, 2, 10, 8, 3, 10, Blocks.GLASS);
		w.box(11, 2, 10, 14, 3, 10, Blocks.GLASS);
		w.box(9, 1, 10, 10, 3, 10, Blocks.AIR);
		w.box(5, 1, 11, 14, 4, 14, Blocks.AIR);
		w.box(5, 0, 11, 14, 0, 14, Blocks.WHITE_CONCRETE);
		w.set(9, 5, 12, Blocks.SEA_LANTERN);

		// Прилавок, ящики, переработчик.
		w.box(6, 1, 12, 8, 1, 12, Blocks.SPRUCE_PLANKS);
		w.crate(5, 1, 14, ModBlocks.LOOT_CRATE);
		w.crate(14, 1, 11, ModBlocks.LOOT_CRATE);
		w.set(12, 1, 14, ModBlocks.RECYCLER);
		w.barrel(13, 14, false);
		w.barrel(6, 14, false);
	}

	// ------------------------------------------------------------------ супермаркет 22×16

	private static void supermarket(Writer w) {
		w.platform(Blocks.SMOOTH_STONE);

		// Парковка с разметкой.
		w.box(0, 0, 0, 21, 0, 5, Blocks.POLISHED_BLACKSTONE);

		for (int u = 2; u < 21; u += 4) {
			w.box(u, 0, 1, u, 0, 4, Blocks.WHITE_CONCRETE);
		}

		// Здание.
		w.walls(1, 1, 6, 20, 5, 15, Blocks.LIGHT_GRAY_CONCRETE);
		w.box(1, 6, 6, 20, 6, 15, Blocks.GRAY_CONCRETE);
		w.box(2, 2, 6, 8, 4, 6, Blocks.GLASS);
		w.box(13, 2, 6, 19, 4, 6, Blocks.GLASS);
		w.box(10, 1, 6, 11, 3, 6, Blocks.AIR);
		w.box(2, 1, 7, 19, 5, 14, Blocks.AIR);
		w.box(2, 0, 7, 19, 0, 14, Blocks.WHITE_CONCRETE);
		w.box(9, 5, 6, 12, 5, 6, Blocks.RED_CONCRETE);

		for (int u = 4; u <= 17; u += 6) {
			for (int v = 9; v <= 13; v += 4) {
				w.set(u, 6, v, Blocks.SEA_LANTERN);
			}
		}

		// Стеллажи с ящиками.
		for (int v : new int[]{9, 12}) {
			for (int u = 3; u <= 18; u++) {
				if (u == 10 || u == 11) {
					continue;
				}

				w.set(u, 1, v, Blocks.SPRUCE_PLANKS);
				w.set(u, 2, v, Blocks.SPRUCE_SLAB);
			}
		}

		w.crate(4, 1, 10, ModBlocks.LOOT_CRATE);
		w.crate(16, 1, 10, ModBlocks.LOOT_CRATE);
		w.crate(7, 1, 13, ModBlocks.LOOT_CRATE);
		w.set(18, 1, 14, ModBlocks.RECYCLER);
		w.barrel(2, 14, false);
		w.barrel(3, 14, false);
		w.barrel(19, 7, true);

		// Брошенная машина на парковке.
		w.box(14, 1, 2, 18, 1, 4, Blocks.WAXED_WEATHERED_CUT_COPPER);
		w.box(15, 2, 2, 16, 2, 4, Blocks.WAXED_WEATHERED_CUT_COPPER);
		w.box(15, 2, 3, 16, 2, 3, Blocks.AIR);
		w.box(15, 3, 2, 16, 3, 4, Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB);
		w.box(14, 2, 2, 14, 2, 4, Blocks.BLACK_STAINED_GLASS);
	}

	// ------------------------------------------------------------------ склад 22×18

	private static void warehouse(Writer w) {
		w.platform(Blocks.GRAVEL);
		w.box(0, 0, 0, 21, 0, 3, Blocks.GRAVEL);

		// Профнастил: полосы светлого и тёмного бетона.
		for (int u = 1; u <= 20; u++) {
			Block wall = u % 2 == 0 ? Blocks.LIGHT_GRAY_CONCRETE : Blocks.GRAY_CONCRETE;
			w.box(u, 1, 4, u, 7, 4, wall);
			w.box(u, 1, 17, u, 7, 17, wall);
		}

		for (int v = 4; v <= 17; v++) {
			Block wall = v % 2 == 0 ? Blocks.LIGHT_GRAY_CONCRETE : Blocks.GRAY_CONCRETE;
			w.box(1, 1, v, 1, 7, v, wall);
			w.box(20, 1, v, 20, 7, v, wall);
		}

		w.box(1, 8, 4, 20, 8, 17, Blocks.GRAY_CONCRETE);

		for (int u = 4; u <= 17; u += 4) {
			w.box(u, 8, 7, u + 1, 8, 14, Blocks.GLASS);
		}

		w.box(8, 1, 4, 13, 5, 4, Blocks.AIR);
		w.box(2, 1, 5, 19, 7, 16, Blocks.AIR);
		w.box(2, 0, 5, 19, 0, 16, Blocks.SMOOTH_STONE);
		w.box(7, 6, 4, 14, 6, 4, Blocks.YELLOW_CONCRETE);

		// Морской контейнер внутри.
		w.box(3, 1, 7, 5, 3, 13, Blocks.BLUE_CONCRETE);
		w.box(4, 1, 8, 4, 2, 13, Blocks.AIR);
		w.crate(4, 1, 12, ModBlocks.LOOT_CRATE);

		// Стопки ящиков.
		w.crate(16, 1, 15, ModBlocks.MILITARY_CRATE);
		w.crate(17, 1, 15, ModBlocks.LOOT_CRATE);
		w.crate(18, 1, 14, ModBlocks.LOOT_CRATE);
		w.box(17, 2, 15, 18, 2, 15, Blocks.SPRUCE_SLAB);
		w.crate(10, 1, 15, ModBlocks.LOOT_CRATE);
		w.set(15, 1, 6, ModBlocks.RECYCLER);
		w.barrel(18, 6, false);
		w.barrel(18, 7, true);
		w.barrel(8, 15, false);
		w.barrel(9, 15, false);

		// Контейнер снаружи.
		w.box(15, 1, 0, 20, 3, 2, Blocks.RED_CONCRETE);
	}
}
