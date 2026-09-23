package mchorse.bbs_mod.api.client.events;

import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;

import java.util.function.Consumer;

/**
 * Posted on the client to register consumers that can inject sub-panels into {@link UIForm} instances.
 */
public class RegisterFormPanelsEvent
{
    public void register(Consumer<UIForm<?>> factory)
    {
        UIForm.registerPanelExtension(factory);
    }
}
