package mchorse.bbs_mod.graphics;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.camera.data.Angle;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.utils.Axis;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import net.minecraft.class_286;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_293;
import net.minecraft.class_4587;
import net.minecraft.class_757;
import net.minecraft.class_7833;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

public class Draw
{
    /* Render-thread scratch for torus/sphere/cone (avoids per-frame float[] allocs). */
    private static final float[] SCRATCH_COS_U = new float[65];
    private static final float[] SCRATCH_SIN_U = new float[65];
    private static final float[] SCRATCH_COS_V = new float[25];
    private static final float[] SCRATCH_SIN_V = new float[25];

    public static void renderBox(class_4587 stack, double x, double y, double z, double w, double h, double d)
    {
        renderBox(stack, x, y, z, w, h, d, 1, 1, 1);
    }

    public static void renderBox(class_4587 stack, double x, double y, double z, double w, double h, double d, float r, float g, float b)
    {
        renderBox(stack, x, y, z, w, h, d, r, g, b, 1F);
    }

    public static void renderBox(class_4587 stack, double x, double y, double z, double w, double h, double d, float r, float g, float b, float a)
    {
        /* Iris TAA turns lines/alpha into stipple during the world pass. Queue solid edges for LAST. */
        if (BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld())
        {
            enqueueIrisBox(stack, x, y, z, w, h, d, r, g, b);

            return;
        }

        stack.method_22903();
        stack.method_22904(x, y, z);
        float fw = (float) w;
        float fh = (float) h;
        float fd = (float) d;
        float t = 1 / 96F + (float) (Math.sqrt(w * w + h + h + d + d) / 2000);

        class_287 builder = class_289.method_1348().method_60827(class_293.class_5596.field_27379, class_290.field_1576);
        RenderSystem.setShader(class_757::method_34540);

        /* Pillars: fillBox(builder, -t, -t, -t, t, t, t, r, g, b, a); */
        fillBox(builder, stack, -t, -t, -t, t, t + fh, t, r, g, b, a);
        fillBox(builder, stack, -t + fw, -t, -t, t + fw, t + fh, t, r, g, b, a);
        fillBox(builder, stack, -t, -t, -t + fd, t, t + fh, t + fd, r, g, b, a);
        fillBox(builder, stack, -t + fw, -t, -t + fd, t + fw, t + fh, t + fd, r, g, b, a);

        /* Top */
        fillBox(builder, stack, -t, -t + fh, -t, t + fw, t + fh, t, r, g, b, a);
        fillBox(builder, stack, -t, -t + fh, -t + fd, t + fw, t + fh, t + fd, r, g, b, a);
        fillBox(builder, stack, -t, -t + fh, -t, t, t + fh, t + fd, r, g, b, a);
        fillBox(builder, stack, -t + fw, -t + fh, -t, t + fw, t + fh, t + fd, r, g, b, a);

        /* Bottom */
        fillBox(builder, stack, -t, -t, -t, t + fw, t, t, r, g, b, a);
        fillBox(builder, stack, -t, -t, -t + fd, t + fw, t, t + fd, r, g, b, a);
        fillBox(builder, stack, -t, -t, -t, t, t, t + fd, r, g, b, a);
        fillBox(builder, stack, -t + fw, -t, -t, t + fw, t, t + fd, r, g, b, a);

        class_286.method_43433(builder.method_60800());

        stack.method_22909();
    }


    private static final java.util.List<IrisBox> irisBoxQueue = new java.util.ArrayList<>();

    private static final class IrisBox
    {
        private final Matrix4f matrix;
        private final float w;
        private final float h;
        private final float d;
        private final float r;
        private final float g;
        private final float b;

