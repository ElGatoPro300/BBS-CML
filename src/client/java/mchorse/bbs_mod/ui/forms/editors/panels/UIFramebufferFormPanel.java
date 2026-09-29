package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.forms.forms.FramebufferForm;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

public class UIFramebufferFormPanel extends UIFormPanel<FramebufferForm>
{
    public UITrackpad width;
    public UITrackpad height;
    public UIIcon resolutionLink;
    public UITrackpad scale;
    public UITrackpad viewExtentX;
    public UITrackpad viewExtentY;
    public UIIcon viewExtentLink;
    public UIToggle lookAt;

    private boolean linkResolution = true;
    private boolean linkViewExtents = true;
    private double resolutionAspect = 1D;

    public UIFramebufferFormPanel(UIForm editor)
    {
        super(editor);

        this.width = new UITrackpad((v) -> this.setResolutionWidth(v.intValue()));
        this.width.limit(2, 4096, true).tooltip(UIKeys.VIDEO_SETTINGS_WIDTH);
        this.height = new UITrackpad((v) -> this.setResolutionHeight(v.intValue()));
        this.height.limit(2, 4096, true).tooltip(UIKeys.VIDEO_SETTINGS_HEIGHT);

        this.resolutionLink = new UIIcon(Icons.LINK, (b) -> this.toggleResolutionLink());
        this.resolutionLink.tooltip(UIKeys.FORMS_EDITORS_FRAMEBUFFER_RESOLUTION_LINK);
        this.resolutionLink.iconColor(Colors.GRAY).activeColor(Colors.A100 + Colors.ACTIVE);
        this.resolutionLink.active(this.linkResolution);

        this.scale = new UITrackpad((v) -> this.form.scale.set(v.floatValue()));
        this.scale.tooltip(UIKeys.TRANSFORMS_SCALE);

        this.viewExtentX = new UITrackpad((v) -> this.setViewExtentX(v.floatValue()));
        this.viewExtentX.limit(0.01D);
        this.viewExtentX.tooltip(UIKeys.FORMS_EDITORS_FRAMEBUFFER_VIEW_EXTENT_X);
        this.viewExtentY = new UITrackpad((v) -> this.setViewExtentY(v.floatValue()));
        this.viewExtentY.limit(0.01D);
        this.viewExtentY.tooltip(UIKeys.FORMS_EDITORS_FRAMEBUFFER_VIEW_EXTENT_Y);

        this.viewExtentLink = new UIIcon(Icons.LINK, (b) -> this.toggleViewExtentLink());
        this.viewExtentLink.tooltip(UIKeys.FORMS_EDITORS_FRAMEBUFFER_VIEW_EXTENT_LINK);
        this.viewExtentLink.iconColor(Colors.GRAY).activeColor(Colors.A100 + Colors.ACTIVE);
        this.viewExtentLink.active(this.linkViewExtents);

        this.lookAt = new UIToggle(UIKeys.FORMS_EDITORS_FRAMEBUFFER_LOOK_AT, false, (b) -> this.form.lookAt.set(b.getValue()));
        this.lookAt.tooltip(UIKeys.FORMS_EDITORS_FRAMEBUFFER_LOOK_AT_TOOLTIP);

        this.options.add(
            UI.label(UIKeys.VIDEO_SETTINGS_RESOLUTION),
            UI.row(this.width, this.resolutionLink, this.height),
            UI.label(UIKeys.TRANSFORMS_SCALE),
            this.scale,
            UI.label(UIKeys.FORMS_EDITORS_FRAMEBUFFER_VIEW_EXTENT),
            UI.row(this.viewExtentX, this.viewExtentLink, this.viewExtentY),
            this.lookAt
        );
    }

    @Override
    public void startEdit(FramebufferForm form)
    {
        super.startEdit(form);

        this.width.setValue(form.width.get());
        this.height.setValue(form.height.get());
        this.scale.setValue(form.scale.get());
        this.viewExtentX.setValue(form.viewExtentX.get());
        this.viewExtentY.setValue(form.viewExtentY.get());
        this.lookAt.setValue(form.lookAt.get());
        this.resolutionAspect = this.aspectFrom(form.width.get(), form.height.get());
        this.resolutionLink.active(this.linkResolution);
        this.viewExtentLink.active(this.linkViewExtents);
    }

    private void setResolutionWidth(int value)
    {
        this.form.width.set(value);

        if (this.linkResolution)
        {
            int linked = this.clampResolution((int) Math.round(value / this.resolutionAspect));

            this.form.height.set(linked);
            this.height.setValue(linked);
        }
        else
        {
            this.resolutionAspect = this.aspectFrom(value, this.form.height.get());
        }
    }

    private void setResolutionHeight(int value)
    {
        this.form.height.set(value);

        if (this.linkResolution)
        {
            int linked = this.clampResolution((int) Math.round(value * this.resolutionAspect));

            this.form.width.set(linked);
            this.width.setValue(linked);
        }
        else
        {
            this.resolutionAspect = this.aspectFrom(this.form.width.get(), value);
        }
    }

    private void toggleResolutionLink()
    {
        this.linkResolution = !this.linkResolution;
        this.resolutionLink.active(this.linkResolution);

        if (this.linkResolution && this.form != null)
        {
            this.resolutionAspect = this.aspectFrom(this.form.width.get(), this.form.height.get());
        }
    }

    private void setViewExtentX(float value)
    {
        this.form.viewExtentX.set(value);

        if (this.linkViewExtents)
        {
            this.form.viewExtentY.set(value);
            this.viewExtentY.setValue(value);
        }
    }

    private void setViewExtentY(float value)
    {
        this.form.viewExtentY.set(value);

        if (this.linkViewExtents)
        {
            this.form.viewExtentX.set(value);
            this.viewExtentX.setValue(value);
        }
    }

    private void toggleViewExtentLink()
    {
        this.linkViewExtents = !this.linkViewExtents;
        this.viewExtentLink.active(this.linkViewExtents);

        if (this.linkViewExtents && this.form != null)
        {
            float x = this.form.viewExtentX.get();

            this.form.viewExtentY.set(x);
            this.viewExtentY.setValue(x);
        }
    }

    private double aspectFrom(int width, int height)
    {
        return height <= 0 ? 1D : width / (double) height;
    }

    private int clampResolution(int value)
    {
        return Math.max(2, Math.min(4096, value));
    }
}
