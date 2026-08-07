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
        if (entity instanceof MCEntity mcEntity && mcEntity.getMcEntity() instanceof ActorEntity actor)
        {
            /* Actor-mode delay came from waiting on server swingHand sync.
             * Start locally as soon as the film hits this tick. Only skip when a
             * swing is already active: applyClientActions repeats every frame
             * while the cursor stays here, and LivingEntity.swingHand restarts
             * while handSwingTicks == -1 (arm jitter). Server applyAction remains
             * as a fallback if this client path did not run. */
            if (!actor.handSwinging)
            {
                actor.swingHand(Hand.MAIN_HAND);
            }

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