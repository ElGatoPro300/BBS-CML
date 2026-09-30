package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.utils.Quad;

import org.joml.Intersectionf;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Soft-opacity queue keys for flat quads (Billboard, VideoForm, …).
 * Farther first: camera look-ray hit on the finite face (clamped), not form origin alone.
 */
public final class SoftFlatFaceSort
{
    public static final float DEFAULT_FACE_Z_BIAS = 0.0005F;

    /* Pull the key slightly toward the camera so near-coplanar soft meshes paint before the plane. */
    public static final float SOFT_FACE_NEAR_BIAS = 0.05F;

    private static final Vector3f CORNER_A = new Vector3f();
    private static final Vector3f CORNER_B = new Vector3f();
    private static final Vector3f CORNER_C = new Vector3f();
    private static final Vector3f CORNER_D = new Vector3f();
    private static final Vector3f TMP = new Vector3f();
    private static final Vector3f HIT = new Vector3f();
    private static final Vector3f NORMAL = new Vector3f();
    private static final Vector3f ORIGIN = new Vector3f();
    private static final Vector3f LOOK = new Vector3f(0F, 0F, -1F);
    private static final Vector3f AB = new Vector3f();
    private static final Vector3f AC = new Vector3f();
    private static final Vector3f AP = new Vector3f();
    private static final Vector3f BP = new Vector3f();
    private static final Vector3f CP = new Vector3f();
    private static final Vector3f BEST = new Vector3f();
    private static final Vector3f CANDIDATE = new Vector3f();
    private static final Vector4f CORNER_H = new Vector4f();

    private SoftFlatFaceSort()
    {
    }

    public static double computeFaceSortKey(Matrix4f drawMatrix, FormRenderingContext context, Quad quad)
    {
        return computeFaceSortKey(drawMatrix, context, quad, DEFAULT_FACE_Z_BIAS);
    }

    public static double computeFaceSortKey(Matrix4f drawMatrix, FormRenderingContext context, Quad quad, float faceZBias)
    {
        Matrix4f viewSpace = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(drawMatrix));
        Vector3f hit = computeLookHitView(viewSpace, quad, faceZBias);

        boolean filmLookAxis = context != null
            && context.type == FormRenderType.ENTITY
            && context.camera != null
            && !context.modelRenderer;

        if (filmLookAxis)
        {
            return -hit.z - SOFT_FACE_NEAR_BIAS;
        }

        float len = hit.length();

        if (len > 1.0E-6F)
        {
            float scale = Math.max(0F, len - SOFT_FACE_NEAR_BIAS) / len;

            hit.mul(scale);
        }

