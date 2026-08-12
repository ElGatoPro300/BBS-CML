package mchorse.bbs_mod.mixin.client.iris;

import mchorse.bbs_mod.client.SunPathRotation;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

import net.irisshaders.iris.shadows.ShadowMatrices;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bakes BBS sun-path yaw into Iris {@code shadowModelView}.
 * <p>
 * Cascade / frustum fitting uses this matrix, so the yaw must live here —
 * warping positions only in GLSL leaves long shadows clipped at some camera angles.
 * Uses {@link SunPathRotation#getLightYawDegrees()} so {@code M * R_y(lightYaw)} matches
 * the old GLSL world-position warp (correct cast direction). Celestial / frustum light
 * uses sky yaw so culling stays aligned with that light.
 */
@Mixin(value = ShadowMatrices.class, remap = false)
public class ShadowMatricesMixin
{
    @Inject(method = "createBaselineModelViewMatrix", at = @At("RETURN"), require = 0)
    private static void bbs$yawShadowBaseline(MatrixStack target, float shadowAngle, float sunPathRotation, CallbackInfo ci)
    {
        float degrees = SunPathRotation.getLightYawDegrees();

        if (degrees == 0F || target == null)
        {
            return;
        }

        target.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(degrees));
    }
}
