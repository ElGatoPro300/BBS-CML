package mchorse.bbs_mod.forms.forms.shape;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.forms.shape.nodes.BumpNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.ClampNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.ColorNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.CombineColorNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.CommentNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.CoordinateNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.FlowNoiseNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.InvertNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.IrisAttributeNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.IrisShaderNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.MathNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.MixColorNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.NoiseNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.OutputNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.RemapNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.ShapeNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.SmoothstepNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.SplitColorNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.TextureNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.TimeNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.TriggerNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.ValueNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.VectorMathNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.VoronoiNode;

import java.util.ArrayList;
import java.util.List;

public class ShapeFormGraph implements INodeGraph
{
    public final List<ShapeNode> nodes = new ArrayList<>();
    public final List<ShapeConnection> connections = new ArrayList<>();

    private int idCounter = 0;

    @Override
    public List<ShapeNode> getNodes()
    {
        return this.nodes;
    }

    @Override
    public List<ShapeConnection> getConnections()
    {
        return this.connections;
    }

    @Override
    public void addNode(ShapeNode node)
    {
        if (node.id == 0)
        {
            node.id = ++this.idCounter;
        }
        else
        {
            this.idCounter = Math.max(this.idCounter, node.id);
        }

        this.nodes.add(node);
    }

    @Override
    public void removeNode(ShapeNode node)
    {
        this.nodes.remove(node);
        this.connections.removeIf(c -> c.inputNodeId == node.id || c.outputNodeId == node.id);
    }

    @Override
    public void connect(int outId, int outIdx, int inId, int inIdx)
    {
        /* Only one connection per input slot */
        this.connections.removeIf(c -> c.inputNodeId == inId && c.inputIndex == inIdx);
        this.connections.add(new ShapeConnection(outId, outIdx, inId, inIdx));
    }

    @Override
    public void bringToFront(ShapeNode node)
    {
        if (this.nodes.remove(node))
        {
            this.nodes.add(node);
        }
    }

    public void toData(MapType data)
    {
        ListType nodesList = new ListType();

        for (ShapeNode node : this.nodes)
        {
            MapType nodeData = new MapType();
            node.toData(nodeData);
            nodesList.add(nodeData);
        }

        data.put("nodes", nodesList);

        ListType connectionsList = new ListType();

        for (ShapeConnection c : this.connections)
        {
            MapType cData = new MapType();
            c.toData(cData);
            connectionsList.add(cData);
        }

        data.put("connections", connectionsList);
        data.putInt("counter", this.idCounter);
    }

    public void fromData(MapType data)
    {
        this.nodes.clear();
        this.connections.clear();
        this.idCounter = data.getInt("counter");

        ListType nodesList = data.getList("nodes");

        for (BaseType element : nodesList)
        {
            if (element instanceof MapType)
            {
                MapType nodeData = (MapType) element;
                String type = nodeData.getString("type");
                ShapeNode node = this.createNode(type);

                if (node != null)
                {
                    node.fromData(nodeData);
                    this.nodes.add(node);
                }
            }
        }

        ListType connectionsList = data.getList("connections");

        for (BaseType element : connectionsList)
        {
            if (element instanceof MapType)
            {
                ShapeConnection c = new ShapeConnection();
                c.fromData((MapType) element);
                this.connections.add(c);
            }
        }
    }

    @Override
    public ShapeNode createNode(String type)
    {
        /* Input / value nodes */
        if ("output".equals(type)) return new OutputNode();
        if ("coordinate".equals(type)) return new CoordinateNode();
        if ("value".equals(type)) return new ValueNode();
        if ("time".equals(type)) return new TimeNode();
        if ("color".equals(type)) return new ColorNode();
        if ("texture".equals(type)) return new TextureNode();

        /* Math nodes */
        if ("invert".equals(type)) return new InvertNode();
        if ("math".equals(type)) return new MathNode();
        if ("vector_math".equals(type)) return new VectorMathNode();
        if ("remap".equals(type)) return new RemapNode();
        if ("clamp".equals(type)) return new ClampNode();
        if ("smoothstep".equals(type)) return new SmoothstepNode();

        /* Color nodes */
        if ("mix_color".equals(type)) return new MixColorNode();
        if ("split_color".equals(type)) return new SplitColorNode();
        if ("combine_color".equals(type)) return new CombineColorNode();

        /* Noise nodes */
        if ("noise".equals(type)) return new NoiseNode();
        if ("voronoi".equals(type)) return new VoronoiNode();
        if ("flow_noise".equals(type)) return new FlowNoiseNode();

        /* Utility nodes */
        if ("trigger".equals(type)) return new TriggerNode();
        if ("bump".equals(type)) return new BumpNode();
        if ("comment".equals(type)) return new CommentNode();

        /* Integration nodes */
        if ("iris_shader".equals(type)) return new IrisShaderNode();
        if ("iris_attribute".equals(type)) return new IrisAttributeNode();

        return null;
    }
}
