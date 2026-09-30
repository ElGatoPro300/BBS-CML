package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.forms.utils.FramebufferViewExtentSettings;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class FramebufferViewExtentKeyframeFactory implements IKeyframeFactory<FramebufferViewExtentSettings>
{
    private final FramebufferViewExtentSettings i = new FramebufferViewExtentSettings();

    @Override
    public FramebufferViewExtentSettings fromData(BaseType data)
    {
        FramebufferViewExtentSettings value = new FramebufferViewExtentSettings();

        value.fromData(data);

        return value;
    }

    @Override
    public BaseType toData(FramebufferViewExtentSettings value)
    {
        return value == null ? new MapType() : value.toData();
    }

    @Override
    public FramebufferViewExtentSettings createEmpty()
    {
        return new FramebufferViewExtentSettings();
    }

    @Override
    public FramebufferViewExtentSettings copy(FramebufferViewExtentSettings value)
    {
        return value == null ? null : value.copy();
    }

    @Override
    public FramebufferViewExtentSettings interpolate(Keyframe<FramebufferViewExtentSettings> preA, Keyframe<FramebufferViewExtentSettings> a, Keyframe<FramebufferViewExtentSettings> b, Keyframe<FramebufferViewExtentSettings> postB, IInterp interpolation, float x)
    {
        return this.interpolate(preA.getValue(), a.getValue(), b.getValue(), postB.getValue(), interpolation, x);
    }

    @Override
    public FramebufferViewExtentSettings interpolate(FramebufferViewExtentSettings preA, FramebufferViewExtentSettings a, FramebufferViewExtentSettings b, FramebufferViewExtentSettings postB, IInterp interpolation, float x)
    {
        FramebufferViewExtentSettings preAValue = this.valueOrDefault(preA);
        FramebufferViewExtentSettings aValue = this.valueOrDefault(a);
        FramebufferViewExtentSettings bValue = this.valueOrDefault(b);
        FramebufferViewExtentSettings postBValue = this.valueOrDefault(postB);

        this.i.x = FramebufferViewExtentSettings.clamp((float) interpolation.interpolate(
            IInterp.context.set(preAValue.x, aValue.x, bValue.x, postBValue.x, x)));
        this.i.y = FramebufferViewExtentSettings.clamp((float) interpolation.interpolate(
            IInterp.context.set(preAValue.y, aValue.y, bValue.y, postBValue.y, x)));

        return this.i;
    }

    @Override
    public double getY(FramebufferViewExtentSettings value)
    {
        return value == null ? 1D : value.x;
    }

    private FramebufferViewExtentSettings valueOrDefault(FramebufferViewExtentSettings value)
    {
        return value == null ? new FramebufferViewExtentSettings() : value;
    }
}
