package madoku.craft.hud.mixin.client;

import madoku.craft.hud.HudJsonConfigSystem;
import madoku.craft.hud.MadokuHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiHudBarsMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void madokuCraftHud$renderCustomHudAfterVanilla(
        GuiGraphics context,
        DeltaTracker tickCounter,
        CallbackInfo ci
    ) {
        MadokuHud.renderAfterVanilla(context, tickCounter);
    }

    @Inject(method = "renderHearts", at = @At("HEAD"), cancellable = true)
    private void madokuCraftHud$hideVanillaHearts(
        GuiGraphics context,
        Player player,
        int x,
        int y,
        int height,
        int offsetHeartIndex,
        float maxHealth,
        int currentHealth,
        int displayHealth,
        int absorptionAmount,
        boolean renderHighlight,
        CallbackInfo ci
    ) {
        if (HudJsonConfigSystem.healthHudEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
    private void madokuCraftHud$hideVanillaFood(
        GuiGraphics context,
        Player player,
        int y,
        int right,
        CallbackInfo ci
    ) {
        if (HudJsonConfigSystem.hungerHudEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
    private static void madokuCraftHud$hideVanillaArmor(
        GuiGraphics context,
        Player player,
        int x,
        int y,
        int armorValue,
        int heartRows,
        CallbackInfo ci
    ) {
        if (HudJsonConfigSystem.armorHudEnabled()) {
            ci.cancel();
        }
    }

    @Redirect(
        method = "renderPlayerHealth",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"
        )
    )
    private void madokuCraftHud$hideVanillaAir(
        GuiGraphics context,
        ResourceLocation sprite,
        int x,
        int y,
        int width,
        int height
    ) {
        if (HudJsonConfigSystem.oxygenHudEnabled() && isAirHudSprite(sprite)) {
            return;
        }
        context.blitSprite(sprite, x, y, width, height);
    }

    private static boolean isAirHudSprite(ResourceLocation sprite) {
        return "minecraft".equals(sprite.getNamespace()) && sprite.getPath().startsWith("hud/air");
    }
}
