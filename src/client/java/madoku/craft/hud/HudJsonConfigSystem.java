package madoku.craft.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import madoku.craft.config.StaticJsonSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public final class HudJsonConfigSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(HudJsonConfigSystem.class);
    private static final String SYSTEM_FOLDER_NAME = "madoku-craft-hud";
    private static final String CONFIG_FILE_NAME = "madoku-hud.json";
    private static volatile HudToggles toggles = HudToggles.defaults();

    private HudJsonConfigSystem() {
    }

    public static void initialize() {
        try {
            Path directory = StaticJsonSystem.getOrCreateGlobalSystemDirectory(SYSTEM_FOLDER_NAME);
            Path configFile = directory.resolve(CONFIG_FILE_NAME);
            JsonObject normalized = StaticJsonSystem.ensureManagedFile(configFile, defaultConfigJson());
            toggles = HudToggles.fromJson(normalized);
        } catch (IOException | RuntimeException exception) {
            toggles = HudToggles.defaults();
            LOGGER.error("Failed to load HUD config; using defaults.", exception);
        }
    }

    public static boolean worldHudEnabled() {
        return toggles.worldHud;
    }

    public static boolean healthHudEnabled() {
        return toggles.healthHud;
    }

    public static boolean hungerHudEnabled() {
        return toggles.hungerHud;
    }

    public static boolean armorHudEnabled() {
        return toggles.armorHud;
    }

    public static boolean oxygenHudEnabled() {
        return toggles.oxygenHud;
    }

    private static JsonObject defaultConfigJson() {
        JsonObject root = new JsonObject();
        JsonObject huds = new JsonObject();
        huds.addProperty("health_hud", true);
        huds.addProperty("hunger_hud", true);
        huds.addProperty("armor_hud", true);
        huds.addProperty("oxygen_hud", true);
        huds.addProperty("world_hud", true);
        root.add("huds", huds);
        return root;
    }

    private static boolean getBoolean(JsonObject object, String key, boolean fallback) {
        if (object == null) {
            return fallback;
        }
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            return fallback;
        }
        return value.getAsBoolean();
    }

    private static final class HudToggles {
        private final boolean healthHud;
        private final boolean hungerHud;
        private final boolean armorHud;
        private final boolean oxygenHud;
        private final boolean worldHud;

        private HudToggles(
            boolean healthHud,
            boolean hungerHud,
            boolean armorHud,
            boolean oxygenHud,
            boolean worldHud
        ) {
            this.healthHud = healthHud;
            this.hungerHud = hungerHud;
            this.armorHud = armorHud;
            this.oxygenHud = oxygenHud;
            this.worldHud = worldHud;
        }

        private static HudToggles defaults() {
            return new HudToggles(true, true, true, true, true);
        }

        private static HudToggles fromJson(JsonObject source) {
            JsonObject huds = source != null && source.get("huds") != null && source.get("huds").isJsonObject()
                ? source.getAsJsonObject("huds")
                : null;
            HudToggles defaults = defaults();
            return new HudToggles(
                getBoolean(huds, "health_hud", defaults.healthHud),
                getBoolean(huds, "hunger_hud", defaults.hungerHud),
                getBoolean(huds, "armor_hud", defaults.armorHud),
                getBoolean(huds, "oxygen_hud", defaults.oxygenHud),
                getBoolean(huds, "world_hud", defaults.worldHud)
            );
        }
    }
}
