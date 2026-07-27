package mchorse.bbs_mod.forms.forms.shape.nodes;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.math.molang.MolangParser;
import mchorse.bbs_mod.math.molang.expressions.MolangExpression;

import java.util.Arrays;
import java.util.List;

public class MathNode extends ShapeNode
{
    public static final MolangParser parser = new MolangParser();

    static
    {
        parser.register("a");
        parser.register("b");
    }

    public int operation = 0; // 0: add, 1: sub, 2: mul, 3: div
    public String expression = "";
    public MolangExpression compiled;

    public MathNode()
    {}

    @Override
    public String getType()
    {
        return "math";
    }

    @Override
    public List<String> getInputs()
    {
        return Arrays.asList("a", "b");
    }

    @Override
    public List<String> getOutputs()
    {
        return Arrays.asList("result");
    }

    @Override
    public void toData(MapType data)
    {
        super.toData(data);
        data.putInt("op", this.operation);
        
        if (this.operation == 8)
        {
            data.putString("expr", this.expression);
        }
    }

    @Override
    public void fromData(MapType data)
    {
        super.fromData(data);
        this.operation = data.getInt("op");
        
        if (data.has("expr"))
        {
            this.setExpression(data.getString("expr"));
        }
    }

    public void setExpression(String expression)
    {
        this.expression = expression;
        
        try
        {
            this.compiled = parser.parseExpression(expression);
        }
        catch (Exception e)
        {}
    }
}
