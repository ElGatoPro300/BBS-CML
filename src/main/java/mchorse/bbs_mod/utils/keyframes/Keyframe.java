package mchorse.bbs_mod.utils.keyframes;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.base.BaseValueGroup;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.interps.Interpolation;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Keyframe <T> extends BaseValueGroup
{
    private float tick;
    private T value;

    public float lx = 5;
    public float ly;
    public float rx = 5;
    public float ry;

    private KeyframeShape shape = KeyframeShape.SQUARE;
    private Color color;

    /**
     * Forced duration that would be used instead of the difference
     * between two keyframes, if not 0
     */
    private float duration;
    private boolean bend;
    private final Interpolation interp = new Interpolation("interp", Interpolations.MAP);

    /**
     * When true, color/glow keyframes interpolate through the full hue spectrum
     * (long path on the color picker bar) instead of direct RGB blending.
     */
    private boolean spectrum;

    /**
     * Color keyframes only: use the clean deferred Iris opacity path for this form
     * at the keyframe's opacity (keeps RGB; does not affect other models).
     */
    private boolean noshadingOpacity;

    private final IKeyframeFactory<T> factory;

    public Keyframe(String id, IKeyframeFactory<T> factory, float tick, T value)
    {
        this(id, factory);

        this.tick = tick;
        this.value = value;
    }

    public Keyframe(String id, IKeyframeFactory<T> factory)
    {
        super(id);

        this.factory = factory;
        this.interp.setParent(this);
    }

    public IKeyframeFactory<T> getFactory()
    {
        return this.factory;
    }

    public float getTick()
    {
        return this.tick;
    }

    public void setTick(float tick)
    {
        this.setTick(tick, false);
    }

    public void setTick(float tick, boolean dirty)
    {
        if (dirty) this.preNotify();

        this.tick = tick;

        if (dirty) this.postNotify();
    }

    public float getDuration()
    {
        return this.duration;
    }

    public void setDuration(float duration)
    {
        this.preNotify();
        this.duration = Math.max(0, duration);
        this.postNotify();
    }

    public boolean isBend()
    {
        return this.bend;
    }

    public void setBend(boolean bend)
    {
        this.preNotify();
        this.bend = bend;
        this.postNotify();
    }

    public T getValue()
    {
        return this.value;
    }

    public double getY(int index)
    {
        return this.factory.getY(this.value);
    }

    public void setValue(T value)
    {
        this.setValue(value, false);
    }

    public void setValue(T value, boolean dirty)
    {
        if (dirty) this.preNotify();

        this.value = value;

        if (dirty) this.postNotify();
    }

    public Interpolation getInterpolation()
    {
        return this.interp;
    }

    public boolean isSpectrum()
    {
        return this.spectrum;
    }

    public void setSpectrum(boolean spectrum)
    {
        this.preNotify();
        this.spectrum = spectrum;
        this.postNotify();
    }

    public boolean isNoshadingOpacity()
    {
        return this.noshadingOpacity;
    }

    public void setNoshadingOpacity(boolean noshadingOpacity)
    {
        this.preNotify();
        this.noshadingOpacity = noshadingOpacity;
        this.postNotify();
    }

    @Override
    public List<BaseValue> getAll()
    {
        return Collections.singletonList(this.interp);
    }

    @Override
    public BaseValue get(String key)
    {
        if (key.equals("interp"))
        {
            return this.interp;
        }

        return null;
    }

    @Override
    public void copy(BaseValueGroup group)
    {
        if (group instanceof Keyframe kf)
        {
            this.copy(kf);
        }
    }

    public KeyframeShape getShape()
    {
        return this.shape;
    }

    public void setShape(KeyframeShape shape)
    {
        this.preNotify();
        this.shape = shape;
        this.postNotify();
    }

    public Color getColor()
    {
        return this.color;
    }

    public void setColor(Color color)
    {
        this.preNotify();
        this.color = color;
        this.postNotify();
    }

    public void copy(Keyframe<T> keyframe)
    {
        this.tick = keyframe.tick;
        this.duration = keyframe.duration;
        this.value = this.factory.copy(keyframe.value);
        this.interp.copy(keyframe.interp);
        this.lx = keyframe.lx;
        this.ly = keyframe.ly;
        this.rx = keyframe.rx;
        this.ry = keyframe.ry;
        this.shape = keyframe.shape;
        this.color = keyframe.color;
        this.bend = keyframe.bend;
        this.spectrum = keyframe.spectrum;
        this.noshadingOpacity = keyframe.noshadingOpacity;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (super.equals(obj))
        {
            return true;
        }

        if (obj instanceof Keyframe<?> kf)
        {
            return this.tick == kf.tick
                && Objects.equals(this.value, kf.value)
                && this.lx == kf.lx
                && this.ly == kf.ly
                && this.rx == kf.rx
                && this.ry == kf.ry
                && this.duration == kf.duration
                && this.bend == kf.bend
                && Objects.equals(this.interp, kf.interp);
        }

        return false;
    }

    @Override
    public BaseType toData()
    {
        MapType data = new MapType();

        data.putFloat("tick", this.tick);
        data.put("value", this.factory.toData(this.value));

        if (this.duration != 0F) data.putFloat("duration", this.duration);
        data.put("interp", this.interp.toData());
        if (this.lx != 5F) data.putFloat("lx", this.lx);
        if (this.ly != 0F) data.putFloat("ly", this.ly);
        if (this.rx != 5F) data.putFloat("rx", this.rx);
        if (this.ry != 0F) data.putFloat("ry", this.ry);
        if (this.color != null) data.putInt("color", this.color.getRGBColor());
        if (this.shape != KeyframeShape.SQUARE) data.putString("shape", this.shape.toString().toUpperCase());
        if (this.bend) data.putBool("bend", true);
        if (this.spectrum) data.putBool("spectrum", true);
        if (this.noshadingOpacity) data.putBool("noshading_opacity", true);

        return data;
    }

    @Override
    public void fromData(BaseType data)
    {
        if (!data.isMap())
        {
            return;
        }

        MapType map = data.asMap();

        this.shape = KeyframeShape.SQUARE;
        this.color = null;
        this.bend = false;
        this.spectrum = false;
        this.noshadingOpacity = false;

        if (map.has("tick")) this.tick = map.getFloat("tick");
        if (map.has("duration")) this.duration = map.getFloat("duration");
        /* value_bbs keeps Color Grade / blend_a across save_as_compatible Int flattening. */
        if (map.has("value_bbs")) this.value = this.factory.fromData(map.get("value_bbs"));
        else if (map.has("value")) this.value = this.factory.fromData(map.get("value"));
        if (map.has("interp")) this.interp.fromData(map.get("interp"));
        if (map.has("lx")) this.lx = map.getFloat("lx");
        if (map.has("ly")) this.ly = map.getFloat("ly");
        if (map.has("rx")) this.rx = map.getFloat("rx");
        if (map.has("ry")) this.ry = map.getFloat("ry");
        if (map.has("shape")) this.shape = KeyframeShape.fromString(map.getString("shape"));
        if (map.has("color")) this.color = Color.rgb(map.getInt("color"));
        if (map.has("bend")) this.bend = map.getBool("bend");
        if (map.has("spectrum")) this.spectrum = map.getBool("spectrum");
        if (map.has("noshading_opacity")) this.noshadingOpacity = map.getBool("noshading_opacity");
    }

    public void copyOverExtra(Keyframe<T> a)
    {
        this.getInterpolation().copy(a.getInterpolation());
        this.setShape(a.getShape());
        this.setColor(a.getColor());
        this.setBend(a.isBend());
        this.spectrum = a.spectrum;
        this.noshadingOpacity = a.noshadingOpacity;
    }
}