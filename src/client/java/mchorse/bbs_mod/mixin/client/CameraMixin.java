package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.controller.CameraController;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.items.GunZoom;

import net.minecraft.client.Camera;

import org.joml.Vector3d;

import com.mojang.blaze3d.platform.Window;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin
{
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void setPosition(double x, double y, double z);

    /* Camera projection and culling are prepared before renderLevel in 26.1,
     * while Window must still report the desktop size to GUI extraction. */
    @Redirect(method = {"update", "createProjectionMatrixForCulling"},
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getWidth()I"))
    private int bbs$cameraWidth(Window window)
    {
        return BBSRendering.isCustomSize() ? Math.max(2, BBSRendering.getVideoWidth()) : window.getWidth();
    }

    @Redirect(method = {"update", "createProjectionMatrixForCulling"},
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getHeight()I"))
    private int bbs$cameraHeight(Window window)
    {
        return BBSRendering.isCustomSize() ? Math.max(2, BBSRendering.getVideoHeight()) : window.getHeight();
    }

    @Inject(method = "alignWithEntity", at = @At("RETURN"))
    private void onAlignWithEntity(float tickDelta, CallbackInfo ci)
    {
        CameraController controller = BBSModClient.getCameraController();

        controller.setup(controller.camera, tickDelta);

        if (controller.getCurrent() != null)
        {
            Vector3d position = controller.getPosition();
            float yaw = controller.getYaw();
            float pitch = controller.getPitch();

            this.setPosition(position.x, position.y, position.z);
            this.setRotation(yaw, pitch);
        }
    }

    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void onCalculateFov(float tickDelta, CallbackInfoReturnable<Float> info)
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
}
