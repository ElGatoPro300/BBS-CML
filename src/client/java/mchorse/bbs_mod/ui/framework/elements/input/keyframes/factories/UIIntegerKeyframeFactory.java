package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.framework.elements.input.keyframes.TrackpadRecorder;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIIntegerKeyframeFactory extends UINumericKeyframeFactory<Integer>
{
    public UIIntegerKeyframeFactory(Keyframe<Integer> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);
        this.value.integer();
    }

    @Override
    protected double getNumericValue(Integer value)
    {
        return value;
    }

    @Override
    protected void setKeyframeValue(double value)
    {
        this.keyframe.setValue((int) value);
    }

    @Override
    protected TrackpadRecorder.ValueConverter createValueConverter()
    {
        return (value) -> (int) value;
    }
}
