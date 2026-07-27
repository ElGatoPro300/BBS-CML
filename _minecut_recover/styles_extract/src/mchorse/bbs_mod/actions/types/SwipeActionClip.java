package mchorse.bbs_mod.actions.types;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.class_1268;
import net.minecraft.class_1309;

public class SwipeActionClip extends ActionClip
{
    @Override
    public boolean isClient()
    {
        return true;
    }

    @Override
    protected void applyClientAction(IEntity entity, Film film, Replay replay, int tick)
    {
        entity.swingArm();
    }

    @Override
    public void applyAction(class_1309 actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        super.applyAction(actor, player, film, replay, tick);

        if (actor != null)
        {
            actor.method_23667(class_1268.field_5808, true);
        }
    }

    @Override
    protected Clip create()
    {
        return new SwipeActionClip();
    }
}