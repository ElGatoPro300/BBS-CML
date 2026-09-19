package mchorse.bbs_mod.camera.clips.screen.nodes;

import mchorse.bbs_mod.forms.forms.shape.nodes.ShapeNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Levels adjustment: (color - black) / (white - black) ^ (1/mid_gamma).
 * black: 0…1 (default 0).
 * mid_gamma: 0.1…10 (default 1 = linear).
 * white: 0…1 (default 1).
 */
public class LevelsNode extends ShapeNode
{
    @Override
    public String getType()
    {
        return "levels";
    }

    @Override
    public List<String> getInputs()
    {
        return Arrays.asList("color", "black", "mid_gamma", "white");
    }

    @Override
    public List<String> getOutputs()
    {
        return Collections.singletonList("color");
    }
}
