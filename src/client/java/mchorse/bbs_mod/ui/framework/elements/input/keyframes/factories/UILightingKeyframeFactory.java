package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.utils.UIBezierHandles;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

/**
 * Form lighting brightness keyframes: {@code 0} = world/natural, {@code 1} = full bright.
 */
public class UILightingKeyframeFactory extends UIKeyframeFactory<Float>
{
    private UITrackpad value;
    private UIBezierHandles handles;

    public UILightingKeyframeFactory(Keyframe<Float> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.value = new UITrackpad((v) -> this.setValue(v.floatValue()));
        this.value.limit(0D, 1D);
        this.value.tooltip(UIKeys.FILM_REPLAY_TRACK_LIGHTING_VALUE_TOOLTIP);
        this.value.setValue(keyframe.getValue() == null ? 0F : keyframe.getValue());
        this.registerValueTrackpad(this.value);

        this.handles = new UIBezierHandles(keyframe);
        this.scroll.add(this.value, this.handles.createColumn());
    }

    @Override
    public void update()
    {
        super.update();

        if (!this.value.isActivelyEditing() && !this.value.isDragging())
        {
            Float amount = this.keyframe.getValue();

            this.value.setValue(amount == null ? 0F : amount);
        }

        this.handles.setKeyframe(this.keyframe);
        this.handles.update();
    }
}
