package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.utils.MathUtils;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import org.joml.Matrix4f;

/**
 * Vanilla-style death tip (Z roll) driven by float {@code death_time} progress.
 * <p>
 * Film render pushes a continuous sample tick ({@code replayTick + transition}) so
 * keyframe transitions stay smooth. Progress is used directly — do not add render
 * {@code tickDelta} on top of a held integer, or the tip oscillates every frame.
 * <p>
 * For recorded MobForm deaths (1…20 per tick), {@code interpolate(tick + delta)}
 * matches LivingEntityRenderer's {@code deathTime + tickDelta} tip. Morph
 * {@code deathTime} stays 0 so vanilla does not double-apply tipDelta.
 */
public final class FormDeathTilt
{
    private static final ThreadLocal<Sample> SAMPLE = new ThreadLocal<>();

    private FormDeathTilt()
    {}

    public static void pushSample(Replay replay, float tick)
    {
        if (replay == null || Float.isNaN(tick))
        {
            return;
        }

        SAMPLE.set(new Sample(replay, tick));
    }

    public static void popSample()
    {
        SAMPLE.remove();
    }

    /**
     * Death tip progress in vanilla deathTime units (0 = upright, ~20 = fully tipped).
     */
    public static float resolveDeathProgress(IEntity source, float tickDelta)
    {
        Sample sample = SAMPLE.get();

        if (sample != null && sample.replay.keyframes != null && !sample.replay.keyframes.deathTime.isEmpty())
        {
            return sample.replay.keyframes.deathTime.interpolate(sample.tick).floatValue();
        }

        if (source instanceof MCEntity mcEntity && mcEntity.getMcEntity() instanceof ActorEntity actor)
        {
            float progress = 0F;
            Replay replay = actor.getReplay();

            if (replay != null && replay.keyframes != null && !replay.keyframes.deathTime.isEmpty())
            {
                progress = replay.keyframes.deathTime.interpolate((float) actor.getCurrentTick() + tickDelta).floatValue();
            }

            /* Combat death still advances LivingEntity.deathTime — keep vanilla sub-tick ease. */
            if (actor.deathTime > 0 || actor.isDead() || actor.getHealth() <= 0F)
            {
                progress = Math.max(progress, actor.deathTime + tickDelta);
            }

            return progress;
        }

        if (source == null)
        {
            return 0F;
        }

        /* Stub fallback when no film sample is pushed (no extra tickDelta wobble). */
        return source.getDeathTime();
    }

    public static int resolveDeathTime(IEntity source)
    {
        return MathHelper.floor(resolveDeathProgress(source, 0F));
    }

    public static float tipDegrees(float deathProgress)
    {
        if (deathProgress <= 0F)
        {
            return 0F;
        }

        /* Same curve as LivingEntityRenderer: (deathTime + tickDelta - 1) / 20 * 1.6
         * with progress already equal to deathTime + fractional tick. */
        float deathAngle = (deathProgress - 1F) / 20F * 1.6F;

        if (deathAngle <= 0F)
        {
            return 0F;
        }

        return Math.min(MathHelper.sqrt(deathAngle), 1F) * 90F;
    }

    public static void apply(MatrixStack matrices, IEntity entity, Form form, float tickDelta)
    {
        if (form == null || entity == null)
        {
            return;
        }

        float degrees = tipDegrees(resolveDeathProgress(entity, tickDelta));

        if (degrees != 0F)
        {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(degrees));
        }
    }

    public static void apply(Matrix4f matrix, IEntity entity, Form form, float tickDelta)
    {
        if (form == null || entity == null)
        {
            return;
        }

        float degrees = tipDegrees(resolveDeathProgress(entity, tickDelta));

        if (degrees != 0F)
        {
            matrix.rotateZ(MathUtils.toRad(degrees));
        }
    }

    private static final class Sample
    {
        private final Replay replay;
        private final float tick;

        private Sample(Replay replay, float tick)
        {
            this.replay = replay;
            this.tick = tick;
        }
    }
}
