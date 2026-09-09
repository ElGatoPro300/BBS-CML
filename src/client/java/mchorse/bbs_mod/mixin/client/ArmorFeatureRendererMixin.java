package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.forms.FormUtilsClient;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

import com.mojang.blaze3d.vertex.PoseStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Armor uses per-texture cutout layers on Immediate's fallback buffer.
 * Flush when the feature finishes so a later held-item throw cannot drop it.
 */
@Mixin(HumanoidArmorLayer.class)
public class ArmorFeatureRendererMixin
{
    @Inject(
        method = "submit",
        at = @At("HEAD")
    )
    private void bbs$prepareArmorLighting(
        PoseStack matrices,
        SubmitNodeCollector queue,
        int light,
        HumanoidRenderState state,
        float armYaw,
        float pitch,
        CallbackInfo info
    )
    {
        if (FormUtilsClient.shouldFlushMobFormFeatureLayers())
        {
            BBSRendering.prepareVanillaEntityLighting();
        }
    }

    @Inject(
        method = "submit",
        at = @At("TAIL")
    )
    private void bbs$flushArmorLayers(
        PoseStack matrices,
        SubmitNodeCollector queue,
        int light,
        HumanoidRenderState state,
        float armYaw,
        float pitch,
        CallbackInfo info
    )
    {
        FormUtilsClient.flushMobFormFeatureLayers(queue);
    }
}
