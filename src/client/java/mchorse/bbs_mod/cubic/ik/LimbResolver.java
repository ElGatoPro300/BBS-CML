package mchorse.bbs_mod.cubic.ik;

import mchorse.bbs_mod.utils.joml.QuaternionMath;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * Single-chain IK: positions are solved to reach the goal (analytic two-bone,
 * FABRIK for longer unconstrained chains, constrained CCD when joint limits
 * are involved), then the bend plane is aimed about the root-to-tip axis.
 *
 * <p>Orchestration: {@code clampGoal} → strategy {@code resolve} →
 * {@code aimBendPlane} → {@code enforceBendLimits}.
 */
public final class LimbResolver
{
    public static final float EPS = 1.0e-6f;
    private static final float REACH_LIMIT = 1F;

    private LimbResolver()
    {
    }

    /**
     * Per-bone rotation limit for constrained CCD. {@code restDir} is the bone's
     * local rest direction toward its child; min/max are euler ZYX degrees.
     */
    public record Limit(boolean enabled, Vector3f restDir, float minX, float minY, float minZ, float maxX, float maxY, float maxZ)
    {
    }

    /**
     * Shared solve state passed into resolution strategies.
     */
    public static final class ResolverContext
    {
        public Vector3f root;
        public boolean applyPole;
        public Vector3f polePoint;
        public float bendOffsetRad;
        public float flexibility;
        public int maxIterations;
        public float tolerance;
        public Limit[] limits;
        public Quaternionf rootParentRotation;
        public Vector3f restHinge;
        public Vector3f outBendNormal;
        public Vector3f hinge;
    }

    interface IResolutionStrategy
    {
        void resolve(List<Vector3f> joints, Vector3f goal, ResolverContext ctx);
    }

    static final class TwoBoneStrategy implements IResolutionStrategy
    {
        @Override
        public void resolve(List<Vector3f> joints, Vector3f goal, ResolverContext ctx)
        {
            Vector3f root = ctx.root;
            float l1 = root.distance(joints.get(1));
            float l2 = joints.get(1).distance(joints.get(2));
            Vector3f dir = new Vector3f(goal).sub(root);
            float dist = dir.length();

            if (dist < EPS)
            {
                return;
            }

            dir.div(dist);

            float cosA = (l1 * l1 + dist * dist - l2 * l2) / (2F * l1 * dist);

            cosA = Math.max(-1F, Math.min(1F, cosA));

            float sinA = (float) Math.sqrt(Math.max(0F, 1F - cosA * cosA));
            Vector3f upper = new Vector3f(joints.get(1)).sub(root);
            Vector3f bend = perpendicular(root, joints.get(1), goal);

            if (bend == null)
            {
                bend = bendFromReach(dir, upper);

                if (bend == null)
                {
                    bend = new Vector3f();
                    anyPerpendicular(dir, bend);
                }
            }

            joints.get(1).set(root).fma(l1 * cosA, dir).fma(l1 * sinA, bend);
            joints.get(2).set(goal);
        }
    }

    static final class FabrikStrategy implements IResolutionStrategy
    {
        @Override
        public void resolve(List<Vector3f> joints, Vector3f goal, ResolverContext ctx)
        {
            int n = joints.size();
            float tolSq = ctx.tolerance * ctx.tolerance;
            float[] lengths = new float[n - 1];

            for (int i = 0; i < n - 1; i++)
            {
                lengths[i] = joints.get(i).distance(joints.get(i + 1));
            }

            Vector3f dir = new Vector3f();

            for (int iter = 0; iter < ctx.maxIterations; iter++)
            {
                if (joints.get(n - 1).distanceSquared(goal) <= tolSq)
                {
                    break;
                }

                joints.get(n - 1).set(goal);

                for (int i = n - 2; i >= 0; i--)
                {
                    dir.set(joints.get(i)).sub(joints.get(i + 1));

                    if (!normalize(dir))
                    {
                        continue;
                    }

                    joints.get(i).set(joints.get(i + 1)).fma(lengths[i], dir);
                }

                joints.get(0).set(ctx.root);

                for (int i = 0; i < n - 1; i++)
                {
                    dir.set(joints.get(i + 1)).sub(joints.get(i));

                    if (!normalize(dir))
                    {
                        continue;
                    }

                    joints.get(i + 1).set(joints.get(i)).fma(lengths[i], dir);
                }
            }
        }
    }

