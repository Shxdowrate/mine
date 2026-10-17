package com.shxdowrate.solorust.client.helmet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.shxdowrate.solorust.client.helmet.HelmetSettings.Option;
import com.shxdowrate.solorust.registry.ModBlocks;
import com.shxdowrate.solorust.suit.SuitArmorItem;
import com.shxdowrate.solorust.suit.SuitTier;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Интерфейс шлема силовой брони: компас, характеристики, данные о цели, обводка мобов сквозь стены
 * и метки на рудах, сундуках и спавнерах. Включается клавишей H, настройки — клавиша J.
 * Работает, когда надет шлем любого поколения; дальность зависит от поколения.
 */
public final class HelmetHud {
	public enum Category {
		ORE, CONTAINER, SPAWNER
	}

	/** Метка на блоке. */
	public record Marker(BlockPos pos, int color, Category category, Block block) {
	}

	private record Ore(TagKey<Block> tag, int color) {
	}

	private static final List<Ore> ORES = List.of(
			new Ore(BlockTags.DIAMOND_ORES, 0x5CF2FF),
			new Ore(BlockTags.EMERALD_ORES, 0x3DFF6E),
			new Ore(BlockTags.GOLD_ORES, 0xFFD23D),
			new Ore(BlockTags.IRON_ORES, 0xE8B894),
			new Ore(BlockTags.REDSTONE_ORES, 0xFF3B3B),
			new Ore(BlockTags.LAPIS_ORES, 0x3D6BFF),
			new Ore(BlockTags.COPPER_ORES, 0xE8834A),
			new Ore(BlockTags.COAL_ORES, 0x9A9A9A));

	public static final int HOSTILE = 0xFF4444;
	public static final int PASSIVE = 0x66FF77;
	public static final int PLAYER = 0x55DDFF;
	public static final int ITEM = 0xFFDD55;
	public static final int TARGET = 0xFFFFFF;

	private static final int MAX_MARKERS = 96;

	private static List<Marker> markers = List.of();
	private static int scanCooldown = 0;
	private static int bootTicks = 0;
	private static boolean wasActive = false;

	private static Entity targetEntity;
	private static BlockPos targetBlock;
	private static double targetDistance;

	private static Vec3 lastPos;
	private static double speed;

	private HelmetHud() {
	}

	// ================================================================== состояние

	/** Поколение надетого шлема (одного шлема достаточно для интерфейса). */
	public static SuitTier helmet(Player player) {
		ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
		return head.getItem() instanceof SuitArmorItem armor ? armor.tier() : null;
	}

	public static boolean active() {
		LocalPlayer player = Minecraft.getInstance().player;
		return player != null && player.isAlive() && HelmetSettings.enabled() && helmet(player) != null;
	}

	/** Дальность подсветки мобов. */
	public static double entityRange(SuitTier tier) {
		return switch (tier) {
			case MK1, LIGHT, BUILDER -> 16;
			case MK2, MINER, DIVER -> 32;
			case MK3, ARCHMAGE, SCOUT -> 48;
			case TITAN -> 40;
			case NANO, APEX -> 64;
			case SINGULARITY -> 96;
		};
	}

	/** Радиус сканирования блоков. */
	public static int blockRange(SuitTier tier) {
		return switch (tier) {
			case MK1, LIGHT, SCOUT -> 8;
			case MK2, BUILDER, APEX -> 12;
			case MK3, ARCHMAGE, DIVER -> 18;
			case TITAN -> 16;
			case NANO, SINGULARITY -> 24;
			case MINER -> 32;
		};
	}

	// ================================================================== тик

	public static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;

		if (player == null || mc.level == null) {
			wasActive = false;
			markers = List.of();
			return;
		}

		boolean active = active();

		if (active && !wasActive) {
			bootTicks = 30;
			scanCooldown = 0;
			player.playSound(SoundEvents.BEACON_POWER_SELECT, 0.5f, 1.8f);
		}

		wasActive = active;

		if (bootTicks > 0) {
			bootTicks--;
		}

		Vec3 pos = player.position();
		speed = lastPos == null ? 0 : speed * 0.6 + pos.distanceTo(lastPos) * 20 * 0.4;
		lastPos = pos;

		if (!active) {
			markers = List.of();
			targetEntity = null;
			targetBlock = null;
			return;
		}

		SuitTier tier = helmet(player);
		updateTarget(mc, player, tier);

