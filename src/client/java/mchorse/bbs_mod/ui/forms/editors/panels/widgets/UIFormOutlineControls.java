package mchorse.bbs_mod.ui.forms.editors.panels.widgets;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.utils.UIFormSectionExpand;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UISection;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Color;

import java.util.function.Supplier;

/**
 * Shared Outline editor controls (toggle / color / thickness / rainbow) for form panels.
 */
public class UIFormOutlineControls
{
    public final UIToggle outline;
    public final UIColor outlineColor;
    public final UITrackpad outlineThickness;
    public final UIToggle outlineRainbow;
    public final UITrackpad outlineRainbowSpeed;
    public final UITrackpad outlineRainbowScale;
    public final UIElement rainbowRow;

    private final Supplier<Form> form;
    private final Runnable onLayoutChange;

    public UIFormOutlineControls(Supplier<Form> form, Runnable onLayoutChange)
    {
        this.form = form;
        this.onLayoutChange = onLayoutChange;

        this.outline = new UIToggle(UIKeys.FORMS_EDITORS_MODEL_OUTLINE, (b) ->
        {
            Form f = this.form.get();

            if (f != null)
            {
                f.outline.set(b.getValue());
            }
        });
        this.outline.tooltip(UIKeys.FORMS_EDITORS_MODEL_OUTLINE_TOOLTIP);

        this.outlineColor = new UIColor((c) ->
        {
            Form f = this.form.get();

            if (f == null)
            {
                return;
            }

            Color copy = f.outlineColor.get().copy();
            Color value = new Color().set(c);

            copy.r = value.r;
            copy.g = value.g;
            copy.b = value.b;
            f.outlineColor.set(copy);
        });
        this.outlineColor.direction(Direction.LEFT);
        this.outlineColor.tooltip(UIKeys.FORMS_EDITORS_MODEL_OUTLINE_COLOR_TOOLTIP);

        this.outlineThickness = new UITrackpad((value) ->
        {
            Form f = this.form.get();

            if (f != null)
            {
                f.outlineThickness.set(value.floatValue());
            }
        });
        this.outlineThickness.limit(0D, 32D).increment(0.5D).values(1D, 2D, 4D);
        this.outlineThickness.tooltip(UIKeys.FORMS_EDITORS_MODEL_OUTLINE_THICKNESS_TOOLTIP);

        this.outlineRainbowSpeed = new UITrackpad((value) ->
        {
            Form f = this.form.get();

            if (f != null)
            {
                f.outlineRainbowSpeed.set(value.floatValue());
            }
        });
        this.outlineRainbowSpeed.limit(-20D, 20D).increment(0.1D).values(0.5D, 1D, 2D);
        this.outlineRainbowSpeed.tooltip(UIKeys.FORMS_EDITORS_MODEL_OUTLINE_RAINBOW_SPEED_TOOLTIP);

        this.outlineRainbowScale = new UITrackpad((value) ->
        {
            Form f = this.form.get();

            if (f != null)
            {
                f.outlineRainbowScale.set(value.floatValue());
            }
        });
        this.outlineRainbowScale.limit(0.05D, 10D).increment(0.1D).values(0.5D, 1D, 2D);
        this.outlineRainbowScale.tooltip(UIKeys.FORMS_EDITORS_MODEL_OUTLINE_RAINBOW_SCALE_TOOLTIP);

        this.rainbowRow = UI.row(this.outlineRainbowSpeed, this.outlineRainbowScale);

        this.outlineRainbow = new UIToggle(UIKeys.FORMS_EDITORS_MODEL_OUTLINE_RAINBOW, (b) ->
        {
            Form f = this.form.get();

            if (f != null)
            {
                f.outlineRainbow.set(b.getValue());
            }

            this.rainbowRow.setVisible(b.getValue());

            if (this.onLayoutChange != null)
            {
                this.onLayoutChange.run();
            }
        });
        this.outlineRainbow.tooltip(UIKeys.FORMS_EDITORS_MODEL_OUTLINE_RAINBOW_TOOLTIP);
    }

    public UISection createSection()
    {
        return UIFormSectionExpand.bind(
            new UISection(UIKeys.FORMS_EDITORS_MODEL_OUTLINE,
                this.outline,
                UIFormColorLayout.colorValueRow(this.outlineColor, this.outlineThickness),
                this.outlineRainbow,
                this.rainbowRow
            ),
            UIFormSectionExpand.SHARED_OUTLINE
        );
    }

    public void syncFromForm(Form form)
    {
        if (form == null)
        {
            return;
        }

        this.outline.setValue(form.outline.get());
        this.outlineColor.setColor(form.outlineColor.get().getRGBColor());
        this.outlineThickness.setValue(form.outlineThickness.get());
        this.outlineRainbow.setValue(form.outlineRainbow.get());
        this.outlineRainbowSpeed.setValue(form.outlineRainbowSpeed.get());
        this.outlineRainbowScale.setValue(form.outlineRainbowScale.get());
        this.rainbowRow.setVisible(form.outlineRainbow.get());
    }
}
