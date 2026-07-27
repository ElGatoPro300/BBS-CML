package mchorse.bbs_mod.ui.forms.editors.panels.widgets;

import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.ui.framework.elements.input.UIEffectTransformCollapse;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Paint {@link EffectTransform} editor as a trailing Transform icon toggle
 * ({@link UIEffectTransformCollapse}). Pair with a paint color row via
 * {@link #withLeading}.
 */
public class UIFormPaintTransform extends UIEffectTransformCollapse
{
    private final Supplier<PaintSettings> settings;

    public UIFormPaintTransform(Supplier<PaintSettings> settings, Consumer<PaintSettings> setter)
    {
        super((apply) ->
        {
            PaintSettings copy = settings.get().copy();

            if (copy.transform == null)
            {
                copy.transform = new EffectTransform();
            }

            apply.accept(copy.transform);
            setter.accept(copy);
        });

        this.settings = settings;
    }

    public void syncFromForm()
    {
        PaintSettings paint = this.settings.get();
        EffectTransform effect = paint == null || paint.transform == null ? new EffectTransform() : paint.transform;

        this.setEffectTransform(effect);
    }
}
