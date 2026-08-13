package mchorse.bbs_mod.forms.forms.utils;

import mchorse.bbs_mod.utils.MathUtils;

/**
 * Form {@code lighting} brightness helpers (common / server-safe).
 * <p>
 * Legacy BBS used world-lighting influence: {@code 1} = natural, {@code 0} = full bright
 * (negatives/`&gt;1` clamped to those). Current blend semantic is brightness override:
 * {@code 0} = natural world lighting, {@code 1} = full bright.
 */
public final class FormLighting
{
    private FormLighting()
    {
    }

    public static float clampBrightness(float value)
    {
        return MathUtils.clamp(value, 0F, 1F);
    }

    /**
     * Convert a legacy lighting float to brightness ({@code 0} natural … {@code 1} full bright).
     */
    public static float legacyToBrightness(float legacy)
    {
        if (legacy < 0F)
        {
            return 1F;
        }

        if (legacy > 1F)
        {
            return 0F;
        }

        return 1F - legacy;
    }

    /**
     * Convert brightness back to the legacy world-influence float for older builds.
     */
    public static float brightnessToLegacy(float brightness)
    {
        return 1F - clampBrightness(brightness);
    }

    /**
     * Approximate fixed light level as a brightness for legacy float dual-write.
     */
    public static float fixedLevelToBrightness(float level)
    {
        return MathUtils.clamp(level, 0F, 15F) / 15F;
    }

    /**
     * Convert lighting settings to a legacy float for older builds.
     */
    public static float settingsToLegacy(LightingSettings settings)
    {
        if (settings == null)
        {
            return brightnessToLegacy(0F);
        }

        if (settings.fixed)
        {
            return brightnessToLegacy(fixedLevelToBrightness(settings.resolveLevel()));
        }

        return brightnessToLegacy(settings.brightness);
    }
}
