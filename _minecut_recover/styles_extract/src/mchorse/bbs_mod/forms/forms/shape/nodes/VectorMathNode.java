package mchorse.bbs_mod.forms.forms.shape.nodes;

import mchorse.bbs_mod.data.types.MapType;

import java.util.Arrays;
import java.util.List;

public class VectorMathNode extends ShapeNode
{
    public int operation = 0;

    public VectorMathNode()
    {}

    @Override
    public String getType()
    {
        return "vector_math";
    }

    @Override
    public List<String> getInputs()
    {
        return Arrays.asList("ax", "ay", "az", "bx", "by", "bz");
    }

    @Override
    public List<String> getOutputs()
    {
        return Arrays.asList("x", "y", "z", "w");
    }

    @Override
    public void toData(MapType data)
    {
        super.toData(data);
        data.putInt("op", this.operation);
    }

    @Override
    public void fromData(MapType data)
    {
        super.fromData(data);
        this.operation = data.getInt("op");
    }
}
