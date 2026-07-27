package mchorse.bbs_mod.forms.forms.shape;

import mchorse.bbs_mod.data.types.MapType;

public class ShapeConnection
{
    public int outputNodeId;
    public int outputIndex;
    public int inputNodeId;
    public int inputIndex;

    public ShapeConnection()
    {}

    public ShapeConnection(int outputNodeId, int outputIndex, int inputNodeId, int inputIndex)
    {
        this.outputNodeId = outputNodeId;
        this.outputIndex = outputIndex;
        this.inputNodeId = inputNodeId;
        this.inputIndex = inputIndex;
    }

    public void toData(MapType data)
    {
        data.putInt("out_id", this.outputNodeId);
        data.putInt("out_idx", this.outputIndex);
        data.putInt("in_id", this.inputNodeId);
        data.putInt("in_idx", this.inputIndex);
    }

    public void fromData(MapType data)
    {
        this.outputNodeId = data.getInt("out_id");
        this.outputIndex = data.getInt("out_idx");
        this.inputNodeId = data.getInt("in_id");
        this.inputIndex = data.getInt("in_idx");
    }
}
