package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.SunPathRotation;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import org.lwjgl.opengl.GL11;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/* Fog class removed in 1.21.11 */





@Mixin(LevelRenderer.class)
public class WorldRendererMixin
{
/*
    @Shadow
    public Framebuffer entityOutlinesFramebuffer;
*/

    /* renderSky injectors disabled — Fog class removed in 1.21.11 require = 0 keeps them inert */

    /* TODO(1.21.11 render): WorldRenderer#renderLayer was removed by the FrameGraphBuilder/
     * OrderedRenderCommandQueue terrain rewrite (per-RenderLayer submission is now handled through
     * renderBlockLayers/SectionRenderState with no simple cancellation point). require = 0 keeps this
     * injector inert instead of crashing until the chroma-sky-terrain occlusion is re-ported. */
    @Inject(method = "renderLayer", at = @At("HEAD"), cancellable = true, require = 0)
    public void onRenderLayer(RenderType renderLayer, double cameraX, double cameraY, double cameraZ, Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo info)
    {
        if (BBSRendering.shouldHideChromaTerrain())
        {
            BBSRendering.onRenderChunkLayer(positionMatrix, projectionMatrix);

            info.cancel();
        }
    }

    @Inject(method = "renderLayer", at = @At("TAIL"), require = 0)
    public void onRenderChunkLayer(RenderType layer, double cameraX, double cameraY, double cameraZ, Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo info)
    {
        /* TODO 1.21.11: RenderLayer.getSolid() removed — re-port later */
        if (false)
        {
            BBSRendering.onRenderChunkLayer(positionMatrix, projectionMatrix);
        }
    }

    @Inject(method = "prepareCullFrustum", at = @At("HEAD"))
    public void onSetupFrustum(Matrix4f posMatrix, Matrix4f projMatrix, Vec3 pos, CallbackInfoReturnable<Frustum> info)
    {
        BBSRendering.camera.set(posMatrix);
    }

    @Inject(at = @At("RETURN"), method = "initOutline")
    private void onLoadEntityOutlineShader(CallbackInfo info)
    {
        BBSRendering.resizeExtraFramebuffers();
    }

    @Inject(at = @At("RETURN"), method = "resize")
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
    @Inject(method = "checkPoseStack", at = @At("HEAD"), cancellable = true, require = 0)
    private void onCheckEmpty(PoseStack matrices, CallbackInfo info)
    {
        if (matrices != null)
        {
            while (!matrices.isEmpty())
            {
                matrices.popPose();
            }
        }

        info.cancel();
    }
}