    static final class ConstrainedCCDStrategy implements IResolutionStrategy
    {
        @Override
        public void resolve(List<Vector3f> joints, Vector3f goal, ResolverContext ctx)
        {
            int n = joints.size();
            float tolSq = ctx.tolerance * ctx.tolerance;
            Limit[] limits = ctx.limits;
            Quaternionf[] parentWorld = limits == null ? null : new Quaternionf[n];

            for (int iter = 0; iter < ctx.maxIterations; iter++)
            {
                if (joints.get(n - 1).distanceSquared(goal) <= tolSq)
                {
                    break;
                }

                if (limits != null)
                {
                    computeParentFrames(joints, limits, ctx.rootParentRotation, parentWorld);
                }

                ccdSweep(joints, goal, limits, parentWorld);
                joints.get(0).set(ctx.root);
            }
        }
    }

    public static List<Vector3f> resolve(List<Vector3f> positions, Vector3f target, boolean applyPole, Vector3f polePoint, float bendOffsetRad, float flexibility, int maxIterations, float tolerance)
    {
        return resolve(positions, target, applyPole, polePoint, bendOffsetRad, flexibility, maxIterations, tolerance, null, null, null, null);
    }

    public static List<Vector3f> resolve(List<Vector3f> positions, Vector3f target, boolean applyPole, Vector3f polePoint, float bendOffsetRad, float flexibility, int maxIterations, float tolerance, Limit[] limits, Quaternionf rootParentRotation, Vector3f restHinge, Vector3f outBendNormal)
    {
        return resolve(positions, target, applyPole, polePoint, bendOffsetRad, flexibility, maxIterations, tolerance, limits, rootParentRotation, restHinge, outBendNormal, false);
    }

    public static List<Vector3f> resolve(List<Vector3f> positions, Vector3f target, boolean applyPole, Vector3f polePoint, float bendOffsetRad, float flexibility, int maxIterations, float tolerance, Limit[] limits, Quaternionf rootParentRotation, Vector3f restHinge, Vector3f outBendNormal, boolean invertBendPlane)
    {
        int n = positions.size();

        if (n < 2)
        {
            return positions;
        }

        float total = 0F;

        for (int i = 0; i < n - 1; i++)
        {
            total += positions.get(i).distance(positions.get(i + 1));
        }

        if (total <= EPS)
        {
            return positions;
        }

        ResolverContext ctx = new ResolverContext();

        ctx.root = new Vector3f(positions.get(0));
        ctx.applyPole = applyPole;
        ctx.polePoint = polePoint;
        ctx.bendOffsetRad = bendOffsetRad;
        ctx.flexibility = flexibility;
        ctx.maxIterations = maxIterations;
        ctx.tolerance = tolerance;
        ctx.limits = limits;
        ctx.rootParentRotation = rootParentRotation;
        ctx.restHinge = restHinge;
        ctx.outBendNormal = outBendNormal;

        Vector3f goal = clampGoal(ctx.root, target, total, flexibility);

        if (n == 3)
        {
            ctx.hinge = liveBendNormal(positions);

            if (ctx.hinge == null)
            {
                ctx.hinge = restHinge;
            }

            if (ctx.hinge == null)
            {
                Vector3f limb = new Vector3f(positions.get(2)).sub(positions.get(0));

                ctx.hinge = normalize(limb) ? sideAxis(limb) : null;
            }

            ctx.hinge = refineTwoBoneHinge(ctx.hinge, ctx.root, goal, positions);
        }
        else
        {
            ctx.hinge = applyPole ? captureHingeAxis(positions) : null;
        }

        boolean constrained = limits != null && rootParentRotation != null;
        IResolutionStrategy strategy;

        if (n == 3)
        {
            strategy = new TwoBoneStrategy();
        }
        else if (constrained)
        {
            strategy = new ConstrainedCCDStrategy();
        }
        else
        {
            strategy = new FabrikStrategy();
        }

        strategy.resolve(positions, goal, ctx);

        if (invertBendPlane && ctx.hinge != null)
        {
            ctx.hinge.negate();
        }

        Vector3f bendNormal = aimBendPlane(positions, ctx.hinge, polePoint, bendOffsetRad);

        if (n == 3 && constrained)
        {
            enforceBendLimits(positions, limits, rootParentRotation);
        }

        if (outBendNormal != null && bendNormal != null)
        {
            outBendNormal.set(bendNormal);
        }

        return positions;
    }

