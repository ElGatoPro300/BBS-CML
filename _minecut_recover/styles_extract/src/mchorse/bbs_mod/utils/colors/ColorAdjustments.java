package mchorse.bbs_mod.utils.colors;

import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.interps.Lerps;

/**
 * Brightness / contrast / hue / saturation adjustments for form Blend Color.
 * Neutral values are all {@code 0}. Formulas match screen color grading
 * ({@code ColorGradeRenderer}).
 */
public final class ColorAdjustments
{
    public static final float EPSILON = 0.001F;
    public static final float MIN_BRIGHTNESS = -1F;
    public static final float MAX_BRIGHTNESS = 1F;
    public static final float MIN_CONTRAST = -1F;
    public static final float MAX_CONTRAST = 10F;
    public static final float MIN_HUE = -180F;
    public static final float MAX_HUE = 180F;
    public static final float MIN_SATURATION = -1F;
    public static final float MAX_SATURATION = 10F;

    private static final Color HSV = new Color();

    private ColorAdjustments()
    {}

    public static boolean isActive(float brightness, float contrast, float hue, float saturation)
    {
        return Math.abs(brightness) > EPSILON
            || Math.abs(contrast) > EPSILON
            || Math.abs(hue) > EPSILON
            || Math.abs(saturation) > EPSILON;
    }

    public static float clampBrightness(float value)
    {
        return MathUtils.clamp(value, MIN_BRIGHTNESS, MAX_BRIGHTNESS);
    }

    public static float clampContrast(float value)
    {
        return MathUtils.clamp(value, MIN_CONTRAST, MAX_CONTRAST);
    }

    public static float clampHue(float value)
    {
        return MathUtils.clamp(value, MIN_HUE, MAX_HUE);
    }

    public static float clampSaturation(float value)
    {
        return MathUtils.clamp(value, MIN_SATURATION, MAX_SATURATION);
    }

    /**
     * Mutates {@code color} RGB in place. Alpha and adjustment fields are left unchanged.
     */
    public static void apply(Color color, float brightness, float contrast, float hue, float saturation)
    {
        if (color == null || !isActive(brightness, contrast, hue, saturation))
        {
            return;
        }

        float r = color.r + brightness;
        float g = color.g + brightness;
        float b = color.b + brightness;

        r = 0.5F + (1F + contrast) * (r - 0.5F);
        g = 0.5F + (1F + contrast) * (g - 0.5F);
        b = 0.5F + (1F + contrast) * (b - 0.5F);

        float luma = 0.2126F * r + 0.7152F * g + 0.0722F * b;
        float satMix = 1F + saturation;

        r = Lerps.lerp(luma, r, satMix);
        g = Lerps.lerp(luma, g, satMix);
        b = Lerps.lerp(luma, b, satMix);

        if (Math.abs(hue) > EPSILON)
        {
            Colors.RGBtoHSV(HSV, r, g, b);
            float h = HSV.r + hue / 360F;

            h %= 1F;

            if (h < 0F)
            {
                h += 1F;
            }

            Colors.HSVtoRGB(HSV, h, HSV.g, HSV.b);
            r = HSV.r;
            g = HSV.g;
            b = HSV.b;
        }

        color.r = MathUtils.clamp(r, 0F, 1F);
        color.g = MathUtils.clamp(g, 0F, 1F);
        color.b = MathUtils.clamp(b, 0F, 1F);
    }
}
