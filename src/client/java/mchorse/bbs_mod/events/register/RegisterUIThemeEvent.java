package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.ui.framework.theme.IUIStyleProvider;
import mchorse.bbs_mod.ui.framework.theme.UIThemeManager;

public class RegisterUIThemeEvent
{
    public void register(IUIStyleProvider theme)
    {
        UIThemeManager.register(theme);
    }
}
