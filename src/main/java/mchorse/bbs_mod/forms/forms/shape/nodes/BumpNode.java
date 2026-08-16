package mchorse.bbs_mod.forms.forms.shape.nodes;

import java.util.Arrays;
import java.util.List;

public class BumpNode extends ShapeNode
{
    public BumpNode()
    {}

    @Override
    public String getType()
    {
        return "bump";
    }

    @Override
    public List<String> getInputs()
    {
        return Arrays.asList("height", "strength", "distance");
    }

    @Override
    public List<String> getOutputs()
    {
        return Arrays.asList("x", "y", "z");
    }
}
