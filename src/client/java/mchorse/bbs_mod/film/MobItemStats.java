package mchorse.bbs_mod.film;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class MobItemStats
{
    public boolean usingItem;
    public int itemUseElapsed;
    public InteractionHand activeHand = InteractionHand.MAIN_HAND;
    public ItemStack mainHand = ItemStack.EMPTY;
    public ItemStack offHand = ItemStack.EMPTY;
}
