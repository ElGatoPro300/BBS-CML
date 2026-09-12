package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;

import org.lwjgl.opengl.GL11;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class WorldRendererMixin
{
    @Shadow
    private LevelTargetBundle targets;

    @Inject(method = "addSkyPass", at = @At("HEAD"), cancellable = true, require = 0)
    public void onRenderSky(FrameGraphBuilder frameGraphBuilder, CameraRenderState camera, GpuBufferSlice fogBuffer, CallbackInfo info)
    {
        if (BBSRendering.isChromaSkyEnabled())
        {
            FramePass pass = frameGraphBuilder.addPass("sky");

            this.targets.main = pass.readsAndWrites(this.targets.main);
            pass.executes(() -> {
                Color color = Color.rgb(BBSRendering.getChromaSkyColor());

                GL11.glClearColor(color.r, color.g, color.b, 1F);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            });

            info.cancel();
        }
    }

    @Inject(method = "addCloudsPass", at = @At("HEAD"), cancellable = true, require = 0)
    public void onRenderClouds(FrameGraphBuilder frameGraphBuilder, CloudStatus cloudRenderMode, Vec3 cameraPos, long tick, float tickDelta, int color, float cloudHeight, int cloudDistance, CallbackInfo info)
    {
        if (BBSRendering.isChromaSkyEnabled() && !BBSRendering.isChromaSkyClouds())
        {
            info.cancel();
        }
    }

    @Inject(method = "addWeatherPass", at = @At("HEAD"), cancellable = true, require = 0)
    public void onRenderWeather(FrameGraphBuilder frameGraphBuilder, GpuBufferSlice fogBuffer, CallbackInfo info)
    {
        if (BBSRendering.shouldHideChromaTerrain())
        {
            info.cancel();
        }
    }

    @Inject(at = @At("RETURN"), method = "initOutline")
    private void onLoadEntityOutlineShader(CallbackInfo info)
    {
        BBSRendering.resizeExtraFramebuffers();
    }

    @Inject(at = @At("RETURN"), method = "resize")
    private void onResized(int width, int height, CallbackInfo info)
    {
        BBSRendering.resizeExtraFramebuffers();
    }
}
