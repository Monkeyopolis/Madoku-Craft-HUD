package madoku.craft.hud;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import madoku.craft.API.system.MadokuClientTickSystem;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public final class OxygenHudSystem {
	private static final Identifier OXYGEN_EMPTY_TEXTURE = Identifier.ofVanilla("hud/air_empty");
	private static final Identifier OXYGEN_POPPING_TEXTURE = Identifier.ofVanilla("hud/air_bursting");
	private static final Identifier OXYGEN_FULL_TEXTURE = Identifier.ofVanilla("hud/air");
	private static final RenderPipeline OXYGEN_PIPELINE = RenderPipelines.GUI_TEXTURED;
	private static final int OXYGEN_SIZE = 9;
	private static final int OXYGEN_TEXT_SPACING = 2;
	private static final int OXYGEN_X_OFFSET_RIGHT = 4;
	private static final int OXYGEN_RIGHT_EDGE = 91;
	private static final int SECOND_LEFT_VANILLA_AIR_SLOT_INDEX = 8;
	private static final float OXYGEN_TEXT_SCALE = 0.8F;
	private static final int POP_TICKS_PER_SECOND_LOSS = 2;
	private static final String OXYGEN_TEXT_PREFIX = "Oxygen: ";
	private static final int TICKS_PER_SECOND = 20;
	private static final int MIN_MAX_AIR = 1;
	private static final int OFFSET_BASELINE_CURRENT = 15;
	private static final int OFFSET_BASELINE_MAX = 15;

	private static int cachedAir = 300;
	private static int cachedMaxAir = 300;
	private static int cachedOxygenPoints = 10;
	private static int previousDisplayedSeconds = -1;
	private static int popTicksRemaining = 0;

	private OxygenHudSystem() {
	}

	public static void init() {
		MadokuClientTickSystem.init();

		MadokuClientTickSystem.registerPlayer(MadokuClientTickSystem.Phase.END, (client, player) -> {
			cachedAir = Math.max(0, player.getAir());
			cachedMaxAir = Math.max(1, player.getMaxAir());
			cachedOxygenPoints = toOxygenPoints(cachedAir, cachedMaxAir);
			int currentDisplayedSeconds = toDisplaySeconds(cachedAir);

			if (previousDisplayedSeconds >= 0 && currentDisplayedSeconds < previousDisplayedSeconds && currentDisplayedSeconds > 0) {
				popTicksRemaining = POP_TICKS_PER_SECOND_LOSS;
				playOxygenPopSound(player);
			} else if (popTicksRemaining > 0) {
				popTicksRemaining--;
			}

			previousDisplayedSeconds = currentDisplayedSeconds;
		});

		HudElementRegistry.replaceElement(VanillaHudElements.AIR_BAR, oldElement ->
			(context, tickCounter) -> renderOxygen(context, tickCounter, oldElement));
	}

	private static void renderOxygen(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter, HudElement oldElement) {
		MinecraftClient client = MinecraftClient.getInstance();
		PlayerEntity player = client.player;

		if (player == null || player.isSpectator()) {
			return;
		}

		if (!HudJsonConfigSystem.isEnabled(HudJsonConfigSystem.OXYGEN_HUD)) {
			oldElement.render(context, tickCounter);
			return;
		}

		boolean shouldRender = player.getAir() < player.getMaxAir() || player.isSubmergedIn(FluidTags.WATER);
		if (!shouldRender) {
			return;
		}

		// We fully control oxygen visuals/sounds so second-based timing stays consistent.

		int oxygenPoints = cachedOxygenPoints;
		String oxygenText = buildOxygenTextFromSeconds(cachedAir, cachedMaxAir);
		TextRenderer textRenderer = client.textRenderer;
		int oxygenX = computeOxygenX(context, textRenderer, oxygenText, cachedMaxAir);
		int oxygenY = context.getScaledWindowHeight() - HudStatusBarHeightRegistry.getHeight(VanillaHudElements.AIR_BAR);
		context.drawGuiTexture(OXYGEN_PIPELINE, selectOxygenTexture(oxygenPoints), oxygenX, oxygenY, OXYGEN_SIZE, OXYGEN_SIZE);

		int textX = oxygenX + OXYGEN_SIZE + OXYGEN_TEXT_SPACING;
		int textY = oxygenY + 1;

		context.getMatrices().pushMatrix();
		context.getMatrices().scale(OXYGEN_TEXT_SCALE, OXYGEN_TEXT_SCALE);
		context.drawTextWithShadow(
			textRenderer,
			oxygenText,
			Math.round(textX / OXYGEN_TEXT_SCALE),
			Math.round(textY / OXYGEN_TEXT_SCALE),
			0xFFFFFFFF
		);
		context.getMatrices().popMatrix();
	}

	private static int toOxygenPoints(int air, int maxAir) {
		double ratio = (Math.max(0, air) * 10.0) / Math.max(1, maxAir);
		return Math.max(0, Math.min(10, (int) Math.ceil(ratio)));
	}

	private static Identifier selectOxygenTexture(int oxygenPoints) {
		if (oxygenPoints <= 0) {
			return OXYGEN_EMPTY_TEXTURE;
		}

		if (popTicksRemaining > 0) {
			return OXYGEN_POPPING_TEXTURE;
		}

		return OXYGEN_FULL_TEXTURE;
	}

	private static int computeOxygenX(DrawContext context, TextRenderer textRenderer, String oxygenText, int maxAir) {
		int oxygenRightEdge = context.getScaledWindowWidth() / 2 + OXYGEN_RIGHT_EDGE;
		String referenceText = buildOffsetBaselineText();
		int referenceTextWidth = getScaledTextWidth(textRenderer, referenceText, OXYGEN_TEXT_SCALE);
		int currentTextWidth = getScaledTextWidth(textRenderer, oxygenText, OXYGEN_TEXT_SCALE);
		int baseX = oxygenRightEdge - OXYGEN_SIZE - (SECOND_LEFT_VANILLA_AIR_SLOT_INDEX * 8) + OXYGEN_X_OFFSET_RIGHT;

		// Reflow around the baseline: narrower text moves right, wider text moves left.
		return baseX + (referenceTextWidth - currentTextWidth);
	}

	private static int getScaledTextWidth(TextRenderer textRenderer, String text, float scale) {
		return Math.round(textRenderer.getWidth(text) * scale);
	}

	private static String buildOxygenTextFromSeconds(int currentAir, int maxAir) {
		int normalizedMaxAir = Math.max(MIN_MAX_AIR, maxAir);
		int normalizedCurrentAir = Math.max(0, Math.min(normalizedMaxAir, currentAir));
		int maxSeconds = Math.max(1, toDisplaySeconds(normalizedMaxAir));
		int currentSeconds = Math.max(0, Math.min(maxSeconds, toDisplaySeconds(normalizedCurrentAir)));
		return OXYGEN_TEXT_PREFIX + currentSeconds + "/" + maxSeconds;
	}

	private static String buildOffsetBaselineText() {
		return OXYGEN_TEXT_PREFIX + OFFSET_BASELINE_CURRENT + "/" + OFFSET_BASELINE_MAX;
	}

	private static int toDisplaySeconds(int ticks) {
		return (int) Math.ceil(Math.max(0, ticks) / (double) TICKS_PER_SECOND);
	}

	private static void playOxygenPopSound(PlayerEntity player) {
		player.playSound(SoundEvents.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 0.75F, 1.0F);
	}
}