    private static Vector3f clampGoal(Vector3f root, Vector3f target, float total, float flexibility)
    {
        Vector3f goal = new Vector3f(target);
        float dist = root.distance(target);

        if (dist < EPS)
        {
            return goal;
        }

        Vector3f dir = new Vector3f(target).sub(root).div(dist);

        if (flexibility > EPS)
        {
            float soft = Math.min(flexibility, 1F) * total;
            float da = total - soft;

            if (dist > da)
            {
                float eff = total - soft * (float) Math.exp(-(dist - da) / soft);

                goal.set(root).fma(Math.min(eff, total * REACH_LIMIT), dir);
            }
        }
        else if (dist > total * REACH_LIMIT)
        {
            goal.set(root).fma(total * REACH_LIMIT, dir);
        }

        return goal;
    }

    private static void computeParentFrames(List<Vector3f> p, Limit[] limits, Quaternionf rootParentRotation, Quaternionf[] parentWorld)
    {
        int n = p.size();
        Vector3f dirWorld = new Vector3f();
        Vector3f dirLocal = new Vector3f();

        parentWorld[0] = new Quaternionf(rootParentRotation);

        for (int i = 0; i < n - 1; i++)
        {
            Quaternionf local = localRotation(p, i, limits[i], parentWorld[i], dirWorld, dirLocal);

            parentWorld[i + 1] = new Quaternionf(parentWorld[i]);

            if (local != null)
            {
                Vector3f euler = QuaternionMath.decomposeEulerZYX(local);

                parentWorld[i + 1].mul(QuaternionMath.composeFromEulerZYX(euler.x, euler.y, euler.z));
            }
        }
    }

    private static Quaternionf localRotation(List<Vector3f> p, int i, Limit lim, Quaternionf parentWorld, Vector3f dirWorld, Vector3f dirLocal)
    {
        if (lim == null || lim.restDir() == null)
        {
            return null;
        }

        dirWorld.set(p.get(i + 1)).sub(p.get(i));

        if (!normalize(dirWorld))
        {
            return null;
        }

        dirLocal.set(dirWorld);
        new Quaternionf(parentWorld).conjugate().transform(dirLocal);

        if (!normalize(dirLocal))
        {
            return null;
        }

        return QuaternionMath.fromToWithMirror(lim.restDir(), dirLocal);
    }

    private static void ccdSweep(List<Vector3f> p, Vector3f goal, Limit[] limits, Quaternionf[] parentWorld)
    {
        int n = p.size();
        Vector3f toEff = new Vector3f();
        Vector3f toGoal = new Vector3f();
        Vector3f dirWorld = new Vector3f();
        Vector3f dirLocal = new Vector3f();
        Vector3f rel = new Vector3f();

        for (int j = n - 2; j >= 0; j--)
        {
            Vector3f pj = p.get(j);

            toEff.set(p.get(n - 1)).sub(pj);
            toGoal.set(goal).sub(pj);

            if (toEff.lengthSquared() < EPS * EPS || toGoal.lengthSquared() < EPS * EPS)
            {
                continue;
            }

            Quaternionf free = new Quaternionf().rotationTo(toEff, toGoal);
            Quaternionf q = free;
            Limit lim = limits == null ? null : limits[j];

            if (lim != null && lim.enabled())
            {
                q = restrictToLimit(p, j, lim, parentWorld[j], free, dirWorld, dirLocal);
            }

            if (q == null)
            {
                continue;
            }

            for (int k = j + 1; k < n; k++)
            {
                rel.set(p.get(k)).sub(pj);
                q.transform(rel);
                p.get(k).set(pj).add(rel);
            }
        }
    }

