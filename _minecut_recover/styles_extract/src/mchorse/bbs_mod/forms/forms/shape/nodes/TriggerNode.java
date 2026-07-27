package mchorse.bbs_mod.forms.forms.shape.nodes;

import mchorse.bbs_mod.data.types.MapType;

import java.util.Arrays;
import java.util.List;

public class TriggerNode extends ShapeNode
{
    public int mode = 0; // 0 = GREATER, 1 = LESS, 2 = EQUAL, 3 = NOT_EQUAL, 4 = PULSE

    @Override
    public String getType()
    {
        return "trigger";
    }

    @Override
    public List<String> getInputs()
    {
        return Arrays.asList("input", "target", "range");
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
        
        data.putInt("mode", this.mode);
    }

    @Override
    public void fromData(MapType data)
    {
        super.fromData(data);
        
        this.mode = data.getInt("mode");
    }
}
