package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.pose.Transform;

public class TransformKeyframeFactory implements IKeyframeFactory<Transform>
{
    private Transform i = new Transform();

    @Override
    public Transform fromData(BaseType data)
    {
        Transform transform = new Transform();

        if (data.isMap())
        {
            transform.fromData(data.asMap());
        }

        return transform;
    }

    @Override
    public BaseType toData(Transform value)
    {
        return value.toData();
    }

    @Override
    public Transform createEmpty()
    {
        return new Transform();
    }

    @Override
    public Transform copy(Transform value)
    {
        return value.copy();
    }

    @Override
    public Transform interpolate(Transform preA, Transform a, Transform b, Transform postB, IInterp interpolation, float x)
    {
        this.i.lerp(preA, a, b, postB, interpolation, x);

        return this.i;
    }
}