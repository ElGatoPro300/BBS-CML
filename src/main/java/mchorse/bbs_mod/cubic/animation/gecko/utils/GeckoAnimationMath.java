package mchorse.bbs_mod.cubic.animation.gecko.utils;

import net.minecraft.util.math.MathHelper;

public class GeckoAnimationMath
{
    private static final float WALK_SWING_FREQUENCY = 0.6662F;
    private static final float WHEEL_RADIUS_BLOCKS = 0.35F;
    private static final float WHEEL_ROTATION_BASE_MULTIPLIER = 0.4F;
    private static final float WHEEL_SPEED_EPSILON = 0.01F;

    public static float idleRoll(float age, float direction)
    {
        return (MathHelper.cos(-age * 0.09F) * 0.05F + 0.05F) * direction;
    }

    public static float idlePitch(float age, float direction)
    {
        return MathHelper.sin(-age * 0.067F) * 0.05F * direction;
    }

    public static float swingPitch(float limbPhase, float limbSpeed, float movementCoefficient, boolean left)
    {
        float offset = left ? (float) Math.PI : 0F;

        return MathHelper.cos(limbPhase * WALK_SWING_FREQUENCY + offset) * limbSpeed / Math.max(0.001F, movementCoefficient);
    }

    public static float wheelTargetAngularSpeed(float forwardSpeedPerSecond, float speedMultiplier)
    {
        float angularSpeedPerSecond = forwardSpeedPerSecond / WHEEL_RADIUS_BLOCKS;

        return angularSpeedPerSecond * speedMultiplier * WHEEL_ROTATION_BASE_MULTIPLIER / 20F;
    }

    public static float wheelDirection(float forwardSpeedPerSecond, float fallbackDirection)
    {
        if (Math.abs(forwardSpeedPerSecond) > WHEEL_SPEED_EPSILON)
        {
            return Math.signum(forwardSpeedPerSecond);
        }

        if (Math.abs(fallbackDirection) > WHEEL_SPEED_EPSILON)
        {
            return Math.signum(fallbackDirection);
        }

        return 1F;
    }

    public static float wheelLinearSpeed(float forwardSpeedPerSecond, float horizontalSpeedPerSecond, float fallbackDirection)
    {
        if (Math.abs(forwardSpeedPerSecond) > WHEEL_SPEED_EPSILON)
        {
            return forwardSpeedPerSecond;
        }

        if (Math.abs(horizontalSpeedPerSecond) > WHEEL_SPEED_EPSILON)
        {
            return Math.abs(horizontalSpeedPerSecond) * wheelDirection(forwardSpeedPerSecond, fallbackDirection);
        }

        return 0F;
    }

    public static float wheelLerpFactor(float horizontalSpeedPerSecond)
    {
        float normalized = Math.min(1F, Math.abs(horizontalSpeedPerSecond) / 4.317F);

        return 0.15F + normalized * 0.35F;
    }

    public static float wheelRotation(float previousAngularSpeed, float targetAngularSpeed, float lerpFactor)
    {
        float factor = MathHelper.clamp(lerpFactor, 0F, 1F);
        float value = previousAngularSpeed + (targetAngularSpeed - previousAngularSpeed) * factor;

        return Math.abs(value) < 0.0001F && Math.abs(targetAngularSpeed) < 0.0001F ? 0F : value;
    }

    public static float swipePitch(float handSwing)
    {
        float factor = 1F - handSwing;
        factor *= factor;
        factor *= factor;
        factor = 1F - factor;

        return MathHelper.sin(factor * (float) Math.PI) * 1.2F;
    }

    public static float swipeRoll(float handSwing)
    {
        return MathHelper.sin(handSwing * (float) Math.PI) * -0.4F;
    }
}
