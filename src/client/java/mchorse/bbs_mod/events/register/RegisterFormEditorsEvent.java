package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;

import java.util.Map;
import java.util.function.Supplier;

public class RegisterFormEditorsEvent
{
    private final Map<Class, Supplier<UIForm>> panels;

    public RegisterFormEditorsEvent(Map<Class, Supplier<UIForm>> panels)
    {
        this.panels = panels;
    }

    public void register(Class clazz, Supplier<UIForm> supplier)
    {
        this.panels.put(clazz, supplier);
    }
}
