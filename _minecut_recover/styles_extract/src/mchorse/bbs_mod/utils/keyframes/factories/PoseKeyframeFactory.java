package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.interps.easings.EasingArgs;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;
import mchorse.bbs_mod.utils.resources.LinkUtils;

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
    public Pose interpolate(Keyframe<Pose> preA, Keyframe<Pose> a, Keyframe<Pose> b, Keyframe<Pose> postB, IInterp interpolation, float x)
    {
        keys.clear();

        Pose pA = preA.getValue();
        Pose A = a.getValue();
        Pose B = b.getValue();
        Pose pB = postB.getValue();

        if (pA != A && pA != null) keys.addAll(pA.transforms.keySet());
        if (A != null) keys.addAll(A.transforms.keySet());
        if (B != null) keys.addAll(B.transforms.keySet());
        if (pB != B && pB != null) keys.addAll(pB.transforms.keySet());

        for (PoseTransform value : this.i.transforms.values())
        {
            value.identity();
        }

        double w0 = 1.0, w1 = 1.0, w2 = 1.0, w3 = 1.0;
        
        if (interpolation == Interpolations.NURBS)
        {
            EasingArgs args = a.getInterpolation().getArgs();
            
            w0 = args.v3 != 0 ? args.v3 : getWeight(preA);
            w1 = args.v1 != 0 ? args.v1 : 1.0;
            w2 = args.v2 != 0 ? args.v2 : getWeight(b);
            w3 = args.v4 != 0 ? args.v4 : getWeight(postB);
        }

        for (String key : keys)
        {
            Transform transform = this.i.get(key);
            Transform preATransform = pA.get(key);
            Transform aTransform = A.get(key);
            Transform bTransform = B.get(key);
            Transform postBTransform = pB.get(key);

            transform.lerp(preATransform, aTransform, bTransform, postBTransform, interpolation, x, w0, w1, w2, w3);

            if (transform instanceof PoseTransform result)
            {
                this.interpolateBoneTexture(result, a, aTransform, bTransform, x);
            }
        }

        return this.i;
    }

    private double getWeight(Keyframe<?> kf)
    {
        if (kf == null) return 1.0;
        double w = kf.getInterpolation().getArgs().v1;
        return w == 0 ? 1.0 : w;
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

            if (transform instanceof PoseTransform poseTransform
                && aTransform instanceof PoseTransform aPose
                && bTransform instanceof PoseTransform bPose)
            {
                poseTransform.applyStepTexture(aPose, bPose, x);
            }
        }

        return this.i;
    }

    private void interpolateBoneTexture(PoseTransform result, Keyframe<Pose> a, Transform aTransform, Transform bTransform, float x)
    {
        PoseTransform aPose = aTransform instanceof PoseTransform ? (PoseTransform) aTransform : new PoseTransform();
        PoseTransform bPose = bTransform instanceof PoseTransform ? (PoseTransform) bTransform : new PoseTransform();
        Link aTexture = aPose.texture;
        Link bTexture = bPose.texture;

        if (a.isBend() && aTexture != null && bTexture != null && !aTexture.equals(bTexture))
        {
            result.texture = LinkUtils.copy(aTexture);
            result.textureBlendTo = LinkUtils.copy(bTexture);
            result.textureBlend = (float) a.getInterpolation().interpolate(0D, 1D, x);
        }
        else
        {
            result.textureBlendTo = null;
            PoseTransform pick = x >= 1F ? bPose : aPose;

            result.texture = pick.texture != null ? LinkUtils.copy(pick.texture) : null;
            result.textureBlend = 1F;
        }
    }
}