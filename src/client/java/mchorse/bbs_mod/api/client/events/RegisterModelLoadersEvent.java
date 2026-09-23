package mchorse.bbs_mod.api.client.events;

import mchorse.bbs_mod.cubic.model.ModelManager;
import mchorse.bbs_mod.cubic.model.loaders.IModelLoader;

import java.util.List;

/**
 * Posted on the client before model loaders are initialized.
 */
public class RegisterModelLoadersEvent
{
    private final ModelManager manager;

    public RegisterModelLoadersEvent(ModelManager manager)
    {
        this.manager = manager;
    }

    public void register(IModelLoader loader)
    {
        if (this.manager != null && loader != null)
        {
            this.manager.loaders.add(loader);
        }
    }

    public List<IModelLoader> getLoaders()
    {
        return this.manager == null ? null : this.manager.loaders;
    }
}
