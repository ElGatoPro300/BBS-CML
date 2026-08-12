package mchorse.bbs_mod.forms.forms.utils;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;

import java.util.Objects;

/**
 * Local transform for spatial paint masks. Offset moves the effect volume,
 * scale sizes it, rotation tilts it, and {@link #shape} picks box / circle / triangle.
 */
public class EffectTransform
{
    private static final float EPSILON = 0.001F;

    public float offsetX;
    public float offsetY;
    public float offsetZ;
    public float scaleX = 1F;
    public float scaleY = 1F;
    public float scaleZ = 1F;
    public float rotateX;
    public float rotateY;
    public float rotateZ;
    public PaintMaskShape shape = PaintMaskShape.BOX;

    public EffectTransform()
    {}

    public EffectTransform copy()
    {
        EffectTransform copy = new EffectTransform();

        copy.offsetX = this.offsetX;
        copy.offsetY = this.offsetY;
        copy.offsetZ = this.offsetZ;
        copy.scaleX = this.scaleX;
        copy.scaleY = this.scaleY;
        copy.scaleZ = this.scaleZ;
        copy.rotateX = this.rotateX;
        copy.rotateY = this.rotateY;
        copy.rotateZ = this.rotateZ;
        copy.shape = this.shape;

        return copy;
    }

    public boolean isActive()
    {
        if (this.shape != PaintMaskShape.BOX)
        {
            return true;
        }

        return Math.abs(this.offsetX) > EPSILON
            || Math.abs(this.offsetY) > EPSILON
            || Math.abs(this.offsetZ) > EPSILON
            || Math.abs(this.scaleX - 1F) > EPSILON
            || Math.abs(this.scaleY - 1F) > EPSILON
            || Math.abs(this.scaleZ - 1F) > EPSILON
            || Math.abs(this.rotateX) > EPSILON
            || Math.abs(this.rotateY) > EPSILON
            || Math.abs(this.rotateZ) > EPSILON;
    }

    public void fromData(BaseType data)
    {
        if (!(data instanceof MapType map))
        {
            return;
        }

        this.offsetX = map.getFloat("offsetX");
        this.offsetY = map.getFloat("offsetY");
        this.offsetZ = map.getFloat("offsetZ");
        this.scaleX = map.has("scaleX") ? map.getFloat("scaleX") : 1F;
        this.scaleY = map.has("scaleY") ? map.getFloat("scaleY") : 1F;
        this.scaleZ = map.has("scaleZ") ? map.getFloat("scaleZ") : 1F;
        this.rotateX = map.getFloat("rotateX");
        this.rotateY = map.getFloat("rotateY");
        this.rotateZ = map.getFloat("rotateZ");
        this.shape = map.has("shape") ? PaintMaskShape.fromName(map.getString("shape")) : PaintMaskShape.BOX;
    }

    public BaseType toData()
    {
        MapType map = new MapType();

        map.putFloat("offsetX", this.offsetX);
        map.putFloat("offsetY", this.offsetY);
        map.putFloat("offsetZ", this.offsetZ);
        map.putFloat("scaleX", this.scaleX);
        map.putFloat("scaleY", this.scaleY);
        map.putFloat("scaleZ", this.scaleZ);
        map.putFloat("rotateX", this.rotateX);
        map.putFloat("rotateY", this.rotateY);
        map.putFloat("rotateZ", this.rotateZ);
        map.putString("shape", this.shape.name());

        return map;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof EffectTransform that))
        {
            return false;
        }

        return Float.compare(this.offsetX, that.offsetX) == 0
            && Float.compare(this.offsetY, that.offsetY) == 0
            && Float.compare(this.offsetZ, that.offsetZ) == 0
            && Float.compare(this.scaleX, that.scaleX) == 0
            && Float.compare(this.scaleY, that.scaleY) == 0
            && Float.compare(this.scaleZ, that.scaleZ) == 0
            && Float.compare(this.rotateX, that.rotateX) == 0
            && Float.compare(this.rotateY, that.rotateY) == 0
            && Float.compare(this.rotateZ, that.rotateZ) == 0
            && this.shape == that.shape;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.offsetX, this.offsetY, this.offsetZ, this.scaleX, this.scaleY, this.scaleZ, this.rotateX, this.rotateY, this.rotateZ, this.shape);
    }
}
