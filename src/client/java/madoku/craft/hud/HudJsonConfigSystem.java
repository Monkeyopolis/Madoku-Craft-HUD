package madoku.craft.hud;

import com.google.gson.JsonObject;
import madoku.craft.API.system.JsonFeatureSystem;

public final class HudJsonConfigSystem {
	public static final String HEALTH_HUD = "health_hud";
	public static final String HUNGER_HUD = "hunger_hud";
	public static final String ARMOR_HUD = "armor_hud";
	public static final String OXYGEN_HUD = "oxygen_hud";
	public static final String WORLD_HUD = "world_hud";

	private static final String FEATURE_ID = "madoku_craft_hud";
	private static final String HUDS_KEY = "huds";

	private static JsonFeatureSystem.ManagedFeature managedFeature;
	private static JsonObject root;

	private HudJsonConfigSystem() {
	}

	public static void init() {
		if (managedFeature != null) {
			return;
		}

		managedFeature = JsonFeatureSystem.loadFeature(FEATURE_ID, buildDefaults());
		root = managedFeature.getRoot();
	}

	public static boolean isEnabled(String hudKey) {
		init();

		JsonObject huds = root.getAsJsonObject(HUDS_KEY);
		if (huds == null || !huds.has(hudKey)) {
			return true;
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
