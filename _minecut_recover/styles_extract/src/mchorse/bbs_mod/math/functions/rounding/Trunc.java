package mchorse.bbs_mod.math.functions.rounding;

import mchorse.bbs_mod.math.IExpression;
import mchorse.bbs_mod.math.MathBuilder;
import mchorse.bbs_mod.math.functions.NNFunction;

public class Trunc extends NNFunction
{
    public Trunc(MathBuilder builder, IExpression[] expressions, String name) throws Exception
    {
        super(builder, expressions, name);
    }

    @Override
    public int getRequiredArguments()
    {
        return 1;
    }

    @Override
    public double doubleValue()
    {
        double value = this.getArg(0).doubleValue();

        return value < 0 ? Math.ceil(value) : Math.floor(value);
    }
}