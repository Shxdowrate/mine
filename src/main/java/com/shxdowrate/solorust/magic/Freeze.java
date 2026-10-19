package com.shxdowrate.solorust.magic;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Заморозка сущностей (остановка времени, багровые узы): без ИИ, без гравитации, скорость обнуляется каждый тик.
 * Исходное состояние запоминается метками, чтобы после перезахода моб не остался «статуей».
 */
final class Freeze {
	static final String TAG = "solorust_frozen";
	private static final String TAG_NOAI = "solorust_was_noai";
	private static final String TAG_NOGRAVITY = "solorust_was_nogravity";

	private static final class State {
		final Vec3 velocity;
		long until;

		State(Vec3 velocity, long until) {
			this.velocity = velocity;
			this.until = until;
		}
	}

	private static final Map<Entity, State> FROZEN = new IdentityHashMap<>();

	private Freeze() {
	}

	static boolean isFrozen(Entity e) {
		return FROZEN.containsKey(e);
	}

	static void freeze(Entity e, long until) {
		State state = FROZEN.get(e);

		if (state != null) {
			state.until = Math.max(state.until, until);
			return;
		}

		if (e instanceof Mob mob && mob.isNoAi()) {
			e.addTag(TAG_NOAI);
		}

		if (e.isNoGravity()) {
			e.addTag(TAG_NOGRAVITY);
		}

		e.addTag(TAG);

		if (e instanceof Mob mob) {
			mob.setNoAi(true);
			mob.setTarget(null);
		}

		e.setNoGravity(true);
		FROZEN.put(e, new State(e.getDeltaMovement(), until));
		e.setDeltaMovement(Vec3.ZERO);
		e.hurtMarked = true;
	}

	static void tick(long now) {
		for (Iterator<Map.Entry<Entity, State>> it = FROZEN.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Entity, State> entry = it.next();
			Entity e = entry.getKey();
			State state = entry.getValue();

			if (e.isRemoved()) {
				it.remove();
				continue;
			}

			if (now >= state.until) {
				it.remove();
				release(e, state.velocity);
				continue;
			}

			e.setDeltaMovement(Vec3.ZERO);
			e.hurtMarked = true;
		}
	}

	private static void release(Entity e, Vec3 velocity) {
		if (e instanceof Mob mob) {
			mob.setNoAi(e.entityTags().contains(TAG_NOAI));
		}

		e.setNoGravity(e.entityTags().contains(TAG_NOGRAVITY));
		e.removeTag(TAG);
		e.removeTag(TAG_NOAI);
		e.removeTag(TAG_NOGRAVITY);
		e.setDeltaMovement(velocity);
		e.hurtMarked = true;
	}

	/** Сущность загрузилась с меткой заморозки, но заморозки уже нет (перезаход, перезапуск) — размораживаем. */
	static void onLoad(Entity e) {
		if (e.entityTags().contains(TAG) && !FROZEN.containsKey(e)) {
			release(e, Vec3.ZERO);
		}
	}

	static void releaseAll() {
		for (Map.Entry<Entity, State> entry : FROZEN.entrySet()) {
			if (!entry.getKey().isRemoved()) {
				release(entry.getKey(), entry.getValue().velocity);
			}
		}

		FROZEN.clear();
	}
}
