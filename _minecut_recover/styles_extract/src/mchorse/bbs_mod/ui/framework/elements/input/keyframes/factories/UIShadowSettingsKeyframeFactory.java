package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.forms.forms.utils.ShadowSettings;
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
 * Keyframe properties for the compound replay {@code shadow} channel:
 * opacity, width X/Y (world X/Z) with link, and offset XYZ.
 */
public class UIShadowSettingsKeyframeFactory extends UIKeyframeFactory<ShadowSettings>
{
    private UITrackpad opacity;
    private UITrackpad widthX;
    private UITrackpad widthY;
    private UIIcon widthLink;
    private UITrackpad offsetX;
    private UITrackpad offsetY;
    private UITrackpad offsetZ;
    private boolean linkWidth = true;

    public UIShadowSettingsKeyframeFactory(Keyframe<ShadowSettings> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.opacity = new UITrackpad((v) -> this.apply((s) -> s.opacity = v.floatValue()));
        this.opacity.limit(0D, 1D).tooltip(UIKeys.FILM_REPLAY_SHADOW_OPACITY);

        this.widthX = new UITrackpad((v) -> this.setWidthX(v.floatValue()));
        this.widthX.limit(0D).tooltip(UIKeys.FILM_REPLAY_SHADOW_SIZE_X);
        this.widthX.textbox.setColor(Colors.RED);

        this.widthY = new UITrackpad((v) -> this.setWidthY(v.floatValue()));
        this.widthY.limit(0D).tooltip(UIKeys.FILM_REPLAY_SHADOW_SIZE_Y);
        this.widthY.textbox.setColor(Colors.GREEN);

        this.widthLink = new UIIcon(Icons.LINK, (b) -> this.toggleWidthLink());
        this.widthLink.tooltip(UIKeys.FILM_REPLAY_SHADOW_SIZE_LINK);
        this.widthLink.iconColor(Colors.GRAY).activeColor(Colors.A100 + Colors.ACTIVE);
        this.widthLink.active(this.linkWidth);

        this.offsetX = new UITrackpad((v) -> this.apply((s) -> s.offsetX = v.floatValue()));
        this.offsetX.tooltip(UIKeys.FILM_REPLAY_SHADOW_OFFSET_X);
        this.offsetX.textbox.setColor(Colors.RED);

        this.offsetY = new UITrackpad((v) -> this.apply((s) -> s.offsetY = v.floatValue()));
        this.offsetY.tooltip(UIKeys.FILM_REPLAY_SHADOW_OFFSET_Y);
        this.offsetY.textbox.setColor(Colors.GREEN);

        this.offsetZ = new UITrackpad((v) -> this.apply((s) -> s.offsetZ = v.floatValue()));
        this.offsetZ.tooltip(UIKeys.FILM_REPLAY_SHADOW_OFFSET_Z);
        this.offsetZ.textbox.setColor(Colors.BLUE);

        this.scroll.add(UI.label(UIKeys.FILM_REPLAY_SHADOW_OPACITY));
        this.scroll.add(this.opacity);
        this.scroll.add(UI.label(UIKeys.FILM_REPLAY_SHADOW_WIDTH).marginTop(4));
        this.scroll.add(UI.row(this.widthX, this.widthLink, this.widthY));
        this.scroll.add(UI.label(UIKeys.FILM_REPLAY_SHADOW_OFFSET).marginTop(4));
        this.scroll.add(UI.row(this.offsetX, this.offsetY, this.offsetZ));

        this.context((menu) ->
        {
            menu.action(Icons.CLOSE, UIKeys.FILM_REPLAY_SHADOW_RESET_ALL, this::resetAll);
            menu.action(Icons.VISIBLE, UIKeys.FILM_REPLAY_SHADOW_RESET_OPACITY, this::resetOpacity);
            menu.action(Icons.SCALE, UIKeys.FILM_REPLAY_SHADOW_RESET_WIDTH, this::resetWidth);
            menu.action(Icons.ALL_DIRECTIONS, UIKeys.FILM_REPLAY_SHADOW_RESET_OFFSET, this::resetOffset);
        });

        this.update();
    }

    @Override
    public void update()
    {
        super.update();

        ShadowSettings value = this.getOrCreate(this.keyframe.getValue());

        this.opacity.setValue(value.opacity);
        this.widthX.setValue(value.widthX);
        this.widthY.setValue(value.widthZ);
        this.offsetX.setValue(value.offsetX);
        this.offsetY.setValue(value.offsetY);
        this.offsetZ.setValue(value.offsetZ);
        this.widthLink.active(this.linkWidth);
    }

    private ShadowSettings getOrCreate(ShadowSettings settings)
    {
        return settings == null ? new ShadowSettings() : settings;
    }

    private void apply(Consumer<ShadowSettings> consumer)
    {
        boolean[] applied = {false};

        UIReplaysEditorUtils.forEachSelectedKeyframe(this.editor, this.keyframe, (selected) ->
        {
            applied[0] = true;

            ShadowSettings settings = this.getOrCreate((ShadowSettings) selected.getValue()).copy();

            consumer.accept(settings);
            selected.setValue(settings, true);
        });

        if (!applied[0])
        {
            ShadowSettings settings = this.getOrCreate(this.keyframe.getValue()).copy();

            consumer.accept(settings);
            this.keyframe.setValue(settings, true);
        }
    }

    private void applyAndRefresh(Consumer<ShadowSettings> consumer)
    {
        this.apply(consumer);
        this.update();
    }

    private void setWidthX(float value)
    {
        this.apply((settings) ->
        {
            settings.widthX = value;

            if (this.linkWidth)
            {
                settings.widthZ = value;
                this.widthY.setValue(value);
            }
        });
    }

    private void setWidthY(float value)
    {
        this.apply((settings) ->
        {
            settings.widthZ = value;

            if (this.linkWidth)
            {
                settings.widthX = value;
                this.widthX.setValue(value);
            }
        });
    }

    private void toggleWidthLink()
    {
        this.linkWidth = !this.linkWidth;
        this.widthLink.active(this.linkWidth);

        if (this.linkWidth)
        {
            float x = (float) this.widthX.getValue();

            this.applyAndRefresh((settings) -> settings.widthZ = x);
        }
    }

    private void resetAll()
    {
        this.applyAndRefresh((settings) ->
        {
            ShadowSettings defaults = new ShadowSettings();

            settings.opacity = defaults.opacity;
            settings.widthX = defaults.widthX;
            settings.widthZ = defaults.widthZ;
            settings.offsetX = defaults.offsetX;
            settings.offsetY = defaults.offsetY;
            settings.offsetZ = defaults.offsetZ;
        });
    }

    private void resetOpacity()
    {
        this.applyAndRefresh((settings) -> settings.opacity = 1F);
    }

    private void resetWidth()
    {
        this.applyAndRefresh((settings) ->
        {
            settings.widthX = 0.5F;
            settings.widthZ = 0.5F;
        });
    }

    private void resetOffset()
    {
        this.applyAndRefresh((settings) ->
        {
            settings.offsetX = 0F;
            settings.offsetY = 0F;
            settings.offsetZ = 0F;
        });
    }
}