        return hit.lengthSquared();
    }

    private static Vector3f computeLookHitView(Matrix4f viewSpace, Quad quad, float faceZBias)
    {
        transformCorner(viewSpace, quad.p1.x, quad.p1.y, faceZBias, CORNER_A);
        transformCorner(viewSpace, quad.p2.x, quad.p2.y, faceZBias, CORNER_B);
        transformCorner(viewSpace, quad.p3.x, quad.p3.y, faceZBias, CORNER_C);
        transformCorner(viewSpace, quad.p4.x, quad.p4.y, faceZBias, CORNER_D);

        NORMAL.set(CORNER_B).sub(CORNER_A).cross(TMP.set(CORNER_C).sub(CORNER_A));

        if (NORMAL.lengthSquared() < 1.0E-12F)
        {
            return HIT.set(CORNER_A)
                .add(CORNER_B)
                .add(CORNER_C)
                .add(CORNER_D)
                .mul(0.25F);
        }

        NORMAL.normalize();
        ORIGIN.set(0F, 0F, 0F);

        float t = Intersectionf.intersectRayPlane(ORIGIN, LOOK, CORNER_A, NORMAL, 1.0E-6F);

        if (!Float.isFinite(t) || t < 0F)
        {
            return closestPointOnQuad(ORIGIN);
        }

        HIT.set(LOOK).mul(t);

        if (pointOnQuad(HIT))
        {
            return HIT;
        }

        return closestPointOnQuad(HIT);
    }

    private static void transformCorner(Matrix4f viewSpace, float x, float y, float faceZBias, Vector3f out)
    {
        CORNER_H.set(x, y, faceZBias, 1F);
        viewSpace.transform(CORNER_H);
        out.set(CORNER_H.x, CORNER_H.y, CORNER_H.z);
    }

    private static boolean pointOnQuad(Vector3f point)
    {
        return pointOnTriangle(point, CORNER_A, CORNER_B, CORNER_C)
            || pointOnTriangle(point, CORNER_B, CORNER_D, CORNER_C);
    }

    private static boolean pointOnTriangle(Vector3f point, Vector3f a, Vector3f b, Vector3f c)
    {
        AB.set(b).sub(a);
        AC.set(c).sub(a);
        AP.set(point).sub(a);

        float d00 = AB.dot(AB);
        float d01 = AB.dot(AC);
        float d11 = AC.dot(AC);
        float d20 = AP.dot(AB);
        float d21 = AP.dot(AC);
        float denom = d00 * d11 - d01 * d01;

        if (Math.abs(denom) < 1.0E-12F)
        {
            return false;
        }

        float v = (d11 * d20 - d01 * d21) / denom;
        float w = (d00 * d21 - d01 * d20) / denom;
        float u = 1F - v - w;

        return u >= -1.0E-4F && v >= -1.0E-4F && w >= -1.0E-4F;
    }

    private static Vector3f closestPointOnQuad(Vector3f point)
    {
        closestPointOnTriangle(CANDIDATE, CORNER_A, CORNER_B, CORNER_C, point);
        BEST.set(CANDIDATE);
        float bestDist = BEST.distanceSquared(point);

        closestPointOnTriangle(CANDIDATE, CORNER_B, CORNER_D, CORNER_C, point);

        if (CANDIDATE.distanceSquared(point) < bestDist)
        {
            BEST.set(CANDIDATE);
        }

        return HIT.set(BEST);
    }

    private static void closestPointOnTriangle(Vector3f out, Vector3f a, Vector3f b, Vector3f c, Vector3f p)
    {
        AB.set(b).sub(a);
        AC.set(c).sub(a);
        AP.set(p).sub(a);

        float d1 = AB.dot(AP);
        float d2 = AC.dot(AP);

        if (d1 <= 0F && d2 <= 0F)
        {
            out.set(a);

            return;
        }

        BP.set(p).sub(b);
        float d3 = AB.dot(BP);
        float d4 = AC.dot(BP);

        if (d3 >= 0F && d4 <= d3)
        {
            out.set(b);

            return;
        }

        float vc = d1 * d4 - d3 * d2;

        if (vc <= 0F && d1 >= 0F && d3 <= 0F)
        {
            float v = d1 / (d1 - d3);

            out.set(a).fma(v, AB);

            return;
        }

        CP.set(p).sub(c);
        float d5 = AB.dot(CP);
        float d6 = AC.dot(CP);

        if (d6 >= 0F && d5 <= d6)
        {
            out.set(c);

            return;
        }

        float vb = d5 * d2 - d1 * d6;

        if (vb <= 0F && d2 >= 0F && d6 <= 0F)
        {
            float w = d2 / (d2 - d6);

            out.set(a).fma(w, AC);

            return;
        }

        float va = d3 * d6 - d5 * d4;

        if (va <= 0F && (d4 - d3) >= 0F && (d5 - d6) >= 0F)
        {
            float w = (d4 - d3) / ((d4 - d3) + (d5 - d6));

            out.set(b).fma(w, TMP.set(c).sub(b));

            return;
        }

        float denom = 1F / (va + vb + vc);
        float v = vb * denom;
        float w = vc * denom;

        out.set(a).fma(v, AB).fma(w, AC);
    }
}
