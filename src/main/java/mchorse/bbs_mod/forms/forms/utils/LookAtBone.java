package mchorse.bbs_mod.forms.forms.utils;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;

/**
 * Per bone "Look at" lock settings: whether the lock is enabled, how strongly the
 * bone gets locked (0..1, 0 by default), which replay it locks onto and which
 * attachment/bone of the target it looks at.
 */
public class LookAtBone implements IMapSerializable
{
    public boolean enabled;
    public float blend;
    public int replay = LookAt.NO_TARGET;
    public String attachment = "";

    public LookAtBone()
    {}

    public boolean isActive()
    {
        return this.enabled && this.blend > 0F && this.replay != LookAt.NO_TARGET;
    }

    public boolean hasSameTarget(LookAtBone bone)
    {
        return bone != null
            && this.enabled == bone.enabled
            && this.replay == bone.replay
            && this.attachment.equals(bone.attachment);
    }

    public LookAtBone copy()
    {
        LookAtBone bone = new LookAtBone();

        bone.enabled = this.enabled;
        bone.blend = this.blend;
        bone.replay = this.replay;
        bone.attachment = this.attachment;

        return bone;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (super.equals(obj))
        {
            return true;
        }

        if (obj instanceof LookAtBone bone)
        {
            return this.hasSameTarget(bone) && this.blend == bone.blend;
        }

        return false;
    }

    @Override
    public void fromData(MapType data)
    {
        this.enabled = data.getBool("enabled", false);
        this.blend = data.getFloat("blend", 0F);
        this.replay = data.getInt("actor", LookAt.NO_TARGET);
        this.attachment = data.getString("attachment");
    }

    @Override
    public void toData(MapType data)
    {
        data.putBool("enabled", this.enabled);
        data.putFloat("blend", this.blend);
        data.putInt("actor", this.replay);
        data.putString("attachment", this.attachment);
    }
}
