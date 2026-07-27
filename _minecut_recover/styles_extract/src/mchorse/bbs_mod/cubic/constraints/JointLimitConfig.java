package mchorse.bbs_mod.cubic.constraints;

import java.util.Map;

/**
 * Per-bone angular limits for skeleton joints.
 *
 * Each bone can have independent min/max constraints on its three
 * rotation axes (X, Y, Z) in degrees. Used by both the IK solver
 * (to clamp solved rotations) and the physics solver (to limit
 * simulated swing).
 */
public record JointLimitConfig(Map<String, JointLimit> joints)
{
    /**
     * Angular constraint for a single bone. When {@code active} is false
     * the bone is unconstrained. Min/max values are in degrees.
     */
    public record JointLimit(boolean active, float minX, float minY, float minZ, float maxX, float maxY, float maxZ)
    {
    }
}
