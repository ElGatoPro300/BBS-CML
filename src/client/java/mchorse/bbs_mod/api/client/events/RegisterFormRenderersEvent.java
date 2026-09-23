package mchorse.bbs_mod.api.client.events;

import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;

/**
 * Posted on the client once BBS has registered the renderers of its built-in forms.
 */
public class RegisterFormRenderersEvent
{
    public <T extends Form> void register(Class<T> clazz, FormUtilsClient.IFormRendererFactory<T> factory)
    {
        FormUtilsClient.register(clazz, factory);
    }

    public <T extends Form> void registerRenderer(Class<T> clazz, FormUtilsClient.IFormRendererFactory<T> factory)
    {
        FormUtilsClient.register(clazz, factory);
    }
}
