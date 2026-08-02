package mchorse.bbs_mod.cubic.model.fbx;

import java.util.ArrayList;
import java.util.List;

public class FBXNode
{
    public String name;
    public List<Object> properties = new ArrayList<Object>();
    public List<FBXNode> children = new ArrayList<FBXNode>();

    public FBXNode(String name)
    {
        this.name = name;
    }

    public FBXNode child(String name)
    {
        for (FBXNode child : this.children)
        {
            if (child.name.equals(name))
            {
                return child;
            }
        }

        return null;
    }

    public List<FBXNode> childrenNamed(String name)
    {
        List<FBXNode> list = new ArrayList<FBXNode>();

        for (FBXNode child : this.children)
        {
            if (child.name.equals(name))
            {
                list.add(child);
            }
        }

        return list;
    }

    public String getString(int index)
    {
        Object o = this.properties.get(index);

        return o == null ? "" : o.toString();
    }

    public long getLong(int index)
    {
        Object o = this.properties.get(index);

        if (o instanceof Number)
        {
            return ((Number) o).longValue();
        }

        return Long.parseLong(o.toString().trim());
    }

    public double getDouble(int index)
    {
        Object o = this.properties.get(index);

        if (o instanceof Number)
        {
            return ((Number) o).doubleValue();
        }

        return Double.parseDouble(o.toString().trim());
    }


    public double[] asDoubleArray()
    {
        for (Object o : this.properties)
        {
            if (o instanceof double[])
            {
                return (double[]) o;
            }

            if (o instanceof float[])
            {
                float[] src = (float[]) o;
                double[] out = new double[src.length];

                for (int i = 0; i < src.length; i++)
                {
                    out[i] = src[i];
                }

                return out;
            }

            if (o instanceof long[])
            {
                long[] src = (long[]) o;
                double[] out = new double[src.length];

                for (int i = 0; i < src.length; i++)
                {
                    out[i] = src[i];
                }

                return out;
            }

            if (o instanceof int[])
            {
                int[] src = (int[]) o;
                double[] out = new double[src.length];

                for (int i = 0; i < src.length; i++)
                {
                    out[i] = src[i];
                }

                return out;
            }
        }

        FBXNode a = this.child("a");

        if (a != null)
        {
            double[] out = new double[a.properties.size()];

            for (int i = 0; i < out.length; i++)
            {
                out[i] = ((Number) a.properties.get(i)).doubleValue();
            }

            return out;
        }

        return new double[0];
    }

    public int[] asIntArray()
    {
        for (Object o : this.properties)
        {
            if (o instanceof int[])
            {
                return (int[]) o;
            }

            if (o instanceof long[])
            {
                long[] src = (long[]) o;
                int[] out = new int[src.length];

                for (int i = 0; i < src.length; i++)
                {
                    out[i] = (int) src[i];
                }

                return out;
            }
        }

        FBXNode a = this.child("a");

        if (a != null)
        {
            int[] out = new int[a.properties.size()];

            for (int i = 0; i < out.length; i++)
            {
                out[i] = ((Number) a.properties.get(i)).intValue();
            }

            return out;
        }

        return new int[0];
    }
}
