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
import net.minecraft.util.Identifier;

public final class OxygenHudSystem {
	private static final Identifier OXYGEN_EMPTY_TEXTURE = Identifier.ofVanilla("hud/air_empty");
	private static final Identifier OXYGEN_POPPING_TEXTURE = Identifier.ofVanilla("hud/air_bursting");
	private static final Identifier OXYGEN_FULL_TEXTURE = Identifier.ofVanilla("hud/air");
	private static final RenderPipeline OXYGEN_PIPELINE = RenderPipelines.GUI_TEXTURED;
	private static final int OXYGEN_SIZE = 9;
	private static final int OXYGEN_TEXT_SPACING = 2;
	private static final int OXYGEN_X_OFFSET_RIGHT = 3;
	private static final float OXYGEN_TEXT_SCALE = 0.8F;
	private static final int POP_TICKS_PER_POINT_LOSS = 2;

	private static int cachedAir = 300;
	private static int cachedMaxAir = 300;
	private static int cachedOxygenPoints = 10;
	private static int previousOxygenPoints = 10;
	private static int popTicksRemaining = 0;

	private OxygenHudSystem() {
	}

	public static void init() {
		MadokuClientTickSystem.init();

		MadokuClientTickSystem.registerPlayer(MadokuClientTickSystem.Phase.END, (client, player) -> {
			cachedAir = Math.max(0, player.getAir());
			cachedMaxAir = Math.max(1, player.getMaxAir());
			cachedOxygenPoints = toOxygenPoints(cachedAir, cachedMaxAir);

			if (cachedOxygenPoints < previousOxygenPoints && cachedOxygenPoints > 0) {
				popTicksRemaining = POP_TICKS_PER_POINT_LOSS;
			} else if (popTicksRemaining > 0) {
				popTicksRemaining--;
			}

			previousOxygenPoints = cachedOxygenPoints;
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

		// Keep vanilla air-bar timing/sounds in sync without rendering the original visuals.
		context.getMatrices().pushMatrix();
		context.getMatrices().translate(-10000.0F, -10000.0F);
		oldElement.render(context, tickCounter);
		context.getMatrices().popMatrix();

		int oxygenRightEdge = context.getScaledWindowWidth() / 2 + 91;
		int secondLeftVanillaAirSlotIndex = 8;
		int oxygenX = oxygenRightEdge - OXYGEN_SIZE - (secondLeftVanillaAirSlotIndex * 8) + OXYGEN_X_OFFSET_RIGHT;
		int oxygenY = context.getScaledWindowHeight() - HudStatusBarHeightRegistry.getHeight(VanillaHudElements.AIR_BAR);

		int oxygenPoints = cachedOxygenPoints;
		context.drawGuiTexture(OXYGEN_PIPELINE, selectOxygenTexture(oxygenPoints), oxygenX, oxygenY, OXYGEN_SIZE, OXYGEN_SIZE);

		String oxygenText = "Oxygen: " + oxygenPoints + "/10";
		TextRenderer textRenderer = client.textRenderer;
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
}
