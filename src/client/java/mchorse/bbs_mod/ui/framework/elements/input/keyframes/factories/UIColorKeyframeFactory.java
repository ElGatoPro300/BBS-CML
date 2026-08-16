package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIColorKeyframeFactory extends UIKeyframeFactory<Color>
{
    private UIColor color;

    public UIColorKeyframeFactory(Keyframe<Color> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.color = new UIColor((c) -> this.setValue(Color.rgba(c)));
        this.color.setColor(keyframe.getValue().getARGBColor());
        this.color.withAlpha();

        this.scroll.add(this.color);
    }
}