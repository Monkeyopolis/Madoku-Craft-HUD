package madoku.craft.hud;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import madoku.craft.API.system.MadokuClientTickSystem;
import madoku.craft.hud.mixin.client.InGameHudAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;

public final class HealthHudSystem {
	private static final Identifier HEART_EMPTY_TEXTURE = Identifier.ofVanilla("hud/heart/container");
	private static final Identifier HEART_EMPTY_BLINKING_TEXTURE = Identifier.ofVanilla("hud/heart/container_blinking");
	private static final Identifier HEART_EMPTY_HARDCORE_TEXTURE = Identifier.ofVanilla("hud/heart/container_hardcore");
	private static final Identifier HEART_EMPTY_HARDCORE_BLINKING_TEXTURE = Identifier.ofVanilla("hud/heart/container_hardcore_blinking");
	private static final Identifier ABSORBING_FULL_TEXTURE = Identifier.ofVanilla("hud/heart/absorbing_full");
	private static final Identifier ABSORBING_FULL_BLINKING_TEXTURE = Identifier.ofVanilla("hud/heart/absorbing_full_blinking");
	private static final Identifier ABSORBING_HALF_TEXTURE = Identifier.ofVanilla("hud/heart/absorbing_half");
	private static final Identifier ABSORBING_HALF_BLINKING_TEXTURE = Identifier.ofVanilla("hud/heart/absorbing_half_blinking");
	private static final Identifier ABSORBING_HARDCORE_FULL_TEXTURE = Identifier.ofVanilla("hud/heart/absorbing_hardcore_full");
	private static final Identifier ABSORBING_HARDCORE_FULL_BLINKING_TEXTURE = Identifier.ofVanilla("hud/heart/absorbing_hardcore_full_blinking");
	private static final Identifier ABSORBING_HARDCORE_HALF_TEXTURE = Identifier.ofVanilla("hud/heart/absorbing_hardcore_half");
	private static final Identifier ABSORBING_HARDCORE_HALF_BLINKING_TEXTURE = Identifier.ofVanilla("hud/heart/absorbing_hardcore_half_blinking");
	private static final RenderPipeline HEART_PIPELINE = RenderPipelines.GUI_TEXTURED;
	private static final int HEART_SIZE = 9;
	private static final int HEART_TEXT_SPACING = 2;
	private static final float HEALTH_TEXT_SCALE = 0.8F;

	private static float cachedHealth;
	private static float cachedMaxHealth = 20.0F;

	private HealthHudSystem() {
	}

	public static void init() {
		MadokuClientTickSystem.init();

		MadokuClientTickSystem.registerPlayer(MadokuClientTickSystem.Phase.END, (client, player) -> {
			cachedHealth = Math.max(0.0F, player.getHealth());
			cachedMaxHealth = Math.max(1.0F, player.getMaxHealth());
		});

		HudElementRegistry.replaceElement(VanillaHudElements.HEALTH_BAR, oldElement ->
			(context, tickCounter) -> render(context, tickCounter, oldElement));
	}

	private static void render(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter, HudElement oldElement) {
		MinecraftClient client = MinecraftClient.getInstance();
		PlayerEntity player = client.player;

		if (player == null || player.isSpectator()) {
			return;
		}

		if (!HudJsonConfigSystem.isEnabled(HudJsonConfigSystem.HEALTH_HUD)) {
			oldElement.render(context, tickCounter);
			return;
		}

		// Keep vanilla health state progression (blink timers, regen timing, etc.) in sync.
		// Render it far off-screen so vanilla visuals are hidden without clipping our custom text.
		context.getMatrices().pushMatrix();
		context.getMatrices().translate(-10000.0F, -10000.0F);
		oldElement.render(context, tickCounter);
		context.getMatrices().popMatrix();

		int heartX = context.getScaledWindowWidth() / 2 - 91;
		int vanillaHealthY = context.getScaledWindowHeight() - HudStatusBarHeightRegistry.getHeight(VanillaHudElements.HEALTH_BAR);
		int heartY = vanillaHealthY;

		boolean hardcore = client.world != null && client.world.getLevelProperties().isHardcore();
		InGameHudAccessor inGameHudAccessor = (InGameHudAccessor) client.inGameHud;
		boolean blinking = isBlinking(inGameHudAccessor);
		int ticks = inGameHudAccessor.madokucrafthud$getTicks();

		if (player.hasStatusEffect(StatusEffects.REGENERATION)) {
			int regenIndex = ticks % Math.max(1, (int) Math.ceil(cachedMaxHealth + 5.0F));
			if (regenIndex == 0) {
				heartY -= 2;
			}
		}

		if (Math.round(cachedHealth + player.getAbsorptionAmount()) <= 4) {
			heartY += player.getRandom().nextInt(2);
		}

		Identifier fillTexture = selectHeartTexture(player, hardcore, blinking);
		Identifier containerTexture = selectContainerTexture(hardcore, blinking);

		context.drawGuiTexture(HEART_PIPELINE, containerTexture, heartX, heartY, HEART_SIZE, HEART_SIZE);

		if (fillTexture != null) {
			context.drawGuiTexture(HEART_PIPELINE, fillTexture, heartX, heartY, HEART_SIZE, HEART_SIZE);
		}

		String shownHealth = formatQuarterValue(cachedHealth);
		String shownMaxHealth = formatQuarterValue(cachedMaxHealth);
		String healthText = "Health: " + shownHealth + "/" + shownMaxHealth;

		TextRenderer textRenderer = client.textRenderer;
		int textX = heartX + HEART_SIZE + HEART_TEXT_SPACING;
		int textY = heartY + 1;
		context.getMatrices().pushMatrix();
		context.getMatrices().scale(HEALTH_TEXT_SCALE, HEALTH_TEXT_SCALE);
		context.drawTextWithShadow(
			textRenderer,
			healthText,
			Math.round(textX / HEALTH_TEXT_SCALE),
			Math.round(textY / HEALTH_TEXT_SCALE),
			0xFFFFFFFF
		);
		context.getMatrices().popMatrix();
	}

