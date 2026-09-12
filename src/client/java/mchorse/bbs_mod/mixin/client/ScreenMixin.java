package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.BBSRendering;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.20.4 has no {@code GameRenderer.renderBlur}. Pause / in-game screens darken with a
 * translucent gradient; sanitize GL state (lightmap / TU0 / ColorModulator) before that draw
 * so hotbar model-block forms cannot leave the world/HUD looking solid dark.
 */
@Mixin(Screen.class)
public class ScreenMixin
{
    @Inject(method = "renderBackground(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("HEAD"))
    private void bbsPreparePauseBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info)
    {
        BBSRendering.preparePauseScreenState();
    }

    @Inject(method = "renderInGameBackground", at = @At("HEAD"))
    private void bbsPrepareInGameBackground(DrawContext context, CallbackInfo info)
    {
        BBSRendering.preparePauseScreenState();
    }
}
