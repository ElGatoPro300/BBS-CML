package mchorse.bbs_mod.utils;

import java.util.function.Function;

public class Factor
{
    private final Function<Integer, Double> function;

    private int x;
    private int min;
    private int max;

    private double cachedValue;

    public Factor()
    {
        this(10, 0, 50, (x) -> Math.pow(x, 2) / 50D);
    }

    public Factor(int x, int min, int max, Function<Integer, Double> function)
    {
        this(min, max, function);

        this.setX(x);
    }

    public Factor(int min, int max, Function<Integer, Double> function)
    {
        this.function = function;
        this.min = min;
        this.max = max;
    }

    public int getX()
    {
        return this.x;
    }

    public void setX(int x)
    {
        this.x = MathUtils.clamp(x, this.min, this.max);

        this.updateValue();
    }

    public void addX(int x)
    {
        this.setX(this.x + Integer.compare(x, 0));
    }

    private void updateValue()
    {
        this.cachedValue = this.function.apply(this.x);
    }

    public double getValue()
    {
        return this.cachedValue;
    }
}