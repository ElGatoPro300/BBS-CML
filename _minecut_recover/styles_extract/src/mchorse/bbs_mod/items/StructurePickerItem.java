package mchorse.bbs_mod.items;

import net.minecraft.class_1269;
import net.minecraft.class_1799;
import net.minecraft.class_1821;
import net.minecraft.class_1834;
import net.minecraft.class_1838;

public class StructurePickerItem extends class_1821
{
    public StructurePickerItem(class_1793 settings)
    {
        super(class_1834.field_8922, settings);
    }

    @Override
    public boolean method_7886(class_1799 stack)
    {
        return true;
    }

    @Override
    public class_1269 method_7884(class_1838 context)
    {
        return class_1269.field_5812;
    }
}
