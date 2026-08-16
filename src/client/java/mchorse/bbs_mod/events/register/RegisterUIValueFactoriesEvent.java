package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.settings.ui.UIValueMap.IUIValueFactory;
import mchorse.bbs_mod.settings.values.base.BaseValue;

import java.util.Map;

public class RegisterUIValueFactoriesEvent
{
    private final Map<Class<? extends BaseValue>, IUIValueFactory<? extends BaseValue>> factories;

    public RegisterUIValueFactoriesEvent(Map<Class<? extends BaseValue>, IUIValueFactory<? extends BaseValue>> factories)
    {
        this.factories = factories;
    }

    public <T extends BaseValue> void register(Class<T> type, IUIValueFactory<T> factory)
    {
        this.factories.put(type, factory);
    }
}
