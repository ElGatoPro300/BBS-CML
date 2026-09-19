package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
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
    private float preservedW;
    private boolean compactLayout;

    public UIVector4fKeyframeFactory(Keyframe<Vector4f> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        Vector4f value = keyframe.getValue();
        this.compactLayout = this.isHotbarLayout(editor, keyframe);
        this.preservedW = value.w;

        this.x = new UITrackpad((v) -> this.setValue(this.getValue()));
        this.x.setValue(value.x);
        this.y = new UITrackpad((v) -> this.setValue(this.getValue()));
        this.y.setValue(value.y);
        this.z = new UITrackpad((v) -> this.setValue(this.getValue()));
        this.z.setValue(value.z);
        this.x.tooltip(UIKeys.C_CLIP.get("bbs:x"));
        this.y.tooltip(UIKeys.C_CLIP.get("bbs:y"));
        this.z.tooltip(UIKeys.C_CLIP.get("bbs:scale"));

        if (this.compactLayout)
        {
            this.scroll.add(
                UI.row(UI.label(UIKeys.C_CLIP.get("bbs:x")), UI.label(UIKeys.C_CLIP.get("bbs:y"))),
                UI.row(this.x, this.y),
                UI.label(UIKeys.C_CLIP.get("bbs:scale")),
                this.z
            );
        }
        else
        {
            this.w = new UITrackpad((v) -> this.setValue(this.getValue()));
            this.w.setValue(value.w);

            this.scroll.add(
                UI.row(UI.label(IKey.constant("X")), UI.label(IKey.constant("Y"))),
                UI.row(this.x, this.y),
                UI.row(UI.label(IKey.constant("Z")), UI.label(IKey.constant("W"))),
                UI.row(this.z, this.w)
            );
        }
    }

    private Vector4f getValue()
    {
        return new Vector4f(
            (float) this.x.getValue(), (float) this.y.getValue(),
            (float) this.z.getValue(), this.compactLayout ? this.preservedW : (float) this.w.getValue()
        );
    }

    private boolean isHotbarLayout(UIKeyframes editor, Keyframe<Vector4f> keyframe)
    {
        UIKeyframeSheet sheet = editor.getGraph().getSheet(keyframe);

        return sheet != null && "layout".equals(sheet.id);
    }
}
