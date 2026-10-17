package com.shxdowrate.solorust.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;

/**
 * Дорожная сеть мира «Rust». Полностью детерминирована от сида: мир разбит на клетки,
 * в каждой клетке есть «перекрёсток», соседние перекрёстки соединены дорогой с изгибом посередине.
 * Высота дороги сглажена по рельефу, чтобы она не прыгала по каждому бугру.
 */
public final class RoadNetwork {
	public static final int CELL = 384;
	private static final int MARGIN = 72;
	private static final int SAMPLE = 8;
	private static final int SMOOTH = 3;
	private static final int MAX_BEND = 48;
	private static final int MAX_NODE_HEIGHT = 140;

	public static final float HALF_WIDTH = 2.6f;
	public static final float CLEAR_HALF = 3.6f;

	private static final Map<Long, Optional<Node>> NODES = new ConcurrentHashMap<>();
	private static final Map<Long, Optional<Road>> ROADS = new ConcurrentHashMap<>();

	private RoadNetwork() {
	}

	public record Node(int x, int z, float height) {
	}

	/** Дорога из трёх точек: A → изгиб M → B. */
	public static final class Road {
		final double ax, az, mx, mz, bx, bz;
		final double len1, length;
		final long id;
		private volatile float[] profile;
		private final Node a, b;

		Road(long id, Node a, Node b, double mx, double mz) {
			this.id = id;
			this.a = a;
			this.b = b;
			this.ax = a.x() + 0.5;
			this.az = a.z() + 0.5;
			this.bx = b.x() + 0.5;
			this.bz = b.z() + 0.5;
			this.mx = mx;
			this.mz = mz;
			this.len1 = Math.hypot(mx - ax, mz - az);
			this.length = len1 + Math.hypot(bx - mx, bz - mz);
		}

		public long id() {
			return id;
		}

		public double length() {
			return length;
		}

		boolean intersects(int minX, int minZ, int maxX, int maxZ, float pad) {
			double lx = Math.min(ax, Math.min(mx, bx)) - pad;
			double hx = Math.max(ax, Math.max(mx, bx)) + pad;
			double lz = Math.min(az, Math.min(mz, bz)) - pad;
			double hz = Math.max(az, Math.max(mz, bz)) + pad;
			return hx >= minX && lx <= maxX + 1 && hz >= minZ && lz <= maxZ + 1;
		}

		/** Точка на дороге по длине s от начала. */
		public double[] pointAt(double s) {
			if (s <= len1) {
				double t = len1 == 0 ? 0 : s / len1;
				return new double[]{ax + (mx - ax) * t, az + (mz - az) * t};
			}

			double l2 = length - len1;
			double t = l2 == 0 ? 0 : (s - len1) / l2;
			return new double[]{mx + (bx - mx) * t, mz + (bz - mz) * t};
		}

		/** Единичное направление дороги в точке s. */
		public double[] directionAt(double s) {
			double dx;
			double dz;

			if (s <= len1) {
				dx = mx - ax;
				dz = mz - az;
			} else {
				dx = bx - mx;
				dz = bz - mz;
			}

			double len = Math.hypot(dx, dz);
			return len == 0 ? new double[]{1, 0} : new double[]{dx / len, dz / len};
		}

		/** Возвращает {расстояние до оси дороги, длина s вдоль дороги} для точки (x, z). */
		public double[] project(double x, double z) {
			double[] first = projectSegment(x, z, ax, az, mx, mz);
			double[] second = projectSegment(x, z, mx, mz, bx, bz);

			if (first[0] <= second[0]) {
				return new double[]{first[0], first[1] * len1};
			}

			return new double[]{second[0], len1 + second[1] * (length - len1)};
		}

		private static double[] projectSegment(double px, double pz, double x1, double z1, double x2, double z2) {
			double dx = x2 - x1;
			double dz = z2 - z1;
			double lenSq = dx * dx + dz * dz;
			double t = lenSq == 0 ? 0 : ((px - x1) * dx + (pz - z1) * dz) / lenSq;
			t = Math.max(0, Math.min(1, t));
			double cx = x1 + dx * t;
			double cz = z1 + dz * t;
			return new double[]{Math.hypot(px - cx, pz - cz), t};
		}

