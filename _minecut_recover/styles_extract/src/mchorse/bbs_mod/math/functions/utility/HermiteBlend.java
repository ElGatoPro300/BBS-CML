package mchorse.bbs_mod.math.functions.utility;

import mchorse.bbs_mod.math.IExpression;
import mchorse.bbs_mod.math.MathBuilder;
import mchorse.bbs_mod.math.functions.NNFunction;

public class HermiteBlend extends NNFunction
{
    public HermiteBlend(MathBuilder builder, IExpression[] expressions, String name) throws Exception
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
        double x = this.getArg(0).doubleValue();

        return 3 * x * x - 2 * x * x * x;
    }
}