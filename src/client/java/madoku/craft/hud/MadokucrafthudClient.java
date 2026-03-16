package madoku.craft.hud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class MadokucrafthudClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MadokuHud.initialize();
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> MadokuHud.clearOxygenHudState());
    }
}
