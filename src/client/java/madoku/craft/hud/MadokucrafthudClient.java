package madoku.craft.hud;

import madoku.craft.api.season.SeasonPayloadManager;
import madoku.craft.api.season.PlayerClimatePayloadManager;
import madoku.craft.api.time.TimePayloadManager;
import madoku.craft.season.ClientSeasonalPrecipitationState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class MadokucrafthudClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MadokuHudManager.initialize();
        ClientPlayNetworking.registerGlobalReceiver(TimePayloadManager.TYPE, (payload, context) ->
            HudPayloadManager.setServerTime(payload.day(), payload.hour(), payload.minute())
        );
        ClientPlayNetworking.registerGlobalReceiver(SeasonPayloadManager.TYPE, (payload, context) ->
            context.client().execute(() -> {
                ClientSeasonalPrecipitationState.update(
                    payload.season(),
                    payload.temperatureOffset(),
                    payload.humidityOffset(),
                    payload.weatherCondition(),
                    payload.seasonDay(),
                    payload.seasonLengthDays());
                ClientSeasonalPrecipitationState.refresh(context.client().level);
                HudPayloadManager.setServerSeason(payload.season());
                HudPayloadManager.setServerSeasonProgress(payload.seasonDay(), payload.seasonLengthDays());
            })
        );
        ClientPlayNetworking.registerGlobalReceiver(PlayerClimatePayloadManager.TYPE, (payload, context) ->
            context.client().execute(() -> HudPayloadManager.setServerClimate(payload.temperature(), payload.humidity()))
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientSeasonalPrecipitationState.clear();
            MadokuHudManager.reset();
        });
    }
}
