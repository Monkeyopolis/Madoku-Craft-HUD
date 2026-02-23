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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
	private static final int FOOD_RIGHT_EDGE = 91;
	private static final int SECOND_LEFT_VANILLA_FOOD_SLOT_INDEX = 8;
	private static final float HUNGER_TEXT_SCALE = 0.8F;
	private static final int DEFAULT_MAX_FOOD_LEVEL = 20;
	private static final int MIN_MAX_FOOD_LEVEL = 1;
	private static final String HUNGER_TEXT_PREFIX = "Hunger: ";
	private static final int OFFSET_BASELINE_CURRENT = 20;
	private static final int OFFSET_BASELINE_MAX = 20;
	private static final String[] MAX_FOOD_METHOD_NAMES = {
		"getMaxFoodLevel",
		"getMaxFood",
		"getMaxHunger"
	};
	private static final String[] MAX_FOOD_FIELD_NAMES = {
		"maxFoodLevel",
		"maxFood",
		"maxHunger"
	};
	private static final Map<Class<?>, MaxFoodLevelResolver> MAX_FOOD_RESOLVER_CACHE = new ConcurrentHashMap<>();

	private static int cachedFoodLevel = DEFAULT_MAX_FOOD_LEVEL;
	private static int cachedMaxFoodLevel = DEFAULT_MAX_FOOD_LEVEL;

	private HungerHudSystem() {
	}

	public static void init() {
		MadokuClientTickSystem.registerPlayer(MadokuClientTickSystem.Phase.END, (client, player) ->
			updateCache(player)
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

		String hungerText = buildHungerText(cachedFoodLevel, cachedMaxFoodLevel);
		TextRenderer textRenderer = client.textRenderer;
		int foodX = computeFoodX(context, textRenderer, hungerText);
		int foodY = context.getScaledWindowHeight() - HudStatusBarHeightRegistry.getHeight(VanillaHudElements.FOOD_BAR);
		boolean hasHungerEffect = player.hasStatusEffect(StatusEffects.HUNGER);

		context.drawGuiTexture(FOOD_PIPELINE, selectFoodContainerTexture(hasHungerEffect), foodX, foodY, FOOD_SIZE, FOOD_SIZE);

		Identifier fillTexture = selectFoodFillTexture(hasHungerEffect);
		if (fillTexture != null) {
			context.drawGuiTexture(FOOD_PIPELINE, fillTexture, foodX, foodY, FOOD_SIZE, FOOD_SIZE);
		}

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
		float foodPercent = cachedFoodLevel / (float) Math.max(MIN_MAX_FOOD_LEVEL, cachedMaxFoodLevel);
		if (foodPercent <= 0.1F) {
			return null;
		}

		boolean half = foodPercent < 0.9F;
		if (hasHungerEffect) {
			return half ? FOOD_HALF_HUNGER_TEXTURE : FOOD_FULL_HUNGER_TEXTURE;
		}

		return half ? FOOD_HALF_TEXTURE : FOOD_FULL_TEXTURE;
	}

	private static int computeFoodX(DrawContext context, TextRenderer textRenderer, String hungerText) {
		int foodRightEdge = context.getScaledWindowWidth() / 2 + FOOD_RIGHT_EDGE;
		String referenceText = buildOffsetBaselineText();
		int referenceTextWidth = getScaledTextWidth(textRenderer, referenceText, HUNGER_TEXT_SCALE);
		int currentTextWidth = getScaledTextWidth(textRenderer, hungerText, HUNGER_TEXT_SCALE);
		int baseX = foodRightEdge - FOOD_SIZE - (SECOND_LEFT_VANILLA_FOOD_SLOT_INDEX * 8) + FOOD_X_OFFSET_RIGHT;

		// Reflow around the baseline: narrower text moves right, wider text moves left.
		return baseX + (referenceTextWidth - currentTextWidth);
	}

	private static int getScaledTextWidth(TextRenderer textRenderer, String text, float scale) {
		return Math.round(textRenderer.getWidth(text) * scale);
	}

	private static String buildHungerText(int foodLevel, int maxFoodLevel) {
		return HUNGER_TEXT_PREFIX + foodLevel + "/" + maxFoodLevel;
	}

	private static String buildOffsetBaselineText() {
		return HUNGER_TEXT_PREFIX + OFFSET_BASELINE_CURRENT + "/" + OFFSET_BASELINE_MAX;
	}

	private static int resolveMaxFoodLevel(Object hungerManager) {
		if (hungerManager == null) {
			return DEFAULT_MAX_FOOD_LEVEL;
		}

		MaxFoodLevelResolver resolver = MAX_FOOD_RESOLVER_CACHE.computeIfAbsent(
			hungerManager.getClass(),
			HungerHudSystem::createMaxFoodLevelResolver
		);

		int resolvedMaxFoodLevel = resolver.resolve(hungerManager);
		return resolvedMaxFoodLevel > 0 ? resolvedMaxFoodLevel : DEFAULT_MAX_FOOD_LEVEL;
	}

	private static MaxFoodLevelResolver createMaxFoodLevelResolver(Class<?> hungerManagerClass) {
		for (String methodName : MAX_FOOD_METHOD_NAMES) {
			try {
				Method method = hungerManagerClass.getMethod(methodName);
				if (isIntLikeType(method.getReturnType())) {
					HudDebugSystem.info(
						"Using hunger max resolver method {}#{}().",
						hungerManagerClass.getName(),
						methodName
					);
					return manager -> invokeIntMethod(method, manager);
				}
			} catch (NoSuchMethodException ignored) {
				// Try the next known method name.
			}
		}

		for (String fieldName : MAX_FOOD_FIELD_NAMES) {
			try {
				Field field = hungerManagerClass.getField(fieldName);
				if (isIntLikeType(field.getType())) {
					HudDebugSystem.info(
						"Using hunger max resolver field {}#{}.",
						hungerManagerClass.getName(),
						fieldName
					);
					return manager -> readIntField(field, manager);
				}
			} catch (NoSuchFieldException ignored) {
				// Try the next known field name.
			}
		}

		HudDebugSystem.info(
			"No hunger max resolver found for {}; defaulting to {}.",
			hungerManagerClass.getName(),
			DEFAULT_MAX_FOOD_LEVEL
		);
		return manager -> DEFAULT_MAX_FOOD_LEVEL;
	}

	private static void updateCache(PlayerEntity player) {
		int resolvedMaxFoodLevel = Math.max(MIN_MAX_FOOD_LEVEL, resolveMaxFoodLevel(player.getHungerManager()));
		cachedMaxFoodLevel = resolvedMaxFoodLevel;
		cachedFoodLevel = Math.max(0, Math.min(resolvedMaxFoodLevel, player.getHungerManager().getFoodLevel()));
	}

	private static boolean isIntLikeType(Class<?> type) {
		return type == int.class || type == Integer.class;
	}

	private static int invokeIntMethod(Method method, Object target) {
		try {
			Object value = method.invoke(target);
			if (value instanceof Number number) {
				return number.intValue();
			}
		} catch (ReflectiveOperationException ignored) {
			// Fallback to the default max food level when reflection fails.
		}

		return DEFAULT_MAX_FOOD_LEVEL;
	}

	private static int readIntField(Field field, Object target) {
		try {
			Object value = field.get(target);
			if (value instanceof Number number) {
				return number.intValue();
			}
		} catch (ReflectiveOperationException ignored) {
			// Fallback to the default max food level when reflection fails.
		}

		return DEFAULT_MAX_FOOD_LEVEL;
	}

	@FunctionalInterface
	private interface MaxFoodLevelResolver {
		int resolve(Object hungerManager);
	}
}
