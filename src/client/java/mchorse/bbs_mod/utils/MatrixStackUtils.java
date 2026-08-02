package mchorse.bbs_mod.utils;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.controller.CameraController;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.utils.pose.Transform;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

public class MatrixStackUtils
{
    private static Matrix3f normal = new Matrix3f();
    private static Matrix3f billboardView = new Matrix3f();

    private static Matrix3f oldInverse = new Matrix3f();
    private static final Quaternionf tempQuaternion = new Quaternionf();

    /**
     * 1.20.4 exposed this on {@link RenderSystem}; 1.21.1 removed it. Rebuild the
     * camera's inverse view-rotation matrix from the active game camera quaternion.
     * When a BBS camera controller is active, also undo film/orbit roll (applied in
     * {@code GameRendererMixin.tiltViewWhenHurt} but missing from {@code Camera}).
     */
    public static Matrix4f getInverseViewRotationMatrix()
    {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        Matrix4f inverse = new Matrix4f().rotation(camera.getRotation().conjugate(MatrixStackUtils.tempQuaternion));
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
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();

        return new Matrix4f().rotation(camera.getRotation());
    }

    public static void scaleStack(MatrixStack stack, float x, float y, float z)
    {
        stack.peek().getPositionMatrix().scale(x, y, z);
        stack.peek().getNormalMatrix().scale(x < 0F ? -1F : 1F, y < 0F ? -1F : 1F, z < 0F ? -1F : 1F);
    }

    /**
     * Orient the matrix stack's top so that geometry drawn on the local XY plane always faces the
     * camera (billboarding). The form's own scale and translation are preserved; its rotation is
     * intentionally discarded — that's the point of a billboard.
     */
    public static void billboard(MatrixStack stack)
    {
        Matrix4f position = stack.peek().getPositionMatrix();
        Vector3f scale = Vectors.TEMP_3F;

        position.getScale(scale);

        RenderSystem.getModelViewMatrix().get3x3(billboardView);
        billboardView.invert();

        position.m00(billboardView.m00()).m01(billboardView.m01()).m02(billboardView.m02());
        position.m10(billboardView.m10()).m11(billboardView.m11()).m12(billboardView.m12());
        position.m20(billboardView.m20()).m21(billboardView.m21()).m22(billboardView.m22());

        position.scale(scale);

        stack.peek().getNormalMatrix().identity();
    }

    public static void cacheMatrices()
    {
        /* Cache the global stuff */
        oldInverse.set(InverseView.get());

        RenderSystem.backupProjectionMatrix();

        Matrix4fStack renderStack = RenderSystem.getModelViewStack();

        renderStack.pushMatrix();
        renderStack.identity();
    }

    public static void restoreMatrices()
    {
        /* Return back to orthographic projection */
        RenderSystem.restoreProjectionMatrix();
        InverseView.set(oldInverse);

        Matrix4fStack renderStack = RenderSystem.getModelViewStack();

        renderStack.popMatrix();
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

    public static void applyTransform(MatrixStack stack, Transform transform)
    {
        stack.translate(transform.translate.x, transform.translate.y, transform.translate.z);

        if (transform.pivot.x != 0F || transform.pivot.y != 0F || transform.pivot.z != 0F)
        {
            stack.translate(transform.pivot.x, transform.pivot.y, transform.pivot.z);
        }

        stack.multiply(RotationAxis.POSITIVE_Z.rotation(transform.rotate.z));
        stack.multiply(RotationAxis.POSITIVE_Y.rotation(transform.rotate.y));
        stack.multiply(RotationAxis.POSITIVE_X.rotation(transform.rotate.x));
        stack.multiply(RotationAxis.POSITIVE_Z.rotation(transform.rotate2.z));
        stack.multiply(RotationAxis.POSITIVE_Y.rotation(transform.rotate2.y));
        stack.multiply(RotationAxis.POSITIVE_X.rotation(transform.rotate2.x));
        scaleStack(stack, transform.scale.x, transform.scale.y, transform.scale.z);

        if (transform.pivot.x != 0F || transform.pivot.y != 0F || transform.pivot.z != 0F)
        {
            stack.translate(-transform.pivot.x, -transform.pivot.y, -transform.pivot.z);
        }
    }

    public static void multiply(MatrixStack stack, Matrix4f matrix)
    {
        normal.set(matrix);
        normal.getScale(Vectors.TEMP_3F);

        Vectors.TEMP_3F.x = Vectors.TEMP_3F.x == 0F ? 0F : 1F / Vectors.TEMP_3F.x;
        Vectors.TEMP_3F.y = Vectors.TEMP_3F.y == 0F ? 0F : 1F / Vectors.TEMP_3F.y;
        Vectors.TEMP_3F.z = Vectors.TEMP_3F.z == 0F ? 0F : 1F / Vectors.TEMP_3F.z;

        normal.scale(Vectors.TEMP_3F);

        stack.peek().getPositionMatrix().mul(matrix);
        stack.peek().getNormalMatrix().mul(normal);
    }

    /**
     * Inverts the Y component of the normal matrix so UI rendering has correct lighting
     * (vanilla GUI coordinate system has Y pointing down).
     */
    public static void invertUiNormalY(MatrixStack stack)
    {
        stack.peek().getNormalMatrix().mul(new Matrix3f().scale(1F, -1F, 1F));
    }

    public static void scaleBack(MatrixStack matrices)
    {
        Matrix4f position = matrices.peek().getPositionMatrix();

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