        private IrisBox(Matrix4f matrix, float w, float h, float d, float r, float g, float b)
        {
            this.matrix = matrix;
            this.w = w;
            this.h = h;
            this.d = d;
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    private static void enqueueIrisBox(class_4587 stack, double x, double y, double z, double w, double h, double d, float r, float g, float b)
    {
        Matrix4f matrix = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(stack.method_23760().method_23761());

        matrix.translate((float) x, (float) y, (float) z);
        irisBoxQueue.add(new IrisBox(matrix, (float) w, (float) h, (float) d, r, g, b));
    }

    /** Flush hitboxes queued during the Iris world pass (call from WorldRenderEvents.LAST). */
    public static void flushIrisBoxes()
    {
        if (irisBoxQueue.isEmpty())
        {
            return;
        }

        boolean savedBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean savedDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        class_4587 stack = new class_4587();

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(class_757::method_34540);
        MatrixStackUtils.pushIdentityModelView();

        try
        {
            for (IrisBox box : irisBoxQueue)
            {
                stack.method_22903();
                stack.method_23760().method_23761().set(box.matrix);
                renderBoxSolidEdges(stack, box.w, box.h, box.d, box.r, box.g, box.b);
                stack.method_22909();
            }
        }
        finally
        {
            MatrixStackUtils.popModelView();
            irisBoxQueue.clear();

            if (savedDepth)
            {
                RenderSystem.enableDepthTest();
            }

            if (savedBlend)
            {
                RenderSystem.enableBlend();
            }
        }
    }

    private static void renderBoxSolidEdges(class_4587 stack, float fw, float fh, float fd, float r, float g, float b)
    {
        float t = 1 / 96F + (float) (Math.sqrt(fw * fw + fh + fh + fd + fd) / 2000);
        class_287 builder = class_289.method_1348().method_60827(class_293.class_5596.field_27379, class_290.field_1576);

        fillBox(builder, stack, -t, -t, -t, t, t + fh, t, r, g, b, 1F);
        fillBox(builder, stack, -t + fw, -t, -t, t + fw, t + fh, t, r, g, b, 1F);
        fillBox(builder, stack, -t, -t, -t + fd, t, t + fh, t + fd, r, g, b, 1F);
        fillBox(builder, stack, -t + fw, -t, -t + fd, t + fw, t + fh, t + fd, r, g, b, 1F);

        fillBox(builder, stack, -t, -t + fh, -t, t + fw, t + fh, t, r, g, b, 1F);
        fillBox(builder, stack, -t, -t + fh, -t + fd, t + fw, t + fh, t + fd, r, g, b, 1F);
        fillBox(builder, stack, -t, -t + fh, -t, t, t + fh, t + fd, r, g, b, 1F);
        fillBox(builder, stack, -t + fw, -t + fh, -t, t + fw, t + fh, t + fd, r, g, b, 1F);

        fillBox(builder, stack, -t, -t, -t, t + fw, t, t, r, g, b, 1F);
        fillBox(builder, stack, -t, -t, -t + fd, t + fw, t, t + fd, r, g, b, 1F);
        fillBox(builder, stack, -t, -t, -t, t, t, t + fd, r, g, b, 1F);
        fillBox(builder, stack, -t + fw, -t, -t, t + fw, t, t + fd, r, g, b, 1F);

        class_286.method_43433(builder.method_60800());
    }

    private static void renderBoxWireframe(class_4587 stack, double x, double y, double z, double w, double h, double d, float r, float g, float b, float a)
    {
        stack.method_22903();
        stack.method_22904(x, y, z);

        Matrix4f matrix = stack.method_23760().method_23761();
        float x1 = 0F;
        float y1 = 0F;
        float z1 = 0F;
        float x2 = (float) w;
        float y2 = (float) h;
        float z2 = (float) d;
        boolean savedBlend = GL11.glIsEnabled(GL11.GL_BLEND);

        RenderSystem.disableBlend();
        RenderSystem.setShader(class_757::method_34540);
        RenderSystem.lineWidth(2F);

        class_287 builder = class_289.method_1348().method_60827(class_293.class_5596.field_29344, class_290.field_1576);

        wireLine(builder, matrix, x1, y1, z1, x2, y1, z1, r, g, b, a);
        wireLine(builder, matrix, x2, y1, z1, x2, y1, z2, r, g, b, a);
        wireLine(builder, matrix, x2, y1, z2, x1, y1, z2, r, g, b, a);
        wireLine(builder, matrix, x1, y1, z2, x1, y1, z1, r, g, b, a);

        wireLine(builder, matrix, x1, y2, z1, x2, y2, z1, r, g, b, a);
        wireLine(builder, matrix, x2, y2, z1, x2, y2, z2, r, g, b, a);
        wireLine(builder, matrix, x2, y2, z2, x1, y2, z2, r, g, b, a);
        wireLine(builder, matrix, x1, y2, z2, x1, y2, z1, r, g, b, a);

        wireLine(builder, matrix, x1, y1, z1, x1, y2, z1, r, g, b, a);
        wireLine(builder, matrix, x2, y1, z1, x2, y2, z1, r, g, b, a);
        wireLine(builder, matrix, x2, y1, z2, x2, y2, z2, r, g, b, a);
        wireLine(builder, matrix, x1, y1, z2, x1, y2, z2, r, g, b, a);

        class_286.method_43433(builder.method_60800());

        RenderSystem.lineWidth(1F);

        if (savedBlend)
        {
            RenderSystem.enableBlend();
        }

        stack.method_22909();
    }

    private static void wireLine(class_287 builder, Matrix4f matrix, float x0, float y0, float z0, float x1, float y1, float z1, float r, float g, float b, float a)
    {
        builder.method_22918(matrix, x0, y0, z0).method_22915(r, g, b, a);
        builder.method_22918(matrix, x1, y1, z1).method_22915(r, g, b, a);
    }
    /**
     * Fill a quad for {@link class_290#field_1577}. Points should
     * be supplied in this order:
     *
     *     3 -------> 4
     *     ^
     *     |
     *     |
     *     2 <------- 1
     *
     * I.e. bottom left, bottom right, top left, top right, where left is -X and right is +X,
     * in case of a quad on fixed on Z axis.
     */
    public static void fillTexturedNormalQuad(class_287 builder, class_4587 stack, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float u1, float v1, float u2, float v2, float r, float g, float b, float a, float nx, float ny, float nz)
    {
        Matrix4f matrix4f = stack.method_23760().method_23761();

        /* 1 - BL, 2 - BR, 3 - TR, 4 - TL */
        builder.method_22918(matrix4f, x2, y2, z2).method_22913(u1, v2).method_22915(r, g, b, a).method_22914(nx, ny, nz);
        builder.method_22918(matrix4f, x1, y1, z1).method_22913(u2, v2).method_22915(r, g, b, a).method_22914(nx, ny, nz);
        builder.method_22918(matrix4f, x4, y4, z4).method_22913(u2, v1).method_22915(r, g, b, a).method_22914(nx, ny, nz);

        builder.method_22918(matrix4f, x2, y2, z2).method_22913(u1, v2).method_22915(r, g, b, a).method_22914(nx, ny, nz);
        builder.method_22918(matrix4f, x4, y4, z4).method_22913(u2, v1).method_22915(r, g, b, a).method_22914(nx, ny, nz);
        builder.method_22918(matrix4f, x3, y3, z3).method_22913(u1, v1).method_22915(r, g, b, a).method_22914(nx, ny, nz);
    }

    public static void fillQuad(class_287 builder, class_4587 stack, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b, float a)
    {
        Matrix4f matrix4f = stack.method_23760().method_23761();

        /* 1 - BR, 2 - BL, 3 - TL, 4 - TR */
        builder.method_22918(matrix4f, x1, y1, z1).method_22915(r, g, b, a);
        builder.method_22918(matrix4f, x2, y2, z2).method_22915(r, g, b, a);
        builder.method_22918(matrix4f, x3, y3, z3).method_22915(r, g, b, a);
        builder.method_22918(matrix4f, x1, y1, z1).method_22915(r, g, b, a);
        builder.method_22918(matrix4f, x3, y3, z3).method_22915(r, g, b, a);
        builder.method_22918(matrix4f, x4, y4, z4).method_22915(r, g, b, a);
    }

    public static void fillBoxTo(class_287 builder, class_4587 stack, float x1, float y1, float z1, float x2, float y2, float z2, float thickness, float r, float g, float b, float a)
    {
        if (stack == null)
        {
            stack = new class_4587();
            MatrixStackUtils.multiply(stack, RenderSystem.getModelViewMatrix());
        }

        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        Angle angle = Angle.angle(dx, dy, dz);

        stack.method_22903();

        stack.method_46416(x1, y1, z1);
        stack.method_22907(class_7833.field_40716.rotationDegrees(angle.yaw));
        stack.method_22907(class_7833.field_40714.rotationDegrees(angle.pitch));

        fillBox(builder, stack, -thickness / 2, -thickness / 2, 0, thickness / 2, thickness / 2, (float) distance, r, g, b, a);

        stack.method_22909();
    }

    public static void fillBox(class_287 builder, class_4587 stack, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b)
    {
        fillBox(builder, stack, x1, y1, z1, x2, y2, z2, r, g, b, 1F);
    }

    public static void fillBox(class_287 builder, class_4587 stack, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a)
    {
        /* X */
        fillQuad(builder, stack, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1, r, g, b, a);
        fillQuad(builder, stack, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, b, a);

        /* Y */
        fillQuad(builder, stack, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a);
        fillQuad(builder, stack, x2, y2, z1, x1, y2, z1, x1, y2, z2, x2, y2, z2, r, g, b, a);

        /* Z */
        fillQuad(builder, stack, x2, y1, z1, x1, y1, z1, x1, y2, z1, x2, y2, z1, r, g, b, a);
        fillQuad(builder, stack, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, r, g, b, a);
    }

    public static void coolerAxes(class_4587 stack, float axisSize, float axisOffset, float outlineSize, float outlineOffset)
    {
        float scale = BBSSettings.axesScale.get();

        axisSize *= scale;
        axisOffset *= scale;
        outlineSize *= scale;
        outlineOffset *= scale;

        class_287 builder = class_289.method_1348().method_60827(class_293.class_5596.field_27379, class_290.field_1576);

        fillBox(builder, stack, 0, -outlineOffset, -outlineOffset, outlineSize, outlineOffset, outlineOffset, 0, 0, 0);
        fillBox(builder, stack, -outlineOffset, 0, -outlineOffset, outlineOffset, outlineSize, outlineOffset, 0, 0, 0);
        fillBox(builder, stack, -outlineOffset, -outlineOffset, 0, outlineOffset, outlineOffset, outlineSize, 0, 0, 0);
        fillBox(builder, stack, -outlineOffset, -outlineOffset, -outlineOffset, outlineOffset, outlineOffset, outlineOffset, 0, 0, 0);

        fillBox(builder, stack, 0, -axisOffset, -axisOffset, axisSize, axisOffset, axisOffset, 1, 0, 0);
        fillBox(builder, stack, -axisOffset, 0, -axisOffset, axisOffset, axisSize, axisOffset, 0, 1, 0);
        fillBox(builder, stack, -axisOffset, -axisOffset, 0, axisOffset, axisOffset, axisSize, 0, 0, 1);
        fillBox(builder, stack, -axisOffset, -axisOffset, -axisOffset, axisOffset, axisOffset, axisOffset, 1, 1, 1);

        RenderSystem.setShader(class_757::method_34540);
        RenderSystem.disableDepthTest();

        class_286.method_43433(builder.method_60800());
    }

    /**
     * Draws a solid cone (with a capped base) between two points, used for the tapered
     * arrow tips on gizmo translate handles. The base circle is perpendicular to the
     * apex-to-base direction, so it works for any axis without extra stack rotation.
     */
    public static void cone(class_287 builder, class_4587 stack, float apexX, float apexY, float apexZ, float baseX, float baseY, float baseZ, float radius, int segments, float r, float g, float b, float a)
    {
        Matrix4f mat = stack.method_23760().method_23761();

        float dx = baseX - apexX;
        float dy = baseY - apexY;
        float dz = baseZ - apexZ;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (len < 1.0E-6F)
        {
            return;
        }

        dx /= len;
        dy /= len;
        dz /= len;

        float upx = 0F;
        float upy = 1F;
        float upz = 0F;

        if (Math.abs(dy) > 0.99F)
        {
            upx = 1F;
            upy = 0F;
            upz = 0F;
        }

        float rx = dy * upz - dz * upy;
        float ry = dz * upx - dx * upz;
        float rz = dx * upy - dy * upx;
        float rl = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);

        rx /= rl;
        ry /= rl;
        rz /= rl;

        float ux = ry * dz - rz * dy;
        float uy = rz * dx - rx * dz;
        float uz = rx * dy - ry * dx;

        float[] cosA = SCRATCH_COS_U;
        float[] sinA = SCRATCH_SIN_U;

        for (int i = 0; i < segments; i++)
        {
            double ang = Math.PI * 2D * i / segments;

            cosA[i] = (float) Math.cos(ang);
            sinA[i] = (float) Math.sin(ang);
        }

        for (int i = 0; i < segments; i++)
        {
            int i2 = (i + 1) % segments;
            float c1 = cosA[i];
            float s1 = sinA[i];
            float c2 = cosA[i2];
            float s2 = sinA[i2];

            float x1 = baseX + (rx * c1 + ux * s1) * radius;
            float y1 = baseY + (ry * c1 + uy * s1) * radius;
            float z1 = baseZ + (rz * c1 + uz * s1) * radius;

            float x2 = baseX + (rx * c2 + ux * s2) * radius;
            float y2 = baseY + (ry * c2 + uy * s2) * radius;
            float z2 = baseZ + (rz * c2 + uz * s2) * radius;

            builder.method_22918(mat, apexX, apexY, apexZ).method_22915(r, g, b, a);
            builder.method_22918(mat, x1, y1, z1).method_22915(r, g, b, a);
            builder.method_22918(mat, x2, y2, z2).method_22915(r, g, b, a);

            builder.method_22918(mat, x1, y1, z1).method_22915(r, g, b, a);
            builder.method_22918(mat, baseX, baseY, baseZ).method_22915(r, g, b, a);
            builder.method_22918(mat, x2, y2, z2).method_22915(r, g, b, a);
        }
    }

