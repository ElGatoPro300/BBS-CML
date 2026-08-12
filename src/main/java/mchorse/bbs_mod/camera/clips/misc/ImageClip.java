package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.forms.utils.TextureBlend;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.settings.values.misc.ValueVector4f;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueDouble;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.KeyframeSegment;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.resources.LinkUtils;

import net.minecraft.util.math.MathHelper;

import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

public class ImageClip extends CameraClip
{
    public static final double BLEND_MIN = 0D;
    public static final double BLEND_MAX = 1D;
    public static final double OPACITY_MIN = 0D;
    public static final double OPACITY_MAX = 1D;

    public ValueLink texture = new ValueLink("texture", null);
    public ValueBoolean linear = new ValueBoolean("linear", false);
    public ValueBoolean mipmap = new ValueBoolean("mipmap", false);
    public ValueVector4f crop = new ValueVector4f("crop", new Vector4f(0, 0, 0, 0));
    public ValueBoolean resizeCrop = new ValueBoolean("resizeCrop", false);
    public ValueLink blendFrom = new ValueLink("blend_from", null);
    public ValueLink blendTo = new ValueLink("blend_to", null);
    public ValueBoolean uniformSize = new ValueBoolean("uniform_size", true);

    public final KeyframeChannel<Link> textureTrack = new KeyframeChannel<>("texture_track", KeyframeFactories.LINK);
    public final KeyframeChannel<Double> offsetX = new KeyframeChannel<>("offsetX", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> offsetY = new KeyframeChannel<>("offsetY", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> rotation = new KeyframeChannel<>("rotation", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> blend = new KeyframeChannel<>("blend", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> x = new KeyframeChannel<>("x", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> y = new KeyframeChannel<>("y", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> width = new KeyframeChannel<>("width", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> height = new KeyframeChannel<>("height", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> anchorX = new KeyframeChannel<>("anchorX", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> anchorY = new KeyframeChannel<>("anchorY", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> windowX = new KeyframeChannel<>("windowX", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> windowY = new KeyframeChannel<>("windowY", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> opacity = new KeyframeChannel<>("opacity", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Color> color = new KeyframeChannel<>("color", KeyframeFactories.COLOR);
    public final ValueBoolean useKeyframes = new ValueBoolean("use_keyframes", false);
    public final ValueBoolean uniformSeeded = new ValueBoolean("uniform_seeded", false);
    public final ImageUniform uniform = new ImageUniform("uniform");

    public final KeyframeChannel[] channels;

    private static final Color DEFAULT_COLOR = Color.white();

    private ImageOverlay overlay = new ImageOverlay();

    public static List<ImageOverlay> getImages(ClipContext context)
    {
        return context.clipData.get("images", ArrayList::new);
    }

    public ImageClip()
    {
        /* Order mirrors SubtitleClip for shared tracks so COLORS[i] matches:
         * x/y, size≈width, anchorX/Y, color, textShadow≈opacity, windowX/Y. */
        this.channels = new KeyframeChannel[]
        {
            this.textureTrack,
            this.x,
            this.y,
            this.width,
            this.anchorX,
            this.anchorY,
            this.color,
            this.opacity,
            this.windowX,
            this.windowY,
            this.height,
            this.offsetX,
            this.offsetY,
            this.rotation,
            this.blend
        };

        this.add(this.texture);
        this.add(this.linear);
        this.add(this.mipmap);
        this.add(this.crop);
        this.add(this.resizeCrop);
        this.add(this.blendFrom);
        this.add(this.blendTo);
        this.add(this.uniformSize);
        this.add(this.textureTrack);
        this.add(this.offsetX);
        this.add(this.offsetY);
        this.add(this.rotation);
        this.add(this.blend);
        this.add(this.x);
        this.add(this.y);
        this.add(this.width);
        this.add(this.height);
        this.add(this.anchorX);
        this.add(this.anchorY);
        this.add(this.windowX);
        this.add(this.windowY);
        this.add(this.opacity);
        this.add(this.color);
        this.add(this.useKeyframes);
        this.add(this.uniformSeeded);
        this.add(this.uniform);
    }

    @Override
    public void fromData(BaseType data)
    {
        BaseType legacyColor = null;
        boolean hasUseKeyframes = data != null && data.isMap() && data.asMap().has("use_keyframes");

        if (data != null && data.isMap())
        {
            MapType map = data.asMap();
            BaseType colorData = map.get("color");

            /* Old clips stored a plain ValueColor (int/map), not a keyframe channel. */
            if (colorData != null && !this.isKeyframeChannelData(colorData))
            {
                legacyColor = colorData;
            }
        }

        super.fromData(data);

        /* Older films did not store this flag — keep keyframe mode enabled for compatibility. */
        if (!hasUseKeyframes)
        {
            this.useKeyframes.set(true);
        }

        if (legacyColor != null && this.color.isEmpty())
        {
            Color migrated = KeyframeFactories.COLOR.fromData(legacyColor);

            if (migrated != null)
            {
                this.uniform.color.set(migrated.copy());

                if (this.useKeyframes.get())
                {
                    this.color.insert(0, migrated.copy());
                }
            }
        }

        this.clampLimitedValues();
    }

    /**
     * Clamp blend/opacity keyframes and uniforms to valid ranges. Out-of-range
     * values from older films snap to the nearest bound.
     */
    public void clampLimitedValues()
    {
        this.clampChannel(this.blend, BLEND_MIN, BLEND_MAX);
        this.clampChannel(this.opacity, OPACITY_MIN, OPACITY_MAX);
        this.uniform.blend.set(MathHelper.clamp(this.uniform.blend.get(), BLEND_MIN, BLEND_MAX));
        this.uniform.opacity.set(MathHelper.clamp(this.uniform.opacity.get(), OPACITY_MIN, OPACITY_MAX));
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

    private boolean isKeyframeChannelData(BaseType data)
    {
        return data.isMap() && data.asMap().has("keyframes");
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        float t = context.relativeTick + context.transition;
        Link link = this.getTextureAt(t);

        if (link == null)
        {
            return;
        }

        List<ImageOverlay> images = getImages(context);
        float factor = this.envelope.factorEnabled(this.duration.get(), t);
        float alpha = factor * (float) MathHelper.clamp(this.valueDouble(this.opacity, this.uniform.opacity, t, 1D), OPACITY_MIN, OPACITY_MAX);

        if (alpha <= 0F)
        {
            return;
        }

        Color tinted = this.valueColor(this.color, this.uniform.color, t, DEFAULT_COLOR).copy();
        tinted.a *= alpha;

        TextureBlend textureBlend = this.getTextureBlend(t);

        this.overlay.updateTexture(
            link,
            this.linear.get(),
            this.mipmap.get(),
            this.resizeCrop.get(),
            this.crop.get(),
            tinted,
            (float) this.valueDouble(this.offsetX, this.uniform.offsetX, t, 0D),
            (float) this.valueDouble(this.offsetY, this.uniform.offsetY, t, 0D),
            (float) this.valueDouble(this.rotation, this.uniform.rotation, t, 0D),
            textureBlend
        );
        this.overlay.updateLayout(
            (float) this.valueDouble(this.x, this.uniform.x, t, 0D),
            (float) this.valueDouble(this.y, this.uniform.y, t, 0D),
            (float) this.valueDouble(this.width, this.uniform.width, t, 100D),
            (float) this.valueDouble(this.height, this.uniform.height, t, 100D),
            (float) this.valueDouble(this.anchorX, this.uniform.anchorX, t, 0.5D),
            (float) this.valueDouble(this.anchorY, this.uniform.anchorY, t, 0.5D),
            (float) this.valueDouble(this.windowX, this.uniform.windowX, t, 0.5D),
            (float) this.valueDouble(this.windowY, this.uniform.windowY, t, 0.5D),
            alpha
        );
        this.overlay.renderOrder = context.applied;
        images.add(this.overlay);
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

        this.uniform.offsetX.set(this.interp(this.offsetX, tick, 0D));
        this.uniform.offsetY.set(this.interp(this.offsetY, tick, 0D));
        this.uniform.rotation.set(this.interp(this.rotation, tick, 0D));
        this.uniform.blend.set(MathHelper.clamp(this.interp(this.blend, tick, 0D), BLEND_MIN, BLEND_MAX));
        this.uniform.x.set(this.interp(this.x, tick, 0D));
        this.uniform.y.set(this.interp(this.y, tick, 0D));
        this.uniform.width.set(this.interp(this.width, tick, 100D));
        this.uniform.height.set(this.interp(this.height, tick, 100D));
        this.uniform.anchorX.set(this.interp(this.anchorX, tick, 0.5D));
        this.uniform.anchorY.set(this.interp(this.anchorY, tick, 0.5D));
        this.uniform.windowX.set(this.interp(this.windowX, tick, 0.5D));
        this.uniform.windowY.set(this.interp(this.windowY, tick, 0.5D));
        this.uniform.opacity.set(MathHelper.clamp(this.interp(this.opacity, tick, 1D), OPACITY_MIN, OPACITY_MAX));
        this.uniform.color.set(this.interpColor(this.color, tick, DEFAULT_COLOR).copy());
        this.uniformSeeded.set(true);
    }

    /**
     * When enabling keyframe mode, fill any empty channels from uniform values
     * so scrubbing/playback can interpolate. Existing keyframes are preserved.
     */
    public void ensureChannelsSeeded(float tick)
    {
        this.ensureUniformSeeded(tick);

        this.seedDouble(this.offsetX, this.uniform.offsetX.get());
        this.seedDouble(this.offsetY, this.uniform.offsetY.get());
        this.seedDouble(this.rotation, this.uniform.rotation.get());
        this.seedDouble(this.blend, MathHelper.clamp(this.uniform.blend.get(), BLEND_MIN, BLEND_MAX));
        this.seedDouble(this.x, this.uniform.x.get());
        this.seedDouble(this.y, this.uniform.y.get());
        this.seedDouble(this.width, this.uniform.width.get());
        this.seedDouble(this.height, this.uniform.height.get());
        this.seedDouble(this.anchorX, this.uniform.anchorX.get());
        this.seedDouble(this.anchorY, this.uniform.anchorY.get());
        this.seedDouble(this.windowX, this.uniform.windowX.get());
        this.seedDouble(this.windowY, this.uniform.windowY.get());
        this.seedDouble(this.opacity, MathHelper.clamp(this.uniform.opacity.get(), OPACITY_MIN, OPACITY_MAX));
        this.seedColor(this.color, this.uniform.color.get());
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

    private Link getTextureAt(float t)
    {
        if (this.useKeyframes.get() && !this.textureTrack.isEmpty())
        {
            return this.textureTrack.interpolate(t);
        }

        return this.texture.get();
    }

    private TextureBlend getTextureBlend(float t)
    {
        float blendValue = (float) MathHelper.clamp(this.valueDouble(this.blend, this.uniform.blend, t, 0D), BLEND_MIN, BLEND_MAX);

        if (blendValue > 0F)
        {
            return new TextureBlend(this.blendFrom.get(), this.blendTo.get(), blendValue);
        }

        if (!this.useKeyframes.get())
        {
            return null;
        }

        KeyframeSegment<Link> segment = this.textureTrack.find(t);

        if (segment != null && segment.a.isBend() && !segment.isSame())
        {
            float blendFactor = (float) segment.a.getInterpolation().interpolate(0D, 1D, segment.x);
            Link from = LinkUtils.copy(segment.a.getValue());
            Link to = LinkUtils.copy(segment.b.getValue());

            return new TextureBlend(from, to, blendFactor);
        }

        return null;
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

        return this.interp(channel, t, fallback);
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

    private double interp(KeyframeChannel<Double> channel, float t, double fallback)
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
        return new ImageClip();
    }
}
