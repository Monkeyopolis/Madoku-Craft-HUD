package madoku.craft.hud;

import madoku.craft.network.WorldSeasonPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class MadokucrafthudClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MadokuHud.initialize();
        ClientPlayNetworking.registerGlobalReceiver(WorldSeasonPayload.TYPE, (payload, context) ->
            MadokuHud.setServerSeason(payload.season())
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            MadokuHud.clearServerSeason();
            MadokuHud.clearOxygenHudState();
        });
    }
}
