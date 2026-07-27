package mchorse.bbs_mod.events.register;

import java.util.List;

public class RegisterShadersEvent
{
    private final List<Runnable> runnables;

    public RegisterShadersEvent(List<Runnable> runnables)
    {
        this.runnables = runnables;
    }

    public void register(Runnable runnable)
    {
        this.runnables.add(runnable);
    }
}
