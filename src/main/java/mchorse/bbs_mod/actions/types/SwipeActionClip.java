package mchorse.bbs_mod.actions.types;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.utils.clips.Clip;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;

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
        /* ActorEntity already receives swingHand from applyAction on the server.
         * Calling swingArm again on the client restarts the swing mid-animation. */
        if (entity instanceof MCEntity mcEntity && mcEntity.getMcEntity() instanceof ActorEntity)
        {
            return;
        }

        entity.swingArm();
    }

    @Override
    public void applyAction(LivingEntity actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        super.applyAction(actor, player, film, replay, tick);

        if (actor != null)
        {
            actor.swingHand(Hand.MAIN_HAND, true);
        }
    }

    @Override
    protected Clip create()
    {
        return new SwipeActionClip();
    }
}