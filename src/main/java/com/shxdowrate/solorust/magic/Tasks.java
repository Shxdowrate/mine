package com.shxdowrate.solorust.magic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.shxdowrate.solorust.SoloRust;

/** Длящиеся эффекты (порталы, щиты, метеоры…): тикаются раз в серверный тик, пока не вернут false. */
final class Tasks {
	@FunctionalInterface
	interface Task {
		/** @param age сколько тиков прошло с запуска (начиная с 0) */
		boolean tick(int age);

		default void end() {
		}
	}

	private static final class Running {
		final Task task;
		int age;

		Running(Task task) {
			this.task = task;
		}
	}

	private static final List<Running> RUNNING = new ArrayList<>();
	private static final List<Running> ADDED = new ArrayList<>();

	private Tasks() {
	}

	static void start(Task task) {
		ADDED.add(new Running(task));
	}

	static void tick() {
		RUNNING.addAll(ADDED);
		ADDED.clear();

		for (Iterator<Running> it = RUNNING.iterator(); it.hasNext(); ) {
			Running r = it.next();
			boolean keep;

			try {
				keep = r.task.tick(r.age++);
			} catch (RuntimeException e) {
				SoloRust.LOGGER.error("Magic task failed", e);
				keep = false;
			}

			if (!keep) {
				it.remove();
				r.task.end();
			}
		}
	}

	static void clear() {
		RUNNING.clear();
		ADDED.clear();
	}
}
