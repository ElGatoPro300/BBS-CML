package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.controller.CameraController;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import org.joml.Vector3d;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin
{
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "setup", at = @At(value = "RETURN"))
    public void onUpdate(Level area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci)
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
}
