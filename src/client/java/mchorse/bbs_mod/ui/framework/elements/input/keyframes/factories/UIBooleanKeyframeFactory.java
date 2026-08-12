package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIBooleanKeyframeFactory extends UIKeyframeFactory<Boolean>
{
    private UIToggle toggle;

    public UIBooleanKeyframeFactory(Keyframe<Boolean> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        /* Booleans are step/hold — duration/interp only affect getClosest() mid-segment. */
        this.interp.setVisible(false);
        this.duration.setVisible(false);
        this.sanitizeDiscreteKeyframe(keyframe);

        this.toggle = new UIToggle(UIKeys.GENERIC_KEYFRAMES_BOOLEAN_TRUE, (b) -> this.setValue(b.getValue()));
        this.toggle.setValue(keyframe.getValue());

        this.scroll.add(this.toggle);
    }

    @Override
    public void update()
    {
        super.update();

        this.sanitizeDiscreteKeyframe(this.keyframe);
        this.toggle.setValue(this.keyframe.getValue());
    }

    private void sanitizeDiscreteKeyframe(Keyframe<Boolean> keyframe)
    {
        if (keyframe == null)
        {
            return;
        }

        if (keyframe.getDuration() != 0F)
        {
            keyframe.setDuration(0F);
        }

        if (keyframe.getInterpolation().getInterp() != Interpolations.CONST)
        {
            keyframe.getInterpolation().setInterp(Interpolations.CONST);
        }
    }
}
