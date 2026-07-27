package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.IntType;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.interps.InterpContext;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.interps.easings.EasingArgs;
import mchorse.bbs_mod.utils.keyframes.BezierUtils;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class IntegerKeyframeFactory implements IKeyframeFactory<Integer>
{
    @Override
    public Integer fromData(BaseType data)
    {
        return data.isNumeric() ? data.asNumeric().intValue() : 0;
    }

    @Override
    public BaseType toData(Integer value)
    {
        return new IntType(value);
    }

    @Override
    public Integer createEmpty()
    {
        return 0;
    }

    @Override
    public Integer copy(Integer value)
    {
        return value;
    }

    @Override
    public Integer interpolate(Keyframe<Integer> preA, Keyframe<Integer> a, Keyframe<Integer> b, Keyframe<Integer> postB, IInterp interpolation, float x)
    {
        if (interpolation.has(Interpolations.BEZIER))
        {
            return (int) BezierUtils.get(
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

        return (int) interpolation.interpolate(ctx);
    }
    
    private double getWeight(Keyframe<?> kf)
    {
        if (kf == null) return 1.0;
        double w = kf.getInterpolation().getArgs().v1;
        return w == 0 ? 1.0 : w;
    }

    @Override
    public Integer interpolate(Integer preA, Integer a, Integer b, Integer postB, IInterp interpolation, float x)
    {
        return (int) interpolation.interpolate(IInterp.context.set(preA, a, b, postB, x));
    }

    @Override
    public double getY(Integer value)
    {
        return value;
    }

    @Override
    public Object yToValue(double y)
    {
        return (int) y;
    }
}