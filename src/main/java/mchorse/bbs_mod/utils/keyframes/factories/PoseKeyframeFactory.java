package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;

import java.util.HashSet;
import java.util.Set;

public class PoseKeyframeFactory implements IKeyframeFactory<Pose>
{
    private static Set<String> keys = new HashSet<>();

    private Pose i = new Pose();

    @Override
    public Pose fromData(BaseType data)
    {
        Pose pose = new Pose();

        if (data.isMap())
        {
            pose.fromData(data.asMap());
        }

        return pose;
    }

    @Override
    public BaseType toData(Pose value)
    {
        return value.toData();
    }

    @Override
    public Pose createEmpty()
    {
        return new Pose();
    }

    @Override
    public Pose copy(Pose value)
    {
        return value.copy();
    }

    @Override
    public Pose interpolate(Pose preA, Pose a, Pose b, Pose postB, IInterp interpolation, float x)
    {
        keys.clear();

        if (preA != a && preA != null) keys.addAll(preA.transforms.keySet());
        if (a != null) keys.addAll(a.transforms.keySet());
        if (b != null) keys.addAll(b.transforms.keySet());
        if (postB != b && postB != null) keys.addAll(postB.transforms.keySet());

        for (PoseTransform value : this.i.transforms.values())
        {
            value.identity();
        }

        for (String key : keys)
        {
            Transform transform = this.i.get(key);
            Transform preATransform = preA.get(key);
            Transform aTransform = a.get(key);
            Transform bTransform = b.get(key);
            Transform postBTransform = postB.get(key);

            transform.lerp(preATransform, aTransform, bTransform, postBTransform, interpolation, x);
        }

        return this.i;
    }
}