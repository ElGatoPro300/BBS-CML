package mchorse.bbs_mod.ui.framework.elements.input;

import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.PaintMaskShape;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcons;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import java.util.function.Consumer;

/**
 * Compact "Transform" disclosure. When closed only the button stays in layout so
 * following sections (e.g. Paint Color) sit directly underneath. The body opens
 * and closes with a height animation under an {@link UIAnimatedCollapseShell}.
 */
public class UIEffectTransformCollapse extends UIElement
{
    private final UIButton toggle;
    private final UIAnimatedCollapseShell shell;
    private final UIIcons shapeIcons;
    private final UIEffectKeyframeTransform transform;
    private boolean expanded;

    public UIEffectTransformCollapse(Consumer<Consumer<EffectTransform>> apply)
    {
        super();

        this.h(16);

        this.toggle = new UIButton(UIKeys.FORMS_EDITORS_COLOR_TRANSFORM, (b) -> this.setExpanded(!this.expanded));
        this.toggle.full(this);

        this.shapeIcons = new UIIcons((b) -> apply.accept((effect) ->
            effect.shape = PaintMaskShape.fromId(b.getValue())));
        this.shapeIcons.add(Icons.SQUARE, UIKeys.FORMS_EDITORS_PAINT_SHAPE_BOX);
        this.shapeIcons.add(Icons.CIRCLE, UIKeys.FORMS_EDITORS_PAINT_SHAPE_CIRCLE);
        this.shapeIcons.add(Icons.TRIANGLE, UIKeys.FORMS_EDITORS_PAINT_SHAPE_TRIANGLE);
        this.shapeIcons.h(20);

        this.transform = new UIEffectKeyframeTransform(apply);
        this.shell = new UIAnimatedCollapseShell(UI.column(
            UI.label(UIKeys.FORMS_EDITORS_PAINT_SHAPE),
            this.shapeIcons,
            this.transform
        ));

        this.add(this.toggle);
    }

    public UIEffectTransformCollapse label(IKey label)
    {
        this.toggle.label = label;

        return this;
    }

    public void registerUndo(UIKeyframes editor)
    {
        this.transform.registerUndo(editor);
    }

    public void setEffectTransform(EffectTransform effect)
    {
        EffectTransform value = effect == null ? new EffectTransform() : effect;

        this.shapeIcons.setValue(value.shape == null ? 0 : value.shape.id);
        this.transform.setEffectTransform(value);
    }

    public void setExpanded(boolean expanded)
    {
        if (this.expanded == expanded && this.shell.isOpen() == expanded)
        {
            return;
        }

        this.expanded = expanded;
        this.toggle.label = expanded
            ? UIKeys.FORMS_EDITORS_COLOR_TRANSFORM_HIDE
            : UIKeys.FORMS_EDITORS_COLOR_TRANSFORM;

        this.shell.setExpanded(expanded, this);
    }
}
