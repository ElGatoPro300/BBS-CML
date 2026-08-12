package mchorse.bbs_mod.forms.forms.utils;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Mine-imator style inverse kinematics: per-bone limb locks onto other replays.
 */
public class InverseKinematics implements IMapSerializable
{
    public static final int NO_TARGET = -1;

    public final Map<String, InverseKinematicsBone> bones = new HashMap<>();

    public InverseKinematics()
    {}

    public boolean isActive()
    {
        for (InverseKinematicsBone bone : this.bones.values())
        {
            if (bone.isActive())
            {
                return true;
            }
        }

        return false;
    }

    public InverseKinematicsBone get(String key)
    {
        return this.bones.computeIfAbsent(key, (k) -> new InverseKinematicsBone());
    }

    public boolean hasSameTarget(InverseKinematics ik)
    {
        if (ik == null || !this.bones.keySet().equals(ik.bones.keySet()))
        {
            return false;
        }

        for (Map.Entry<String, InverseKinematicsBone> entry : this.bones.entrySet())
        {
            if (!entry.getValue().hasSameTarget(ik.bones.get(entry.getKey())))
            {
                return false;
            }
        }

        return true;
    }

    public InverseKinematics copy()
    {
        InverseKinematics ik = new InverseKinematics();

        for (Map.Entry<String, InverseKinematicsBone> entry : this.bones.entrySet())
        {
            ik.bones.put(entry.getKey(), entry.getValue().copy());
        }

        return ik;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (super.equals(obj))
        {
            return true;
        }

        if (obj instanceof InverseKinematics ik)
        {
            return Objects.equals(this.bones, ik.bones);
        }

        return false;
    }

    @Override
    public void fromData(MapType data)
    {
        this.bones.clear();

        if (data.has("bones"))
        {
            MapType bones = data.getMap("bones");

            for (String key : bones.keys())
            {
                InverseKinematicsBone bone = new InverseKinematicsBone();

                bone.fromData(bones.getMap(key));
                this.bones.put(key, bone);
            }
        }
    }

    @Override
    public void toData(MapType data)
    {
        if (!this.bones.isEmpty())
        {
            MapType bones = new MapType();

            for (Map.Entry<String, InverseKinematicsBone> entry : this.bones.entrySet())
            {
                bones.put(entry.getKey(), entry.getValue().toData());
            }

            data.put("bones", bones);
        }
    }
}
