package mchorse.bbs_mod.forms.forms.shape.nodes;

import java.util.Arrays;
import java.util.List;

/**
 * Remaps a value from [from_min, from_max] to [to_min, to_max].
 * Defaults: from range 0-1, to range 0-1 (no-op).
 */
public class RemapNode extends ShapeNode
{
    @Override
    public String getType()
    {
        return "remap";
    }

    @Override
    public List<String> getInputs()
    {
        return Arrays.asList("value", "from_min", "from_max", "to_min", "to_max");
    }

    @Override
    public List<String> getOutputs()
    {
        return Arrays.asList("result");
    }
}
