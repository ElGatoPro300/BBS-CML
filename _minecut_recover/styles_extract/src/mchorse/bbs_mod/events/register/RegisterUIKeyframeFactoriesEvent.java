package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory.IUIKeyframeFactoryFactory;
import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;

import java.util.Map;

public class RegisterUIKeyframeFactoriesEvent
{
    private final Map<IKeyframeFactory, IUIKeyframeFactoryFactory> factories;

    public RegisterUIKeyframeFactoriesEvent(Map<IKeyframeFactory, IUIKeyframeFactoryFactory> factories)
    {
        this.factories = factories;
    }

    public void register(IKeyframeFactory factory, IUIKeyframeFactoryFactory uiFactory)
    {
        this.factories.put(factory, uiFactory);
    }
}
