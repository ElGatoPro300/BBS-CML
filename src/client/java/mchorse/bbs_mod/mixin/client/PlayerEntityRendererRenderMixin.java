package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.bridge.IEntityRenderState;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.renderer.MorphRenderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class PlayerEntityRendererRenderMixin
{
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void onRender(LivingEntityRenderState state, MatrixStack matrixStack, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo info)
    {
        if ((Object) this instanceof PlayerEntityRenderer)
        {
            if (state instanceof PlayerEntityRenderState playerState)
            {
                Entity entity = ((IEntityRenderState) state).bbs$getEntity();

                if (entity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity)
                {
                    /* TODO(1.21.11 render): MorphRenderer.renderPlayer still expects VertexConsumerProvider and int light.
                     * This needs re-porting to work with OrderedRenderCommandQueue + CameraRenderState. */
                }
            }
        }
    }
}
