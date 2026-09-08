package mchorse.bbs_mod.film;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class MobItemStats
{
    public boolean usingItem;
    public int itemUseElapsed;
    public Hand activeHand = Hand.MAIN_HAND;
    public ItemStack mainHand = ItemStack.EMPTY;
    public ItemStack offHand = ItemStack.EMPTY;
}
