package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.MobForm;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

import com.mojang.blaze3d.vertex.PoseStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Villager type/profession layers are separate dynamic cutouts. On film stubs
 * ({@code AFTER_ENTITIES}) the last clothing layer was deferred until a late
 * {@code Immediate.draw()} after held-item/shadow, with the wrong light basis.
 * Restore vanilla entity lights before clothing, then flush the pending layer
 * as soon as the clothing feature finishes.
 */
@Mixin(VillagerProfessionLayer.class)
public class VillagerClothingFeatureRendererMixin
{
    @Inject(
        method = "submit",
        at = @At("HEAD")
    )
    private void bbs$prepareClothingLighting(
        PoseStack matrices,
        SubmitNodeCollector queue,
        int light,
        LivingEntityRenderState state,
        float armYaw,
        float pitch,
        CallbackInfo info
    )
    {
        if (!this.bbs$shouldFixMobFormClothing())
        {
            return;
        }

        BBSRendering.prepareVanillaEntityLighting();
    }

    @Inject(
        method = "submit",
        at = @At("TAIL")
    )
    private void bbs$flushClothingLayers(
        PoseStack matrices,
        SubmitNodeCollector queue,
        int light,
        LivingEntityRenderState state,
        float armYaw,
        float pitch,
        CallbackInfo info
    )
    {
        if (!this.bbs$shouldFixMobFormClothing())
        {
            return;
        }

        BBSRendering.prepareVanillaEntityLighting();

        FormUtilsClient.flushMobFormFeatureLayers(queue);
    }

    private boolean bbs$shouldFixMobFormClothing()
    {
        if (!(FormUtilsClient.getCurrentForm() instanceof MobForm))
        {
            return false;
        }

        return BBSRendering.isRenderingWorld() && !BBSRendering.isIrisShadowPass();
    }
}
