package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.forms.forms.utils.FramebufferViewExtentSettings;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

import java.util.function.Consumer;

/**
 * Compound view-extent keyframe UI: X/Y with optional equal-axis link
 * (same idea as the FramebufferForm panel).
 */
public class UIFramebufferViewExtentKeyframeFactory extends UIKeyframeFactory<FramebufferViewExtentSettings>
{
    private static boolean linkPreference = true;

    private UITrackpad extentX;
    private UITrackpad extentY;
    private UIIcon link;
    private boolean linkAxes;

    public UIFramebufferViewExtentKeyframeFactory(Keyframe<FramebufferViewExtentSettings> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.linkAxes = linkPreference;

        this.extentX = new UITrackpad((v) -> this.setX(v.floatValue()));
        this.extentX.limit(0.01D).tooltip(UIKeys.FORMS_EDITORS_FRAMEBUFFER_VIEW_EXTENT_X);
        this.extentX.textbox.setColor(Colors.RED);

        this.extentY = new UITrackpad((v) -> this.setY(v.floatValue()));
        this.extentY.limit(0.01D).tooltip(UIKeys.FORMS_EDITORS_FRAMEBUFFER_VIEW_EXTENT_Y);
        this.extentY.textbox.setColor(Colors.GREEN);

        this.link = new UIIcon(Icons.LINK, (b) -> this.toggleLink());
        this.link.tooltip(UIKeys.FORMS_EDITORS_FRAMEBUFFER_VIEW_EXTENT_LINK);
        this.link.iconColor(Colors.GRAY).activeColor(Colors.A100 + Colors.ACTIVE);
        this.link.active(this.linkAxes);

        this.scroll.add(UI.row(this.extentX, this.link, this.extentY));

        this.update();
    }

    @Override
    public void update()
    {
        super.update();

        FramebufferViewExtentSettings value = this.getOrCreate(this.keyframe.getValue());

        this.extentX.setValue(value.x);
        this.extentY.setValue(value.y);
        this.link.active(this.linkAxes);
    }

    private FramebufferViewExtentSettings getOrCreate(FramebufferViewExtentSettings settings)
    {
        return settings == null ? new FramebufferViewExtentSettings() : settings;
    }

    private void apply(Consumer<FramebufferViewExtentSettings> consumer)
    {
        boolean[] applied = {false};

        UIReplaysEditorUtils.forEachSelectedKeyframe(this.editor, this.keyframe, (selected) ->
        {
            applied[0] = true;

            FramebufferViewExtentSettings settings = this.getOrCreate((FramebufferViewExtentSettings) selected.getValue()).copy();

            consumer.accept(settings);
            selected.setValue(settings, true);
        });

        if (!applied[0])
        {
            FramebufferViewExtentSettings settings = this.getOrCreate(this.keyframe.getValue()).copy();

            consumer.accept(settings);
            this.keyframe.setValue(settings, true);
        }
    }

    private void applyAndRefresh(Consumer<FramebufferViewExtentSettings> consumer)
    {
        this.apply(consumer);
        this.update();
    }

    private void setX(float value)
    {
        this.apply((settings) ->
        {
            settings.x = FramebufferViewExtentSettings.clamp(value);

            if (this.linkAxes)
            {
                settings.y = settings.x;
                this.extentY.setValue(settings.y);
            }
        });
    }

    private void setY(float value)
    {
        this.apply((settings) ->
        {
            settings.y = FramebufferViewExtentSettings.clamp(value);

            if (this.linkAxes)
            {
                settings.x = settings.y;
                this.extentX.setValue(settings.x);
            }
        });
    }

    private void toggleLink()
    {
        this.linkAxes = !this.linkAxes;
        linkPreference = this.linkAxes;
        this.link.active(this.linkAxes);

        if (this.linkAxes)
        {
            float x = (float) this.extentX.getValue();

            this.applyAndRefresh((settings) -> settings.y = x);
        }
    }
}
