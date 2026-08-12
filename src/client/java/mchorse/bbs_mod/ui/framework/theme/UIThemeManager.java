package mchorse.bbs_mod.ui.framework.theme;

import java.util.HashMap;
import java.util.Map;

/**
 * Manager class for registering and switching UI themes.
 */
public class UIThemeManager
{
    private static final Map<String, IUIStyleProvider> THEMES = new HashMap<>();
    private static IUIStyleProvider activeTheme;

    public static void register(IUIStyleProvider theme)
    {
        if (theme != null)
        {
            THEMES.put(theme.getId(), theme);
        }
    }

    public static IUIStyleProvider getActiveTheme()
    {
        return activeTheme;
    }

    public static void setActiveTheme(String id)
    {
        if (THEMES.containsKey(id))
        {
            activeTheme = THEMES.get(id);
        }
    }

    public static Map<String, IUIStyleProvider> getThemes()
    {
        return THEMES;
    }
}
