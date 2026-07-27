package mchorse.bbs_mod.items;

import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_1829;
import net.minecraft.class_1834;

public class MobKillerItem extends class_1829
{
    public MobKillerItem(class_1793 settings)
    {
        super(class_1834.field_8922, settings);
    }

    @Override
    public boolean method_7886(class_1799 stack)
    {
        return true;
    }

    @Override
    public boolean method_7873(class_1799 stack, class_1309 target, class_1309 attacker)
    {
        if (!target.method_37908().field_9236 && !(target instanceof class_1657))
        {
            target.method_5768();
        }

        return super.method_7873(stack, target, attacker);
    }
}
