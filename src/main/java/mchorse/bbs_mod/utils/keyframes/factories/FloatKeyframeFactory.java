package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.FloatType;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.interps.InterpContext;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.interps.easings.EasingArgs;
import mchorse.bbs_mod.utils.keyframes.BezierUtils;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class FloatKeyframeFactory implements IKeyframeFactory<Float>
{
    @Override
    public Float fromData(BaseType data)
    {
        return data.isNumeric() ? data.asNumeric().floatValue() : 0F;
    }

    @Override
    public BaseType toData(Float value)
    {
        return new FloatType(value);
    }

    @Override
    public Float createEmpty()
    {
        return 0F;
    }

    @Override
    public Float copy(Float value)
    {
        return value;
    }

    @Override
    public Float interpolate(Keyframe<Float> preA, Keyframe<Float> a, Keyframe<Float> b, Keyframe<Float> postB, IInterp interpolation, float x)
    {
        if (interpolation.has(Interpolations.BEZIER))
        {
            return (float) BezierUtils.get(
                a.getValue(), b.getValue(),
                a.getTick(), b.getTick(),
                a.rx, a.ry,
                b.lx, b.ly,
                x
            );
        }

        InterpContext ctx = IInterp.context.set(preA.getValue(), a.getValue(), b.getValue(), postB.getValue(), x)
            .setBoundary(preA == a, postB == b)
            .extra(a.getInterpolation().getArgs());

        if (interpolation == Interpolations.NURBS)
        {
            EasingArgs args = a.getInterpolation().getArgs();
            
            double w0 = args.v3 != 0 ? args.v3 : getWeight(preA);
            double w1 = args.v1 != 0 ? args.v1 : 1.0;
            double w2 = args.v2 != 0 ? args.v2 : getWeight(b);
            double w3 = args.v4 != 0 ? args.v4 : getWeight(postB);

            ctx.weights(w0, w1, w2, w3);
        }

        return (float) interpolation.interpolate(ctx);
    }
    
    private double getWeight(Keyframe<?> kf)
    {
        if (kf == null) return 1.0;
        double w = kf.getInterpolation().getArgs().v1;
        return w == 0 ? 1.0 : w;
    }

    @Override
    public Float interpolate(Float preA, Float a, Float b, Float postB, IInterp interpolation, float x)
    {
        return (float) interpolation.interpolate(IInterp.context.set(preA, a, b, postB, x));
    }

    @Override
    public double getY(Float value)
    {
        return value;
    }

    @Override
    public Object yToValue(double y)
    {
        return (float) y;
    }
}