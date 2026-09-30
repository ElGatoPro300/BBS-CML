package mchorse.bbs_mod.forms.forms.utils;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;

import java.util.Objects;

/**
 * Framebuffer pixel resolution (width/height). Keyframed as one compound track with
 * optional aspect link in the keyframe UI; form storage stays as separate ValueInts.
 */
public class FramebufferResolutionSettings
{
    public int width = 512;
    public int height = 512;

    public FramebufferResolutionSettings()
    {
    }

    public FramebufferResolutionSettings(int width, int height)
    {
        this.width = clamp(width);
        this.height = clamp(height);
    }

    public static FramebufferResolutionSettings of(int width, int height)
    {
        return new FramebufferResolutionSettings(width, height);
    }

    public FramebufferResolutionSettings copy()
    {
        return new FramebufferResolutionSettings(this.width, this.height);
    }

    public void fromData(BaseType data)
    {
        if (!(data instanceof MapType map))
        {
            return;
        }

        if (map.has("width"))
        {
            this.width = clamp(map.getInt("width"));
        }

        if (map.has("height"))
        {
            this.height = clamp(map.getInt("height"));
        }
        else if (map.has("width"))
        {
            this.height = this.width;
        }
    }

    public BaseType toData()
    {
        MapType map = new MapType();

        map.putInt("width", this.width);
        map.putInt("height", this.height);

        return map;
    }

    public static int clamp(int value)
    {
        return Math.max(2, Math.min(4096, value));
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof FramebufferResolutionSettings that))
        {
            return false;
        }

        return this.width == that.width && this.height == that.height;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.width, this.height);
    }
}
