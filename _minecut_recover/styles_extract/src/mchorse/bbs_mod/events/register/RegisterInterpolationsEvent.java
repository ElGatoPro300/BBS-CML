package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.utils.interps.IInterp;

import java.util.Map;

public class RegisterInterpolationsEvent
{
    public final Map<String, IInterp> interpolations;

    public RegisterInterpolationsEvent(Map<String, IInterp> interpolations)
    {
        this.interpolations = interpolations;
    }

    public void register(String id, IInterp interpolation)
    {
        this.interpolations.put(id, interpolation);
    }
}
