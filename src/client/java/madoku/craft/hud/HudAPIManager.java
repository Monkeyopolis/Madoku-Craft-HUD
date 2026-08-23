package madoku.craft.hud;

import madoku.craft.api.season.SeasonBiomeClimateManager;
import madoku.craft.api.season.SeasonEnvironmentTransitionManager;
import madoku.craft.api.time.MadokuTimeManager;
import madoku.craft.season.ClientSeasonalPrecipitationState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;

import java.util.Locale;

/** HUD integration for time, season, climate, and biome data exposed by the API. */
public final class HudAPIManager {
	private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(Madokucrafthud.MOD_ID, "madoku_hud");
	private static final int WORLD_X = 4;
	private static final int WORLD_Y = 4;
	private static final float WORLD_HUD_SCALE = 0.8F;
	private static final int COLOR = 0xFFFFFFFF;

	private HudAPIManager() { }

	public static void initialize() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, HUD_ID, HudAPIManager::renderWorldHud);
	}
	public static void reset() { }

	private static void renderWorldHud(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
		if (!HudConfigManager.isEnabled() || !MadokuHudManager.hasRenderablePlayer(Minecraft.getInstance())) return;
		Minecraft client = Minecraft.getInstance();
		ClientLevel level = client.level;
		LocalPlayer player = client.player;
		long day;
		int hour;
		int minute;
		if (HudPayloadManager.hasServerTime()) {
			day = HudPayloadManager.getServerDay();
			hour = HudPayloadManager.getServerHour();
			minute = HudPayloadManager.getServerMinute();
		} else {
			long clock = level.getOverworldClockTime();
			day = MadokuTimeManager.getDay(clock);
			int totalMinutes = MadokuTimeManager.getTotalMinutes(clock);
			hour = totalMinutes / 60;
			minute = totalMinutes % 60;
		}

		SeasonBiomeClimateManager.Climate climate = HudPayloadManager.hasServerClimate()
			? new SeasonBiomeClimateManager.Climate(
				HudPayloadManager.getServerTemperature(),
				HudPayloadManager.getServerHumidity())
			: ClientSeasonalPrecipitationState.resolveClimate(level.getBiome(player.blockPosition()).value());
		if (!HudPayloadManager.hasServerClimate()) {
			climate = SeasonEnvironmentTransitionManager.adjustForShelter(level, player.blockPosition(), climate);
		}
		int line = 0;
		if (HudConfigManager.isEnabled("day")) drawLine(context, client, "Day", Long.toString(Math.max(0L, day)), line++ , COLOR);
		if (HudConfigManager.isEnabled("time")) drawLine(context, client, "Time", hour + ":" + twoDigits(minute), line++, HudConfigManager.isColored("time") ? timeColor(hour * 60 + minute) : COLOR);
		if (HudConfigManager.isEnabled("season") && HudPayloadManager.hasServerSeason()) {
			String season = capitalize(HudPayloadManager.getServerSeason());
			drawLine(context, client, "Season", season, line++, HudConfigManager.isColored("season") ? seasonColor(season) : COLOR);
		}
		if (HudConfigManager.isEnabled("temperature")) drawLine(context, client, "Temperature", formatClimate(climate.temperature()), line++, HudConfigManager.isColored("temperature") ? climateColor(climate.temperature(), 0xFF5AA9FF, 0xFF55C878, 0xFFFF5A5A) : COLOR);
		if (HudConfigManager.isEnabled("humidity")) drawLine(context, client, "Humidity", formatClimate(climate.humidity()), line++, HudConfigManager.isColored("humidity") ? climateColor(climate.humidity(), 0xFFFF5A5A, 0xFF55C878, 0xFF5AA9FF) : COLOR);
		if (HudConfigManager.isEnabled("biome")) drawLine(context, client, "Biome", getBiomeDisplayName(player, level), line, COLOR);
	}

	private static void drawLine(GuiGraphicsExtractor context, Minecraft client, String label, String value, int line, int valueColor) {
		context.pose().pushMatrix();
		context.pose().translate(WORLD_X, lineOffset(client, line));
		context.pose().scale(WORLD_HUD_SCALE, WORLD_HUD_SCALE);
		String prefix = label + ": ";
		context.text(client.font, prefix, 0, 0, COLOR, true);
		context.text(client.font, value, client.font.width(prefix), 0, valueColor, true);
		context.pose().popMatrix();
	}

	private static int lineOffset(Minecraft client, int lines) { return WORLD_Y + Math.round((client.font.lineHeight + 4) * WORLD_HUD_SCALE) * lines; }
	private static String formatClimate(double value) { return Integer.toString((int) Math.round(value)); }
	private static String twoDigits(int value) { return value < 10 ? "0" + value : Integer.toString(value); }
	private static String capitalize(String value) {
		if (value == null || value.isBlank()) return "Unknown";
		String normalized = value.trim().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
	}

	private static String getBiomeDisplayName(LocalPlayer player, ClientLevel level) {
		return level.getBiome(player.blockPosition()).unwrapKey().map(key -> {
			String normalized = key.identifier().getPath().replace('_', ' ').replace('/', ' ');
			StringBuilder result = new StringBuilder();
			for (String word : normalized.split(" ")) {
				if (word.isEmpty()) continue;
				if (!result.isEmpty()) result.append(' ');
				result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
			}
			return result.isEmpty() ? "Unknown" : result.toString();
		}).orElse("Unknown");
	}

	private static int timeColor(int totalMinutes) {
		int minutes = Math.floorMod(totalMinutes, 1440);
		if (minutes < 60) return 0xFF243B80;
		if (minutes < 300) return interpolate(0xFF243B80, 0xFFFFF2A6, (minutes - 60) / 240.0D);
		if (minutes < 420) return 0xFFFFF2A6;
		if (minutes < 660) return interpolate(0xFFFFF2A6, 0xFFFFB347, (minutes - 420) / 240.0D);
		if (minutes < 780) return 0xFFFFB347;
		if (minutes < 1020) return interpolate(0xFFFFB347, 0xFF8EC8FF, (minutes - 780) / 240.0D);
		if (minutes < 1140) return 0xFF8EC8FF;
		if (minutes < 1380) return interpolate(0xFF8EC8FF, 0xFF243B80, (minutes - 1140) / 240.0D);
		return 0xFF243B80;
	}

	private static int seasonColor(String season) {
		int first = palette(season.toLowerCase(Locale.ROOT));
		if (!HudPayloadManager.hasServerSeason() || first == COLOR) return first;
		int day = HudPayloadManager.getServerSeasonDay();
		int length = HudPayloadManager.getServerSeasonLengthDays();
		String next = switch (season.toLowerCase(Locale.ROOT)) { case "spring" -> "summer"; case "summer" -> "fall"; case "fall" -> "winter"; default -> "spring"; };
		return interpolate(first, palette(next), SeasonEnvironmentTransitionManager.resolveSeasonalTransitionProgress(day, length));
	}
	private static int palette(String season) { return switch (season) { case "spring" -> 0xFFFF9ECF; case "summer" -> 0xFFFFD34E; case "fall" -> 0xFFFF7043; case "winter" -> 0xFFB8E3FF; default -> COLOR; }; }
	private static int climateColor(double value, int low, int peak, int high) {
		double clamped = Math.max(0.0D, Math.min(100.0D, value));
		if (clamped <= 5.0D) return low;
		if (clamped < 45.0D) return interpolate(low, peak, (clamped - 5.0D) / 40.0D);
		if (clamped <= 55.0D) return peak;
		if (clamped < 95.0D) return interpolate(peak, high, (clamped - 55.0D) / 40.0D);
		return high;
	}
	private static int interpolate(int first, int second, double progress) {
		double t = Math.max(0.0D, Math.min(1.0D, progress));
		t = t * t * (3.0D - 2.0D * t);
		return (channel(first >>> 24, second >>> 24, t) << 24) | (channel(first >>> 16, second >>> 16, t) << 16) | (channel(first >>> 8, second >>> 8, t) << 8) | channel(first, second, t);
	}
	private static int channel(int first, int second, double progress) { return (int) Math.round((first & 0xFF) + ((second & 0xFF) - (first & 0xFF)) * progress); }
}
