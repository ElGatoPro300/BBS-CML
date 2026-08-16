package mchorse.bbs_mod.settings.values.misc;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;

import org.joml.Vector3f;

public class ValueVector3f extends BaseValueBasic<Vector3f>
{
    public ValueVector3f(String id)
    {
        this(id, new Vector3f());
    }

    public ValueVector3f(String id, Vector3f value)
    {
        super(id, value);
    }

    public void set(float x, float y, float z)
    {
        this.preNotify();
        this.value.set(x, y, z);
        this.postNotify();
    }

    @Override
    public BaseType toData()
    {
        ListType list = new ListType();

        list.addFloat(this.value.x);
        list.addFloat(this.value.y);
        list.addFloat(this.value.z);

        return list;
    }

    @Override
    public void fromData(BaseType data)
    {
        if (data.isList())
        {
            ListType list = data.asList();

            if (list.size() >= 3)
            {
                this.value.set(list.getFloat(0), list.getFloat(1), list.getFloat(2));
            }
        }
    }
    
    @Override
    public String toString()
    {
        return this.value.toString();
    }
}
