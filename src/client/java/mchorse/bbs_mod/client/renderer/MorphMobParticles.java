package mchorse.bbs_mod.client.renderer;

import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

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

    public static void afterTick(Entity morph, IEntity source, boolean enabled)
    {
        if (!enabled || morph == null)
        {
            return;
        }

        ClientLevel world = Minecraft.getInstance().level;

        if (world == null)
        {
            return;
        }

        ParticleState state = STATES.computeIfAbsent(morph.getUUID(), (key) -> new ParticleState());

        if (morph instanceof LivingEntity living)
        {
            emitDeathParticles(world, living, source, state);
        }

        emitStatusParticles(morph, source, state);
    }

    /**
     * Vanilla living entities only spawn the poof burst once when the corpse finishes
     * its death animation ({@code deathTime == 20}), not every tick while dying.
     */
    private static void emitDeathParticles(Level world, LivingEntity living, IEntity source, ParticleState state)
    {
        int deathTime = source.getDeathTime();

        if (deathTime < 19)
        {
            state.deathBurstEmitted = false;
            return;
        }

        if (state.deathBurstEmitted)
        {
            return;
        }

        state.deathBurstEmitted = true;

        double x = living.getX();
        double y = living.getY() + living.getEyeHeight(living.getPose()) * 0.5D;
        double z = living.getZ();
        float width = Math.max(living.getBbWidth(), 0.6F);

        for (int i = 0; i < 20; ++i)
        {
            double offsetX = (living.getRandom().nextDouble() - 0.5D) * width;
            double offsetY = living.getRandom().nextDouble() * living.getBbHeight();
            double offsetZ = (living.getRandom().nextDouble() - 0.5D) * width;
            double dx = living.getRandom().nextGaussian() * 0.02D;
            double dy = living.getRandom().nextGaussian() * 0.02D;
            double dz = living.getRandom().nextGaussian() * 0.02D;

            world.addParticle(ParticleTypes.POOF, x + offsetX, y + offsetY, z + offsetZ, dx, dy, dz);
        }
    }

    private static void emitStatusParticles(Entity morph, IEntity source, ParticleState state)
    {
        if (morph instanceof Warden warden)
        {
            emitWardenStatus(warden, source, state);
        }
    }

    private static void emitWardenStatus(Warden warden, IEntity source, ParticleState state)
    {
        Warden sourceWarden = getSourceWarden(source, warden);

        tickWardenAnimations(warden);

        if (sourceWarden != null)
        {
            tickWardenAnimations(sourceWarden);
        }

        boolean charging = sourceWarden != null
            ? isAnimationRunning(sourceWarden.sonicBoomAnimationState)
            : isAnimationRunning(warden.sonicBoomAnimationState);

        if (charging && !state.chargingSonicBoom)
        {
            triggerSonicBoom(warden);
        }

        state.chargingSonicBoom = charging;

        boolean roaring = sourceWarden != null
            ? isAnimationRunning(sourceWarden.roarAnimationState)
            : isAnimationRunning(warden.roarAnimationState);

        if (roaring && !state.roaring)
        {
            warden.handleEntityEvent((byte) 4);
        }

        state.roaring = roaring;

        boolean sniffing = sourceWarden != null
            ? isAnimationRunning(sourceWarden.sniffAnimationState)
            : isAnimationRunning(warden.sniffAnimationState);

        if (sniffing && !state.sniffing)
        {
            warden.handleEntityEvent((byte) 61);
        }

        state.sniffing = sniffing;
    }

    private static Warden getSourceWarden(IEntity source, Warden morph)
    {
        if (source instanceof MCEntity mcEntity)
        {
            Entity entity = mcEntity.getMcEntity();

            if (entity instanceof Warden warden)
            {
                return warden;
            }
        }

        Level world = Minecraft.getInstance().level;

        if (world == null)
        {
            return null;
        }

        AABB box = morph.getBoundingBox().inflate(2.0D);

        for (Warden warden : world.getEntitiesOfClass(Warden.class, box, (candidate) -> candidate != morph))
        {
            if (isAnimationRunning(warden.sonicBoomAnimationState)
                || isAnimationRunning(warden.roarAnimationState)
                || isAnimationRunning(warden.sniffAnimationState))
            {
                return warden;
            }
        }

        return null;
    }

    private static void tickWardenAnimations(Warden warden)
    {
        int age = warden.tickCount;

        warden.sonicBoomAnimationState.fastForward(age, 1.0F);
        warden.roarAnimationState.fastForward(age, 1.0F);
        warden.sniffAnimationState.fastForward(age, 1.0F);
        warden.attackAnimationState.fastForward(age, 1.0F);
        warden.emergeAnimationState.fastForward(age, 1.0F);
        warden.diggingAnimationState.fastForward(age, 1.0F);
    }

    private static void triggerSonicBoom(Warden warden)
    {
        warden.handleEntityEvent(EntityEvent.SONIC_CHARGE);
        spawnSonicBoomParticle(warden);
    }

    private static void spawnSonicBoomParticle(Entity entity)
    {
        ClientLevel world = Minecraft.getInstance().level;

        if (world == null)
        {
            return;
        }

        double x = entity.getX();
        double y = entity.getY() + entity.getBbHeight() * 0.5D;
        double z = entity.getZ();

        world.addParticle(ParticleTypes.SONIC_BOOM, x, y, z, 0D, 0D, 0D);
    }

    private static boolean isAnimationRunning(AnimationState animationState)
    {
        return animationState != null && animationState.isStarted();
    }

    public static void clear(Entity morph)
    {
        if (morph != null)
        {
            STATES.remove(morph.getUUID());
        }
    }

    private static class ParticleState
    {
        public boolean chargingSonicBoom;
        public boolean roaring;
        public boolean sniffing;
        public boolean deathBurstEmitted;
    }
}
