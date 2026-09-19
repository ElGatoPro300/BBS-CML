package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.utils.IRayTracingHandler;
import mchorse.bbs_mod.utils.RayTracing;

public class RegisterRayTracingEvent
{
    public void register(IRayTracingHandler handler)
    {
        RayTracing.handlers.add(handler);
    }
}
