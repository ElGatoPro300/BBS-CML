package mchorse.bbs_mod.forms.forms.shape.nodes;

import mchorse.bbs_mod.data.types.MapType;

import java.util.Collections;
import java.util.List;

public class ValueNode extends ShapeNode
{
    public float value = 0F;

    public ValueNode()
    {}

    @Override
    public String getType()
    {
        return "value";
    }

    @Override
    public List<String> getOutputs()
    {
        return Collections.singletonList("value");
    }

    @Override
    public void toData(MapType data)
    {
        super.toData(data);
        data.putFloat("val", this.value);
    }

    @Override
    public void fromData(MapType data)
    {
        super.fromData(data);
        this.value = data.getFloat("val");
    }
}
