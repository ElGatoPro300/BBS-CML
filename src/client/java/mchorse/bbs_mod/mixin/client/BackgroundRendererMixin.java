package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;

import org.joml.Vector4f;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public class BackgroundRendererMixin
{
    @Inject(method = "computeFogColor", at = @At("HEAD"), cancellable = true)
    private void onGetFogColor(Camera camera, float tickDelta, ClientLevel world, int clampedViewDistance, float skyDarkness, Vector4f color, CallbackInfo info)
    {
        if (BBSRendering.isChromaSkyEnabled())
        {
            Color chromaColor = Color.rgb(BBSRendering.getChromaSkyColor());

            color.set(chromaColor.r, chromaColor.g, chromaColor.b, 1F);
            info.cancel();
        }
    }
}