	private static boolean isBlinking(InGameHudAccessor accessor) {
		long heartJumpEndTick = accessor.madokucrafthud$getHeartJumpEndTick();
		long ticks = accessor.madokucrafthud$getTicks();
		return heartJumpEndTick > ticks && ((heartJumpEndTick - ticks) / 3L) % 2L == 1L;
	}

	private static Identifier selectContainerTexture(boolean hardcore, boolean blinking) {
		if (!hardcore) {
			return blinking ? HEART_EMPTY_BLINKING_TEXTURE : HEART_EMPTY_TEXTURE;
		}

		return blinking ? HEART_EMPTY_HARDCORE_BLINKING_TEXTURE : HEART_EMPTY_HARDCORE_TEXTURE;
	}

	private static Identifier selectHeartTexture(PlayerEntity player, boolean hardcore, boolean blinking) {
		if (cachedHealth <= 0.0F) {
			return null;
		}

		float healthPercent = cachedHealth / Math.max(1.0F, cachedMaxHealth);
		if (healthPercent <= 0.1F) {
			return null;
		}

		boolean half = healthPercent < 0.9F;
		String fill = half ? "half" : "full";
		String blinkingSuffix = blinking ? "_blinking" : "";

		if (player.hasStatusEffect(StatusEffects.WITHER)) {
			return Identifier.ofVanilla("hud/heart/withered_" + (hardcore ? "hardcore_" : "") + fill + blinkingSuffix);
		}

		if (player.getAbsorptionAmount() > 0.0F) {
			return selectAbsorbingTexture(hardcore, half, blinking);
		}

		if (player.hasStatusEffect(StatusEffects.POISON)) {
			return Identifier.ofVanilla("hud/heart/poisoned_" + (hardcore ? "hardcore_" : "") + fill + blinkingSuffix);
		}

		if (player.isFrozen()) {
			return Identifier.ofVanilla("hud/heart/frozen_" + (hardcore ? "hardcore_" : "") + fill + blinkingSuffix);
		}

		return Identifier.ofVanilla("hud/heart/" + (hardcore ? "hardcore_" : "") + fill + blinkingSuffix);
	}

	private static Identifier selectAbsorbingTexture(boolean hardcore, boolean half, boolean blinking) {
		if (hardcore) {
			if (half) {
				return blinking ? ABSORBING_HARDCORE_HALF_BLINKING_TEXTURE : ABSORBING_HARDCORE_HALF_TEXTURE;
			}
			return blinking ? ABSORBING_HARDCORE_FULL_BLINKING_TEXTURE : ABSORBING_HARDCORE_FULL_TEXTURE;
		}

		if (half) {
			return blinking ? ABSORBING_HALF_BLINKING_TEXTURE : ABSORBING_HALF_TEXTURE;
		}
		return blinking ? ABSORBING_FULL_BLINKING_TEXTURE : ABSORBING_FULL_TEXTURE;
	}

	private static String formatQuarterValue(float value) {
		int quarterUnits = Math.round(value * 4.0F);
		int whole = quarterUnits / 4;
		int quarterRemainder = Math.abs(quarterUnits % 4);

		return switch (quarterRemainder) {
			case 0 -> Integer.toString(whole);
			case 1 -> whole + ".25";
			case 2 -> whole + ".5";
			case 3 -> whole + ".75";
			default -> Integer.toString(whole);
		};
	}
}
