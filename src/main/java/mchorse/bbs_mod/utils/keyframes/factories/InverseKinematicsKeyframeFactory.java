package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.forms.forms.utils.InverseKinematics;
import mchorse.bbs_mod.forms.forms.utils.InverseKinematicsBone;
import mchorse.bbs_mod.utils.interps.IInterp;

import java.util.Map;

public class InverseKinematicsKeyframeFactory implements IKeyframeFactory<InverseKinematics>
{
    @Override
    public InverseKinematics fromData(BaseType data)
    {
        InverseKinematics ik = new InverseKinematics();

        if (data.isMap())
        {
            ik.fromData(data.asMap());
        }

        return ik;
    }

    @Override
    public BaseType toData(InverseKinematics value)
    {
        return value.toData();
    }

    @Override
    public InverseKinematics createEmpty()
    {
        return new InverseKinematics();
    }

    @Override
    public InverseKinematics copy(InverseKinematics value)
    {
        return value.copy();
    }

    @Override
    public InverseKinematics interpolate(InverseKinematics preA, InverseKinematics a, InverseKinematics b, InverseKinematics postB, IInterp interpolation, float x)
    {
        InverseKinematics ik = (x < 1F ? a : b).copy();

        if (a.hasSameTarget(b))
        {
            for (Map.Entry<String, InverseKinematicsBone> entry : ik.bones.entrySet())
            {
                InverseKinematicsBone boneA = a.bones.get(entry.getKey());
                InverseKinematicsBone boneB = b.bones.get(entry.getKey());

                if (boneA != null && boneB != null)
                {
                    entry.getValue().blend = interpolation.interpolate(boneA.blend, boneB.blend, x);
                    entry.getValue().angleOffset = interpolation.interpolate(boneA.angleOffset, boneB.angleOffset, x);
                }
            }
        }

        return ik;
    }
}
