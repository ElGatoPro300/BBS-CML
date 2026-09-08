package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.bridge.IEntityRenderState;
import mchorse.bbs_mod.client.renderer.MorphRenderer;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import com.mojang.blaze3d.vertex.PoseStack;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin
{
    @WrapOperation(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/client/renderer/state/CameraRenderState;DDDLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V"
        ),
        require = 0
    )
    private void wrapRender(
        EntityRenderer renderer, EntityRenderState state,
        PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState,
        Operation<Void> original
    )
    {
        Entity entity = ((IEntityRenderState) state).bbs$getEntity();

        if (entity instanceof LivingEntity livingEntity && state instanceof LivingEntityRenderState livingState)
        {
            float whiteOverlayProgress = 0F;

            if (renderer instanceof LivingEntityRendererInvoker invoker)
            {
                whiteOverlayProgress = invoker.bbs$getAnimationCounter(livingState);
            }

            int u = OverlayTexture.u(whiteOverlayProgress);
            int v = OverlayTexture.v(livingEntity.hurtTime > 0 || livingEntity.deathTime > 0);
            int o = u | (v << 16);
            CustomVertexConsumerProvider vcp = FormUtilsClient.getProvider();
            int light = livingState.lightCoords;
            float tickDelta = 0F;

            if (MorphRenderer.renderLivingEntity(livingEntity, livingState, tickDelta, matrices, vcp, light, o))
            {
                return;
            }
        }

        original.call(renderer, state, matrices, queue, cameraState);
    }
}