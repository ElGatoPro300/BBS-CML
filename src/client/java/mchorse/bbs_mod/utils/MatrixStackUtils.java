package mchorse.bbs_mod.utils;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.controller.CameraController;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.utils.pose.Transform;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

public class MatrixStackUtils
{
    private static Matrix3f normal = new Matrix3f();

    private static Matrix4f oldProjection = new Matrix4f();
    private static Matrix4f oldMV = new Matrix4f();
    private static Matrix3f oldInverse = new Matrix3f();
    private static final Quaternionf tempQuaternion = new Quaternionf();
    /* Near-zero axis scale collapses ModelView; Iris then rebuilds normals from a singular
     * inverse-transpose and lit meshes go solid black. Keep a tiny thickness for lighting. */
    private static final float MIN_SCALE = 1.0E-4F;

    /**
     * Reciprocal safe for normal-matrix correction when an axis scale is 0 (flat bones/billboards).
     */
    public static float safeNormalScaleReciprocal(float scale)
    {
        return Math.abs(scale) < MIN_SCALE ? 1F : 1F / scale;
    }

    /**
     * Non-zero scale for position matrices so Iris/vanilla inverse-transpose normals stay valid.
     */
    public static float safePositionScale(float scale)
    {
        if (Math.abs(scale) >= MIN_SCALE)
        {
            return scale;
        }

        return scale < 0F ? -MIN_SCALE : MIN_SCALE;
    }

    /**
     * 1.20.4 exposed this on {@link RenderSystem}; 1.21.1 removed it. Rebuild the
     * camera's inverse view-rotation matrix from the active game camera quaternion.
     * When a BBS camera controller is active, also undo film/orbit roll (applied in
     * {@code GameRendererMixin.tiltViewWhenHurt} but missing from {@code Camera}).
     */
    public static Matrix4f getInverseViewRotationMatrix()
    {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Matrix4f inverse = new Matrix4f().rotation(camera.rotation().conjugate(MatrixStackUtils.tempQuaternion));
        CameraController controller = BBSModClient.getCameraController();

        if (controller.getCurrent() != null)
        {
            float rollDeg = controller.getRoll();

            if (Math.abs(rollDeg) > 1.0E-4F)
            {
                /* View = Rz(roll) * ViewRot → Inv = ViewRot^-1 * Rz(-roll). */
                inverse.rotateZ(-MathUtils.toRad(rollDeg));
            }
        }

        return inverse;
    }

    /**
     * View rotation matrix paired with {@link #getInverseViewRotationMatrix()}.
     */
    public static Matrix4f getViewRotationMatrix()
    {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();

        return new Matrix4f().rotation(camera.rotation());
    }

    public static void scaleStack(PoseStack stack, float x, float y, float z)
    {
        stack.last().pose().scale(safePositionScale(x), safePositionScale(y), safePositionScale(z));
        stack.last().normal().scale(x < 0F ? -1F : 1F, y < 0F ? -1F : 1F, z < 0F ? -1F : 1F);
    }

    /**
     * UI previews flip Y lighting; divide out current normal scale without Inf on flat axes.
     */
    public static void invertUiNormalY(PoseStack stack)
    {
        stack.last().normal().getScale(Vectors.EMPTY_3F);
        stack.last().normal().scale(
            safeNormalScaleReciprocal(Vectors.EMPTY_3F.x),
            -safeNormalScaleReciprocal(Vectors.EMPTY_3F.y),
            safeNormalScaleReciprocal(Vectors.EMPTY_3F.z)
        );
    }

    public static void cacheMatrices()
    {
        /* Cache the global stuff */
        RenderSystem.backupProjectionMatrix();
        oldMV.set(RenderSystem.getModelViewMatrix());
        oldInverse.set(new Matrix3f(RenderSystem.getModelViewMatrix()));

        Matrix4fStack mvStack = RenderSystem.getModelViewStack();
        mvStack.identity();
    }

    public static void restoreMatrices()
    {
        /* Return back to orthographic projection */
        RenderSystem.restoreProjectionMatrix();

        Matrix4fStack mvStack = RenderSystem.getModelViewStack();
        mvStack.set(oldMV);
    }

