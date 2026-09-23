package mchorse.bbs_mod.api.client.events;

import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;

import java.util.function.Consumer;

public class RegisterStencilMapEvent
{
    public void register(Consumer<StencilMap> consumer)
    {
        StencilMap.extensions.add(consumer);
    }
}
