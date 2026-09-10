package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.controller.ICameraController;
import mchorse.bbs_mod.camera.controller.PlayCameraController;
import mchorse.bbs_mod.client.BBSRendering;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class InGameHudMixin
{
    @Inject(method = "extractRenderState", at = @At(value = "HEAD"), cancellable = true)
    public void render(GuiGraphicsExtractor drawContext, DeltaTracker tickCounter, CallbackInfo info)
    {
        ICameraController current = BBSModClient.getCameraController().getCurrent();

        if (current instanceof PlayCameraController)
        {
            BBSRendering.onRenderBeforeScreen();

            info.cancel();

            return;
        }

        /* World forms / model-block items can leave shaderColor or lightmap dirty (worse after
         * pause present). Reset before widgets.png hotbar and GUI item forms. */
        BBSRendering.prepareHudRenderState();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    public void onRenderEnd(GuiGraphicsExtractor drawContext, DeltaTracker tickCounter, CallbackInfo info)
    {
        BBSRendering.onRenderBeforeScreen();
    }
}