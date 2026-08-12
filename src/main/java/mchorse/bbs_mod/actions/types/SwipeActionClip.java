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

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;

import java.util.Map;
import java.util.WeakHashMap;

public class SwipeActionClip extends ActionClip
{
    /**
     * When {@link BBSSettings#editorActorPausedSwipeLoop} is off, records the film
     * tick on which a client entity already started a swipe so paused scrubbing
     * does not loop the swing forever (actors and non-actor stubs).
     */
    private static final Map<Object, Integer> CLIENT_ONE_SHOT_AT_TICK = new WeakHashMap<>();

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

        Object key = clientSwipeKey(entity);

        if (key == null)
        {
            return;
        }

        Integer at = CLIENT_ONE_SHOT_AT_TICK.get(key);

        if (at != null && at != tick)
        {
            CLIENT_ONE_SHOT_AT_TICK.remove(key);
        }
    }

    private static Object clientSwipeKey(IEntity entity)
    {
        if (entity instanceof MCEntity mcEntity && mcEntity.getMcEntity() instanceof ActorEntity actor)
        {
            return actor;
        }

        if (entity instanceof StubEntity stub)
        {
            return stub;
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
                Integer at = CLIENT_ONE_SHOT_AT_TICK.get(actor);

                if (at != null && at == tick)
                {
                    return;
                }

                /* Claim this tick even if a swing is already in progress so we do
                 * not re-fire after it ends while the cursor stays parked here. */
                CLIENT_ONE_SHOT_AT_TICK.put(actor, tick);
            }

            if (!actor.handSwinging)
            {
                actor.swingHand(Hand.MAIN_HAND);
            }

            return;
        }

        /* Stubs have no server LivingEntity swingHand(true) fallback. Guard by film
         * tick (not isHandSwinging) so a later swipe clip can restart mid-swing,
         * while parked scrubbing on the same tick still only fires once. */
        if (entity instanceof StubEntity stub)
        {
            boolean loopWhileParked = BBSSettings.editorActorPausedSwipeLoop != null
                && BBSSettings.editorActorPausedSwipeLoop.get();

            if (!loopWhileParked)
            {
                Integer at = CLIENT_ONE_SHOT_AT_TICK.get(stub);

                if (at != null && at == tick)
                {
                    return;
                }

                CLIENT_ONE_SHOT_AT_TICK.put(stub, tick);
            }

            stub.swingArm();

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
