package mchorse.bbs_mod.camera.clips.screen.nodes;

import mchorse.bbs_mod.forms.forms.shape.nodes.ShapeNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** amplitude * sin(2π * frequency * time + phase) */
public class SineWaveNode extends ShapeNode
{
    @Override
    public String getType()
    {
        return "sine_wave";
    }

    @Override
    public List<String> getInputs()
    {
        return Arrays.asList("frequency", "amplitude", "phase");
    }

    @Override
    public List<String> getOutputs()
    {
        return Collections.singletonList("value");
    }
}
