package mchorse.bbs_mod.items;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

public class StructurePickerItem extends Item
{
    public StructurePickerItem(Properties settings)
    {
        super(settings);
    }

    @Override
    public boolean isFoil(ItemStack stack)
    {
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        return InteractionResult.SUCCESS;
    }
}
