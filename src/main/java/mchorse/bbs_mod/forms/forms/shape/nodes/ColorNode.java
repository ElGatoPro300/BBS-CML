package mchorse.bbs_mod.forms.forms.shape.nodes;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.colors.Color;

import java.util.Collections;
import java.util.List;

public class ColorNode extends ShapeNode
{
    public final Color color = new Color(1F, 1F, 1F, 1F);

    @Override
    public String getType()
    {
        return "color";
    }

    @Override
    public List<String> getOutputs()
    {
        return Collections.singletonList("rgba");
    }

    @Override
    public void toData(MapType data)
    {
        super.toData(data);
        data.putInt("c", this.color.getARGBColor());
    }

    @Override
    public void fromData(MapType data)
    {
        super.fromData(data);
        if (data.has("c"))
        {
            this.color.set(data.getInt("c"));
        }
    }
}
