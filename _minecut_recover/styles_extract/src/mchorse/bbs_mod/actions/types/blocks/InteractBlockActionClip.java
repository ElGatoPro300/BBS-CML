package mchorse.bbs_mod.actions.types.blocks;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.actions.types.ActionClip;
import mchorse.bbs_mod.actions.values.ValueBlockHitResult;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.class_1268;
import net.minecraft.class_1309;
import net.minecraft.class_1799;
import net.minecraft.class_2281;
import net.minecraft.class_3965;

public class InteractBlockActionClip extends ActionClip
{
    public final ValueBlockHitResult hit = new ValueBlockHitResult("hit");
    public final ValueBoolean hand = new ValueBoolean("hand", true);

    public InteractBlockActionClip()
    {
        super();

        this.add(this.hit);
        this.add(this.hand);
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
        this.applyPositionRotation(player, replay, tick);

        class_3965 result = this.hit.getHitResult();
        
        if (player.method_37908().method_8320(result.method_17777()).method_26204() instanceof class_2281)
        {
            player.openReplayChest(replay.getId(), result.method_17777());
            return;
        }
        
        class_1268 hand = this.hand.get() ? class_1268.field_5808 : class_1268.field_5810;
        class_1799 stack = player.method_5998(hand);

        player.field_13974.method_14262(player, player.method_37908(), stack, hand, result);
    }

    @Override
    protected Clip create()
    {
        return new InteractBlockActionClip();
    }
}
