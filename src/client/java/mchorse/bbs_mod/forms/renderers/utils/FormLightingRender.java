package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.forms.forms.utils.FormLighting;
import mchorse.bbs_mod.forms.forms.utils.LightingSettings;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.interps.Lerps;

import net.minecraft.client.render.LightmapTextureManager;

/**
 * Client-side packing of form lighting into Minecraft lightmap coordinates.
 */
public final class FormLightingRender
{
    private FormLightingRender()
    {
    }

    public static int applyBrightness(int packedLight, float brightness)
    {
        float lf = FormLighting.clampBrightness(brightness);
        int u = packedLight & '\uffff';
        int v = packedLight >> 16 & '\uffff';

        u = (int) Lerps.lerp(u, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, lf);

        return u | v << 16;
    }

    /**
     * Absolute form lighting for fixed light levels.
     * <p>
     * When {@code truncate} is true, uses discrete Minecraft light levels 0–15.
     * When false, maps the float level through the same continuous lightmap range as
     * brightness 0–1 ({@code 0}…{@link LightmapTextureManager#MAX_BLOCK_LIGHT_COORDINATE}),
     * so intermediate values (e.g. {@code 7.5}) are not snapped to whole levels.
     */
    public static int packFixedLevel(float level, boolean truncate)
    {
        float clamped = MathUtils.clamp(level, 0F, 15F);

        if (truncate)
        {
            int i = Math.round(clamped);

            return LightmapTextureManager.pack(i, i);
        }

        /* Continuous absolute lighting — same UV span as brightness 0–1, both channels.
         * model.vsh samples with minecraft_sample_lightmap (filtered), so fractional
         * coordinates blend between adjacent MC light levels. */
        float t = clamped / 15F;
        int coord = Math.round(Lerps.lerp(0F, (float) LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, t));

        return coord | coord << 16;
    }

    public static int apply(int packedLight, LightingSettings settings, float brightnessFallback)
    {
        if (settings != null)
        {
            if (settings.fixed)
            {
                return packFixedLevel(settings.level, settings.truncate);
            }

            return applyBrightness(packedLight, settings.brightness);
        }

        return applyBrightness(packedLight, brightnessFallback);
    }
}
