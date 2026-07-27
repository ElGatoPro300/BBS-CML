package mchorse.bbs_mod.camera.clips.screen.nodes;

import mchorse.bbs_mod.forms.forms.shape.nodes.ShapeNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Standalone color-grade effect node. Connect its "layer" output to a
 * LayerNode or directly to ScreenOutputNode to include it in the effect chain.
 *
 * Inputs:
 *   0  brightness  -1…1, 0=neutral
 *   1  contrast    -1…1, 0=neutral
 *   2  saturation  -1…1, 0=neutral
 */
public class ColorGradeEffectNode extends ShapeNode
{
    private static final List<String> INPUTS  = Arrays.asList("brightness", "contrast", "saturation");
    private static final List<String> OUTPUTS = Collections.singletonList("layer");

    @Override
    public String getType()
    {
        return "screen_color_grade";
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
