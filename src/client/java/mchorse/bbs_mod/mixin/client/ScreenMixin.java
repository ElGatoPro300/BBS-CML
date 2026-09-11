package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.BBSRendering;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.20.1 has no {@code GameRenderer.renderBlur} and no {@code renderInGameBackground}.
 * Pause / in-game screens darken via {@code Screen.renderBackground(DrawContext)}; sanitize
 * GL state before that draw so hotbar model-block forms cannot leave HUD/world looking dark.
 */
@Mixin(Screen.class)
public class ScreenMixin
{
    @Inject(method = "renderBackground(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("HEAD"))
    private void bbsPreparePauseBackground(DrawContext context, CallbackInfo info)
    {
        BBSRendering.preparePauseScreenState();
    }
}
