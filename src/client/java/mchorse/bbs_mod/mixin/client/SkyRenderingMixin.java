package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.SunPathRotation;

import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.level.MoonPhase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.commands.RenderPass;

import org.joml.Vector4fc;

@Mixin(SkyRenderer.class)
public class SkyRenderingMixin
{
    @Inject(method = "renderSunMoonAndStars", at = @At("HEAD"))
    private void bbs$applySunPathToCelestialBodies(RenderPass pass, PoseStack matrices, float skyAngle, float starBrightness, float rainGradient, MoonPhase moonPhase, float alpha, float skyDarkness, CallbackInfo info)
    {
        SunPathRotation.applyY(matrices.last().pose());
    }

    @Inject(method = "renderSunriseAndSunset", at = @At("HEAD"))
    private void bbs$applySunPathToGlowingSky(RenderPass pass, PoseStack matrices, float skyAngle, Vector4fc color, CallbackInfo info)
    {
        matrices.pushPose();
        SunPathRotation.applyY(matrices.last().pose());
    }

    @Inject(method = "renderSunriseAndSunset", at = @At("RETURN"))
    private void bbs$popSunPathFromGlowingSky(RenderPass pass, PoseStack matrices, float skyAngle, Vector4fc color, CallbackInfo info)
    {
        matrices.popPose();
    }
}
