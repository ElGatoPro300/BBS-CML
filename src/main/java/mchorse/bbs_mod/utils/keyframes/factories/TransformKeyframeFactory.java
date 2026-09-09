package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;

public class TransformKeyframeFactory implements IKeyframeFactory<Transform>
{
    private final Transform i = new Transform();
    private final PoseTransform ip = new PoseTransform();
    private static final PoseTransform DEFAULT_POSE = new PoseTransform();

    @Override
    public Transform fromData(BaseType data)
    {
        Transform transform;

        if (data.isMap())
        {
            boolean poseTransform = data.asMap().has("fix") || data.asMap().has("color") || data.asMap().has("lighting") || data.asMap().has("texture");

            transform = poseTransform ? new PoseTransform() : new Transform();
            transform.fromData(data.asMap());

            return transform;
        }

        transform = new Transform();

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
        if (preA instanceof PoseTransform || a instanceof PoseTransform || b instanceof PoseTransform || postB instanceof PoseTransform)
        {
            this.ip.lerp(preA, a, b, postB, interpolation, x);

            PoseTransform aPose = a instanceof PoseTransform ? (PoseTransform) a : DEFAULT_POSE;
            PoseTransform bPose = b instanceof PoseTransform ? (PoseTransform) b : DEFAULT_POSE;

            this.ip.applyStepTexture(aPose, bPose, x);

            return this.ip;
        }

        this.i.lerp(preA, a, b, postB, interpolation, x);

        return this.i;
    }
}