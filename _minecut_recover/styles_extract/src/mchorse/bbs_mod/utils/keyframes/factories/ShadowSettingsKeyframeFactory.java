package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.forms.utils.ShadowSettings;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class ShadowSettingsKeyframeFactory implements IKeyframeFactory<ShadowSettings>
{
    private final ShadowSettings i = new ShadowSettings();

    @Override
    public ShadowSettings fromData(BaseType data)
    {
        ShadowSettings value = new ShadowSettings();

        if (data != null && data.isMap())
        {
            value.fromData(data.asMap());
        }

        return value;
    }

    @Override
    public BaseType toData(ShadowSettings value)
    {
        return value == null ? new MapType() : value.toData();
    }

    @Override
    public ShadowSettings createEmpty()
    {
        return new ShadowSettings();
    }

    @Override
    public ShadowSettings copy(ShadowSettings value)
    {
        return value == null ? null : value.copy();
    }

    @Override
    public ShadowSettings interpolate(Keyframe<ShadowSettings> preA, Keyframe<ShadowSettings> a, Keyframe<ShadowSettings> b, Keyframe<ShadowSettings> postB, IInterp interpolation, float x)
    {
        return this.interpolate(preA.getValue(), a.getValue(), b.getValue(), postB.getValue(), interpolation, x);
    }

    @Override
    public ShadowSettings interpolate(ShadowSettings preA, ShadowSettings a, ShadowSettings b, ShadowSettings postB, IInterp interpolation, float x)
    {
        ShadowSettings preAValue = this.valueOrDefault(preA);
        ShadowSettings aValue = this.valueOrDefault(a);
        ShadowSettings bValue = this.valueOrDefault(b);
        ShadowSettings postBValue = this.valueOrDefault(postB);

        this.i.opacity = (float) interpolation.interpolate(IInterp.context.set(preAValue.opacity, aValue.opacity, bValue.opacity, postBValue.opacity, x));
        this.i.widthX = (float) interpolation.interpolate(IInterp.context.set(preAValue.widthX, aValue.widthX, bValue.widthX, postBValue.widthX, x));
        this.i.widthZ = (float) interpolation.interpolate(IInterp.context.set(preAValue.widthZ, aValue.widthZ, bValue.widthZ, postBValue.widthZ, x));
        this.i.offsetX = (float) interpolation.interpolate(IInterp.context.set(preAValue.offsetX, aValue.offsetX, bValue.offsetX, postBValue.offsetX, x));
        this.i.offsetY = (float) interpolation.interpolate(IInterp.context.set(preAValue.offsetY, aValue.offsetY, bValue.offsetY, postBValue.offsetY, x));
        this.i.offsetZ = (float) interpolation.interpolate(IInterp.context.set(preAValue.offsetZ, aValue.offsetZ, bValue.offsetZ, postBValue.offsetZ, x));

        return this.i;
    }

    private ShadowSettings valueOrDefault(ShadowSettings value)
    {
        return value == null ? new ShadowSettings() : value;
    }
}
