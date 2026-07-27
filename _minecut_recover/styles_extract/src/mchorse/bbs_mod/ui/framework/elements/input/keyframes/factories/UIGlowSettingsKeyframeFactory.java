package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

import java.util.function.Consumer;

public class UIGlowSettingsKeyframeFactory extends UIKeyframeFactory<GlowSettings>
{
    private UIColor glowColor;
    private UITrackpad intensity;
    private UIToggle spectrum;

    public UIGlowSettingsKeyframeFactory(Keyframe<GlowSettings> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.glowColor = new UIColor((c) -> this.setColor(c));
        this.glowColor.tooltip(UIKeys.FORMS_EDITORS_GLOWING_COLOR);

        this.intensity = new UITrackpad((value) -> this.setIntensity(value.floatValue()));
        this.intensity.increment(0.05D).values(0.1D, 0.05D, 0.2D);
        this.intensity.tooltip(UIKeys.FORMS_EDITORS_GLOW_INTENSITY);

        this.spectrum = new UIToggle(UIKeys.GENERIC_KEYFRAMES_COLOR_SPECTRUM, (b) -> this.setSpectrum(b.getValue()));
        this.spectrum.tooltip(UIKeys.GENERIC_KEYFRAMES_COLOR_SPECTRUM_TOOLTIP);
        this.spectrum.setValue(keyframe.isSpectrum());

        this.scroll.add(UI.row(UI.label(UIKeys.FORMS_EDITORS_GLOWING_COLOR), this.glowColor));
        this.scroll.add(UI.label(UIKeys.FORMS_EDITORS_GLOW_INTENSITY), this.intensity);
        this.scroll.add(this.spectrum);

        this.update();
    }

    @Override
    public void update()
    {
        super.update();

        GlowSettings value = this.getOrCreateSettings(this.keyframe.getValue());

        this.glowColor.setColor(new Color().set(value.r, value.g, value.b, 1F).getRGBColor());
        this.intensity.setValue(value.intensity);
        this.spectrum.setValue(this.keyframe.isSpectrum());
    }

    private GlowSettings getOrCreateSettings(GlowSettings settings)
    {
        if (settings == null)
        {
            return new GlowSettings();
        }

        return settings;
    }

    private void applyToSelected(Consumer<GlowSettings> consumer)
    {
        boolean[] applied = {false};

        UIReplaysEditorUtils.forEachSelectedKeyframe(this.editor, this.keyframe, (selected) ->
        {
            applied[0] = true;

            GlowSettings settings = this.getOrCreateSettings((GlowSettings) selected.getValue()).copy();

            consumer.accept(settings);
            selected.setValue(settings, true);
        });

        if (!applied[0])
        {
            GlowSettings settings = this.getOrCreateSettings(this.keyframe.getValue()).copy();

            consumer.accept(settings);
            this.keyframe.setValue(settings, true);
        }
    }

    private void setColor(int c)
    {
        Color color = new Color().set(c);

        this.applyToSelected((settings) ->
        {
            settings.r = color.r;
            settings.g = color.g;
            settings.b = color.b;
        });
    }

    private void setIntensity(float value)
    {
        this.applyToSelected((settings) -> settings.intensity = value);
    }

    private void setSpectrum(boolean value)
    {
        boolean[] applied = {false};

        UIReplaysEditorUtils.forEachSelectedKeyframe(this.editor, this.keyframe, (selected) ->
        {
            applied[0] = true;
            selected.setSpectrum(value);
        });

        if (!applied[0])
        {
            this.keyframe.setSpectrum(value);
        }
    }
}
