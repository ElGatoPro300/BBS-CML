package mchorse.bbs_mod.cubic.ik;

import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * FABRIK (Forward And Backward Reaching Inverse Kinematics) solver.
 *
 * This implementation is stateless and works purely on arrays of 3D positions.
 * It is significantly simpler than the Jacobian-based solver in Blender's
 * iksolver (in open-source/blender-5.1.0/intern/iksolver) while still
 * producing visually correct results for short bone chains.
 *
 * Algorithm (per iteration):
 *   1. BACKWARD pass: move tip to target, then drag each bone toward the next.
 *   2. FORWARD  pass: anchor root, then push each bone toward the next.
 * Repeat until tip is within tolerance of the target, or max iterations hit.
 */
public final class IKSolver
{
    private IKSolver() {}

    /**
     * Solves a bone chain using the FABRIK algorithm.
     *
     * @param positions  World-space positions of each joint, length N.
     *                   positions[0]   = root joint (fixed anchor).
     *                   positions[N-1] = tip joint (end-effector, moved to target).
     *                   Modified IN PLACE.
     * @param lengths    Bone lengths between joints, length N-1.
     *                   lengths[i] = distance between positions[i] and positions[i+1].
     * @param target     Desired world-space position of the tip.
     * @param iterations Maximum FABRIK iterations.
     * @param tolerance  Convergence threshold in world units (meters).
     */
    public static void solve(Vector3f[] positions, float[] lengths,
                             Vector3f target, int iterations, float tolerance)
    {
        if (positions == null || positions.length < 2 || lengths == null)
        {
            return;
        }

        /* Store the root anchor – it must never move */
        Vector3f root = new Vector3f(positions[0]);

        /* Total chain length */
        float totalLength = 0F;

        for (float l : lengths)
        {
            totalLength += l;
        }

        float distToTarget = positions[0].distance(target);

        /* If target is unreachable, fully stretch the chain toward it */
        if (distToTarget >= totalLength)
        {
            Vector3f dir = new Vector3f(target).sub(positions[0]).normalize();

            for (int i = 1; i < positions.length; i++)
            {
                positions[i].set(positions[i - 1]).add(new Vector3f(dir).mul(lengths[i - 1]));
            }

            return;
        }

        /* FABRIK iteration loop */
        for (int iter = 0; iter < iterations; iter++)
        {
            /* --- BACKWARD pass (tip → root) --- */
            positions[positions.length - 1].set(target);

            for (int i = positions.length - 2; i >= 0; i--)
            {
                Vector3f dir = new Vector3f(positions[i]).sub(positions[i + 1]);

                float len = dir.length();

                if (len > 1e-6F)
                {
                    dir.div(len);
                }
                else
                {
                    dir.set(0F, 1F, 0F);
                }

                positions[i].set(positions[i + 1]).add(dir.mul(lengths[i]));
            }

            /* --- FORWARD pass (root → tip) --- */
            positions[0].set(root);

            for (int i = 1; i < positions.length; i++)
            {
                Vector3f dir = new Vector3f(positions[i]).sub(positions[i - 1]);

                float len = dir.length();

                if (len > 1e-6F)
                {
                    dir.div(len);
                }
                else
                {
                    dir.set(0F, 1F, 0F);
                }

                positions[i].set(positions[i - 1]).add(dir.mul(lengths[i - 1]));
            }

            /* Check convergence */
            if (positions[positions.length - 1].distance(target) < tolerance)
            {
                break;
            }
        }
    }

    /**
     * Blends two position arrays by a factor t (0 = a, 1 = b).
     * Used to apply IK weight blending.
     *
     * @param a  FK (original) positions.
     * @param b  IK (solved) positions.
     * @param t  Blend factor [0, 1].
     * @param out Output array (may be the same reference as b).
     */
    public static void blend(Vector3f[] a, Vector3f[] b, float t, Vector3f[] out)
    {
        for (int i = 0; i < out.length; i++)
        {
            out[i].set(a[i]).lerp(b[i], t);
        }
    }

    /**
     * Computes the rotation delta (as Euler angles in RADIANS) that a bone must
     * apply so that its tail moves from {@code from} to {@code to}, given its
     * current world-space basis.
     *
     * @param boneOrigin    World-space position of the bone head (pivot).
     * @param boneDirection Current world-space direction vector of the bone (normalized).
     * @param targetEnd     Desired world-space position of the bone tail.
     * @param out           Output Euler angles (radians) applied as XYZ intrinsic rotations.
     */
    public static void computeBoneRotation(Vector3f boneOrigin,
                                           Vector3f boneDirection,
                                           Vector3f targetEnd,
                                           Vector3f out)
    {
        Vector3f desired = new Vector3f(targetEnd).sub(boneOrigin).normalize();
        Vector3f current = new Vector3f(boneDirection).normalize();

        float dot = current.dot(desired);

        dot = Math.max(-1F, Math.min(1F, dot));

        if (dot >= 1F - 1e-6F)
        {
            /* Already aligned */
            out.set(0F, 0F, 0F);
            return;
        }

        if (dot <= -1F + 1e-6F)
        {
            /* 180 degrees flip – use an arbitrary perpendicular axis */
            Vector3f perp = new Vector3f(1F, 0F, 0F);

            if (Math.abs(current.dot(perp)) > 0.9F)
            {
                perp.set(0F, 1F, 0F);
            }

            current.cross(perp, perp).normalize();

            Quaternionf q = new Quaternionf().fromAxisAngleRad(perp.x, perp.y, perp.z, (float) Math.PI);
            float[] euler = quaternionToEulerXYZ(q);

            out.set(euler[0], euler[1], euler[2]);

            return;
        }

        Vector3f axis = new Vector3f();
        current.cross(desired, axis).normalize();

        float angle = (float) Math.acos(dot);

        Quaternionf q = new Quaternionf().fromAxisAngleRad(axis.x, axis.y, axis.z, angle);
        float[] euler = quaternionToEulerXYZ(q);

        out.set(euler[0], euler[1], euler[2]);
    }

    /**
     * Converts a quaternion to XYZ intrinsic Euler angles (radians).
     *
     * @return float[3] {x, y, z} in radians.
     */
    private static float[] quaternionToEulerXYZ(Quaternionf q)
    {
        float[] euler = new float[3];

        /* XYZ intrinsic: Rx * Ry * Rz */
        float sinY = 2F * (q.w * q.y - q.z * q.x);

        if (Math.abs(sinY) >= 1F)
        {
            euler[1] = (float) (Math.signum(sinY) * Math.PI / 2.0);
            euler[0] = (float) Math.atan2(2F * (q.x * q.y - q.w * q.z), 1F - 2F * (q.y * q.y + q.z * q.z));
            euler[2] = 0F;
        }
        else
        {
            euler[1] = (float) Math.asin(sinY);
            euler[0] = (float) Math.atan2(2F * (q.w * q.x + q.y * q.z), 1F - 2F * (q.x * q.x + q.y * q.y));
            euler[2] = (float) Math.atan2(2F * (q.w * q.z + q.x * q.y), 1F - 2F * (q.y * q.y + q.z * q.z));
        }

        return euler;
    }
}
