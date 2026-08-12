package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.camera.clips.screen.LensRadiusSettings;
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
 * Keyframe properties for cinematic fisheye {@code lens_radius}:
 * radius X/Y with optional aspect link (same pattern as shadow width).
 */
public class UILensRadiusSettingsKeyframeFactory extends UIKeyframeFactory<LensRadiusSettings>
{
    /**
     * Survives keyframe panel recreation (deselect/reselect). The factory instance
     * is rebuilt each time, so instance fields alone would always reset to linked.
     */
    private static boolean linkRadiusPreference = true;

    private UITrackpad radiusX;
    private UITrackpad radiusY;
    private UIIcon radiusLink;
    private boolean linkRadius;

    public UILensRadiusSettingsKeyframeFactory(Keyframe<LensRadiusSettings> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.linkRadius = linkRadiusPreference;

        this.radiusX = new UITrackpad((v) -> this.setRadiusX(v.floatValue()));
        this.radiusX.limit(0D).tooltip(UIKeys.CAMERA_CLIPS_CHANNEL_FISHEYE_RADIUS_X);
        this.radiusX.textbox.setColor(Colors.RED);

        this.radiusY = new UITrackpad((v) -> this.setRadiusY(v.floatValue()));
        this.radiusY.limit(0D).tooltip(UIKeys.CAMERA_CLIPS_CHANNEL_FISHEYE_RADIUS_Y);
        this.radiusY.textbox.setColor(Colors.GREEN);

        this.radiusLink = new UIIcon(Icons.LINK, (b) -> this.toggleRadiusLink());
        this.radiusLink.tooltip(UIKeys.CAMERA_CLIPS_CHANNEL_FISHEYE_RADIUS_LINK);
        this.radiusLink.iconColor(Colors.GRAY).activeColor(Colors.A100 + Colors.ACTIVE);
        this.radiusLink.active(this.linkRadius);

        this.scroll.add(UI.row(this.radiusX, this.radiusLink, this.radiusY));

        this.update();
    }

    @Override
    public void update()
    {
        super.update();

        LensRadiusSettings value = this.getOrCreate(this.keyframe.getValue());

        this.radiusX.setValue(value.x);
        this.radiusY.setValue(value.y);
        this.radiusLink.active(this.linkRadius);
    }

    private LensRadiusSettings getOrCreate(LensRadiusSettings settings)
    {
        return settings == null ? new LensRadiusSettings() : settings;
    }

    private void apply(Consumer<LensRadiusSettings> consumer)
    {
        boolean[] applied = {false};

        UIReplaysEditorUtils.forEachSelectedKeyframe(this.editor, this.keyframe, (selected) ->
        {
            applied[0] = true;

            LensRadiusSettings settings = this.getOrCreate((LensRadiusSettings) selected.getValue()).copy();

            consumer.accept(settings);
            selected.setValue(settings, true);
        });

        if (!applied[0])
        {
            LensRadiusSettings settings = this.getOrCreate(this.keyframe.getValue()).copy();

            consumer.accept(settings);
            this.keyframe.setValue(settings, true);
        }
    }

    private void applyAndRefresh(Consumer<LensRadiusSettings> consumer)
    {
        this.apply(consumer);
        this.update();
    }

    private void setRadiusX(float value)
    {
        this.apply((settings) ->
        {
            settings.x = value;

            if (this.linkRadius)
            {
                settings.y = value;
                this.radiusY.setValue(value);
            }
        });
    }

    private void setRadiusY(float value)
    {
        this.apply((settings) ->
        {
            settings.y = value;

            if (this.linkRadius)
            {
                settings.x = value;
                this.radiusX.setValue(value);
            }
        });
    }

    private void toggleRadiusLink()
    {
        this.linkRadius = !this.linkRadius;
        linkRadiusPreference = this.linkRadius;
        this.radiusLink.active(this.linkRadius);

        if (this.linkRadius)
        {
            float x = (float) this.radiusX.getValue();

            this.applyAndRefresh((settings) -> settings.y = x);
        }
    }
}