		/** Сглаженная высота (первая свободная Y над дорожным полотном) в точке s. */
		public float heightAt(double s, RustWorld.HeightSampler sampler) {
			float[] p = profile;

			if (p == null) {
				p = buildProfile(sampler);
				profile = p;
			}

			double f = Math.max(0, Math.min(s, length)) / SAMPLE;
			int i = (int) Math.floor(f);

			if (i >= p.length - 1) {
				return p[p.length - 1];
			}

			double frac = f - i;
			return (float) (p[i] + (p[i + 1] - p[i]) * frac);
		}

		private float[] buildProfile(RustWorld.HeightSampler sampler) {
			int n = (int) Math.ceil(length / SAMPLE) + 1;
			float[] raw = new float[n];
			int sea = sampler.seaLevel();

			for (int i = 0; i < n; i++) {
				double[] pt = pointAt(Math.min(i * (double) SAMPLE, length));
				raw[i] = Math.max(sampler.ground((int) Math.floor(pt[0]), (int) Math.floor(pt[1])), sea + 1);
			}

			float[] smooth = new float[n];

			for (int i = 0; i < n; i++) {
				float sum = 0;
				int count = 0;

				for (int k = i - SMOOTH; k <= i + SMOOTH; k++) {
					if (k >= 0 && k < n) {
						sum += raw[k];
						count++;
					}
				}

				smooth[i] = sum / count;
			}

			// Подгоняем концы к высотам перекрёстков, чтобы соседние дороги стыковались без ступенек.
			float startFix = a.height() - smooth[0];
			float endFix = b.height() - smooth[n - 1];

			for (int i = 0; i < n; i++) {
				float t = n == 1 ? 0 : i / (float) (n - 1);
				smooth[i] += startFix * (1 - t) + endFix * t;
			}

			return smooth;
		}
	}

	private static Optional<Node> node(int cx, int cz, RustWorld.HeightSampler sampler) {
		long key = RustWorld.mix(sampler.seed(), cx, cz);

		if (NODES.size() > 50_000) {
			NODES.clear();
		}

		return NODES.computeIfAbsent(key, k -> {
			SplittableRandom random = new SplittableRandom(k);
			int x = cx * CELL + MARGIN + random.nextInt(CELL - 2 * MARGIN);
			int z = cz * CELL + MARGIN + random.nextInt(CELL - 2 * MARGIN);
			int sea = sampler.seaLevel();

			int[][] offsets = {{0, 0}, {6, 0}, {-6, 0}, {0, 6}, {0, -6}};
			float sum = 0;

			for (int[] o : offsets) {
				int h = sampler.ground(x + o[0], z + o[1]);

				// Перекрёсток в воде или на пике горы — не ставим.
				if (h <= sea || h > MAX_NODE_HEIGHT) {
					return Optional.empty();
				}

				sum += h;
			}

			return Optional.of(new Node(x, z, sum / offsets.length));
		});
	}

	/** dir 0 — дорога на восток (к клетке cx+1), dir 1 — на юг (к клетке cz+1). */
	private static Optional<Road> road(int cx, int cz, int dir, RustWorld.HeightSampler sampler) {
		long key = RustWorld.mix(sampler.seed() ^ 0x5DEECE66DL, cx * 2L + dir, cz);

		if (ROADS.size() > 20_000) {
			ROADS.clear();
		}

		Optional<Road> cached = ROADS.get(key);

		if (cached != null) {
			return cached;
		}

		Optional<Node> a = node(cx, cz, sampler);
		Optional<Node> b = dir == 0 ? node(cx + 1, cz, sampler) : node(cx, cz + 1, sampler);
		Optional<Road> result = Optional.empty();

		if (a.isPresent() && b.isPresent()) {
			SplittableRandom random = new SplittableRandom(key);
			Node na = a.get();
			Node nb = b.get();
			double dx = nb.x() - na.x();
			double dz = nb.z() - na.z();
			double len = Math.hypot(dx, dz);
			double bend = (random.nextDouble() * 2 - 1) * MAX_BEND;
			double mx = (na.x() + nb.x()) / 2.0 + 0.5 + (-dz / len) * bend;
			double mz = (na.z() + nb.z()) / 2.0 + 0.5 + (dx / len) * bend;
			result = Optional.of(new Road(key, na, nb, mx, mz));
		}

		ROADS.put(key, result);
		return result;
	}

