package mchorse.bbs_mod.forms.forms.shape.nodes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Hermite smooth interpolation between two edge values.
 * Formula: t = clamp((x-edge0)/(edge1-edge0)); result = t*t*(3-2*t)
 */
public class SmoothstepNode extends ShapeNode
{
    @Override
    public String getType()
    {
        return "smoothstep";
    }

    @Override
    public List<String> getInputs()
    {
        return Arrays.asList("edge0", "edge1", "x");
    }

    @Override
    public List<String> getOutputs()
    {
        return Collections.singletonList("result");
    }
}
