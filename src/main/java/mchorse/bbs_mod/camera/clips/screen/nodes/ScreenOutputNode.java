package mchorse.bbs_mod.camera.clips.screen.nodes;

import mchorse.bbs_mod.forms.forms.shape.nodes.ShapeNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Terminal sink node for the screen effect graph. Connect effect nodes or a
 * LayerNode to its single "layer" input to declare which effects are active.
 * The evaluator scans the graph for effect nodes regardless of connections, so
 * the connection is visual/organisational — but it clearly shows intent.
 */
public class ScreenOutputNode extends ShapeNode
{
    private static final List<String> INPUTS = Collections.singletonList("layer");

    @Override
    public String getType()
    {
        return "screen_output";
    }

    @Override
    public List<String> getInputs()
    {
        return INPUTS;
    }

    @Override
    public List<String> getOutputs()
    {
        return Collections.emptyList();
    }
}
