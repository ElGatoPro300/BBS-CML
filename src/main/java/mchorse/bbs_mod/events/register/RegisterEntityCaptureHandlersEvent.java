package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.morphing.IEntityCaptureHandler;

import java.util.List;

public class RegisterEntityCaptureHandlersEvent
{
    private final List<IEntityCaptureHandler> handlers;

    public RegisterEntityCaptureHandlersEvent(List<IEntityCaptureHandler> handlers)
    {
        this.handlers = handlers;
    }

    public void register(IEntityCaptureHandler handler)
    {
        this.handlers.add(handler);
    }
}
