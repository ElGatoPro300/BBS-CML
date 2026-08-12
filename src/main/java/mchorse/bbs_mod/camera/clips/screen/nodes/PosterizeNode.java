package mchorse.bbs_mod.camera.clips.screen.nodes;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.forms.shape.nodes.ShapeNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Quantizes a value to N discrete steps: floor(value * steps) / steps.
 * Also works on packed ARGB colors when mode=1 (quantize each channel).
 */
public class PosterizeNode extends ShapeNode
{
    public int mode = 0; /* 0 = scalar, 1 = color */

    @Override
    public String getType()
    {
        return "posterize";
    }

    @Override
    public List<String> getInputs()
    {
        return Arrays.asList("value", "steps");
    }

    @Override
    public List<String> getOutputs()
    {
        return Collections.singletonList("result");
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
