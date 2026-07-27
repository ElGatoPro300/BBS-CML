package mchorse.bbs_mod.math.molang.functions;

import mchorse.bbs_mod.math.IExpression;
import mchorse.bbs_mod.math.MathBuilder;
import mchorse.bbs_mod.math.functions.trig.Acos;

public class AcosDegrees extends Acos
{
    public AcosDegrees(MathBuilder builder, IExpression[] expressions, String name) throws Exception
    {
        super(builder, expressions, name);
    }

    @Override
    public double doubleValue()
    {
        return super.doubleValue() / Math.PI * 180;
    }
}