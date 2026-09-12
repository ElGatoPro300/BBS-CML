package mchorse.bbs_mod.cubic.physics;

import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.cubic.constraints.JointLimitConfig;
import mchorse.bbs_mod.cubic.render.CubicRenderer.PivotFrame;
import mchorse.bbs_mod.utils.joml.QuaternionMath;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

/**
 * Per-chain Verlet bone-physics solver. Integrates particles a fixed number of
 * sub-steps per film tick via nested phases, then reconstructs the render shape.
 */
final class VerletChainSimulator
{
    static final float EPS = 1.0e-6f;
    static final float BASE_GRAVITY = 0.08F;
    static final float TIP_STIFFNESS_SCALE = 0.4F;
    static final float COLLISION_FRICTION = 0.5F;
    static final float COLLISION_MAX_ANCHOR_STEP = 0.25F;
    static final int SUBSTEPS_PER_TICK = 3;
    static final float PHYSICS_STEP = 1F / SUBSTEPS_PER_TICK;
    static final int PHYSICS_MAX_STEPS = 30;
    static final int MAX_TICK_CATCHUP = 4;

    private VerletChainSimulator()
    {
    }

    static void computePoseTargets(IModel model, List<String> ids, List<PivotFrame> chainFrames, float[] lengths, Vector3f anchor, Quaternionf anchorOrientation, boolean hardTarget, VerletChainSnapshot state)
    {
        VerletChainSnapshot.Particle[] particles = state.particles;
        int pivotCount = chainFrames.size();

        if (particles == null || particles.length != pivotCount + 1)
        {
            return;
        }

        Quaternionf invAnchor = new Quaternionf(anchorOrientation).invert();
        Vector3f tmp = new Vector3f();

        particles[0].restPoseLocal.set(0F, 0F, 0F);

        for (int i = 1; i < pivotCount; i++)
        {
            tmp.set(chainFrames.get(i).position()).sub(anchor);
            particles[i].restPoseLocal.set(invAnchor.transform(tmp));
        }

        int tip = particles.length - 1;

        if (hardTarget)
        {
            particles[tip].restPoseLocal.set(particles[pivotCount - 1].restPoseLocal);
            return;
        }

        Vector3f tipDir = lengths != null && lengths.length >= pivotCount ? SkeletonGeometryAdapter.tipRestDirectionLocal(model, ids) : null;

        if (tipDir == null || tipDir.lengthSquared() < EPS * EPS)
        {
            particles[tip].restPoseLocal.set(particles[pivotCount - 1].restPoseLocal);
            return;
        }

        PivotFrame lastFrame = chainFrames.get(pivotCount - 1);
        new Quaternionf(lastFrame.worldRotation()).transform(tipDir.normalize()).mul(lengths[pivotCount - 1]);
        tmp.set(lastFrame.position()).add(tipDir).sub(anchor);
        particles[tip].restPoseLocal.set(invAnchor.transform(tmp));
    }

    static Vector3f[] renderInterpolate(VerletChainSnapshot state, float transition, Vector3f liveAnchor, Quaternionf liveAnchorOrientation, Vector3f target)
    {
        Vector3f[] render = state.renderOutput;
        Vector3f[] settled = state.snapshotCurrent;
        Vector3f[] settledPrev = state.snapshotPrevious;

        if (render == null || settled == null || settledPrev == null || render.length != settled.length || settledPrev.length != settled.length)
        {
            return positionsView(state);
        }

        float alpha = clamp01(transition);

        Vector3f dir = new Vector3f();
        Vector3f dirCurr = new Vector3f();
        Quaternionf segRot = new Quaternionf();
        Quaternionf frac = new Quaternionf();

        render[0].set(liveAnchor);

        for (int i = 0; i + 1 < render.length; i++)
        {
            dir.set(settledPrev[i + 1]).sub(settledPrev[i]);
            dirCurr.set(settled[i + 1]).sub(settled[i]);

            float lenPrev = dir.length();
            float lenCurr = dirCurr.length();
            float len = lenPrev + (lenCurr - lenPrev) * alpha;

            boolean okPrev = lenPrev > EPS;
            boolean okCurr = lenCurr > EPS;

            if (okPrev && okCurr)
            {
                dir.div(lenPrev);
                dirCurr.div(lenCurr);
                segRot.rotationTo(dir, dirCurr);
                frac.identity().slerp(segRot, alpha).transform(dir);
            }
            else if (okCurr)
            {
                dir.set(dirCurr).div(lenCurr);
            }
            else if (okPrev)
            {
                dir.div(lenPrev);
            }
            else
            {
                render[i + 1].set(render[i]);
                continue;
            }

            liveAnchorOrientation.transform(dir);
            render[i + 1].set(render[i]).add(dir.mul(len));
        }

        if (target != null)
        {
            render[render.length - 1].set(target);
        }

        return render;
    }

