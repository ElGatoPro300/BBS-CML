package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ByteType;
import mchorse.bbs_mod.utils.interps.IInterp;

public class BooleanKeyframeFactory implements IKeyframeFactory<Boolean>
{
    @Override
    public Boolean fromData(BaseType data)
    {
        return data.isNumeric() && data.asNumeric().boolValue();
    }

    @Override
    public BaseType toData(Boolean value)
    {
        return new ByteType(value);
    }

    @Override
    public Boolean createEmpty()
    {
        return false;
    }

    @Override
    public Boolean copy(Boolean value)
    {
        return value;
    }

    @Override
    public Boolean interpolate(Boolean preA, Boolean a, Boolean b, Boolean postB, IInterp interpolation, float x)
    {
        /* Discrete hold: keep the left keyframe for the whole segment.
         * Never threshold at 0.5 / use easing — that creates mid-segment “ghost”
         * flips on visible/render tracks (value changes before the next keyframe). */
        return a;
    }
}