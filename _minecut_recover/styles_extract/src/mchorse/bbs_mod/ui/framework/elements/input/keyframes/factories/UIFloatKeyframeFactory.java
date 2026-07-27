package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.utils.UIBezierHandles;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIFloatKeyframeFactory extends UIKeyframeFactory<Float>
{
    private UITrackpad value;
    private UIBezierHandles handles;

    private UIToggle renderDepthEnabled;

    public UIFloatKeyframeFactory(Keyframe<Float> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.value = new UITrackpad(this::setValue);
        this.value.setValue(keyframe.getValue());
        this.handles = new UIBezierHandles(keyframe);
        this.registerValueTrackpad(this.value);

        Form renderDepthForm = this.getRenderDepthForm();

        if (renderDepthForm != null)
        {
            this.renderDepthEnabled = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_RENDER_DEPTH, (b) -> renderDepthForm.renderDepthEnabled.set(b.getValue()));
            this.renderDepthEnabled.setValue(renderDepthForm.renderDepthEnabled.get());
            this.renderDepthEnabled.tooltip(UIKeys.FORMS_EDITORS_GENERAL_RENDER_DEPTH_TOOLTIP);

            this.scroll.add(this.renderDepthEnabled);
        }

        this.scroll.add(this.value, this.handles.createColumn());
    }

    /** Returns the form owning this track's property when this is a render depth track. */
    private Form getRenderDepthForm()
    {
        for (UIKeyframeSheet sheet : this.editor.getGraph().getSheets())
        {
            if (sheet.channel != this.keyframe.getParent())
            {
                continue;
            }

            if (sheet.property != null && StringUtils.fileName(sheet.id).equals("render_depth"))
            {
                return FormUtils.getForm(sheet.property);
            }

            break;
        }

        return null;
    }

    @Override
    public void update()
    {
        super.update();

        if (!this.value.isActivelyEditing() && !this.value.isDragging())
        {
            this.value.setValue(this.keyframe.getValue());
        }

        this.handles.setKeyframe(this.keyframe);
        this.handles.update();

        if (this.renderDepthEnabled != null)
        {
            Form form = this.getRenderDepthForm();

            if (form != null)
            {
                this.renderDepthEnabled.setValue(form.renderDepthEnabled.get());
            }
        }
    }
}
