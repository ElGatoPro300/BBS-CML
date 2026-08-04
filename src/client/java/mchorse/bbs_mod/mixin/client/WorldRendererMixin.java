package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.SunPathRotation;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;

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
/*
    @Shadow
    public Framebuffer entityOutlinesFramebuffer;
*/

    @Inject(method = "renderSky(Lnet/minecraft/client/render/FrameGraphBuilder;Lnet/minecraft/client/render/Camera;FLnet/minecraft/client/render/Fog;)V", at = @At("HEAD"), cancellable = true, require = 0)
    public void onRenderSky(FrameGraphBuilder frameGraphBuilder, Camera camera, float tickDelta, Fog fog, CallbackInfo info)
    {
        if (BBSRendering.isChromaSkyEnabled())
        {
            Color color = Color.rgb(BBSRendering.getChromaSkyColor());

            GL11.glClearColor(color.r, color.g, color.b, 1F);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

            info.cancel();

            return;
        }

        SunPathRotation.begin(new Matrix4f());
    }

    @Inject(method = "renderSky(Lnet/minecraft/client/render/FrameGraphBuilder;Lnet/minecraft/client/render/Camera;FLnet/minecraft/client/render/Fog;)V", at = @At("RETURN"), require = 0)
    public void onRenderSkyReturn(FrameGraphBuilder frameGraphBuilder, Camera camera, float tickDelta, Fog fog, CallbackInfo info)
    {
        SunPathRotation.end(new Matrix4f());
    }

    @Inject(method = "renderLayer", at = @At("HEAD"), cancellable = true)
    public void onRenderLayer(RenderLayer renderLayer, double cameraX, double cameraY, double cameraZ, Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo info)
    {
        if (BBSRendering.shouldHideChromaTerrain())
        {
            BBSRendering.onRenderChunkLayer(positionMatrix, projectionMatrix);

            info.cancel();
        }
    }

    @Inject(method = "renderLayer", at = @At("TAIL"))
    public void onRenderChunkLayer(RenderLayer layer, double cameraX, double cameraY, double cameraZ, Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo info)
    {
        if (layer == RenderLayer.getSolid())
        {
            BBSRendering.onRenderChunkLayer(positionMatrix, projectionMatrix);
        }
    }

    @Inject(method = "setupFrustum", at = @At("HEAD"))
    public void onSetupFrustum(Vec3d vec3d, Matrix4f matrix4f, Matrix4f positionMatrix, CallbackInfo info)
    {
        BBSRendering.camera.set(matrix4f);
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
}
