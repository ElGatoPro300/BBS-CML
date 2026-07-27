package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class GlowSettingsKeyframeFactory implements IKeyframeFactory<GlowSettings>
{
    private final GlowSettings i = new GlowSettings();
    private final Color hsvPreA = new Color();
    private final Color hsvA = new Color();
    private final Color hsvB = new Color();
    private final Color hsvPostB = new Color();
    private final Color rgb = new Color();

    @Override
    public GlowSettings fromData(BaseType data)
    {
        GlowSettings value = new GlowSettings();

        if (data.isMap())
        {
            value.fromData(data.asMap());
        }

        return value;
    }

    @Override
    public BaseType toData(GlowSettings value)
    {
        return value == null ? new MapType() : value.toData();
    }

    @Override
    public GlowSettings createEmpty()
    {
        return new GlowSettings();
    }

    @Override
    public GlowSettings copy(GlowSettings value)
    {
        return value == null ? null : value.copy();
    }

    @Override
    public GlowSettings interpolate(Keyframe<GlowSettings> preA, Keyframe<GlowSettings> a, Keyframe<GlowSettings> b, Keyframe<GlowSettings> postB, IInterp interpolation, float x)
    {
        GlowSettings preAValue = this.valueOrDefault(preA.getValue());
        GlowSettings aValue = this.valueOrDefault(a.getValue());
        GlowSettings bValue = this.valueOrDefault(b.getValue());
        GlowSettings postBValue = this.valueOrDefault(postB.getValue());

        if (a.isSpectrum())
        {
            this.interpolateColorHSV(preAValue, aValue, bValue, postBValue, interpolation, x);
        }
        else
        {
            this.interpolateColorRGB(preAValue, aValue, bValue, postBValue, interpolation, x);
        }

        this.i.intensity = (float) interpolation.interpolate(IInterp.context.set(preAValue.intensity, aValue.intensity, bValue.intensity, postBValue.intensity, x));
        this.i.radius = (float) interpolation.interpolate(IInterp.context.set(preAValue.radius, aValue.radius, bValue.radius, postBValue.radius, x));
        this.i.centerX = (float) interpolation.interpolate(IInterp.context.set(preAValue.centerX, aValue.centerX, bValue.centerX, postBValue.centerX, x));
        this.i.centerY = (float) interpolation.interpolate(IInterp.context.set(preAValue.centerY, aValue.centerY, bValue.centerY, postBValue.centerY, x));
        this.i.centerZ = (float) interpolation.interpolate(IInterp.context.set(preAValue.centerZ, aValue.centerZ, bValue.centerZ, postBValue.centerZ, x));
        this.i.width = (float) interpolation.interpolate(IInterp.context.set(preAValue.width, aValue.width, bValue.width, postBValue.width, x));
        this.i.height = (float) interpolation.interpolate(IInterp.context.set(preAValue.height, aValue.height, bValue.height, postBValue.height, x));
        this.i.sync = x >= 0.5F ? bValue.sync : aValue.sync;
        this.i.paintOnly = x >= 0.5F ? bValue.paintOnly : aValue.paintOnly;

        return this.i;
    }

    @Override
    public GlowSettings interpolate(GlowSettings preA, GlowSettings a, GlowSettings b, GlowSettings postB, IInterp interpolation, float x)
    {
        GlowSettings preAValue = this.valueOrDefault(preA);
        GlowSettings aValue = this.valueOrDefault(a);
        GlowSettings bValue = this.valueOrDefault(b);
        GlowSettings postBValue = this.valueOrDefault(postB);

        this.interpolateColorRGB(preAValue, aValue, bValue, postBValue, interpolation, x);

        this.i.intensity = (float) interpolation.interpolate(IInterp.context.set(preAValue.intensity, aValue.intensity, bValue.intensity, postBValue.intensity, x));
        this.i.radius = (float) interpolation.interpolate(IInterp.context.set(preAValue.radius, aValue.radius, bValue.radius, postBValue.radius, x));
        this.i.centerX = (float) interpolation.interpolate(IInterp.context.set(preAValue.centerX, aValue.centerX, bValue.centerX, postBValue.centerX, x));
        this.i.centerY = (float) interpolation.interpolate(IInterp.context.set(preAValue.centerY, aValue.centerY, bValue.centerY, postBValue.centerY, x));
        this.i.centerZ = (float) interpolation.interpolate(IInterp.context.set(preAValue.centerZ, aValue.centerZ, bValue.centerZ, postBValue.centerZ, x));
        this.i.width = (float) interpolation.interpolate(IInterp.context.set(preAValue.width, aValue.width, bValue.width, postBValue.width, x));
        this.i.height = (float) interpolation.interpolate(IInterp.context.set(preAValue.height, aValue.height, bValue.height, postBValue.height, x));
        this.i.sync = x >= 0.5F ? bValue.sync : aValue.sync;
        this.i.paintOnly = x >= 0.5F ? bValue.paintOnly : aValue.paintOnly;

        return this.i;
    }

    private GlowSettings valueOrDefault(GlowSettings value)
    {
        return value == null ? new GlowSettings() : value;
    }

    private void interpolateColorRGB(GlowSettings preA, GlowSettings a, GlowSettings b, GlowSettings postB, IInterp interpolation, float x)
    {
        this.i.r = MathUtils.clamp((float) interpolation.interpolate(IInterp.context.set(preA.r, a.r, b.r, postB.r, x)), 0F, 1F);
        this.i.g = MathUtils.clamp((float) interpolation.interpolate(IInterp.context.set(preA.g, a.g, b.g, postB.g, x)), 0F, 1F);
        this.i.b = MathUtils.clamp((float) interpolation.interpolate(IInterp.context.set(preA.b, a.b, b.b, postB.b, x)), 0F, 1F);
    }

    private void interpolateColorHSV(GlowSettings preA, GlowSettings a, GlowSettings b, GlowSettings postB, IInterp interpolation, float x)
    {
        Colors.RGBtoHSV(this.hsvPreA, preA.r, preA.g, preA.b);
        Colors.RGBtoHSV(this.hsvA, a.r, a.g, a.b);
        Colors.RGBtoHSV(this.hsvB, b.r, b.g, b.b);
        Colors.RGBtoHSV(this.hsvPostB, postB.r, postB.g, postB.b);

        float hueB = Colors.longPathHueTarget(this.hsvA.r, this.hsvB.r);
        float huePostB = Colors.longPathHueTarget(this.hsvB.r, this.hsvPostB.r);
        float hueX = Colors.smootherstep(x);
        float satX = Colors.smootherstep(x);
        float valX = Colors.smootherstep(x);

        float hue = (float) interpolation.interpolate(IInterp.context.set(this.hsvPreA.r, this.hsvA.r, hueB, huePostB, hueX));

        hue %= 1F;

        if (hue < 0F)
        {
            hue += 1F;
        }

        float sat = (float) interpolation.interpolate(IInterp.context.set(this.hsvPreA.g, this.hsvA.g, this.hsvB.g, this.hsvPostB.g, satX));
        float val = (float) interpolation.interpolate(IInterp.context.set(this.hsvPreA.b, this.hsvA.b, this.hsvB.b, this.hsvPostB.b, valX));

        Colors.HSVtoRGB(this.rgb, hue, sat, val);

        this.i.r = this.rgb.r;
        this.i.g = this.rgb.g;
        this.i.b = this.rgb.b;
    }
}
