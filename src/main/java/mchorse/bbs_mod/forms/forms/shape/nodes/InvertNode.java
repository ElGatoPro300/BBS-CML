package mchorse.bbs_mod.forms.forms.shape.nodes;

import mchorse.bbs_mod.data.types.MapType;

import java.util.Collections;
import java.util.List;

/**
 * Inverts a value. Mode 0: scalar (1 - value). Mode 1: color (invert RGB channels).
 */
public class InvertNode extends ShapeNode
{
    public int mode = 0; /* 0 = scalar, 1 = color */

    @Override
    public String getType()
    {
        return "invert";
    }

    @Override
    public List<String> getInputs()
    {
        return Collections.singletonList("value");
    }

    @Override
    public List<String> getOutputs()
    {
        return Collections.singletonList("result");
    }

    @Override
    public void toData(MapType data)
    {
        super.toData(data);
        data.putInt("mode", this.mode);
    }

    @Override
    public void fromData(MapType data)
    {
        super.fromData(data);
        this.mode = data.getInt("mode");
    }
}