    /** Temporary SoA view of particle positions for collision / render fallback. */
    private static Vector3f[] positionsView(VerletChainSnapshot state)
    {
        VerletChainSnapshot.Particle[] particles = state.particles;
        Vector3f[] out = new Vector3f[particles.length];

        for (int i = 0; i < particles.length; i++)
        {
            out[i] = particles[i].position;
        }

        return out;
    }

    private static Vector3f[] previousView(VerletChainSnapshot state)
    {
        VerletChainSnapshot.Particle[] particles = state.particles;
        Vector3f[] out = new Vector3f[particles.length];

        for (int i = 0; i < particles.length; i++)
        {
            out[i] = particles[i].previousPosition;
        }

        return out;
    }

    private static void copySnapshots(Vector3f[] src, Vector3f[] dst)
    {
        for (int i = 0; i < src.length; i++)
        {
            dst[i].set(src[i]);
        }
    }

    private static void snapshotLocal(VerletChainSnapshot.Particle[] particles, Vector3f anchor, Quaternionf anchorOrientation, Vector3f[] out)
    {
        Quaternionf inv = new Quaternionf(anchorOrientation).invert();

        for (int i = 0; i < particles.length; i++)
        {
            out[i].set(particles[i].position).sub(anchor);
            inv.transform(out[i]);
        }
    }