    /**
     * Draws a standard UV sphere centered at the local origin, used for the invisible
     * free-rotate trackball hit volume (and its stencil id encoding).
     */
    public static void sphere(class_287 builder, class_4587 stack, float radius, int rings, int sectors, float r, float g, float b, float a)
    {
        Matrix4f mat = stack.method_23760().method_23761();
        float[] sinV = SCRATCH_SIN_V;
        float[] cosV = SCRATCH_COS_V;
        float[] sinU = SCRATCH_SIN_U;
        float[] cosU = SCRATCH_COS_U;

        for (int i = 0; i <= rings; i++)
        {
            double v = Math.PI * i / rings;

            sinV[i] = (float) Math.sin(v);
            cosV[i] = (float) Math.cos(v);
        }

        for (int j = 0; j <= sectors; j++)
        {
            double u = Math.PI * 2D * j / sectors;

            sinU[j] = (float) Math.sin(u);
            cosU[j] = (float) Math.cos(u);
        }

        for (int i = 0; i < rings; i++)
        {
            float sv1 = sinV[i];
            float cv1 = cosV[i];
            float sv2 = sinV[i + 1];
            float cv2 = cosV[i + 1];

            for (int j = 0; j < sectors; j++)
            {
                float cu1 = cosU[j];
                float su1 = sinU[j];
                float cu2 = cosU[j + 1];
                float su2 = sinU[j + 1];

                float x11 = sv1 * cu1 * radius;
                float y11 = cv1 * radius;
                float z11 = sv1 * su1 * radius;

                float x12 = sv2 * cu1 * radius;
                float y12 = cv2 * radius;
                float z12 = sv2 * su1 * radius;

                float x21 = sv1 * cu2 * radius;
                float y21 = y11;
                float z21 = sv1 * su2 * radius;

                float x22 = sv2 * cu2 * radius;
                float y22 = y12;
                float z22 = sv2 * su2 * radius;

                builder.method_22918(mat, x11, y11, z11).method_22915(r, g, b, a);
                builder.method_22918(mat, x12, y12, z12).method_22915(r, g, b, a);
                builder.method_22918(mat, x22, y22, z22).method_22915(r, g, b, a);

                builder.method_22918(mat, x11, y11, z11).method_22915(r, g, b, a);
                builder.method_22918(mat, x22, y22, z22).method_22915(r, g, b, a);
                builder.method_22918(mat, x21, y21, z21).method_22915(r, g, b, a);
            }
        }
    }

