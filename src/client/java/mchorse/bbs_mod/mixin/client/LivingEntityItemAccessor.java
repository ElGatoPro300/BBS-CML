package mchorse.bbs_mod.mixin.client;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityItemAccessor
{
    @Accessor("itemUseTimeLeft")
    void setItemUseTimeLeft(int itemUseTimeLeft);

    @Accessor("activeItemStack")
    void setActiveItemStack(ItemStack stack);
}
