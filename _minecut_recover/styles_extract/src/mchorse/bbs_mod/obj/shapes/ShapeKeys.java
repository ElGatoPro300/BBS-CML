package mchorse.bbs_mod.obj.shapes;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;

import java.util.HashMap;
import java.util.Map;

public class ShapeKeys implements IMapSerializable
{
    public final Map<String, Float> shapeKeys = new HashMap<>();

    public ShapeKeys copy()
    {
        ShapeKeys keys = new ShapeKeys();

        keys.shapeKeys.putAll(this.shapeKeys);

        return keys;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof ShapeKeys keys)
        {
            return this.shapeKeys.equals(keys.shapeKeys);
        }

        return super.equals(obj);
    }

    @Override
    public void toData(MapType data)
    {
        MapType keys = new MapType();

        for (Map.Entry<String, Float> entry : this.shapeKeys.entrySet())
        {
            keys.putFloat(entry.getKey(), entry.getValue());
        }

        data.put("keys", keys);
    }

    @Override
    public void fromData(MapType data)
    {
        this.shapeKeys.clear();

        MapType keys = data.getMap("keys");

        for (String key : keys.keys())
        {
            this.shapeKeys.put(key, keys.getFloat(key));
        }
    }
}