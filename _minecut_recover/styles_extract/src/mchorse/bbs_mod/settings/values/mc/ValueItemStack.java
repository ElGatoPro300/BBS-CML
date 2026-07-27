package mchorse.bbs_mod.settings.values.mc;

import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import net.minecraft.class_1799;

public class ValueItemStack extends BaseKeyframeFactoryValue<class_1799>
{
    public ValueItemStack(String id)
    {
        super(id, KeyframeFactories.ITEM_STACK, class_1799.field_8037);
    }
}