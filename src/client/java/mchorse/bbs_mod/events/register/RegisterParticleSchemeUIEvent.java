package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.ui.framework.elements.UIElement;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RegisterParticleSchemeUIEvent
{
    private final List<Consumer<UIElement>> sectionBuilders = new ArrayList<>();

    public void registerSection(Consumer<UIElement> builder)
    {
        if (builder != null)
        {
            this.sectionBuilders.add(builder);
        }
    }

    public List<Consumer<UIElement>> getSectionBuilders()
    {
        return this.sectionBuilders;
    }
}
