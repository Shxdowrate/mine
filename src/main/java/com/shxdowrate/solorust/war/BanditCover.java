package com.shxdowrate.solorust.war;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Поиск укрытия: ближайшая точка, где между бандитом и целью есть стена. */
final class BanditCover {
	private BanditCover() {
	}

	/**
	 * Случайно перебирает точки вокруг бандита и возвращает ближайшую, куда можно встать и где
	 * луч от глаз цели до груди бандита упирается в блок. null — укрытия рядом нет.
	 */
	static Vec3 find(ServerLevel level, Bandit bandit, LivingEntity threat, int radius) {
		RandomSource random = bandit.getRandom();
		Vec3 eye = threat.getEyePosition();
		Vec3 best = null;
		double bestDist = Double.MAX_VALUE;
		BlockPos origin = bandit.blockPosition();

		for (int i = 0; i < 24; i++) {
			int dx = random.nextInt(radius * 2 + 1) - radius;
			int dz = random.nextInt(radius * 2 + 1) - radius;
			BlockPos column = origin.offset(dx, 0, dz);
			BlockPos stand = standable(level, column);

			if (stand == null) {
				continue;
			}

			Vec3 chest = Vec3.atBottomCenterOf(stand).add(0, 1.1, 0);

			// Не прячемся ближе к врагу, чем стоим сейчас.
			if (chest.distanceToSqr(eye) < bandit.getEyePosition().distanceToSqr(eye) * 0.7) {
				continue;
			}

			BlockHitResult hit = level.clip(new ClipContext(eye, chest, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, threat));

			if (hit.getType() != HitResult.Type.BLOCK) {
				continue;
			}

			double d = chest.distanceToSqr(bandit.position());

			if (d < bestDist) {
				bestDist = d;
				best = Vec3.atBottomCenterOf(stand);
			}
		}

		return best;
	}

	/** Ищет по высоте ±4 блок, на котором можно стоять: твёрдый пол и два блока воздуха над ним. */
	private static BlockPos standable(ServerLevel level, BlockPos column) {
		for (int dy = 3; dy >= -4; dy--) {
			BlockPos pos = column.above(dy);

			if (!level.isLoaded(pos)) {
				return null;
			}

			BlockState floor = level.getBlockState(pos.below());

			if (floor.isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
					&& level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
					&& level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
					&& level.getFluidState(pos).isEmpty()) {
				return pos;
			}
		}

		return null;
	}
}