    public static void applyModelViewMatrix()
    {
        /* 1.21.11: RenderPipeline handles ModelViewMat */
    }

    public static void pushIdentityModelView()
    {
        Matrix4fStack mvStack = RenderSystem.getModelViewStack();

        mvStack.pushMatrix();
        mvStack.identity();
    }

    public static void popModelView()
    {
        Matrix4fStack mvStack = RenderSystem.getModelViewStack();

        mvStack.popMatrix();
    }

    public static void applyTransform(PoseStack stack, Transform transform)
    {
        stack.translate(transform.translate.x, transform.translate.y, transform.translate.z);

        if (transform.pivot.x != 0F || transform.pivot.y != 0F || transform.pivot.z != 0F)
        {
            stack.translate(transform.pivot.x, transform.pivot.y, transform.pivot.z);
        }

        stack.mulPose(Axis.ZP.rotation(transform.rotate.z));
        stack.mulPose(Axis.YP.rotation(transform.rotate.y));
        stack.mulPose(Axis.XP.rotation(transform.rotate.x));
        stack.mulPose(Axis.ZP.rotation(transform.rotate2.z));
        stack.mulPose(Axis.YP.rotation(transform.rotate2.y));
        stack.mulPose(Axis.XP.rotation(transform.rotate2.x));
        scaleStack(stack, transform.scale.x, transform.scale.y, transform.scale.z);

        if (transform.pivot.x != 0F || transform.pivot.y != 0F || transform.pivot.z != 0F)
        {
            stack.translate(-transform.pivot.x, -transform.pivot.y, -transform.pivot.z);
        }
    }

    public static void multiply(PoseStack stack, Matrix4f matrix)
    {
        normal.set(matrix);
        normal.getScale(Vectors.TEMP_3F);

        Vectors.TEMP_3F.x = safeNormalScaleReciprocal(Vectors.TEMP_3F.x);
        Vectors.TEMP_3F.y = safeNormalScaleReciprocal(Vectors.TEMP_3F.y);
        Vectors.TEMP_3F.z = safeNormalScaleReciprocal(Vectors.TEMP_3F.z);

        normal.scale(Vectors.TEMP_3F);

        stack.last().pose().mul(matrix);
        stack.last().normal().mul(normal);
    }

    public static void scaleBack(PoseStack matrices)
    {
        Matrix4f position = matrices.last().pose();

        float scaleX = (float) Math.sqrt(position.m00() * position.m00() + position.m10() * position.m10() + position.m20() * position.m20());
        float scaleY = (float) Math.sqrt(position.m01() * position.m01() + position.m11() * position.m11() + position.m21() * position.m21());
        float scaleZ = (float) Math.sqrt(position.m02() * position.m02() + position.m12() * position.m12() + position.m22() * position.m22());

        float max = Math.max(scaleX, Math.max(scaleY, scaleZ));

        position.m00(position.m00() / max);
        position.m10(position.m10() / max);
        position.m20(position.m20() / max);

        position.m01(position.m01() / max);
        position.m11(position.m11() / max);
        position.m21(position.m21() / max);

        position.m02(position.m02() / max);
        position.m12(position.m12() / max);
        position.m22(position.m22() / max);
    }

    public static Matrix4f stripScale(Matrix4f matrix)
    {
        Matrix4f m = new Matrix4f(matrix);

        float sx = (float) Math.sqrt(m.m00() * m.m00() + m.m01() * m.m01() + m.m02() * m.m02());
        float sy = (float) Math.sqrt(m.m10() * m.m10() + m.m11() * m.m11() + m.m12() * m.m12());
        float sz = (float) Math.sqrt(m.m20() * m.m20() + m.m21() * m.m21() + m.m22() * m.m22());

        if (sx != 0F)
        {
            m.m00(m.m00() / sx);
            m.m01(m.m01() / sx);
            m.m02(m.m02() / sx);
        }

        if (sy != 0F)
        {
            m.m10(m.m10() / sy);
            m.m11(m.m11() / sy);
            m.m12(m.m12() / sy);
        }

        if (sz != 0F)
        {
            m.m20(m.m20() / sz);
            m.m21(m.m21() / sz);
            m.m22(m.m22() / sz);
        }

        return m;
    }
}
