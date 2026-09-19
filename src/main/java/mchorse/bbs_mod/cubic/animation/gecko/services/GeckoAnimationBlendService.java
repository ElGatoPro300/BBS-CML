package mchorse.bbs_mod.cubic.animation.gecko.services;

import mchorse.bbs_mod.cubic.animation.gecko.model.GeckoAnimationContext;
import mchorse.bbs_mod.cubic.animation.gecko.model.GeckoAnimationState;

import java.util.EnumMap;
import java.util.Map;

public class GeckoAnimationBlendService
{
    public Map<GeckoAnimationState, Float> resolveTargets(GeckoAnimationContext context)
    {
        EnumMap<GeckoAnimationState, Float> targets = new EnumMap<>(GeckoAnimationState.class);

        for (GeckoAnimationState state : GeckoAnimationState.values())
        {
            targets.put(state, 0F);
        }

        float horizontalSpeed = Math.abs(context.horizontalSpeed);
        boolean moving = horizontalSpeed > 0.08F;
        boolean running = context.sprinting && horizontalSpeed > 2.4F;
        boolean attacking = context.handSwing > 0.02F;
        boolean jumping = !context.onGround && context.verticalSpeed > 0.02F;
        boolean falling = !context.onGround && context.verticalSpeed < -0.02F;

        if (context.riding)
        {
            targets.put(GeckoAnimationState.IDLE, 1F);

            if (attacking)
            {
                targets.put(GeckoAnimationState.ATTACK, 1F);
            }

            return targets;
        }

        if (context.swimming)
        {
            targets.put(GeckoAnimationState.SWIM, 1F);
        }
        else if (context.fallFlying || context.usingRiptide)
        {
            targets.put(GeckoAnimationState.FLY, 1F);
        }
        else
        {
            if (running)
            {
                targets.put(GeckoAnimationState.RUN, 1F);
            }
            else if (moving)
            {
                targets.put(GeckoAnimationState.WALK, 1F);
            }
            else
            {
                targets.put(GeckoAnimationState.IDLE, 1F);
            }

            if (jumping)
            {
                targets.put(GeckoAnimationState.JUMP, 1F);
            }
            else if (falling)
            {
                targets.put(GeckoAnimationState.FALL, 1F);
            }
        }

        if (attacking)
        {
            targets.put(GeckoAnimationState.ATTACK, 1F);
        }

        if (moving)
        {
            targets.put(GeckoAnimationState.WHEEL, 1F);
        }

        return targets;
    }
}
