package mchorse.bbs_mod.events.register;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RegisterParticleSimulationsEvent
{
    private final List<Consumer<Object>> particleSimulationHandlers = new ArrayList<>();

    public void register(Consumer<Object> handler)
    {
        if (handler != null)
        {
            this.particleSimulationHandlers.add(handler);
        }
    }

    public List<Consumer<Object>> getParticleSimulationHandlers()
    {
        return this.particleSimulationHandlers;
    }
}
