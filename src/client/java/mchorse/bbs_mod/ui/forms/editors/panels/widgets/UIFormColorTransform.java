package mchorse.bbs_mod.ui.forms.editors.panels.widgets;

import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UIEffectTransformCollapse;
import mchorse.bbs_mod.utils.colors.Color;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Color {@link EffectTransform} as a trailing Transform icon toggle.
 * Pair with a color swatch via {@link #withLeading}.
 */
public class UIFormColorTransform extends UIEffectTransformCollapse
{
    private final Supplier<Color> color;

    public UIFormColorTransform(Supplier<Color> color, Consumer<Color> setter)
    {
        super((apply) ->
        {
            Color copy = color.get().copy();

            if (copy.transform == null)
            {
                copy.transform = new EffectTransform();
            }

            apply.accept(copy.transform);
            setter.accept(copy);
        });

        this.color = color;
    }

    public void syncFromForm()
    {
        Color value = this.color.get();
        EffectTransform effect = value == null || value.transform == null ? new EffectTransform() : value.transform;

        this.setEffectTransform(effect);
    }
}