    private static Quaternionf restrictToLimit(List<Vector3f> p, int j, Limit lim, Quaternionf parentWorld, Quaternionf free, Vector3f dirWorld, Vector3f dirLocal)
    {
        Quaternionf curLocal = localRotation(p, j, lim, parentWorld, dirWorld, dirLocal);

        if (curLocal == null)
        {
            return free;
        }

        Quaternionf invParent = new Quaternionf(parentWorld).conjugate();
        Quaternionf candLocal = new Quaternionf(invParent).mul(free).mul(parentWorld).mul(curLocal);
        Vector3f euler = QuaternionMath.decomposeEulerZYX(candLocal);

        float cx = clamp(euler.x, lim.minX(), lim.maxX());
        float cy = clamp(euler.y, lim.minY(), lim.maxY());
        float cz = clamp(euler.z, lim.minZ(), lim.maxZ());

        Quaternionf clampedLocal = QuaternionMath.composeFromEulerZYX(cx, cy, cz);
        Quaternionf curBoneWorld = new Quaternionf(parentWorld).mul(curLocal);
        Quaternionf clampedBoneWorld = new Quaternionf(parentWorld).mul(clampedLocal);

        return clampedBoneWorld.mul(curBoneWorld.conjugate());
    }

    private static void enforceBendLimits(List<Vector3f> p, Limit[] limits, Quaternionf rootParentRotation)
    {
        int n = p.size();

        if (n < 3)
        {
            return;
        }

        Vector3f root = new Vector3f(p.get(0));
        Vector3f axis = new Vector3f(p.get(n - 1)).sub(root);

        if (!normalize(axis))
        {
            return;
        }

        float bestPhi = 0F;
        float bestCost = bendCost(p, limits, rootParentRotation, root, axis, 0F);

        if (bestCost > EPS)
        {
            int half = 24;
            float window = (float) (Math.PI / 2.0);

            for (int s = -half; s <= half; s++)
            {
                if (s == 0)
                {
                    continue;
                }

                float phi = window * s / half;
                float cost = bendCost(p, limits, rootParentRotation, root, axis, phi);

                if (cost < bestCost)
                {
                    bestCost = cost;
                    bestPhi = phi;
                }
            }

            float step = window / half;
            float center = bestPhi;

            for (int s = -5; s <= 5; s++)
            {
                float phi = center + step * s / 5F;

                if (Math.abs(phi) > window)
                {
                    continue;
                }

                float cost = bendCost(p, limits, rootParentRotation, root, axis, phi);

                if (cost < bestCost)
                {
                    bestCost = cost;
                    bestPhi = phi;
                }
            }
        }

        if (Math.abs(bestPhi) > EPS)
        {
            Quaternionf q = new Quaternionf().fromAxisAngleRad(axis.x, axis.y, axis.z, bestPhi);
            Vector3f rel = new Vector3f();

            for (int i = 1; i < n - 1; i++)
            {
                rel.set(p.get(i)).sub(root);
                q.transform(rel);
                p.get(i).set(root).add(rel);
            }
        }
    }

    private static float bendCost(List<Vector3f> p, Limit[] limits, Quaternionf rootParentRotation, Vector3f root, Vector3f axis, float phi)
    {
        return bendViolation(p, limits, rootParentRotation, root, axis, phi) + 0.01F * Math.abs((float) Math.toDegrees(phi));
    }

