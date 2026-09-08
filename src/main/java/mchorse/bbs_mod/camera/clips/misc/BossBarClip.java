package mchorse.bbs_mod.camera.clips.misc;

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

public class BossBarClip extends CameraClip
{
    public static final int PRESET_ENDER_DRAGON = 0xFFFF1493; /* pink */
    public static final int PRESET_WITHER = 0xFF800080; /* purple */
    public static final double DEFAULT_ZOOM = 5D;
    public static final double DEFAULT_TEXT_SIZE = 2D;
    public static final double DEFAULT_HEIGHT = 100D;

    private static final Color DEFAULT_COLOR = Color.rgba(PRESET_ENDER_DRAGON);
    private static final Color DEFAULT_TEXT_COLOR = Color.rgba(0xFFFFFFFF);

    public final KeyframeChannel<String> text = new KeyframeChannel<>("text", KeyframeFactories.STRING);
    public final KeyframeChannel<Double> x = new KeyframeChannel<>("x", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> y = new KeyframeChannel<>("y", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> width = new KeyframeChannel<>("width", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> height = new KeyframeChannel<>("height", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> bossZoom = new KeyframeChannel<>("boss_zoom", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> progress = new KeyframeChannel<>("progress", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Color> color = new KeyframeChannel<>("color", KeyframeFactories.COLOR);
    public final KeyframeChannel<Color> textColor = new KeyframeChannel<>("text_color", KeyframeFactories.COLOR);
    public final KeyframeChannel<Double> textSize = new KeyframeChannel<>("text_size", KeyframeFactories.DOUBLE);

    public final KeyframeChannel[] channels;

    public BossBarClip()
    {
        this.channels = new KeyframeChannel[]
        {
            this.text,
            this.x,
            this.y,
            this.width,
            this.height,
            this.bossZoom,
            this.progress,
            this.color,
            this.textColor,
            this.textSize
        };

        for (KeyframeChannel channel : this.channels)
        {
            this.add(channel);
        }

        this.x.insert(0, 0D);
        this.y.insert(0, 100D);
        this.width.insert(0, 1D);
        this.height.insert(0, DEFAULT_HEIGHT);
        this.bossZoom.insert(0, DEFAULT_ZOOM);
        this.progress.insert(0, 1D);
        this.color.insert(0, DEFAULT_COLOR.copy());
        this.textColor.insert(0, DEFAULT_TEXT_COLOR.copy());
        this.textSize.insert(0, DEFAULT_TEXT_SIZE);
    }

    public static List<BossBarState> getBossBars(ClipContext context)
    {
        return context.clipData.get("boss_bars", ArrayList::new);
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

        if (this.text.isEmpty() && !this.title.get().isEmpty())
        {
            this.text.insert(0, this.title.get());
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
        this.migrateDouble(legacy, "y", this.y, 100D);
        this.migrateDouble(legacy, "width", this.width, 1D);
        this.migrateDouble(legacy, "height", this.height, DEFAULT_HEIGHT);
        this.migrateDouble(legacy, "boss_zoom", this.bossZoom, DEFAULT_ZOOM);
        this.migrateDouble(legacy, "zoom", this.bossZoom, DEFAULT_ZOOM);
        this.migrateDouble(legacy, "progress", this.progress, 1D);
        this.migrateColor(legacy, "color", this.color, DEFAULT_COLOR);
        this.migrateColor(legacy, "text_color", this.textColor, DEFAULT_TEXT_COLOR);
        this.migrateDouble(legacy, "text_size", this.textSize, DEFAULT_TEXT_SIZE);
    }

    private void migrateString(Map<String, BaseType> legacy, String key, KeyframeChannel<String> channel, String fallback)
    {
        if (!channel.isEmpty())
        {
            return;
        }

        BaseType data = legacy.get(key);

        if (data != null && data.isString())
        {
            channel.insert(0, data.asString());
        }
        else if (!fallback.isEmpty())
        {
            channel.insert(0, fallback);
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
        float alpha = this.envelope.factorEnabled(this.duration.get(), t);

        if (alpha <= 0F)
        {
            return;
        }

        BossBarState state = new BossBarState();
        List<BossBarState> bossBars = getBossBars(context);
        float baseY = this.interpDouble(this.y, t, 100D).floatValue();

        state.text = this.interpString(this.text, t, this.title.get());
        state.x = this.interpDouble(this.x, t, 0D).floatValue();
        state.y = baseY + bossBars.size() * 19F;
        state.width = Math.max(0.05F, this.interpDouble(this.width, t, 1D).floatValue());
        state.height = Math.max(0.05F, this.toHeightFactor(this.interpDouble(this.height, t, DEFAULT_HEIGHT)));
        state.zoom = Math.max(0.05F, this.interpDouble(this.bossZoom, t, DEFAULT_ZOOM).floatValue());
        state.progress = MathHelper.clamp(this.interpDouble(this.progress, t, 1D).floatValue(), 0F, 1F);
        state.color = Colors.setA(this.interpColor(this.color, t, DEFAULT_COLOR).getARGBColor(), alpha);
        state.textColor = Colors.setA(this.interpColor(this.textColor, t, DEFAULT_TEXT_COLOR).getARGBColor(), alpha);
        state.textSize = Math.max(0.05F, this.interpDouble(this.textSize, t, DEFAULT_TEXT_SIZE).floatValue());
        state.alpha = alpha;
        state.renderOrder = context.applied;

        getBossBars(context).add(state);
    }

    private String interpString(KeyframeChannel<String> channel, float t, String fallback)
    {
        if (channel.isEmpty())
        {
            return fallback;
        }

        return channel.interpolate(t, fallback);
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

    private float toHeightFactor(double raw)
    {
        return (float) Math.max(0.05D, raw / 100D);
    }

    @Override
    public boolean isPositionClip()
    {
        return false;
    }

    @Override
    protected Clip create()
    {
        return new BossBarClip();
    }
}
