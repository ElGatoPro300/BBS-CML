package mchorse.bbs_mod.forms.forms.shape.nodes;

import java.util.Arrays;
import java.util.List;

public class CoordinateNode extends ShapeNode
{
    public CoordinateNode()
    {}

    @Override
    public String getType()
    {
        return "coordinate";
    }

    @Override
    public List<String> getOutputs()
    {
        return Arrays.asList("x", "y", "z", "u", "v");
    }
}
