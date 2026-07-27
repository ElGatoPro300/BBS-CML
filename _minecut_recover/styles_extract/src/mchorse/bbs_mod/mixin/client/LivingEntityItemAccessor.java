package mchorse.bbs_mod.mixin.client;

import net.minecraft.class_1309;
import net.minecraft.class_1799;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(class_1309.class)
public interface LivingEntityItemAccessor
{
    @Accessor("itemUseTimeLeft")
    void setItemUseTimeLeft(int itemUseTimeLeft);

    @Accessor("activeItemStack")
    void setActiveItemStack(class_1799 stack);
}
