package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.camera.clips.misc.ChromaSkyCurveSettings;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.interps.IInterp;

public class ChromaSkyCurveSettingsKeyframeFactory implements IKeyframeFactory<ChromaSkyCurveSettings>
{
    private final ChromaSkyCurveSettings i = new ChromaSkyCurveSettings();

    @Override
    public ChromaSkyCurveSettings fromData(BaseType data)
    {
        ChromaSkyCurveSettings value = new ChromaSkyCurveSettings();

        if (data.isMap())
        {
            value.fromData(data.asMap());
        }

        return value;
    }

    @Override
    public BaseType toData(ChromaSkyCurveSettings value)
    {
        return value == null ? new MapType() : value.toData();
    }

    @Override
    public ChromaSkyCurveSettings createEmpty()
    {
        return new ChromaSkyCurveSettings();
    }

    @Override
    public ChromaSkyCurveSettings copy(ChromaSkyCurveSettings value)
    {
        return value == null ? null : value.copy();
    }

    @Override
    public ChromaSkyCurveSettings interpolate(ChromaSkyCurveSettings preA, ChromaSkyCurveSettings a, ChromaSkyCurveSettings b, ChromaSkyCurveSettings postB, IInterp interpolation, float x)
    {
        this.i.enabled = a.enabled;
        this.i.terrain = a.terrain;
        this.i.clouds = a.clouds;
        this.i.billboard = (float) MathUtils.clamp(interpolation.interpolate(IInterp.context.set(preA.billboard, a.billboard, b.billboard, postB.billboard, x)), 0D, 256D);
        this.i.color.r = MathUtils.clamp((float) interpolation.interpolate(IInterp.context.set(preA.color.r, a.color.r, b.color.r, postB.color.r, x)), 0F, 1F);
        this.i.color.g = MathUtils.clamp((float) interpolation.interpolate(IInterp.context.set(preA.color.g, a.color.g, b.color.g, postB.color.g, x)), 0F, 1F);
        this.i.color.b = MathUtils.clamp((float) interpolation.interpolate(IInterp.context.set(preA.color.b, a.color.b, b.color.b, postB.color.b, x)), 0F, 1F);
        this.i.color.a = 1F;

        return this.i;
    }
}