	/** Все дороги, проходящие рядом с прямоугольником (включительно). */
	public static List<Road> roadsNear(int minX, int minZ, int maxX, int maxZ, RustWorld.HeightSampler sampler) {
		List<Road> list = new ArrayList<>();
		int cx0 = Math.floorDiv(minX, CELL) - 1;
		int cx1 = Math.floorDiv(maxX, CELL) + 1;
		int cz0 = Math.floorDiv(minZ, CELL) - 1;
		int cz1 = Math.floorDiv(maxZ, CELL) + 1;

		for (int cx = cx0; cx <= cx1; cx++) {
			for (int cz = cz0; cz <= cz1; cz++) {
				for (int dir = 0; dir < 2; dir++) {
					road(cx, cz, dir, sampler).ifPresent(r -> {
						if (r.intersects(minX, minZ, maxX, maxZ, CLEAR_HALF + 1)) {
							list.add(r);
						}
					});
				}
			}
		}

		return list;
	}

	// ------------------------------------------------------------------ монументы на перекрёстках

	public enum MonumentType { GAS_STATION, SUPERMARKET, WAREHOUSE }

	/**
	 * Монумент у перекрёстка. (x0, z0) — угол площадки, width — вдоль фасада, depth — вглубь;
	 * front — сторона света, куда смотрит фасад (0=север -Z, 1=юг +Z, 2=запад -X, 3=восток +X).
	 */
	public record MonumentSite(MonumentType type, int x0, int z0, int width, int depth, int front, int floorY, long seed) {
		public int sizeX() {
			return front <= 1 ? width : depth;
		}

		public int sizeZ() {
			return front <= 1 ? depth : width;
		}
	}

	private static final Map<Long, Optional<MonumentSite>> MONUMENTS = new ConcurrentHashMap<>();

