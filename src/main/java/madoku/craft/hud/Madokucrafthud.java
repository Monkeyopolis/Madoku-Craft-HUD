package madoku.craft.hud;

import madoku.craft.config.StaticJsonSystem;
import net.fabricmc.api.ModInitializer;

public class Madokucrafthud implements ModInitializer {
    public static final String MOD_ID = "madoku-craft-hud";

    @Override
    public void onInitialize() {
        StaticJsonSystem.initialize();
    }
}
