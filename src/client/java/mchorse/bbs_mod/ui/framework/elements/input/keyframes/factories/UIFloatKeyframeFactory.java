package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.framework.elements.input.keyframes.TrackpadRecorder;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIFloatKeyframeFactory extends UINumericKeyframeFactory<Float>
{
    public UIFloatKeyframeFactory(Keyframe<Float> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);
    }

    @Override
    protected double getNumericValue(Float value)
    {
        return value;
    }

    @Override
    protected void setKeyframeValue(double value)
    {
        this.keyframe.setValue((float) value);
    }
    
    @Override
    protected TrackpadRecorder.ValueConverter createValueConverter()
    {
        return (value) -> (float) value;
    }
}
