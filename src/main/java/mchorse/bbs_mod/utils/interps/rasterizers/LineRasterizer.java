package mchorse.bbs_mod.utils.interps.rasterizers;

import mchorse.bbs_mod.utils.interps.Lerps;
import org.joml.Vector2d;
import org.joml.Vector2i;

public class LineRasterizer extends BaseRasterizer
{
    public Vector2d start;
    public Vector2d end;

    public LineRasterizer(Vector2d start, Vector2d end)
    {
        this.start = start;
        this.end = end;
    }

    @Override
    protected Vector2i calculate(float i)
    {
        double x = Lerps.lerp(this.start.x, this.end.x, i);
        double y = Lerps.lerp(this.start.y, this.end.y, i);

        return new Vector2i((int) Math.round(x), (int) Math.round(y));
    }
}