    public static void arc3D(class_287 builder, class_4587 stack, Axis axis, float radius, float thickness, float r, float g, float b)
    {
        arc3D(builder, stack, axis, radius, thickness, r, g, b, 0F, 360F, false);
    }

    public static void arc3D(class_287 builder, class_4587 stack, Axis axis, float radius, float thickness, float r, float g, float b, float startDeg, float sweepDeg)
    {
        arc3D(builder, stack, axis, radius, thickness, r, g, b, startDeg, sweepDeg, false);
    }

    /**
     * Torus-segment ring. Segment counts scale with sweep so half-rings and short process
     * arcs stay cheap; {@code lowDetail} is for invisible stencil/pick passes.
     */
    public static void arc3D(class_287 builder, class_4587 stack, Axis axis, float radius, float thickness, float r, float g, float b, float startDeg, float sweepDeg, boolean lowDetail)
    {
        float absSweep = Math.abs(sweepDeg);

        if (absSweep < 0.01F || thickness <= 0F || radius <= 0F)
        {
            return;
        }

        /* Visual: ~64×10 for a full ring (was 96×24). Stencil: ~36×5. Scales with sweep. */
        int segU = Math.max(lowDetail ? 6 : 12, Math.round((lowDetail ? 36F : 64F) * absSweep / 360F));
        /* Continuous drags can accumulate past 360°; never write past the scratch buffers. */
        segU = Math.min(segU, SCRATCH_COS_U.length - 1);
        int segV = lowDetail ? 5 : 10;
        segV = Math.min(segV, SCRATCH_COS_V.length - 1);
        double u0 = Math.toRadians(startDeg);
        double uStep = Math.toRadians(sweepDeg / (double) segU);
        double vStep = Math.PI * 2D / (double) segV;

        stack.method_22903();

        if (axis == Axis.X) stack.method_22907(class_7833.field_40718.rotation(MathUtils.PI / 2F));
        if (axis == Axis.Z) stack.method_22907(class_7833.field_40714.rotation(MathUtils.PI / 2F));

        float tubeR = thickness * 0.5F;
        Matrix4f mat = stack.method_23760().method_23761();

        float[] cosV = SCRATCH_COS_V;
        float[] sinV = SCRATCH_SIN_V;
        float[] cosU = SCRATCH_COS_U;
        float[] sinU = SCRATCH_SIN_U;

        for (int iv = 0; iv <= segV; iv++)
        {
            double v = vStep * iv;

            cosV[iv] = (float) Math.cos(v);
            sinV[iv] = (float) Math.sin(v);
        }

        for (int iu = 0; iu <= segU; iu++)
        {
            double u = u0 + uStep * iu;

            cosU[iu] = (float) Math.cos(u);
            sinU[iu] = (float) Math.sin(u);
        }

        for (int iu = 0; iu < segU; iu++)
        {
            float cu1 = cosU[iu];
            float su1 = sinU[iu];
            float cu2 = cosU[iu + 1];
            float su2 = sinU[iu + 1];

            for (int iv = 0; iv < segV; iv++)
            {
                float ring1 = radius + tubeR * cosV[iv];
                float ring2 = radius + tubeR * cosV[iv + 1];
                float y1 = tubeR * sinV[iv];
                float y2 = tubeR * sinV[iv + 1];

                float x11 = ring1 * cu1;
                float z11 = ring1 * su1;
                float x12 = ring2 * cu1;
                float z12 = ring2 * su1;
                float x21 = ring1 * cu2;
                float z21 = ring1 * su2;
                float x22 = ring2 * cu2;
                float z22 = ring2 * su2;

                builder.method_22918(mat, x11, y1, z11).method_22915(r, g, b, 1F);
                builder.method_22918(mat, x12, y2, z12).method_22915(r, g, b, 1F);
                builder.method_22918(mat, x22, y2, z22).method_22915(r, g, b, 1F);

                builder.method_22918(mat, x11, y1, z11).method_22915(r, g, b, 1F);
                builder.method_22918(mat, x22, y2, z22).method_22915(r, g, b, 1F);
                builder.method_22918(mat, x21, y1, z21).method_22915(r, g, b, 1F);
            }
        }

        stack.method_22909();
    }
}