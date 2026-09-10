package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.camera.controller.CameraController;
import mchorse.bbs_mod.camera.controller.ICameraController;
import mchorse.bbs_mod.camera.controller.PlayCameraController;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.film.Films;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;

import org.joml.Matrix4fc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    public void onBob(CameraRenderState cameraRenderState, PoseStack matrices, CallbackInfo ci)
    {
        Minecraft mc = Minecraft.getInstance();
        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
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
     * Replaces vanilla camera roll with the active BBS film/editor roll, while still
     * applying vanilla hurt/death tilt from the camera entity.
     */
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    public void onTiltViewWhenHurt(CameraRenderState cameraRenderState, PoseStack matrices, CallbackInfo info)
    {
        CameraController controller = BBSModClient.getCameraController();

        if (controller.getCurrent() == null || BBSRendering.isIrisShadowPass())
        {
            return;
        }

        matrices.mulPose(Axis.ZP.rotationDegrees(controller.getRoll()));

        CameraEntityRenderState entityRenderState = cameraRenderState.entityRenderState;

        if (entityRenderState != null && entityRenderState.isLiving)
        {
            float f = entityRenderState.hurtTime;

            if (entityRenderState.isDeadOrDying)
            {
                float deathTilt = Math.min(entityRenderState.deathTime, 20.0F);

                matrices.mulPose(Axis.ZP.rotationDegrees(40.0F - 8000.0F / (deathTilt + 200.0F)));
            }

            if (f >= 0.0F && entityRenderState.hurtDuration > 0)
            {
                f /= (float) entityRenderState.hurtDuration;
                f = Mth.sin(f * f * f * f * (float) Math.PI);

                float tiltYaw = entityRenderState.hurtDir;
                Minecraft client = Minecraft.getInstance();
                float strength = (float) (-f * 14.0 * client.options.damageTiltStrength().get());

                matrices.mulPose(Axis.YP.rotationDegrees(-tiltYaw));
                matrices.mulPose(Axis.ZP.rotationDegrees(strength));
                matrices.mulPose(Axis.YP.rotationDegrees(tiltYaw));
            }
        }

        info.cancel();
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    public void onRenderHand(CameraRenderState cameraRenderState, float tickDelta, Matrix4fc positionMatrix, CallbackInfo info)
    {
        ICameraController current = BBSModClient.getCameraController().getCurrent();
        UIBaseMenu menu = UIScreen.getCurrentMenu();

        if (current instanceof PlayCameraController || (menu != null && menu.canHideHUD()))
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
     * AAA Particles pastes a cleared depth buffer and draws Effekseer.
     */
    @Inject(
        method = "renderLevel",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lnet/minecraft/client/renderer/state/level/CameraRenderState;FLorg/joml/Matrix4fc;)V"),
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

    /**
     * Pause / screen background blur runs after the world pass. World model-block forms can
     * leave ColorModulator, TU0, or blend (DST_COLOR) dirty — blur then presents a black world
     * while menu buttons still look fine.
     */
    @Inject(method = "processBlurEffect", at = @At("HEAD"))
    private void bbsPrepareMenuBlurState(CallbackInfo callbackInfo)
    {
        BBSRendering.prepareMenuBackgroundState();
    }

    @Inject(method = "extractGui", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"), require = 0)
    private void onBeforeHudRendering(DeltaTracker tickCounter, boolean tick, boolean isPaused, CallbackInfo info)
    {
        ICameraController current = BBSModClient.getCameraController().getCurrent();

        if (Minecraft.getInstance().options.hideGui && current == null)
        {
            BBSRendering.onRenderBeforeScreen();
        }
    }
}
