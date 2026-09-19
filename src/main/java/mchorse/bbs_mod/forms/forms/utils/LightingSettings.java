package mchorse.bbs_mod.forms.forms.utils;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.MathUtils;

import java.util.Objects;

/**
 * Replay lighting keyframe payload.
 * <p>
 * When {@link #fixed} is false, {@link #brightness} blends toward full bright
 * ({@code 0} = world/natural, {@code 1} = full bright). When {@link #fixed} is true,
 * {@link #level} is an absolute Minecraft light level ({@code 0}–{@code 15}) that
 * ignores world lighting; {@link #truncate} snaps that level to integers.
 */
public class LightingSettings
{
    public float brightness;
    public boolean fixed;
    public float level = 15F;
    public boolean truncate;

    public LightingSettings()
    {
    }

    public LightingSettings(float brightness, boolean fixed, float level, boolean truncate)
    {
        this.brightness = brightness;
        this.fixed = fixed;
        this.level = level;
        this.truncate = truncate;
    }

    public static LightingSettings fromBrightness(float brightness)
    {
        return new LightingSettings(FormLighting.clampBrightness(brightness), false, 15F, false);
    }

    public float resolveLevel()
    {
        float clamped = MathUtils.clamp(this.level, 0F, 15F);

        /* Snap only for consumers that need a discrete MC light level (legacy export, etc.).
         * Playback keeps {@link #level} as a float and lets the renderer truncate when packing. */
        return this.truncate ? Math.round(clamped) : clamped;
    }

    public LightingSettings copy()
    {
        return new LightingSettings(this.brightness, this.fixed, this.level, this.truncate);
    }

    public void fromData(BaseType data)
    {
        if (data == null)
        {
            return;
        }

        if (data.isNumeric())
        {
            this.brightness = FormLighting.clampBrightness(data.asNumeric().floatValue());
            this.fixed = false;
            this.level = 15F;
            this.truncate = false;

            return;
        }

        if (!(data instanceof MapType map))
        {
            return;
        }

        this.brightness = map.has("brightness")
            ? FormLighting.clampBrightness(map.getFloat("brightness"))
            : 0F;
        this.fixed = map.getBool("fixed");
        this.level = map.has("level") ? MathUtils.clamp(map.getFloat("level"), 0F, 15F) : 15F;
        this.truncate = map.getBool("truncate");
    }

    public BaseType toData()
    {
        MapType map = new MapType();

        map.putFloat("brightness", this.brightness);
        map.putBool("fixed", this.fixed);
        map.putFloat("level", this.level);
        map.putBool("truncate", this.truncate);

        return map;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof LightingSettings that))
        {
            return false;
        }

        return Float.compare(this.brightness, that.brightness) == 0
            && this.fixed == that.fixed
            && Float.compare(this.level, that.level) == 0
            && this.truncate == that.truncate;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.brightness, this.fixed, this.level, this.truncate);
    }
}
