package mchorse.bbs_mod.cubic.physics;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.interps.AutoBezier;
import mchorse.bbs_mod.utils.interps.IInterp;

/**
 * Animatable per-chain spring scalars, layered over the form's spring-chain
 * config at playback (root/end/pin bone structure stays on the config).
 * Floats interpolate; the active flag steps.
 */
public class SpringDynamicParams implements IMapSerializable
{
    /* Mirrors SpringChainSerializer defaults; duplicated because that config
     * class lives in the client source set and this keyframe value lives in main. */
    public static final float DEFAULT_INFLUENCE = 1F;
    public static final float DEFAULT_PULL_STRENGTH = 1F;
    public static final float DEFAULT_DRAG = 0.15F;
    public static final float DEFAULT_SPRING_RETURN = 0F;

    public static final SpringDynamicParams DEFAULT = new SpringDynamicParams();

    public float influence = DEFAULT_INFLUENCE;
    public float pullStrength = DEFAULT_PULL_STRENGTH;
    public float drag = DEFAULT_DRAG;
    public float springReturn = DEFAULT_SPRING_RETURN;
    public boolean active = true;

    public void identity()
    {
        this.influence = DEFAULT_INFLUENCE;
        this.pullStrength = DEFAULT_PULL_STRENGTH;
        this.drag = DEFAULT_DRAG;
        this.springReturn = DEFAULT_SPRING_RETURN;
        this.active = true;
    }

    public void lerp(SpringDynamicParams preA, SpringDynamicParams a, SpringDynamicParams b, SpringDynamicParams postB, IInterp interp, float x)
    {
        this.influence = (float) interp.interpolate(IInterp.context.set(preA.influence, a.influence, b.influence, postB.influence, x));
        this.pullStrength = (float) interp.interpolate(IInterp.context.set(preA.pullStrength, a.pullStrength, b.pullStrength, postB.pullStrength, x));
        this.drag = (float) interp.interpolate(IInterp.context.set(preA.drag, a.drag, b.drag, postB.drag, x));
        this.springReturn = (float) interp.interpolate(IInterp.context.set(preA.springReturn, a.springReturn, b.springReturn, postB.springReturn, x));
        this.active = a.active;
    }

    public void autoLerp(SpringDynamicParams preA, SpringDynamicParams a, SpringDynamicParams b, SpringDynamicParams postB, float pt, float at, float bt, float qt, boolean clamped, float x)
    {
        this.influence = (float) AutoBezier.get(preA.influence, a.influence, b.influence, postB.influence, pt, at, bt, qt, clamped, x);
        this.pullStrength = (float) AutoBezier.get(preA.pullStrength, a.pullStrength, b.pullStrength, postB.pullStrength, pt, at, bt, qt, clamped, x);
        this.drag = (float) AutoBezier.get(preA.drag, a.drag, b.drag, postB.drag, pt, at, bt, qt, clamped, x);
        this.springReturn = (float) AutoBezier.get(preA.springReturn, a.springReturn, b.springReturn, postB.springReturn, pt, at, bt, qt, clamped, x);
        this.active = a.active;
    }

    public SpringDynamicParams copy()
    {
        SpringDynamicParams params = new SpringDynamicParams();

        params.copy(this);

        return params;
    }

    public void copy(SpringDynamicParams other)
    {
        this.influence = other.influence;
        this.pullStrength = other.pullStrength;
        this.drag = other.drag;
        this.springReturn = other.springReturn;
        this.active = other.active;
    }

    public boolean isDefault()
    {
        return this.influence == DEFAULT.influence
            && this.pullStrength == DEFAULT.pullStrength
            && this.drag == DEFAULT.drag
            && this.springReturn == DEFAULT.springReturn
            && this.active == DEFAULT.active;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj instanceof SpringDynamicParams params)
        {
            return this.influence == params.influence
                && this.pullStrength == params.pullStrength
                && this.drag == params.drag
                && this.springReturn == params.springReturn
                && this.active == params.active;
        }

        return false;
    }

    @Override
    public void toData(MapType data)
    {
        data.putDouble("influence", this.influence);
        data.putDouble("pull_strength", this.pullStrength);
        data.putDouble("drag", this.drag);
        data.putDouble("spring_return", this.springReturn);
        data.putBool("active", this.active);
    }

    @Override
    public void fromData(MapType data)
    {
        this.influence = (float) data.getDouble("influence", DEFAULT.influence);
        this.pullStrength = (float) data.getDouble("pull_strength", DEFAULT.pullStrength);
        this.drag = (float) data.getDouble("drag", DEFAULT.drag);
        this.springReturn = (float) data.getDouble("spring_return", DEFAULT.springReturn);
        this.active = data.getBool("active", DEFAULT.active);
    }
}
