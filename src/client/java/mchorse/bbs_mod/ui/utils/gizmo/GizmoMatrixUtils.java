package mchorse.bbs_mod.ui.utils.gizmo;

import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCacheEntry;
import mchorse.bbs_mod.ui.model_blocks.UIModelBlockPanel;
import mchorse.bbs_mod.utils.pose.Transform;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Shared gizmo-basis helpers used to keep drag math aligned with
 * {@link UIModelBlockPanel}'s world-space gizmo frame.
 */
public final class GizmoMatrixUtils
{
    private GizmoMatrixUtils()
    {}

    /**
     * Bone gizmo basis for film pose keyframes ({@link BaseFilmController#renderAxes}).
     * Non-local mode uses the bone pivot matrix; local mode keeps the posed rotation with pivot translation
     * for cubic models. BOBJ translates before rotating in the bone frame, so local mode must use the
     * origin (pre-pose-rotation) basis — otherwise the visible arrows diverge from the direction
     * {@code PoseTransform.translate} actually moves the bone.
     */
    public static Matrix4f resolveFilmPoseBoneMatrix(MatrixCacheEntry entry, boolean local)
    {
        return resolveFilmPoseBoneMatrix(entry, local ? TransformOrientation.LOCAL : TransformOrientation.PARENT, false);
    }

    public static Matrix4f resolveFilmPoseBoneMatrix(MatrixCacheEntry entry, boolean local, boolean bobj)
    {
        return resolveFilmPoseBoneMatrix(entry, local ? TransformOrientation.LOCAL : TransformOrientation.PARENT, bobj);
    }

    public static Matrix4f resolveFilmPoseBoneMatrix(MatrixCacheEntry entry, TransformOrientation orientation, boolean bobj)
    {
        if (entry == null)
        {
            return null;
        }

        if (orientation == null)
        {
            orientation = TransformOrientation.PARENT;
        }

        Matrix4f matrix;
        boolean local = orientation.usesLocalBoneBasis();

        if (!local)
        {
            matrix = entry.origin();

            if (matrix == null)
            {
                matrix = entry.matrix();
            }

            return applyOrientationSpace(matrix == null ? null : new Matrix4f(matrix), orientation);
        }

        Matrix4f localMatrix = entry.matrix();
        Matrix4f originMatrix = entry.origin();

        /* BOBJ: translate is applied before rotate in BOBJBone.applyTransformations, so the
         * local gizmo basis must match originMat (parent × relBone × T), not orderedBone.mat
         * which already includes pose R/S. */
        if (bobj)
        {
            if (originMatrix != null)
            {
                return applyOrientationSpace(new Matrix4f(originMatrix), orientation);
            }

            return applyOrientationSpace(localMatrix == null ? null : new Matrix4f(localMatrix), orientation);
        }

        if (localMatrix != null && originMatrix != null)
        {
            matrix = new Matrix4f(localMatrix);

            matrix.setTranslation(originMatrix.getTranslation(new Vector3f()));

            return applyOrientationSpace(matrix, orientation);
        }

        if (localMatrix != null)
        {
            return applyOrientationSpace(new Matrix4f(localMatrix), orientation);
        }

        if (originMatrix != null)
        {
            return applyOrientationSpace(new Matrix4f(originMatrix), orientation);
        }

        return null;
    }

    public static Matrix4f applyOrientationSpace(Matrix4f matrix, TransformOrientation orientation)
    {
        if (matrix == null)
        {
            return null;
        }

        if (orientation == TransformOrientation.GLOBAL || orientation == TransformOrientation.VIEW)
        {
            Vector3f translation = matrix.getTranslation(new Vector3f());

            matrix.identity();
            matrix.translate(translation);
        }

        return matrix;
    }

    public static Matrix4f applyViewCaptureAlignment(Matrix4f captureMatrix, TransformOrientation orientation)
    {
        if (captureMatrix != null && orientation == TransformOrientation.VIEW)
        {
            Vector3f translation = captureMatrix.getTranslation(new Vector3f());

            captureMatrix.identity();
            captureMatrix.translate(translation);
        }

        return captureMatrix;
    }

    /**
     * Bakes the edited transform's rotation into a position-only gizmo matrix when local mode
     * is active, matching the model block gizmo's {@code gizmoWorldMatrix} composition.
     */
    public static Matrix4f withLocalRotation(Matrix4f matrix, Transform transform, boolean local)
    {
        return withLocalRotation(matrix, transform, local ? TransformOrientation.LOCAL : TransformOrientation.PARENT);
    }

    public static Matrix4f withLocalRotation(Matrix4f matrix, Transform transform, TransformOrientation orientation)
    {
        if (matrix == null || orientation == null || !orientation.isLocal() || transform == null)
        {
            return matrix;
        }

        Matrix4f result = new Matrix4f(matrix);

        result.mul(new Matrix4f(transform.createRotationMatrix()));

        return result;
    }

    /** Converts a camera-relative gizmo matrix from the world render pass into world space. */
    public static Matrix4f cameraRelativeToWorld(Matrix4f cameraView, Matrix4f cameraRelative)
    {
        return new Matrix4f(cameraView).invert().mul(cameraRelative);
    }

    /**
     * Strips scale/skew/mirroring out of a gizmo origin matrix, leaving only position and a
     * right-handed unit-length rotation basis. Matrices carrying parent hierarchy scale
     * distort the gizmo into stretched ellipses/rectangles and skew the drag math.
     */
    public static Matrix4f normalizeBasis(Matrix4f matrix)
    {
        if (matrix == null)
        {
            return null;
        }

        Vector3f x = new Vector3f();
        Vector3f y = new Vector3f();
        Vector3f z = new Vector3f();

        matrix.getColumn(0, x);
        matrix.getColumn(1, y);
        matrix.getColumn(2, z);

        if (x.lengthSquared() < 1.0E-12F || y.lengthSquared() < 1.0E-12F)
        {
            return matrix;
        }

        x.normalize();
        y.normalize();

        /* Rebuild Z (and re-square Y) from a cross product so the basis is orthogonal and
         * always right-handed, even if the source matrix was mirrored or non-uniformly scaled. */
        z.set(x).cross(y).normalize();
        y.set(z).cross(x).normalize();

        Matrix4f result = new Matrix4f(matrix);

        result.setColumn(0, new Vector4f(x, 0F));
        result.setColumn(1, new Vector4f(y, 0F));
        result.setColumn(2, new Vector4f(z, 0F));

        return result;
    }
}
