package mchorse.bbs_mod.cubic.ik;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Keyframe value holding per-chain {@link LimbDynamicParams}, keyed by tip bone.
 */
public class LimbDynamicParamsMap implements IMapSerializable
{
    private static Set<String> keys = new HashSet<>();

    public final Map<String, LimbDynamicParams> params = new HashMap<>();

    public LimbDynamicParams get(String tip)
    {
        LimbDynamicParams entry = this.params.get(tip);

        if (entry == null)
        {
            entry = new LimbDynamicParams();

            this.params.put(tip, entry);
        }

        return entry;
    }

    public LimbDynamicParamsMap copy()
    {
        LimbDynamicParamsMap copy = new LimbDynamicParamsMap();

        copy.copy(this);

        return copy;
    }

    public void copy(LimbDynamicParamsMap other)
    {
        this.params.clear();

        for (Map.Entry<String, LimbDynamicParams> entry : other.params.entrySet())
        {
            if (!entry.getValue().isDefault())
            {
                this.params.put(entry.getKey(), entry.getValue().copy());
            }
        }
    }

    @Override
    public void toData(MapType data)
    {
        if (this.params.isEmpty())
        {
            return;
        }

        MapType limbs = new MapType();

        for (Map.Entry<String, LimbDynamicParams> entry : this.params.entrySet())
        {
            if (!entry.getValue().isDefault())
            {
                limbs.put(entry.getKey(), entry.getValue().toData());
            }
        }

        data.put("limbs", limbs);
    }

    @Override
    public void fromData(MapType data)
    {
        this.params.clear();

        MapType limbs = data.getMap("limbs");

        for (String key : limbs.keys())
        {
            LimbDynamicParams entry = new LimbDynamicParams();

            entry.fromData(limbs.getMap(key));

            if (!entry.isDefault())
            {
                this.params.put(key, entry);
            }
        }
    }

    public boolean isEmpty()
    {
        return this.params.isEmpty();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj instanceof LimbDynamicParamsMap other)
        {
            keys.clear();
            keys.addAll(this.params.keySet());
            keys.addAll(other.params.keySet());

            for (String key : keys)
            {
                LimbDynamicParams a = this.params.get(key);
                LimbDynamicParams b = other.params.get(key);

                if (a != null && b != null && !a.equals(b)) return false;
                if (a == null && !b.isDefault()) return false;
                if (b == null && !a.isDefault()) return false;
            }

            return true;
        }

        return false;
    }
}
