package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import org.joml.Vector4f;

public class UIVector4fKeyframeFactory extends UIKeyframeFactory<Vector4f>
{
    private UITrackpad x;
    private UITrackpad y;
    private UITrackpad z;
    private UITrackpad w;

    public UIVector4fKeyframeFactory(Keyframe<Vector4f> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        Vector4f value = keyframe.getValue();

        this.x = new UITrackpad((v) -> this.setValue(this.getValue()));
        this.x.setValue(value.x);
        this.y = new UITrackpad((v) -> this.setValue(this.getValue()));
        this.y.setValue(value.y);
        this.z = new UITrackpad((v) -> this.setValue(this.getValue()));
        this.z.setValue(value.z);
        this.w = new UITrackpad((v) -> this.setValue(this.getValue()));
        this.w.setValue(value.w);

        this.scroll.add(UI.row(this.x, this.y), UI.row(this.z, this.w));
    }

    private Vector4f getValue()
    {
        return new Vector4f(
            (float) this.x.getValue(), (float) this.y.getValue(),
            (float) this.z.getValue(), (float) this.w.getValue()
        );
    }
}