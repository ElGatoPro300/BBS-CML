package mchorse.bbs_mod.forms.forms.shape;

import mchorse.bbs_mod.forms.forms.shape.nodes.ShapeNode;

import java.util.List;

/**
 * Abstraction over node graph data. Implement this interface on any graph to
 * make it compatible with UIShapeNodeEditor without coupling to ShapeFormGraph.
 */
public interface INodeGraph
{
    List<ShapeNode> getNodes();

    List<ShapeConnection> getConnections();

    ShapeNode createNode(String type);

    void addNode(ShapeNode node);

    void removeNode(ShapeNode node);

    void connect(int outId, int outIdx, int inId, int inIdx);

    /** Move a node to the end of the render list so it draws on top. */
    void bringToFront(ShapeNode node);
}
