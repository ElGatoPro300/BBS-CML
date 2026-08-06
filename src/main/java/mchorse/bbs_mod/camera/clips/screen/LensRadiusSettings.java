package mchorse.bbs_mod.camera.clips.screen;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;

import java.util.Objects;

/**
 * Fisheye lens radius on UV X/Y axes ({@code 1} = legacy coverage to the frame corners
 * along that axis). Equal X/Y reproduces the old circular radius.
 */
public class LensRadiusSettings
{
    public float x = 1F;
    public float y = 1F;

    public LensRadiusSettings()
    {
    }

    public LensRadiusSettings(float x, float y)
    {
        this.x = x;
        this.y = y;
    }

    public static LensRadiusSettings ofUniform(float radius)
    {
        float value = Math.max(0F, radius);

        return new LensRadiusSettings(value, value);
    }

    public LensRadiusSettings copy()
    {
        return new LensRadiusSettings(this.x, this.y);
    }

    public void fromData(BaseType data)
    {
        if (data == null)
        {
            return;
        }

        /* Legacy scalar radius keyframe value. */
        if (data.isNumeric())
        {
            float radius = Math.max(0F, data.asNumeric().floatValue());

            this.x = radius;
            this.y = radius;

            return;
        }

        if (!(data instanceof MapType map))
        {
            return;
        }

        if (map.has("x"))
        {
            this.x = Math.max(0F, map.getFloat("x"));
        }
        else if (map.has("radius"))
        {
            this.x = Math.max(0F, map.getFloat("radius"));
        }

        this.y = map.has("y") ? Math.max(0F, map.getFloat("y")) : this.x;
    }

    public BaseType toData()
    {
        MapType map = new MapType();

        map.putFloat("x", this.x);
        map.putFloat("y", this.y);

        return map;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof LensRadiusSettings that))
        {
            return false;
        }

        return Float.compare(this.x, that.x) == 0
            && Float.compare(this.y, that.y) == 0;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.x, this.y);
    }
}
