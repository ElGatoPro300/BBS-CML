package mchorse.bbs_mod.settings.values.core;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.StringType;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;

public class ValueEnum<E extends Enum<E>> extends BaseValueBasic<E>
{
    private Class<E> enumClass;

    public ValueEnum(String id, Class<E> enumClass, E value)
    {
        super(id, value);
        this.enumClass = enumClass;
    }

    @Override
    public BaseType toData()
    {
        return new StringType(this.value.name());
    }

    @Override
    public void fromData(BaseType data)
    {
        if (data.isString())
        {
            try
            {
                this.set(Enum.valueOf(this.enumClass, data.asString()));
            }
            catch (Exception e)
            {
                /* Ignore invalid values */
            }
        }
    }
}
