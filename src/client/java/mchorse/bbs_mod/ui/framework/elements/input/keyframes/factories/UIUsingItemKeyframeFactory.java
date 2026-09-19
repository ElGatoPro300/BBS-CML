package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.utils.UIBezierHandles;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIUsingItemKeyframeFactory extends UIKeyframeFactory<Double>
{
    private UITrackpad value;
    private UIBezierHandles handles;

    public UIUsingItemKeyframeFactory(Keyframe<Double> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.value = new UITrackpad(this::setValue);
        this.value.limit(0F, 1F).tooltip(UIKeys.FILM_REPLAY_TRACK_USING_ITEM);
        this.value.setValue(keyframe.getValue() == null ? 0D : keyframe.getValue());
        this.handles = new UIBezierHandles(keyframe);
        this.registerValueTrackpad(this.value);

        this.scroll.add(this.value, this.handles.createColumn());
    }

    @Override
    public void update()
    {
        super.update();

        if (!this.value.isActivelyEditing() && !this.value.isDragging())
        {
            Double keyframeValue = this.keyframe.getValue();

            this.value.setValue(keyframeValue == null ? 0D : keyframeValue);
        }

        this.handles.setKeyframe(this.keyframe);
        this.handles.update();
    }
}
