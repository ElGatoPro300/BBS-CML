package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.forms.forms.utils.LightingSettings;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.utils.UIBezierHandles;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

import java.util.function.Consumer;

/**
 * Form lighting keyframes: blend brightness 0–1, or fixed absolute light level 0–15.
 */
public class UILightingKeyframeFactory extends UIKeyframeFactory<LightingSettings>
{
    private UITrackpad brightness;
    private UIToggle fixed;
    private UITrackpad level;
    private UIToggle truncate;
    private UIBezierHandles handles;

    public UILightingKeyframeFactory(Keyframe<LightingSettings> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.brightness = new UITrackpad((v) -> this.apply((settings) -> settings.brightness = MathUtils.clamp(v.floatValue(), 0F, 1F)));
        this.brightness.limit(0D, 1D);
        this.brightness.tooltip(UIKeys.FILM_REPLAY_TRACK_LIGHTING_VALUE_TOOLTIP);
        this.registerValueTrackpad(this.brightness);

        this.fixed = new UIToggle(UIKeys.FILM_REPLAY_TRACK_LIGHTING_FIXED, (b) ->
        {
            this.applyAndRefresh((settings) -> settings.fixed = b.getValue());
        });
        this.fixed.tooltip(UIKeys.FILM_REPLAY_TRACK_LIGHTING_FIXED_TOOLTIP);

        this.level = new UITrackpad((v) -> this.apply((settings) -> settings.level = MathUtils.clamp(v.floatValue(), 0F, 15F)));
        this.level.limit(0D, 15D);
        this.level.tooltip(UIKeys.FILM_REPLAY_TRACK_LIGHTING_LEVEL_TOOLTIP);
        this.registerValueTrackpad(this.level);

        this.truncate = new UIToggle(UIKeys.FILM_REPLAY_TRACK_LIGHTING_TRUNCATE, (b) ->
        {
            this.applyAndRefresh((settings) ->
            {
                settings.truncate = b.getValue();

                if (settings.truncate)
                {
                    settings.level = Math.round(MathUtils.clamp(settings.level, 0F, 15F));
                }
            });
        });
        this.truncate.tooltip(UIKeys.FILM_REPLAY_TRACK_LIGHTING_TRUNCATE_TOOLTIP);

        this.handles = new UIBezierHandles(keyframe);

        this.scroll.add(this.brightness, this.fixed, this.level, this.truncate, this.handles.createColumn());
        this.update();
    }

    @Override
    public void update()
    {
        super.update();

        LightingSettings value = this.getOrCreate(this.keyframe.getValue());

        this.fixed.setValue(value.fixed);
        this.truncate.setValue(value.truncate);
        this.updateFieldEnabled();
        /* Integer mode must be updated before setValue — otherwise float levels get truncated. */
        this.updateLevelIntegerMode();

        if (!this.brightness.isActivelyEditing() && !this.brightness.isDragging())
        {
            this.brightness.setValue(value.brightness);
        }

        if (!this.level.isActivelyEditing() && !this.level.isDragging())
        {
            this.level.setValue(value.level);
        }

        this.handles.setKeyframe(this.keyframe);
        this.handles.update();
    }

    /**
     * Keep blend and fixed fields always laid out; disable the ones that do not apply
     * to the current mode instead of hiding them (avoids layout jumps).
     */
    private void updateFieldEnabled()
    {
        boolean fixed = this.getOrCreate(this.keyframe.getValue()).fixed;

        this.brightness.setEnabled(!fixed);
        this.level.setEnabled(fixed);
        this.truncate.setEnabled(fixed);
    }

    private void updateLevelIntegerMode()
    {
        LightingSettings value = this.getOrCreate(this.keyframe.getValue());

        this.level.limit(0D, 15D, value.fixed && value.truncate);
    }

    private LightingSettings getOrCreate(LightingSettings settings)
    {
        return settings == null ? new LightingSettings() : settings;
    }

    private void apply(Consumer<LightingSettings> consumer)
    {
        this.rebindKeyframe();

        boolean[] applied = {false};

        UIReplaysEditorUtils.forEachSelectedKeyframe(this.editor, this.keyframe, (selected) ->
        {
            applied[0] = true;

            LightingSettings settings = this.getOrCreate((LightingSettings) selected.getValue()).copy();

            consumer.accept(settings);
            selected.setValue(settings, true);
        });

        if (!applied[0])
        {
            LightingSettings settings = this.getOrCreate(this.keyframe.getValue()).copy();

            consumer.accept(settings);
            this.keyframe.setValue(settings, true);
        }
    }

    private void applyAndRefresh(Consumer<LightingSettings> consumer)
    {
        this.apply(consumer);
        this.rebindKeyframe();
        this.update();
    }
}
