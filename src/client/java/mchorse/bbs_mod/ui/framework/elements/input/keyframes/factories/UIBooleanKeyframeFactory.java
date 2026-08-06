package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIBooleanKeyframeFactory extends UIKeyframeFactory<Boolean>
{
    private UIToggle toggle;

    public UIBooleanKeyframeFactory(Keyframe<Boolean> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.toggle = new UIToggle(UIKeys.GENERIC_KEYFRAMES_BOOLEAN_TRUE, (b) -> this.setValue(b.getValue()));
        this.toggle.setValue(keyframe.getValue());

        this.scroll.add(this.toggle);
    }
}