    private static float bendViolation(List<Vector3f> p, Limit[] limits, Quaternionf rootParentRotation, Vector3f root, Vector3f axis, float phi)
    {
        int n = p.size();
        Quaternionf q = new Quaternionf().fromAxisAngleRad(axis.x, axis.y, axis.z, phi);
        Quaternionf parentWorld = new Quaternionf(rootParentRotation);
        Vector3f a = new Vector3f();
        Vector3f b = new Vector3f();
        Vector3f dirWorld = new Vector3f();
        Vector3f dirLocal = new Vector3f();
        Vector3f rel = new Vector3f();
        float violation = 0F;

        for (int i = 0; i < n - 1; i++)
        {
            Limit lim = i < limits.length ? limits[i] : null;

            if (lim == null || lim.restDir() == null)
            {
                continue;
            }

            rotatedJoint(p, i, root, q, rel, a);
            rotatedJoint(p, i + 1, root, q, rel, b);
            dirWorld.set(b).sub(a);

            if (!normalize(dirWorld))
            {
                continue;
            }

            dirLocal.set(dirWorld);
            new Quaternionf(parentWorld).conjugate().transform(dirLocal);

            if (!normalize(dirLocal))
            {
                continue;
            }

            Vector3f euler = QuaternionMath.decomposeEulerZYX(QuaternionMath.fromToWithMirror(lim.restDir(), dirLocal));

            if (lim.enabled())
            {
                violation += overflow(euler.x, lim.minX(), lim.maxX());
                violation += overflow(euler.y, lim.minY(), lim.maxY());
                violation += overflow(euler.z, lim.minZ(), lim.maxZ());
            }

            parentWorld.mul(QuaternionMath.composeFromEulerZYX(euler.x, euler.y, euler.z));
        }

        return violation;
    }

    private static void rotatedJoint(List<Vector3f> p, int i, Vector3f root, Quaternionf q, Vector3f tmp, Vector3f out)
    {
        if (i == 0 || i == p.size() - 1)
        {
            out.set(p.get(i));

            return;
        }

        tmp.set(p.get(i)).sub(root);
        q.transform(tmp);
        out.set(root).add(tmp);
    }

    private static float overflow(float value, float min, float max)
    {
        if (min > max)
        {
            float t = min;

            min = max;
            max = t;
        }

        if (value < min)
        {
            return min - value;
        }

        return value > max ? value - max : 0F;
    }

    private static Vector3f refineTwoBoneHinge(Vector3f hinge, Vector3f root, Vector3f goal, List<Vector3f> positions)
    {
        Vector3f axis = new Vector3f(goal).sub(root);
        Vector3f upper = new Vector3f(positions.get(1)).sub(positions.get(0));

        if (!normalize(axis) || !normalize(upper))
        {
            return hinge;
        }

        boolean parallel = hinge == null || Math.abs(hinge.dot(axis)) > 0.9F;

        if (!parallel)
        {
            return hinge;
        }

        Vector3f refined = new Vector3f(upper).cross(axis);

        if (!normalize(refined))
        {
            refined = new Vector3f(axis).cross(upper);

            if (!normalize(refined))
            {
                return hinge;
            }
        }

        return refined;
    }

    /**
     * Bend direction in the reach plane when the posed elbow lies on the root-to-goal
     * line (straight BOBJ limbs). {@code cross(cross(dir, upper), dir)} is the
     * component of {@code upper} perpendicular to {@code dir}.
     */
    private static Vector3f bendFromReach(Vector3f dir, Vector3f upper)
    {
        if (!normalize(upper))
        {
            return null;
        }

        Vector3f bend = new Vector3f(dir).cross(upper).cross(dir);

        return normalize(bend) ? bend : null;
    }

