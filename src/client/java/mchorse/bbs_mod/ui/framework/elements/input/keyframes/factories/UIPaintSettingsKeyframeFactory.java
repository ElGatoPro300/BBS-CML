package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIFormColorLayout;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.events.UITrackpadDragEndEvent;
import mchorse.bbs_mod.ui.framework.elements.events.UITrackpadDragStartEvent;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UIEffectTransformCollapse;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

import java.util.function.Consumer;

public class UIPaintSettingsKeyframeFactory extends UIKeyframeFactory<PaintSettings>
{
    private UIEffectTransformCollapse paintTransform;
    private UIColor paintColor;
    private UITrackpad intensity;
    private UIToggle spectrum;

    public UIPaintSettingsKeyframeFactory(Keyframe<PaintSettings> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.paintTransform = new UIEffectTransformCollapse((apply) -> this.applyPaintEdit((settings) ->
        {
            if (settings.transform == null)
            {
                settings.transform = new EffectTransform();
            }

            apply.accept(settings.transform);
        }));
        this.paintTransform.registerUndo(editor);

        this.paintColor = new UIColor((c) -> this.applyPaintEdit((settings) -> this.setPaintColor(settings, c)));
        this.paintColor.tooltip(UIKeys.FORMS_EDITORS_PAINT_COLOR);

        this.intensity = new UITrackpad((value) -> this.applyPaintEdit((settings) -> this.setPaintIntensity(settings, value.floatValue())));
        this.intensity.increment(0.05D).values(0.1D, 0.05D, 0.2D).limit(PaintSettings.MIN_INTENSITY, PaintSettings.MAX_INTENSITY);
        this.intensity.tooltip(UIKeys.FORMS_EDITORS_PAINT_INTENSITY);
        this.wireUndo(this.intensity);

        this.spectrum = new UIToggle(UIKeys.GENERIC_KEYFRAMES_COLOR_SPECTRUM, (b) -> this.setSpectrum(b.getValue()));
        this.spectrum.tooltip(UIKeys.GENERIC_KEYFRAMES_COLOR_SPECTRUM_TOOLTIP);
        this.spectrum.setValue(keyframe.isSpectrum());

        this.scroll.add(UIFormColorLayout.paintColorRowWithTransform(this.paintColor, this.intensity, this.paintTransform));
        this.scroll.add(this.spectrum.marginTop(8));

        this.update();
    }

    private void wireUndo(UITrackpad trackpad)
    {
        trackpad.getEvents().register(UITrackpadDragStartEvent.class, (e) -> this.editor.cacheKeyframes());
        trackpad.getEvents().register(UITrackpadDragEndEvent.class, (e) -> this.editor.submitKeyframes());
    }

    @Override
    public void update()
    {
        super.update();

        PaintSettings value = this.getOrCreateSettings(this.keyframe.getValue());
        EffectTransform effect = value.transform == null ? new EffectTransform() : value.transform;

        this.paintTransform.setEffectTransform(effect);
        this.paintColor.setColor(new Color().set(value.r, value.g, value.b, 1F).getRGBColor());
        this.intensity.setValue(value.intensity);
        this.spectrum.setValue(this.keyframe.isSpectrum());
    }

    private PaintSettings getOrCreateSettings(PaintSettings settings)
    {
        if (settings == null)
        {
            return new PaintSettings();
        }

        if (settings.transform == null)
        {
            settings.transform = new EffectTransform();
        }

        return settings;
    }

    private void applyPaintEdit(Consumer<PaintSettings> editor)
    {
        boolean[] applied = {false};

        UIReplaysEditorUtils.forEachSelectedKeyframe(this.editor, this.keyframe, (selected) ->
        {
            applied[0] = true;

            PaintSettings settings = this.getOrCreateSettings((PaintSettings) selected.getValue());

            selected.preNotify();
            editor.accept(settings);
            settings.applyAutoShaderShadow();
            selected.postNotify();
        });

        if (!applied[0])
        {
            PaintSettings settings = this.getOrCreateSettings(this.keyframe.getValue());

            this.keyframe.preNotify();
            editor.accept(settings);
            settings.applyAutoShaderShadow();
            this.keyframe.postNotify();
        }
    }

    private void setPaintColor(PaintSettings settings, int c)
    {
        Color color = new Color().set(c);

        settings.r = color.r;
        settings.g = color.g;
        settings.b = color.b;
    }

    private void setPaintIntensity(PaintSettings settings, float value)
    {
        settings.intensity = PaintSettings.clampIntensity(value);
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
