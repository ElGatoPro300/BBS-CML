package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.film.replays.FormProperties;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.LabelForm;
import mchorse.bbs_mod.forms.forms.TrailForm;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIFormColorAdjustments;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.events.UITrackpadDragEndEvent;
import mchorse.bbs_mod.ui.framework.elements.events.UITrackpadDragStartEvent;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UIEffectTransformCollapse;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.List;
import java.util.function.Consumer;

/**
 * Film Color track: Blend Color (RGB + intensity + Transform) and Paint Color (RGB + intensity + Transform).
 * Label/text forms only expose Blend Color itself. Trail forms hide Color grade but keep transforms.
 */
public class UIFormColorKeyframeFactory extends UIKeyframeFactory<Color>
{
    private static final UIFormColorAdjustments.CollapseState REMEMBERED_GRADE = new UIFormColorAdjustments.CollapseState();
    private static boolean rememberedBlendTransformOpen;
    private static boolean rememberedPaintTransformOpen;
    private static boolean hasRememberedCollapseState;

    private final boolean simpleBlendColorOnly;
    private final boolean hideColorGrade;

    private UIColor blendColor;
    private UITrackpad blendIntensity;
    private UIFormColorAdjustments blendAdjustments;
    private UIEffectTransformCollapse blendTransform;
    private UIColor paintColor;
    private UITrackpad paintIntensity;
    private UIEffectTransformCollapse paintTransform;
    private UIToggle spectrum;

