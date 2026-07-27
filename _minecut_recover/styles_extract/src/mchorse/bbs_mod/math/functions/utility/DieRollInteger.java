package mchorse.bbs_mod.math.functions.utility;

import mchorse.bbs_mod.math.IExpression;
import mchorse.bbs_mod.math.MathBuilder;

public class DieRollInteger extends DieRoll
{
    public DieRollInteger(MathBuilder builder, IExpression[] expressions, String name) throws Exception
    {
        super(builder, expressions, name);
    }

    @Override
    public double doubleValue()
    {
        return (int) super.doubleValue();
    }
}