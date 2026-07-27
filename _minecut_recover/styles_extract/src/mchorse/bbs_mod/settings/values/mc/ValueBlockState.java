package mchorse.bbs_mod.settings.values.mc;

import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import net.minecraft.class_2246;
import net.minecraft.class_2680;

public class ValueBlockState extends BaseKeyframeFactoryValue<class_2680>
{
    public ValueBlockState(String id)
    {
        super(id, KeyframeFactories.BLOCK_STATE, class_2246.field_10124.method_9564());
    }
}