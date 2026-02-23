package madoku.craft.hud;

import madoku.craft.API.system.MadokuClientTickSystem;
import net.fabricmc.api.ClientModInitializer;

public class MadokucrafthudClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HudDebugSystem.info("Client entrypoint started.");

		HudDebugSystem.info("Initializing {}.", "HudJsonConfigSystem");
		HudJsonConfigSystem.init();
		HudDebugSystem.info("Initializing {}.", "MadokuClientTickSystem");
		MadokuClientTickSystem.init();
		HudDebugSystem.info("Initializing {}.", "HealthHudSystem");
		HealthHudSystem.init();
		HudDebugSystem.info("Initializing {}.", "HungerHudSystem");
		HungerHudSystem.init();
		HudDebugSystem.info("Initializing {}.", "ArmorHudSystem");
		ArmorHudSystem.init();
		HudDebugSystem.info("Initializing {}.", "OxygenHudSystem");
		OxygenHudSystem.init();
		HudDebugSystem.info("Initializing {}.", "WorldHudSystem");
		WorldHudSystem.init();

		HudDebugSystem.info("Client entrypoint finished.");
	}
}
