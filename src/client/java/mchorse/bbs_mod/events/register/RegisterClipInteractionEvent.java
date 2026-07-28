package mchorse.bbs_mod.events.register;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RegisterClipInteractionEvent
{
    private final List<Consumer<Object>> doubleClickHandlers = new ArrayList<>();

    public void registerDoubleClick(Consumer<Object> handler)
    {
        if (handler != null)
        {
            this.doubleClickHandlers.add(handler);
        }
    }

    public List<Consumer<Object>> getDoubleClickHandlers()
    {
        return this.doubleClickHandlers;
    }
}
