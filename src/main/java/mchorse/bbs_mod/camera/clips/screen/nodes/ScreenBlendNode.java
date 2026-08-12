package mchorse.bbs_mod.camera.clips.screen.nodes;

import mchorse.bbs_mod.forms.forms.shape.nodes.ShapeNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Screen blend mode: result = 1 - (1-base)(1-blend) per channel. */
public class ScreenBlendNode extends ShapeNode
{
    @Override
    public String getType()
    {
        return "screen_blend";
    }

    @Override
    public List<String> getInputs()
    {
        return Arrays.asList("base_color", "blend_color");
    }

    @Override
    public List<String> getOutputs()
    {
        return Collections.singletonList("color");
    }
}
