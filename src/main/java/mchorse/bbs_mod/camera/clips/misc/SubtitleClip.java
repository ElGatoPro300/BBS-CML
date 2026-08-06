package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueDouble;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.CompatibleDoubleKeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubtitleClip extends CameraClip
{
    public static final double CONSTRAINT_MIN = 0D;

    public final KeyframeChannel<String> text = new KeyframeChannel<>("text", KeyframeFactories.STRING);
    public final KeyframeChannel<Double> x = new KeyframeChannel<>("x", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> y = new KeyframeChannel<>("y", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> size = new KeyframeChannel<>("size", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> anchorX = new KeyframeChannel<>("anchorX", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> anchorY = new KeyframeChannel<>("anchorY", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Color> color = new KeyframeChannel<>("color", KeyframeFactories.COLOR);
    public final KeyframeChannel<Boolean> textShadow = new KeyframeChannel<>("textShadow", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<Double> windowX = new KeyframeChannel<>("windowX", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> windowY = new KeyframeChannel<>("windowY", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Color> background = new KeyframeChannel<>("background", KeyframeFactories.COLOR);
    public final KeyframeChannel<Double> backgroundOffset = new KeyframeChannel<>("backgroundOffset", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> shadow = new KeyframeChannel<>("shadow", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Boolean> shadowOpaque = new KeyframeChannel<>("shadowOpaque", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<Double> lineHeight = new CompatibleDoubleKeyframeChannel("lineHeight");
    public final KeyframeChannel<Double> maxWidth = new CompatibleDoubleKeyframeChannel("maxWidth");
    public final ValueBoolean useKeyframes = new ValueBoolean("use_keyframes", false);
    public final ValueBoolean uniformSeeded = new ValueBoolean("uniform_seeded", false);
    public final SubtitleUniform uniform = new SubtitleUniform("uniform");

    public final KeyframeChannel[] channels;

    private static final Color DEFAULT_COLOR = Color.white();
    private static final Color DEFAULT_BACKGROUND = new Color().set(0);

    private Subtitle subtitle = new Subtitle();

    public static List<Subtitle> getSubtitles(ClipContext context)
    {
        return context.clipData.get("subtitles", ArrayList::new);
    }

    public SubtitleClip()
    {
        this.channels = new KeyframeChannel[]
        {
            this.text,
            this.x,
            this.y,
            this.size,
            this.anchorX,
            this.anchorY,
            this.color,
            this.textShadow,
            this.windowX,
            this.windowY,
            this.background,
            this.backgroundOffset,
            this.shadow,
            this.shadowOpaque,
            this.lineHeight,
            this.maxWidth
        };

        for (KeyframeChannel channel : this.channels)
        {
            this.add(channel);
        }

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

        /* Older films saved lineHeight/maxWidth as integer channels. Loading them
         * overwrites the factory to INTEGER; without migration, later Double
         * access throws ClassCastException and Clips.fromData drops the whole clip. */
        this.migrateToDoubleChannel(this.lineHeight);
        this.migrateToDoubleChannel(this.maxWidth);

        /* Older films did not store this flag — keep keyframe mode enabled for compatibility. */
        if (!hasUseKeyframes)
        {
            this.useKeyframes.set(true);
        }

        if (legacy != null)
        {
            this.migrateLegacy(legacy);
        }

        if (this.text.isEmpty() && !this.title.get().isEmpty())
        {
            this.text.insert(0, this.title.get());
        }

        this.clampLimitedValues();
    }

    /**
     * Convert a channel that was deserialized with a non-double numeric factory
     * (typically integer from older films) into a proper double channel.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void migrateToDoubleChannel(KeyframeChannel<Double> channel)
    {
        if (channel.getFactory() == KeyframeFactories.DOUBLE)
        {
            return;
        }

        ListType keyframesData = new ListType();
        List keyframes = channel.getKeyframes();

        for (Object object : keyframes)
        {
            Keyframe keyframe = (Keyframe) object;
            BaseType raw = keyframe.toData();

            if (raw == null || !raw.isMap())
            {
                continue;
            }

            MapType data = raw.asMap();
            Object value = keyframe.getValue();
            double number = value instanceof Number ? ((Number) value).doubleValue() : 0D;

            data.putDouble("value", number);
            keyframesData.add(data);
        }

        MapType map = new MapType();

        map.putString("type", "double");
        map.put("keyframes", keyframesData);
        channel.fromData(map);
    }

    /**
     * Clamp lineHeight/maxWidth keyframes and uniforms to valid ranges. Out-of-range
     * values from older films snap to the nearest bound.
     */
    public void clampLimitedValues()
    {
        this.clampChannel(this.lineHeight, CONSTRAINT_MIN, Double.POSITIVE_INFINITY);
        this.clampChannel(this.maxWidth, CONSTRAINT_MIN, Double.POSITIVE_INFINITY);
        this.uniform.lineHeight.set(Math.max(CONSTRAINT_MIN, this.uniform.lineHeight.get()));
        this.uniform.maxWidth.set(Math.max(CONSTRAINT_MIN, this.uniform.maxWidth.get()));
    }

    private void clampChannel(KeyframeChannel<Double> channel, double min, double max)
    {
        for (Keyframe<Double> keyframe : channel.getKeyframes())
        {
            double value = keyframe.getValue();
            double clamped = MathHelper.clamp(value, min, max);

            if (clamped != value)
            {
                keyframe.setValue(clamped);
            }
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
        this.migrateString(legacy, "text", this.text, this.title.get());
        this.migrateDouble(legacy, "x", this.x, 0D);
        this.migrateDouble(legacy, "y", this.y, 0D);
        this.migrateDouble(legacy, "size", this.size, 10D);
        this.migrateDouble(legacy, "anchorX", this.anchorX, 0.5D);
        this.migrateDouble(legacy, "anchorY", this.anchorY, 0.5D);
        this.migrateColor(legacy, "color", this.color, DEFAULT_COLOR);
        this.migrateBoolean(legacy, "textShadow", this.textShadow, true);
        this.migrateDouble(legacy, "windowX", this.windowX, 0.5D);
        this.migrateDouble(legacy, "windowY", this.windowY, 0.5D);
        this.migrateColor(legacy, "background", this.background, DEFAULT_BACKGROUND);
        this.migrateDouble(legacy, "backgroundOffset", this.backgroundOffset, 2D);
        this.migrateDouble(legacy, "shadow", this.shadow, 0D);
        this.migrateBoolean(legacy, "shadowOpaque", this.shadowOpaque, false);
        this.migrateDouble(legacy, "lineHeight", this.lineHeight, 12D);
        this.migrateDouble(legacy, "maxWidth", this.maxWidth, 0D);
    }

    private void migrateString(Map<String, BaseType> legacy, String key, KeyframeChannel<String> channel, String fallback)
    {
        if (!channel.isEmpty())
        {
            return;
        }

        BaseType data = legacy.get(key);

        /* Only convert legacy scalar properties — never invent a single keyframe
         * for empty modern channels (that breaks uniform mode and scrubbing). */
        if (data != null && data.isString())
        {
            channel.insert(0, data.asString());
        }
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
    }

    private void migrateBoolean(Map<String, BaseType> legacy, String key, KeyframeChannel<Boolean> channel, boolean fallback)
    {
        if (!channel.isEmpty())
        {
            return;
        }

        BaseType data = legacy.get(key);

        if (data != null && data.isNumeric())
        {
            channel.insert(0, data.asNumeric().boolValue());
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
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        List<Subtitle> subtitles = getSubtitles(context);
        float t = context.relativeTick + context.transition;
        float factor = this.envelope.factorEnabled(this.duration.get(), t);
        Color tinted = this.valueColor(this.color, this.uniform.color, t, DEFAULT_COLOR).copy();
        int color = Colors.setA(tinted.getARGBColor(), factor * tinted.a);

        this.subtitle.update(
            this.valueString(this.text, this.uniform.text, t, this.title.get()),
            (int) Math.round(this.valueDouble(this.x, this.uniform.x, t, 0D)),
            (int) Math.round(this.valueDouble(this.y, this.uniform.y, t, 0D)),
            (float) this.valueDouble(this.size, this.uniform.size, t, 10D),
            (float) this.valueDouble(this.anchorX, this.uniform.anchorX, t, 0.5D),
            (float) this.valueDouble(this.anchorY, this.uniform.anchorY, t, 0.5D),
            color,
            this.valueBoolean(this.textShadow, this.uniform.textShadow, t, true)
        );
        this.subtitle.updateWindow(
            (float) this.valueDouble(this.windowX, this.uniform.windowX, t, 0.5D),
            (float) this.valueDouble(this.windowY, this.uniform.windowY, t, 0.5D)
        );
        this.subtitle.updateBackground(
            this.valueColor(this.background, this.uniform.background, t, DEFAULT_BACKGROUND).getARGBColor(),
            (float) this.valueDouble(this.backgroundOffset, this.uniform.backgroundOffset, t, 2D),
            (float) this.valueDouble(this.shadow, this.uniform.shadow, t, 0D),
            this.valueBoolean(this.shadowOpaque, this.uniform.shadowOpaque, t, false)
        );
        this.subtitle.updateConstraints(
            (float) Math.max(CONSTRAINT_MIN, this.valueDouble(this.lineHeight, this.uniform.lineHeight, t, 12D)),
            (float) Math.max(CONSTRAINT_MIN, this.valueDouble(this.maxWidth, this.uniform.maxWidth, t, 0D))
        );
        this.subtitle.renderOrder = context.applied;
        subtitles.add(this.subtitle);
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

        this.uniform.text.set(this.interpString(this.text, tick, this.title.get()));
        this.uniform.x.set(this.interpDouble(this.x, tick, 0D));
        this.uniform.y.set(this.interpDouble(this.y, tick, 0D));
        this.uniform.size.set(this.interpDouble(this.size, tick, 10D));
        this.uniform.anchorX.set(this.interpDouble(this.anchorX, tick, 0.5D));
        this.uniform.anchorY.set(this.interpDouble(this.anchorY, tick, 0.5D));
        this.uniform.color.set(this.interpColor(this.color, tick, DEFAULT_COLOR).copy());
        this.uniform.textShadow.set(this.interpBoolean(this.textShadow, tick, true));
        this.uniform.windowX.set(this.interpDouble(this.windowX, tick, 0.5D));
        this.uniform.windowY.set(this.interpDouble(this.windowY, tick, 0.5D));
        this.uniform.background.set(this.interpColor(this.background, tick, DEFAULT_BACKGROUND).copy());
        this.uniform.backgroundOffset.set(this.interpDouble(this.backgroundOffset, tick, 2D));
        this.uniform.shadow.set(this.interpDouble(this.shadow, tick, 0D));
        this.uniform.shadowOpaque.set(this.interpBoolean(this.shadowOpaque, tick, false));
        this.uniform.lineHeight.set(Math.max(CONSTRAINT_MIN, this.interpDouble(this.lineHeight, tick, 12D)));
        this.uniform.maxWidth.set(Math.max(CONSTRAINT_MIN, this.interpDouble(this.maxWidth, tick, 0D)));
        this.uniformSeeded.set(true);
    }

    /**
     * When enabling keyframe mode, fill any empty channels from uniform values
     * so scrubbing/playback can interpolate. Existing keyframes are preserved.
     */
    public void ensureChannelsSeeded(float tick)
    {
        this.ensureUniformSeeded(tick);

        this.seedString(this.text, this.uniform.text.get(), this.title.get());
        this.seedDouble(this.x, this.uniform.x.get());
        this.seedDouble(this.y, this.uniform.y.get());
        this.seedDouble(this.size, this.uniform.size.get());
        this.seedDouble(this.anchorX, this.uniform.anchorX.get());
        this.seedDouble(this.anchorY, this.uniform.anchorY.get());
        this.seedColor(this.color, this.uniform.color.get());
        this.seedBoolean(this.textShadow, this.uniform.textShadow.get());
        this.seedDouble(this.windowX, this.uniform.windowX.get());
        this.seedDouble(this.windowY, this.uniform.windowY.get());
        this.seedColor(this.background, this.uniform.background.get());
        this.seedDouble(this.backgroundOffset, this.uniform.backgroundOffset.get());
        this.seedDouble(this.shadow, this.uniform.shadow.get());
        this.seedBoolean(this.shadowOpaque, this.uniform.shadowOpaque.get());
        this.seedDouble(this.lineHeight, Math.max(CONSTRAINT_MIN, this.uniform.lineHeight.get()));
        this.seedDouble(this.maxWidth, Math.max(CONSTRAINT_MIN, this.uniform.maxWidth.get()));
    }

    private void seedString(KeyframeChannel<String> channel, String value, String fallback)
    {
        if (!channel.isEmpty())
        {
            return;
        }

        String seed = value == null || value.isEmpty() ? fallback : value;

        channel.insert(0, seed == null ? "" : seed);
    }

    private void seedDouble(KeyframeChannel<Double> channel, double value)
    {
        if (channel.isEmpty())
        {
            channel.insert(0, value);
        }
    }

    private void seedBoolean(KeyframeChannel<Boolean> channel, boolean value)
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
            channel.insert(0, value == null ? DEFAULT_COLOR.copy() : value.copy());
        }
    }

    private String valueString(KeyframeChannel<String> channel, ValueString uniform, float t, String fallback)
    {
        if (!this.useKeyframes.get())
        {
            String value = uniform.get();

            return value == null || value.isEmpty() ? fallback : value;
        }

        if (channel.isEmpty())
        {
            String value = uniform.get();

            return value == null || value.isEmpty() ? fallback : value;
        }

        return this.interpString(channel, t, fallback);
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

    private boolean valueBoolean(KeyframeChannel<Boolean> channel, ValueBoolean uniform, float t, boolean fallback)
    {
        if (!this.useKeyframes.get())
        {
            return uniform.get();
        }

        if (channel.isEmpty())
        {
            return this.uniformSeeded.get() ? uniform.get() : fallback;
        }

        return this.interpBoolean(channel, t, fallback);
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

    private String interpString(KeyframeChannel<String> channel, float t, String fallback)
    {
        if (channel.isEmpty())
        {
            return fallback;
        }

        return channel.interpolate(t, fallback);
    }

    private double interpDouble(KeyframeChannel<Double> channel, float t, double fallback)
    {
        if (channel.isEmpty())
        {
            return fallback;
        }

        return channel.interpolate(t);
    }

    private boolean interpBoolean(KeyframeChannel<Boolean> channel, float t, boolean fallback)
    {
        if (channel.isEmpty())
        {
            return fallback;
        }

        return channel.interpolate(t, fallback);
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
        return new SubtitleClip();
    }
}
