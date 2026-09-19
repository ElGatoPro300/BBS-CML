package mchorse.bbs_mod.camera.clips.screen;

import mchorse.bbs_mod.utils.MathUtils;

/**
 * Fisheye UV warp uses {@code uv' = uv * (1 + k * r²)}.
 * <ul>
 *   <li>Positive {@code k}: zoom-out — widen render FOV by {@code s = 1 + CORNER_R2·k}, then
 *       divide warped UVs by {@code s} so screen corners map to the wide image corners.</li>
 *   <li>Negative {@code k}: zoom-in — narrow render FOV by {@code s = 1 + EDGE_R2·k}.
 *       Corner-matched {@code s} would push mid-edge UVs outside {@code [0,1]} (texture
 *       REPEAT → tiled grid). Edge-matched {@code s} keeps the whole screen inside the
 *       texture while still filling the frame.</li>
 * </ul>
 */
public final class LensDistortionOverscan
{
    /** {@code r²} at UV corners of the unit square (offset ±0.5, ±0.5). */
    public static final float CORNER_R2 = 0.5F;
    /** {@code r²} at mid-edge of the unit square (offset ±0.5, 0) — bound for negative k. */
    public static final float EDGE_R2 = 0.25F;
    /**
     * Floor for negative-fisheye FOV shrink. Must stay {@code > 0.5} so
     * {@code 1 + CORNER_R2 * kFit} stays positive when {@code kFit = (s-1)/EDGE_R2}.
     */
    public static final float MIN_UNDERSCAN_SCALE = 0.55F;
    /**
     * Reference subject distance (blocks) used to convert the positive-fisheye UV
     * fit-zoom into a look-axis dolly, matching the “same place” framing feel.
     */
    public static final float FRAMING_FOCUS_BLOCKS = 4F;
    /** UV half-diagonal of the unit square ({@code √0.5}). */
    public static final float CORNER_RADIUS = 0.70710678F;

    private LensDistortionOverscan()
    {}

    /**
     * Same UV fit scale used by the color-grade fisheye shader for positive {@code k}.
     * {@code > 1} means the warp zooms into the native frame to stay in bounds.
     */
    public static float positiveFitScale(float lensDistortion, float lensRadiusX, float lensRadiusY, float lensHardness)
    {
        if (lensDistortion <= 1.0e-6F)
        {
            return 1F;
        }

        float radiusX = Math.max(lensRadiusX * CORNER_RADIUS, 1.0e-6F);
        float radiusY = Math.max(lensRadiusY * CORNER_RADIUS, 1.0e-6F);
        float hardness = MathUtils.clamp(lensHardness, 0F, 1F);
        float feather = (1F - hardness) * 0.75F;
        /* Match the shader: isotropic extent capped at the UV half-edge so
         * radius > 1 (unlocked X/Y) does not under-fit and clamp-smear. */
        float extent = Math.min(0.5F, Math.max(radiusX, radiusY) * (1F + feather));

        return Math.max(1F, extent * (1F + lensDistortion * CORNER_R2) / 0.5F);
    }

    /**
     * Look-axis distance offset that counters the positive-fisheye UV zoom so a subject
     * around {@link #FRAMING_FOCUS_BLOCKS} keeps similar screen size. Negative = dolly back.
     * Multiplied by the animatable distance-factor track by the caller.
     */
    public static float framingDistanceOffset(float lensDistortion, float lensRadiusX, float lensRadiusY, float lensHardness)
    {
        float fitScale = positiveFitScale(lensDistortion, lensRadiusX, lensRadiusY, lensHardness);

        if (fitScale <= 1.0e-4F)
        {
            return 0F;
        }

        return FRAMING_FOCUS_BLOCKS * (1F - fitScale);
    }

    /**
     * Tan-space scale matching the UV remapping for {@code k}.
     * {@code > 1} widen (corner-matched), {@code < 1} narrow (edge-matched), {@code 1} off.
     */
    public static float overscanScale(float lensDistortion)
    {
        if (Math.abs(lensDistortion) <= 1.0e-6F)
        {
            return 1F;
        }

        if (lensDistortion > 0F)
        {
            return 1F + CORNER_R2 * lensDistortion;
        }

        return Math.max(MIN_UNDERSCAN_SCALE, 1F + EDGE_R2 * lensDistortion);
    }

    public static float adjustFovDegrees(float fovDegrees, float lensDistortion)
    {
        return adjustFovDegreesByScale(fovDegrees, overscanScale(lensDistortion));
    }

    public static float widenFovDegrees(float fovDegrees, float lensDistortion)
    {
        if (lensDistortion <= 0F)
        {
            return fovDegrees;
        }

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

    /** {@code k} implied by an overscan scale (corner fit for {@code s>1}, edge fit for {@code s<1}). */
    public static float kFitForScale(float scale)
    {
        if (scale > 1F)
        {
            return (scale - 1F) / CORNER_R2;
        }

        if (scale < 1F)
        {
            return (scale - 1F) / EDGE_R2;
        }

        return 0F;
    }
}
