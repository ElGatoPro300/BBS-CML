package mchorse.bbs_mod.ui.forms.editors.panels.widgets;

import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.UIAnimatedCollapseShell;
import mchorse.bbs_mod.ui.framework.elements.input.UIEffectTransformCollapse;
import mchorse.bbs_mod.ui.framework.elements.input.UIPoseSectionCollapse;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.ColorAdjustments;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Color grade disclosure: closed shows only a slim Color grade bar with arrow;
 * open reveals brightness / contrast / saturation / hue with animated height,
 * each with its own Transform collapse.
 */
public class UIFormColorAdjustments extends UIElement
{
    private static final int HEADER_H = 16;
    private static final int COLOR_GRADE_COLOR = 0xfa0e49;

    public final UITrackpad brightness;
    public final UITrackpad contrast;
    public final UITrackpad saturation;
    public final UITrackpad hue;
    public final UIEffectTransformCollapse brightnessTransform;
    public final UIEffectTransformCollapse contrastTransform;
    public final UIEffectTransformCollapse saturationTransform;
    public final UIEffectTransformCollapse hueTransform;

    private final Supplier<Color> color;
    private final Consumer<Color> setter;
    private final UIPoseSectionCollapse.SectionHeader toggle;
    private final UIAnimatedCollapseShell shell;
    private boolean expanded;

    public UIFormColorAdjustments(Supplier<Color> color, Consumer<Color> setter)
    {
        super();

        this.color = color;
        this.setter = setter;
        this.h(HEADER_H);

        this.toggle = new UIPoseSectionCollapse.SectionHeader((b) -> this.setExpanded(!this.expanded));
        this.toggle.full(this).h(HEADER_H);
        this.toggle.setLabel(UIKeys.FORMS_EDITORS_COLOR_GRADE);
        this.toggle.setTrackColor(COLOR_GRADE_COLOR);

        this.brightness = this.createTrackpad(ColorAdjustments.MIN_BRIGHTNESS, ColorAdjustments.MAX_BRIGHTNESS, (value) ->
        {
            Color copy = this.color.get().copy();

            copy.brightness = ColorAdjustments.clampBrightness(value);
            this.setter.accept(copy);
        });
        this.brightness.tooltip(UIKeys.FORMS_EDITORS_COLOR_BRIGHTNESS);
        this.brightnessTransform = this.createTransform((c) -> c.brightnessTransform, (c, t) -> c.brightnessTransform = t);

        this.contrast = this.createTrackpad(ColorAdjustments.MIN_CONTRAST, ColorAdjustments.MAX_CONTRAST, (value) ->
        {
            Color copy = this.color.get().copy();

            copy.contrast = ColorAdjustments.clampContrast(value);
            this.setter.accept(copy);
        });
        this.contrast.tooltip(UIKeys.FORMS_EDITORS_COLOR_CONTRAST);
        this.contrastTransform = this.createTransform((c) -> c.contrastTransform, (c, t) -> c.contrastTransform = t);

        this.saturation = this.createTrackpad(ColorAdjustments.MIN_SATURATION, ColorAdjustments.MAX_SATURATION, (value) ->
        {
            Color copy = this.color.get().copy();

            copy.saturation = ColorAdjustments.clampSaturation(value);
            this.setter.accept(copy);
        });
        this.saturation.tooltip(UIKeys.FORMS_EDITORS_COLOR_SATURATION);
        this.saturationTransform = this.createTransform((c) -> c.saturationTransform, (c, t) -> c.saturationTransform = t);

        this.hue = this.createTrackpad(ColorAdjustments.MIN_HUE, ColorAdjustments.MAX_HUE, (value) ->
        {
            Color copy = this.color.get().copy();

            copy.hue = ColorAdjustments.clampHue(value);
            this.setter.accept(copy);
        });
        this.hue.increment(1D).values(5D, 1D, 15D);
        this.hue.tooltip(UIKeys.FORMS_EDITORS_COLOR_HUE);
        this.hueTransform = this.createTransform((c) -> c.hueTransform, (c, t) -> c.hueTransform = t);

        this.shell = new UIAnimatedCollapseShell(UI.column(
            UI.label(UIKeys.FORMS_EDITORS_COLOR_BRIGHTNESS), this.brightness, this.brightnessTransform,
            UI.label(UIKeys.FORMS_EDITORS_COLOR_CONTRAST), this.contrast, this.contrastTransform,
            UI.label(UIKeys.FORMS_EDITORS_COLOR_SATURATION), this.saturation, this.saturationTransform,
            UI.label(UIKeys.FORMS_EDITORS_COLOR_HUE), this.hue, this.hueTransform
        ));

        this.add(this.toggle);
    }

