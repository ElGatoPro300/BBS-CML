package mchorse.bbs_mod.api.client.events;

import mchorse.bbs_mod.ui.utils.keys.KeybindSettings;

/**
 * Posted on the client so addons can register classes containing {@link mchorse.bbs_mod.ui.utils.keys.KeyCombo} fields.
 */
public class RegisterKeybindsEvent
{
    public void register(Class clazz)
    {
        KeybindSettings.registerClass(clazz);
    }
}
