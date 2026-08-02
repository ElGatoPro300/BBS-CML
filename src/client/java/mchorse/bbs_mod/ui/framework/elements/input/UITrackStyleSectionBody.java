package mchorse.bbs_mod.ui.framework.elements.input;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;

import java.util.function.IntSupplier;

/**
 * Disclosure body panel: draws a light section gradient under children.
 * Nested sections share the same content alignment (no extra left indent).
 */
public class UITrackStyleSectionBody extends UIElement
{
    private final IntSupplier color;

    public UITrackStyleSectionBody(IntSupplier color)
    {
        super();

        this.color = color;
        this.column(0).vertical().stretch();
    }

    public UITrackStyleSectionBody addContent(UIElement child)
    {
        this.add(child);

        return this;
    }

    @Override
    public void render(UIContext context)
    {
        UITrackStyleSectionHeader.renderBodyChrome(context, this.area, this.color.getAsInt());
        super.render(context);
    }
}
