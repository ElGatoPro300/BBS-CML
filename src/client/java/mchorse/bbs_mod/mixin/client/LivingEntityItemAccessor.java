package mchorse.bbs_mod.mixin.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityItemAccessor
{
    @Accessor("useItemRemaining")
    void setItemUseTimeLeft(int itemUseTimeLeft);

    @Accessor("useItem")
    void setActiveItemStack(ItemStack stack);
}
