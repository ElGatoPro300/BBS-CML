package mchorse.bbs_mod.actions.types.item;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.actions.values.ValueBlockHitResult;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.items.GunItem;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.class_1268;
import net.minecraft.class_1309;
import net.minecraft.class_1799;

public class UseBlockItemActionClip extends ItemActionClip
{
    public final ValueBlockHitResult hit = new ValueBlockHitResult("hit");

    public UseBlockItemActionClip()
    {
        super();

        this.add(this.hit);
    }

    @Override
    public void shift(double dx, double dy, double dz)
    {
        super.shift(dx, dy, dz);

        this.hit.shift(dx, dy, dz);
    }

    @Override
    public void applyAction(class_1309 actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        class_1268 hand = this.hand.get() ? class_1268.field_5808 : class_1268.field_5810;
        class_1799 copy = this.itemStack.get().method_7972();
        class_1799 previous = player.method_5998(hand).method_7972();

        GunItem.actor = actor;

        this.applyPositionRotation(player, replay, tick);
        player.method_6122(hand, copy);
        player.field_13974.method_14262(player, player.method_37908(), copy, hand, this.hit.getHitResult());
        player.method_6122(hand, previous);

        GunItem.actor = null;
    }

    @Override
    protected Clip create()
    {
        return new UseBlockItemActionClip();
    }
}
