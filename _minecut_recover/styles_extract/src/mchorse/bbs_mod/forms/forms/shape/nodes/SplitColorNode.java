package mchorse.bbs_mod.forms.forms.shape.nodes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Splits a packed RGBA color into individual 0-1 float channels. */
public class SplitColorNode extends ShapeNode
{
    @Override
    public String getType()
    {
        return "split_color";
    }

    @Override
    public List<String> getInputs()
    {
        return Collections.singletonList("rgba");
    }

    @Override
    public List<String> getOutputs()
    {
        return Arrays.asList("r", "g", "b", "a");
    }
}
