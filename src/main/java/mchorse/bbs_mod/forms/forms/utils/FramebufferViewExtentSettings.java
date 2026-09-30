package mchorse.bbs_mod.forms.forms.utils;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;

import java.util.Objects;

/**
 * Framebuffer view-extent multipliers (X/Y). Keyframed as one compound track with
 * optional equal-axis link; form storage stays as separate ValueFloats.
 */
public class FramebufferViewExtentSettings
{
    public float x = 1F;
    public float y = 1F;

    public FramebufferViewExtentSettings()
    {
    }

    public FramebufferViewExtentSettings(float x, float y)
    {
        this.x = clamp(x);
        this.y = clamp(y);
    }

    public static FramebufferViewExtentSettings of(float x, float y)
    {
        return new FramebufferViewExtentSettings(x, y);
    }

    public FramebufferViewExtentSettings copy()
    {
        return new FramebufferViewExtentSettings(this.x, this.y);
    }

    public void fromData(BaseType data)
    {
        if (!(data instanceof MapType map))
        {
            return;
        }

        if (map.has("x"))
        {
            this.x = clamp(map.getFloat("x"));
        }

        this.y = map.has("y") ? clamp(map.getFloat("y")) : this.x;
    }

    public BaseType toData()
    {
        MapType map = new MapType();

        map.putFloat("x", this.x);
        map.putFloat("y", this.y);

        return map;
    }

    public static float clamp(float value)
    {
        return Math.max(0.01F, value);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof FramebufferViewExtentSettings that))
        {
            return false;
        }

        return Float.compare(this.x, that.x) == 0 && Float.compare(this.y, that.y) == 0;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.x, this.y);
    }
}
