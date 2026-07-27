package mchorse.bbs_mod.forms.forms.shape.nodes;

import java.util.Arrays;
import java.util.List;

public class OutputNode extends ShapeNode
{
    public OutputNode()
    {}

    @Override
    public String getType()
    {
        return "output";
    }

    @Override
    public List<String> getInputs()
    {
        return Arrays.asList("result", "color", "material");
    }
}
