package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.MobForm;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.VillagerClothingFeatureRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerClothingFeatureRenderer.class)
public class VillagerClothingFeatureRendererMixin
{
    @Inject(
        method = "render",
        at = @At("HEAD")
    )
    private void bbs$prepareClothingLighting(
        MatrixStack matrices,
        OrderedRenderCommandQueue queue,
        int light,
        LivingEntityRenderState state,
        float yaw,
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
        method = "render",
        at = @At("TAIL")
    )
    private void bbs$flushClothingLayers(
        MatrixStack matrices,
        OrderedRenderCommandQueue queue,
        int light,
        LivingEntityRenderState state,
        float yaw,
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

    private boolean bbs$shouldFixMobFormClothing()
    {
        if (!(FormUtilsClient.getCurrentForm() instanceof MobForm))
        {
            return false;
        }

        return BBSRendering.isRenderingWorld() && !BBSRendering.isIrisShadowPass();
    }
}
