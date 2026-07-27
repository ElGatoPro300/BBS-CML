package mchorse.bbs_mod.utils.interps;

public interface IInterp
{
    public static final InterpContext context = new InterpContext();

    public default boolean has(IInterp interp)
    {
        return this == interp;
    }

    public default float interpolate(float a, float b, float x)
    {
        return (float) this.interpolate(context.set(a, b, x));
    }

    public default double interpolate(double a, double b, double x)
    {
        return this.interpolate(context.set(a, b, x));
    }

    public double interpolate(InterpContext context);

    public String getKey();

    public int getKeyCode();
}
