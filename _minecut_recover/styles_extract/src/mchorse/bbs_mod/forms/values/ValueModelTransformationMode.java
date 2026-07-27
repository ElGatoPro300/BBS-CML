package mchorse.bbs_mod.forms.values;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.StringType;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import net.minecraft.class_811;

public class ValueModelTransformationMode extends BaseValueBasic<class_811>
{
    public ValueModelTransformationMode(String id, class_811 value)
    {
        super(id, value);
    }

    @Override
    public BaseType toData()
    {
        return new StringType((this.value == null ? class_811.field_4315 : this.value).method_15434());
    }

    @Override
    public void fromData(BaseType data)
    {
        String string = data.isString() ? data.asString() : "";

        this.set(class_811.field_4315);

        for (class_811 value : class_811.values())
        {
            if (value.method_15434().equals(string))
            {
                this.set(value);

                break;
            }
        }
    }
}