package mchorse.bbs_mod.events.register;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class RegisterFormBlendEvent
{
    private final List<BiConsumer<Object, Object>> blendHandlers = new ArrayList<>();

    public void registerBlendHandler(BiConsumer<Object, Object> handler)
    {
        if (handler != null)
        {
            this.blendHandlers.add(handler);
        }
    }

    public List<BiConsumer<Object, Object>> getBlendHandlers()
    {
        return this.blendHandlers;
    }
}
