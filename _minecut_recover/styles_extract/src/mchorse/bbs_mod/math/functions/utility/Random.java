package mchorse.bbs_mod.math.functions.utility;

import mchorse.bbs_mod.math.IExpression;
import mchorse.bbs_mod.math.MathBuilder;
import mchorse.bbs_mod.math.functions.NNFunction;

public class Random extends NNFunction
{
    public java.util.Random random;

    public Random(MathBuilder builder, IExpression[] expressions, String name) throws Exception
    {
        super(builder, expressions, name);

        this.random = new java.util.Random();
    }

    @Override
    public double doubleValue()
    {
        double random;

        if (this.args.length >= 3)
        {
            this.random.setSeed((long) this.getArg(2).doubleValue());
            random = this.random.nextDouble();
        }
        else
        {
            random = Math.random();
        }

        if (this.args.length >= 2)
        {
            double a = this.getArg(0).doubleValue();
            double b = this.getArg(1).doubleValue();

            double min = Math.min(a, b);
            double max = Math.max(a, b);

            random = random * (max - min) + min;
        }
        else if (this.args.length >= 1)
        {
            random = random * this.getArg(0).doubleValue();
        }

        return random;
    }
}