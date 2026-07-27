package mchorse.bbs_mod.forms.forms.shape.nodes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Clamps a value between min and max. Defaults: min=0, max=1. */
public class ClampNode extends ShapeNode
{
    @Override
    public String getType()
    {
        return "clamp";
    }

    @Override
    public List<String> getInputs()
    {
        return Arrays.asList("value", "min", "max");
    }

    @Override
    public List<String> getOutputs()
    {
        return Collections.singletonList("result");
    }
}
