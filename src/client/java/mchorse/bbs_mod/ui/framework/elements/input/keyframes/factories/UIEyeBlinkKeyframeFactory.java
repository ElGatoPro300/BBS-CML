package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIEyeBlinkKeyframeFactory extends UIDoubleKeyframeFactory
{
    public UIEyeBlinkKeyframeFactory(Keyframe<Double> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.value.limit(0D, 1D).increment(0.01D).values(0.1D, 0.01D, 0.25D);
    }
}
