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
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

public final class HungerHudSystem {
	private static final Identifier FOOD_EMPTY_TEXTURE = Identifier.ofVanilla("hud/food_empty");
	private static final Identifier FOOD_HALF_TEXTURE = Identifier.ofVanilla("hud/food_half");
	private static final Identifier FOOD_FULL_TEXTURE = Identifier.ofVanilla("hud/food_full");
	private static final Identifier FOOD_EMPTY_HUNGER_TEXTURE = Identifier.ofVanilla("hud/food_empty_hunger");
	private static final Identifier FOOD_HALF_HUNGER_TEXTURE = Identifier.ofVanilla("hud/food_half_hunger");
	private static final Identifier FOOD_FULL_HUNGER_TEXTURE = Identifier.ofVanilla("hud/food_full_hunger");
	private static final RenderPipeline FOOD_PIPELINE = RenderPipelines.GUI_TEXTURED;
	private static final int FOOD_SIZE = 9;
	private static final int FOOD_TEXT_SPACING = 2;
	private static final int FOOD_X_OFFSET_RIGHT = 4;
	private static final float HUNGER_TEXT_SCALE = 0.8F;

	private static int cachedFoodLevel = 20;

	private HungerHudSystem() {
	}

	public static void init() {
		MadokuClientTickSystem.init();

		MadokuClientTickSystem.registerPlayer(MadokuClientTickSystem.Phase.END, (client, player) ->
			cachedFoodLevel = Math.max(0, Math.min(20, player.getHungerManager().getFoodLevel()))
		);

		HudElementRegistry.replaceElement(VanillaHudElements.FOOD_BAR, oldElement ->
			(context, tickCounter) -> renderFood(context, tickCounter, oldElement));
	}

	private static void renderFood(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter, HudElement oldElement) {
		MinecraftClient client = MinecraftClient.getInstance();
		PlayerEntity player = client.player;

		if (player == null || player.isSpectator()) {
			return;
		}

		if (!HudJsonConfigSystem.isEnabled(HudJsonConfigSystem.HUNGER_HUD)) {
			oldElement.render(context, tickCounter);
			return;
		}

		// Keep vanilla food-bar animation/effect state progression in sync.
		context.getMatrices().pushMatrix();
		context.getMatrices().translate(-10000.0F, -10000.0F);
		oldElement.render(context, tickCounter);
		context.getMatrices().popMatrix();

		int foodRightEdge = context.getScaledWindowWidth() / 2 + 91;
		int secondLeftVanillaFoodSlotIndex = 8;
		int foodX = foodRightEdge - FOOD_SIZE - (secondLeftVanillaFoodSlotIndex * 8) + FOOD_X_OFFSET_RIGHT;
		int foodY = context.getScaledWindowHeight() - HudStatusBarHeightRegistry.getHeight(VanillaHudElements.FOOD_BAR);
		boolean hasHungerEffect = player.hasStatusEffect(StatusEffects.HUNGER);

		context.drawGuiTexture(FOOD_PIPELINE, selectFoodContainerTexture(hasHungerEffect), foodX, foodY, FOOD_SIZE, FOOD_SIZE);

		Identifier fillTexture = selectFoodFillTexture(hasHungerEffect);
		if (fillTexture != null) {
			context.drawGuiTexture(FOOD_PIPELINE, fillTexture, foodX, foodY, FOOD_SIZE, FOOD_SIZE);
		}

		String hungerText = "Hunger: " + cachedFoodLevel + "/20";
		TextRenderer textRenderer = client.textRenderer;
		int textX = foodX + FOOD_SIZE + FOOD_TEXT_SPACING;
		int textY = foodY + 1;

		context.getMatrices().pushMatrix();
		context.getMatrices().scale(HUNGER_TEXT_SCALE, HUNGER_TEXT_SCALE);
		context.drawTextWithShadow(
			textRenderer,
			hungerText,
			Math.round(textX / HUNGER_TEXT_SCALE),
			Math.round(textY / HUNGER_TEXT_SCALE),
			0xFFFFFFFF
		);
		context.getMatrices().popMatrix();
	}

	private static Identifier selectFoodContainerTexture(boolean hasHungerEffect) {
		return hasHungerEffect ? FOOD_EMPTY_HUNGER_TEXTURE : FOOD_EMPTY_TEXTURE;
	}

	private static Identifier selectFoodFillTexture(boolean hasHungerEffect) {
		float foodPercent = cachedFoodLevel / 20.0F;
		if (foodPercent <= 0.1F) {
			return null;
		}

		boolean half = foodPercent < 0.9F;
		if (hasHungerEffect) {
			return half ? FOOD_HALF_HUNGER_TEXTURE : FOOD_FULL_HUNGER_TEXTURE;
		}

		return half ? FOOD_HALF_TEXTURE : FOOD_FULL_TEXTURE;
	}
}
