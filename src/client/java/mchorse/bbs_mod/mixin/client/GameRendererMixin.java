package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.camera.controller.CameraController;
import mchorse.bbs_mod.camera.controller.ICameraController;
import mchorse.bbs_mod.camera.controller.PlayCameraController;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.film.Films;
import mchorse.bbs_mod.items.GunZoom;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin
{
    private long bbs$lastFpBobbingTick = Long.MIN_VALUE;
    private float bbs$fpBobPhase;
    private float bbs$fpBobPrevPhase;
    private float bbs$fpBobStride;
    private float bbs$fpBobPrevStride;

    /**
     * This injection cancels bobbing when camera controller takes over
     */
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    public void onBob(PoseStack matrices, float tickDelta, CallbackInfo ci)
    {
        Films.FirstPersonBobbingSample sample = BBSModClient.getFilms().getFirstPersonBobbingSample(tickDelta);

        if (sample != null)
        {
            this.bbs$applyReplayFirstPersonBobbing(matrices, tickDelta, sample);
            ci.cancel();

            return;
        }

        this.bbs$resetReplayFirstPersonBobbing();

        if (BBSModClient.getCameraController().getCurrent() != null)
        {
            ci.cancel();
        }
    }

    private void bbs$applyReplayFirstPersonBobbing(PoseStack matrices, float tickDelta, Films.FirstPersonBobbingSample sample)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null)
        {
            return;
        }

        long worldTick = mc.level.getGameTime();

        if (this.bbs$lastFpBobbingTick != worldTick)
        {
            this.bbs$lastFpBobbingTick = worldTick;
            this.bbs$fpBobPrevPhase = this.bbs$fpBobPhase;
            this.bbs$fpBobPrevStride = this.bbs$fpBobStride;

            if (!sample.paused)
            {
                float movement = sample.grounded ? Mth.sqrt(sample.vX * sample.vX + sample.vZ * sample.vZ) * 4F : 0F;
                float frequency = BBSSettings.replayFpBobbingFrequency == null ? 1F : Mth.clamp(BBSSettings.replayFpBobbingFrequency.get(), 0F, 3F);

                movement = Math.min(1F, movement);
                this.bbs$fpBobStride += (movement - this.bbs$fpBobStride) * 0.4F;
                this.bbs$fpBobPhase += this.bbs$fpBobStride * frequency;
            }
        }

        float phase = Mth.lerp(tickDelta, this.bbs$fpBobPrevPhase, this.bbs$fpBobPhase);
        float intensity = BBSSettings.replayFpBobbingIntensity == null ? 1F : Mth.clamp(BBSSettings.replayFpBobbingIntensity.get(), 0F, 2F);
        float stride = Mth.lerp(tickDelta, this.bbs$fpBobPrevStride, this.bbs$fpBobStride) * intensity;

        matrices.translate(Mth.sin(phase * (float) Math.PI) * stride * 0.5F, -Math.abs(Mth.cos(phase * (float) Math.PI) * stride), 0F);
        matrices.mulPose(Axis.ZP.rotationDegrees(Mth.sin(phase * (float) Math.PI) * stride * 3F));
        matrices.mulPose(Axis.XP.rotationDegrees(Math.abs(Mth.cos(phase * (float) Math.PI - 0.2F) * stride) * 5F));
    }

    private void bbs$resetReplayFirstPersonBobbing()
    {
        this.bbs$lastFpBobbingTick = Long.MIN_VALUE;
        this.bbs$fpBobPhase = 0F;
        this.bbs$fpBobPrevPhase = 0F;
        this.bbs$fpBobStride = 0F;
        this.bbs$fpBobPrevStride = 0F;
    }

    /**
     * This injection replaces the camera FOV when camera controller takes over
     */
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    public void onGetFov(CallbackInfoReturnable<Float> info)
    {
        GunZoom gunZoom = BBSModClient.getGunZoom();

        if (gunZoom != null)
        {
            info.setReturnValue(gunZoom.getFOV(info.getReturnValue()));

            return;
        }

        CameraController controller = BBSModClient.getCameraController();

        if (controller.getCurrent() != null && !BBSRendering.isIrisShadowPass())
        {
            info.setReturnValue((float) controller.getFOV());
        }
    }

    /**
     * Replaces vanilla camera roll with the active BBS film/editor roll, while still
     * applying vanilla hurt/death tilt from the camera entity. Cancelling the whole
     * method previously removed damage shake during first-person film playback whenever
     * a camera controller (e.g. film editor runner) was active.
     */
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    public void onTiltViewWhenHurt(PoseStack matrices, float tickDelta, CallbackInfo info)
    {
        CameraController controller = BBSModClient.getCameraController();

        if (controller.getCurrent() == null || BBSRendering.isIrisShadowPass())
        {
            return;
        }

        matrices.mulPose(Axis.ZP.rotationDegrees(controller.getRoll()));

        Minecraft client = Minecraft.getInstance();

        if (client.getCameraEntity() instanceof LivingEntity livingEntity)
        {
            float f = livingEntity.hurtTime - tickDelta;

            if (livingEntity.isDeadOrDying())
            {
                float deathTilt = Math.min(livingEntity.deathTime + tickDelta, 20.0F);

                matrices.mulPose(Axis.ZP.rotationDegrees(40.0F - 8000.0F / (deathTilt + 200.0F)));
            }

            if (f >= 0.0F && livingEntity.hurtDuration > 0)
            {
                f /= livingEntity.hurtDuration;
                f = Mth.sin(f * f * f * f * (float) Math.PI);

                float tiltYaw = livingEntity.getHurtDir();
                float strength = (float) (-f * 14.0 * client.options.damageTiltStrength().get());

                matrices.mulPose(Axis.YP.rotationDegrees(-tiltYaw));
                matrices.mulPose(Axis.ZP.rotationDegrees(strength));
                matrices.mulPose(Axis.YP.rotationDegrees(tiltYaw));
            }
        }

        info.cancel();
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    public void onRenderHand(float tickDelta, boolean sleeping, Matrix4f positionMatrix, CallbackInfo info)
    {
        ICameraController current = BBSModClient.getCameraController().getCurrent();

        if (current instanceof PlayCameraController)
        {
            info.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "renderLevel")
    private void onWorldRenderBegin(CallbackInfo callbackInfo)
    {
        BBSRendering.onWorldRenderBegin();
    }

    /**
     * Flush Iris-deferred paint overlays after the world has been composited but before
     * AAA Particles pastes a cleared depth buffer and draws Effekseer (same GETFIELD point
     * as AAA's {@code beforeRenderHand}, earlier {@code order} so we run first).
     */
    @Inject(
        method = "renderLevel",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(FZLorg/joml/Matrix4f;)V"),
        order = 900
    )
    private void bbsFlushPaintOverlaysBeforeHand(CallbackInfo callbackInfo)
    {
        if (!BBSRendering.isIrisShadersEnabled() || !ModelVAORenderer.hasQueuedPaintOverlays())
        {
            return;
        }

        ModelVAORenderer.flushPaintOverlayQueue();
    }

    @Inject(at = @At("RETURN"), method = "renderLevel")
    private void onWorldRenderEnd(CallbackInfo callbackInfo)
    {
        BBSRendering.onWorldRenderEnd();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"), require = 0)
    private void onBeforeHudRendering(DeltaTracker tickCounter, boolean tick, CallbackInfo info)
    {
        ICameraController current = BBSModClient.getCameraController().getCurrent();

        if (Minecraft.getInstance().options.hideGui && current == null)
        {
            BBSRendering.onRenderBeforeScreen();
        }
    }
}
