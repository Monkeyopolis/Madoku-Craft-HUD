package madoku.craft.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.Locale;

/** Vanilla attribute bars replaced with compact, value-aware HUD readouts. */
public final class HudAttributesManager {
	private static final RenderPipeline PIPELINE = RenderPipelines.GUI_TEXTURED;
	private static final Identifier HEART_EMPTY = Identifier.withDefaultNamespace("hud/heart/container");
	private static final Identifier HEART_EMPTY_BLINKING = Identifier.withDefaultNamespace("hud/heart/container_blinking");
	private static final Identifier HEART_EMPTY_HARDCORE = Identifier.withDefaultNamespace("hud/heart/container_hardcore");
	private static final Identifier HEART_EMPTY_HARDCORE_BLINKING = Identifier.withDefaultNamespace("hud/heart/container_hardcore_blinking");
	private static final Identifier ABSORBING_FULL = Identifier.withDefaultNamespace("hud/heart/absorbing_full");
	private static final Identifier ABSORBING_FULL_BLINKING = Identifier.withDefaultNamespace("hud/heart/absorbing_full_blinking");
	private static final Identifier ABSORBING_HALF = Identifier.withDefaultNamespace("hud/heart/absorbing_half");
	private static final Identifier ABSORBING_HALF_BLINKING = Identifier.withDefaultNamespace("hud/heart/absorbing_half_blinking");
	private static final Identifier ABSORBING_HARDCORE_FULL = Identifier.withDefaultNamespace("hud/heart/absorbing_hardcore_full");
	private static final Identifier ABSORBING_HARDCORE_FULL_BLINKING = Identifier.withDefaultNamespace("hud/heart/absorbing_hardcore_full_blinking");
	private static final Identifier ABSORBING_HARDCORE_HALF = Identifier.withDefaultNamespace("hud/heart/absorbing_hardcore_half");
	private static final Identifier ABSORBING_HARDCORE_HALF_BLINKING = Identifier.withDefaultNamespace("hud/heart/absorbing_hardcore_half_blinking");
	private static final Identifier FOOD_EMPTY = Identifier.withDefaultNamespace("hud/food_empty");
	private static final Identifier FOOD_HALF = Identifier.withDefaultNamespace("hud/food_half");
	private static final Identifier FOOD_FULL = Identifier.withDefaultNamespace("hud/food_full");
	private static final Identifier FOOD_EMPTY_HUNGER = Identifier.withDefaultNamespace("hud/food_empty_hunger");
	private static final Identifier FOOD_HALF_HUNGER = Identifier.withDefaultNamespace("hud/food_half_hunger");
	private static final Identifier FOOD_FULL_HUNGER = Identifier.withDefaultNamespace("hud/food_full_hunger");
	private static final Identifier ARMOR_EMPTY = Identifier.withDefaultNamespace("hud/armor_empty");
	private static final Identifier ARMOR_HALF = Identifier.withDefaultNamespace("hud/armor_half");
	private static final Identifier ARMOR_FULL = Identifier.withDefaultNamespace("hud/armor_full");
	private static final Identifier OXYGEN_EMPTY = Identifier.withDefaultNamespace("hud/air_empty");
	private static final Identifier OXYGEN_POPPING = Identifier.withDefaultNamespace("hud/air_bursting");
	private static final Identifier OXYGEN_FULL = Identifier.withDefaultNamespace("hud/air");
	private static final int COLOR = 0xFFFFFFFF;
	private static final int FOOD_SIZE = 9;
	private static final int ARMOR_SIZE = 9;
	private static final int OXYGEN_SIZE = 9;
	private static final int VANILLA_MAX_FOOD = 20;
	private static final int VANILLA_MAX_AIR = 300;
	private static final int TICKS_PER_SECOND = 20;
	private static final int OXYGEN_X_OFFSET_RIGHT = 4;
	private static final int OXYGEN_RIGHT_EDGE = 91;
	private static final int FOOD_X_OFFSET_RIGHT = 4;
	private static final int FOOD_RIGHT_EDGE = 91;
	private static final int SECOND_LEFT_SLOT = 8;
	private static final int OXYGEN_POP_TICKS = 2;
	private static final float TEXT_SCALE = 0.8F;
	private static final float HEALTH_STEP = 0.125F;
	private static final float ARMOR_STEP = 0.25F;
	private static volatile int cachedAirSupply = VANILLA_MAX_AIR;
	private static volatile int cachedMaxAirSupply = VANILLA_MAX_AIR;
	private static volatile int cachedOxygenPoints = 10;
	private static volatile int previousDisplayedOxygenSeconds = -1;
	private static volatile int oxygenPopTicksRemaining;
	private static volatile long lastOxygenStateUpdateTick = Long.MIN_VALUE;

