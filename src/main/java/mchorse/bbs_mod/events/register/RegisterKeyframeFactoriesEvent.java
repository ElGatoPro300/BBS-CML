package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;

import java.util.Map;

public class RegisterKeyframeFactoriesEvent
{
    private final Map<String, IKeyframeFactory> factories;

    public RegisterKeyframeFactoriesEvent(Map<String, IKeyframeFactory> factories)
    {
        this.factories = factories;
    }

    public void register(String name, IKeyframeFactory factory)
    {
        this.factories.put(name, factory);
    }
}
