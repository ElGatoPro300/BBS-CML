package mchorse.bbs_mod.ui.framework.elements.input;

import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.PaintMaskShape;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcons;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import java.util.function.Consumer;

/**
 * Slim "Transform" disclosure (left label, right arrow). When closed only the
 * header stays in layout so following sections sit directly underneath. The body
 * opens and closes with a height animation under an {@link UIAnimatedCollapseShell}.
 */
public class UIEffectTransformCollapse extends UIElement
{
    private static final int HEADER_H = 16;
    private static final int TRANSFORM_COLOR = 0x3a6dff;

    private IKey baseLabel = UIKeys.FORMS_EDITORS_COLOR_TRANSFORM;
    private final UIPoseSectionCollapse.SectionHeader toggle;
    private final UIAnimatedCollapseShell shell;
    private final UIIcons shapeIcons;
    private final UIEffectKeyframeTransform transform;
    private boolean expanded;

    public UIEffectTransformCollapse(Consumer<Consumer<EffectTransform>> apply)
    {
        super();

        this.h(HEADER_H);

        this.toggle = new UIPoseSectionCollapse.SectionHeader((b) -> this.setExpanded(!this.expanded));
        this.toggle.full(this).h(HEADER_H);
        this.toggle.setLabel(this.baseLabel);
        this.toggle.setTrackColor(TRANSFORM_COLOR);

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
        this.baseLabel = label == null ? IKey.EMPTY : label;
        this.toggle.setLabel(this.baseLabel);

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

        this.shell.setExpanded(expanded, this);

        this.expanded = this.shell.isOpen() || (expanded && this.shell.isAnimating());
        this.toggle.setLabel(this.baseLabel);
        this.toggle.setExpanded(this.expanded);
    }

    public boolean isExpanded()
    {
        return this.expanded || this.shell.isOpen();
    }
}
