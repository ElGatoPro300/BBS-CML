package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.utils.MathUtils;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import org.joml.Matrix4f;

/**
 * Vanilla-style death tip (Z roll) for film forms that are not {@link MobForm}.
 * Mob morphs already tip inside LivingEntityRenderer via morph.deathTime.
 * <p>
 * Samples keyframed {@code death_time} for actors without writing
 * {@link ActorEntity#deathTime} (combat death stays Attack-driven).
 */
public final class FormDeathTilt
{
    private FormDeathTilt()
    {}

    public static boolean supportsFormTip(Form form)
    {
        return form != null && !(form instanceof MobForm);
    }

    /**
     * Death progress for render. Prefers the entity field; for film actors also
     * honors keyframed {@code death_time} without mutating combat state.
     */
    public static int resolveDeathTime(IEntity source)
    {
        int deathTime = source == null ? 0 : source.getDeathTime();

        if (!(source instanceof MCEntity mcEntity) || !(mcEntity.getMcEntity() instanceof ActorEntity actor))
        {
            return deathTime;
        }

        Replay replay = actor.getReplay();

        if (replay != null && replay.keyframes != null)
        {
            int keyDeath = replay.keyframes.deathTime.interpolate((float) actor.getCurrentTick()).intValue();

            if (keyDeath > 0)
            {
                deathTime = Math.max(deathTime, keyDeath);
            }
        }

        if (deathTime <= 0 && (actor.isDead() || actor.getHealth() <= 0F))
        {
            deathTime = Math.max(1, actor.deathTime);
        }

        return deathTime;
    }

    public static float tipDegrees(int deathTime, float tickDelta)
    {
        if (deathTime <= 0)
        {
            return 0F;
        }

        float deathAngle = (deathTime + tickDelta - 1F) / 20F * 1.6F;

        return Math.min(MathHelper.sqrt(deathAngle), 1F) * 90F;
    }

    public static void apply(MatrixStack matrices, IEntity entity, Form form, float tickDelta)
    {
        if (!supportsFormTip(form))
        {
            return;
        }

        float degrees = tipDegrees(resolveDeathTime(entity), tickDelta);

        if (degrees != 0F)
        {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(degrees));
        }
    }

    public static void apply(Matrix4f matrix, IEntity entity, Form form, float tickDelta)
    {
        if (!supportsFormTip(form))
        {
            return;
        }

        float degrees = tipDegrees(resolveDeathTime(entity), tickDelta);

        if (degrees != 0F)
        {
            matrix.rotateZ(MathUtils.toRad(degrees));
        }
    }
}
