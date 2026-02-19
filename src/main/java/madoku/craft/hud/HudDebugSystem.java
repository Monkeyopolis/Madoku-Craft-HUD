package madoku.craft.hud;

import madoku.craft.API.system.MadokuInfoDebugSystem;
import org.slf4j.Logger;

public final class HudDebugSystem {
	private static final String LOG_SOURCE = "HUD";

	private HudDebugSystem() {
	}

	public static void info(String message, Object... args) {
		info(Madokucrafthud.LOGGER, message, args);
	}

	public static void info(Logger logger, String message, Object... args) {
		MadokuInfoDebugSystem.info(logger, LOG_SOURCE, message, args);
	}
}
