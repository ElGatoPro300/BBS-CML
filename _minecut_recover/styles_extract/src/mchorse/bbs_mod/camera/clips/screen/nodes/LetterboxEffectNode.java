package mchorse.bbs_mod.camera.clips.screen.nodes;

import mchorse.bbs_mod.forms.forms.shape.nodes.ShapeNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Standalone letterbox effect node. No output connections needed — the evaluator
 * detects this node in the graph and applies its inputs directly to the effect.
 *
 * Inputs:
 *   0  size   0…1, fraction of screen height per bar
 *   1  color  ARGB packed color (default opaque black)
 */
public class LetterboxEffectNode extends ShapeNode
{
    private static final List<String> INPUTS  = Arrays.asList("size", "color");
    private static final List<String> OUTPUTS = Collections.singletonList("layer");

    @Override
    public String getType()
    {
        return "screen_letterbox";
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
