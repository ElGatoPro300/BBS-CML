package mchorse.bbs_mod.cubic.animation.gecko.model;

import net.minecraft.util.math.MathHelper;

import java.util.EnumMap;
import java.util.Map;

public class GeckoStateBlend
{
    private final EnumMap<GeckoAnimationState, Float> weights = new EnumMap<>(GeckoAnimationState.class);

    public GeckoStateBlend()
    {
        for (GeckoAnimationState state : GeckoAnimationState.values())
        {
            this.weights.put(state, 0F);
        }
    }

    public void blendTo(Map<GeckoAnimationState, Float> targets, float factor)
    {
        float clamped = MathHelper.clamp(factor, 0F, 1F);

        for (GeckoAnimationState state : GeckoAnimationState.values())
        {
            float current = this.weights.getOrDefault(state, 0F);
            float target = MathHelper.clamp(targets.getOrDefault(state, 0F), 0F, 1F);
            float value = current + (target - current) * clamped;

            this.weights.put(state, MathHelper.clamp(value, 0F, 1F));
        }
    }

    public float get(GeckoAnimationState state)
    {
        return this.weights.getOrDefault(state, 0F);
    }

    public Map<GeckoAnimationState, Float> snapshot()
    {
        return new EnumMap<>(this.weights);
    }
}
