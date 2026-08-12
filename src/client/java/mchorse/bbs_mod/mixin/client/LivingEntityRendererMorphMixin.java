package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.renderer.IRenderStateEntityHolder;
import mchorse.bbs_mod.client.renderer.MorphRenderer;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import com.mojang.blaze3d.vertex.PoseStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMorphMixin
{
    @Inject(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void bbs$onRenderMorph(LivingEntityRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState, CallbackInfo info)
    {
        if (!(state instanceof IRenderStateEntityHolder holder))
        {
            return;
        }

        Entity entity = holder.bbs$getRenderedEntity();

        if (entity == null)
        {
            return;
        }

        float tickDelta = holder.bbs$getRenderedTickDelta();

        if (entity instanceof AbstractClientPlayer player)
        {
            int overlay = LivingEntityRenderer.getOverlayCoords(state, 0F);

            if (MorphRenderer.collectPlayer(player, state.lightCoords, overlay, tickDelta))
            {
                info.cancel();
            }
        }
        else if (entity instanceof LivingEntity living)
        {
            float counter = ((LivingEntityRendererInvoker) (Object) this).bbs$getAnimationCounter(state);
            int overlay = LivingEntityRenderer.getOverlayCoords(state, counter);

            if (MorphRenderer.collectLivingEntity(living, state.lightCoords, overlay, tickDelta))
            {
                info.cancel();
            }
        }
    }
}
