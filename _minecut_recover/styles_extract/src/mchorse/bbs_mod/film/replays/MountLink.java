package mchorse.bbs_mod.film.replays;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;

public class MountLink implements IMapSerializable
{
    public static final int NO_REPLAY = -1;

    public boolean active = false;
    public int replay = NO_REPLAY;

    public MountLink()
    {}

    public MountLink(boolean active, int replay)
    {
        this.active = active;
        this.replay = replay;
    }

    public boolean hasSameTarget(MountLink other)
    {
        return other != null && this.active == other.active && this.replay == other.replay;
    }

    public MountLink copy()
    {
        return new MountLink(this.active, this.replay);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (super.equals(obj))
        {
            return true;
        }

        if (obj instanceof MountLink link)
        {
            return this.hasSameTarget(link);
        }

        return false;
    }

    @Override
    public void fromData(MapType data)
    {
        this.active = data.getBool("active", false);
        this.replay = data.getInt("replay", NO_REPLAY);
    }

    @Override
    public void toData(MapType data)
    {
        data.putBool("active", this.active);
        data.putInt("replay", this.replay);
    }
}
