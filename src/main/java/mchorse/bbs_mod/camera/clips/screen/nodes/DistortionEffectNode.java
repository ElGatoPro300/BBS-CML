package mchorse.bbs_mod.camera.clips.screen.nodes;

import mchorse.bbs_mod.forms.forms.shape.nodes.ShapeNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Standalone UV-distortion effect node. No output connections needed — the
 * evaluator detects this node in the graph and shifts the sample UV in the
 * shader by (offset_x, offset_y), expressed as fractions of screen size.
 *
 * Inputs:
 *   0  offset_x   horizontal shift  (-1 … 1)
 *   1  offset_y   vertical shift    (-1 … 1)
 *
 * Connect GlitchNode outputs (offset_x / offset_y) here for a pulse-glitch
 * look, or drive inputs with SineWave for smooth warping.
 */
public class DistortionEffectNode extends ShapeNode
{
    private static final List<String> INPUTS  = Arrays.asList("offset_x", "offset_y");
    private static final List<String> OUTPUTS = Collections.singletonList("layer");

    @Override
    public String getType()
    {
        return "screen_distortion";
    }

    @Override
    public List<String> getInputs()
    {
        return INPUTS;
    }

    @Override
    public List<String> getOutputs()
    {
        return OUTPUTS;
    }
}
