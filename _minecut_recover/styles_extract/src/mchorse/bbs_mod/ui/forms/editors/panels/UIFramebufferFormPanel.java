package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.forms.forms.FramebufferForm;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;

public class UIFramebufferFormPanel extends UIFormPanel<FramebufferForm>
{
    public UITrackpad width;
    public UITrackpad height;
    public UITrackpad scale;

    public UIFramebufferFormPanel(UIForm editor)
    {
        super(editor);

        this.width = new UITrackpad((v) -> this.form.width.set(v.intValue()));
        this.width.limit(2, 4096, true).tooltip(UIKeys.VIDEO_SETTINGS_WIDTH);
        this.height = new UITrackpad((v) -> this.form.height.set(v.intValue()));
        this.height.limit(2, 4096, true).tooltip(UIKeys.VIDEO_SETTINGS_HEIGHT);
        this.scale = new UITrackpad((v) -> this.form.scale.set(v.floatValue()));
        this.scale.tooltip(UIKeys.TRANSFORMS_SCALE);

        this.options.add(UI.label(UIKeys.VIDEO_SETTINGS_RESOLUTION), this.width, this.height, this.scale);
    }

    @Override
    public void startEdit(FramebufferForm form)
    {
        super.startEdit(form);

        this.width.setValue(form.width.get());
        this.height.setValue(form.height.get());
        this.scale.setValue(form.scale.get());
    }
}