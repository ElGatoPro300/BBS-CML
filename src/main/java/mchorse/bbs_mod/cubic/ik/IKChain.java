package mchorse.bbs_mod.cubic.ik;

import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.model.IKChainConfig;

import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Runtime IK chain that wraps an {@link IKChainConfig} and performs
 * a FABRIK solve every time {@link #solve(IModel)} is called.
 *
 * <h3>Algorithm (inspired by Blockbench FIK / Chain3D)</h3>
 * <ol>
 *   <li>Collect world-space pivot positions of every bone in the chain
 *       from the model's {@code current.pivot} (already set by FK).</li>
 *   <li>Find the IK target: either the manual X/Y/Z or the world-space
 *       pivot of a designated <em>locator bone</em> ({@code target_bone}).</li>
 *   <li>Run FABRIK in world space.</li>
 *   <li>For each bone in the chain, compute the <em>delta rotation</em>
 *       in that bone's local parent space and ADD it to {@code current.rotate}.</li>
 * </ol>
 *
 * Both Cubic (ModelGroup) and BOBJ (BOBJBone) models are supported.
 */
public class IKChain
{
    private final IKChainConfig config;

    /* Cached chain data — rebuilt when tip/root names change */
    private List<ModelGroup> cubicChain = new ArrayList<>();
    private List<BOBJBone>   bobjChain  = new ArrayList<>();
    private float[]          lengths    = new float[0];
    private String           cachedTip  = null;
    private String           cachedRoot = null;

    /* FABRIK work buffers reused every frame to avoid GC pressure */
    private Vector3f[] fkPositions  = new Vector3f[0];
    private Vector3f[] ikPositions  = new Vector3f[0];

    public IKChain(IKChainConfig config)
    {
        this.config = config;
    }

    public IKChainConfig getConfig()
    {
        return this.config;
    }

    /**
     * Solves the IK chain and writes rotations back into the model's groups/bones.
     * Must be called AFTER FK animation has been applied for this frame.
     */
    public void solve(IModel model)
    {
        if (!this.config.enabled.get())
        {
            return;
        }

        String tip  = this.config.tipBone.get();
        String root = this.config.rootBone.get();

        if (tip.isEmpty() || root.isEmpty())
        {
            return;
        }

        /* Rebuild chain cache if bone names changed */
        if (!tip.equals(this.cachedTip) || !root.equals(this.cachedRoot))
        {
            this.rebuildChain(model, tip, root);
            this.cachedTip  = tip;
            this.cachedRoot = root;
        }

        float weight = Math.max(0F, Math.min(1F, this.config.weight.get()));

        if (weight < 1e-4F)
        {
            return;
        }

        /* Resolve the IK target position */
        Vector3f target = this.resolveTarget(model);

        if (!this.cubicChain.isEmpty())
        {
            this.solveCubic(target, weight);
        }
        else if (!this.bobjChain.isEmpty())
        {
            this.solveBobj(target, weight);
        }
    }

    /**
     * Resolves the effective IK target in world/model space (units = blocks/16 = meters).
     * Priority: locator bone > manual X/Y/Z.
     */
    private Vector3f resolveTarget(IModel model)
    {
        String targetBoneName = this.config.targetBone.get();

        if (!targetBoneName.isEmpty())
        {
            /* Look for a locator bone in the model hierarchy */
            ModelGroup locator = this.findGroup(model, targetBoneName);

            if (locator != null)
            {
                /* current.pivot is the world-space pivot of the group in blocks */
                return new Vector3f(
                    locator.current.pivot.x / 16F,
                    locator.current.pivot.y / 16F,
                    locator.current.pivot.z / 16F
                );
            }

            /* Try BOBJ bones */
            BOBJBone locatorBobj = this.findBobjBone(model, targetBoneName);

            if (locatorBobj != null)
            {
                return new Vector3f(locatorBobj.transform.translate);
            }
        }

        /* Fallback to manual target */
        return new Vector3f(this.config.target.get());
    }

    /* -----------------------------------------------------------------------
     * Chain building
     * --------------------------------------------------------------------- */

    private void rebuildChain(IModel model, String tipName, String rootName)
    {
        this.cubicChain.clear();
        this.bobjChain.clear();

        Collection<ModelGroup> groups = model.getAllGroups();

        if (!groups.isEmpty())
        {
            this.buildCubicChain(groups, tipName, rootName);
        }

        if (this.cubicChain.isEmpty())
        {
            Collection<BOBJBone> bones = model.getAllBOBJBones();

            if (!bones.isEmpty())
            {
                this.buildBobjChain(bones, tipName, rootName);
            }
        }

        this.allocateBuffers();
    }

    private void buildCubicChain(Collection<ModelGroup> groups, String tipName, String rootName)
    {
        /* Find the tip group */
        ModelGroup tip = null;

        for (ModelGroup g : groups)
        {
            if (g.id.equals(tipName))
            {
                tip = g;
                break;
            }
        }

        if (tip == null)
        {
            return;
        }

        int maxLength = this.config.chainLength.get();
        List<ModelGroup> chain = new ArrayList<>();
        ModelGroup current = tip;

        /* Walk up the hierarchy from tip → root */
        while (current != null)
        {
            chain.add(current);

            if (current.id.equals(rootName))
            {
                break;
            }

            if (maxLength > 0 && chain.size() >= maxLength)
            {
                break;
            }

            current = current.parent;
        }

        if (chain.size() < 2)
        {
            return;
        }

        /* Reverse so chain goes root → tip (matches FABRIK convention) */
        for (int i = 0, j = chain.size() - 1; i < j; i++, j--)
        {
            ModelGroup tmp = chain.get(i);
            chain.set(i, chain.get(j));
            chain.set(j, tmp);
        }

        this.cubicChain = chain;
    }

    private void buildBobjChain(Collection<BOBJBone> bones, String tipName, String rootName)
    {
        BOBJBone tip = null;

        for (BOBJBone b : bones)
        {
            if (b.name.equals(tipName))
            {
                tip = b;
                break;
            }
        }

        if (tip == null)
        {
            return;
        }

        int maxLength = this.config.chainLength.get();
        List<BOBJBone> chain = new ArrayList<>();
        BOBJBone current = tip;

        while (current != null)
        {
            chain.add(current);

            if (current.name.equals(rootName))
            {
                break;
            }

            if (maxLength > 0 && chain.size() >= maxLength)
            {
                break;
            }

            current = current.parentBone;
        }

        if (chain.size() < 2)
        {
            return;
        }

        for (int i = 0, j = chain.size() - 1; i < j; i++, j--)
        {
            BOBJBone tmp = chain.get(i);
            chain.set(i, chain.get(j));
            chain.set(j, tmp);
        }

        this.bobjChain = chain;
    }

    private void allocateBuffers()
    {
        int n = Math.max(this.cubicChain.size(), this.bobjChain.size());

        if (n < 2)
        {
            this.lengths     = new float[0];
            this.fkPositions = new Vector3f[0];
            this.ikPositions = new Vector3f[0];
            return;
        }

        this.lengths     = new float[n - 1];
        this.fkPositions = new Vector3f[n];
        this.ikPositions = new Vector3f[n];

        for (int i = 0; i < n; i++)
        {
            this.fkPositions[i] = new Vector3f();
            this.ikPositions[i] = new Vector3f();
        }
    }

    /* -----------------------------------------------------------------------
     * Cubic (ModelGroup) solver
     * --------------------------------------------------------------------- */

    private void solveCubic(Vector3f target, float weight)
    {
        int n = this.cubicChain.size();

        if (n < 2 || this.fkPositions.length < n)
        {
            return;
        }

        /*
         * Extract world-space pivot positions.
         * ModelGroup.current.pivot is in "blocks" (16 = 1 meter for IK).
         * Convert to meters so FABRIK works in a reasonable numeric scale.
         */
        for (int i = 0; i < n; i++)
        {
            ModelGroup g = this.cubicChain.get(i);
            this.fkPositions[i].set(
                g.current.pivot.x / 16F,
                g.current.pivot.y / 16F,
                g.current.pivot.z / 16F
            );
        }

        /* Compute segment lengths from FK positions */
        for (int i = 0; i < n - 1; i++)
        {
            this.lengths[i] = this.fkPositions[i].distance(this.fkPositions[i + 1]);

            if (this.lengths[i] < 1e-6F)
            {
                this.lengths[i] = 0.0625F; /* 1 pixel = minimum non-zero length */
            }
        }

        /* Copy FK positions into IK buffer */
        for (int i = 0; i < n; i++)
        {
            this.ikPositions[i].set(this.fkPositions[i]);
        }

        /* Run FABRIK */
        IKSolver.solve(this.ikPositions, this.lengths, target,
            this.config.iterations.get(), this.config.tolerance.get());

        /*
         * Apply rotations back into each bone using its parent's frame of reference.
         *
         * For bone i (chain[i] → chain[i+1]):
         *   - FK direction: fkPositions[i+1] - fkPositions[i]  (world space)
         *   - IK direction: ikPositions[i+1] - ikPositions[i]  (world space)
         *   - The rotation that maps FK→IK direction is applied as a local rotation delta.
         *
         * To convert the world-space rotation axis into parent-local space we need the
         * inverse of the parent's world rotation. Since we don't have full matrix data
         * here (MatrixCache is on the client side), we approximate the parent transform
         * by noting that the current.pivot chain gives us the parent hierarchy implicitly.
         */
        for (int i = 0; i < n - 1; i++)
        {
            ModelGroup bone = this.cubicChain.get(i);

            Vector3f fkDir = new Vector3f(this.fkPositions[i + 1]).sub(this.fkPositions[i]);
            Vector3f ikDir = new Vector3f(this.ikPositions[i + 1]).sub(this.ikPositions[i]);

            float fkLen = fkDir.length();
            float ikLen = ikDir.length();

            if (fkLen < 1e-6F || ikLen < 1e-6F)
            {
                continue;
            }

            fkDir.div(fkLen);
            ikDir.div(ikLen);

            float dot = fkDir.dot(ikDir);
            dot = Math.max(-1F, Math.min(1F, dot));

            if (dot >= 1F - 1e-6F)
            {
                continue; /* Already aligned */
            }

            /* Rotation axis in world space */
            Vector3f axis = new Vector3f();
            fkDir.cross(ikDir, axis);

            if (axis.length() < 1e-8F)
            {
                /* Anti-parallel: pick arbitrary perpendicular axis */
                axis.set(Math.abs(fkDir.y) < 0.9F ? 0F : 1F,
                         Math.abs(fkDir.y) < 0.9F ? 1F : 0F,
                         0F);
                fkDir.cross(axis, axis);
            }

            axis.normalize();

            float angle = (float) Math.acos(dot);

            /*
             * Convert world-space rotation axis into this bone's local parent space.
             * The bone's parent chain pivot sequence gives us the parent's effective
             * world rotation. We build an approximate inverse parent rotation by
             * using the parent's FK direction as a reference.
             */
            if (bone.parent != null)
            {
                /* Approximate parent world rotation from FK skeleton */
                int parentIdx = i - 1;
                if (parentIdx >= 0)
                {
                    Vector3f parentFkDir = new Vector3f(this.fkPositions[i]).sub(this.fkPositions[parentIdx]).normalize();
                    /* Transform axis from world to parent-local using parent direction */
                    axis = worldToParentLocal(axis, parentFkDir);
                }
                /* else: root bone → world space IS parent space */
            }

            /* Convert axis-angle to Euler XYZ (radians) and apply with weight */
            float[] euler = axisAngleToEulerXYZ(axis, angle);

            bone.current.rotate.x += (float) Math.toDegrees(euler[0]) * weight;
            bone.current.rotate.y += (float) Math.toDegrees(euler[1]) * weight;
            bone.current.rotate.z += (float) Math.toDegrees(euler[2]) * weight;
        }
    }

    /* -----------------------------------------------------------------------
     * BOBJ (BOBJBone) solver
     * --------------------------------------------------------------------- */

    private void solveBobj(Vector3f target, float weight)
    {
        int n = this.bobjChain.size();

        if (n < 2 || this.fkPositions.length < n)
        {
            return;
        }

        for (int i = 0; i < n; i++)
        {
            BOBJBone b = this.bobjChain.get(i);
            this.fkPositions[i].set(b.transform.translate);
        }

        for (int i = 0; i < n - 1; i++)
        {
            this.lengths[i] = this.fkPositions[i].distance(this.fkPositions[i + 1]);

            if (this.lengths[i] < 1e-6F)
            {
                this.lengths[i] = 0.0625F;
            }
        }

        for (int i = 0; i < n; i++)
        {
            this.ikPositions[i].set(this.fkPositions[i]);
        }

        IKSolver.solve(this.ikPositions, this.lengths, target,
            this.config.iterations.get(), this.config.tolerance.get());

        for (int i = 0; i < n - 1; i++)
        {
            BOBJBone bone = this.bobjChain.get(i);

            Vector3f fkDir = new Vector3f(this.fkPositions[i + 1]).sub(this.fkPositions[i]);
            Vector3f ikDir = new Vector3f(this.ikPositions[i + 1]).sub(this.ikPositions[i]);

            float fkLen = fkDir.length();
            float ikLen = ikDir.length();

            if (fkLen < 1e-6F || ikLen < 1e-6F)
            {
                continue;
            }

            fkDir.div(fkLen);
            ikDir.div(ikLen);

            float dot = fkDir.dot(ikDir);
            dot = Math.max(-1F, Math.min(1F, dot));

            if (dot >= 1F - 1e-6F)
            {
                continue;
            }

            Vector3f axis = new Vector3f();
            fkDir.cross(ikDir, axis);

            if (axis.length() < 1e-8F)
            {
                axis.set(Math.abs(fkDir.y) < 0.9F ? 0F : 1F,
                         Math.abs(fkDir.y) < 0.9F ? 1F : 0F,
                         0F);
                fkDir.cross(axis, axis);
            }

            axis.normalize();

            float angle = (float) Math.acos(dot);
            float[] euler = axisAngleToEulerXYZ(axis, angle);

            bone.transform.rotate.x += euler[0] * weight;
            bone.transform.rotate.y += euler[1] * weight;
            bone.transform.rotate.z += euler[2] * weight;
        }
    }

    /* -----------------------------------------------------------------------
     * Math helpers
     * --------------------------------------------------------------------- */

    /**
     * Approximates the conversion of a world-space axis into a bone's parent-local
     * space given the parent bone's FK direction vector.
     *
     * This uses a minimal rotation frame (MRF) aligned with the parent direction,
     * similar to how Blockbench's Chain3D maps hinge rotation axes into parent frame.
     */
    private static Vector3f worldToParentLocal(Vector3f worldAxis, Vector3f parentFkDir)
    {
        /* Build a rotation matrix whose Z-axis aligns with parentFkDir */
        Vector3f z = new Vector3f(parentFkDir).normalize();
        Vector3f x = new Vector3f();

        if (Math.abs(z.y) < 0.99F)
        {
            x.set(0F, 1F, 0F).cross(z).normalize();
        }
        else
        {
            x.set(1F, 0F, 0F).cross(z).normalize();
        }

        Vector3f y = new Vector3f(z).cross(x).normalize();

        /* Build 3x3 rotation matrix (columns are x, y, z basis vectors) */
        Matrix3f m = new Matrix3f(
            x.x, y.x, z.x,
            x.y, y.y, z.y,
            x.z, y.z, z.z
        );

        /* Transform world axis into parent local (multiply by transpose = inverse for orthonormal) */
        Vector3f local = new Vector3f();
        m.transpose().transform(worldAxis, local);

        return local.normalize();
    }

    /**
     * Converts an axis-angle rotation to XYZ intrinsic Euler angles (radians).
     */
    private static float[] axisAngleToEulerXYZ(Vector3f axis, float angle)
    {
        Quaternionf q = new Quaternionf().fromAxisAngleRad(axis.x, axis.y, axis.z, angle);

        float[] euler = new float[3];

        /* XYZ intrinsic decomposition */
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

    /**
     * Looks up a ModelGroup by name from the model.
     */
    private static ModelGroup findGroup(IModel model, String name)
    {
        for (ModelGroup g : model.getAllGroups())
        {
            if (g.id.equals(name))
            {
                return g;
            }
        }

        return null;
    }

    /**
     * Looks up a BOBJBone by name from the model.
     */
    private static BOBJBone findBobjBone(IModel model, String name)
    {
        for (BOBJBone b : model.getAllBOBJBones())
        {
            if (b.name.equals(name))
            {
                return b;
            }
        }

        return null;
    }

    /**
     * Invalidates the cached chain so it is rebuilt on the next {@link #solve} call.
     * Call this when the model changes or bone names are edited.
     */
    public void invalidate()
    {
        this.cachedTip  = null;
        this.cachedRoot = null;
        this.cubicChain.clear();
        this.bobjChain.clear();
    }
}
