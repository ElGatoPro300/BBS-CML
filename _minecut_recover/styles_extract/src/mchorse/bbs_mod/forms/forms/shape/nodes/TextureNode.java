package mchorse.bbs_mod.forms.forms.shape.nodes;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.resources.Link;

import java.util.Arrays;
import java.util.List;

/**
 * Samples a texture at (u, v) coordinates and outputs individual RGBA channels.
 * Actual pixel sampling must be provided client-side via
 * ShapeGraphEvaluator.setTextureSampler(); the evaluator returns 0 by default.
 */
public class TextureNode extends ShapeNode
{
    public Link texture;

    @Override
    public String getType()
    {
        return "texture";
    }

    @Override
    public List<String> getInputs()
    {
        return Arrays.asList("u", "v");
    }

    @Override
    public List<String> getOutputs()
    {
        return Arrays.asList("r", "g", "b", "a", "rgba");
    }

    @Override
    public void toData(MapType data)
    {
        super.toData(data);
        data.putString("texture", this.texture == null ? "" : this.texture.toString());
    }

    @Override
    public void fromData(MapType data)
    {
        super.fromData(data);

        String t = data.getString("texture");

        /* Backward compat: old "path" field */
        if (t.isEmpty()) t = data.getString("path");

        this.texture = t.isEmpty() ? null : Link.create(t);
    }
}
