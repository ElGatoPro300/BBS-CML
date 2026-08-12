package mchorse.bbs_mod.cubic.physics;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Keyframe value holding per-chain {@link SpringDynamicParams} scalars, keyed
 * by the chain's root bone. Serialized under the {@code springs} map key.
 */
public class SpringDynamicParamsMap implements IMapSerializable
{
    private static Set<String> keys = new HashSet<>();

    public final Map<String, SpringDynamicParams> controls = new HashMap<>();

    public SpringDynamicParams get(String root)
    {
        SpringDynamicParams params = this.controls.get(root);

        if (params == null)
        {
            params = new SpringDynamicParams();

            this.controls.put(root, params);
        }

        return params;
    }

    public SpringDynamicParamsMap copy()
    {
        SpringDynamicParamsMap map = new SpringDynamicParamsMap();

        map.copy(this);

        return map;
    }

    public void copy(SpringDynamicParamsMap other)
    {
        this.controls.clear();

        for (Map.Entry<String, SpringDynamicParams> entry : other.controls.entrySet())
        {
            if (!entry.getValue().isDefault())
            {
                this.controls.put(entry.getKey(), entry.getValue().copy());
            }
        }
    }

    @Override
    public void toData(MapType data)
    {
        if (this.controls.isEmpty())
        {
            return;
        }

        MapType springs = new MapType();

        for (Map.Entry<String, SpringDynamicParams> entry : this.controls.entrySet())
        {
            if (!entry.getValue().isDefault())
            {
                springs.put(entry.getKey(), entry.getValue().toData());
            }
        }

        data.put("springs", springs);
    }

    @Override
    public void fromData(MapType data)
    {
        this.controls.clear();

        MapType springs = data.getMap("springs");

        for (String key : springs.keys())
        {
            SpringDynamicParams params = new SpringDynamicParams();

            params.fromData(springs.getMap(key));

            if (!params.isDefault())
            {
                this.controls.put(key, params);
            }
        }
    }

    public boolean isEmpty()
    {
        return this.controls.isEmpty();
    }

    /** Value equality over the union of chains; a chain absent on one side counts as default. */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj instanceof SpringDynamicParamsMap other)
        {
            keys.clear();
            keys.addAll(this.controls.keySet());
            keys.addAll(other.controls.keySet());

            for (String key : keys)
            {
                SpringDynamicParams a = this.controls.get(key);
                SpringDynamicParams b = other.controls.get(key);

                if (a != null && b != null && !a.equals(b))
                {
                    return false;
                }

                if (a == null && !b.isDefault())
                {
                    return false;
                }

                if (b == null && !a.isDefault())
                {
                    return false;
                }
            }

            return true;
        }

        return false;
    }
}
