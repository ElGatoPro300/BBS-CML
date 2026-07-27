package mchorse.bbs_mod.forms.forms.utils;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;
import net.minecraft.class_2960;
import java.util.Objects;

public class ParticleSettings implements IMapSerializable
{
    public class_2960 particle = class_2960.method_60655("minecraft", "flame");
    public String arguments = "";

    @Override
    public void toData(MapType data)
    {
        data.putString("particle", this.particle.toString());
        data.putString("args", this.arguments);
    }

    @Override
    public void fromData(MapType data)
    {
        this.particle = class_2960.method_12829(data.getString("particle"));
        this.arguments = data.getString("args");
    }
}