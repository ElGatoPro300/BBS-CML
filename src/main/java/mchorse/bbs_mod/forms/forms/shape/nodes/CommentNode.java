package mchorse.bbs_mod.forms.forms.shape.nodes;

import mchorse.bbs_mod.data.types.MapType;

public class CommentNode extends ShapeNode
{
    public String title = "Comment";
    public String comment = "";
    public float width = 150;
    public float height = 100;

    @Override
    public String getType()
    {
        return "comment";
    }

    @Override
    public void toData(MapType data)
    {
        super.toData(data);
        data.putString("title", this.title);
        data.putString("comment", this.comment);
        data.putFloat("w", this.width);
        data.putFloat("h", this.height);
    }

    @Override
    public void fromData(MapType data)
    {
        super.fromData(data);
        if (data.has("title")) this.title = data.getString("title");
        if (data.has("comment")) this.comment = data.getString("comment");
        if (data.has("w")) this.width = data.getFloat("w");
        if (data.has("h")) this.height = data.getFloat("h");
    }
}