    static void step(World world, int age, float transition, IModel model, List<String> ids, SpringChainCompiler.CompiledChain chain, float pullStrengthMul, float dragValue, float springReturnValue, WindDef wind, Map<String, JointLimitConfig.JointLimit> jointLimits, Vector3f anchorPosition, Quaternionf anchorOrientation, Quaternionf parentRotation, Vector3f targetPosition, List<PivotFrame> chainFrames, VerletChainSnapshot state)
    {
        Vector3f newAnchor = anchorPosition;
        Quaternionf newAnchorOrientation = anchorOrientation;
        float[] lengths = chain.restLengths();

        if (lengths == null || lengths.length != state.particles.length - 1)
        {
            return;
        }

        if (state.lastTick == Integer.MIN_VALUE)
        {
            seedFromPose(state, chainFrames, lengths, newAnchor, newAnchorOrientation);
            state.lastTick = age;
            state.renderBlend = 0F;
            return;
        }

        int delta = age - state.lastTick;

        if (delta == 0)
        {
            state.renderBlend = clamp01(transition);
            return;
        }

        if (delta < 0 || delta > MAX_TICK_CATCHUP)
        {
            seedFromPose(state, chainFrames, lengths, newAnchor, newAnchorOrientation);
            state.lastTick = age;
            state.renderBlend = clamp01(transition);
            return;
        }

        float gravity = BASE_GRAVITY * pullStrengthMul;
        float damping = clamp01(dragValue);
        int iterations = chain.relaxSteps();
        boolean collisions = chain.hitDetection() && world != null && chain.hitRadius() > 0F;
        float radius = chain.hitRadius();
        boolean hardTarget = targetPosition != null;
        int last = state.particles.length - 1;

        int steps = delta * SUBSTEPS_PER_TICK;

        if (steps > PHYSICS_MAX_STEPS)
        {
            steps = PHYSICS_MAX_STEPS;
        }

        state.renderBlend = clamp01(transition);

        float h = PHYSICS_STEP;
        float dampMul = (float) Math.pow(1F - damping, h);
        float gravityScale = h * h;

        Vector3f gravityVec = new Vector3f();
        EnvironmentalForces.computeGravityDirection(chain, parentRotation, gravity, gravityVec);
        float gravityX = gravityVec.x * gravityScale;
        float gravityY = gravityVec.y * gravityScale;
        float gravityZ = gravityVec.z * gravityScale;

        Vector3f windDir = new Vector3f();
        float windMagnitude = EnvironmentalForces.prepareWind(wind, BASE_GRAVITY, windDir);
        boolean hasWind = windMagnitude > 0F;
        Vector3f windVec = hasWind ? new Vector3f() : null;
        int startAge = state.lastTick;

        float[] stiffStep = computeStiffnessSteps(clamp01(springReturnValue), state.particles.length, h);

        boolean limits = jointLimits != null && !jointLimits.isEmpty();
        SkeletonGeometryAdapter rig = limits ? SkeletonGeometryAdapter.of(model) : null;

        if (rig == null)
        {
            limits = false;
        }

        Vector3f startAnchor = new Vector3f(state.anchorPoint);
        Quaternionf startAnchorOrientation = new Quaternionf(state.anchorOrientation);
        Vector3f stepAnchor = new Vector3f();
        Quaternionf stepAnchorOrientation = new Quaternionf();
        Vector3f vel = new Vector3f();
        Vector3f poseDir = new Vector3f();
        Vector3f curDir = new Vector3f();
        BlockPos.Mutable mutable = collisions ? new BlockPos.Mutable() : null;

        copySnapshots(state.snapshotCurrent, state.snapshotPrevious);

        for (int s = 0; s < steps; s++)
        {
            float progress = (s + 1) / (float) steps;
            float filmTime = startAge + (s + 1) * h;
            stepAnchor.set(startAnchor).lerp(newAnchor, progress);
            stepAnchorOrientation.set(startAnchorOrientation).slerp(newAnchorOrientation, progress);

            state.anchorPoint.set(stepAnchor);
            state.anchorOrientation.set(stepAnchorOrientation);
            state.particles[0].position.set(stepAnchor);
            state.particles[0].previousPosition.set(stepAnchor);

            IntegrationPhase.execute(state, dampMul, gravityX, gravityY, gravityZ, hasWind, windDir, windMagnitude, wind, filmTime, windVec, gravityScale, collisions, world, mutable, radius, vel);
            SpringPhase.execute(state.particles, stepAnchorOrientation, stiffStep, poseDir, curDir);
            ConstraintPhase.execute(state, lengths, iterations, hardTarget, targetPosition, last);

            if (limits)
            {
                AngleLimitPhase.execute(rig, ids, state.particles, lengths, jointLimits, parentRotation);

                if (!collisions)
                {
                    ConstraintPhase.lengthForward(state.particles, lengths);
                    ConstraintPhase.pinEnds(state.particles, state.anchorPoint, targetPosition, last);
                }
            }

            if (collisions)
            {
                CollisionPhase.execute(world, state, targetPosition, last, radius);
                ConstraintPhase.lengthForward(state.particles, lengths);
                ConstraintPhase.pinEnds(state.particles, state.anchorPoint, targetPosition, last);
            }
        }

        snapshotLocal(state.particles, state.anchorPoint, state.anchorOrientation, state.snapshotCurrent);
        state.lastTick = age;
    }

