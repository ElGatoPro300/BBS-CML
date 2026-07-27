package mchorse.bbs_mod.forms.forms.utils;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Mine-imator style "Look at" constraint. Every bone of the form can get locked
 * onto another replay's entity (optionally onto one of its attachments/bones)
 * with its own lock strength. Optionally the whole form also follows the
 * target's displacement.
 */
public class LookAt implements IMapSerializable
{
    public static final int NO_TARGET = -1;

    /* Follow the (first locked bone's) target's displacement */
    public boolean translate = false;

    /* Per bone lock settings of this form's own bones */
    public final Map<String, LookAtBone> bones = new HashMap<>();

    public LookAt()
    {}

    public boolean isActive()
    {
        for (LookAtBone bone : this.bones.values())
        {
            if (bone.isActive())
            {
                return true;
            }
        }

        return false;
    }

    /**
     * The bone entry for given key, creating it on demand.
     */
    public LookAtBone get(String key)
    {
        return this.bones.computeIfAbsent(key, (k) -> new LookAtBone());
    }

    public boolean hasSameTarget(LookAt lookAt)
    {
        if (lookAt == null || this.translate != lookAt.translate)
        {
            return false;
        }

        if (!this.bones.keySet().equals(lookAt.bones.keySet()))
        {
            return false;
        }

        for (Map.Entry<String, LookAtBone> entry : this.bones.entrySet())
        {
            if (!entry.getValue().hasSameTarget(lookAt.bones.get(entry.getKey())))
            {
                return false;
            }
        }

        return true;
    }

    public LookAt copy()
    {
        LookAt lookAt = new LookAt();

        lookAt.translate = this.translate;

        for (Map.Entry<String, LookAtBone> entry : this.bones.entrySet())
        {
            lookAt.bones.put(entry.getKey(), entry.getValue().copy());
        }

        return lookAt;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (super.equals(obj))
        {
            return true;
        }

        if (obj instanceof LookAt lookAt)
        {
            return this.translate == lookAt.translate && Objects.equals(this.bones, lookAt.bones);
        }

        return false;
    }

    @Override
    public void fromData(MapType data)
    {
        this.translate = data.getBool("translate", false);
        this.bones.clear();

        if (data.has("bones"))
        {
            MapType bones = data.getMap("bones");

            for (String key : bones.keys())
            {
                LookAtBone bone = new LookAtBone();

                bone.fromData(bones.getMap(key));
                this.bones.put(key, bone);
            }
        }
    }

    @Override
    public void toData(MapType data)
    {
        data.putBool("translate", this.translate);

        if (!this.bones.isEmpty())
        {
            MapType bones = new MapType();

            for (Map.Entry<String, LookAtBone> entry : this.bones.entrySet())
            {
                bones.put(entry.getKey(), entry.getValue().toData());
            }

            data.put("bones", bones);
        }
    }
}