	private HudAttributesManager() { }

	public static void initialize() {
		HudElementRegistry.replaceElement(VanillaHudElements.HEALTH_BAR, old -> (context, ticks) -> renderHealth(context, ticks, old));
		HudElementRegistry.replaceElement(VanillaHudElements.FOOD_BAR, old -> (context, ticks) -> renderFood(context, ticks, old));
		HudElementRegistry.replaceElement(VanillaHudElements.ARMOR_BAR, old -> (context, ticks) -> renderArmor(context, ticks, old));
		HudElementRegistry.replaceElement(VanillaHudElements.AIR_BAR, old -> (context, ticks) -> renderOxygen(context, ticks, old));
	}
	public static void reset() { clearOxygenHudState(); }

	private static boolean visible(LocalPlayer player, ClientLevel level, String entry) {
		return player != null && level != null && !player.isSpectator() && HudConfigManager.isEnabled() && HudConfigManager.isEnabled(entry);
	}
	private static void hideVanilla(GuiGraphicsExtractor context, DeltaTracker ticks, HudElement old) {
		context.pose().pushMatrix();
		context.pose().translate(-10000.0F, -10000.0F);
		old.extractRenderState(context, ticks);
		context.pose().popMatrix();
	}

	private static void renderHealth(GuiGraphicsExtractor context, DeltaTracker ticks, HudElement old) {
		Minecraft client = Minecraft.getInstance(); LocalPlayer player = client.player; ClientLevel level = client.level;
		if (!visible(player, level, "health")) { old.extractRenderState(context, ticks); return; }
		hideVanilla(context, ticks, old);
		float health = round(player.getHealth(), HEALTH_STEP);
		float effective = round(player.getHealth() + player.getAbsorptionAmount(), HEALTH_STEP);
		float max = round(Math.max(1.0F, player.getMaxHealth()), HEALTH_STEP);
		int guiTicks = client.gui.hud.getGuiTicks();
		// Minecraft 26.2 no longer exposes Gui.healthBlinkTime.
		boolean blinking = false;
		int x = context.guiWidth() / 2 - 91;
		int y = context.guiHeight() - HudStatusBarHeightRegistry.getHeight(VanillaHudElements.HEALTH_BAR);
		if (player.hasEffect(MobEffects.REGENERATION) && guiTicks % Math.max(1, (int) Math.ceil(max + 5.0F)) == 0) y -= 2;
		if (Math.round(health + player.getAbsorptionAmount()) <= 4) y += player.getRandom().nextInt(2);
		boolean hardcore = level.getLevelData().isHardcore();
		context.blitSprite(PIPELINE, container(hardcore, blinking), x, y, 9, 9);
		Identifier fill = heart(player, health, max, hardcore, blinking);
		if (fill != null) context.blitSprite(PIPELINE, fill, x, y, 9, 9);
		drawText(context, client, "Health: " + format(effective) + "/" + format(max), x + 11, y + 1);
	}

	private static void renderFood(GuiGraphicsExtractor context, DeltaTracker ticks, HudElement old) {
		Minecraft client = Minecraft.getInstance(); LocalPlayer player = client.player; ClientLevel level = client.level;
		if (!visible(player, level, "hunger")) { old.extractRenderState(context, ticks); return; }
		hideVanilla(context, ticks, old);
		int current = Math.max(0, Math.min(VANILLA_MAX_FOOD, player.getFoodData().getFoodLevel()));
		float percent = current / (float) VANILLA_MAX_FOOD;
		String text = "Hunger: " + current + "/" + VANILLA_MAX_FOOD;
		int x = context.guiWidth() / 2 + FOOD_RIGHT_EDGE - FOOD_SIZE - (SECOND_LEFT_SLOT * 8) + FOOD_X_OFFSET_RIGHT;
		int y = context.guiHeight() - HudStatusBarHeightRegistry.getHeight(VanillaHudElements.FOOD_BAR);
		boolean hungry = player.hasEffect(MobEffects.HUNGER);
		context.blitSprite(PIPELINE, hungry ? FOOD_EMPTY_HUNGER : FOOD_EMPTY, x, y, FOOD_SIZE, FOOD_SIZE);
		Identifier fill = food(hungry, percent); if (fill != null) context.blitSprite(PIPELINE, fill, x, y, FOOD_SIZE, FOOD_SIZE);
		drawText(context, client, text, x + 11, y + 1);
	}

