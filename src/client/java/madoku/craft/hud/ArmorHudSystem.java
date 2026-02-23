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
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

public final class ArmorHudSystem {
	private static final Identifier ARMOR_EMPTY_TEXTURE = Identifier.ofVanilla("hud/armor_empty");
	private static final Identifier ARMOR_HALF_TEXTURE = Identifier.ofVanilla("hud/armor_half");
	private static final Identifier ARMOR_FULL_TEXTURE = Identifier.ofVanilla("hud/armor_full");
	private static final RenderPipeline ARMOR_PIPELINE = RenderPipelines.GUI_TEXTURED;
	private static final int ARMOR_SIZE = 9;
	private static final int ARMOR_ROW_SPACING = 10;
	private static final int ARMOR_TEXT_SPACING = 2;
	private static final float ARMOR_TEXT_SCALE = 0.8F;

	private static int cachedArmor = 0;
	private static int cachedArmorPieces = 0;

	private ArmorHudSystem() {
	}

	public static void init() {
		MadokuClientTickSystem.registerPlayer(MadokuClientTickSystem.Phase.END, (client, player) ->
			updateCache(player)
		);

		HudElementRegistry.replaceElement(VanillaHudElements.ARMOR_BAR, oldElement ->
			(context, tickCounter) -> renderArmor(context, tickCounter, oldElement));
	}

	private static void updateCache(PlayerEntity player) {
		cachedArmor = Math.max(0, player.getArmor());
		cachedArmorPieces = countArmorPieces(player);
	}

	private static void renderArmor(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter, HudElement oldElement) {
		MinecraftClient client = MinecraftClient.getInstance();
		PlayerEntity player = client.player;

		if (player == null || player.isSpectator()) {
			return;
		}

		if (!HudJsonConfigSystem.isEnabled(HudJsonConfigSystem.ARMOR_HUD)) {
			oldElement.render(context, tickCounter);
			return;
		}

		if (cachedArmorPieces <= 0) {
			return;
		}

		// Keep vanilla armor bar flow in sync without showing the old visuals.
		context.getMatrices().pushMatrix();
		context.getMatrices().translate(-10000.0F, -10000.0F);
		oldElement.render(context, tickCounter);
		context.getMatrices().popMatrix();

		int armorX = context.getScaledWindowWidth() / 2 - 91;
		int armorY = computeArmorY(context);

		context.drawGuiTexture(ARMOR_PIPELINE, ARMOR_EMPTY_TEXTURE, armorX, armorY, ARMOR_SIZE, ARMOR_SIZE);

		Identifier fillTexture = selectArmorFillTexture();
		if (fillTexture != null) {
			context.drawGuiTexture(ARMOR_PIPELINE, fillTexture, armorX, armorY, ARMOR_SIZE, ARMOR_SIZE);
		}

		String armorText = "Armor: " + cachedArmor;
		TextRenderer textRenderer = client.textRenderer;
		int textX = armorX + ARMOR_SIZE + ARMOR_TEXT_SPACING;
		int textY = armorY + 1;

		context.getMatrices().pushMatrix();
		context.getMatrices().scale(ARMOR_TEXT_SCALE, ARMOR_TEXT_SCALE);
		context.drawTextWithShadow(
			textRenderer,
			armorText,
			Math.round(textX / ARMOR_TEXT_SCALE),
			Math.round(textY / ARMOR_TEXT_SCALE),
			0xFFFFFFFF
		);
		context.getMatrices().popMatrix();
	}

	private static int computeArmorY(DrawContext context) {
		int windowHeight = context.getScaledWindowHeight();

		if (HudJsonConfigSystem.isEnabled(HudJsonConfigSystem.HEALTH_HUD)) {
			int healthY = windowHeight - HudStatusBarHeightRegistry.getHeight(VanillaHudElements.HEALTH_BAR);
			return healthY - ARMOR_ROW_SPACING;
		}

		return windowHeight - HudStatusBarHeightRegistry.getHeight(VanillaHudElements.ARMOR_BAR);
	}

	private static Identifier selectArmorFillTexture() {
		if (cachedArmorPieces >= 4) {
			return ARMOR_FULL_TEXTURE;
		}

		if (cachedArmorPieces >= 2) {
			return ARMOR_HALF_TEXTURE;
		}

		// For exactly 1 piece, keep container-only (empty armor icon).
		return null;
	}

	private static int countArmorPieces(PlayerEntity player) {
		int pieces = 0;
		if (!player.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) pieces++;
		if (!player.getEquippedStack(EquipmentSlot.CHEST).isEmpty()) pieces++;
		if (!player.getEquippedStack(EquipmentSlot.LEGS).isEmpty()) pieces++;
		if (!player.getEquippedStack(EquipmentSlot.FEET).isEmpty()) pieces++;
		return pieces;
	}
}
