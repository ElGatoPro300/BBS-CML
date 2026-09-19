package mchorse.bbs_mod.forms.forms.shape.nodes;

import mchorse.bbs_mod.data.types.MapType;

import java.util.Arrays;
import java.util.List;

public class FlowNoiseNode extends ShapeNode
{
    public int seed = 0;

    public FlowNoiseNode()
    {}

    @Override
    public String getType()
    {
        return "flow_noise";
    }

    @Override
    public List<String> getInputs()
    {
        return Arrays.asList("x", "y", "z", "scale", "speed", "taper");
    }

    @Override
    public List<String> getOutputs()
    {
        return Arrays.asList("result");
    }

    @Override
    public void toData(MapType data)
    {
        super.toData(data);
        data.putInt("seed", this.seed);
    }

    @Override
    public void fromData(MapType data)
    {
        super.fromData(data);
        if (data.has("seed")) this.seed = data.getInt("seed");
    }
}
