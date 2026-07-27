package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.camera.controller.CameraController;
import mchorse.bbs_mod.camera.controller.ICameraController;
import mchorse.bbs_mod.camera.controller.PlayCameraController;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.film.Films;
import mchorse.bbs_mod.items.GunZoom;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_4587;
import net.minecraft.class_757;
import net.minecraft.class_7833;
import net.minecraft.class_9779;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_757.class)
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
    public void onBob(class_4587 matrices, float tickDelta, CallbackInfo ci)
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

    private void bbs$applyReplayFirstPersonBobbing(class_4587 matrices, float tickDelta, Films.FirstPersonBobbingSample sample)
    {
        class_310 mc = class_310.method_1551();

        if (mc.field_1687 == null)
        {
            return;
        }

        long worldTick = mc.field_1687.method_8510();

        if (this.bbs$lastFpBobbingTick != worldTick)
        {
            this.bbs$lastFpBobbingTick = worldTick;
            this.bbs$fpBobPrevPhase = this.bbs$fpBobPhase;
            this.bbs$fpBobPrevStride = this.bbs$fpBobStride;

            if (!sample.paused)
            {
                float movement = sample.grounded ? class_3532.method_15355(sample.vX * sample.vX + sample.vZ * sample.vZ) * 4F : 0F;
                float frequency = BBSSettings.replayFpBobbingFrequency == null ? 1F : class_3532.method_15363(BBSSettings.replayFpBobbingFrequency.get(), 0F, 3F);

                movement = Math.min(1F, movement);
                this.bbs$fpBobStride += (movement - this.bbs$fpBobStride) * 0.4F;
                this.bbs$fpBobPhase += this.bbs$fpBobStride * frequency;
            }
        }

        float phase = class_3532.method_16439(tickDelta, this.bbs$fpBobPrevPhase, this.bbs$fpBobPhase);
        float intensity = BBSSettings.replayFpBobbingIntensity == null ? 1F : class_3532.method_15363(BBSSettings.replayFpBobbingIntensity.get(), 0F, 2F);
        float stride = class_3532.method_16439(tickDelta, this.bbs$fpBobPrevStride, this.bbs$fpBobStride) * intensity;

        matrices.method_46416(class_3532.method_15374(phase * (float) Math.PI) * stride * 0.5F, -Math.abs(class_3532.method_15362(phase * (float) Math.PI) * stride), 0F);
        matrices.method_22907(class_7833.field_40718.rotationDegrees(class_3532.method_15374(phase * (float) Math.PI) * stride * 3F));
        matrices.method_22907(class_7833.field_40714.rotationDegrees(Math.abs(class_3532.method_15362(phase * (float) Math.PI - 0.2F) * stride) * 5F));
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
    public void onGetFov(CallbackInfoReturnable<Double> info)
    {
        GunZoom gunZoom = BBSModClient.getGunZoom();

        if (gunZoom != null)
        {
            info.setReturnValue((double) gunZoom.getFOV(info.getReturnValue().floatValue()));

            return;
        }

        CameraController controller = BBSModClient.getCameraController();

        if (controller.getCurrent() != null && !BBSRendering.isIrisShadowPass())
        {
            info.setReturnValue(controller.getFOV());
        }
    }

    /**
     * This injection replaces the camera roll when camera controller takes over
     */
    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    public void onTiltViewWhenHurt(class_4587 matrices, float tickDelta, CallbackInfo info)
    {
        CameraController controller = BBSModClient.getCameraController();

        if (controller.getCurrent() != null && !BBSRendering.isIrisShadowPass())
        {
            matrices.method_22907(class_7833.field_40718.rotationDegrees(controller.getRoll()));

            info.cancel();
        }
    }

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    public void onRenderHand(CallbackInfo info)
    {
        ICameraController current = BBSModClient.getCameraController().getCurrent();

        if (current instanceof PlayCameraController)
        {
            info.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "renderWorld")
    private void onWorldRenderBegin(CallbackInfo callbackInfo)
    {
        BBSRendering.onWorldRenderBegin();
    }

    @Inject(at = @At("RETURN"), method = "renderWorld")
    private void onWorldRenderEnd(CallbackInfo callbackInfo)
    {
        BBSRendering.onWorldRenderEnd();
    }

    @Inject(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;hudHidden:Z", opcode = Opcodes.GETFIELD, ordinal = 0))
    private void onBeforeHudRendering(class_9779 tickCounter, boolean tick, CallbackInfo info)
    {
        ICameraController current = BBSModClient.getCameraController().getCurrent();

        if (class_310.method_1551().field_1690.field_1842 && current == null)
        {
            BBSRendering.onRenderBeforeScreen();
        }
    }
}
