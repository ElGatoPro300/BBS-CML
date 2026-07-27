package mchorse.bbs_mod.ui.forms.editors.panels.widgets;

import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.PaintMaskShape;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcons;
import mchorse.bbs_mod.ui.framework.elements.input.UIEffectKeyframeTransform;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Color;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class UIFormColorTransform extends UIElement
{
    private final Supplier<Color> color;
    private final UIIcons shapeIcons;
    private final UIEffectKeyframeTransform transform;

    public UIFormColorTransform(Supplier<Color> color, Consumer<Color> setter)
    {
        super();

        this.color = color;
        this.column().vertical().stretch();

        this.shapeIcons = new UIIcons((b) ->
        {
            Color copy = color.get().copy();

            copy.transform.shape = PaintMaskShape.fromId(b.getValue());
            setter.accept(copy);
        });
        this.shapeIcons.add(Icons.SQUARE, UIKeys.FORMS_EDITORS_PAINT_SHAPE_BOX);
        this.shapeIcons.add(Icons.CIRCLE, UIKeys.FORMS_EDITORS_PAINT_SHAPE_CIRCLE);
        this.shapeIcons.add(Icons.TRIANGLE, UIKeys.FORMS_EDITORS_PAINT_SHAPE_TRIANGLE);
        this.shapeIcons.h(20);

        this.transform = new UIEffectKeyframeTransform((apply) ->
        {
            Color copy = color.get().copy();

            if (copy.transform == null)
            {
                copy.transform = new EffectTransform();
            }

            apply.accept(copy.transform);
            setter.accept(copy);
        });

        this.add(UI.label(UIKeys.FORMS_EDITORS_PAINT_SHAPE), this.shapeIcons, this.transform);
    }

    public void syncFromForm()
    {
        Color value = this.color.get();
        EffectTransform effect = value == null || value.transform == null ? new EffectTransform() : value.transform;

        this.shapeIcons.setValue(effect.shape == null ? 0 : effect.shape.id);
        this.transform.setEffectTransform(effect);
    }
}
