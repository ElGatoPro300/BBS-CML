package mchorse.bbs_mod.forms.forms.shape.nodes;

import mchorse.bbs_mod.data.types.MapType;

import java.util.Collections;
import java.util.List;

public class IrisAttributeNode extends ShapeNode
{
    public Attribute attribute = Attribute.COLOR_R;

    @Override
    public String getType()
    {
        return "iris_attribute";
    }

    @Override
    public List<String> getInputs()
    {
        return Collections.singletonList("value");
    }

    @Override
    public void toData(MapType data)
    {
        super.toData(data);

        data.putInt("attr", this.attribute.ordinal());
    }

    @Override
    public void fromData(MapType data)
    {
        super.fromData(data);

        if (data.has("attr"))
        {
            int index = data.getInt("attr");

            if (index >= 0 && index < Attribute.values().length)
            {
                this.attribute = Attribute.values()[index];
            }
        }
    }

    public enum Attribute
    {
        COLOR_R, COLOR_G, COLOR_B, COLOR_A, LIGHT_BLOCK, LIGHT_SKY, OVERLAY_U, OVERLAY_V
    }
}
