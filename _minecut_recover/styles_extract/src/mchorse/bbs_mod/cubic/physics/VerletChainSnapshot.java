package mchorse.bbs_mod.cubic.physics;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Per-chain Verlet simulation state: AoS particles plus tick bookkeeping
 * owned by {@link DynamicBoneOrchestrator}.
 */
class VerletChainSnapshot
{
    public static final class Particle
    {
        public final Vector3f position = new Vector3f();
        public final Vector3f previousPosition = new Vector3f();
        public final Vector3f restPoseLocal = new Vector3f();
    }

    public int lastTick = Integer.MIN_VALUE;
    public Vector3f anchorPoint = new Vector3f();
    public Quaternionf anchorOrientation = new Quaternionf();
    public float renderBlend;
    public Particle[] particles;

    /** Settled shapes of the two latest simulation ticks, each in its tick's anchor frame. */
    public Vector3f[] snapshotCurrent;
    public Vector3f[] snapshotPrevious;

    public Vector3f[] renderOutput;
}
