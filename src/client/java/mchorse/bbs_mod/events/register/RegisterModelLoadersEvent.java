package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.cubic.model.ModelManager;
import mchorse.bbs_mod.cubic.model.loaders.IModelLoader;

public class RegisterModelLoadersEvent
{
    private final ModelManager manager;

    public RegisterModelLoadersEvent(ModelManager manager)
    {
        this.manager = manager;
    }

    public void registerLoader(IModelLoader loader)
    {
        this.manager.registerLoader(loader);
    }

    public void registerRelodableSuffix(String suffix)
    {
        this.manager.registerRelodableSuffix(suffix);
    }
}
