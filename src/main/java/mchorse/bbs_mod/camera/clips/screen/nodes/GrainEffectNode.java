package mchorse.bbs_mod.camera.clips.screen.nodes;

import mchorse.bbs_mod.forms.forms.shape.nodes.ShapeNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Standalone film grain effect node. No output connections needed — the evaluator
 * detects this node in the graph and applies its inputs directly to the effect.
 *
 * Inputs:
 *   0  strength  0=off, higher=more grain
 *   1  size      pixel size of grain (default 1)
 */
public class GrainEffectNode extends ShapeNode
{
    private static final List<String> INPUTS  = Arrays.asList("strength", "size");
    private static final List<String> OUTPUTS = Collections.singletonList("layer");

    @Override
    public String getType()
    {
        return "screen_grain";
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
