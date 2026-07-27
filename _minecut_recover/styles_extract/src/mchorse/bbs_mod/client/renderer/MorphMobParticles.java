package mchorse.bbs_mod.client.renderer;

import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1937;
import net.minecraft.class_238;
import net.minecraft.class_2398;
import net.minecraft.class_6024;
import net.minecraft.class_7094;
import net.minecraft.class_7260;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Emits and optionally suppresses vanilla mob particles for morph stub entities
 * during film replay playback.
 */
public final class MorphMobParticles
{
    private static final ThreadLocal<Boolean> SUPPRESS = new ThreadLocal<>();
    private static final Map<UUID, ParticleState> STATES = new WeakHashMap<>();

    private MorphMobParticles()
    {}

    public static boolean shouldSuppress()
    {
        return Boolean.TRUE.equals(SUPPRESS.get());
    }

    public static void beginTick(boolean enabled)
    {
        if (enabled)
        {
            SUPPRESS.remove();
        }
        else
        {
            SUPPRESS.set(true);
        }
    }

    public static void endTick()
    {
        SUPPRESS.remove();
    }

    public static void afterTick(class_1297 morph, IEntity source, boolean enabled)
    {
        if (!enabled || morph == null)
        {
            return;
        }

        class_1937 world = morph.method_37908();

        if (world == null || !world.field_9236)
        {
            return;
        }

        ParticleState state = STATES.computeIfAbsent(morph.method_5667(), (key) -> new ParticleState());

        if (morph instanceof class_1309 living)
        {
            emitDeathParticles(world, living, source);
        }

        emitStatusParticles(morph, source, state);
    }

    private static void emitDeathParticles(class_1937 world, class_1309 living, IEntity source)
    {
        int deathTime = source.getDeathTime();

        if (deathTime <= 0 || deathTime >= 20)
        {
            return;
        }

        double x = living.method_23317();
        double y = living.method_23318() + living.method_17682() * 0.5D;
        double z = living.method_23321();

        for (int i = 0; i < 4; ++i)
        {
            double dx = living.method_59922().method_43059() * 0.02D;
            double dy = living.method_59922().method_43059() * 0.02D;
            double dz = living.method_59922().method_43059() * 0.02D;

            world.method_8406(class_2398.field_11203, x, y, z, dx, dy, dz);
        }
    }

    private static void emitStatusParticles(class_1297 morph, IEntity source, ParticleState state)
    {
        if (morph instanceof class_7260 warden)
        {
            emitWardenStatus(warden, source, state);
        }
    }

    private static void emitWardenStatus(class_7260 warden, IEntity source, ParticleState state)
    {
        class_7260 sourceWarden = getSourceWarden(source, warden);

        tickWardenAnimations(warden);

        if (sourceWarden != null)
        {
            tickWardenAnimations(sourceWarden);
        }

        boolean charging = sourceWarden != null
            ? isAnimationRunning(sourceWarden.field_38859)
            : isAnimationRunning(warden.field_38859);

        if (charging && !state.chargingSonicBoom)
        {
            triggerSonicBoom(warden);
        }

        state.chargingSonicBoom = charging;

        boolean roaring = sourceWarden != null
            ? isAnimationRunning(sourceWarden.field_38168)
            : isAnimationRunning(warden.field_38168);

        if (roaring && !state.roaring)
        {
            warden.method_5711((byte) 4);
        }

        state.roaring = roaring;

        boolean sniffing = sourceWarden != null
            ? isAnimationRunning(sourceWarden.field_38169)
            : isAnimationRunning(warden.field_38169);

        if (sniffing && !state.sniffing)
        {
            warden.method_5711((byte) 61);
        }

        state.sniffing = sniffing;
    }

    private static class_7260 getSourceWarden(IEntity source, class_7260 morph)
    {
        if (source instanceof MCEntity mcEntity)
        {
            class_1297 entity = mcEntity.getMcEntity();

            if (entity instanceof class_7260 warden)
            {
                return warden;
            }
        }

        class_1937 world = morph.method_37908();

        if (world == null)
        {
            return null;
        }

        class_238 box = morph.method_5829().method_1014(2.0D);

        for (class_7260 warden : world.method_8390(class_7260.class, box, (candidate) -> candidate != morph))
        {
            if (isAnimationRunning(warden.field_38859)
                || isAnimationRunning(warden.field_38168)
                || isAnimationRunning(warden.field_38169))
            {
                return warden;
            }
        }

        return null;
    }

    private static void tickWardenAnimations(class_7260 warden)
    {
        int age = warden.field_6012;

        warden.field_38859.method_43686(age, 1.0F);
        warden.field_38168.method_43686(age, 1.0F);
        warden.field_38169.method_43686(age, 1.0F);
        warden.field_38137.method_43686(age, 1.0F);
        warden.field_38135.method_43686(age, 1.0F);
        warden.field_38136.method_43686(age, 1.0F);
    }

    private static void triggerSonicBoom(class_7260 warden)
    {
        warden.method_5711(class_6024.field_38847);
        spawnSonicBoomParticle(warden);
    }

    private static void spawnSonicBoomParticle(class_1297 entity)
    {
        class_1937 world = entity.method_37908();

        if (world == null || !world.field_9236)
        {
            return;
        }

        double x = entity.method_23317();
        double y = entity.method_23318() + entity.method_17682() * 0.5D;
        double z = entity.method_23321();

        world.method_8494(class_2398.field_38908, x, y, z, 0D, 0D, 0D);
    }

    private static boolean isAnimationRunning(class_7094 animationState)
    {
        return animationState != null && animationState.method_41327();
    }

    public static void clear(class_1297 morph)
    {
        if (morph != null)
        {
            STATES.remove(morph.method_5667());
        }
    }

    private static class ParticleState
    {
        public boolean chargingSonicBoom;
        public boolean roaring;
        public boolean sniffing;
    }
}
