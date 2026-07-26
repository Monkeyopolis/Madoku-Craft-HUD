package madoku.craft.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import madoku.craft.api.json.JSONFormatManager;
import madoku.craft.api.json.MadokuJSONManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Client configuration shared by the standalone HUD subsystems. */
public final class HudConfigManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(HudConfigManager.class);
	private static final String CONFIG_FOLDER_NAME = "madoku-craft-hud";
	private static final String CONFIG_FILE_NAME = "madoku-hud";
	private static final String HUD_GROUP = "hud";
	private static final String ENABLED = "enabled";
	private static final String COLORED_TEXT = "colored-text";
	private static volatile Settings settings = Settings.defaults();

	private HudConfigManager() { }

	public static void initialize() { loadConfig(); }
	public static void reset() { settings = Settings.defaults(); }
	public static boolean isEnabled() { return settings.enabled; }
	public static boolean isEnabled(String entry) {
		return settings.enabled && settings.entries.getOrDefault(normalize(entry), EntrySettings.DISABLED).enabled;
	}
	public static boolean isColored(String entry) {
		return isEnabled(entry) && settings.entries.getOrDefault(normalize(entry), EntrySettings.DISABLED).coloredText;
	}

	private static void loadConfig() {
		Settings fallback = Settings.defaults();
		try {
			Path directory = MadokuJSONManager.getOrCreateGlobalSystemDirectory(CONFIG_FOLDER_NAME);
			Path file = directory.resolve(CONFIG_FILE_NAME + ".json");
			JsonObject normalized = JSONFormatManager.ensureManagedFile(file, fallback.toConfigJson());
			Settings loaded = Settings.fromJson(normalized);
			JSONFormatManager.writeManagedFile(file, loaded.toConfigJson(), fallback.toConfigJson());
			settings = loaded;
		} catch (IOException | RuntimeException exception) {
			settings = fallback;
			LOGGER.error("Failed to load Madoku HUD configuration; using defaults.", exception);
		}
	}

	private static JsonObject object(JsonObject source, String key) {
		JsonElement element = source == null ? null : source.get(key);
		return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
	}

	private static boolean bool(JsonObject source, String key, boolean fallback) {
		try { return source != null && source.has(key) ? source.get(key).getAsBoolean() : fallback; }
		catch (RuntimeException ignored) { return fallback; }
	}

	private static String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }

	private static final class EntrySettings {
		private static final EntrySettings DISABLED = new EntrySettings(false, false, false);
		private final boolean enabled;
		private final boolean coloredText;
		private final boolean supportsColoredText;

		private EntrySettings(boolean enabled, boolean coloredText, boolean supportsColoredText) {
			this.enabled = enabled;
			this.coloredText = coloredText;
			this.supportsColoredText = supportsColoredText;
		}
		private static EntrySettings defaults(boolean coloredText) { return new EntrySettings(true, coloredText, coloredText); }
		private static EntrySettings fromJson(JsonObject source, EntrySettings fallback) {
			return new EntrySettings(bool(source, ENABLED, fallback.enabled), bool(source, COLORED_TEXT, fallback.coloredText), fallback.supportsColoredText);
		}
		private void write(JSONFormatManager.ObjectBuilder builder) {
			builder.put(ENABLED, enabled);
			if (supportsColoredText) builder.put(COLORED_TEXT, coloredText);
		}
	}

	private static final class Settings {
		private final boolean enabled;
		private final Map<String, EntrySettings> entries;
		private Settings(boolean enabled, Map<String, EntrySettings> entries) { this.enabled = enabled; this.entries = Map.copyOf(entries); }

		private static Settings defaults() {
			LinkedHashMap<String, EntrySettings> entries = new LinkedHashMap<>();
			entries.put("day", EntrySettings.defaults(false));
			entries.put("time", EntrySettings.defaults(true));
			entries.put("season", EntrySettings.defaults(true));
			entries.put("temperature", EntrySettings.defaults(true));
			entries.put("humidity", EntrySettings.defaults(true));
			entries.put("biome", EntrySettings.defaults(false));
			entries.put("health", EntrySettings.defaults(false));
			entries.put("hunger", EntrySettings.defaults(false));
			entries.put("armor", EntrySettings.defaults(false));
			entries.put("oxygen", EntrySettings.defaults(false));
			return new Settings(true, entries);
		}

		private static Settings fromJson(JsonObject source) {
			Settings fallback = defaults();
			JsonObject hud = object(source, HUD_GROUP);
			LinkedHashMap<String, EntrySettings> entries = new LinkedHashMap<>();
			for (Map.Entry<String, EntrySettings> entry : fallback.entries.entrySet()) {
				JsonObject current = object(hud, entry.getKey());
				if (current.isEmpty() && source != null) {
					String legacy = switch (entry.getKey()) {
						case "day", "time", "biome" -> "world_hud_enabled";
						case "season" -> "season_hud_enabled";
						default -> entry.getKey() + "_hud_enabled";
					};
					JsonObject legacyGroup = object(source, "huds");
					String legacyGroupKey = switch (entry.getKey()) {
						case "day", "time", "biome" -> "world_hud";
						case "season" -> "season_hud";
						default -> entry.getKey() + "_hud";
					};
					current.addProperty(ENABLED, bool(source, legacy, bool(legacyGroup, legacyGroupKey, entry.getValue().enabled)));
				}
				entries.put(entry.getKey(), EntrySettings.fromJson(current, entry.getValue()));
			}
			return new Settings(bool(source, ENABLED, bool(source, "hud_enabled", fallback.enabled)), entries);
		}

		private JsonObject toConfigJson() {
			JSONFormatManager.ObjectBuilder root = JSONFormatManager.object();
			root.object(HUD_GROUP, hud -> entries.forEach((key, value) -> hud.object(key, value::write)));
			return root.build();
		}
	}
}
