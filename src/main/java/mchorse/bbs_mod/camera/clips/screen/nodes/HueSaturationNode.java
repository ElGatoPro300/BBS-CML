package mchorse.bbs_mod.camera.clips.screen.nodes;

import mchorse.bbs_mod.forms.forms.shape.nodes.ShapeNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Shifts hue and scales saturation of a color.
 * hue_shift: full turns (0=none, 1=360°).
 * saturation: multiplier (1=unchanged, 0=grayscale, 2=double).
 */
public class HueSaturationNode extends ShapeNode
{
    @Override
    public String getType()
    {
        return "hue_saturation";
    }

    @Override
    public List<String> getInputs()
    {
        return Arrays.asList("color", "hue_shift", "saturation");
    }

    @Override
    public List<String> getOutputs()
    {
        return Collections.singletonList("color");
    }
}
