package mchorse.bbs_mod.forms.forms.utils;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;

/**
 * Per-bone Mine-imator style inverse kinematics settings: lock a limb toward
 * another replay's attachment with optional angle target, offset and blend.
 */
public class InverseKinematicsBone implements IMapSerializable
{
    public boolean enabled;
    public int targetReplay = InverseKinematics.NO_TARGET;
    public String targetAttachment = "";
    public int angleTargetReplay = InverseKinematics.NO_TARGET;
    public String angleTargetAttachment = "";
    public float angleOffset;
    public float blend;
    public boolean bendXAsOffset;

    public InverseKinematicsBone()
    {}

    public boolean isActive()
    {
        return this.enabled && this.blend > 0F && this.targetReplay != InverseKinematics.NO_TARGET;
    }

    public boolean hasSameTarget(InverseKinematicsBone bone)
    {
        return bone != null
            && this.enabled == bone.enabled
            && this.targetReplay == bone.targetReplay
            && this.targetAttachment.equals(bone.targetAttachment)
            && this.angleTargetReplay == bone.angleTargetReplay
            && this.angleTargetAttachment.equals(bone.angleTargetAttachment)
            && this.bendXAsOffset == bone.bendXAsOffset;
    }

    public InverseKinematicsBone copy()
    {
        InverseKinematicsBone bone = new InverseKinematicsBone();

        bone.enabled = this.enabled;
        bone.targetReplay = this.targetReplay;
        bone.targetAttachment = this.targetAttachment;
        bone.angleTargetReplay = this.angleTargetReplay;
        bone.angleTargetAttachment = this.angleTargetAttachment;
        bone.angleOffset = this.angleOffset;
        bone.blend = this.blend;
        bone.bendXAsOffset = this.bendXAsOffset;

        return bone;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (super.equals(obj))
        {
            return true;
        }

        if (obj instanceof InverseKinematicsBone bone)
        {
            return this.hasSameTarget(bone)
                && this.angleOffset == bone.angleOffset
                && this.blend == bone.blend;
        }

        return false;
    }

    @Override
    public void fromData(MapType data)
    {
        this.enabled = data.getBool("enabled", false);
        this.targetReplay = data.getInt("target_actor", InverseKinematics.NO_TARGET);
        this.targetAttachment = data.getString("target_attachment");
        this.angleTargetReplay = data.getInt("angle_actor", InverseKinematics.NO_TARGET);
        this.angleTargetAttachment = data.getString("angle_attachment");
        this.angleOffset = data.getFloat("angle_offset", 0F);
        this.blend = data.getFloat("blend", 0F);
        this.bendXAsOffset = data.getBool("bend_x_as_offset", false);
    }

    @Override
    public void toData(MapType data)
    {
        data.putBool("enabled", this.enabled);
        data.putInt("target_actor", this.targetReplay);
        data.putString("target_attachment", this.targetAttachment);
        data.putInt("angle_actor", this.angleTargetReplay);
        data.putString("angle_attachment", this.angleTargetAttachment);
        data.putFloat("angle_offset", this.angleOffset);
        data.putFloat("blend", this.blend);
        data.putBool("bend_x_as_offset", this.bendXAsOffset);
    }
}
