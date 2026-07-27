package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIRenderKeyframeFactory extends UIKeyframeFactory<Boolean>
{
    private UIToggle enabled;

    public UIRenderKeyframeFactory(Keyframe<Boolean> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.enabled = new UIToggle(UIKeys.FILM_REPLAY_TRACK_RENDER_ENABLED, (b) -> this.setValue(b.getValue()));
        this.enabled.tooltip(UIKeys.FILM_REPLAY_TRACK_RENDER_ENABLED_TOOLTIP);
        this.enabled.setValue(keyframe.getValue() == null || keyframe.getValue());

        this.scroll.add(this.enabled);
    }

    @Override
    public void update()
    {
        super.update();

        Boolean value = this.keyframe.getValue();

        this.enabled.setValue(value == null || value);
    }
}
