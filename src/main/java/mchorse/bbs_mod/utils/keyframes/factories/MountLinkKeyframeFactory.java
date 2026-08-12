package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.film.replays.MountLink;
import mchorse.bbs_mod.utils.interps.IInterp;

public class MountLinkKeyframeFactory implements IKeyframeFactory<MountLink>
{
    @Override
    public MountLink fromData(BaseType data)
    {
        MountLink link = new MountLink();

        if (data.isMap())
        {
            link.fromData(data.asMap());
        }

        return link;
    }

    @Override
    public BaseType toData(MountLink value)
    {
        return value.toData();
    }

    @Override
    public MountLink createEmpty()
    {
        return new MountLink();
    }

    @Override
    public MountLink copy(MountLink value)
    {
        return value.copy();
    }

    @Override
    public MountLink interpolate(MountLink preA, MountLink a, MountLink b, MountLink postB, IInterp interpolation, float x)
    {
        if (a.hasSameTarget(b))
        {
            return b.copy();
        }

        return x < 0.5F ? a.copy() : b.copy();
    }
}
