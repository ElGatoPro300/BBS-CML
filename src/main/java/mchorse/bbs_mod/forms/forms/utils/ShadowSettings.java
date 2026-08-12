package mchorse.bbs_mod.forms.forms.utils;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;

import java.util.Objects;

/**
 * Per-replay shadow appearance: opacity, ground-plane width (X/Z), and world offset.
 */
public class ShadowSettings
{
    public float opacity = 1F;
    public float widthX = 0.5F;
    public float widthZ = 0.5F;
    public float offsetX;
    public float offsetY;
    public float offsetZ;

    public ShadowSettings()
    {
    }

    public ShadowSettings(float opacity, float widthX, float widthZ)
    {
        this.opacity = opacity;
        this.widthX = widthX;
        this.widthZ = widthZ;
    }

    public ShadowSettings copy()
    {
        ShadowSettings copy = new ShadowSettings(this.opacity, this.widthX, this.widthZ);

        copy.offsetX = this.offsetX;
        copy.offsetY = this.offsetY;
        copy.offsetZ = this.offsetZ;

        return copy;
    }

    public void fromData(BaseType data)
    {
        if (!(data instanceof MapType map))
        {
            return;
        }

        this.opacity = map.has("opacity") ? map.getFloat("opacity") : 1F;
        this.widthX = map.has("widthX") ? map.getFloat("widthX") : (map.has("size") ? map.getFloat("size") : 0.5F);
        this.widthZ = map.has("widthZ") ? map.getFloat("widthZ") : this.widthX;
        this.offsetX = map.getFloat("offsetX");
        this.offsetY = map.getFloat("offsetY");
        this.offsetZ = map.getFloat("offsetZ");
    }

    public BaseType toData()
    {
        MapType map = new MapType();

        map.putFloat("opacity", this.opacity);
        map.putFloat("widthX", this.widthX);
        map.putFloat("widthZ", this.widthZ);
        map.putFloat("offsetX", this.offsetX);
        map.putFloat("offsetY", this.offsetY);
        map.putFloat("offsetZ", this.offsetZ);

        return map;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof ShadowSettings that))
        {
            return false;
        }

        return Float.compare(this.opacity, that.opacity) == 0
            && Float.compare(this.widthX, that.widthX) == 0
            && Float.compare(this.widthZ, that.widthZ) == 0
            && Float.compare(this.offsetX, that.offsetX) == 0
            && Float.compare(this.offsetY, that.offsetY) == 0
            && Float.compare(this.offsetZ, that.offsetZ) == 0;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.opacity, this.widthX, this.widthZ, this.offsetX, this.offsetY, this.offsetZ);
    }
}