    public UIFormColorKeyframeFactory(Keyframe<Color> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.simpleBlendColorOnly = this.isSimpleBlendColorOnly();
        this.hideColorGrade = this.isTrailForm();

        this.blendColor = new UIColor((c) -> this.applyColorEdit((color) ->
        {
            Color value = Color.rgb(c);
            float intensity = color.a;

            color.set(value.r, value.g, value.b, intensity);
        }));
        this.blendColor.setColor(keyframe.getValue().getRGBColor());

        this.spectrum = new UIToggle(UIKeys.GENERIC_KEYFRAMES_COLOR_SPECTRUM, (b) -> this.setSpectrum(b.getValue()));
        this.spectrum.tooltip(UIKeys.GENERIC_KEYFRAMES_COLOR_SPECTRUM_TOOLTIP);
        this.spectrum.setValue(keyframe.isSpectrum());

        this.scroll.add(UI.label(UIKeys.FORMS_EDITORS_BLEND_COLOR).marginTop(4));
        this.scroll.add(this.blendColor);

        if (!this.simpleBlendColorOnly)
        {
            this.blendIntensity = new UITrackpad((value) -> this.applyColorEdit((color) ->
                color.a = MathUtils.clamp(value.floatValue(), 0F, 1F)));
            this.blendIntensity.limit(0F, 1F).values(0.1D, 0.05D, 0.2D);
            this.blendIntensity.tooltip(UIKeys.FORMS_EDITORS_BLEND_INTENSITY);
            this.wireUndo(this.blendIntensity);

            this.blendTransform = new UIEffectTransformCollapse((apply) -> this.applyColorEdit((color) ->
            {
                if (color.transform == null)
                {
                    color.transform = new EffectTransform();
                }

                apply.accept(color.transform);
            }));
            this.blendTransform.registerUndo(editor);

            this.paintColor = new UIColor((c) -> this.applyPaintEdit((settings) -> this.setPaintColor(settings, c)));
            this.paintColor.tooltip(UIKeys.FORMS_EDITORS_PAINT_COLOR);

            this.paintIntensity = new UITrackpad((value) -> this.applyPaintEdit((settings) ->
                settings.intensity = PaintSettings.clampIntensity(value.floatValue())));
            this.paintIntensity.increment(0.05D).values(0.1D, 0.05D, 0.2D).limit(PaintSettings.MIN_INTENSITY, PaintSettings.MAX_INTENSITY);
            this.paintIntensity.tooltip(UIKeys.FORMS_EDITORS_PAINT_INTENSITY);

            this.paintTransform = new UIEffectTransformCollapse((apply) -> this.applyPaintEdit((settings) ->
            {
                if (settings.transform == null)
                {
                    settings.transform = new EffectTransform();
                }

                apply.accept(settings.transform);
            }));
            /* Paint lives on a hidden channel (not a timeline sheet); undo via color-sheet
             * cache/submit would not capture paint and was snapping the intensity bar back. */

            this.scroll.add(UI.label(UIKeys.FORMS_EDITORS_BLEND_INTENSITY), this.blendIntensity);
            this.scroll.add(this.blendTransform);
            this.scroll.add(UI.label(UIKeys.FORMS_EDITORS_PAINT_COLOR).marginTop(4));
            this.scroll.add(this.paintColor);
            this.scroll.add(UI.label(UIKeys.FORMS_EDITORS_PAINT_INTENSITY), this.paintIntensity);
            this.scroll.add(this.paintTransform);
            this.scroll.add(this.spectrum.marginTop(8));

            this.wireResetThisValue(this.blendIntensity, () -> this.applyColorEdit((color) -> color.a = 0F));
            this.wireResetThisValue(this.paintIntensity, () -> this.applyPaintEdit((settings) -> settings.intensity = 0F));

            if (!this.hideColorGrade)
            {
                this.blendAdjustments = new UIFormColorAdjustments(
                    () -> this.getOrCreateColor(this.keyframe.getValue()),
                    (color) -> this.applyColorEdit((target) ->
                    {
                        target.brightness = color.brightness;
                        target.contrast = color.contrast;
                        target.hue = color.hue;
                        target.saturation = color.saturation;
                        target.brightnessTransform = color.brightnessTransform == null ? new EffectTransform() : color.brightnessTransform.copy();
                        target.contrastTransform = color.contrastTransform == null ? new EffectTransform() : color.contrastTransform.copy();
                        target.hueTransform = color.hueTransform == null ? new EffectTransform() : color.hueTransform.copy();
                        target.saturationTransform = color.saturationTransform == null ? new EffectTransform() : color.saturationTransform.copy();
                    })
                );
                this.wireUndo(this.blendAdjustments.brightness);
                this.wireUndo(this.blendAdjustments.contrast);
                this.wireUndo(this.blendAdjustments.hue);
                this.wireUndo(this.blendAdjustments.saturation);
                this.blendAdjustments.registerUndo(editor);
                this.blendAdjustments.wireResetThisValue(this::wireResetThisValue);
                this.scroll.add(this.blendAdjustments.marginTop(4));
            }
        }
        else
        {
            this.scroll.add(this.spectrum.marginTop(8));
        }

        this.context((menu) ->
        {
            menu.action(Icons.CLOSE, UIKeys.FORMS_EDITORS_COLOR_RESET_ALL, this::resetAll);

            if (!this.simpleBlendColorOnly)
            {
                menu.action(Icons.REFRESH, UIKeys.FORMS_EDITORS_COLOR_RESET_BLEND, this::resetBlendColor);
                menu.action(Icons.REFRESH, UIKeys.FORMS_EDITORS_COLOR_RESET_PAINT, this::resetPaintColor);

                if (!this.hideColorGrade)
                {
                    menu.action(Icons.REFRESH, UIKeys.FORMS_EDITORS_COLOR_RESET_GRADE, this::resetColorGrade);
                }
            }
            else
            {
                menu.action(Icons.REFRESH, UIKeys.FORMS_EDITORS_COLOR_RESET_BLEND, this::resetBlendColor);
            }
        });

        this.update();
    }

    @Override
    public void saveUiState()
    {
        this.saveCollapseState();
    }

    @Override
    public void restoreUiState()
    {
        this.restoreCollapseState();
    }

    private void saveCollapseState()
    {
        hasRememberedCollapseState = true;

        if (this.blendTransform != null)
        {
            rememberedBlendTransformOpen = this.blendTransform.isExpanded();
        }

        if (this.paintTransform != null)
        {
            rememberedPaintTransformOpen = this.paintTransform.isExpanded();
        }

        if (this.blendAdjustments != null)
        {
            this.blendAdjustments.saveCollapseState(REMEMBERED_GRADE);
        }
    }

    private void restoreCollapseState()
    {
        if (!hasRememberedCollapseState)
        {
            return;
        }

        if (this.blendTransform != null)
        {
            this.blendTransform.setExpanded(rememberedBlendTransformOpen);
        }

        if (this.paintTransform != null)
        {
            this.paintTransform.setExpanded(rememberedPaintTransformOpen);
        }

        if (this.blendAdjustments != null)
        {
            this.blendAdjustments.restoreCollapseState(REMEMBERED_GRADE);
        }
    }

