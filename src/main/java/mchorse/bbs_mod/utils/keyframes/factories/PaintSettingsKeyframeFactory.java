package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class PaintSettingsKeyframeFactory implements IKeyframeFactory<PaintSettings>
{
    private final PaintSettings i = new PaintSettings();
    private final Color hsvPreA = new Color();
    private final Color hsvA = new Color();
    private final Color hsvB = new Color();
    private final Color hsvPostB = new Color();
    private final Color rgb = new Color();

    @Override
    public PaintSettings fromData(BaseType data)
    {
        PaintSettings value = new PaintSettings();

        if (data.isMap())
        {
            value.fromData(data.asMap());
        }

        return value;
    }

    @Override
    public BaseType toData(PaintSettings value)
    {
        return value == null ? new MapType() : value.toData();
    }

    @Override
    public PaintSettings createEmpty()
    {
        return new PaintSettings();
    }

    @Override
    public PaintSettings copy(PaintSettings value)
    {
        return value == null ? null : value.copy();
    }

    @Override
    public PaintSettings interpolate(Keyframe<PaintSettings> preA, Keyframe<PaintSettings> a, Keyframe<PaintSettings> b, Keyframe<PaintSettings> postB, IInterp interpolation, float x)
    {
        PaintSettings preAValue = this.valueOrDefault(preA.getValue());
        PaintSettings aValue = this.valueOrDefault(a.getValue());
        PaintSettings bValue = this.valueOrDefault(b.getValue());
        PaintSettings postBValue = this.valueOrDefault(postB.getValue());

        if (a.isSpectrum())
        {
            this.interpolateColorHSV(preAValue, aValue, bValue, postBValue, interpolation, x);
        }
        else
        {
            this.interpolateColorRGB(preAValue, aValue, bValue, postBValue, interpolation, x);
        }

        this.i.intensity = PaintSettings.clampIntensity((float) interpolation.interpolate(IInterp.context.set(preAValue.intensity, aValue.intensity, bValue.intensity, postBValue.intensity, x)));
        this.i.sync = x >= 0.5F ? bValue.sync : aValue.sync;
        this.i.shaderShadow = PaintSettings.resolveAutoShaderShadow(this.i.intensity);
        EffectTransformInterpolation.interpolate(this.i.transform, preAValue.transform, aValue.transform, bValue.transform, postBValue.transform, interpolation, x);

        return this.i;
    }

    @Override
    public PaintSettings interpolate(PaintSettings preA, PaintSettings a, PaintSettings b, PaintSettings postB, IInterp interpolation, float x)
    {
        PaintSettings preAValue = this.valueOrDefault(preA);
        PaintSettings aValue = this.valueOrDefault(a);
        PaintSettings bValue = this.valueOrDefault(b);
        PaintSettings postBValue = this.valueOrDefault(postB);

        this.interpolateColorRGB(preAValue, aValue, bValue, postBValue, interpolation, x);

        this.i.intensity = PaintSettings.clampIntensity((float) interpolation.interpolate(IInterp.context.set(preAValue.intensity, aValue.intensity, bValue.intensity, postBValue.intensity, x)));
        this.i.sync = x >= 0.5F ? bValue.sync : aValue.sync;
        this.i.shaderShadow = PaintSettings.resolveAutoShaderShadow(this.i.intensity);
        EffectTransformInterpolation.interpolate(this.i.transform, preAValue.transform, aValue.transform, bValue.transform, postBValue.transform, interpolation, x);

        return this.i;
    }

    private PaintSettings valueOrDefault(PaintSettings value)
    {
        return value == null ? new PaintSettings() : value;
    }

    private void interpolateColorRGB(PaintSettings preA, PaintSettings a, PaintSettings b, PaintSettings postB, IInterp interpolation, float x)
    {
        this.i.r = MathUtils.clamp((float) interpolation.interpolate(IInterp.context.set(preA.r, a.r, b.r, postB.r, x)), 0F, 1F);
        this.i.g = MathUtils.clamp((float) interpolation.interpolate(IInterp.context.set(preA.g, a.g, b.g, postB.g, x)), 0F, 1F);
        this.i.b = MathUtils.clamp((float) interpolation.interpolate(IInterp.context.set(preA.b, a.b, b.b, postB.b, x)), 0F, 1F);
    }

    private void interpolateColorHSV(PaintSettings preA, PaintSettings a, PaintSettings b, PaintSettings postB, IInterp interpolation, float x)
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
