package mchorse.bbs_mod.camera.clips.screen.nodes;

import mchorse.bbs_mod.forms.forms.shape.nodes.ShapeNode;

import java.util.Arrays;
import java.util.List;

/**
 * Generates a time-based glitch pulse.
 * strength: 0…1, controls glitch intensity.
 * frequency: glitch pulses per second.
 * Outputs: active (0 or 1), offset_x, offset_y (-1…1 random offset).
 */
public class GlitchNode extends ShapeNode
{
    @Override
    public String getType()
    {
        return "glitch";
    }

    @Override
    public List<String> getInputs()
    {
        return Arrays.asList("strength", "frequency");
    }

    @Override
    public List<String> getOutputs()
    {
        return Arrays.asList("active", "offset_x", "offset_y");
    }
}
