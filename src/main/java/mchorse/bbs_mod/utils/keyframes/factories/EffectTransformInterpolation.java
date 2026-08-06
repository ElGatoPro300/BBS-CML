package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.utils.interps.IInterp;

/**
 * Interpolates {@link EffectTransform} fields for paint keyframes.
 */
public class EffectTransformInterpolation
{
    private EffectTransformInterpolation()
    {}

    public static void interpolate(EffectTransform out, EffectTransform preA, EffectTransform a, EffectTransform b, EffectTransform postB, IInterp interpolation, float x)
    {
        EffectTransform preAValue = valueOrDefault(preA);
        EffectTransform aValue = valueOrDefault(a);
        EffectTransform bValue = valueOrDefault(b);
        EffectTransform postBValue = valueOrDefault(postB);

        out.offsetX = (float) interpolation.interpolate(IInterp.context.set(preAValue.offsetX, aValue.offsetX, bValue.offsetX, postBValue.offsetX, x));
        out.offsetY = (float) interpolation.interpolate(IInterp.context.set(preAValue.offsetY, aValue.offsetY, bValue.offsetY, postBValue.offsetY, x));
        out.offsetZ = (float) interpolation.interpolate(IInterp.context.set(preAValue.offsetZ, aValue.offsetZ, bValue.offsetZ, postBValue.offsetZ, x));
        out.scaleX = (float) interpolation.interpolate(IInterp.context.set(preAValue.scaleX, aValue.scaleX, bValue.scaleX, postBValue.scaleX, x));
        out.scaleY = (float) interpolation.interpolate(IInterp.context.set(preAValue.scaleY, aValue.scaleY, bValue.scaleY, postBValue.scaleY, x));
        out.scaleZ = (float) interpolation.interpolate(IInterp.context.set(preAValue.scaleZ, aValue.scaleZ, bValue.scaleZ, postBValue.scaleZ, x));
        out.rotateX = (float) interpolation.interpolate(IInterp.context.set(preAValue.rotateX, aValue.rotateX, bValue.rotateX, postBValue.rotateX, x));
        out.rotateY = (float) interpolation.interpolate(IInterp.context.set(preAValue.rotateY, aValue.rotateY, bValue.rotateY, postBValue.rotateY, x));
        out.rotateZ = (float) interpolation.interpolate(IInterp.context.set(preAValue.rotateZ, aValue.rotateZ, bValue.rotateZ, postBValue.rotateZ, x));
        out.shape = x < 0.5F ? aValue.shape : bValue.shape;
    }

    private static EffectTransform valueOrDefault(EffectTransform value)
    {
        return value == null ? new EffectTransform() : value;
    }
}
