package mchorse.bbs_mod.camera.clips.screen;

import mchorse.bbs_mod.utils.MathUtils;

/**
 * Fisheye zoom-out (positive {@code k}) expands UV corners by {@code 1 + 0.5 * k}.
 * When FOV-widen mode is enabled, the world pass widens FOV by the same factor
 * so the post-process warp can map screen edges to the rendered image edges.
 */
public final class LensDistortionOverscan
{
    public static final float CORNER_R2 = 0.5F;

    private LensDistortionOverscan()
    {}

    public static float overscanScale(float lensDistortion)
    {
        if (lensDistortion <= 0F)
        {
            return 1F;
        }

        return 1F + CORNER_R2 * lensDistortion;
    }

    public static float widenFovDegrees(float fovDegrees, float lensDistortion)
    {
        float scale = overscanScale(lensDistortion);

        if (scale <= 1.0001F)
        {
            return fovDegrees;
        }

        float half = MathUtils.toRad(fovDegrees) * 0.5F;
        float tanHalf = (float) Math.tan(half);

        if (!Float.isFinite(tanHalf) || tanHalf <= 0F)
        {
            return fovDegrees;
        }

        float wideHalf = (float) Math.atan(tanHalf * (double) scale);
        float widened = MathUtils.toDeg(wideHalf * 2F);

        return Float.isFinite(widened) ? Math.max(1F, widened) : fovDegrees;
    }

    /**
     * Actual tan-space scale achieved between two vertical FOV values.
     */
    public static float scaleBetweenFovDegrees(float fovBeforeDegrees, float fovAfterDegrees)
    {
        float halfBefore = MathUtils.toRad(fovBeforeDegrees) * 0.5F;
        float halfAfter = MathUtils.toRad(fovAfterDegrees) * 0.5F;
        float tanBefore = (float) Math.tan(halfBefore);
        float tanAfter = (float) Math.tan(halfAfter);

        if (!Float.isFinite(tanBefore) || tanBefore <= 1.0e-6F || !Float.isFinite(tanAfter) || tanAfter <= 0F)
        {
            return 1F;
        }

        float scale = tanAfter / tanBefore;

        if (!Float.isFinite(scale) || scale < 1F)
        {
            return 1F;
        }

        return scale;
    }
}