    public void registerUndo(UIKeyframes editor)
    {
        this.brightnessTransform.registerUndo(editor);
        this.contrastTransform.registerUndo(editor);
        this.saturationTransform.registerUndo(editor);
        this.hueTransform.registerUndo(editor);
    }

    /**
     * Wire per-trackpad "Reset this value" (sets that grade channel to 0).
     */
    public void wireResetThisValue(BiConsumer<UITrackpad, Runnable> wire)
    {
        wire.accept(this.brightness, () -> this.resetChannel((color) -> color.brightness = 0F));
        wire.accept(this.contrast, () -> this.resetChannel((color) -> color.contrast = 0F));
        wire.accept(this.saturation, () -> this.resetChannel((color) -> color.saturation = 0F));
        wire.accept(this.hue, () -> this.resetChannel((color) -> color.hue = 0F));
    }

    public void resetGrade()
    {
        this.resetChannel((color) ->
        {
            color.brightness = 0F;
            color.contrast = 0F;
            color.saturation = 0F;
            color.hue = 0F;
            color.brightnessTransform = new EffectTransform();
            color.contrastTransform = new EffectTransform();
            color.saturationTransform = new EffectTransform();
            color.hueTransform = new EffectTransform();
        });
    }

    private void resetChannel(Consumer<Color> editor)
    {
        Color copy = this.color.get().copy();

        editor.accept(copy);
        this.setter.accept(copy);
    }

    public void setExpanded(boolean expanded)
    {
        if (this.expanded == expanded && this.shell.isOpen() == expanded)
        {
            return;
        }

        this.shell.setExpanded(expanded, this);

        this.expanded = this.shell.isOpen() || (expanded && this.shell.isAnimating());
        this.toggle.setLabel(UIKeys.FORMS_EDITORS_COLOR_GRADE);
        this.toggle.setExpanded(this.expanded);
    }

    public boolean isExpanded()
    {
        return this.expanded || this.shell.isOpen();
    }

    public void saveCollapseState(CollapseState state)
    {
        if (state == null)
        {
            return;
        }

        state.gradeOpen = this.isExpanded();
        state.brightnessTransformOpen = this.brightnessTransform.isExpanded();
        state.contrastTransformOpen = this.contrastTransform.isExpanded();
        state.saturationTransformOpen = this.saturationTransform.isExpanded();
        state.hueTransformOpen = this.hueTransform.isExpanded();
    }

    public void restoreCollapseState(CollapseState state)
    {
        if (state == null)
        {
            return;
        }

        this.setExpanded(state.gradeOpen);
        this.brightnessTransform.setExpanded(state.brightnessTransformOpen);
        this.contrastTransform.setExpanded(state.contrastTransformOpen);
        this.saturationTransform.setExpanded(state.saturationTransformOpen);
        this.hueTransform.setExpanded(state.hueTransformOpen);
    }

    public static final class CollapseState
    {
        public boolean gradeOpen;
        public boolean brightnessTransformOpen;
        public boolean contrastTransformOpen;
        public boolean saturationTransformOpen;
        public boolean hueTransformOpen;
    }

    private UIEffectTransformCollapse createTransform(Function<Color, EffectTransform> getter, BiConsumer<Color, EffectTransform> assign)
    {
        return new UIEffectTransformCollapse((apply) ->
        {
            Color copy = this.color.get().copy();
            EffectTransform transform = getter.apply(copy);

            if (transform == null)
            {
                transform = new EffectTransform();
                assign.accept(copy, transform);
            }

            apply.accept(transform);
            this.setter.accept(copy);
        });
    }

    private UITrackpad createTrackpad(float min, float max, Consumer<Float> editor)
    {
        UITrackpad trackpad = new UITrackpad((value) -> editor.accept(value.floatValue()));

        trackpad.limit(min, max).increment(0.05D).values(0.1D, 0.05D, 0.25D);

        return trackpad;
    }

    public void syncFromForm()
    {
        Color value = this.color.get();

        this.brightness.setValue(value.brightness);
        this.contrast.setValue(value.contrast);
        this.saturation.setValue(value.saturation);
        this.hue.setValue(value.hue);
        this.brightnessTransform.setEffectTransform(value.brightnessTransform);
        this.contrastTransform.setEffectTransform(value.contrastTransform);
        this.saturationTransform.setEffectTransform(value.saturationTransform);
        this.hueTransform.setEffectTransform(value.hueTransform);
    }
}
