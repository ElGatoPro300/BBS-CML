package mchorse.bbs_mod.utils.interps.rasterizers;

import mchorse.bbs_mod.utils.interps.Lerps;
import org.joml.Vector2d;
import org.joml.Vector2i;

public class QuadraticBezierRasterizer extends BaseRasterizer
{
    public Vector2d a;
    public Vector2d b;
    public Vector2d control;

    public QuadraticBezierRasterizer(Vector2d a, Vector2d b, Vector2d control)
    {
        this.a = a;
        this.b = b;
        this.control = control;
    }

    @Override
    protected Vector2i calculate(float i)
    {
        double dx = Lerps.quadBezier(this.a.x, this.control.x, this.b.x, i);
        double dy = Lerps.quadBezier(this.a.y, this.control.y, this.b.y, i);

        return new Vector2i((int) Math.round(dx), (int) Math.round(dy));
    }
}