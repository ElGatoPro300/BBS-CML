package mchorse.bbs_mod.camera.clips.screen.nodes;

import mchorse.bbs_mod.forms.forms.shape.nodes.ShapeNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Applies pow(color, 1/gamma) per RGB channel. gamma default 1 = no change. */
public class GammaCorrectionNode extends ShapeNode
{
    @Override
    public String getType()
    {
        return "gamma_correction";
    }

    @Override
    public List<String> getInputs()
    {
        return Arrays.asList("color", "gamma");
    }

    @Override
    public List<String> getOutputs()
    {
        return Collections.singletonList("color");
    }
}