	private static Optional<MonumentSite> monument(int cx, int cz, RustWorld.HeightSampler sampler) {
		long key = RustWorld.mix(sampler.seed() ^ 0x4D4F4EL, cx, cz);

		if (MONUMENTS.size() > 20_000) {
			MONUMENTS.clear();
		}

		Optional<MonumentSite> cached = MONUMENTS.get(key);

		if (cached != null) {
			return cached;
		}

		Optional<MonumentSite> result = Optional.empty();
		Optional<Node> nodeOpt = node(cx, cz, sampler);
		int roll = (int) Math.floorMod(key, 100);
		MonumentType type = roll < 30 ? MonumentType.GAS_STATION : roll < 52 ? MonumentType.SUPERMARKET : roll < 72 ? MonumentType.WAREHOUSE : null;

		if (nodeOpt.isPresent() && type != null) {
			Node node = nodeOpt.get();
			List<Double> angles = new ArrayList<>();
			road(cx, cz, 0, sampler).ifPresent(r -> angles.add(Math.atan2(r.mz - r.az, r.mx - r.ax)));
			road(cx, cz, 1, sampler).ifPresent(r -> angles.add(Math.atan2(r.mz - r.az, r.mx - r.ax)));
			road(cx - 1, cz, 0, sampler).ifPresent(r -> angles.add(Math.atan2(r.mz - r.bz, r.mx - r.bx)));
			road(cx, cz - 1, 1, sampler).ifPresent(r -> angles.add(Math.atan2(r.mz - r.bz, r.mx - r.bx)));

			if (!angles.isEmpty()) {
				// Ставим монумент в ту «четверть» между дорогами, где до ближайшей дороги дальше всего.
				double bestAngle = 0;
				double bestGap = -1;

				for (int q = 0; q < 4; q++) {
					double a = Math.PI / 4 + q * Math.PI / 2;
					double gap = Double.MAX_VALUE;

					for (double r : angles) {
						double d = Math.abs(Math.atan2(Math.sin(a - r), Math.cos(a - r)));
						gap = Math.min(gap, d);
					}

					if (gap > bestGap) {
						bestGap = gap;
						bestAngle = a;
					}
				}

				int width = switch (type) {
					case GAS_STATION -> 20;
					case SUPERMARKET -> 22;
					case WAREHOUSE -> 22;
				};
				int depth = switch (type) {
					case GAS_STATION -> 16;
					case SUPERMARKET -> 16;
					case WAREHOUSE -> 18;
				};

				double dist = 30;
				int centerX = (int) Math.round(node.x() + Math.cos(bestAngle) * dist);
				int centerZ = (int) Math.round(node.z() + Math.sin(bestAngle) * dist);

				// Фасад смотрит в сторону перекрёстка (по преобладающей оси).
				double toNodeX = node.x() - centerX;
				double toNodeZ = node.z() - centerZ;
				int front = Math.abs(toNodeX) > Math.abs(toNodeZ) ? (toNodeX < 0 ? 2 : 3) : (toNodeZ < 0 ? 0 : 1);
				int sizeX = front <= 1 ? width : depth;
				int sizeZ = front <= 1 ? depth : width;
				int floorY = Math.round(node.height()) - 1;

				result = Optional.of(new MonumentSite(type, centerX - sizeX / 2, centerZ - sizeZ / 2, width, depth, front, floorY, key));
			}
		}

		MONUMENTS.put(key, result);
		return result;
	}

	/** Монументы, площадка которых (с запасом) задевает прямоугольник. */
	public static List<MonumentSite> monumentsNear(int minX, int minZ, int maxX, int maxZ, RustWorld.HeightSampler sampler) {
		List<MonumentSite> list = new ArrayList<>();
		int cx0 = Math.floorDiv(minX, CELL) - 1;
		int cx1 = Math.floorDiv(maxX, CELL) + 1;
		int cz0 = Math.floorDiv(minZ, CELL) - 1;
		int cz1 = Math.floorDiv(maxZ, CELL) + 1;

		for (int cx = cx0; cx <= cx1; cx++) {
			for (int cz = cz0; cz <= cz1; cz++) {
				monument(cx, cz, sampler).ifPresent(m -> {
					int pad = 4;

					if (m.x0() - pad <= maxX && m.x0() + m.sizeX() + pad >= minX && m.z0() - pad <= maxZ && m.z0() + m.sizeZ() + pad >= minZ) {
						list.add(m);
					}
				});
			}
		}

		return list;
	}

	/** Точка спавна прямо на дороге, ближайшей к центру мира. */
	public static BlockPos findSpawn(RustWorld.HeightSampler sampler) {
		Road best = null;
		double bestDist = Double.MAX_VALUE;

		for (int radius = 1; radius <= 4 && best == null; radius++) {
			for (int cx = -radius; cx <= radius; cx++) {
				for (int cz = -radius; cz <= radius; cz++) {
					for (int dir = 0; dir < 2; dir++) {
						Optional<Road> road = road(cx, cz, dir, sampler);

						if (road.isPresent()) {
							Road r = road.get();
							double d = Math.hypot(r.ax, r.az);

							if (d < bestDist) {
								bestDist = d;
								best = r;
							}
						}
					}
				}
			}
		}

		if (best == null) {
			return null;
		}

		double s = Math.min(24, best.length() / 2);
		double[] pt = best.pointAt(s);
		int y = Math.round(best.heightAt(s, sampler));
		return new BlockPos((int) Math.floor(pt[0]), y, (int) Math.floor(pt[1]));
	}
}
