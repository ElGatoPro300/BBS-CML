package mchorse.bbs_mod.ui.forms.editors.panels.widgets;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UIPoseSectionCollapse;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared classic-style Color / Glow / Advanced layout for form + pose editors.
 * Glow fields are always visible (no collapse). Paint / masks / Color grade stay under Advanced.
 * Linked color swatch + value inputs share one row; each control has a label above it.
 */
public final class UIFormColorLayout
{
    /** Muted header so Advanced does not look like a Color/Glow timeline track. */
    public static final int ADVANCED_HEADER = 0x777777;

    private UIFormColorLayout()
    {}

    public static UIElement sectionLabel(IKey key)
    {
        return UI.label(key).marginTop(8);
    }

    /**
     * Color swatch + linked numeric input on one row, each with its own label above.
     */
    public static UIElement labeledColorValueRow(IKey colorLabel, UIColor color, IKey valueLabel, UITrackpad value)
    {
        return UI.row(
            UI.column(UI.label(colorLabel), color),
            UI.column(UI.label(valueLabel), value)
        );
    }

    public static UIElement colorValueRow(UIColor color, UITrackpad value)
    {
        return UI.row(color, value);
    }

    public static UIElement paintColorRow(UIColor paintColor, UITrackpad paintIntensity)
    {
        return labeledColorValueRow(
            UIKeys.FORMS_EDITORS_PAINT_COLOR,
            paintColor,
            UIKeys.FORMS_EDITORS_PAINT_INTENSITY,
            paintIntensity
        ).marginTop(4);
    }

    /**
     * Always-visible glow color + intensity (no disclosure header).
     */
    public static UIElement createGlowSection(UIColor glowingColor, UITrackpad glowIntensity)
    {
        return labeledColorValueRow(
            UIKeys.FORMS_EDITORS_GLOWING_COLOR,
            glowingColor,
            UIKeys.FORMS_EDITORS_GLOW_INTENSITY,
            glowIntensity
        ).marginTop(4);
    }

    public static UIPoseSectionCollapse createAdvancedSection(UIElement... content)
    {
        List<UIElement> elements = new ArrayList<>();

        for (UIElement element : content)
        {
            if (element != null)
            {
                elements.add(element);
            }
        }

        return new UIPoseSectionCollapse(
            UIKeys.FORMS_EDITORS_COLOR_ADVANCED,
            ADVANCED_HEADER,
            UI.column(elements.toArray(new UIElement[0]))
        );
    }
}
