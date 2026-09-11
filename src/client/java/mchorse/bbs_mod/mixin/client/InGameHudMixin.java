package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.controller.ICameraController;
import mchorse.bbs_mod.camera.controller.PlayCameraController;
import mchorse.bbs_mod.client.BBSRendering;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class InGameHudMixin
{
    @Inject(method = "extractRenderState", at = @At(value = "HEAD"), cancellable = true)
    public void render(GuiGraphicsExtractor drawContext, DeltaTracker tickCounter, CallbackInfo info)
    {
        ICameraController current = BBSModClient.getCameraController().getCurrent();

        if (current instanceof PlayCameraController)
        {
            info.cancel();

            return;
        }

        /* World forms / model-block items can leave shaderColor or lightmap dirty (worse after
         * pause present). Reset before widgets.png hotbar and GUI item forms. */
        BBSRendering.prepareHudRenderState();
    }

}
