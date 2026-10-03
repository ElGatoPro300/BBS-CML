package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.forms.forms.VideoForm;
import mchorse.bbs_mod.forms.forms.utils.VideoResolution;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIVideoOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Color;

import java.io.File;

public class UIVideoFormPanel extends UIFormPanel<VideoForm>
{
    public UIButton pick;
    public UIToggle billboard;
    public UIToggle linear;
    public UIToggle loop;
    public UIToggle paused;
    public UICirculate resolution;
    public UITrackpad speed;
    public UITrackpad time;
    public UIColor color;

    public UIVideoFormPanel(UIForm editor)
    {
        super(editor);

        this.pick = new UIButton(UIKeys.FORMS_EDITORS_VIDEO_PICK_VIDEO, (b) ->
        {
            UIVideoOverlayPanel panel = new UIVideoOverlayPanel((value) ->
            {
                String next = value == null ? "" : value;

                if (next.equals(UIKeys.GENERAL_NONE.get()) || next.equalsIgnoreCase("none"))
                {
                    next = "";
                }

                this.form.video.set(next);
                this.refreshPickLabel();
            }, this.getContext());

            UIOverlay.addOverlay(this.getContext(), panel.set(this.form.video.get()));
        });
        this.billboard = new UIToggle(UIKeys.FORMS_EDITORS_VIDEO_BILLBOARD, false, (b) -> this.form.billboard.set(b.getValue()));
        this.linear = new UIToggle(UIKeys.TEXTURES_LINEAR, true, (b) -> this.form.linear.set(b.getValue()));
        this.loop = new UIToggle(UIKeys.FORMS_EDITORS_VIDEO_LOOP, true, (b) -> this.form.loop.set(b.getValue()));
        this.paused = new UIToggle(UIKeys.FORMS_EDITORS_VIDEO_PAUSED, false, (b) -> this.form.paused.set(b.getValue()));
        this.paused.tooltip(UIKeys.FORMS_EDITORS_VIDEO_PAUSED_TOOLTIP);
        this.resolution = new UICirculate((b) ->
        {
            this.form.resolution.set(VideoResolution.fromIndex(this.resolution.getValue()));
        });
        this.resolution.addLabel(UIKeys.FORMS_EDITORS_VIDEO_RESOLUTION_NATIVE);
        this.resolution.addLabel(UIKeys.FORMS_EDITORS_VIDEO_RESOLUTION_1080);
        this.resolution.addLabel(UIKeys.FORMS_EDITORS_VIDEO_RESOLUTION_720);
        this.resolution.addLabel(UIKeys.FORMS_EDITORS_VIDEO_RESOLUTION_480);
        this.resolution.addLabel(UIKeys.FORMS_EDITORS_VIDEO_RESOLUTION_360);
        this.resolution.addLabel(UIKeys.FORMS_EDITORS_VIDEO_RESOLUTION_240);
        this.resolution.tooltip(UIKeys.FORMS_EDITORS_VIDEO_RESOLUTION_TOOLTIP);
        this.speed = new UITrackpad((value) -> this.form.speed.set(value.floatValue()));
        this.speed.limit(0.01D, 8D).values(0.25D, 0.05D, 1D);
        this.speed.tooltip(UIKeys.FORMS_EDITORS_VIDEO_SPEED_TOOLTIP);
        this.time = new UITrackpad((value) -> this.form.time.set(value.intValue()));
        this.time.integer().limit(0D, Integer.MAX_VALUE);
        this.time.tooltip(UIKeys.FORMS_EDITORS_VIDEO_TIME_TOOLTIP);

        /* Tint/opacity only — match MobForm Color UI (no paint/glow/grade/transform extras). */
        this.color = new UIColor((c) ->
        {
            Color color = this.form.color.get().copy();
            Color value = Color.rgba(c);

            color.set(value.r, value.g, value.b, value.a);
            this.form.color.set(color);
        }).withAlpha();
        this.color.tooltip(UIKeys.FORMS_EDITORS_BLEND_COLOR);

        this.options.add(
            this.pick,
            this.color,
            this.billboard,
            this.linear,
            this.loop,
            this.paused
        );
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_VIDEO_RESOLUTION).marginTop(8), this.resolution);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_VIDEO_SPEED).marginTop(8), this.speed);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_VIDEO_TIME).marginTop(8), this.time);
    }

    @Override
    public void startEdit(VideoForm form)
    {
        super.startEdit(form);

        this.billboard.setValue(form.billboard.get());
        this.linear.setValue(form.linear.get());
        this.loop.setValue(form.loop.get());
        this.paused.setValue(form.paused.get());
        this.resolution.setValue(VideoResolution.indexOf(form.resolution.get()));
        this.speed.setValue(form.speed.get());
        this.time.setValue(form.time.get());
        this.color.setColor(form.color.get().getARGBColor());
        this.refreshPickLabel();
    }

    private void refreshPickLabel()
    {
        String path = this.form == null ? "" : this.form.video.get();

        if (path == null || path.isEmpty())
        {
            this.pick.label = UIKeys.FORMS_EDITORS_VIDEO_PICK_VIDEO;

            return;
        }

        String name = path;

        if (path.startsWith("external:"))
        {
            name = new File(path.substring("external:".length())).getName();
        }
        else
        {
            int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));

            if (slash >= 0 && slash + 1 < path.length())
            {
                name = path.substring(slash + 1);
            }
        }

        this.pick.label = IKey.constant(name);
    }
}
