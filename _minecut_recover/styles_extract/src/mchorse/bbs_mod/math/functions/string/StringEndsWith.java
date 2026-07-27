package mchorse.bbs_mod.math.functions.string;

import mchorse.bbs_mod.math.IExpression;
import mchorse.bbs_mod.math.MathBuilder;
import mchorse.bbs_mod.math.functions.SNFunction;

public class StringEndsWith extends SNFunction
{
    public StringEndsWith(MathBuilder builder, IExpression[] expressions, String name) throws Exception
    {
        super(builder, expressions, name);
    }

    @Override
    public int getRequiredArguments()
    {
        return 2;
    }

    @Override
    public double doubleValue()
    {
        return this.getArg(0).stringValue().endsWith(this.getArg(1).stringValue()) ? 1 : 0;
    }
}
