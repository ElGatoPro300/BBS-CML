package mchorse.bbs_mod.graphics.line;

public class LinePoint <T>
{
    public float x;
    public float y;
    public T user;

    public LinePoint(float x, float y, T user)
    {
        this.x = x;
        this.y = y;
        this.user = user;
    }
}