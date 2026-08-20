package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.SunPathRotation;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import org.joml.Matrix4f;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin
{

    /* TODO(1.21.11 render): WorldRenderer#renderLayer was removed by the FrameGraphBuilder/
     * OrderedRenderCommandQueue terrain rewrite (per-RenderLayer submission is now handled through
     * renderBlockLayers/SectionRenderState with no simple cancellation point). require = 0 keeps this
     * injector inert instead of crashing until the chroma-sky-terrain occlusion is re-ported. */
    @Inject(method = "renderLayer", at = @At("HEAD"), cancellable = true, require = 0)
    public void onRenderLayer(RenderLayer renderLayer, double cameraX, double cameraY, double cameraZ, Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo info)
    {
        if (BBSRendering.shouldHideChromaTerrain())
        {
            BBSRendering.onRenderChunkLayer(positionMatrix, projectionMatrix);

            info.cancel();
        }
    }

    @Inject(method = "renderLayer", at = @At("TAIL"), require = 0)
    public void onRenderChunkLayer(RenderLayer layer, double cameraX, double cameraY, double cameraZ, Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo info)
    {
        /* TODO 1.21.11: RenderLayer.getSolid() removed — re-port later */
        if (false)
        {
            BBSRendering.onRenderChunkLayer(positionMatrix, projectionMatrix);
        }
    }

    @Inject(method = "setupFrustum", at = @At("HEAD"))
    public void onSetupFrustum(Matrix4f posMatrix, Matrix4f projMatrix, Vec3d pos, CallbackInfoReturnable<Frustum> info)
    {
        BBSRendering.camera.set(posMatrix);
    }

    @Inject(at = @At("RETURN"), method = "loadEntityOutlinePostProcessor")
    private void onLoadEntityOutlineShader(CallbackInfo info)
    {
        BBSRendering.resizeExtraFramebuffers();
    }

    @Inject(at = @At("RETURN"), method = "onResized")
    private void onResized(CallbackInfo info)
    {
        /*
        if (this.entityOutlinesFramebuffer == null)
        {
            return;
        }
        */

        BBSRendering.resizeExtraFramebuffers();
    }
    /* 1.21.11 MatrixStack keeps a reusable List plus stackDepth; size() is not the
     * logical depth. isEmpty() is true iff only the identity entry remains. */
    @Inject(method = "checkEmpty", at = @At("HEAD"), cancellable = true, require = 0)
    private void onCheckEmpty(MatrixStack matrices, CallbackInfo info)
    {
        if (matrices != null)
        {
            while (!matrices.isEmpty())
            {
                matrices.pop();
            }
        }

        info.cancel();
    }
}
