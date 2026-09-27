package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;
import mchorse.bbs_mod.ui.framework.elements.UIElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class RegisterFormEditorSectionEvent
{
    private final Map<Class<? extends Form>, List<BiConsumer<UIFormPanel<?>, UIElement>>> extensions = new HashMap<>();

    public <T extends Form> void registerSection(Class<T> formClass, BiConsumer<UIFormPanel<T>, UIElement> builder)
    {
        /* Register section builder for form editor panel */
        List<BiConsumer<UIFormPanel<?>, UIElement>> list = this.extensions.computeIfAbsent(formClass, (k) -> new ArrayList<>());
        
        @SuppressWarnings("unchecked")
        BiConsumer<UIFormPanel<?>, UIElement> genericBuilder = (BiConsumer<UIFormPanel<?>, UIElement>) (Object) builder;
        list.add(genericBuilder);
    }

    public Map<Class<? extends Form>, List<BiConsumer<UIFormPanel<?>, UIElement>>> getExtensions()
    {
        return this.extensions;
    }
}
