package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.pose.PoseTransform;

public class PoseTransformKeyframeFactory implements IKeyframeFactory<PoseTransform>
{
    private PoseTransform i = new PoseTransform();

    @Override
    public PoseTransform fromData(BaseType data)
    {
        PoseTransform transform = new PoseTransform();

        if (data.isMap())
        {
            transform.fromData(data.asMap());
        }

        return transform;
    }

    @Override
    public BaseType toData(PoseTransform value)
    {
        return value.toData();
    }

    @Override
    public PoseTransform createEmpty()
    {
        return new PoseTransform();
    }

    @Override
    public PoseTransform copy(PoseTransform value)
    {
        return (PoseTransform) value.copy();
    }

    @Override
    public PoseTransform interpolate(PoseTransform preA, PoseTransform a, PoseTransform b, PoseTransform postB, IInterp interpolation, float x)
    {
        this.i.lerp(preA, a, b, postB, interpolation, x);

        return this.i;
    }
}
