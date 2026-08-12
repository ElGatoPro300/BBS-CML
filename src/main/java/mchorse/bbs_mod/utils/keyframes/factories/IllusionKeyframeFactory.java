package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.forms.forms.utils.Illusion;
import mchorse.bbs_mod.utils.interps.IInterp;

public class IllusionKeyframeFactory implements IKeyframeFactory<Illusion>
{
    @Override
    public Illusion fromData(BaseType data)
    {
        Illusion illusion = new Illusion();

        if (data.isMap())
        {
            illusion.fromData(data.asMap());
        }

        return illusion;
    }

    @Override
    public BaseType toData(Illusion value)
    {
        return value.toData();
    }

    @Override
    public Illusion createEmpty()
    {
        return new Illusion();
    }

    @Override
    public Illusion copy(Illusion value)
    {
        return value.copy();
    }

    @Override
    public Illusion interpolate(Illusion preA, Illusion a, Illusion b, Illusion postB, IInterp interpolation, float x)
    {
        Illusion illusion = (x < 1F ? a : b).copy();

        if (a.hasSameShape(b))
        {
            illusion.count = Math.round(interpolation.interpolate(a.count, b.count, x));
            illusion.spread = interpolation.interpolate(a.spread, b.spread, x);
            illusion.offset = interpolation.interpolate(a.offset, b.offset, x);
            illusion.opacity = interpolation.interpolate(a.opacity, b.opacity, x);
            illusion.spacing = interpolation.interpolate(a.spacing, b.spacing, x);
            illusion.delay = interpolation.interpolate(a.delay, b.delay, x);
            illusion.distort = interpolation.interpolate(a.distort, b.distort, x);
            illusion.glow = interpolation.interpolate(a.glow, b.glow, x);
            illusion.distortUniform = a.distortUniform;
            illusion.distortInvert = a.distortInvert;
            illusion.opacityUniform = a.opacityUniform;
            illusion.glowUniform = a.glowUniform;
            illusion.glowInvert = a.glowInvert;
            illusion.gradual = a.gradual;
            illusion.gradualInvert = a.gradualInvert;
            illusion.transform.lerp(a.transform, a.transform, b.transform, b.transform, interpolation, x);
        }

        return illusion;
    }
}
