package mchorse.bbs_mod.cubic.animation.gecko.model;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.class_3532;

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
        float clamped = class_3532.method_15363(factor, 0F, 1F);

        for (GeckoAnimationState state : GeckoAnimationState.values())
        {
            float current = this.weights.getOrDefault(state, 0F);
            float target = class_3532.method_15363(targets.getOrDefault(state, 0F), 0F, 1F);
            float value = current + (target - current) * clamped;

            this.weights.put(state, class_3532.method_15363(value, 0F, 1F));
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
