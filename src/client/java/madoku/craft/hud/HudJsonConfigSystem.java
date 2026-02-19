package madoku.craft.hud;

import com.google.gson.JsonObject;
import madoku.craft.API.system.MadokuJSONSystem;

public final class HudJsonConfigSystem {
	public static final String HEALTH_HUD = "health_hud";
	public static final String HUNGER_HUD = "hunger_hud";
	public static final String ARMOR_HUD = "armor_hud";
	public static final String OXYGEN_HUD = "oxygen_hud";
	public static final String WORLD_HUD = "world_hud";

	private static final String FEATURE_ID = "madoku_craft_hud";
	private static final String JSON_FOLDER_ID = "HUD";
	private static final String JSON_FILE_ID = FEATURE_ID;
	private static final String HUDS_KEY = "huds";

	private static MadokuJSONSystem.ManagedJSON managedFeature;
	private static JsonObject root;

	private HudJsonConfigSystem() {
	}

	public static void init() {
		if (managedFeature != null) {
			return;
		}

		managedFeature = MadokuJSONSystem.load(JSON_FOLDER_ID, JSON_FILE_ID, buildDefaults());
		root = managedFeature.getRoot();

		HudDebugSystem.info("Loaded HUD config at {}.", managedFeature.getPath());
		HudDebugSystem.info(
			"HUD toggles: health={}, hunger={}, armor={}, oxygen={}, world={}.",
			readEnabled(HEALTH_HUD, true),
			readEnabled(HUNGER_HUD, true),
			readEnabled(ARMOR_HUD, true),
			readEnabled(OXYGEN_HUD, true),
			readEnabled(WORLD_HUD, true)
		);
	}

	public static boolean isEnabled(String hudKey) {
		init();
		return readEnabled(hudKey, true);
	}

	private static boolean readEnabled(String hudKey, boolean fallback) {
		JsonObject huds = root.getAsJsonObject(HUDS_KEY);
		if (huds == null || !huds.has(hudKey)) {
			return fallback;
		}

		return huds.get(hudKey).getAsBoolean();
	}

	private static JsonObject buildDefaults() {
		JsonObject defaults = new JsonObject();
		JsonObject huds = new JsonObject();

		huds.addProperty(HEALTH_HUD, true);
		huds.addProperty(HUNGER_HUD, true);
		huds.addProperty(ARMOR_HUD, true);
		huds.addProperty(OXYGEN_HUD, true);
		huds.addProperty(WORLD_HUD, true);

		defaults.add(HUDS_KEY, huds);
		return defaults;
	}
}
