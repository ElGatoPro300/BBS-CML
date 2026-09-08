package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.forms.utils.FormLighting;
import mchorse.bbs_mod.forms.forms.utils.LightingSettings;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.interps.InterpContext;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.interps.easings.EasingArgs;
import mchorse.bbs_mod.utils.keyframes.BezierUtils;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class LightingSettingsKeyframeFactory implements IKeyframeFactory<LightingSettings>
{
    private final LightingSettings i = new LightingSettings();

    @Override
    public LightingSettings fromData(BaseType data)
    {
        LightingSettings value = new LightingSettings();

        value.fromData(data);

        return value;
    }

    @Override
    public BaseType toData(LightingSettings value)
    {
        return value == null ? new MapType() : value.toData();
    }

    @Override
    public LightingSettings createEmpty()
    {
        return new LightingSettings();
    }

    @Override
    public LightingSettings copy(LightingSettings value)
    {
        return value == null ? null : value.copy();
    }

    @Override
    public LightingSettings interpolate(Keyframe<LightingSettings> preA, Keyframe<LightingSettings> a, Keyframe<LightingSettings> b, Keyframe<LightingSettings> postB, IInterp interpolation, float x)
    {
        LightingSettings preAValue = this.valueOrDefault(preA.getValue());
        LightingSettings aValue = this.valueOrDefault(a.getValue());
        LightingSettings bValue = this.valueOrDefault(b.getValue());
        LightingSettings postBValue = this.valueOrDefault(postB.getValue());

        /* Fixed holds from the left keyframe. Truncate only stays on when both
         * endpoints request it — turning it off on either side keeps float packing. */
        this.i.fixed = aValue.fixed;
        this.i.truncate = aValue.truncate && bValue.truncate;

        if (aValue.fixed)
        {
            this.i.brightness = aValue.brightness;
            /* Always keep a float level here; truncate snaps only when packing light. */
            this.i.level = MathUtils.clamp(this.interpolateAmount(
                preAValue.level, aValue.level, bValue.level, postBValue.level,
                preA, a, b, postB, interpolation, x
            ), 0F, 15F);

            return this.i;
        }

        this.i.level = aValue.level;
        this.i.brightness = FormLighting.clampBrightness(this.interpolateAmount(
            preAValue.brightness, aValue.brightness, bValue.brightness, postBValue.brightness,
            preA, a, b, postB, interpolation, x
        ));

        return this.i;
    }

    @Override
    public LightingSettings interpolate(LightingSettings preA, LightingSettings a, LightingSettings b, LightingSettings postB, IInterp interpolation, float x)
    {
        LightingSettings preAValue = this.valueOrDefault(preA);
        LightingSettings aValue = this.valueOrDefault(a);
        LightingSettings bValue = this.valueOrDefault(b);
        LightingSettings postBValue = this.valueOrDefault(postB);

        this.i.fixed = aValue.fixed;
        this.i.truncate = aValue.truncate && bValue.truncate;

        if (aValue.fixed)
        {
            this.i.brightness = aValue.brightness;
            /* Always keep a float level here; truncate snaps only when packing light. */
            this.i.level = MathUtils.clamp((float) interpolation.interpolate(IInterp.context.set(
                preAValue.level, aValue.level, bValue.level, postBValue.level, x
            )), 0F, 15F);

            return this.i;
        }

        this.i.level = aValue.level;
        this.i.brightness = FormLighting.clampBrightness((float) interpolation.interpolate(IInterp.context.set(
            preAValue.brightness, aValue.brightness, bValue.brightness, postBValue.brightness, x
        )));

        return this.i;
    }

    @Override
    public double getY(LightingSettings value)
    {
        if (value == null)
        {
            return 0D;
        }

        return value.fixed ? value.level : value.brightness;
    }

    @Override
    public Object yToValue(double y)
    {
        /* Graph insert / default Y mapping uses blend brightness. Fixed-mode
         * drags preserve flags in the keyframe graph handlers instead. */
        return LightingSettings.fromBrightness(FormLighting.clampBrightness((float) y));
    }

    /**
     * Apply a graph Y (brightness or light level) while preserving fixed/truncate flags.
     */
    public static LightingSettings applyGraphY(LightingSettings base, double y)
    {
        LightingSettings next = base == null ? new LightingSettings() : base.copy();

        if (next.fixed)
        {
            next.level = MathUtils.clamp((float) y, 0F, 15F);

            if (next.truncate)
            {
                next.level = Math.round(next.level);
            }
        }
        else
        {
            next.brightness = FormLighting.clampBrightness((float) y);
        }

        return next;
    }

    private float interpolateAmount(float preA, float a, float b, float postB, Keyframe<LightingSettings> preAkf, Keyframe<LightingSettings> akf, Keyframe<LightingSettings> bkf, Keyframe<LightingSettings> postBkf, IInterp interpolation, float x)
    {
        if (interpolation.has(Interpolations.BEZIER))
        {
            return (float) BezierUtils.get(a, b, akf.getTick(), bkf.getTick(), akf.rx, akf.ry, bkf.lx, bkf.ly, x);
        }

        InterpContext ctx = IInterp.context.set(preA, a, b, postB, x)
            .setBoundary(preAkf == akf, postBkf == bkf)
            .extra(akf.getInterpolation().getArgs());

        if (interpolation == Interpolations.NURBS)
        {
            EasingArgs args = akf.getInterpolation().getArgs();

            double w0 = args.v3 != 0 ? args.v3 : this.getWeight(preAkf);
            double w1 = args.v1 != 0 ? args.v1 : 1.0;
            double w2 = args.v2 != 0 ? args.v2 : this.getWeight(bkf);
            double w3 = args.v4 != 0 ? args.v4 : this.getWeight(postBkf);

            ctx.weights(w0, w1, w2, w3);
        }

        return (float) interpolation.interpolate(ctx);
    }

    private double getWeight(Keyframe<?> kf)
    {
        if (kf == null)
        {
            return 1.0;
        }

        double w = kf.getInterpolation().getArgs().v1;

        return w == 0 ? 1.0 : w;
    }

    private LightingSettings valueOrDefault(LightingSettings value)
    {
        return value == null ? new LightingSettings() : value;
    }
}
