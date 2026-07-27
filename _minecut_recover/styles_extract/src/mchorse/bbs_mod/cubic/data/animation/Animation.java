package mchorse.bbs_mod.cubic.data.animation;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.math.molang.MolangParser;

import java.util.HashMap;
import java.util.Map;

public class Animation implements IMapSerializable
{
    public final String id;
    public final MolangParser parser;

    /**
     * Animation length in seconds
     */
    private double length;

    public Map<String, AnimationPart> parts = new HashMap<>();

    public Animation(String id, MolangParser parser)
    {
        this.id = id;
        this.parser = parser;
    }

    public void setLength(double length)
    {
        this.length = length;
    }

    public double getLength()
    {
        return this.length;
    }

    public int getLengthInTicks()
    {
        return (int) Math.floor(this.length * 20);
    }

    @Override
    public void fromData(MapType data)
    {
        if (data.has("duration"))
        {
            this.setLength(data.getDouble("duration"));
        }

        if (data.has("groups"))
        {
            for (Map.Entry<String, BaseType> entry : data.getMap("groups"))
            {
                AnimationPart value = new AnimationPart(this.parser);

                value.fromData((MapType) entry.getValue());
                this.parts.put(entry.getKey(), value);
            }
        }
    }

    @Override
    public void toData(MapType data)
    {
        data.putDouble("duration", this.length);

        MapType groups = new MapType();

        for (Map.Entry<String, AnimationPart> entry : this.parts.entrySet())
        {
            groups.put(entry.getKey(), entry.getValue().toData());
        }

        data.put("groups", groups);
    }
}