    private static void seedFromPose(VerletChainSnapshot state, List<PivotFrame> chainFrames, float[] lengths, Vector3f anchor, Quaternionf anchorOrientation)
    {
        state.anchorPoint.set(anchor);
        state.anchorOrientation.set(anchorOrientation);

        state.particles[0].position.set(anchor);
        state.particles[0].previousPosition.set(anchor);

        for (int i = 1; i < chainFrames.size(); i++)
        {
            Vector3f p = chainFrames.get(i).position();
            state.particles[i].position.set(p);
            state.particles[i].previousPosition.set(p);
        }

        Vector3f tipDir = new Vector3f();

        if (chainFrames.size() >= 2)
        {
            tipDir.set(state.particles[chainFrames.size() - 1].position).sub(state.particles[chainFrames.size() - 2].position);

            if (tipDir.lengthSquared() < EPS * EPS)
            {
                tipDir.set(0F, -1F, 0F);
            }
            else
            {
                tipDir.normalize();
            }
        }
        else
        {
            tipDir.set(0F, -1F, 0F);
        }

        int tip = state.particles.length - 1;
        state.particles[tip].position.set(state.particles[chainFrames.size() - 1].position).add(tipDir.mul(lengths[lengths.length - 1]));
        state.particles[tip].previousPosition.set(state.particles[tip].position);

        snapshotLocal(state.particles, anchor, anchorOrientation, state.snapshotCurrent);
        copySnapshots(state.snapshotCurrent, state.snapshotPrevious);
    }

    private static float[] computeStiffnessSteps(float baseStiffness, int pointCount, float h)
    {
        float[] out = new float[pointCount];

        if (baseStiffness <= 0F || pointCount <= 1)
        {
            return out;
        }

        int freeCount = pointCount - 1;

        for (int i = 1; i < pointCount; i++)
        {
            float t = freeCount <= 1 ? 0F : (i - 1) / (float) (freeCount - 1);
            float falloff = 1F - (1F - TIP_STIFFNESS_SCALE) * t;
            float perTick = baseStiffness * falloff;

            out[i] = 1F - (float) Math.pow(1F - perTick, h);
        }

        return out;
    }

    private static float clamp01(float v)
    {
        if (v < 0F)
        {
            return 0F;
        }

        return v > 1F ? 1F : v;
    }

    /* ---- phases ---- */

    static final class IntegrationPhase
    {
        private IntegrationPhase()
        {
        }

        static void execute(VerletChainSnapshot state, float dampMul, float gravityX, float gravityY, float gravityZ, boolean hasWind, Vector3f windDir, float windMagnitude, WindDef wind, float filmTime, Vector3f windVec, float gravityScale, boolean collisions, World world, BlockPos.Mutable mutable, float radius, Vector3f vel)
        {
            VerletChainSnapshot.Particle[] particles = state.particles;

            for (int i = 1; i < particles.length; i++)
            {
                Vector3f p = particles[i].position;
                Vector3f prev = particles[i].previousPosition;

                vel.set(p).sub(prev).mul(dampMul);
                prev.set(p);
                p.add(vel);
                p.x += gravityX;
                p.y += gravityY;
                p.z += gravityZ;

                if (hasWind)
                {
                    EnvironmentalForces.windForceAt(windDir, windMagnitude, wind, filmTime, p, windVec);
                    p.x += windVec.x * gravityScale;
                    p.y += windVec.y * gravityScale;
                    p.z += windVec.z * gravityScale;
                }

                if (collisions)
                {
                    clampTunnelStep(world, mutable, p, prev, radius);
                }
            }
        }

        private static void clampTunnelStep(World world, BlockPos.Mutable mutable, Vector3f p, Vector3f prev, float radius)
        {
            float dx = p.x - prev.x;
            float dy = p.y - prev.y;
            float dz = p.z - prev.z;

            float maxStep = Math.max(COLLISION_MAX_ANCHOR_STEP, radius * 2F);
            float lenSq = dx * dx + dy * dy + dz * dz;

            if (lenSq <= maxStep * maxStep)
            {
                return;
            }

            int minBX = MathHelper.floor(Math.min(prev.x, p.x) - radius);
            int minBY = MathHelper.floor(Math.min(prev.y, p.y) - radius);
            int minBZ = MathHelper.floor(Math.min(prev.z, p.z) - radius);
            int maxBX = MathHelper.floor(Math.max(prev.x, p.x) + radius);
            int maxBY = MathHelper.floor(Math.max(prev.y, p.y) + radius);
            int maxBZ = MathHelper.floor(Math.max(prev.z, p.z) + radius);

            if (!TerrainCollisionResolver.hasFullCubeInAabb(world, mutable, minBX, minBY, minBZ, maxBX, maxBY, maxBZ))
            {
                return;
            }

            float inv = maxStep / (float) Math.sqrt(lenSq);
            p.x = prev.x + dx * inv;
            p.y = prev.y + dy * inv;
            p.z = prev.z + dz * inv;
        }
    }

