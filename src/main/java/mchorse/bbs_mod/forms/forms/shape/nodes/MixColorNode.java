package mchorse.bbs_mod.forms.forms.shape.nodes;

import java.util.Arrays;
import java.util.List;

public class MixColorNode extends ShapeNode
{
    public MixColorNode()
    {}

    @Override
    public String getType()
    {
        return "mix_color";
    }

    @Override
    public List<String> getInputs()
    {
        return Arrays.asList("a", "b", "factor");
    }

    @Override
    public List<String> getOutputs()
    {
        return Arrays.asList("result");
    }
}