	private static void renderArmor(GuiGraphicsExtractor context, DeltaTracker ticks, HudElement old) {
		Minecraft client = Minecraft.getInstance(); LocalPlayer player = client.player; ClientLevel level = client.level;
		if (!visible(player, level, "armor")) { old.extractRenderState(context, ticks); return; }
		int pieces = armorPieces(player); if (pieces <= 0) return;
		hideVanilla(context, ticks, old);
		int x = context.guiWidth() / 2 - 91;
		int y = context.guiHeight() - HudStatusBarHeightRegistry.getHeight(VanillaHudElements.HEALTH_BAR) - 10;
		context.blitSprite(PIPELINE, ARMOR_EMPTY, x, y, ARMOR_SIZE, ARMOR_SIZE);
		Identifier fill = pieces >= 4 ? ARMOR_FULL : pieces >= 2 ? ARMOR_HALF : null;
		if (fill != null) context.blitSprite(PIPELINE, fill, x, y, ARMOR_SIZE, ARMOR_SIZE);
		float points = round((float) Math.max(0.0D, player.getAttributeValue(Attributes.ARMOR)), ARMOR_STEP);
		drawText(context, client, "Armor: " + format(points), x + 11, y + 1);
	}

	private static void renderOxygen(GuiGraphicsExtractor context, DeltaTracker ticks, HudElement old) {
		Minecraft client = Minecraft.getInstance(); LocalPlayer player = client.player; ClientLevel level = client.level;
		if (!visible(player, level, "oxygen")) { old.extractRenderState(context, ticks); return; }
		updateOxygenState(player, level.getGameTime());
		if (player.getAirSupply() >= player.getMaxAirSupply() && !player.isEyeInFluid(FluidTags.WATER)) return;
		String text = oxygenText(cachedAirSupply, cachedMaxAirSupply);
		int x = context.guiWidth() / 2 + OXYGEN_RIGHT_EDGE - OXYGEN_SIZE - SECOND_LEFT_SLOT * 8 + OXYGEN_X_OFFSET_RIGHT;
		int y = context.guiHeight() - HudStatusBarHeightRegistry.getHeight(VanillaHudElements.AIR_BAR);
		context.blitSprite(PIPELINE, oxygenTexture(cachedOxygenPoints), x, y, OXYGEN_SIZE, OXYGEN_SIZE);
		drawText(context, client, text, x + 11, y + 1);
	}

