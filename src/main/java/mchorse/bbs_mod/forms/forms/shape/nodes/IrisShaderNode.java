package mchorse.bbs_mod.forms.forms.shape.nodes;

import mchorse.bbs_mod.data.types.MapType;

import java.util.Collections;
import java.util.List;

public class IrisShaderNode extends ShapeNode
{
    public String uniform = "";

    @Override
    public String getType()
    {
        return "iris_shader";
    }

    @Override
    public List<String> getInputs()
    {
        return Collections.singletonList("value");
    }

    @Override
    public void toData(MapType data)
    {
        super.toData(data);
        data.putString("uniform", this.uniform);
    }

    @Override
    public void fromData(MapType data)
    {
        super.fromData(data);
        this.uniform = data.getString("uniform");
    }
}
