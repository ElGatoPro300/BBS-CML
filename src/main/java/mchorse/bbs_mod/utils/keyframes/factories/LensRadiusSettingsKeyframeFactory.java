package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.camera.clips.screen.LensRadiusSettings;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class LensRadiusSettingsKeyframeFactory implements IKeyframeFactory<LensRadiusSettings>
{
    private final LensRadiusSettings i = new LensRadiusSettings();

    @Override
    public LensRadiusSettings fromData(BaseType data)
    {
        LensRadiusSettings value = new LensRadiusSettings();

        value.fromData(data);

        return value;
    }

    @Override
    public BaseType toData(LensRadiusSettings value)
    {
        return value == null ? new MapType() : value.toData();
    }

    @Override
    public LensRadiusSettings createEmpty()
    {
        return new LensRadiusSettings();
    }

    @Override
    public LensRadiusSettings copy(LensRadiusSettings value)
    {
        return value == null ? null : value.copy();
    }

    @Override
    public LensRadiusSettings interpolate(Keyframe<LensRadiusSettings> preA, Keyframe<LensRadiusSettings> a, Keyframe<LensRadiusSettings> b, Keyframe<LensRadiusSettings> postB, IInterp interpolation, float x)
    {
        return this.interpolate(preA.getValue(), a.getValue(), b.getValue(), postB.getValue(), interpolation, x);
    }

    @Override
    public LensRadiusSettings interpolate(LensRadiusSettings preA, LensRadiusSettings a, LensRadiusSettings b, LensRadiusSettings postB, IInterp interpolation, float x)
    {
        LensRadiusSettings preAValue = this.valueOrDefault(preA);
        LensRadiusSettings aValue = this.valueOrDefault(a);
        LensRadiusSettings bValue = this.valueOrDefault(b);
        LensRadiusSettings postBValue = this.valueOrDefault(postB);

        this.i.x = (float) interpolation.interpolate(IInterp.context.set(preAValue.x, aValue.x, bValue.x, postBValue.x, x));
        this.i.y = (float) interpolation.interpolate(IInterp.context.set(preAValue.y, aValue.y, bValue.y, postBValue.y, x));

        return this.i;
    }

    @Override
    public double getY(LensRadiusSettings value)
    {
        return value == null ? 1D : value.x;
    }

    private LensRadiusSettings valueOrDefault(LensRadiusSettings value)
    {
        return value == null ? new LensRadiusSettings() : value;
    }
}