		if (--scanCooldown <= 0) {
			scanCooldown = 20;
			markers = scan(mc.level, player, blockRange(tier));
		}
	}

	private static void updateTarget(Minecraft mc, LocalPlayer player, SuitTier tier) {
		double range = entityRange(tier);
		HitResult blockHit = player.pick(range, 1f, false);
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getViewVector(1f).scale(range));
		double best = blockHit.getType() == HitResult.Type.MISS ? range * range : eye.distanceToSqr(blockHit.getLocation());
		Entity found = null;

		for (Entity e : mc.level.getEntities(player, new AABB(eye, end).inflate(1.0), e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator())) {
			var hit = e.getBoundingBox().inflate(0.2).clip(eye, end);

			if (hit.isPresent()) {
				double d = eye.distanceToSqr(hit.get());

				if (d < best) {
					best = d;
					found = e;
				}
			}
		}

		targetEntity = found;
		targetBlock = found == null && blockHit instanceof BlockHitResult bh && blockHit.getType() == HitResult.Type.BLOCK ? bh.getBlockPos() : null;
		targetDistance = Math.sqrt(best);
	}

	private static List<Marker> scan(ClientLevel level, LocalPlayer player, int r) {
		boolean ores = Option.ORES.on();
		boolean containers = Option.CONTAINERS.on();
		boolean spawners = Option.SPAWNERS.on();

		if (!ores && !containers && !spawners) {
			return List.of();
		}

		List<Marker> found = new ArrayList<>();
		BlockPos center = player.blockPosition();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int minY = Math.max(level.getMinY(), center.getY() - r);
		int maxY = Math.min(level.getMaxY(), center.getY() + r);

		for (int x = center.getX() - r; x <= center.getX() + r; x++) {
			for (int z = center.getZ() - r; z <= center.getZ() + r; z++) {
				for (int y = minY; y <= maxY; y++) {
					pos.set(x, y, z);
					BlockState state = level.getBlockState(pos);

					if (state.isAir()) {
						continue;
					}

					Marker marker = classify(state, pos, ores, containers, spawners);

					if (marker != null) {
						found.add(marker);
					}
				}
			}
		}

		Vec3 eye = player.getEyePosition();
		found.sort(Comparator.comparingDouble(m -> m.pos().distToCenterSqr(eye)));
		return found.size() > MAX_MARKERS ? new ArrayList<>(found.subList(0, MAX_MARKERS)) : found;
	}

	private static Marker classify(BlockState state, BlockPos pos, boolean ores, boolean containers, boolean spawners) {
		Block block = state.getBlock();

		if (ores) {
			for (Ore ore : ORES) {
				if (state.is(ore.tag())) {
					return new Marker(pos.immutable(), ore.color(), Category.ORE, block);
				}
			}

			if (block == Blocks.ANCIENT_DEBRIS) {
				return new Marker(pos.immutable(), 0xB06A4A, Category.ORE, block);
			}

			if (block == Blocks.NETHER_QUARTZ_ORE) {
				return new Marker(pos.immutable(), 0xF0E8DC, Category.ORE, block);
			}

			if (block == ModBlocks.SULFUR_NODE) {
				return new Marker(pos.immutable(), 0xF2E24A, Category.ORE, block);
			}

			if (block == ModBlocks.METAL_NODE) {
				return new Marker(pos.immutable(), 0xC9A27E, Category.ORE, block);
			}
		}

		if (containers && (block instanceof AbstractChestBlock<?> || block instanceof BarrelBlock || block instanceof ShulkerBoxBlock
				|| block == ModBlocks.LOOT_BARREL || block == ModBlocks.OIL_BARREL || block == ModBlocks.LOOT_CRATE
				|| block == ModBlocks.MILITARY_CRATE || block == ModBlocks.SUPPLY_DROP)) {
			return new Marker(pos.immutable(), 0xFFB13D, Category.CONTAINER, block);
		}

		if (spawners && (block == Blocks.SPAWNER || block == Blocks.TRIAL_SPAWNER || block == Blocks.VAULT)) {
			return new Marker(pos.immutable(), 0xFF3DA5, Category.SPAWNER, block);
		}

		return null;
	}

	// ================================================================== обводка мобов (вызывается миксином)

	/** Цвет обводки для сущности или 0, если шлем её не подсвечивает. */
	public static int outlineColor(Entity entity) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;

		if (player == null || entity == player || !active()) {
			return 0;
		}

		if (entity == targetEntity && Option.TARGET.on()) {
			return TARGET;
		}

		SuitTier tier = helmet(player);

		if (tier == null || entity.distanceToSqr(player) > entityRange(tier) * entityRange(tier)) {
			return 0;
		}

		if (entity instanceof ItemEntity) {
			return Option.ITEMS.on() ? ITEM : 0;
		}

		if (!(entity instanceof LivingEntity) || entity instanceof ArmorStand || !entity.isAlive()) {
			return 0;
		}

		if (entity instanceof Player) {
			return Option.PLAYERS.on() ? PLAYER : 0;
		}

		if (entity instanceof Enemy) {
			return Option.HOSTILE.on() ? HOSTILE : 0;
		}

		return Option.PASSIVE.on() ? PASSIVE : 0;
	}

	// ================================================================== отрисовка

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;

		if (player == null || mc.options.hideGui || !active()) {
			return;
		}

		SuitTier tier = helmet(player);
		int color = tier.hudColor & 0xFFFFFF;
		Font font = mc.font;
		int w = graphics.guiWidth();
		int h = graphics.guiHeight();

		if (bootTicks > 0) {
			boot(graphics, font, w, h, color);
			return;
		}

		if (Option.ORES.on() || Option.CONTAINERS.on() || Option.SPAWNERS.on()) {
			markers(graphics, mc, font, w, h);
		}

		if (Option.COMPASS.on()) {
			compass(graphics, font, player, w, color);
		}

		if (Option.STATS.on()) {
			stats(graphics, mc, font, player, color);
		}

		if (Option.TARGET.on()) {
			target(graphics, mc, font, w, h, color);
		}
	}

	/** Анимация запуска: полоса сканирования сверху вниз и надпись. */
	private static void boot(GuiGraphicsExtractor graphics, Font font, int w, int h, int color) {
		float t = 1f - bootTicks / 30f;
		int y = Math.round(t * h);
		graphics.fill(0, Math.max(0, y - 2), w, Math.min(h, y + 1), argb(0xC0, color));
		graphics.fill(0, 0, w, y, argb(0x18, color));

		Component text = Component.translatable("helmet.solorust.boot", Math.round(t * 100));
		graphics.centeredText(font, text, w / 2, h / 2 - 20, argb(0xFF, color));
	}

	private static void compass(GuiGraphicsExtractor graphics, Font font, LocalPlayer player, int w, int color) {
		int width = 180;
		int x0 = w / 2 - width / 2;
		int y = 4;
		// yaw 0 = юг; переводим в «0 = север, по часовой».
		float heading = Mth.positiveModulo(player.getYRot() + 180f, 360f);
		float span = 90f;

		graphics.fill(x0, y, x0 + width, y + 12, 0x60000000);

		for (int deg = 0; deg < 360; deg += 15) {
			float diff = Mth.wrapDegrees(deg - heading);

			if (Math.abs(diff) > span / 2) {
				continue;
			}

			int x = Math.round(w / 2f + diff / span * width);

			if (deg % 45 == 0) {
				String label = switch (deg) {
					case 0 -> "N";
					case 45 -> "NE";
					case 90 -> "E";
					case 135 -> "SE";
					case 180 -> "S";
					case 225 -> "SW";
					case 270 -> "W";
					default -> "NW";
				};
				int c = deg == 0 ? 0xFFFF5555 : argb(0xFF, color);
				graphics.centeredText(font, label, x, y + 2, c);
			} else {
				graphics.fill(x, y + 4, x + 1, y + 8, argb(0xA0, color));
			}
		}

		graphics.fill(w / 2, y + 12, w / 2 + 1, y + 15, 0xFFFFFFFF);
		String deg = Math.round(heading) % 360 + "°";
		graphics.centeredText(font, deg, w / 2, y + 16, 0xFFFFFFFF);
	}

	private static void stats(GuiGraphicsExtractor graphics, Minecraft mc, Font font, LocalPlayer player, int color) {
		// Под панелью энергии костюма (если надет полный комплект) или в самом верху.
		int x = 10;
		int y = SuitTier.worn(player) != null ? 56 : 10;
		int c = argb(0xFF, color);

		BlockPos pos = player.blockPosition();
		String biome = mc.level.getBiome(pos).unwrapKey().map(k -> k.identifier().toLanguageKey("biome")).orElse("");

		List<Component> lines = List.of(
				Component.translatable("helmet.solorust.health", fmt(player.getHealth()), fmt(player.getMaxHealth())),
				Component.translatable("helmet.solorust.armor", player.getArmorValue()),
				Component.translatable("helmet.solorust.coords", pos.getX(), pos.getY(), pos.getZ()),
				Component.translatable("helmet.solorust.speed", fmt(speed)),
				Component.translatable("helmet.solorust.biome", Component.translatable(biome)));

		int width = 0;

		for (Component line : lines) {
			width = Math.max(width, font.width(line));
		}

		graphics.fill(x - 4, y - 4, x + Math.max(108, width) + 4, y + lines.size() * 10 + 1, 0x60000000);
		graphics.fill(x - 4, y - 4, x - 3, y + lines.size() * 10 + 1, c);

		for (int i = 0; i < lines.size(); i++) {
			graphics.text(font, lines.get(i), x, y + i * 10, i == 0 && player.getHealth() < player.getMaxHealth() * 0.3f ? 0xFFFF5555 : 0xFFDDDDDD, true);
		}
	}

	private static void target(GuiGraphicsExtractor graphics, Minecraft mc, Font font, int w, int h, int color) {
		int x = w / 2 + 16;
		int y = h / 2 + 10;
		List<Component> lines = new ArrayList<>();
		int titleColor = argb(0xFF, color);
		float hpFraction = -1;

		if (targetEntity instanceof LivingEntity living) {
			titleColor = argb(0xFF, outlineFor(living));
			lines.add(living.getDisplayName());
			lines.add(Component.translatable("helmet.solorust.target.hp", fmt(living.getHealth()), fmt(living.getMaxHealth())));
			lines.add(Component.translatable(living instanceof Enemy ? "helmet.solorust.target.hostile" : living instanceof Player ? "helmet.solorust.target.player" : "helmet.solorust.target.passive"));
			lines.add(Component.translatable("helmet.solorust.target.distance", fmt(targetDistance)));
			hpFraction = living.getHealth() / Math.max(1f, living.getMaxHealth());
		} else if (targetBlock != null && mc.level != null) {
			BlockState state = mc.level.getBlockState(targetBlock);
			lines.add(state.getBlock().getName());
			lines.add(Component.translatable("helmet.solorust.target.distance", fmt(targetDistance)));
			lines.add(Component.literal(targetBlock.getX() + " " + targetBlock.getY() + " " + targetBlock.getZ()));
			float hardness = state.getDestroySpeed(mc.level, targetBlock);
			lines.add(Component.translatable("helmet.solorust.target.hardness", hardness < 0 ? "∞" : fmt(hardness)));
		} else {
			return;
		}

		int width = 70;

		for (Component line : lines) {
			width = Math.max(width, font.width(line));
		}

		int height = lines.size() * 10 + (hpFraction >= 0 ? 5 : 0);
		graphics.fill(x - 3, y - 3, x + width + 3, y + height + 1, 0x60000000);

		for (int i = 0; i < lines.size(); i++) {
			graphics.text(font, lines.get(i), x, y + i * 10, i == 0 ? titleColor : 0xFFCCCCCC, true);
		}

		if (hpFraction >= 0) {
			int by = y + lines.size() * 10;
			graphics.fill(x, by, x + width, by + 3, 0xFF331111);
			graphics.fill(x, by, x + Math.round(width * Mth.clamp(hpFraction, 0, 1)), by + 3, hpFraction < 0.3f ? 0xFFFF4444 : 0xFF55FF66);
		}
	}

	private static int outlineFor(LivingEntity e) {
		if (e instanceof Player) {
			return PLAYER;
		}

		return e instanceof Enemy ? HOSTILE : PASSIVE;
	}

	/** Рамки на блоках: проецируем 8 углов блока на экран и рисуем уголки вокруг. */
	private static void markers(GuiGraphicsExtractor graphics, Minecraft mc, Font font, int w, int h) {
		Camera camera = mc.gameRenderer.getMainCamera();
		Vec3 eye = camera.position();
		Quaternionf inverse = camera.rotation().conjugate(new Quaternionf());
		float tan = (float) Math.tan(Math.toRadians(camera.getFov()) / 2.0);
		float aspect = (float) w / h;
		int cx = w / 2;
		int cy = h / 2;
		Marker labelled = null;
		double labelScore = 400;

		for (Marker m : markers) {
			Category cat = m.category();

			if ((cat == Category.ORE && !Option.ORES.on()) || (cat == Category.CONTAINER && !Option.CONTAINERS.on())
					|| (cat == Category.SPAWNER && !Option.SPAWNERS.on())) {
				continue;
			}

			float minX = Float.MAX_VALUE;
			float minY = Float.MAX_VALUE;
			float maxX = -Float.MAX_VALUE;
			float maxY = -Float.MAX_VALUE;
			boolean visible = true;

			for (int i = 0; i < 8 && visible; i++) {
				Vec3 corner = new Vec3(m.pos().getX() + (i & 1), m.pos().getY() + ((i >> 1) & 1), m.pos().getZ() + ((i >> 2) & 1));
				float[] p = project(corner, eye, inverse, tan, aspect, w, h);

				if (p == null) {
					visible = false;
				} else {
					minX = Math.min(minX, p[0]);
					minY = Math.min(minY, p[1]);
					maxX = Math.max(maxX, p[0]);
					maxY = Math.max(maxY, p[1]);
				}
			}

			if (!visible || maxX < 0 || maxY < 0 || minX > w || minY > h) {
				continue;
			}

			double dist = Math.sqrt(m.pos().distToCenterSqr(eye));
			int alpha = (int) Mth.clampedLerp(dist / 30.0, 0xE0, 0x70);
			int c = argb(alpha, m.color());
			brackets(graphics, Math.round(minX), Math.round(minY), Math.round(maxX), Math.round(maxY), c);

			// Подпись — только у метки ближе всего к перекрестью.
			double mx = (minX + maxX) / 2 - cx;
			double my = (minY + maxY) / 2 - cy;
			double score = mx * mx + my * my;

			if (score < labelScore) {
				labelScore = score;
				labelled = m;
			}
		}

		if (labelled != null) {
			float[] p = project(Vec3.atCenterOf(labelled.pos()), eye, inverse, tan, aspect, w, h);

			if (p != null) {
				double dist = Math.sqrt(labelled.pos().distToCenterSqr(eye));
				Component label = labelled.block().getName().copy().append(" · " + fmt(dist) + "m");
				int lx = Math.round(p[0]);
				int ly = Math.round(p[1]) - 18;
				int lw = font.width(label);
				graphics.fill(lx - lw / 2 - 2, ly - 2, lx + lw / 2 + 2, ly + 9, 0x80000000);
				graphics.centeredText(font, label, lx, ly, argb(0xFF, labelled.color()));
			}
		}
	}

	private static void brackets(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int c) {
		// Не даём рамке схлопнуться вдали и раздуться вплотную.
		if (x1 - x0 < 6) {
			int m = (x0 + x1) / 2;
			x0 = m - 3;
			x1 = m + 3;
		}

		if (y1 - y0 < 6) {
			int m = (y0 + y1) / 2;
			y0 = m - 3;
			y1 = m + 3;
		}

		int lx = Math.max(2, Math.min(8, (x1 - x0) / 4));
		int ly = Math.max(2, Math.min(8, (y1 - y0) / 4));
		graphics.fill(x0, y0, x0 + lx, y0 + 1, c);
		graphics.fill(x0, y0, x0 + 1, y0 + ly, c);
		graphics.fill(x1 - lx, y0, x1, y0 + 1, c);
		graphics.fill(x1 - 1, y0, x1, y0 + ly, c);
		graphics.fill(x0, y1 - 1, x0 + lx, y1, c);
		graphics.fill(x0, y1 - ly, x0 + 1, y1, c);
		graphics.fill(x1 - lx, y1 - 1, x1, y1, c);
		graphics.fill(x1 - 1, y1 - ly, x1, y1, c);
	}

	/** Мировая точка → координаты на экране интерфейса (или null, если позади камеры). */
	private static float[] project(Vec3 world, Vec3 eye, Quaternionf inverse, float tan, float aspect, int w, int h) {
		Vector3f v = new Vector3f((float) (world.x - eye.x), (float) (world.y - eye.y), (float) (world.z - eye.z));
		v.rotate(inverse);

		if (v.z > -0.1f) {
			return null;
		}

		float nx = v.x / (-v.z) / (tan * aspect);
		float ny = v.y / (-v.z) / tan;
		return new float[] {(nx + 1f) / 2f * w, (1f - ny) / 2f * h};
	}

	private static int argb(int alpha, int rgb) {
		return (alpha << 24) | (rgb & 0xFFFFFF);
	}

	private static String fmt(double v) {
		return v >= 100 ? Integer.toString((int) Math.round(v)) : String.format(java.util.Locale.ROOT, "%.1f", v);
	}

	// ================================================================== клавиши

	public static void toggle() {
		HelmetSettings.setEnabled(!HelmetSettings.enabled());
		Minecraft mc = Minecraft.getInstance();

		if (mc.player != null) {
			mc.player.sendOverlayMessage(Component.translatable(HelmetSettings.enabled() ? "helmet.solorust.on" : "helmet.solorust.off"));

			if (!HelmetSettings.enabled()) {
				mc.player.playSound(SoundEvents.BEACON_DEACTIVATE, 0.4f, 2.0f);
			}
		}
	}
}
