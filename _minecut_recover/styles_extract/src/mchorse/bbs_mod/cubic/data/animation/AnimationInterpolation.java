package mchorse.bbs_mod.cubic.data.animation;

import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.interps.Interpolations;

import java.util.HashMap;
import java.util.Map;

public class AnimationInterpolation
{
    public static final Map<String, IInterp> GECKO_LIB_NAMES = new HashMap<>();

    static
    {
        GECKO_LIB_NAMES.put("linear", Interpolations.LINEAR);
        GECKO_LIB_NAMES.put("catmullrom", Interpolations.HERMITE);
        GECKO_LIB_NAMES.put("step", Interpolations.CONST);
        GECKO_LIB_NAMES.put("easeInSine", Interpolations.SINE_IN);
        GECKO_LIB_NAMES.put("easeOutSine", Interpolations.SINE_OUT);
        GECKO_LIB_NAMES.put("easeInOutSine", Interpolations.SINE_INOUT);
        GECKO_LIB_NAMES.put("easeInQuad", Interpolations.QUAD_IN);
        GECKO_LIB_NAMES.put("easeOutQuad", Interpolations.QUAD_OUT);
        GECKO_LIB_NAMES.put("easeInOutQuad", Interpolations.QUAD_INOUT);
        GECKO_LIB_NAMES.put("easeInCubic", Interpolations.CUBIC_IN);
        GECKO_LIB_NAMES.put("easeOutCubic", Interpolations.CUBIC_OUT);
        GECKO_LIB_NAMES.put("easeInOutCubic", Interpolations.CUBIC_INOUT);
        GECKO_LIB_NAMES.put("easeInQuart", Interpolations.QUART_IN);
        GECKO_LIB_NAMES.put("easeOutQuart", Interpolations.QUART_OUT);
        GECKO_LIB_NAMES.put("easeInOutQuart", Interpolations.QUART_INOUT);
        GECKO_LIB_NAMES.put("easeInQuint", Interpolations.QUINT_IN);
        GECKO_LIB_NAMES.put("easeOutQuint", Interpolations.QUINT_OUT);
        GECKO_LIB_NAMES.put("easeInOutQuint", Interpolations.QUINT_INOUT);
        GECKO_LIB_NAMES.put("easeInExpo", Interpolations.EXP_IN);
        GECKO_LIB_NAMES.put("easeOutExpo", Interpolations.EXP_OUT);
        GECKO_LIB_NAMES.put("easeInOutExpo", Interpolations.EXP_INOUT);
        GECKO_LIB_NAMES.put("easeInCirc", Interpolations.CIRCLE_IN);
        GECKO_LIB_NAMES.put("easeOutCirc", Interpolations.CIRCLE_OUT);
        GECKO_LIB_NAMES.put("easeInOutCirc", Interpolations.CIRCLE_INOUT);
        GECKO_LIB_NAMES.put("easeInBack", Interpolations.BACK_IN);
        GECKO_LIB_NAMES.put("easeOutBack", Interpolations.BACK_OUT);
        GECKO_LIB_NAMES.put("easeInOutBack", Interpolations.BACK_INOUT);

        /* These are inverted (i.e. in and out swapped places)
         * because that's how the GeckoLib plugin shows */
        GECKO_LIB_NAMES.put("easeInElastic", Interpolations.ELASTIC_OUT);
        GECKO_LIB_NAMES.put("easeOutElastic", Interpolations.ELASTIC_IN);
        GECKO_LIB_NAMES.put("easeInOutElastic", Interpolations.ELASTIC_INOUT);
        GECKO_LIB_NAMES.put("easeInBounce", Interpolations.BOUNCE_OUT);
        GECKO_LIB_NAMES.put("easeOutBounce", Interpolations.BOUNCE_IN);
        GECKO_LIB_NAMES.put("easeInOutBounce", Interpolations.BOUNCE_INOUT);
    }

    public static IInterp byName(String easing)
    {
        return GECKO_LIB_NAMES.getOrDefault(easing, Interpolations.LINEAR);
    }

    public static String toName(IInterp interp)
    {
        return CollectionUtils.getKey(GECKO_LIB_NAMES, interp);
    }
}