package mchorse.bbs_mod.forms.forms.shape;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.base.BaseValue;

public class ValueShapeGraph extends BaseValue
{
    private ShapeFormGraph graph;

    public ValueShapeGraph(String id)
    {
        this(id, new ShapeFormGraph());
    }

    public ValueShapeGraph(String id, ShapeFormGraph graph)
    {
        super(id);
        this.graph = graph;
    }

    public ShapeFormGraph get()
    {
        return this.graph;
    }

    public void set(ShapeFormGraph graph)
    {
        this.graph = graph;
        this.postNotify();
    }

    @Override
    public BaseType toData()
    {
        MapType data = new MapType();
        this.graph.toData(data);
        return data;
    }

    @Override
    public void fromData(BaseType data)
    {
        if (data.isMap())
        {
            this.graph.fromData(data.asMap());
        }
    }
}
