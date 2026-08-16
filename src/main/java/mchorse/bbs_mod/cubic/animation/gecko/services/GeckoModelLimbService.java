package mchorse.bbs_mod.cubic.animation.gecko.services;

import mchorse.bbs_mod.cubic.animation.gecko.config.GeckoLimbAnimationConfig;
import mchorse.bbs_mod.cubic.animation.gecko.model.GeckoAnimationContext;
import mchorse.bbs_mod.cubic.animation.gecko.model.GeckoAnimationState;
import mchorse.bbs_mod.cubic.animation.gecko.routes.GeckoLimbRole;
import mchorse.bbs_mod.cubic.animation.gecko.utils.GeckoAnimationMath;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.utils.MathUtils;

import java.util.Map;
import java.util.WeakHashMap;

public class GeckoModelLimbService
{
    private static final float TWO_PI = (float) (Math.PI * 2D);
    private final WeakHashMap<ModelGroup, WheelState> wheelStates = new WeakHashMap<>();

    public void apply(ModelGroup group, GeckoLimbRole role, GeckoLimbAnimationConfig config, GeckoAnimationContext context, Map<GeckoAnimationState, Float> weights)
    {
        if (context.riding && role != GeckoLimbRole.HEAD)
        {
            return;
        }

        float direction = config.invert ? -1F : 1F;
        float idleWeight = this.weight(weights, GeckoAnimationState.IDLE);
        float walkWeight = this.weight(weights, GeckoAnimationState.WALK);
        float runWeight = this.weight(weights, GeckoAnimationState.RUN);
        float attackWeight = this.weight(weights, GeckoAnimationState.ATTACK);
        float moveWeight = Math.max(Math.max(walkWeight, runWeight), this.weight(weights, GeckoAnimationState.SWIM));

        if (config.lookX && role == GeckoLimbRole.HEAD)
        {
            group.current.rotate.x += -context.pitch * direction;
        }

        if (config.lookY)
        {
            group.current.rotate.y += -context.yaw * direction;
        }

        if (config.idle && idleWeight > 0F)
        {
            float armDirection = role == GeckoLimbRole.LEFT_ARM ? -1F : 1F;
            float idleFactor = config.invert ? -1F : 1F;

            group.current.rotate.z += MathUtils.toDeg(GeckoAnimationMath.idleRoll(context.age, armDirection) * idleFactor * idleWeight);
            group.current.rotate.x += MathUtils.toDeg(GeckoAnimationMath.idlePitch(context.age, armDirection) * idleFactor * idleWeight);
        }

        if (config.swiping && attackWeight > 0F && context.handSwing > 0F)
        {
            float progress = config.invert ? 1F - context.handSwing : context.handSwing;

            group.current.rotate.x += MathUtils.toDeg(GeckoAnimationMath.swipePitch(progress) * direction * attackWeight);
            group.current.rotate.z += MathUtils.toDeg(GeckoAnimationMath.swipeRoll(progress) * direction * attackWeight);
        }

        if (config.swinging && moveWeight > 0F && (role == GeckoLimbRole.RIGHT_ARM || role == GeckoLimbRole.LEFT_ARM || role == GeckoLimbRole.RIGHT_LEG || role == GeckoLimbRole.LEFT_LEG))
        {
            boolean left = role == GeckoLimbRole.LEFT_ARM || role == GeckoLimbRole.LEFT_LEG;
            float swing = GeckoAnimationMath.swingPitch(context.limbPhase, context.limbSpeed, context.movementCoefficient, left) * direction;

            group.current.rotate.x += MathUtils.toDeg(swing * moveWeight);
        }

        if (config.wheel && (context.preview || this.weight(weights, GeckoAnimationState.WHEEL) > 0F))
        {
            WheelState state = this.wheelStates.computeIfAbsent(group, key -> new WheelState());
            state.direction = GeckoAnimationMath.wheelDirection(context.forwardSpeed, state.direction);
            float linearSpeed = GeckoAnimationMath.wheelLinearSpeed(context.forwardSpeed, context.horizontalSpeed, state.direction);

            if (context.preview && Math.abs(linearSpeed) < 0.01F)
            {
                linearSpeed = 1.2F * state.direction;
            }

            if (context.preview)
            {
                linearSpeed *= context.previewWheelSpeed;
            }

            float targetVelocity = GeckoAnimationMath.wheelTargetAngularSpeed(linearSpeed, config.wheelSpeed) * direction;
            float lerpFactor = context.preview ? 0.45F : GeckoAnimationMath.wheelLerpFactor(context.horizontalSpeed);
            float angularVelocity = GeckoAnimationMath.wheelRotation(state.angularVelocity, targetVelocity, lerpFactor);
            state.angularVelocity = angularVelocity;
            state.accumulatedAngle += angularVelocity;

            if (state.accumulatedAngle > TWO_PI || state.accumulatedAngle < -TWO_PI)
            {
                state.accumulatedAngle %= TWO_PI;
            }

            float rotation = MathUtils.toDeg(state.accumulatedAngle);

            if ("y".equals(config.wheelAxis))
            {
                group.current.rotate.y += rotation;
            }
            else if ("z".equals(config.wheelAxis))
            {
                group.current.rotate.z += rotation;
            }
            else
            {
                group.current.rotate.x += rotation;
            }

        }
        else
        {
            this.wheelStates.remove(group);
        }
    }

    private float weight(Map<GeckoAnimationState, Float> weights, GeckoAnimationState state)
    {
        return weights.getOrDefault(state, 0F);
    }

    private static class WheelState
    {
        private float angularVelocity;
        private float accumulatedAngle;
        private float direction = 1F;
    }
}
