package mchorse.bbs_mod.api.client.events;

import mchorse.bbs_mod.settings.ui.UIValueMap;
import mchorse.bbs_mod.settings.values.base.BaseValue;

import java.util.Map;

/**
 * Posted on the client once BBS has registered the UI widgets for its built-in Value types.
 */
public class RegisterValueWidgetsEvent
{
    public final Map<Class<? extends BaseValue>, UIValueMap.IUIValueFactory<? extends BaseValue>> factories;

    public RegisterValueWidgetsEvent()
    {
        this(UIValueMap.factories);
    }

    public RegisterValueWidgetsEvent(Map<Class<? extends BaseValue>, UIValueMap.IUIValueFactory<? extends BaseValue>> factories)
    {
        this.factories = factories;
    }

    public <T extends BaseValue> void register(Class<T> clazz, UIValueMap.IUIValueFactory<T> factory)
    {
        UIValueMap.register(clazz, factory);
    }
}
