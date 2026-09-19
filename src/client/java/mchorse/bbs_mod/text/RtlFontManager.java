package mchorse.bbs_mod.text;

import mchorse.bbs_mod.ui.framework.elements.utils.CustomFontManager;

/**
 * Loads the UI font for RTL languages (Persian, Arabic, Urdu): either the user-selected
 * {@code .ttf} from settings, or the bundled Vazirmatn fallback.
 */
public final class RtlFontManager
{
    private static String loadedSourceKey;

    private RtlFontManager()
    {}

    public static void ensureLoaded()
    {
        if (!RtlTextEngine.isActive())
        {
            return;
        }

        String path = CustomFontManager.getConfiguredFontPath();
        float size = CustomFontManager.getFontScale();
        String sourceKey = path.isEmpty() ? "default|" + size : path + "|" + size;

        if (sourceKey.equals(loadedSourceKey) && RtlAwtTextRenderer.isReady())
        {
            return;
        }

        if (!path.isEmpty())
        {
            byte[] bytes = CustomFontManager.readConfiguredFontBytes();

            if (bytes != null)
            {
                RtlAwtTextRenderer.invalidate();
                RtlAwtTextRenderer.loadFont(bytes);

                if (RtlAwtTextRenderer.isReady())
                {
                    loadedSourceKey = sourceKey;

                    return;
                }
            }
        }

        loadedSourceKey = "default|" + size;
        RtlAwtTextRenderer.invalidate();
        RtlAwtTextRenderer.ensureFont();
    }

    public static void invalidate()
    {
        loadedSourceKey = null;
        RtlTextEngine.clearCache();
        RtlAwtTextRenderer.invalidate();
        CustomFontManager.invalidateBundledFont();
    }
}
