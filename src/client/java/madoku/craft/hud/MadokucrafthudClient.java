package madoku.craft.hud;

import net.fabricmc.api.ClientModInitializer;

public class MadokucrafthudClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		HudJsonConfigSystem.init();
		HealthHudSystem.init();
		HungerHudSystem.init();
		ArmorHudSystem.init();
		OxygenHudSystem.init();
		WorldHudSystem.init();
	}
}
