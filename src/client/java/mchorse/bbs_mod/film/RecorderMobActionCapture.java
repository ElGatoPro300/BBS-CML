package mchorse.bbs_mod.film;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.actions.types.ActionClip;
import mchorse.bbs_mod.actions.types.AttackActionClip;
import mchorse.bbs_mod.actions.types.DamageActionClip;
import mchorse.bbs_mod.actions.types.ProjectileAttackActionClip;
import mchorse.bbs_mod.actions.types.SwipeActionClip;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.base.BaseValue;

/**
 * Places combat action clips on autocaptured mob replays from server damage events.
 * Stub/non-actor playback ignores Attack/Damage for death (keyframes); Actor mode uses them for HP.
 * Swipe is client-side and drives arm-swing on both stub and Actor playback.
 */
public final class RecorderMobActionCapture
{
    public static final byte KIND_MELEE = 0;
    public static final byte KIND_PROJECTILE = 1;
    public static final byte KIND_DAMAGE = 2;

    private RecorderMobActionCapture()
    {}

    public static void handleServerCombat(int victimEntityId, int sourceEntityId, float amount, byte kind)
    {
        if (BBSSettings.recordingAutoCaptureMobActions == null || !BBSSettings.recordingAutoCaptureMobActions.get())
        {
            return;
        }

        if (amount <= 0F)
        {
            return;
        }

        Recorder recorder = BBSModClient.getFilms().getRecorder();

        if (recorder == null || recorder.hasNotStarted())
        {
            return;
        }

        RecorderMobCapture mobCapture = recorder.getMobCapture();
        int victimReplay = mobCapture.getReplayIndexForEntity(victimEntityId);
        int sourceReplay = sourceEntityId >= 0 ? mobCapture.getReplayIndexForEntity(sourceEntityId) : -1;
        int tick = recorder.getTick();

        if (kind == KIND_MELEE)
        {
            if (sourceReplay >= 0)
            {
                Replay attacker = recorder.film.replays.getList().get(sourceReplay);

                /* Same pairing as ActionRecorder: swipe for the arm, Attack for HP. */
                addClip(attacker, new SwipeActionClip(), tick);

                if (victimReplay >= 0)
                {
                    AttackActionClip clip = new AttackActionClip();

                    clip.damage.set(amount);
                    clip.target.set(recorder.film.replays.getList().get(victimReplay).getId());
                    addClip(attacker, clip, tick);
                }

                return;
            }

            /* Attacker not captured — still keep victim HP for Actor mode. */
            if (victimReplay >= 0)
            {
                DamageActionClip clip = new DamageActionClip();

                clip.damage.set(amount);
                addClip(recorder.film.replays.getList().get(victimReplay), clip, tick);
            }

            return;
        }

        if (kind == KIND_PROJECTILE)
        {
            if (sourceReplay >= 0 && victimReplay >= 0)
            {
                ProjectileAttackActionClip clip = new ProjectileAttackActionClip();

                clip.damage.set(amount);
                clip.target.set(recorder.film.replays.getList().get(victimReplay).getId());
                addClip(recorder.film.replays.getList().get(sourceReplay), clip, tick);

                return;
            }

            if (victimReplay >= 0)
            {
                DamageActionClip clip = new DamageActionClip();

                clip.damage.set(amount);
                addClip(recorder.film.replays.getList().get(victimReplay), clip, tick);
            }

            return;
        }

        if (kind == KIND_DAMAGE)
        {
            if (victimReplay < 0)
            {
                return;
            }

            DamageActionClip clip = new DamageActionClip();

            clip.damage.set(amount);
            addClip(recorder.film.replays.getList().get(victimReplay), clip, tick);
        }
    }

    private static void addClip(Replay replay, ActionClip clip, int tick)
    {
        if (replay == null || tick < 0)
        {
            return;
        }

        clip.tick.set(tick);
        clip.duration.set(1);

        BaseValue.edit(replay.actions, (actions) -> actions.addClip(clip));
    }
}
