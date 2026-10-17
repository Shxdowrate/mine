package com.shxdowrate.solorust.gun;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.shxdowrate.solorust.registry.ModSounds;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/** Щелчок «магазин встал на место» в конце перезарядки. */
public final class ReloadSounds {
	private record Pending(ServerPlayer player, int ticksLeft) {
	}

	private static final List<Pending> PENDING = new ArrayList<>();

	private ReloadSounds() {
	}

	public static void schedule(ServerPlayer player, int ticks) {
		PENDING.add(new Pending(player, Math.max(1, ticks - 4)));
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (PENDING.isEmpty()) {
				return;
			}

			List<Pending> next = new ArrayList<>();

			for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
				Pending p = it.next();

				if (p.ticksLeft() <= 1) {
					if (!p.player().isRemoved()) {
						p.player().level().playSound(null, p.player().getX(), p.player().getEyeY(), p.player().getZ(),
								ModSounds.RELOAD_END, SoundSource.PLAYERS, 0.8f, 1.0f);
					}
				} else {
					next.add(new Pending(p.player(), p.ticksLeft() - 1));
				}
			}

			PENDING.clear();
			PENDING.addAll(next);
		});
	}
}
