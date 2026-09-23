package mchorse.bbs_mod.api.client.events;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Posted on the client once BBS has registered the editor panels of its built-in forms.
 */
public class RegisterFormEditorsEvent
{
    public final Map<Class, Supplier<UIForm>> panels;

    public RegisterFormEditorsEvent()
    {
        this(UIFormEditor.panels);
    }

    public RegisterFormEditorsEvent(Map<Class, Supplier<UIForm>> panels)
    {
        this.panels = panels;
    }

    public <T extends Form> void register(Class<T> clazz, Supplier<UIForm<T>> supplier)
    {
        UIFormEditor.register(clazz, (Supplier) supplier);
    }
}
