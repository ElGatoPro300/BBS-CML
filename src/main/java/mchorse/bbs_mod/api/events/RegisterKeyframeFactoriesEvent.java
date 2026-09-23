package mchorse.bbs_mod.api.events;

import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.Map;

/**
 * Posted on both sides once BBS has registered its built-in keyframe value types.
 */
public class RegisterKeyframeFactoriesEvent
{
    public final Map<String, IKeyframeFactory> factories;

    public RegisterKeyframeFactoriesEvent()
    {
        this(KeyframeFactories.FACTORIES);
    }

    public RegisterKeyframeFactoriesEvent(Map<String, IKeyframeFactory> factories)
    {
        this.factories = factories;
    }

    public void register(String key, IKeyframeFactory factory)
    {
        this.factories.put(key, factory);
    }
}
