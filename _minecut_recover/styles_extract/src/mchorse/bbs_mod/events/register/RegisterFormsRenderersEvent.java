package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.FormUtilsClient.IFormRendererFactory;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.utils.UIPickableFormRenderer;

import java.util.function.Function;
import java.util.function.Supplier;

public class RegisterFormsRenderersEvent
{
    public <T extends Form> void registerRenderer(Class<T> clazz, IFormRendererFactory<T> factory)
    {
        FormUtilsClient.register(clazz, factory);
    }

    public void registerPanel(Class<? extends Form> clazz, Supplier<UIForm> supplier)
    {
        UIFormEditor.panels.put(clazz, supplier);
    }

    public void registerEditorRenderer(Function<UIFormEditor, UIPickableFormRenderer> factory)
    {
        UIFormEditor.rendererFactory = factory;
    }
}
