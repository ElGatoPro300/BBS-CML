package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.forms.forms.utils.FramebufferResolutionSettings;
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
 * Compound resolution keyframe UI: width/height with optional aspect link
 * (same idea as the FramebufferForm panel and shadow width link).
 */
public class UIFramebufferResolutionKeyframeFactory extends UIKeyframeFactory<FramebufferResolutionSettings>
{
    private static boolean linkPreference = true;

    private UITrackpad width;
    private UITrackpad height;
    private UIIcon link;
    private boolean linkAspect;
    private double aspect = 1D;

    public UIFramebufferResolutionKeyframeFactory(Keyframe<FramebufferResolutionSettings> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.linkAspect = linkPreference;

        this.width = new UITrackpad((v) -> this.setWidth(v.intValue()));
        this.width.limit(2, 4096, true).tooltip(UIKeys.VIDEO_SETTINGS_WIDTH);
        this.width.textbox.setColor(Colors.RED);

        this.height = new UITrackpad((v) -> this.setHeight(v.intValue()));
        this.height.limit(2, 4096, true).tooltip(UIKeys.VIDEO_SETTINGS_HEIGHT);
        this.height.textbox.setColor(Colors.GREEN);

        this.link = new UIIcon(Icons.LINK, (b) -> this.toggleLink());
        this.link.tooltip(UIKeys.FORMS_EDITORS_FRAMEBUFFER_RESOLUTION_LINK);
        this.link.iconColor(Colors.GRAY).activeColor(Colors.A100 + Colors.ACTIVE);
        this.link.active(this.linkAspect);

        this.scroll.add(UI.row(this.width, this.link, this.height));

        this.update();
    }

    @Override
    public void update()
    {
        super.update();

        FramebufferResolutionSettings value = this.getOrCreate(this.keyframe.getValue());

        this.width.setValue(value.width);
        this.height.setValue(value.height);
        this.aspect = this.aspectFrom(value.width, value.height);
        this.link.active(this.linkAspect);
    }

    private FramebufferResolutionSettings getOrCreate(FramebufferResolutionSettings settings)
    {
        return settings == null ? new FramebufferResolutionSettings() : settings;
    }

    private void apply(Consumer<FramebufferResolutionSettings> consumer)
    {
        boolean[] applied = {false};

        UIReplaysEditorUtils.forEachSelectedKeyframe(this.editor, this.keyframe, (selected) ->
        {
            applied[0] = true;

            FramebufferResolutionSettings settings = this.getOrCreate((FramebufferResolutionSettings) selected.getValue()).copy();

            consumer.accept(settings);
            selected.setValue(settings, true);
        });

        if (!applied[0])
        {
            FramebufferResolutionSettings settings = this.getOrCreate(this.keyframe.getValue()).copy();

            consumer.accept(settings);
            this.keyframe.setValue(settings, true);
        }
    }

    private void applyAndRefresh(Consumer<FramebufferResolutionSettings> consumer)
    {
        this.apply(consumer);
        this.update();
    }

    private void setWidth(int value)
    {
        this.apply((settings) ->
        {
            settings.width = FramebufferResolutionSettings.clamp(value);

            if (this.linkAspect)
            {
                settings.height = FramebufferResolutionSettings.clamp((int) Math.round(value / this.aspect));
                this.height.setValue(settings.height);
            }
            else
            {
                this.aspect = this.aspectFrom(settings.width, settings.height);
            }
        });
    }

    private void setHeight(int value)
    {
        this.apply((settings) ->
        {
            settings.height = FramebufferResolutionSettings.clamp(value);

            if (this.linkAspect)
            {
                settings.width = FramebufferResolutionSettings.clamp((int) Math.round(value * this.aspect));
                this.width.setValue(settings.width);
            }
            else
            {
                this.aspect = this.aspectFrom(settings.width, settings.height);
            }
        });
    }

    private void toggleLink()
    {
        this.linkAspect = !this.linkAspect;
        linkPreference = this.linkAspect;
        this.link.active(this.linkAspect);

        if (this.linkAspect)
        {
            this.aspect = this.aspectFrom((int) this.width.getValue(), (int) this.height.getValue());
        }
    }

    private double aspectFrom(int width, int height)
    {
        return height <= 0 ? 1D : width / (double) height;
    }
}
