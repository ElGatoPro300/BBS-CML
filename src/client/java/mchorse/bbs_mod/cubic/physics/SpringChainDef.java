package mchorse.bbs_mod.cubic.physics;

/**
 * One Verlet spring chain rooted at the map key that owns this def.
 * {@code endBone} is the tip of the bone hierarchy; {@code pinTarget}
 * optionally hard-pins the virtual tip.
 */
public record SpringChainDef(String endBone, String pinTarget, float pullStrength, float drag, float springReturn, int relaxSteps, boolean bodyRelativePull, float pullRotX, float pullRotY, float pullRotZ, boolean hitDetection, float hitRadius, float influence)
{
    public static final float DEFAULT_INFLUENCE = 1F;
    public static final float DEFAULT_SPRING_RETURN = 0F;

    public SpringChainDef
    {
        pinTarget = pinTarget == null ? "" : pinTarget;
        springReturn = clamp01(springReturn);
        relaxSteps = Math.max(1, relaxSteps);
        hitRadius = Math.max(0F, hitRadius);
        influence = clamp01(influence);
    }

    public SpringChainDef(String endBone, String pinTarget, float pullStrength, float drag, int relaxSteps, boolean hitDetection, float hitRadius)
    {
        this(endBone, pinTarget, pullStrength, drag, DEFAULT_SPRING_RETURN, relaxSteps, false, 0F, 0F, 0F, hitDetection, hitRadius, DEFAULT_INFLUENCE);
    }

    public SpringChainDef(String endBone, String pinTarget, float pullStrength, float drag, int relaxSteps, boolean bodyRelativePull, float pullRotX, float pullRotY, float pullRotZ, boolean hitDetection, float hitRadius)
    {
        this(endBone, pinTarget, pullStrength, drag, DEFAULT_SPRING_RETURN, relaxSteps, bodyRelativePull, pullRotX, pullRotY, pullRotZ, hitDetection, hitRadius, DEFAULT_INFLUENCE);
    }

    public boolean hasPullRotation()
    {
        return this.pullRotX != 0F || this.pullRotY != 0F || this.pullRotZ != 0F;
    }

    private static float clamp01(float value)
    {
        if (value < 0F)
        {
            return 0F;
        }

        return Math.min(value, 1F);
    }
}
