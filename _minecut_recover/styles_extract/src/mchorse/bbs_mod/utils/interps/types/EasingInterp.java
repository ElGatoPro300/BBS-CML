package mchorse.bbs_mod.utils.interps.types;

import mchorse.bbs_mod.utils.interps.InterpContext;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.interps.easings.IEasing;

public class EasingInterp extends BaseInterp
{
    public final IEasing easing;

    public EasingInterp(String key, int keybind, IEasing easing)
    {
        super(key, keybind);

        this.easing = easing;
    }

    @Override
    public double interpolate(InterpContext context)
    {
        return Lerps.lerp(context.a, context.b, this.easing.calculate(context.getArgs(), context.x));
    }
}