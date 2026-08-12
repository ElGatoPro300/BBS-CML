package mchorse.bbs_mod.camera.clips.screen.nodes;

import mchorse.bbs_mod.forms.forms.shape.nodes.ShapeNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Square wave: 1 when (time * frequency) % 1 < duty, else 0. */
public class SquareWaveNode extends ShapeNode
{
    @Override
    public String getType()
    {
        return "square_wave";
    }

    @Override
    public List<String> getInputs()
    {
        return Arrays.asList("frequency", "duty");
    }

    @Override
    public List<String> getOutputs()
    {
        return Collections.singletonList("value");
    }
}
