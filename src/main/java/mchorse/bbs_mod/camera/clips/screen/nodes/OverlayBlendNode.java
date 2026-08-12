package mchorse.bbs_mod.camera.clips.screen.nodes;

import mchorse.bbs_mod.forms.forms.shape.nodes.ShapeNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Overlay blend mode per channel:
 *   if base < 0.5: 2 * base * blend
 *   else:          1 - 2*(1-base)*(1-blend)
 */
public class OverlayBlendNode extends ShapeNode
{
    @Override
    public String getType()
    {
        return "overlay_blend";
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
