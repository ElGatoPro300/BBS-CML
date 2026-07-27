package mchorse.bbs_mod.camera.clips.screen.nodes;

import mchorse.bbs_mod.forms.forms.shape.nodes.ShapeNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Layer combiner node. Accepts up to 4 effect-node "layer" outputs and merges
 * them into a single "layer" output that can be wired into ScreenOutputNode (or
 * another LayerNode for deeper nesting).
 *
 * The node has no computational role — the evaluator scans the graph directly.
 * It exists purely to organise the visual layout of the effect chain.
 */
public class LayerNode extends ShapeNode
{
    private static final List<String> INPUTS  = Arrays.asList("layer_1", "layer_2", "layer_3", "layer_4");
    private static final List<String> OUTPUTS = Collections.singletonList("layer");

    @Override
    public String getType()
    {
        return "screen_layer";
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
