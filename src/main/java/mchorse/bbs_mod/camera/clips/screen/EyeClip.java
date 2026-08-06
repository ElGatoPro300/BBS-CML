package mchorse.bbs_mod.camera.clips.screen;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EyeClip extends CameraClip
{
    private static final Color DEFAULT_COLOR = Color.rgba(Colors.A100);

    public final KeyframeChannel<Color> color = new KeyframeChannel<>("color", KeyframeFactories.COLOR);
    public final KeyframeChannel<Double> colorOpacity = new KeyframeChannel<>("color_opacity", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> size = new KeyframeChannel<>("size", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> width = new KeyframeChannel<>("width", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> height = new KeyframeChannel<>("height", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> smoothness = new KeyframeChannel<>("smoothness", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> rotation = new KeyframeChannel<>("rotation", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> zoom = new KeyframeChannel<>("zoom", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> offsetX = new KeyframeChannel<>("offsetX", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> offsetY = new KeyframeChannel<>("offsetY", KeyframeFactories.DOUBLE);

    public final KeyframeChannel[] channels;

    private EyeEffect effect = new EyeEffect();

    public static List<EyeEffect> getEffects(ClipContext context)
    {
        return context.clipData.get("eyeEffects", ArrayList::new);
    }

    public EyeClip()
    {
        this.channels = new KeyframeChannel[]
        {
            this.color,
            this.colorOpacity,
            this.width,
            this.height,
            this.smoothness,
            this.rotation,
            this.zoom
        };

        this.add(this.color);
        this.add(this.colorOpacity);
        this.add(this.size);
        this.add(this.width);
        this.add(this.height);
        this.add(this.smoothness);
        this.add(this.rotation);
        this.add(this.zoom);
        this.add(this.offsetX);
        this.add(this.offsetY);

        this.color.insert(0, DEFAULT_COLOR.copy());
        this.colorOpacity.insert(0, 1D);
        this.width.insert(0, 1D);
        this.zoom.insert(0, 1D);
        this.size.insert(0, 1D);
    }

    @Override
    public void fromData(BaseType data)
    {
        Map<String, BaseType> legacy = null;

        if (data != null && data.isMap())
        {
            legacy = this.collectLegacyScalars(data.asMap());
        }

        super.fromData(data);

        if (legacy != null)
        {
            this.migrateLegacy(legacy);
        }
    }

    private Map<String, BaseType> collectLegacyScalars(MapType map)
    {
        Map<String, BaseType> legacy = new HashMap<>();

        for (Map.Entry<String, BaseType> entry : map)
        {
            BaseType value = entry.getValue();

            if (value != null && !value.isList())
            {
                legacy.put(entry.getKey(), value);
            }
        }

        return legacy.isEmpty() ? null : legacy;
    }

    private void migrateLegacy(Map<String, BaseType> legacy)
    {
        this.migrateColor(legacy, "color", this.color, DEFAULT_COLOR);
        this.migrateDouble(legacy, "color_opacity", this.colorOpacity, 1D);
    }

    private void migrateDouble(Map<String, BaseType> legacy, String key, KeyframeChannel<Double> channel, double fallback)
    {
        if (!channel.isEmpty())
        {
            return;
        }

        BaseType data = legacy.get(key);

        if (data != null && data.isNumeric())
        {
            channel.insert(0, data.asNumeric().doubleValue());
        }
        else
        {
            channel.insert(0, fallback);
        }
    }

    private void migrateColor(Map<String, BaseType> legacy, String key, KeyframeChannel<Color> channel, Color fallback)
    {
        if (!channel.isEmpty())
        {
            return;
        }

        BaseType data = legacy.get(key);

        if (data != null && data.isNumeric())
        {
            channel.insert(0, Color.rgba(data.asNumeric().intValue()));
        }
        else
        {
            channel.insert(0, fallback.copy());
        }
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        float t = context.relativeTick + context.transition;
        float factor = this.envelope.factorEnabled(this.duration.get(), t);

        if (factor <= 0F)
        {
            return;
        }

        /* "Blink" drives closedness: 0 = fully open (nothing drawn), 1 = fully closed
           (whole screen covered by the eyelid color). The envelope only fades opacity. */
        float blink = this.height.isEmpty() ? 0F : (float) (double) this.height.interpolate(t);
        blink = MathHelper.clamp(blink, 0F, 1F);

        if (blink <= 0F)
        {
            return;
        }

        float barWidth = this.width.isEmpty() ? 1F : (float) (double) this.width.interpolate(t);
        float smooth = (this.smoothness.isEmpty() ? 0F : (float) (double) this.smoothness.interpolate(t)) * 0.25F;
        float rot = this.rotation.isEmpty() ? 0F : (float) (double) this.rotation.interpolate(t);
        float zm = this.zoom.isEmpty() ? 1F : (float) (double) this.zoom.interpolate(t);
        float offX = this.offsetX.isEmpty() ? 0F : (float) (double) this.offsetX.interpolate(t);
        float offY = this.offsetY.isEmpty() ? 0F : (float) (double) this.offsetY.interpolate(t);
        float alpha = (float) (double) this.interpDouble(this.colorOpacity, t, 1D) * factor;

        this.effect.size = blink;
        this.effect.width = barWidth;
        this.effect.smoothness = smooth;
        this.effect.color = Colors.setA(this.interpColor(this.color, t, DEFAULT_COLOR).getARGBColor(), alpha);
        this.effect.rotation = rot;
        this.effect.zoom = Math.max(0.01F, zm);
        this.effect.offsetX = offX;
        this.effect.offsetY = offY;

        getEffects(context).add(this.effect);
    }

    private Double interpDouble(KeyframeChannel<Double> channel, float t, double fallback)
    {
        if (channel.isEmpty())
        {
            return fallback;
        }

        return channel.interpolate(t);
    }

    private Color interpColor(KeyframeChannel<Color> channel, float t, Color fallback)
    {
        if (channel.isEmpty())
        {
            return fallback;
        }

        return channel.interpolate(t, fallback);
    }

    @Override
    public boolean isPositionClip()
    {
        return false;
    }

    @Override
    protected Clip create()
    {
        return new EyeClip();
    }
}
