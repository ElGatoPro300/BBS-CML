package mchorse.bbs_mod.camera.clips.screen.nodes;

import mchorse.bbs_mod.forms.forms.shape.nodes.ShapeNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Outputs screen UV coordinates (0…1).
 * When evaluated with (x, y), x=u and y=v.
 * Useful for building UV-dependent expressions (e.g. distance to center).
 */
public class ScreenUVNode extends ShapeNode
{
    @Override
    public String getType()
    {
        return "screen_uv";
    }

    @Override
    public List<String> getInputs()
    {
        return Collections.emptyList();
    }

    @Override
    public List<String> getOutputs()
    {
        return Arrays.asList("u", "v");
    }
}
