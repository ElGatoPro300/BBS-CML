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

    private LensDistortionOverscan()
    {}

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

    /**
     * Blend full-frame overscan toward identity by lens radius cover.
     * {@code radiusCover} of 1 = full FOV match; 0 = no overscan. Avoids a hard
     * zoom jump when radius crosses 1 → 0.99.
     */
    public static float overscanScaleForRadius(float lensDistortion, float radiusCover)
    {
        float full = overscanScale(lensDistortion);
        float cover = MathUtils.clamp(radiusCover, 0F, 1F);

        if (cover <= 1.0e-4F || Math.abs(full - 1F) <= 1.0e-6F)
        {
            return 1F;
        }

        return 1F + (full - 1F) * cover;
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
