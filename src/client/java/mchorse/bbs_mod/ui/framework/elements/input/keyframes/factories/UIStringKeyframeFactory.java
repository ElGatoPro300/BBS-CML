package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIStringKeyframeFactory extends UIKeyframeFactory<String>
{
    private UITextbox string;

    public UIStringKeyframeFactory(Keyframe<String> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.string = new UITextbox(1000, this::setValue);
        this.string.setText(keyframe.getValue());

        this.scroll.add(this.string);
    }
}