	private static void drawText(GuiGraphicsExtractor context, Minecraft client, String text, int x, int y) {
		context.pose().pushMatrix(); context.pose().scale(TEXT_SCALE, TEXT_SCALE);
		context.text(client.font, text, Math.round(x / TEXT_SCALE), Math.round(y / TEXT_SCALE), COLOR, true);
		context.pose().popMatrix();
	}
	private static Identifier container(boolean hardcore, boolean blinking) { return !hardcore ? (blinking ? HEART_EMPTY_BLINKING : HEART_EMPTY) : (blinking ? HEART_EMPTY_HARDCORE_BLINKING : HEART_EMPTY_HARDCORE); }
	private static Identifier heart(LocalPlayer player, float health, float max, boolean hardcore, boolean blinking) {
		if (health <= 0.0F || health / Math.max(1.0F, max) <= 0.1F) return null;
		boolean half = health / Math.max(1.0F, max) < 0.9F; String fill = half ? "half" : "full"; String prefix = hardcore ? "hardcore_" : ""; String suffix = blinking ? "_blinking" : "";
		if (player.hasEffect(MobEffects.WITHER)) return Identifier.withDefaultNamespace("hud/heart/withered_" + prefix + fill + suffix);
		if (player.getAbsorptionAmount() > 0.0F) {
			if (hardcore) return half ? (blinking ? ABSORBING_HARDCORE_HALF_BLINKING : ABSORBING_HARDCORE_HALF) : (blinking ? ABSORBING_HARDCORE_FULL_BLINKING : ABSORBING_HARDCORE_FULL);
			return half ? (blinking ? ABSORBING_HALF_BLINKING : ABSORBING_HALF) : (blinking ? ABSORBING_FULL_BLINKING : ABSORBING_FULL);
		}
		if (player.hasEffect(MobEffects.POISON)) return Identifier.withDefaultNamespace("hud/heart/poisoned_" + prefix + fill + suffix);
		if (player.isFullyFrozen()) return Identifier.withDefaultNamespace("hud/heart/frozen_" + prefix + fill + suffix);
		return Identifier.withDefaultNamespace("hud/heart/" + prefix + fill + suffix);
	}
	private static Identifier food(boolean hungry, float percent) {
		if (percent <= 0.1F) return null;
		boolean half = percent < 0.9F;
		return hungry ? (half ? FOOD_HALF_HUNGER : FOOD_FULL_HUNGER) : (half ? FOOD_HALF : FOOD_FULL);
	}
	private static int armorPieces(LocalPlayer player) {
		int pieces = 0;
		for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) if (!player.getItemBySlot(slot).isEmpty()) pieces++;
		return pieces;
	}
	private static float round(float value, float step) { return value <= 0.0F || step <= 0.0F ? 0.0F : Math.round(value / step) * step; }
	private static String format(float value) {
		String text = String.format(Locale.ROOT, "%.3f", value);
		while (text.endsWith("0")) text = text.substring(0, text.length() - 1);
		return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
	}

	private static void updateOxygenState(LocalPlayer player, long gameTime) {
		if (lastOxygenStateUpdateTick == gameTime) return;
		lastOxygenStateUpdateTick = gameTime;
		cachedMaxAirSupply = Math.max(1, player.getMaxAirSupply());
		cachedAirSupply = decodeAir(player.getAirSupply(), cachedMaxAirSupply);
		cachedOxygenPoints = Math.max(0, Math.min(10, (int) Math.ceil(cachedAirSupply * 10.0D / cachedMaxAirSupply)));
		int seconds = displaySeconds(cachedAirSupply);
		if (previousDisplayedOxygenSeconds >= 0 && seconds < previousDisplayedOxygenSeconds && seconds > 0) {
			oxygenPopTicksRemaining = OXYGEN_POP_TICKS;
			player.playSound(SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 0.75F, 1.0F);
		} else if (oxygenPopTicksRemaining > 0) oxygenPopTicksRemaining--;
		previousDisplayedOxygenSeconds = seconds;
	}
	private static int decodeAir(int observed, int max) {
		int safeMax = Math.max(1, max); int clamped = MadokuHudManager.clamp(observed, 0, safeMax);
		if (safeMax <= VANILLA_MAX_AIR || clamped > VANILLA_MAX_AIR) return clamped;
		return MadokuHudManager.clamp((int) Math.round(clamped / (double) VANILLA_MAX_AIR * safeMax), 0, safeMax);
	}
	private static int displaySeconds(int ticks) { return (int) Math.ceil(Math.max(0, ticks) / (double) TICKS_PER_SECOND); }
	private static String oxygenText(int current, int max) { int maxSeconds = Math.max(1, displaySeconds(max)); return "Oxygen: " + Math.min(maxSeconds, displaySeconds(current)) + "/" + maxSeconds; }
	private static Identifier oxygenTexture(int points) { return points <= 0 ? OXYGEN_EMPTY : oxygenPopTicksRemaining > 0 ? OXYGEN_POPPING : OXYGEN_FULL; }
	private static void clearOxygenHudState() { cachedAirSupply = VANILLA_MAX_AIR; cachedMaxAirSupply = VANILLA_MAX_AIR; cachedOxygenPoints = 10; previousDisplayedOxygenSeconds = -1; oxygenPopTicksRemaining = 0; lastOxygenStateUpdateTick = Long.MIN_VALUE; }
}
