package mchorse.bbs_mod.camera.clips.screen;

import mchorse.bbs_mod.utils.MathUtils;

/**
 * Fisheye UV warp uses {@code uv' = uv * (1 + k * r²)}.
 * <ul>
 *   <li>Positive {@code k}: zoom-out — widen render FOV by {@code s = 1 + 0.5·k}, then
 *       divide warped UVs by {@code s} so screen edges map to the wide image edges.</li>
 *   <li>Negative {@code k}: zoom-in — narrow render FOV by the same {@code s < 1}, then
 *       divide by {@code s} so screen edges still map to rendered edges (no empty border).</li>
 * </ul>
 */
public final class LensDistortionOverscan
{
    public static final float CORNER_R2 = 0.5F;
    /** Floor for negative-fisheye FOV shrink so tan-space scale stays usable. */
    public static final float MIN_UNDERSCAN_SCALE = 0.2F;

    private LensDistortionOverscan()
    {}

    /**
     * Tan-space scale matching corner warp {@code 1 + k·CORNER_R2}.
     * {@code > 1} widen, {@code < 1} narrow, {@code 1} off.
     */
    public static float overscanScale(float lensDistortion)
    {
        if (Math.abs(lensDistortion) <= 1.0e-6F)
        {
            return 1F;
        }

        float scale = 1F + CORNER_R2 * lensDistortion;

        if (scale >= 1F)
        {
            return scale;
        }

        return Math.max(MIN_UNDERSCAN_SCALE, scale);
    }

    public static float adjustFovDegrees(float fovDegrees, float lensDistortion)
    {
        return adjustFovDegreesByScale(fovDegrees, overscanScale(lensDistortion));
    }

    /** @deprecated use {@link #adjustFovDegrees(float, float)} */
    @Deprecated
    public static float widenFovDegrees(float fovDegrees, float lensDistortion)
    {
        return adjustFovDegrees(fovDegrees, lensDistortion);
    }

    public static float adjustFovDegreesByScale(float fovDegrees, float scale)
    {
        if (Math.abs(scale - 1F) <= 1.0e-4F)
        {
            return fovDegrees;
        }

        float half = MathUtils.toRad(fovDegrees) * 0.5F;
        float tanHalf = (float) Math.tan(half);

        if (!Float.isFinite(tanHalf) || tanHalf <= 0F)
        {
            return fovDegrees;
        }

        float scaledHalf = (float) Math.atan(tanHalf * (double) scale);
        float scaled = MathUtils.toDeg(scaledHalf * 2F);

        if (!Float.isFinite(scaled))
        {
            return fovDegrees;
        }

        return Math.max(1F, scaled);
    }

    /**
     * Actual tan-space scale achieved between two vertical FOV values ({@code >1} widen,
     * {@code <1} narrow).
     */
    public static float scaleBetweenFovDegrees(float fovBeforeDegrees, float fovAfterDegrees)
    {
        float halfBefore = MathUtils.toRad(fovBeforeDegrees) * 0.5F;
        float halfAfter = MathUtils.toRad(fovAfterDegrees) * 0.5F;
        float tanBefore = (float) Math.tan(halfBefore);
        float tanAfter = (float) Math.tan(halfAfter);

        if (!Float.isFinite(tanBefore) || tanBefore <= 1.0e-6F || !Float.isFinite(tanAfter) || tanAfter <= 1.0e-6F)
        {
            return 1F;
        }

        float scale = tanAfter / tanBefore;

        if (!Float.isFinite(scale) || scale <= 1.0e-6F)
        {
            return 1F;
        }

        return scale;
    }

    public static boolean isActiveScale(float scale)
    {
        return Float.isFinite(scale) && Math.abs(scale - 1F) > 1.0e-4F && scale > 1.0e-3F;
    }
}
