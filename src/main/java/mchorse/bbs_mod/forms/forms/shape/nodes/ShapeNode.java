package mchorse.bbs_mod.forms.forms.shape.nodes;

import mchorse.bbs_mod.data.types.MapType;

import java.util.Collections;
import java.util.List;

public abstract class ShapeNode
{
    public float x;
    public float y;
    public int id;

    public ShapeNode()
    {}

    public abstract String getType();

    public List<String> getInputs()
    {
        return Collections.emptyList();
    }

    public List<String> getOutputs()
    {
        return Collections.emptyList();
    }

    public void toData(MapType data)
    {
        data.putFloat("x", this.x);
        data.putFloat("y", this.y);
        data.putInt("id", this.id);
        data.putString("type", this.getType());
    }

    public void fromData(MapType data)
    {
        this.x = data.getFloat("x");
        this.y = data.getFloat("y");
        this.id = data.getInt("id");
    }
}
