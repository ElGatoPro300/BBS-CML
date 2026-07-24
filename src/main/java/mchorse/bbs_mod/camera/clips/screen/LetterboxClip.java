package mchorse.bbs_mod.camera.clips.screen;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueDouble;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LetterboxClip extends CameraClip
{
    private static final Color DEFAULT_COLOR = Color.rgba(Colors.A100);
    private static final double DEFAULT_HEIGHT = 0.4D;
    private static final double DEFAULT_WIDTH = 1D;
    private static final double DEFAULT_ZOOM = 1D;

    public final KeyframeChannel<Color> color = new KeyframeChannel<>("color", KeyframeFactories.COLOR);
    public final KeyframeChannel<Double> size = new KeyframeChannel<>("size", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> width = new KeyframeChannel<>("width", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> height = new KeyframeChannel<>("height", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> smoothness = new KeyframeChannel<>("smoothness", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> rotation = new KeyframeChannel<>("rotation", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> zoom = new KeyframeChannel<>("zoom", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> offsetX = new KeyframeChannel<>("offsetX", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> offsetY = new KeyframeChannel<>("offsetY", KeyframeFactories.DOUBLE);

    public final ValueBoolean useKeyframes = new ValueBoolean("use_keyframes", false);
    public final ValueBoolean uniformSeeded = new ValueBoolean("uniform_seeded", false);
    public final LetterboxUniform uniform = new LetterboxUniform("uniform");

    public final KeyframeChannel[] channels;

    private LetterboxEffect effect = new LetterboxEffect();

    public static List<LetterboxEffect> getEffects(ClipContext context)
    {
        return context.clipData.get("letterboxEffects", ArrayList::new);
    }

    public LetterboxClip()
    {
        this.channels = new KeyframeChannel[]
        {
            this.color,
            this.width,
            this.height,
            this.smoothness,
            this.rotation,
            this.zoom,
            this.offsetX,
            this.offsetY
        };

        this.add(this.color);
        this.add(this.size);
        this.add(this.width);
        this.add(this.height);
        this.add(this.smoothness);
        this.add(this.rotation);
        this.add(this.zoom);
        this.add(this.offsetX);
        this.add(this.offsetY);
        this.add(this.useKeyframes);
        this.add(this.uniformSeeded);
        this.add(this.uniform);
    }

    @Override
    public void fromData(BaseType data)
    {
        Map<String, BaseType> legacy = null;
        boolean hasUseKeyframes = data != null && data.isMap() && data.asMap().has("use_keyframes");

        if (data != null && data.isMap())
        {
            legacy = this.collectLegacyScalars(data.asMap());
        }

        super.fromData(data);

        /* Older films did not store this flag — keep keyframe mode enabled for compatibility. */
        if (!hasUseKeyframes)
        {
            this.useKeyframes.set(true);
        }

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
            Color migrated = Color.rgba(data.asNumeric().intValue());

            this.uniform.color.set(migrated.copy());

            if (this.useKeyframes.get())
            {
                channel.insert(0, migrated.copy());
            }
        }
        else if (this.useKeyframes.get())
        {
            channel.insert(0, fallback.copy());
        }
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        float t = context.relativeTick + context.transition;
        float factor = this.envelope.factorEnabled(this.duration.get(), t);

        float thickness = this.resolveHeight(t);
        float sz = thickness * 0.25F;

        if (sz > 0F)
        {
            float barWidth = (float) this.valueDouble(this.width, this.uniform.width, t, DEFAULT_WIDTH);
            float smooth = (float) this.valueDouble(this.smoothness, this.uniform.smoothness, t, 0D) * 0.25F;
            float rot = (float) this.valueDouble(this.rotation, this.uniform.rotation, t, 0D);
            float zm = (float) this.valueDouble(this.zoom, this.uniform.zoom, t, DEFAULT_ZOOM);
            float offX = (float) this.valueDouble(this.offsetX, this.uniform.offsetX, t, 0D);
            float offY = (float) this.valueDouble(this.offsetY, this.uniform.offsetY, t, 0D);

            this.effect.size = Math.max(0F, sz * factor);
            this.effect.width = barWidth;
            this.effect.smoothness = smooth;
            /* Keep alpha from the letterbox color (opacity in the color picker). */
            this.effect.color = this.valueColor(this.color, this.uniform.color, t, DEFAULT_COLOR).getARGBColor();
            this.effect.rotation = rot;
            this.effect.zoom = Math.max(0.01F, zm);
            this.effect.offsetX = offX;
            this.effect.offsetY = offY;

            getEffects(context).add(this.effect);
        }
    }

    private float resolveHeight(float t)
    {
        if (!this.useKeyframes.get())
        {
            return (float) (double) this.uniform.height.get();
        }

        if (!this.height.isEmpty())
        {
            return (float) (double) this.height.interpolate(t);
        }

        /* Legacy films used the size channel before height existed. */
        if (!this.size.isEmpty())
        {
            return (float) (double) this.size.interpolate(t);
        }

        return this.uniformSeeded.get() ? (float) (double) this.uniform.height.get() : (float) DEFAULT_HEIGHT;
    }

    /**
     * Copy the current keyframed values into uniform storage the first time
     * keyframe mode is disabled, without modifying the keyframe channels.
     */
    public void ensureUniformSeeded(float tick)
    {
        if (this.uniformSeeded.get())
        {
            return;
        }

        this.uniform.color.set(this.interpColor(this.color, tick, DEFAULT_COLOR).copy());
        this.uniform.width.set(this.interpDouble(this.width, tick, DEFAULT_WIDTH));
        this.uniform.height.set(this.resolveHeightForSeed(tick));
        this.uniform.smoothness.set(this.interpDouble(this.smoothness, tick, 0D));
        this.uniform.rotation.set(this.interpDouble(this.rotation, tick, 0D));
        this.uniform.zoom.set(this.interpDouble(this.zoom, tick, DEFAULT_ZOOM));
        this.uniform.offsetX.set(this.interpDouble(this.offsetX, tick, 0D));
        this.uniform.offsetY.set(this.interpDouble(this.offsetY, tick, 0D));
        this.uniformSeeded.set(true);
    }

    /**
     * When enabling keyframe mode, fill any empty channels from uniform values
     * so scrubbing/playback can interpolate. Existing keyframes are preserved.
     */
    public void ensureChannelsSeeded(float tick)
    {
        this.ensureUniformSeeded(tick);

        this.seedColor(this.color, this.uniform.color.get());
        this.seedDouble(this.width, this.uniform.width.get());
        this.seedDouble(this.height, this.uniform.height.get());
        this.seedDouble(this.smoothness, this.uniform.smoothness.get());
        this.seedDouble(this.rotation, this.uniform.rotation.get());
        this.seedDouble(this.zoom, this.uniform.zoom.get());
        this.seedDouble(this.offsetX, this.uniform.offsetX.get());
        this.seedDouble(this.offsetY, this.uniform.offsetY.get());
    }

    private double resolveHeightForSeed(float tick)
    {
        if (!this.height.isEmpty())
        {
            return this.height.interpolate(tick);
        }

        if (!this.size.isEmpty())
        {
            return this.size.interpolate(tick);
        }

        return DEFAULT_HEIGHT;
    }

    private void seedDouble(KeyframeChannel<Double> channel, double value)
    {
        if (channel.isEmpty())
        {
            channel.insert(0, value);
        }
    }

    private void seedColor(KeyframeChannel<Color> channel, Color value)
    {
        if (channel.isEmpty())
        {
            channel.insert(0, value.copy());
        }
    }

    private double valueDouble(KeyframeChannel<Double> channel, ValueDouble uniform, float t, double fallback)
    {
        if (!this.useKeyframes.get())
        {
            return uniform.get();
        }

        if (channel.isEmpty())
        {
            return this.uniformSeeded.get() ? uniform.get() : fallback;
        }

        return this.interpDouble(channel, t, fallback);
    }

    private Color valueColor(KeyframeChannel<Color> channel, ValueColor uniform, float t, Color fallback)
    {
        if (!this.useKeyframes.get())
        {
            return uniform.get();
        }

        if (channel.isEmpty())
        {
            return this.uniformSeeded.get() ? uniform.get() : fallback;
        }

        return this.interpColor(channel, t, fallback);
    }

    private double interpDouble(KeyframeChannel<Double> channel, float t, double fallback)
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
        return new LetterboxClip();
    }
}
