package com.shxdowrate.solorust.client.helmet;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.shxdowrate.solorust.SoloRust;

import net.fabricmc.loader.api.FabricLoader;

/** Что показывает и подсвечивает шлем. Хранится в config/solorust-helmet.properties. */
public final class HelmetSettings {
	public enum Option {
		STATS("stats", true),
		COMPASS("compass", true),
		TARGET("target", true),
		HOSTILE("hostile", true),
		PASSIVE("passive", false),
		PLAYERS("players", true),
		ITEMS("items", false),
		ORES("ores", true),
		CONTAINERS("containers", true),
		SPAWNERS("spawners", true);

		public final String key;
		final boolean byDefault;
		boolean value;

		Option(String key, boolean byDefault) {
			this.key = key;
			this.byDefault = byDefault;
			this.value = byDefault;
		}

		public boolean on() {
			return value;
		}

		public void toggle() {
			value = !value;
			save();
		}

		public String translationKey() {
			return "helmet.solorust.option." + key;
		}
	}

	/** Включён ли интерфейс шлема (клавиша H). */
	private static boolean enabled = true;

	private HelmetSettings() {
	}

	public static boolean enabled() {
		return enabled;
	}

	public static void setEnabled(boolean value) {
		enabled = value;
		save();
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("solorust-helmet.properties");
	}

	public static void load() {
		Path path = file();

		if (!Files.exists(path)) {
			return;
		}

		Properties props = new Properties();

		try (Reader reader = Files.newBufferedReader(path)) {
			props.load(reader);
		} catch (Exception e) {
			SoloRust.LOGGER.warn("Не удалось прочитать настройки шлема: {}", e.toString());
			return;
		}

		enabled = Boolean.parseBoolean(props.getProperty("enabled", "true"));

		for (Option option : Option.values()) {
			option.value = Boolean.parseBoolean(props.getProperty(option.key, Boolean.toString(option.byDefault)));
		}
	}

	public static void save() {
		Properties props = new Properties();
		props.setProperty("enabled", Boolean.toString(enabled));

		for (Option option : Option.values()) {
			props.setProperty(option.key, Boolean.toString(option.value));
		}

		try {
			Files.createDirectories(file().getParent());

			try (Writer writer = Files.newBufferedWriter(file())) {
				props.store(writer, "Solo Rust - power armor helmet");
			}
		} catch (Exception e) {
			SoloRust.LOGGER.warn("Не удалось сохранить настройки шлема: {}", e.toString());
		}
	}
}
