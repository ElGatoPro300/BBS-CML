package mchorse.bbs_mod.actions.types;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.utils.clips.Clip;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

public class SwipeActionClip extends ActionClip
{
    /**
     * When {@link BBSSettings#editorActorPausedSwipeLoop} is off, records the film
     * tick on which an actor already started a client swipe so paused scrubbing
     * does not loop the swing forever.
     */
    private static final Map<Object, Integer> ACTOR_ONE_SHOT_AT_TICK = new WeakHashMap<>();

    @Override
    public boolean isClient()
    {
        return true;
    }

    /**
     * Drop one-shot locks when the film cursor leaves the swipe tick so scrubbing
     * back can play the swing again.
     */
    public static void noteClientFilmTick(IEntity entity, int tick)
    {
        if (BBSSettings.editorActorPausedSwipeLoop != null && BBSSettings.editorActorPausedSwipeLoop.get())
        {
            return;
        }

        Object key = actorSwipeKey(entity);

        if (key == null)
        {
            return;
        }

        Integer at = ACTOR_ONE_SHOT_AT_TICK.get(key);

        if (at != null && at != tick)
        {
            ACTOR_ONE_SHOT_AT_TICK.remove(key);
        }
    }

    private static Object actorSwipeKey(IEntity entity)
    {
        if (entity instanceof MCEntity mcEntity && mcEntity.getMcEntity() instanceof ActorEntity actor)
        {
            return actor;
        }

        return null;
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
            boolean loopWhileParked = BBSSettings.editorActorPausedSwipeLoop != null
                && BBSSettings.editorActorPausedSwipeLoop.get();

            if (!loopWhileParked)
            {
                Integer at = ACTOR_ONE_SHOT_AT_TICK.get(actor);

                if (at != null && at == tick)
                {
                    return;
                }

                /* Claim this tick even if a swing is already in progress so we do
                 * not re-fire after it ends while the cursor stays parked here. */
                ACTOR_ONE_SHOT_AT_TICK.put(actor, tick);
            }

            if (!actor.swinging)
            {
                actor.swing(InteractionHand.MAIN_HAND);
            }

            return;
        }

        /* Stubs: applyClientActions repeats every frame on this tick. Re-calling
         * swingArm() reset progress to 0 each frame and desynced the swipe from
         * ActorEntity / LivingEntity timing. */
        if (entity instanceof StubEntity stub)
        {
            if (!stub.isHandSwinging())
            {
                stub.swingArm();
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
            actor.swing(InteractionHand.MAIN_HAND, true);
        }
    }

    @Override
    protected Clip create()
    {
        return new SwipeActionClip();
    }
}
