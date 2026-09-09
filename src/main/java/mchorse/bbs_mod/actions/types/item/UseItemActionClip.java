package mchorse.bbs_mod.actions.types.item;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.items.GunItem;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class UseItemActionClip extends ItemActionClip
{
    public final ValueInt useTicks = new ValueInt("use_ticks", 0, 0, Integer.MAX_VALUE);

    public UseItemActionClip()
    {
        super();

        this.add(this.useTicks);
    }

    @Override
    public void applyAction(LivingEntity actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        InteractionHand hand = this.hand.get() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;

        GunItem.actor = actor;

        this.applyPositionRotation(player, replay, tick);
        ItemStack copy = this.itemStack.get().copy();
        int maxUseTime = copy.getUseDuration(player);
        int used = this.useTicks.get();

        player.setItemInHand(hand, copy);
        copy.use(player.level(), player, hand);

        if (used > 0 && maxUseTime > 0)
        {
            int remaining = Math.max(0, maxUseTime - used);
            copy.releaseUsing(player.level(), player, remaining);
            player.releaseUsingItem();
        }

        player.setItemInHand(hand, ItemStack.EMPTY);

        GunItem.actor = null;
    }

    @Override
    protected Clip create()
    {
        return new UseItemActionClip();
    }
}