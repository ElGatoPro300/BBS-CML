package mchorse.bbs_mod.forms.forms.shape.nodes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Combines individual 0-1 RGBA float channels into a packed RGBA color. */
public class CombineColorNode extends ShapeNode
{
    @Override
    public String getType()
    {
        return "combine_color";
    }

    @Override
    public List<String> getInputs()
    {
        return Arrays.asList("r", "g", "b", "a");
    }

    @Override
    public List<String> getOutputs()
    {
        return Collections.singletonList("rgba");
    }
}
