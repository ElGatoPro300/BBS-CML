package mchorse.bbs_mod.camera.clips.screen.nodes;

import mchorse.bbs_mod.forms.forms.shape.nodes.ShapeNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Standalone color overlay effect node. No output connections needed — the evaluator
 * detects this node in the graph and applies its inputs directly to the effect.
 *
 * Inputs:
 *   0  color  ARGB packed color for the overlay tint
 *   1  alpha  0…1 opacity
 */
public class OverlayEffectNode extends ShapeNode
{
    private static final List<String> INPUTS  = Arrays.asList("color", "alpha");
    private static final List<String> OUTPUTS = Collections.singletonList("layer");

    @Override
    public String getType()
    {
        return "screen_overlay";
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
