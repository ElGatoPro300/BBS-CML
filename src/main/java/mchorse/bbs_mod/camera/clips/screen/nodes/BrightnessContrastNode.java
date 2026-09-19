package mchorse.bbs_mod.camera.clips.screen.nodes;

import mchorse.bbs_mod.forms.forms.shape.nodes.ShapeNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Adjusts brightness and contrast of a color.
 * brightness: -1…1, 0=neutral.
 * contrast:   -1…1, 0=neutral.
 */
public class BrightnessContrastNode extends ShapeNode
{
    @Override
    public String getType()
    {
        return "brightness_contrast";
    }

    @Override
    public List<String> getInputs()
    {
        return Arrays.asList("color", "brightness", "contrast");
    }

    @Override
    public List<String> getOutputs()
    {
        return Collections.singletonList("color");
    }
}
