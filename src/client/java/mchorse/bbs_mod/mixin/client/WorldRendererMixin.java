package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.SunPathRotation;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.FramePass;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin
{
    @Shadow private DefaultFramebufferSet framebufferSet;

    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true, require = 0)
    public void onRenderSky(FrameGraphBuilder frameGraphBuilder, Camera camera, GpuBufferSlice fogBuffer, CallbackInfo info)
    {
        if (BBSRendering.isChromaSkyEnabled())
        {
            FramePass pass = frameGraphBuilder.createPass("sky");

            this.framebufferSet.mainFramebuffer = pass.transfer(this.framebufferSet.mainFramebuffer);
            pass.setRenderer(() -> {
                Color color = Color.rgb(BBSRendering.getChromaSkyColor());
                int colorInt = color.getRGBAColor();
                Framebuffer mainFb = MinecraftClient.getInstance().getFramebuffer();
                if (mainFb != null && mainFb.getColorAttachment() != null)
                {
                    RenderSystem.getDevice().createCommandEncoder().clearColorTexture(mainFb.getColorAttachment(), colorInt);
                }
            });

            info.cancel();
        }
    }

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true, require = 0)
    public void onRenderClouds(FrameGraphBuilder frameGraphBuilder, CloudRenderMode cloudRenderMode, Vec3d cameraPos, long ticks, float tickDelta, int color, float cloudHeight, CallbackInfo info)
    {
        if (BBSRendering.isChromaSkyEnabled() && !BBSRendering.isChromaSkyClouds())
        {
            info.cancel();
        }
    }

    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true, require = 0)
    public void onRenderWeather(FrameGraphBuilder frameGraphBuilder, GpuBufferSlice fogBuffer, CallbackInfo info)
    {
        if (BBSRendering.shouldHideChromaTerrain())
        {
            info.cancel();
        }
    }

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
    private void onResized(int width, int height, CallbackInfo info)
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
