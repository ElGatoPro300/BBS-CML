package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.forms.forms.utils.LookAt;
import mchorse.bbs_mod.forms.forms.utils.LookAtBone;
import mchorse.bbs_mod.utils.interps.IInterp;

import java.util.Map;

public class LookAtKeyframeFactory implements IKeyframeFactory<LookAt>
{
    @Override
    public LookAt fromData(BaseType data)
    {
        LookAt lookAt = new LookAt();

        if (data.isMap())
        {
            lookAt.fromData(data.asMap());
        }

        return lookAt;
    }

    @Override
    public BaseType toData(LookAt value)
    {
        return value.toData();
    }

    @Override
    public LookAt createEmpty()
    {
        return new LookAt();
    }

    @Override
    public LookAt copy(LookAt value)
    {
        return value.copy();
    }

    @Override
    public LookAt interpolate(LookAt preA, LookAt a, LookAt b, LookAt postB, IInterp interpolation, float x)
    {
        LookAt lookAt = (x < 1F ? a : b).copy();

        if (a.hasSameTarget(b))
        {
            for (Map.Entry<String, LookAtBone> entry : lookAt.bones.entrySet())
            {
                LookAtBone boneA = a.bones.get(entry.getKey());
                LookAtBone boneB = b.bones.get(entry.getKey());

                if (boneA != null && boneB != null)
                {
                    entry.getValue().blend = interpolation.interpolate(boneA.blend, boneB.blend, x);
                }
            }
        }

        return lookAt;
    }
}