    private static Vector3f aimBendPlane(List<Vector3f> p, Vector3f hinge, Vector3f polePoint, float bendOffsetRad)
    {
        int n = p.size();

        if (n < 3)
        {
            return null;
        }

        Vector3f root = p.get(0);
        Vector3f axis = new Vector3f(p.get(n - 1)).sub(root);

        if (!normalize(axis))
        {
            return null;
        }

        Vector3f desired = new Vector3f();

        if (polePoint != null)
        {
            desired.set(polePoint).sub(root);

            if (!project(desired, axis))
            {
                return null;
            }
        }
        else if (hinge != null)
        {
            desired.set(axis).cross(hinge);

            if (!project(desired, axis))
            {
                return null;
            }
        }
        else
        {
            return null;
        }

        if (bendOffsetRad != 0F)
        {
            new Quaternionf().fromAxisAngleRad(axis.x, axis.y, axis.z, bendOffsetRad).transform(desired);
        }

        orientBendTo(p, root, axis, desired);

        Vector3f normal = new Vector3f(desired).cross(axis);

        return normalize(normal) ? normal : null;
    }

    private static void orientBendTo(List<Vector3f> p, Vector3f root, Vector3f axis, Vector3f desired)
    {
        Vector3f current = new Vector3f(p.get(1)).sub(root);

        if (!project(current, axis))
        {
            return;
        }

        float theta = signedAngle(current, desired, axis);

        if (Math.abs(theta) < EPS)
        {
            return;
        }

        Quaternionf q = new Quaternionf().fromAxisAngleRad(axis.x, axis.y, axis.z, theta);
        Vector3f rel = new Vector3f();

        for (int i = 1; i < p.size() - 1; i++)
        {
            rel.set(p.get(i)).sub(root);
            q.transform(rel);
            p.get(i).set(root).add(rel);
        }
    }

    private static Vector3f captureHingeAxis(List<Vector3f> p)
    {
        Vector3f normal = liveBendNormal(p);

        if (normal != null)
        {
            return normal;
        }

        Vector3f limb = new Vector3f(p.get(p.size() - 1)).sub(p.get(0));

        return normalize(limb) ? sideAxis(limb) : null;
    }

    private static Vector3f liveBendNormal(List<Vector3f> p)
    {
        if (p.size() < 3)
        {
            return null;
        }

        Vector3f a = p.get(0);
        Vector3f normal = new Vector3f(p.get(1)).sub(a).cross(new Vector3f(p.get(2)).sub(a));

        return normalize(normal) ? normal : null;
    }

    private static Vector3f sideAxis(Vector3f axis)
    {
        Vector3f side = new Vector3f(axis).cross(0F, 0F, 1F);

        if (normalize(side))
        {
            return side;
        }

        side = new Vector3f(axis).cross(0F, 1F, 0F);

        return normalize(side) ? side : null;
    }

    private static float clamp(float value, float min, float max)
    {
        if (min > max)
        {
            float t = min;

            min = max;
            max = t;
        }

        return value < min ? min : Math.min(value, max);
    }

    private static Vector3f perpendicular(Vector3f a, Vector3f b, Vector3f c)
    {
        Vector3f axis = new Vector3f(c).sub(a);

        if (!normalize(axis))
        {
            return null;
        }

        Vector3f out = new Vector3f(b).sub(a);

        return project(out, axis) ? out : null;
    }

    private static void anyPerpendicular(Vector3f axis, Vector3f out)
    {
        Vector3f ref = Math.abs(axis.x) < 0.9F ? new Vector3f(1F, 0F, 0F) : new Vector3f(0F, 1F, 0F);

        out.set(axis).cross(ref);

        if (!normalize(out))
        {
            out.set(0F, 1F, 0F);
        }
    }

    private static float signedAngle(Vector3f from, Vector3f to, Vector3f axis)
    {
        Vector3f cross = new Vector3f(from).cross(to);
        float sin = axis.dot(cross);
        float cos = from.dot(to);

        return (float) Math.atan2(sin, cos);
    }

    private static boolean project(Vector3f v, Vector3f axis)
    {
        float dot = v.dot(axis);

        v.x -= axis.x * dot;
        v.y -= axis.y * dot;
        v.z -= axis.z * dot;

        return normalize(v);
    }

    private static boolean normalize(Vector3f v)
    {
        float lenSq = v.lengthSquared();

        if (lenSq <= EPS * EPS)
        {
            return false;
        }

        v.mul(1F / (float) Math.sqrt(lenSq));

        return true;
    }
}
