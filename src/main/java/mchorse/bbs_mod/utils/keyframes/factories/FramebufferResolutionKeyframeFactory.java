package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.forms.utils.FramebufferResolutionSettings;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class FramebufferResolutionKeyframeFactory implements IKeyframeFactory<FramebufferResolutionSettings>
{
    private final FramebufferResolutionSettings i = new FramebufferResolutionSettings();

    @Override
    public FramebufferResolutionSettings fromData(BaseType data)
    {
        FramebufferResolutionSettings value = new FramebufferResolutionSettings();

        value.fromData(data);

        return value;
    }

    @Override
    public BaseType toData(FramebufferResolutionSettings value)
    {
        return value == null ? new MapType() : value.toData();
    }

    @Override
    public FramebufferResolutionSettings createEmpty()
    {
        return new FramebufferResolutionSettings();
    }

    @Override
    public FramebufferResolutionSettings copy(FramebufferResolutionSettings value)
    {
        return value == null ? null : value.copy();
    }

    @Override
    public FramebufferResolutionSettings interpolate(Keyframe<FramebufferResolutionSettings> preA, Keyframe<FramebufferResolutionSettings> a, Keyframe<FramebufferResolutionSettings> b, Keyframe<FramebufferResolutionSettings> postB, IInterp interpolation, float x)
    {
        return this.interpolate(preA.getValue(), a.getValue(), b.getValue(), postB.getValue(), interpolation, x);
    }

    @Override
    public FramebufferResolutionSettings interpolate(FramebufferResolutionSettings preA, FramebufferResolutionSettings a, FramebufferResolutionSettings b, FramebufferResolutionSettings postB, IInterp interpolation, float x)
    {
        FramebufferResolutionSettings preAValue = this.valueOrDefault(preA);
        FramebufferResolutionSettings aValue = this.valueOrDefault(a);
        FramebufferResolutionSettings bValue = this.valueOrDefault(b);
        FramebufferResolutionSettings postBValue = this.valueOrDefault(postB);

        this.i.width = FramebufferResolutionSettings.clamp((int) Math.round(
            interpolation.interpolate(IInterp.context.set(preAValue.width, aValue.width, bValue.width, postBValue.width, x))));
        this.i.height = FramebufferResolutionSettings.clamp((int) Math.round(
            interpolation.interpolate(IInterp.context.set(preAValue.height, aValue.height, bValue.height, postBValue.height, x))));

        return this.i;
    }

    @Override
    public double getY(FramebufferResolutionSettings value)
    {
        return value == null ? 512D : value.width;
    }

    private FramebufferResolutionSettings valueOrDefault(FramebufferResolutionSettings value)
    {
        return value == null ? new FramebufferResolutionSettings() : value;
    }
}
