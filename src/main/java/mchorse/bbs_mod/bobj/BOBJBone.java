package mchorse.bbs_mod.bobj;

import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.joml.QuaternionMath;
import mchorse.bbs_mod.utils.pose.Transform;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class BOBJBone
{
    /* Meta information */
    public int index;
    public String name;
    public String parent;
    public BOBJBone parentBone;

    /* Transformations */
    public final Transform transform = new Transform();

    public float lighting;
    public final Color color = new Color(1, 1, 1, 1);
    public Link texture;
    public float textureBlend = 1F;

    /**
     * Computed bone matrix which is used for transformations. This 
     * matrix isn't multiplied by inverse bone matrix. 
     */
    public Matrix4f mat = new Matrix4f();

    public Matrix4f originMat = new Matrix4f();

    /**
     * Bone matrix 
     */
    public Matrix4f boneMat;

    /**
     * Inverse bone matrix 
     */
    public Matrix4f invBoneMat = new Matrix4f();

    /**
     * Relative-to-parent bone matrix
     */
    public Matrix4f relBoneMat = new Matrix4f();

    /**
     * Transient full local orientation from IK, applied in place of euler rotate.
     * Null when not IK-driven this frame.
     */
    public Quaternionf orient;

    /**
     * Transient cumulative world translation for IK stretch on the skinning matrix.
     * Null when unused this frame.
     */
    public Vector3f offset;

    public BOBJBone(int index, String name, String parent, Matrix4f boneMat)
    {
        this.index = index;
        this.name = name;
        this.parent = parent;
        this.boneMat = boneMat;

        this.invBoneMat.set(boneMat);
        this.invBoneMat.invert();

        this.relBoneMat.identity();
    }

    public Matrix4f compute()
    {
        Matrix4f mat = this.computeMatrix(new Matrix4f());

        this.mat.set(mat);
        mat.mul(this.invBoneMat);

        /* Stretch shifts only the skinning matrix — skeleton frames stay nominal. */
        if (this.offset != null)
        {
            mat.translateLocal(this.offset);
        }

        return mat;
    }

    public Matrix4f computeMatrix(Matrix4f m)
    {
        this.mat.set(this.relBoneMat);
        this.originMat.set(this.relBoneMat);
        this.applyTransformations();

        if (this.parentBone != null)
        {
            m.set(this.parentBone.mat).mul(this.originMat);
            this.originMat.set(m);
            m.identity().set(this.parentBone.mat);
        }

        m.mul(this.mat);

        return m;
    }

    public void applyTransformations()
    {
        this.mat.translate(this.transform.translate);
        this.originMat.translate(this.transform.translate);

        /* Keep gizmo / rotation center at translate + pivot (same as Transform.setupMatrix). */
        if (this.transform.pivot.x != 0F || this.transform.pivot.y != 0F || this.transform.pivot.z != 0F)
        {
            this.mat.translate(this.transform.pivot);
            this.originMat.translate(this.transform.pivot);
        }

        if (this.orient != null)
        {
            /* orient already folds rotate2, so the euler triples are skipped. */
            this.mat.rotate(this.orient);
        }
        else
        {
            if (this.transform.rotate.z != 0F) this.mat.rotateZ(this.transform.rotate.z);
            if (this.transform.rotate.y != 0F) this.mat.rotateY(this.transform.rotate.y);
            if (this.transform.rotate.x != 0F) this.mat.rotateX(this.transform.rotate.x);

            if (this.transform.rotate2.z != 0F) this.mat.rotateZ(this.transform.rotate2.z);
            if (this.transform.rotate2.y != 0F) this.mat.rotateY(this.transform.rotate2.y);
            if (this.transform.rotate2.x != 0F) this.mat.rotateX(this.transform.rotate2.x);
        }

        this.mat.scale(this.transform.scale);

        if (this.transform.pivot.x != 0F || this.transform.pivot.y != 0F || this.transform.pivot.z != 0F)
        {
            this.mat.translate(-this.transform.pivot.x, -this.transform.pivot.y, -this.transform.pivot.z);
        }
    }

    /**
     * Composes one rotation layer into {@link #orient} (BOBJ rotations are radians).
     */
    public void composeOrient(Quaternionf delta)
    {
        if (this.orient == null)
        {
            this.orient = QuaternionMath.composeFromEulerZYXRadians(this.transform.rotate.x, this.transform.rotate.y, this.transform.rotate.z);

            if (this.transform.rotate2.x != 0F || this.transform.rotate2.y != 0F || this.transform.rotate2.z != 0F)
            {
                this.orient.mul(QuaternionMath.composeFromEulerZYXRadians(this.transform.rotate2.x, this.transform.rotate2.y, this.transform.rotate2.z));
            }
        }
        else
        {
            this.orient.mul(delta);
        }
    }

    public BOBJBone copy()
    {
        BOBJBone bone = new BOBJBone(this.index, this.name, this.parent, new Matrix4f(this.boneMat));

        bone.transform.copy(this.transform);
        bone.lighting = this.lighting;
        bone.color.copy(this.color);
        bone.texture = this.texture;
        bone.textureBlend = this.textureBlend;
        bone.mat.set(this.mat);
        bone.originMat.set(this.originMat);
        bone.invBoneMat.set(this.invBoneMat);
        bone.relBoneMat.set(this.relBoneMat);

        return bone;
    }

    public void reset()
    {
        this.transform.identity();
        this.orient = null;
        this.offset = null;
    }
}