    static final class SpringPhase
    {
        private SpringPhase()
        {
        }

        static void execute(VerletChainSnapshot.Particle[] particles, Quaternionf anchorOrientation, float[] stiffStep, Vector3f poseDir, Vector3f curDir)
        {
            int last = particles.length - 1;

            for (int i = 1; i <= last; i++)
            {
                float k = stiffStep[i];

                if (k <= 0F)
                {
                    continue;
                }

                curDir.set(particles[i].position).sub(particles[i - 1].position);

                float curLen = curDir.length();

                if (curLen < EPS)
                {
                    continue;
                }

                poseDir.set(particles[i].restPoseLocal).sub(particles[i - 1].restPoseLocal);
                anchorOrientation.transform(poseDir);

                float poseLen = poseDir.length();

                if (poseLen < EPS)
                {
                    continue;
                }

                poseDir.div(poseLen);
                curDir.div(curLen);
                curDir.lerp(poseDir, k);

                float blendLen = curDir.length();

                if (blendLen < EPS)
                {
                    curDir.set(poseDir);
                }
                else
                {
                    curDir.div(blendLen);
                }

                particles[i].position.set(particles[i - 1].position).add(curDir.mul(curLen));
            }
        }
    }

    static final class ConstraintPhase
    {
        private ConstraintPhase()
        {
        }

        static void execute(VerletChainSnapshot state, float[] lengths, int iterations, boolean hardTarget, Vector3f targetPosition, int last)
        {
            VerletChainSnapshot.Particle[] particles = state.particles;

            for (int iter = 0; iter < iterations; iter++)
            {
                if (hardTarget)
                {
                    particles[last].position.set(targetPosition);
                }

                lengthBackward(particles, lengths, last);
                particles[0].position.set(state.anchorPoint);
                lengthForward(particles, lengths);

                if (hardTarget)
                {
                    particles[last].position.set(targetPosition);
                }
            }
        }

        static void pinEnds(VerletChainSnapshot.Particle[] particles, Vector3f anchor, Vector3f target, int last)
        {
            particles[0].position.set(anchor);

            if (target != null)
            {
                particles[last].position.set(target);
            }
        }

        static void lengthBackward(VerletChainSnapshot.Particle[] particles, float[] lengths, int last)
        {
            Vector3f dir = new Vector3f();

            for (int i = last - 1; i >= 0; i--)
            {
                Vector3f a = particles[i].position;
                Vector3f b = particles[i + 1].position;

                dir.set(a).sub(b);

                float lenSq = dir.lengthSquared();

                if (lenSq < EPS * EPS)
                {
                    continue;
                }

                dir.mul((float) (lengths[i] / Math.sqrt(lenSq)));
                a.set(b).add(dir);
            }
        }

        static void lengthForward(VerletChainSnapshot.Particle[] particles, float[] lengths)
        {
            Vector3f dir = new Vector3f();

            for (int i = 1; i < particles.length; i++)
            {
                Vector3f a = particles[i - 1].position;
                Vector3f b = particles[i].position;

                dir.set(b).sub(a);

                float lenSq = dir.lengthSquared();

                if (lenSq < EPS * EPS)
                {
                    continue;
                }

                dir.mul((float) (lengths[i - 1] / Math.sqrt(lenSq)));
                b.set(a).add(dir);
            }
        }
    }

    static final class AngleLimitPhase
    {
        private AngleLimitPhase()
        {
        }

