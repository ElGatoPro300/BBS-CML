package mchorse.bbs_mod.settings.values.mc;

import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import net.minecraft.item.ItemStack;

public class ValueItemStack extends BaseKeyframeFactoryValue<ItemStack>
{
    public ValueItemStack(String id)
    {
        super(id, KeyframeFactories.ITEM_STACK, ItemStack.EMPTY);
    }
}