    private Form getEditingForm()
    {
        if (this.editor == null)
        {
            return null;
        }

        UIKeyframeSheet sheet = this.editor.getGraph().getSheet(this.keyframe);

        if (sheet != null && sheet.property != null)
        {
            return FormUtils.getForm(sheet.property);
        }

        UIReplaysEditor replays = this.editor.getParent(UIReplaysEditor.class);

        if (replays == null || replays.getReplay() == null)
        {
            return null;
        }

        return replays.getReplay().form.get();
    }

    private boolean isSimpleBlendColorOnly()
    {
        return this.getEditingForm() instanceof LabelForm;
    }

    private boolean isTrailForm()
    {
        return this.getEditingForm() instanceof TrailForm;
    }

    private void wireUndo(UITrackpad trackpad)
    {
        trackpad.getEvents().register(UITrackpadDragStartEvent.class, (e) -> this.editor.cacheKeyframes());
        trackpad.getEvents().register(UITrackpadDragEndEvent.class, (e) -> this.editor.submitKeyframes());
    }

    private void wireResetThisValue(UITrackpad trackpad, Runnable reset)
    {
        trackpad.context((menu) -> menu.action(Icons.REFRESH, UIKeys.FORMS_EDITORS_COLOR_RESET_THIS_VALUE, () ->
        {
            if (this.editor != null)
            {
                this.editor.cacheKeyframes();
            }

            reset.run();

            if (this.editor != null)
            {
                this.editor.submitKeyframes();
            }

            this.update();
        }));
    }

    private void resetAll()
    {
        this.resetBlendColor();

        if (!this.simpleBlendColorOnly)
        {
            this.resetPaintColor();

            if (!this.hideColorGrade)
            {
                this.resetColorGrade();
            }
        }
    }

    private void resetBlendColor()
    {
        this.applyColorEdit((color) ->
        {
            color.set(1F, 1F, 1F, 1F);
            color.transform = new EffectTransform();
        });
        this.update();
    }

    private void resetPaintColor()
    {
        this.applyPaintEdit((settings) ->
        {
            settings.r = 1F;
            settings.g = 1F;
            settings.b = 1F;
            settings.intensity = 0F;
            settings.transform = new EffectTransform();
        });
        this.update();
    }

    private void resetColorGrade()
    {
        if (this.blendAdjustments == null)
        {
            return;
        }

        this.blendAdjustments.resetGrade();
        this.update();
    }

    @Override
    public void update()
    {
        super.update();

        this.syncLiveColorKeyframe();

        Color value = this.getOrCreateColor(this.keyframe.getValue());

        this.blendColor.setColor(value.getRGBColor());
        this.spectrum.setValue(this.keyframe.isSpectrum());

        if (this.simpleBlendColorOnly)
        {
            return;
        }

        PaintSettings paint = this.getPaintSettingsAtTick(this.keyframe.getTick());

        this.blendIntensity.setValue(MathUtils.clamp(value.a, 0F, 1F));

        if (this.blendAdjustments != null)
        {
            this.blendAdjustments.syncFromForm();
        }

        this.blendTransform.setEffectTransform(value.transform);
        this.paintColor.setColor(new Color().set(paint.r, paint.g, paint.b, 1F).getRGBColor());
        this.paintIntensity.setValue(paint.intensity);
        this.paintTransform.setEffectTransform(paint.transform);
    }

    /**
     * {@link UIKeyframes#submitKeyframes()} replaces channel keyframe instances. Keep
     * {@link #keyframe} pointed at the live selected color keyframe so Blend intensity
     * is not read back from an orphaned copy (which made Paint edits appear to revert it).
     */
    @SuppressWarnings("unchecked")
    private void syncLiveColorKeyframe()
    {
        if (this.editor == null || this.keyframe == null)
        {
            return;
        }

        UIKeyframeSheet colorSheet = null;

        for (UIKeyframeSheet sheet : this.editor.getGraph().getSheets())
        {
            if (sheet.channel.getFactory() == KeyframeFactories.COLOR && "color".equals(sheet.id))
            {
                colorSheet = sheet;

                break;
            }
        }

        if (colorSheet == null)
        {
            return;
        }

        List selected = colorSheet.selection.getSelected();

        if (!selected.isEmpty())
        {
            this.keyframe = (Keyframe<Color>) selected.get(0);

            return;
        }

        float tick = this.keyframe.getTick();

        for (Object kfObj : colorSheet.channel.getKeyframes())
        {
            Keyframe<?> kf = (Keyframe<?>) kfObj;

            if (Math.abs(kf.getTick() - tick) < 0.001F && kf.getValue() instanceof Color)
            {
                this.keyframe = (Keyframe<Color>) kf;

                return;
            }
        }
    }

