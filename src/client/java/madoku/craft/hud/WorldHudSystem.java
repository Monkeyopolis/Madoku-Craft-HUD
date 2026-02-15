package madoku.craft.hud;

import java.util.Locale;
import madoku.craft.API.system.MadokuClientTickSystem;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

public final class WorldHudSystem {
	private static final int X = 4;
	private static final int Y = 4;
	private static final int LINE_SPACING = 10;
	private static final float TEXT_SCALE = 0.8F;
	private static final Identifier WORLD_HUD_ID = Identifier.of(Madokucrafthud.MOD_ID, "world_hud");

	private static long cachedDay = 1L;
	private static String cachedTimeText = "00:00";
	private static String cachedBiomeName = "Unknown";

	private WorldHudSystem() {
	}

	public static void init() {
		MadokuClientTickSystem.init();
		MadokuClientTickSystem.registerPlayer(MadokuClientTickSystem.Phase.END, (client, player) -> {
			long timeOfDay = player.getEntityWorld().getTimeOfDay();
			long dayTicks = Math.floorMod(timeOfDay, 24000L);

			cachedDay = (timeOfDay / 24000L) + 1L;
			int hours = (int) ((dayTicks / 1000L + 6L) % 24L);
			int minutes = (int) ((dayTicks % 1000L) * 60L / 1000L);
			cachedTimeText = String.format(Locale.ROOT, "%02d:%02d", hours, minutes);
			cachedBiomeName = getBiomeName(player);
		});

		HudElementRegistry.attachElementAfter(
			VanillaHudElements.MISC_OVERLAYS,
			WORLD_HUD_ID,
			WorldHudSystem::render
		);
	}

	private static void render(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();
		PlayerEntity player = client.player;

		if (player == null) {
			return;
		}

		if (!HudJsonConfigSystem.isEnabled(HudJsonConfigSystem.WORLD_HUD)) {
			return;
		}

		String dayText = "Day: " + cachedDay;
		String timeText = "Time: " + cachedTimeText;
		String biomeText = "Biome: " + cachedBiomeName;

		TextRenderer textRenderer = client.textRenderer;
		context.getMatrices().pushMatrix();
		context.getMatrices().scale(TEXT_SCALE, TEXT_SCALE);
		context.drawTextWithShadow(textRenderer, dayText, Math.round(X / TEXT_SCALE), Math.round(Y / TEXT_SCALE), 0xFFFFFFFF);
		context.drawTextWithShadow(
			textRenderer,
			timeText,
			Math.round(X / TEXT_SCALE),
			Math.round((Y + LINE_SPACING) / TEXT_SCALE),
			0xFFFFFFFF
		);
		context.drawTextWithShadow(
			textRenderer,
			biomeText,
			Math.round(X / TEXT_SCALE),
			Math.round((Y + (LINE_SPACING * 2)) / TEXT_SCALE),
			0xFFFFFFFF
		);
		context.getMatrices().popMatrix();
	}

	private static String getBiomeName(PlayerEntity player) {
		return player.getEntityWorld()
			.getBiome(player.getBlockPos())
			.getKey()
			.map(WorldHudSystem::biomeKeyToName)
			.orElse("Unknown");
	}

	private static String biomeKeyToName(RegistryKey<Biome> biomeKey) {
		String normalized = biomeKey.getValue().getPath().replace('_', ' ').replace('/', ' ');
		String[] words = normalized.split(" ");
		StringBuilder builder = new StringBuilder();

		for (String word : words) {
			if (word.isEmpty()) {
				continue;
			}

			if (!builder.isEmpty()) {
				builder.append(' ');
			}

			builder.append(Character.toUpperCase(word.charAt(0)));
			if (word.length() > 1) {
				builder.append(word.substring(1));
			}
		}

		return builder.isEmpty() ? "Unknown" : builder.toString();
	}
}
