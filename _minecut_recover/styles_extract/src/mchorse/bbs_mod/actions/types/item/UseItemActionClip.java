package mchorse.bbs_mod.actions.types.item;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.items.GunItem;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.class_1268;
import net.minecraft.class_1309;
import net.minecraft.class_1799;

public class UseItemActionClip extends ItemActionClip
{
    public final ValueInt useTicks = new ValueInt("use_ticks", 0, 0, Integer.MAX_VALUE);

    public UseItemActionClip()
    {
        super();

        this.add(this.useTicks);
    }

    @Override
    public void applyAction(class_1309 actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        class_1268 hand = this.hand.get() ? class_1268.field_5808 : class_1268.field_5810;

        GunItem.actor = actor;

        this.applyPositionRotation(player, replay, tick);
        class_1799 copy = this.itemStack.get().method_7972();
        int maxUseTime = copy.method_7935(player);
        int used = this.useTicks.get();

        player.method_6122(hand, copy);
        copy.method_7913(player.method_37908(), player, hand);

        if (used > 0 && maxUseTime > 0)
        {
            int remaining = Math.max(0, maxUseTime - used);
            copy.method_7930(player.method_37908(), player, remaining);
            player.method_6075();
        }

        player.method_6122(hand, class_1799.field_8037);

        GunItem.actor = null;
    }

    @Override
    protected Clip create()
    {
        return new UseItemActionClip();
    }
}