        static void execute(SkeletonGeometryAdapter rig, List<String> ids, VerletChainSnapshot.Particle[] particles, float[] lengths, Map<String, JointLimitConfig.JointLimit> jointLimits, Quaternionf rootParentRotation)
        {
            int boneCount = ids.size();

            if (boneCount == 0 || particles == null || particles.length < 2 || lengths == null || lengths.length < 1 || rootParentRotation == null)
            {
                return;
            }

            Quaternionf parentWorld = new Quaternionf(rootParentRotation);

            for (int i = 0; i < boneCount; i++)
            {
                String boneId = ids.get(i);
                String childId = i + 1 < boneCount ? ids.get(i + 1) : null;
                JointLimitConfig.JointLimit c = boneId == null ? null : jointLimits.get(boneId);

                Vector3f restDirLocal = rig.restDirectionLocal(boneId, childId);

                if (restDirLocal == null)
                {
                    return;
                }

                Vector3f desiredDirWorld = new Vector3f(particles[i + 1].position).sub(particles[i].position);

                if (restDirLocal.lengthSquared() < EPS * EPS || desiredDirWorld.lengthSquared() < EPS * EPS)
                {
                    continue;
                }

                restDirLocal.normalize();
                desiredDirWorld.normalize();

                Quaternionf invParent = new Quaternionf(parentWorld).invert();
                Vector3f desiredDirLocal = new Vector3f(desiredDirWorld);
                invParent.transform(desiredDirLocal);

                if (desiredDirLocal.lengthSquared() < EPS * EPS)
                {
                    continue;
                }

                desiredDirLocal.normalize();

                Quaternionf localRot = QuaternionMath.fromToWithMirror(restDirLocal, desiredDirLocal);
                Quaternionf applied = localRot;

                if (c != null && c.active())
                {
                    Vector3f eulerDeg = QuaternionMath.decomposeEulerZYX(localRot);

                    float minX = c.minX();
                    float minY = c.minY();
                    float minZ = c.minZ();
                    float maxX = c.maxX();
                    float maxY = c.maxY();
                    float maxZ = c.maxZ();

                    if (minX > maxX)
                    {
                        float t = minX;
                        minX = maxX;
                        maxX = t;
                    }

                    if (minY > maxY)
                    {
                        float t = minY;
                        minY = maxY;
                        maxY = t;
                    }

                    if (minZ > maxZ)
                    {
                        float t = minZ;
                        minZ = maxZ;
                        maxZ = t;
                    }

                    eulerDeg.x = clampAngleArc(eulerDeg.x, minX, maxX);
                    eulerDeg.y = clampAngleArc(eulerDeg.y, minY, maxY);
                    eulerDeg.z = clampAngleArc(eulerDeg.z, minZ, maxZ);

                    applied = QuaternionMath.composeFromEulerZYX(eulerDeg.x, eulerDeg.y, eulerDeg.z);
                    Vector3f dirLocal = new Vector3f(restDirLocal);
                    applied.transform(dirLocal);
                    parentWorld.transform(dirLocal);

                    if (dirLocal.lengthSquared() >= EPS * EPS)
                    {
                        dirLocal.normalize().mul(lengths[i]);
                        particles[i + 1].position.set(particles[i].position).add(dirLocal);
                    }
                }

                parentWorld.mul(applied);
            }
        }

        private static float clampAngleArc(float angle, float min, float max)
        {
            if (angle >= min && angle <= max)
            {
                return angle;
            }

            return circularDistance(angle, min) <= circularDistance(angle, max) ? min : max;
        }

        private static float circularDistance(float a, float b)
        {
            float d = Math.abs(a - b) % 360F;

            return d > 180F ? 360F - d : d;
        }
    }

    static final class CollisionPhase
    {
        private CollisionPhase()
        {
        }

        static void execute(World world, VerletChainSnapshot state, Vector3f target, int last, float radius)
        {
            int to = target != null ? last : state.particles.length;
            Vector3f[] pos = positionsView(state);
            Vector3f[] prev = previousView(state);

            TerrainCollisionResolver.resolve(world, pos, prev, 1, to, radius, COLLISION_FRICTION);
            ConstraintPhase.pinEnds(state.particles, state.anchorPoint, target, last);
        }
    }
}