    private Color getOrCreateColor(Color color)
    {
        if (color == null)
        {
            color = Color.white();
        }

        if (color.transform == null)
        {
            color.transform = new EffectTransform();
        }

        return color;
    }

    @SuppressWarnings("unchecked")
    private void applyColorEdit(Consumer<Color> editor)
    {
        this.syncLiveColorKeyframe();

        boolean[] applied = {false};

        UIReplaysEditorUtils.forEachSelectedKeyframe(this.editor, this.keyframe, (selected) ->
        {
            applied[0] = true;
            this.keyframe = (Keyframe<Color>) (Keyframe<?>) selected;

            Color color = this.getOrCreateColor((Color) selected.getValue());

            selected.preNotify();
            editor.accept(color);
            selected.postNotify();
        });

        if (!applied[0])
        {
            Color color = this.getOrCreateColor(this.keyframe.getValue());

            this.keyframe.preNotify();
            editor.accept(color);
            this.keyframe.postNotify();
        }
    }

    private void applyPaintEdit(Consumer<PaintSettings> editor)
    {
        KeyframeChannel<PaintSettings> channel = this.resolvePaintChannel();

        if (channel == null)
        {
            return;
        }

        this.syncLiveColorKeyframe();

        float tick = this.keyframe.getTick();
        PaintSettings base = this.getPaintSettingsAt(channel, tick);
        int index = channel.insert(tick, base);
        Keyframe<PaintSettings> paintKeyframe = channel.get(index);

        if (paintKeyframe == null)
        {
            paintKeyframe = this.findPaintKeyframe(channel, tick);
        }

        if (paintKeyframe == null)
        {
            return;
        }

        paintKeyframe.preNotify();
        editor.accept(this.getOrCreatePaint(paintKeyframe.getValue()));
        paintKeyframe.getValue().applyAutoShaderShadow();
        paintKeyframe.postNotify();

        this.refreshPaintFields(paintKeyframe.getValue());
    }

    private void refreshPaintFields(PaintSettings paint)
    {
        if (this.simpleBlendColorOnly || this.paintColor == null)
        {
            return;
        }

        PaintSettings value = this.getOrCreatePaint(paint);

        this.paintColor.setColor(new Color().set(value.r, value.g, value.b, 1F).getRGBColor());
        this.paintIntensity.setValue(value.intensity);
        this.paintTransform.setEffectTransform(value.transform);
    }

    private PaintSettings getPaintSettingsAtTick(float tick)
    {
        KeyframeChannel<PaintSettings> channel = this.resolvePaintChannel();

        if (channel == null)
        {
            return new PaintSettings();
        }

        return this.getPaintSettingsAt(channel, tick);
    }

    private PaintSettings getPaintSettingsAt(KeyframeChannel<PaintSettings> channel, float tick)
    {
        PaintSettings settings = channel.interpolate(tick);

        if (settings == null)
        {
            settings = new PaintSettings();
        }
        else
        {
            settings = settings.copy();
        }

        return this.getOrCreatePaint(settings);
    }

    private PaintSettings getOrCreatePaint(PaintSettings settings)
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

    @SuppressWarnings("unchecked")
    private KeyframeChannel<PaintSettings> resolvePaintChannel()
    {
        UIReplaysEditor replays = this.editor.getParent(UIReplaysEditor.class);

        if (replays == null || replays.getReplay() == null)
        {
            return null;
        }

        Form form = replays.getReplay().form.get();
        FormProperties properties = replays.getReplay().properties;
        KeyframeChannel channel = properties.getOrCreate(form, "paint");

        if (channel != null && channel.getFactory() == KeyframeFactories.PAINT_SETTINGS)
        {
            return (KeyframeChannel<PaintSettings>) channel;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Keyframe<PaintSettings> findPaintKeyframe(KeyframeChannel<PaintSettings> channel, float tick)
    {
        for (Object kfObj : channel.getKeyframes())
        {
            Keyframe<?> kf = (Keyframe<?>) kfObj;

            if (Math.abs(kf.getTick() - tick) < 0.001F && kf.getValue() instanceof PaintSettings)
            {
                return (Keyframe<PaintSettings>) kf;
            }
        }

        return null;
    }

    private void setPaintColor(PaintSettings settings, int c)
    {
        Color color = new Color().set(c);

        settings.r = color.r;
        settings.g = color.g;
        settings.b = color.b;
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
