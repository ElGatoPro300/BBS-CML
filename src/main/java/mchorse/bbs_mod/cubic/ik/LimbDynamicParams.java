package mchorse.bbs_mod.cubic.ik;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.interps.IInterp;

/**
 * Animatable per-chain IK scalars layered over the form's limb constraint config
 * at playback. Floats interpolate; booleans step.
 */
public class LimbDynamicParams implements IMapSerializable
{
    public static final float DEFAULT_INFLUENCE = 1F;
    public static final float DEFAULT_FLEXIBILITY = 0.05F;
    public static final float DEFAULT_BEND_OFFSET = 0F;

    public static final LimbDynamicParams DEFAULT = new LimbDynamicParams();

    public float influence = DEFAULT_INFLUENCE;
    public float flexibility = DEFAULT_FLEXIBILITY;
    public float bendOffset = DEFAULT_BEND_OFFSET;
    public boolean active = true;
    public boolean usePole = true;

    public void identity()
    {
        this.influence = DEFAULT_INFLUENCE;
        this.flexibility = DEFAULT_FLEXIBILITY;
        this.bendOffset = DEFAULT_BEND_OFFSET;
        this.active = true;
        this.usePole = true;
    }

    public void lerp(LimbDynamicParams preA, LimbDynamicParams a, LimbDynamicParams b, LimbDynamicParams postB, IInterp interp, float x)
    {
        this.influence = (float) interp.interpolate(IInterp.context.set(preA.influence, a.influence, b.influence, postB.influence, x));
        this.flexibility = (float) interp.interpolate(IInterp.context.set(preA.flexibility, a.flexibility, b.flexibility, postB.flexibility, x));
        this.bendOffset = (float) interp.interpolate(IInterp.context.set(preA.bendOffset, a.bendOffset, b.bendOffset, postB.bendOffset, x));
        this.active = a.active;
        this.usePole = a.usePole;
    }

    public LimbDynamicParams copy()
    {
        LimbDynamicParams params = new LimbDynamicParams();

        params.copy(this);

        return params;
    }

    public void copy(LimbDynamicParams other)
    {
        this.influence = other.influence;
        this.flexibility = other.flexibility;
        this.bendOffset = other.bendOffset;
        this.active = other.active;
        this.usePole = other.usePole;
    }

    public boolean isDefault()
    {
        return this.influence == DEFAULT.influence
            && this.flexibility == DEFAULT.flexibility
            && this.bendOffset == DEFAULT.bendOffset
            && this.active == DEFAULT.active
            && this.usePole == DEFAULT.usePole;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj instanceof LimbDynamicParams params)
        {
            return this.influence == params.influence
                && this.flexibility == params.flexibility
                && this.bendOffset == params.bendOffset
                && this.active == params.active
                && this.usePole == params.usePole;
        }

        return false;
    }

    @Override
    public void toData(MapType data)
    {
        data.putDouble("influence", this.influence);
        data.putDouble("flexibility", this.flexibility);
        data.putDouble("bend_offset", this.bendOffset);
        data.putBool("active", this.active);
        data.putBool("use_pole", this.usePole);
    }

    @Override
    public void fromData(MapType data)
    {
        this.influence = (float) data.getDouble("influence", DEFAULT.influence);
        this.flexibility = (float) data.getDouble("flexibility", DEFAULT.flexibility);
        this.bendOffset = (float) data.getDouble("bend_offset", DEFAULT.bendOffset);
        this.active = data.getBool("active", DEFAULT.active);
        this.usePole = data.getBool("use_pole", DEFAULT.usePole);
    }
}
